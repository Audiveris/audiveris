//----------------------------------------------------------------------------//
//                                                                            //
//                           M i d i A c t i o n s                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.midi;

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
import omr.sheet.ui.SheetsController;

import omr.util.BasicTask;
import omr.util.Implement;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import java.awt.event.*;

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

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MidiActions.class);

    //~ Instance fields --------------------------------------------------------

    // Companion Midi Agent
    private volatile MidiAgent agent;

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

        return new WriteTask(score);
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

        //~ Constructors -------------------------------------------------------

        WriteTask (Score score)
        {
            this.score = score;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                ScoreManager.getInstance()
                            .midiWrite(score, null);
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
