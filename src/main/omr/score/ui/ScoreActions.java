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

import omr.score.Score;
import omr.score.entity.ScorePart;
import omr.score.midi.MidiAgent;

import omr.sheet.SheetManager;

import omr.ui.ScoreBoard;

import omr.util.BasicTask;
import omr.util.Logger;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

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
    extends ScoreDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreActions.class);

    /** Singleton */
    private static ScoreActions INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** Flag to allow automatic score rebuild on every user edition action */
    private boolean rebuildAllowed = true;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScoreActions //
    //--------------//
    /**
     * Creates a new ScoreActions object.
     */
    protected ScoreActions ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the singleton
     *
     * @return the unique instance of this class
     */
    public static synchronized ScoreActions getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new ScoreActions();
        }

        return INSTANCE;
    }

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
                return parametersAreConfirmed(score);
            } else {
                return setDefaultParameters(score);
            }
        } else {
            return true;
        }
    }

    //------------------------//
    // parametersAreConfirmed //
    //------------------------//
    /**
     * Prompts the user for interactive confirmation or modification of
     * score parameters
     *
     * @param score the provided score
     * @return true if parameters are accepted, false if not
     */
    public static boolean parametersAreConfirmed (Score score)
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

    //-------------------//
    // setRebuildAllowed //
    //-------------------//
    public void setRebuildAllowed (boolean value)
    {
        boolean oldValue = this.rebuildAllowed;
        this.rebuildAllowed = value;
        firePropertyChange("rebuildAllowed", oldValue, value);
    }

    //------------------//
    // isRebuildAllowed //
    //------------------//
    public boolean isRebuildAllowed ()
    {
        return rebuildAllowed;
    }

    //-------------//
    // browseScore //
    //-------------//
    /**
     * Launch the tree display of the current score.
     * @param e
     */
    @Action(enabledProperty = "scoreAvailable")
    public void browseScore (ActionEvent e)
    {
        Main.getInstance()
            .show(ScoreController.getCurrentScore().getBrowserFrame());
    }

    //------------------//
    // defineParameters //
    //------------------//
    /**
     * Launch the dialog to set up score parameters.
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = "scoreAvailable")
    public void defineParameters (ActionEvent e)
    {
        Score score = ScoreController.getCurrentScore();

        if (parametersAreConfirmed(score)) {
            // Invalidate the midi sequence
            try {
                MidiAgent agent = MidiAgent.getInstance();

                if (agent.getScore() == score) {
                    agent.reset();
                }
            } catch (Exception ex) {
                logger.warning("Cannot reset Midi sequence", ex);
            }
        }
    }

    //-----------//
    // dumpScore //
    //-----------//
    /**
     * Dump the internals of a score to system output
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = "scoreAvailable")
    public void dumpScore (ActionEvent e)
    {
        ScoreController.getCurrentScore()
                       .dump();
    }

    //--------------//
    // rebuildScore //
    //--------------//
    /**
     * Re-translate all sheet glyphs to score entities.
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = "scoreAvailable")
    public Task rebuildScore (ActionEvent e)
    {
        return new RebuildTask();
    }

    //------------//
    // storeScore //
    //------------//
    /**
     * Export the currently selected score, using MusicXML format
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = "scoreAvailable")
    public Task storeScore (ActionEvent e)
    {
        return new StoreTask();
    }

    //---------------//
    // toggleRebuild //
    //---------------//
    /**
     * Action that toggles thr rebuild of score on every user edition
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = "rebuildAllowed")
    public void toggleRebuild (ActionEvent e)
    {
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

    //-------------//
    // RebuildTask //
    //-------------//
    private static class RebuildTask
        extends BasicTask
    {
        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                SheetManager.getSelectedSheet()
                            .getSheetSteps()
                            .updateLastSteps(null, null, /* imposed => */
                                             true);
            } catch (Exception ex) {
                logger.warning("Could not refresh score", ex);
            }

            return null;
        }
    }

    //-----------//
    // StoreTask //
    //-----------//
    private static class StoreTask
        extends BasicTask
    {
        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
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
    }
}
