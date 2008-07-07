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
import static omr.plugin.Dependency.*;
import omr.plugin.Plugin;
import static omr.plugin.PluginType.*;

import omr.score.MeasureRange;
import omr.score.Score;
import omr.score.ScoreManager;
import static omr.score.midi.MidiAgent.Status.*;
import omr.score.ui.ScoreActions;
import omr.score.ui.ScoreDependent;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionObserver;
import omr.selection.SelectionTag;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.ui.MainGui;
import omr.ui.util.FileFilter;
import omr.ui.util.UIUtilities;

import omr.util.BasicTask;
import omr.util.Implement;
import omr.util.Logger;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import java.awt.event.*;
import java.io.*;

import javax.swing.*;

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

    //---------//
    // getName //
    //---------//
    @Implement(SelectionObserver.class)
    @Override
    public String getName ()
    {
        return "MidiActions";
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
        Sheet sheet = SheetManager.getSelectedSheet();

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

    //--------//
    // update //
    //--------//
    /**
     * Notification of sheet selection, to update UI actions
     *
     * @param selection the selection object (SHEET)
     * @param hint processing hint (not used)
     */
    @Implement(SelectionObserver.class)
    @Override
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        super.update(selection, hint);

        if (selection.getTag() == SelectionTag.SHEET) {
            updateActions();
        }
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

        // To be removed ASAP
        if (!MainGui.useSwingApplicationFramework()) {
            if (playAction == null) {
                // Action instances
                omr.ui.ActionManager mgr = omr.ui.ActionManager.getInstance();
                playAction = mgr.getActionInstance(PlayAction.class.getName());
                pauseAction = mgr.getActionInstance(
                    PauseAction.class.getName());
                stopAction = mgr.getActionInstance(StopAction.class.getName());
                writeAction = mgr.getActionInstance(
                    WriteAction.class.getName());
            }

            playAction.setEnabled(isMidiPlayable());
            pauseAction.setEnabled(isMidiPausable());
            stopAction.setEnabled(isMidiStoppable());
            writeAction.setEnabled(isMidiWritable());
        }
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
        Sheet sheet = SheetManager.getSelectedSheet();

        if (sheet == null) {
            return null;
        } else {
            return sheet.getScore();
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------------//
    // PauseAction //
    //-------------//
    @Deprecated
    @Plugin(type = MIDI_EXPORT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class PauseAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .pauseMidi(e);
        }
    }

    //------------//
    // PlayAction //
    //------------//
    @Deprecated
    @Plugin(type = MIDI_EXPORT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class PlayAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Task task = getInstance()
                            .playMidi(e);

            if (task != null) {
                task.execute();
            }
        }
    }

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

    //------------//
    // StopAction //
    //------------//
    @Deprecated
    @Plugin(type = MIDI_EXPORT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class StopAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .stopMidi(e);
        }
    }

    //-------------//
    // WriteAction //
    //-------------//
    @Deprecated
    @Plugin(type = MIDI_EXPORT, dependency = SCORE_AVAILABLE, onToolbar = false)
    public static class WriteAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Task task = getInstance()
                            .writeMidi(e);

            if (task != null) {
                task.execute();
            }
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
