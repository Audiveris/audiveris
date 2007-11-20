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

import omr.score.Score;
import omr.score.ScoreManager;
import static omr.score.midi.MidiAgent.Status.*;
import omr.score.midi.MidiAgent.UnavailableException;
import omr.score.ui.ScoreActions;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionObserver;
import omr.selection.SelectionTag;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.ui.ActionManager;
import omr.ui.UIDressing;
import omr.ui.util.FileFilter;
import omr.ui.util.UIUtilities;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.event.*;
import java.io.*;

import javax.sound.midi.*;
import javax.swing.*;

/**
 * Class <code>MidiActions</code> is merely a collection of UI actions that
 * drive the MidiAgent activity (Play, Pause, Stop)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class MidiActions
    implements SelectionObserver
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MidiActions.class);

    // PauseAction instance
    private static Action pauseAction = new PauseAction();

    static {
        UIDressing.dressUp(pauseAction, pauseAction.getClass().getName());
    }

    // PlayAction instance
    private static Action      playAction = ActionManager.getInstance()
                                                         .getActionInstance(
        PlayAction.class.getName());
    private static Icon        playIcon = (Icon) playAction.getValue(
        Action.SMALL_ICON);
    private static String      playShortDescription = (String) playAction.getValue(
        Action.SHORT_DESCRIPTION);
    private static Action      stopAction = ActionManager.getInstance()
                                                         .getActionInstance(
        StopAction.class.getName());
    private static Action      writeAction = ActionManager.getInstance()
                                                          .getActionInstance(
        WriteAction.class.getName());

    /** A private instance, just to be notified of sheet selection */
    private static MidiActions INSTANCE = new MidiActions();

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // MidiActions //
    //-------------//
    private MidiActions ()
    {
        // Stay informed on sheet selection
        SheetManager.getSelection()
                    .addObserver(this);
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getName //
    //---------//
    @Implement(SelectionObserver.class)
    public String getName ()
    {
        return "MidiActions";
    }

    //------//
    // play //
    //------//
    /**
     * Launch the playback of Midi Agent on the provided score (if paused), or
     * stop the playback if playing.
     * @param score the provided score
     */
    public static void play (final Score score)
    {
        if (score != null) {
            try {
                final MidiAgent agent = MidiAgent.getInstance();

                // Processing depends on player status
                Thread t = new Thread(
                    new Runnable() {
                            public void run ()
                            {
                                if (agent.getStatus() == PLAYING) {
                                    agent.pause();
                                } else {
                                    agent.setScore(score);
                                    agent.play();
                                }
                            }
                        });

                t.start();
            } catch (UnavailableException ex) {
                logger.warning("Cannot play", ex);
            }
        }
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
    public void update (Selection     selection,
                        SelectionHint hint)
    {
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
    static void updateActions ()
    {
        try {
            Sheet sheet = SheetManager.getSelectedSheet();

            if (sheet == null) {
                setPlay(true);
                playAction.setEnabled(false);
                stopAction.setEnabled(false);
                writeAction.setEnabled(false);

                return;
            }

            Score     score = sheet.getScore();
            MidiAgent agent = MidiAgent.getInstance();

            switch (agent.getStatus()) {
            case STOPPED :
                setPlay(true);
                playAction.setEnabled(true);
                stopAction.setEnabled(false);
                writeAction.setEnabled(true);

                break;

            case PLAYING :
            case PAUSED :
                setPlay(agent.getStatus() != PLAYING);

                // Playing/Pausing this score?
                if (agent.getScore() == score) {
                    playAction.setEnabled(true);
                    stopAction.setEnabled(true);
                    writeAction.setEnabled(true);
                } else {
                    playAction.setEnabled(false);
                    stopAction.setEnabled(false);
                    writeAction.setEnabled(false);
                }

                break;
            }
        } catch (UnavailableException ex) {
            logger.warning("Cannot update Midi actions", ex);
        }
    }

    //-----------------//
    // getCurrentScore //
    //-----------------//
    /**
     * Report the currently selected score, if any
     * @return the current score, or null
     */
    private static Score getCurrentScore ()
    {
        Sheet sheet = SheetManager.getSelectedSheet();

        if (sheet == null) {
            return null;
        } else {
            return sheet.getScore();
        }
    }

    //---------//
    // setPlay //
    //---------//
    /**
     * Decorate the Play action for a real play (if boolean play is true) or
     * for a pause (if boolean play is false)
     * @param play true for a true play
     */
    private static void setPlay (boolean play)
    {
        if (play) { // A Play action
            playAction.putValue(Action.SMALL_ICON, playIcon);
            playAction.putValue(Action.SHORT_DESCRIPTION, playShortDescription);
        } else { // A Pause action
            playAction.putValue(
                Action.SMALL_ICON,
                pauseAction.getValue(Action.SMALL_ICON));
            playAction.putValue(
                Action.SHORT_DESCRIPTION,
                pauseAction.getValue(Action.SHORT_DESCRIPTION));
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // PlayAction //
    //------------//
    /**
     * Class <code>PlayAction</code> allows to start , pause or restart a Midi
     * playback.
     */
    @Plugin(type = SCORE_EXPORT, dependency = SHEET_AVAILABLE, onToolbar = true)
    public static class PlayAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Score score = getCurrentScore();

            if (score == null) {
                return;
            }

            // Make sure score parameters are set up
            if (!ScoreActions.checkParameters(score)) {
                return;
            }

            play(score);
        }
    }

    //------------//
    // StopAction //
    //------------//
    /**
     * Class <code>StopAction</code> allows to stop a Midi playback.
     */
    @Plugin(type = SCORE_EXPORT, dependency = SHEET_AVAILABLE, onToolbar = true)
    public static class StopAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Thread t = new Thread(
                new Runnable() {
                        public void run ()
                        {
                            try {
                                MidiAgent.getInstance()
                                         .stop();
                            } catch (UnavailableException ex) {
                                logger.warning("Cannot stop", ex);
                            }
                        }
                    });

            t.start();
        }
    }

    //-------------//
    // WriteAction //
    //-------------//
    /**
     * Class <code>WriteAction</code> allows to write a Midi file.
     */
    @Plugin(type = SCORE_EXPORT, dependency = SHEET_AVAILABLE, onToolbar = false)
    public static class WriteAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Score score = getCurrentScore();

            if (score == null) {
                return;
            }

            // Check whether global score parameters have been set
            if (!ScoreActions.checkParameters(score)) {
                return;
            }

            // Where do we write the score midi file?
            File       midiFile = new File(
                constants.defaultMidiDirectory.getValue(),
                score.getRadix() + MidiAbstractions.MIDI_EXTENSION);

            // Let the user select a score output file
            FileFilter filter = new FileFilter(
                "Midi files",
                new String[] { MidiAbstractions.MIDI_EXTENSION });
            midiFile = UIUtilities.fileChooser(true, null, midiFile, filter);

            if (midiFile != null) {
                try {
                    ScoreManager.getInstance()
                                .midiWrite(score, midiFile);
                    // Remember (even across runs) the selected directory
                    constants.defaultMidiDirectory.setValue(
                        midiFile.getParent());
                } catch (Exception ignored) {
                    // User already informed
                }
            }
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
            "Default directory for Midi files");
    }

    //-------------//
    // PauseAction //
    //-------------//
    //@Plugin(type = SCORE_EXPORT, dependency = SCORE_AVAILABLE, onToolbar = false)
    private static class PauseAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            try {
                MidiAgent.getInstance()
                         .pause();
            } catch (UnavailableException ex) {
                logger.warning("Cannot pause", ex);
            }
        }
    }
}
