//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e A c t i o n s                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.Main;
import static omr.plugin.Dependency.*;
import omr.plugin.Plugin;
import static omr.plugin.PluginType.*;

import omr.sheet.SheetManager;

import omr.step.Step;

import omr.ui.ScoreBoard;
import omr.ui.util.SwingWorker;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.event.*;
import java.beans.*;

import javax.swing.*;

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
    ///    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreActions.class);

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // checkParameters //
    //-----------------//
    public static void checkParameters (Score score)
    {
        if (score.getTempo() == null) {
            defineParameters(score);
        }
    }

    //------------------//
    // defineParameters //
    //------------------//
    public static void defineParameters (Score score)
    {
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

                            if (value == JOptionPane.OK_OPTION) {
                                scoreBoard.commit();
                            }

                            dialog.setVisible(false);
                            dialog.dispose();
                        }
                    }
                });
        dialog.pack();
        dialog.setVisible(true);
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

        @Override
        public void actionPerformed (ActionEvent e)
        {
            ScoreController.getCurrentScore()
                           .viewScore();
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

        @Override
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

        @Override
        public void actionPerformed (ActionEvent e)
        {
            defineParameters(ScoreController.getCurrentScore());
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
            final SwingWorker worker = new SwingWorker() {
                public Object construct ()
                {
                    try {
                        SheetManager.getSelectedSheet()
                                    .getSheetSteps()
                                    .doit(Step.SCORE);
                    } catch (Exception ex) {
                        logger.warning("Could not refresh score", ex);
                    }

                    return null;
                }
            };

            worker.start();
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

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final SwingWorker worker = new SwingWorker() {
                public Object construct ()
                {
                    Score score = ScoreController.getCurrentScore();

                    checkParameters(score);

                    try {
                        score.export();
                    } catch (Exception ex) {
                        logger.warning("Could not store " + score, ex);
                    }

                    return null;
                }
            };

            worker.start();
        }
    }
}
