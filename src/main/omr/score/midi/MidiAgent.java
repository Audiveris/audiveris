//----------------------------------------------------------------------------//
//                                                                            //
//                             M i d i A g e n t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.midi;

import omr.Main;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ScoreExporter;
import omr.score.entity.MeasureId.MeasureRange;
import omr.score.ui.ScoreActions;

import com.xenoage.util.io.IO;
import com.xenoage.util.language.Lang;
import com.xenoage.util.language.LanguageInfo;
import com.xenoage.util.logging.Log;
import com.xenoage.zong.io.midi.out.SynthManager;
import com.xenoage.zong.musicxml.MusicXMLDocument;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Class {@code MidiAgent} is in charge of representing a score when
 * dealing with the Midi System. There are two main usages: playing a score
 * and writing the Midi file for a score.
 *
 * <p>There is only one instance of this class for the whole application, to
 * allow the preloading of the default Midi sequencer. Also, it would not really
 * make sense to have two score playbacks at the same time, since there is no
 * means to accurately synchronize the moment when a playback really starts.
 *
 * <p>This class now delegates to the Zong! player the actual play / pause /
 * stop actions with a few other Midi-related actions. The purpose of the
 * {@link #play} method is just to launch the player on the current score.
 *
 * <p>There is no public constructor for this class. The (single) instance
 * must be obtained through the {@link MidiAgentFactory} class.
 *
 * @author Hervé Bitteur
 */
public class MidiAgent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MidiAgent.class);

    /** Type used when writing Midi files */
    public static final int MIDI_FILE_TYPE = 1;

    //~ Instance fields --------------------------------------------------------

    /** The underlying Zong player */
    private OmrFrameController controller;

    /** In charge of receiving Midi events to update score display */
    private final MidiReceiver receiver;

    /** (Current) related score. Beware of memory leak! */
    private Score score;

    /** The MusicXML document */
    //private ScorePartwise document;
    private Document document;

    /** The range of measures to play (perhaps null, meaning whole score) */
    private MeasureRange measureRange;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // MidiAgent //
    //-----------//
    /**
     * Create the Midi Agent singleton
     */
    MidiAgent ()
    {
        //== Stolen from Zong Player init sequence
        ///System.out.println("Initializing Log");
        Log.initAppletLog();

        ///System.out.println("Initializing IO");
        IO.initApplet(null);

        try {
            //get available languages
            ///System.out.println("Getting available languages");
            List<LanguageInfo> languages = LanguageInfo.getAvailableLanguages(
                Lang.defaultLangPath);

            for (LanguageInfo info : languages) {
                ///System.out.println("available: " + info.getID());
            }

            //use system's default (TODO: config)
            ///System.out.println("Getting langId");
            String langID = LanguageInfo.getDefaultID(languages);
            ///System.out.println("Loading language " + langID);
            Lang.loadLanguage(langID);
        } catch (Exception ex) {
            logger.warning(
                "Could not load Zong language from " + Lang.defaultLangPath,
                ex);
            logger.info("Loading 'en' language");

            try {
                Lang.loadLanguage("en");
            } catch (Exception e) {
                logger.warning("Could not load 'en' language", ex);
            }
        }

        try {
            ///System.out.println("Init SynthManager");
            SynthManager.init(true);
        } catch (Exception ex) {
            logger.warning("Could not initialize MIDI", ex);
            receiver = null;
            controller = null;

            return;
        }

        //== End of init sequence

        // Our playback listener, if needed
        if (Main.getGui() != null) {
            receiver = new MidiReceiver(this);

            try {
                controller = new OmrFrameController(new OmrFrameView());
                controller.addPlaybackListener(receiver);
            } catch (Exception ex) {
                logger.warning("Error creating OmrFrameController", ex);
            }
        } else {
            receiver = null;
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getMeasureRange //
    //-----------------//
    /**
     * Report the current measure range
     * @return the measureRange
     */
    public MeasureRange getMeasureRange ()
    {
        return measureRange;
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the score (if any) currently handled by the Midi Agent
     *
     * @return the score currently handled, if any
     */
    public Score getScore ()
    {
        return score;
    }

    //------//
    // play //
    //------//
    /**
     * Start the playback from start (or continue if just paused)
     *
     * @param measureRange a specific range of measures if non null, otherwise
     * the whole score is played
     */
    public void play (MeasureRange measureRange)
    {
        if (ScoreActions.checkParameters(score)) {
            if (measureRange == null) {
                measureRange = score.getMeasureRange();
            }

            // Do we have a different range of measures?
            boolean rangesDiffer = (this.measureRange != measureRange) &&
                                   ((this.measureRange == null) ||
                                   !(this.measureRange.equals(measureRange)));

            if (!controller.isPlaying() || (document == null) || rangesDiffer) {
                this.measureRange = measureRange;
                logger.info(
                    "Playing " + score.getRadix() +
                    ((measureRange != null) ? (" " + measureRange) : "") +
                    " tempo:" + score.getTempo() + " ...");

                // Make sure the document (and the Midi sequence) is available
                retrieveMusic(measureRange);
                controller.play();
            }
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate the MusicXML document, so that it gets recreated the next time
     * it is needed
     */
    public void reset ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("MidiAgent reset");
        }

        document = null;
        measureRange = null;

        if (receiver != null) {
            receiver.reset();
        }
    }

    //----------//
    // setScore //
    //----------//
    /**
     * Assign a score to the Midi Agent
     *
     * @param score the new current score (perhaps null)
     */
    public void setScore (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("MidiAgent setScore");
        }

        if (this.score != score) {
            reset();
            this.score = score;

            MidiActions.getInstance()
                       .updateActions();
        }
    }

    //-------//
    // write //
    //-------//
    /**
     * Write the Midi sequence to an output stream
     * @param os the stream to be written
     */
    public void write (OutputStream os)
    {
        // Make sure the document (and the Midi sequence) is available
        if ((document == null) || (measureRange != null)) {
            retrieveMusic(null);
        }

        Sequence seq = controller.getSequence();

        if (seq == null) {
            logger.warning("No MIDI sequence");

            return;
        }

        try {
            MidiSystem.write(seq, 1, os);
        } catch (Exception ex) {
            logger.warning("Error saving MIDI sequence", ex);
        }
    }

    //-------//
    // write //
    //-------//
    /**
     * Write the Midi sequence to an output file
     *
     * @param file the output file
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public void write (File file)
        throws FileNotFoundException, IOException
    {
        OutputStream os = new FileOutputStream(file);
        write(os);
        os.close();
    }

    //---------------//
    // retrieveMusic //
    //---------------//
    /**
     * Make sure the score partwise document is available and the corresponding
     * MIDI sequence is ready
     * @param measureRange the required rande of measures, if any
     */
    private void retrieveMusic (MeasureRange measureRange)
    {
        /*
         *
         * So here are the functions you need for Audiveris:
                                                             1) call com.xenoage.zong.musicxml.MusicXMLDocument.read(org.w3c.dom.Document doc)
         * to get an instance of a MusicXMLDocument out of your DOM document
                                                             2) handle this document to
         * com.xenoage.zong.player.gui.Controller.loadScore(MxlScorePartwise doc, boolean ignoreErrors)
         * (set ignoreErrors to true, of course)
         */
        try {
            // Populate the document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder        builder = factory.newDocumentBuilder();
            document = builder.newDocument();

            ScoreExporter exporter = new ScoreExporter(score);
            exporter.setMeasureRange(measureRange);
            exporter.export(document, true); // true for injectSignature

            // Hand it over directly to MusicXML reader
            MusicXMLDocument mxlDocument = MusicXMLDocument.read(document);
            controller.loadDocument(mxlDocument.getScore());
        } catch (Exception ex) {
            logger.warning("Midi Agent error", ex);
            document = null; // Safer
            throw new RuntimeException(ex);
        }
    }
}
