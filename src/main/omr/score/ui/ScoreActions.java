//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e A c t i o n s                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ScoresManager;
import omr.score.entity.ScorePart;
import omr.score.midi.MidiAgent;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

import omr.step.Stepping;
import omr.step.Steps;

import omr.ui.MainGui;
import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtilities;

import omr.util.BasicTask;
import omr.util.Implement;
import omr.util.WrappedBoolean;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * Class <code>ScoreActions</code> gathers user actions related to scores
 *
 * @author Hervé Bitteur
 */
public class ScoreActions
    extends ScoreDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreActions.class);

    /** Should we rebuild the score on each user action */
    private static final String REBUILD_ALLOWED = "rebuildAllowed";

    /** Should we persist any manual assignment (for later training) */
    private static final String MANUAL_PERSISTED = "manualPersisted";

    /** Singleton */
    private static ScoreActions INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** Flag to allow automatic score rebuild on every user edition action */
    private boolean rebuildAllowed = true;

    /** Flag to indicate that manual assignments must be persisted */
    private boolean manualPersisted = false;

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
        if (constants.promptParameters.getValue()) {
            return parametersAreConfirmed(score);
        } else {
            return fillParametersWithDefaults(score);
        }
    }

    //--------------------//
    // setManualPersisted //
    //--------------------//
    public void setManualPersisted (boolean value)
    {
        boolean oldValue = this.manualPersisted;
        this.manualPersisted = value;
        firePropertyChange(MANUAL_PERSISTED, oldValue, value);
    }

    //-------------------//
    // isManualPersisted //
    //-------------------//
    public boolean isManualPersisted ()
    {
        return manualPersisted;
    }

    //-------------------//
    // setRebuildAllowed //
    //-------------------//
    public void setRebuildAllowed (boolean value)
    {
        boolean oldValue = this.rebuildAllowed;
        this.rebuildAllowed = value;
        firePropertyChange(REBUILD_ALLOWED, oldValue, value);
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
    @Action(enabledProperty = SCORE_AVAILABLE)
    public void browseScore (ActionEvent e)
    {
        MainGui.getInstance()
               .show(ScoreController.getCurrentScore().getBrowserFrame());
    }

    //------------------//
    // defineParameters //
    //------------------//
    /**
     * Launch the dialog to set up score parameters.
     * @param e the event that triggered this action
     */
    @Action
    public void defineParameters (ActionEvent e)
    {
        Score score = ScoreController.getCurrentScore();

        if (parametersAreConfirmed(score)) {
            // Invalidate the midi sequence
            try {
                MidiAgent agent = MidiAgent.getInstance();

                if ((agent.getScore() == score) && (score != null)) {
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
    @Action(enabledProperty = SCORE_AVAILABLE)
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
    @Action(enabledProperty = SCORE_AVAILABLE)
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
    @Action(enabledProperty = SCORE_MERGED)
    public Task storeScore (ActionEvent e)
    {
        final Score score = ScoreController.getCurrentScore();

        if (score == null) {
            return null;
        }

        final File exportFile = score.getExportFile();

        if (exportFile != null) {
            return new StoreScoreTask(score, exportFile);
        } else {
            return storeScoreAs(e);
        }
    }

    //--------------//
    // storeScoreAs //
    //--------------//
    /**
     * Export the currently selected score, using MusicXML format,
     * to a user-provided file
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SCORE_MERGED)
    public Task storeScoreAs (ActionEvent e)
    {
        final Score score = ScoreController.getCurrentScore();

        if (score == null) {
            return null;
        }

        // Let the user select a score output file
        File exportFile = UIUtilities.fileChooser(
            true,
            null,
            ScoresManager.getInstance().getDefaultExportFile(score),
            new OmrFileFilter(
                "XML files",
                new String[] { ScoresManager.SCORE_EXTENSION }));

        if (exportFile != null) {
            return new StoreScoreTask(score, exportFile);
        } else {
            return null;
        }
    }

    //---------------//
    // togglePersist //
    //---------------//
    /**
     * Action that toggles the persistency of manual assignments
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = MANUAL_PERSISTED)
    public void togglePersist (ActionEvent e)
    {
        logger.info(
            "Persistency mode is " + (isManualPersisted() ? "on" : "off"));
    }

    //---------------//
    // toggleRebuild //
    //---------------//
    /**
     * Action that toggles the rebuild of score on every user edition
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = REBUILD_ALLOWED)
    public void toggleRebuild (ActionEvent e)
    {
    }

    //------------------//
    // writePhysicalPdf //
    //------------------//
    /**
     * Write the currently selected score, as a PDF file
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SCORE_AVAILABLE)
    public Task writeSheetPdf (ActionEvent e)
    {
        final Score score = ScoreController.getCurrentScore();

        if (score == null) {
            return null;
        }

        final File pdfFile = score.getSheetPdfFile();

        if (pdfFile != null) {
            return new WriteSheetPdfTask(score, pdfFile);
        } else {
            return writeSheetPdfAs(e);
        }
    }

    //-----------------//
    // writeSheetPdfAs //
    //-----------------//
    /**
     * Write the currently selected score, using PDF format,
     * to a user-provided file
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SCORE_AVAILABLE)
    public Task writeSheetPdfAs (ActionEvent e)
    {
        final Score score = ScoreController.getCurrentScore();

        if (score == null) {
            return null;
        }

        // Let the user select a PDF output file
        File pdfFile = UIUtilities.fileChooser(
            true,
            null,
            ScoresManager.getInstance().getDefaultSheetPdfFile(score),
            new OmrFileFilter("PDF files", new String[] { ".pdf" }));

        if (pdfFile != null) {
            return new WriteSheetPdfTask(score, pdfFile);
        } else {
            return null;
        }
    }

    //----------------------------//
    // fillParametersWithDefaults //
    //----------------------------//
    /**
     * For some needed key parameters, fill them with default values if they are
     * not yet set.
     * @param score the related score
     * @return true
     */
    private static boolean fillParametersWithDefaults (Score score)
    {
        if (score.getPartList() != null) {
            for (ScorePart scorePart : score.getPartList()) {
                // Part name
                if (scorePart.getName() == null) {
                    scorePart.setName(scorePart.getDefaultName());
                }

                // Part midi program
                if (scorePart.getMidiProgram() == null) {
                    scorePart.setMidiProgram(scorePart.getDefaultProgram());
                }
            }
        }

        // Score global data
        if (!score.hasTempo()) {
            score.setTempo(Score.getDefaultTempo());
        }

        if (!score.hasVolume()) {
            score.setVolume(Score.getDefaultVolume());
        }

        return true;
    }

    //------------------------//
    // parametersAreConfirmed //
    //------------------------//
    /**
     * Prompts the user for interactive confirmation or modification of
     * score parameters
     *
     * @param score the provided score
     * @return true if parameters are applied, false otherwise
     */
    private static boolean parametersAreConfirmed (final Score score)
    {
        final WrappedBoolean  apply = new WrappedBoolean(false);
        final ScoreParameters scoreBoard = new ScoreParameters(score);
        final JOptionPane     optionPane = new JOptionPane(
            scoreBoard.getComponent(),
            JOptionPane.QUESTION_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION);
        final String          frameTitle = (score != null)
                                           ? (score.getRadix() + " parameters")
                                           : "Score parameters";
        final JDialog         dialog = new JDialog(
            Main.getGui().getFrame(),
            frameTitle,
            true); // Modal flag
        dialog.setContentPane(optionPane);
        dialog.setName("scoreBoard");

        optionPane.addPropertyChangeListener(
            new PropertyChangeListener() {
                    @Implement(PropertyChangeListener.class)
                    public void propertyChange (PropertyChangeEvent e)
                    {
                        String prop = e.getPropertyName();

                        if (dialog.isVisible() &&
                            (e.getSource() == optionPane) &&
                            (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                            Object obj = optionPane.getValue();
                            int    value = ((Integer) obj).intValue();
                            apply.set(value == JOptionPane.OK_OPTION);

                            Sheet sheet = (score != null)
                                          ? score.getFirstPage()
                                                 .getSheet() : null;

                            // Exit only if user gives up or enters correct data
                            if (!apply.isSet() || scoreBoard.commit(sheet)) {
                                dialog.setVisible(false);
                                dialog.dispose();
                            } else {
                                // Incorrect data, so don't exit yet
                                try {
                                    // TODO: Is there a more civilized way?
                                    optionPane.setValue(
                                        JOptionPane.UNINITIALIZED_VALUE);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                });

        dialog.pack();
        MainGui.getInstance()
               .show(dialog);

        return apply.value;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------------------//
    // WriteSheetPdfTask //
    //-------------------//
    public static class WriteSheetPdfTask
        extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        final Score score;
        final File  pdfFile;

        //~ Constructors -------------------------------------------------------

        public WriteSheetPdfTask (Score score,
                                  File  pdfFile)
        {
            this.score = score;
            this.pdfFile = pdfFile;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            ScoresManager.getInstance()
                         .writePhysicalPdf(score, pdfFile);

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

        Constant.Boolean promptParameters = new Constant.Boolean(
            false,
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
                Sheet sheet = SheetsController.getCurrentSheet();
                Stepping.reprocessSheet(
                    Steps.valueOf(Steps.VERTICALS),
                    sheet,
                    null,
                    true);
            } catch (Exception ex) {
                logger.warning("Could not refresh score", ex);
            }

            return null;
        }
    }

    //----------------//
    // StoreScoreTask //
    //----------------//
    private static class StoreScoreTask
        extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        final Score score;
        final File  exportFile;

        //~ Constructors -------------------------------------------------------

        public StoreScoreTask (Score score,
                               File  exportFile)
        {
            this.score = score;
            this.exportFile = exportFile;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            if (checkParameters(score)) {
                ScoresManager.getInstance()
                             .export(score, exportFile, null);
            }

            return null;
        }
    }
}
