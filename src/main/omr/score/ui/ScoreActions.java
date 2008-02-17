//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e A c t i o n s                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.ui;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import static omr.plugin.Dependency.*;
import omr.plugin.Plugin;
import static omr.plugin.PluginType.*;

import omr.score.Score;
import omr.score.entity.ScorePart;
import omr.score.midi.MidiAgent;
import omr.score.midi.MidiAgent.UnavailableException;

import omr.sheet.SheetManager;

import omr.ui.ScoreBoard;

import omr.util.Implement;
import omr.util.Logger;

import org.jdesktop.swingworker.SwingWorker;

import java.awt.event.*;
import java.beans.*;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Class <code>ScoreActions</code> gathers actions related to scores
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreActions
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreActions.class);

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // checkParameters //
    //-----------------//
    /**
     * Make sure that the score parameters are properly set up, even by
     * prompting the user for them, otherwise return false
     *
     * @param score the provided score
     * @return true if OK, false otherwise
     */
    public static boolean checkParameters (Score score)
    {
        if (score.getTempo() == null) {
            if (constants.promptParameters.getValue()) {
                return defineParameters(score);
            } else {
                return setDefaultParameters(score);
            }
        } else {
            return true;
        }
    }

    //------------------//
    // defineParameters //
    //------------------//
    /**
     * Prompts the user for interactive confirmation or modification of
     * score parameters
     *
     * @param score the provided score
     * @return true if parameters are accepted, false if not
     */
    public static boolean defineParameters (Score score)
    {
        final boolean[]   apply = new boolean[1];
        final ScoreBoard  scoreBoard = new ScoreBoard("Parameters", score);
        final JOptionPane optionPane = new JOptionPane(
            scoreBoard.getComponent(),
            JOptionPane.QUESTION_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION);

        final JDialog     dialog = new JDialog(
            Main.getGui().getFrame(),
            score.getRadix() + " parameters",
            true); // Modal flag
        dialog.setContentPane(optionPane);

        optionPane.addPropertyChangeListener(
            new PropertyChangeListener() {
                    public void propertyChange (PropertyChangeEvent e)
                    {
                        String prop = e.getPropertyName();

                        if (dialog.isVisible() &&
                            (e.getSource() == optionPane) &&
                            (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                            Object obj = optionPane.getValue();
                            int    value = ((Integer) obj).intValue();

                            apply[0] = value == JOptionPane.OK_OPTION;

                            // Exit only if user gives up or enters correct data
                            if (!apply[0]) {
                                dialog.setVisible(false);
                                dialog.dispose();
                            } else {
                                if (scoreBoard.commit()) {
                                    dialog.setVisible(false);
                                    dialog.dispose();
                                } else {
                                    // I'm ashamed! TBI
                                    try {
                                        optionPane.setValue(
                                            JOptionPane.UNINITIALIZED_VALUE);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    }
                });

        dialog.pack();
        dialog.setVisible(true);

        return apply[0];
    }

    //----------------------//
    // setDefaultParameters //
    //----------------------//
    private static boolean setDefaultParameters (Score score)
    {
        for (ScorePart scorePart : score.getPartList()) {
            // Part name
            scorePart.setName(scorePart.getDefaultName());

            // Part midi program
            scorePart.setMidiProgram(scorePart.getDefaultProgram());

            // Replicate the score tempo
            scorePart.setTempo(score.getDefaultTempo());
        }

        // Score global data
        score.setTempo(score.getDefaultTempo());
        score.setVelocity(score.getDefaultVelocity());

        return true;
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // BrowseAction //
    //--------------//
    /**
     * Class <code>BrowseAction</code> launches the tree display of the current
     * score.
     */
    @Plugin(type = SCORE_EDIT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class BrowseAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            JFrame frame = ScoreController.getCurrentScore()
                                          .viewScore();
            Main.getInstance().show(frame);
        }
    }

    //------------//
    // DumpAction //
    //------------//
    /**
     * Class <code>DumpAction</code> dumps the internals of a score to system
     * output
     *
     */
    @Plugin(type = SCORE_EDIT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class DumpAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            ScoreController.getCurrentScore()
                           .dump();
        }
    }

    //-------------//
    // ParamAction //
    //-------------//
    /**
     * Class <code>ParamAction</code> launches the dialog to set up score
     * parameters.
     */
    @Plugin(type = SCORE_EDIT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class ParamAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Score score = ScoreController.getCurrentScore();

            if (defineParameters(score)) {
                // Invalidate the midi sequence
                try {
                    MidiAgent agent = MidiAgent.getInstance();

                    if (agent.getScore() == score) {
                        agent.reset();
                    }
                } catch (UnavailableException ex) {
                    logger.warning("Cannot reset Midi sequence", ex);
                }
            }
        }
    }

    //---------------//
    // RebuildAction //
    //---------------//
    /**
     * Class <code>RebuildAction</code> re-translates all sheet glyphs to score
     * entities.
     */
    @Plugin(type = SCORE_EDIT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class RebuildAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground ()
                {
                    try {
                        SheetManager.getSelectedSheet()
                                    .getSheetSteps()
                                    .updateLastSteps(null, null);
                    } catch (Exception ex) {
                        logger.warning("Could not refresh score", ex);
                    }

                    return null;
                }
            };

            worker.execute();
        }
    }

    //-------------//
    // StoreAction //
    //-------------//
    /**
     * Class <code>StoreAction</code> handles the saving of the currently
     * selected score, using MusicXML format.
     */
    @Plugin(type = SCORE_EXPORT, dependency = SCORE_AVAILABLE, onToolbar = true)
    public static class StoreAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground ()
                {
                    Score score = ScoreController.getCurrentScore();

                    if (checkParameters(score)) {
                        try {
                            score.export();
                        } catch (Exception ex) {
                            logger.warning("Could not store " + score, ex);
                        }
                    }

                    return null;
                }
            };

            worker.execute();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean promptParameters = new Constant.Boolean(
            true,
            "Should we prompt the user for score parameters?");
    }
}
