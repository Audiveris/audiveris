//----------------------------------------------------------------------------//
//                                                                            //
//                           M i d i A c t i o n s                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.score.midi;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.MeasureRange;
import omr.score.Score;
import omr.score.ScoreManager;
import static omr.score.midi.MidiAgent.Status.*;
import omr.score.ui.ScoreActions;
import omr.score.ui.ScoreDependent;

import omr.selection.MouseMovement;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;

import omr.ui.util.FileFilter;
import omr.ui.util.UIUtilities;

import omr.util.BasicTask;
import omr.util.Implement;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import java.awt.event.*;
import java.io.*;
import omr.sheet.ui.SheetsController;

/**
 * Class <code>MidiActions</code> is merely a collection of UI actions that
 * drive the MidiAgent activity for Midi playback (Play, Pause, Stop) and for
 * writing Midi files.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class MidiActions
    extends ScoreDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MidiActions.class);

    //~ Instance fields --------------------------------------------------------

    // Companion Midi Agent
    private volatile MidiAgent agent;

    // Action instances (to be removed ASAP)
    private javax.swing.Action playAction;
    private javax.swing.Action pauseAction;
    private javax.swing.Action stopAction;
    private javax.swing.Action writeAction;

    // Status variables
    private boolean midiPlayable = false;
    private boolean midiPausable = false;
    private boolean midiStoppable = false;
    private boolean midiWritable = false;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // MidiActions //
    //-------------//
    private MidiActions ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    public static MidiActions getInstance ()
    {
        return Holder.INSTANCE;
    }

    //-----------------//
    // setMidiPausable //
    //-----------------//
    public void setMidiPausable (boolean midiPausable)
    {
        boolean oldValue = this.midiPausable;
        this.midiPausable = midiPausable;
        firePropertyChange("midiPausable", oldValue, this.midiPausable);
    }

    //----------------//
    // isMidiPausable//
    //----------------//
    public boolean isMidiPausable ()
    {
        return midiPausable;
    }

    //-----------------//
    // setMidiPlayable //
    //-----------------//
    public void setMidiPlayable (boolean midiPlayable)
    {
        boolean oldValue = this.midiPlayable;
        this.midiPlayable = midiPlayable;
        firePropertyChange("midiPlayable", oldValue, this.midiPlayable);
    }

    //----------------//
    // isMidiPlayable //
    //----------------//
    public boolean isMidiPlayable ()
    {
        return midiPlayable;
    }

    //------------------//
    // setMidiStoppable //
    //------------------//
    public void setMidiStoppable (boolean midiStoppable)
    {
        boolean oldValue = this.midiStoppable;
        this.midiStoppable = midiStoppable;
        firePropertyChange("midiStoppable", oldValue, this.midiStoppable);
    }

    //-----------------//
    // isMidiStoppable //
    //-----------------//
    public boolean isMidiStoppable ()
    {
        return midiStoppable;
    }

    //-----------------//
    // setMidiWritable //
    //-----------------//
    public void setMidiWritable (boolean midiWritable)
    {
        boolean oldValue = this.midiWritable;
        this.midiWritable = midiWritable;
        firePropertyChange("midiWritable", oldValue, this.midiWritable);
    }

    //----------------//
    // isMidiWritable //
    //----------------//
    public boolean isMidiWritable ()
    {
        return midiWritable;
    }

    //--------//
    // update //
    //--------//
    /**
     * Notification of sheet selection, to update UI actions
     *
     * @param event the notified sheet event
     */
    @Implement(EventSubscriber.class)
    @Override
    public void onEvent (SheetEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            super.onEvent(event);

            updateActions();
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }

    //-----------//
    // pauseMidi //
    //-----------//
    /**
     * Action that allows to pause a Midi playback.
     * @param e the event which triggered this action
     */
    @Action(enabledProperty = "midiPausable")
    public void pauseMidi (ActionEvent e)
    {
        getAgent()
            .pause();
    }

    //----------//
    // playMidi //
    //----------//
    /**
     * Action that allows to start or resume a Midi playback.
     * @param e the event which triggered this action
     * @return the asynchronous task, or null
     */
    @Action(enabledProperty = "midiPlayable")
    public Task playMidi (ActionEvent e)
    {
        Sheet sheet = SheetsController.selectedSheet();

        if (sheet != null) {
            Score score = sheet.getScore();

            if ((score != null) && ScoreActions.checkParameters(score)) {
                return new PlayTask(score, null);
            }
        }

        return null;
    }

    //----------//
    // stopMidi //
    //----------//
    /**
     * Action that allows to stop a Midi playback.
     * @param e the event which triggered this action
     */
    @Action(enabledProperty = "midiStoppable")
    public void stopMidi (ActionEvent e)
    {
        getAgent()
            .stop();
    }

    //---------------//
    // updateActions //
    //---------------//
    /**
     * Refresh the various Midi actions, according to the current context
     */
    public void updateActions ()
    {
        MidiAgent.Status status = getAgent()
                                      .getStatus();
        ///logger.info("updateActions, status=" + status);
        setMidiPlayable(isScoreAvailable() && (status != PLAYING));
        setMidiPausable(isScoreAvailable() && (status == PLAYING));
        setMidiStoppable(isScoreAvailable() && (status != STOPPED));
        setMidiWritable(
            isScoreAvailable() &&
            ((status == STOPPED) ||
                        (getAgent()
                             .getScore() == getCurrentScore())));
    }

    //-----------//
    // writeMidi //
    //-----------//
    /**
     * Action that allows to write a Midi file.
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action(enabledProperty = "midiWritable")
    public Task writeMidi (ActionEvent e)
    {
        Score score = getInstance()
                          .getCurrentScore();

        if (score == null) {
            return null;
        }

        // Check whether global score parameters have been set
        if (!ScoreActions.checkParameters(score)) {
            return null;
        }

        // Let the user select a score output file
        File       midiFile = new File(
            constants.defaultMidiDirectory.getValue(),
            score.getRadix() + MidiAbstractions.MIDI_EXTENSION);
        FileFilter filter = new FileFilter(
            "Midi files",
            new String[] { MidiAbstractions.MIDI_EXTENSION });
        midiFile = UIUtilities.fileChooser(true, null, midiFile, filter);

        if (midiFile != null) {
            return new WriteTask(score, midiFile);
        } else {
            return null;
        }
    }

    //----------//
    // getAgent //
    //----------//
    private MidiAgent getAgent ()
    {
        if (agent == null) {
            try {
                agent = MidiAgent.getInstance();
            } catch (Exception ex) {
                logger.severe("Cannot get MidiAgent", ex);
            }
        }

        return agent;
    }

    //-----------------//
    // getCurrentScore //
    //-----------------//
    /**
     * Report the currently selected score, if any
     * @return the current score, or null
     */
    private Score getCurrentScore ()
    {
        Sheet sheet = SheetsController.selectedSheet();

        if (sheet == null) {
            return null;
        } else {
            return sheet.getScore();
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------//
    // PlayTask //
    //----------//
    /**
     * Asynchronous task to play the provided score
     */
    public static class PlayTask
        extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        private final Score  score;
        private MeasureRange measureRange;

        //~ Constructors -------------------------------------------------------

        public PlayTask (Score        score,
                         MeasureRange measureRange)
        {
            this.score = score;
            this.measureRange = measureRange;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            if (score != null) {
                try {
                    if (measureRange == null) {
                        measureRange = score.getMeasureRange();
                    }

                    MidiAgent agent = MidiAgent.getInstance();
                    agent.setScore(score);
                    agent.play(measureRange);
                } catch (Exception ex) {
                    logger.warning("Cannot play", ex);
                }
            }

            return null;
        }
    }

    //-----------//
    // WriteTask //
    //-----------//
    /**
     * Asynchronous task to write the provided score into a midi file
     */
    public static class WriteTask
        extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        private final Score score;
        private final File  midiFile;

        //~ Constructors -------------------------------------------------------

        WriteTask (Score score,
                   File  midiFile)
        {
            this.score = score;
            this.midiFile = midiFile;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                ScoreManager.getInstance()
                            .midiWrite(score, midiFile);
                // Remember (even across runs) the selected directory
                constants.defaultMidiDirectory.setValue(midiFile.getParent());
            } catch (Exception ignored) {
                // User already informed
            }

            return null;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String defaultMidiDirectory = new Constant.String(
            "",
            "Default directory for writing Midi files");
    }

    //--------//
    // Holder //
    //--------//
    private static class Holder
    {
        //~ Static fields/initializers -----------------------------------------

        public static final MidiActions INSTANCE = new MidiActions();
    }
}
