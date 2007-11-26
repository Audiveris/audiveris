//----------------------------------------------------------------------------//
//                                                                            //
//                             M i d i A g e n t                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.score.midi;

import omr.score.Score;
import omr.score.ui.ScoreActions;
import omr.score.visitor.ScoreExporter;

import omr.util.Logger;

import com.xenoage.player.ExternalPlayer;

import org.w3c.dom.Document;

import java.io.*;

import javax.sound.midi.*;
import javax.xml.parsers.*;

/**
 * Class <code>MidiAgent</code> is in charge of representing a score when
 * dealing with the Midi System. There are two main usages: playing a score
 * and writing the Midi file for a score.
 *
 * There is only one instance of this class for the whole application, to allow
 * the preloading of the default Midi sequencer. Also, it would not really make
 * sense to have two score playbacks at the same time, since there is no means
 * to accurately synchronize the moment when a playback really starts.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class MidiAgent
    extends ExternalPlayer
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MidiAgent.class);

    /** Type used when writing Midi files */
    public static final int MIDI_FILE_TYPE = 1;

    /** The only instance of this class */
    private static volatile MidiAgent INSTANCE;

    //~ Enumerations -----------------------------------------------------------

    /**
     * The various possibilities for the status of this entity regarding Midi
     * playback
     */
    public enum Status {
        //~ Enumeration constant initializers ----------------------------------


        /** Playback is not started or paused, sequence position is irrelevant */
        STOPPED,
        /** Playback has started */
        PLAYING, 
        /** Playback is paused, current sequence position is kept */
        PAUSED;
    }

    //~ Instance fields --------------------------------------------------------

    /** (Current) related score. Beware of memory leak! */
    private Score score;

    /** Current status of this player */
    private Status status = Status.STOPPED;

    /** The MusicXML document */
    private Document document;

    /** In charge of receiving Midi events to update score display */
    private MidiReceiver receiver;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // MidiAgent //
    //-----------//
    /**
     * Create a Midi AGent
     *
     * @param score the related score
     * @exception MidiUnavailableException if Midi resources can't be found
     */
    private MidiAgent ()
        throws MidiUnavailableException
    {
        super(null);
        receiver = new MidiReceiver(this);
        connectReceiver(receiver);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this singleton class (after creating it if
     * necessary)
     * @return the single instance of MidiAgent
     * @throws omr.score.midi.MidiAgent.UnavailableException
     */
    public static MidiAgent getInstance ()
        throws UnavailableException
    {
        if (INSTANCE == null) {
            synchronized (MidiAgent.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = new MidiAgent();
                    } catch (MidiUnavailableException ex) {
                        throw new UnavailableException(ex.getMessage());
                    }
                }
            }
        }

        return INSTANCE;
    }

    //----------//
    // setScore //
    //----------//
    /**
     * Assign a score to the Midi Agent
     * @param score the new current score (perhaps null)
     */
    public void setScore (Score score)
    {
        if (this.score != score) {
            reset();
            this.score = score;
        }
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the score (if any) currently handled by the Midi Agent
     * @return the score currently handled, if any
     */
    public Score getScore ()
    {
        return score;
    }

    //-----------//
    // getStatus //
    //-----------//
    /**
     * Report the current status of the agent
     *
     * @return the current status (STOPPED, PLAYING, PAUSED)
     */
    public Status getStatus ()
    {
        return status;
    }

    //-------//
    // pause //
    //-------//
    /**
     * Pause the playback, keeping the current position in the Midi sequence
     */
    @Override
    public void pause ()
    {
        logger.info("Paused " + score.getRadix() + "...");
        status = Status.PAUSED;
        MidiActions.updateActions();
        super.pause();
    }

    //------//
    // play //
    //------//
    /**
     * Start the playback from start (or continue if just paused)
     */
    @Override
    public void play ()
    {
        if (ScoreActions.checkParameters(score)) {
            logger.info("Playing " + score.getRadix() + "...");
            status = Status.PLAYING;
            MidiActions.updateActions();

            // Make sure the document (and the Midi sequence) is available
            getDocument();

            //                connectSequencer();
            //                sequencer.setSequence(sequence);

            // We could adjust the tempo here
            ///sequencer.setTempoFactor(2.0f);
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Midi sequence length is " + getLengthInMs() + " ms");
                logger.fine("Midi tick length is " + getLengthInTicks());
            }

            receiver.setScore(score);
            super.play();
        }
    }

    //--------------//
    // preloadAgent //
    //--------------//
    /**
     * A convenient method to preload the instance of MidiAgent class
     */
    public static void preloadAgent ()
    {
        try {
            if (logger.isFineEnabled()) {
                logger.fine("preloading the Midi agent...");
            }

            getInstance();

            if (logger.isFineEnabled()) {
                logger.fine("Midi agent loaded.");
            }
        } catch (MidiUnavailableException ex) {
            logger.warning("Could not preload the Midi Agent", ex);
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
        document = null;

        if (receiver != null) {
            receiver.reset();
        }
    }

    //------//
    // stop //
    //------//
    /**
     * Stop the playback, discarding current position in the sequence
     */
    @Override
    public void stop ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("Stopping...");
        }

        status = Status.STOPPED;
        super.stop();

        if (logger.isFineEnabled()) {
            logger.fine("super Stopped.");
        }

        receiver.reset();
        MidiActions.updateActions();
        logger.info("Stopped.");
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
        saveSequence(os);
    }

    //-------//
    // write //
    //-------//
    /**
     * Write the Midi sequence to an output file
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

    //--------//
    // ending //
    //--------//
    /**
     * Notification of the end of playback
     */
    void ending ()
    {
        logger.info("Ended.");
        status = Status.STOPPED;
        MidiActions.updateActions();
    }

    //-------------//
    // getDocument //
    //-------------//
    /**
     * Make sure the MusicXML document (and its Midi counterpart) is available
     */
    private void getDocument ()
    {
        if (document == null) {
            try {
                // Populate the document
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder        builder = factory.newDocumentBuilder();
                document = builder.newDocument();
                new ScoreExporter(score).export(document);

                // Hand it over directly to MusicXML reader
                setMusicXMLData(document);
            } catch (Exception ex) {
                logger.warning("Midi Agent error", ex);
                document = null; // Safer
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------------//
    // UnavailableException //
    //----------------------//
    /**
     * Specific exception class, to avoid the need for users to import Midi
     * exception
     */
    public static class UnavailableException
        extends MidiUnavailableException
    {
        //~ Constructors -------------------------------------------------------

        public UnavailableException (String message)
        {
            super(message);
        }
    }
}
