//----------------------------------------------------------------------------//
//                                                                            //
//                           M i d i A c t i o n s                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.midi;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ScoresManager;
import omr.score.entity.MeasureId.MeasureRange;
import omr.score.ui.ScoreActions;
import omr.score.ui.ScoreDependent;

import omr.selection.MouseMovement;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

import omr.step.Steps;

import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtilities;

import omr.util.BasicTask;
import omr.util.Implement;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Class <code>MidiActions</code> is merely a collection of UI actions that
 * drive the MidiAgent activity for Midi playback (Play, Pause, Stop) and for
 * writing Midi files.
 *
 * @author Hervé Bitteur
 */
public class MidiActions
    extends ScoreDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MidiActions.class);

    /** Is Midi available */
    protected static final String MIDI_AVAILABLE = "midiAvailable";

    //~ Instance fields --------------------------------------------------------

    // Companion Midi Agent
    private volatile MidiAgent agent;

    // Status variables
    private boolean midiPlayable = false;

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

    //------------------//
    // setMidiAvailable //
    //------------------//
    public void setMidiAvailable (boolean midiPlayable)
    {
        boolean oldValue = this.midiPlayable;
        this.midiPlayable = midiPlayable;
        firePropertyChange(MIDI_AVAILABLE, oldValue, this.midiPlayable);
    }

    //-----------------//
    // isMidiAvailable //
    //-----------------//
    public boolean isMidiAvailable ()
    {
        return midiPlayable;
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

    //----------//
    // playMidi //
    //----------//
    /**
     * Action that launches the Zong! player on the score
     * @param e the event which triggered this action
     * @return the asynchronous task, or null
     */
    @Action(enabledProperty = MIDI_AVAILABLE)
    public Task playMidi (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            Score score = sheet.getScore();

            if ((score != null) && ScoreActions.checkParameters(score)) {
                return new PlayTask(score, null);
            }
        }

        return null;
    }

    //---------------//
    // updateActions //
    //---------------//
    /**
     * Refresh the various Midi actions, according to the current context
     */
    public void updateActions ()
    {
        final Sheet sheet = SheetsController.getCurrentSheet();
        setMidiAvailable(
            (sheet != null) && sheet.isDone(Steps.valueOf(Steps.MERGE)));
    }

    //-----------//
    // writeMidi //
    //-----------//
    /**
     * Action that allows to write a Midi file.
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action(enabledProperty = MIDI_AVAILABLE)
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

        final File midiFile = score.getMidiFile();

        if (midiFile != null) {
            return new WriteTask(score, midiFile);
        } else {
            return writeMidiAs(e);
        }
    }

    //-------------//
    // writeMidiAs //
    //-------------//
    /**
     * Export the MIDI data to a user-provided file
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = MIDI_AVAILABLE)
    public Task writeMidiAs (ActionEvent e)
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

        // Let the user select a MIDI output file
        File midiFile = UIUtilities.fileChooser(
            true,
            null,
            ScoresManager.getInstance().getDefaultMidiFile(null, score),
            new OmrFileFilter(
                "MIDI files",
                new String[] { MidiAbstractions.MIDI_EXTENSION }));

        if (midiFile != null) {
            return new WriteTask(score, midiFile);
        } else {
            return null;
        }
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
        Sheet sheet = SheetsController.getCurrentSheet();

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

        /**
         * Play (part of) the score
         * @param score the score to play
         * @param measureRange a specific range of measures, or (if null) the
         * default measure range as stored in the score
         */
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

                    MidiAgent agent = MidiAgentFactory.getAgent();
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

        public WriteTask (Score score,
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
                ScoresManager.getInstance()
                             .midiWrite(score, midiFile);
            } catch (Exception ignored) {
                // User already informed
            }

            return null;
        }
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
