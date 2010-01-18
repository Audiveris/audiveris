//----------------------------------------------------------------------------//
//                                                                            //
//                             M i d i A g e n t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.midi;

import omr.log.Logger;

import omr.score.MeasureRange;
import omr.score.Score;
import omr.score.ScoreExporter;
import omr.score.ui.ScoreActions;

import omr.util.OmrExecutors;

import com.xenoage.util.io.IO;
import com.xenoage.util.logging.Log;
import com.xenoage.zong.Zong;
import com.xenoage.zong.io.midi.out.SynthManager;
import com.xenoage.zong.player.gui.Controller;

import proxymusic.ScorePartwise;

import java.io.*;
import java.util.concurrent.*;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;

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
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MidiAgent.class);

    /** Type used when writing Midi files */
    public static final int MIDI_FILE_TYPE = 1;

    /** A future which reflects whether Midi Agent has been initialized **/
    private static final Future<Void> loading = OmrExecutors.getCachedLowExecutor()
                                                            .submit(
        new Callable<Void>() {
                public Void call ()
                    throws Exception
                {
                    try {
                        Object obj = Holder.INSTANCE;
                    } catch (Exception ex) {
                        logger.warning("Could not preload the Midi Agent", ex);
                        throw ex;
                    }

                    return null;
                }
            });


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

    /** The underlying Zong player */
    private final Controller controller;

    /** In charge of receiving Midi events to update score display */
    private final MidiReceiver receiver;

    /** (Current) related score. Beware of memory leak! */
    private Score score;

    /** Current status of this player */
    private Status status = Status.STOPPED;

    /** The MusicXML document */
    private ScorePartwise document;

    /** The range of measures to play (perhaps null, meaning whole score) */
    private MeasureRange measureRange;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // MidiAgent //
    //-----------//
    /**
     * Create the Midi Agent singleton
     */
    private MidiAgent ()
    {
        // Stolen from Zong Player init sequence
        Log.initApplicationLog(
            "zong.log",
            Zong.getNameAndVersion("AudiverisZongPlayer"));

        IO.initApplication();

        try {
            SynthManager.init(true);
        } catch (MidiUnavailableException ex) {
            logger.warning("Could not initialize MIDI");
            receiver = null;
            controller = null;

            return;
        }

        receiver = new MidiReceiver(this);
        controller = new Controller();
        controller.addPlaybackListener(receiver);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this singleton class (after creating it if
     * necessary)
     * @return the single instance of MidiAgent (or null if failed)
     */
    public static MidiAgent getInstance ()
    {
        try {
            loading.get();
        } catch (Throwable ex) {
            logger.severe("Cannot load Midi", ex);

            return null;
        }

        return Holder.INSTANCE;
    }

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

    //---------//
    // preload //
    //---------//
    /**
     * Purpose if to make this class be loaded, and thus the 'loading' task be
     * launched
     */
    public static void preload ()
    {
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

            switch (status) {
            case PLAYING :
                stop();

                break;

            case PAUSED :
                status = Status.STOPPED;
                MidiActions.getInstance()
                           .updateActions();
                logger.info("Stopped.");
            }
        }
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
    public void pause ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("MidiAgent pause");
        }

        if (status == Status.PLAYING) {
            status = Status.PAUSED;
            controller.pause();
            MidiActions.getInstance()
                       .updateActions();
        }
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
            logger.info(
                "Playing " + score.getRadix() +
                ((measureRange != null) ? (" " + measureRange) : "") +
                " tempo:" + score.getTempo() + " volume:" + score.getVolume() +
                " ...");

            if (measureRange == null) {
                measureRange = score.getMeasureRange();
            }

            if ((document == null) || (this.measureRange != measureRange)) {
                this.measureRange = measureRange;

                // Make sure the document (and the Midi sequence) is available
                retrieveMusic(measureRange);
            }

            controller.play();

            status = Status.PLAYING;
            MidiActions.getInstance()
                       .updateActions();
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

    //------//
    // stop //
    //------//
    /**
     * Stop the playback, discarding current position in the sequence
     */
    public void stop ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("MidiAgent stop");
        }

        if ((status == Status.PLAYING) || (status == Status.PAUSED)) {
            status = Status.STOPPED;
            controller.stop();
            document = null; // Workaround for Zong Player
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
        retrieveMusic(null);

        Sequence seq = controller.getPlayer()
                                 .getSequence();

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
        try {
            ScoreExporter exporter = new ScoreExporter(score);
            exporter.setMeasureRange(measureRange);
            document = exporter.buildScorePartwise();

            // Hand it over directly to the MusicXML reader
            controller.loadScore(document);
        } catch (Exception ex) {
            logger.warning("Midi Agent error", ex);
            document = null; // Safer
            throw new RuntimeException(ex);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // Holder //
    //--------//
    private static class Holder
    {
        //~ Static fields/initializers -----------------------------------------

        public static final MidiAgent INSTANCE = new MidiAgent();
    }
}
