//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S c o r e A c t i o n s                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.Score;
import omr.score.entity.ScorePart;

import omr.script.Script;

import omr.sheet.Book;
import omr.sheet.BookManager;
import omr.sheet.Sheet;
import omr.sheet.ui.BookController;
import omr.sheet.ui.SheetsController;

import omr.step.ExportStep;
import omr.step.Stepping;
import omr.step.Steps;

import omr.ui.MainGui;
import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtil;

import omr.util.BasicTask;
import omr.util.FileUtil;
import omr.util.WrappedBoolean;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Path;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * Class {@code ScoreActions} gathers user actions related to scores
 *
 * @author Hervé Bitteur
 */
public class ScoreActions
        extends ScoreDependent
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ScoreActions.class);

    /** Should we rebuild the book on each user action */
    private static final String REBUILD_ALLOWED = "rebuildAllowed";

    /** Should we persist any manual assignment (for later training) */
    private static final String MANUAL_PERSISTED = "manualPersisted";

    /** Singleton */
    private static ScoreActions INSTANCE;

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Flag to allow automatic book rebuild on every user edition action */
    private boolean rebuildAllowed = true;

    /** Flag to indicate that manual assignments must be persisted */
    private boolean manualPersisted = false;

    //~ Constructors -------------------------------------------------------------------------------
    //
    //--------------//
    // ScoreActions //
    //--------------//
    /**
     * Creates a new ScoreActions object.
     */
    protected ScoreActions ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // checkParameters //
    //-----------------//
    /**
     * Make sure that the book parameters are properly set up, even by
     * prompting the user for them, otherwise return false
     *
     * @param sheet the provided sheet
     * @return true if OK, false otherwise
     */
    public static boolean checkParameters (Sheet sheet)
    {
        if (constants.promptParameters.getValue()) {
            return applyUserSettings(sheet);
        } else {
            return true; /////////////////////////////////////////////////////////////////////////////////////////////
            ///return fillParametersWithDefaults(sheet.getBook());
        }
    }

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

    //
    //-------------//
    // browseScore //
    //-------------//
    /**
     * Launch the tree display of the current book.
     *
     * @param e
     */
    @Action(enabledProperty = SCORE_AVAILABLE)
    public void browseScore (ActionEvent e)
    {
        MainGui.getInstance().show(BookController.getCurrentBook().getBrowserFrame());
    }

    //------------//
    // buildScore //
    //------------//
    /**
     * Translate all sheet glyphs to book entities, or rebuild the
     * sheet at end if SCORE step has already been reached.
     * Actually, it's just a convenient way to launch the SCORE step
     * or relaunch from the SYMBOLS step.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SCORE_IDLE)
    public Task<Void, Void> buildScore (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet.isDone(Steps.valueOf(Steps.SCORE))) {
            return new RebuildTask(sheet);
        } else {
            return new BuildTask(sheet);
        }
    }

    //------------------//
    // defineParameters //
    //------------------//
    /**
     * Launch the dialog to set up book parameters.
     *
     * @param e the event that triggered this action
     */
    @Action
    public void defineParameters (ActionEvent e)
    {
        applyUserSettings(SheetsController.getCurrentSheet());
    }

    //----------//
    // dumpBook //
    //----------//
    /**
     * Dump the internals of a book to system output
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SCORE_AVAILABLE)
    public void dumpBook (ActionEvent e)
    {
        logger.warn("dumpBook not yet implemented");

        ///BookController.getCurrentBook().dump();
    }

    /**
     * Dump the script of the sheet currently selected
     */
    @Action(enabledProperty = "sheetAvailable")
    public void dumpCurrentScript ()
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            Script script = sheet.getBook().getScript();

            if (script != null) {
                script.dump();
            }
        }
    }

    //-------------------//
    // isManualPersisted //
    //-------------------//
    public boolean isManualPersisted ()
    {
        return manualPersisted;
    }

    //------------------//
    // isRebuildAllowed //
    //------------------//
    public boolean isRebuildAllowed ()
    {
        return rebuildAllowed;
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
    // setRebuildAllowed //
    //-------------------//
    public void setRebuildAllowed (boolean value)
    {
        boolean oldValue = this.rebuildAllowed;
        this.rebuildAllowed = value;
        firePropertyChange(REBUILD_ALLOWED, oldValue, value);
    }

    //------------//
    // storeScore //
    //------------//
    /**
     * Export the currently selected book, using compressed or standard MusicXML format
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SCORE_AVAILABLE)
    public Task<Void, Void> storeScore (ActionEvent e)
    {
        final Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet == null) {
            return null;
        }

        final Path exportPath = sheet.getBook().getExportPath();

        if (exportPath != null) {
            return new StoreBookTask(sheet, exportPath);
        } else {
            return storeScoreAs(e);
        }
    }

    //--------------//
    // storeScoreAs //
    //--------------//
    /**
     * Export the currently selected book, using compressed or standard MusicXML format,
     * to a user-provided file
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SCORE_AVAILABLE)
    public Task<Void, Void> storeScoreAs (ActionEvent e)
    {
        final Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet == null) {
            return null;
        }

        // Let the user select a book output file
        final boolean compressed = ExportStep.useCompression();
        final OmrFileFilter filter = compressed
                ? new OmrFileFilter(
                        "MXL files",
                        new String[]{BookManager.COMPRESSED_SCORE_EXTENSION})
                : new OmrFileFilter(
                        "XML files",
                        new String[]{BookManager.SCORE_EXTENSION});
        File exportFile = UIUtil.fileChooser(
                true,
                null,
                BookManager.getInstance().getDefaultExportPath(sheet.getBook()).toFile(),
                filter);

        if (exportFile != null) {
            final Path filePath = exportFile.toPath();

            // Remove .xml or .mxl extension is any
            final Path exportPath;
            final String ext = FileUtil.getExtension(exportFile);

            if (ext.equalsIgnoreCase(BookManager.SCORE_EXTENSION)
                || ext.equalsIgnoreCase(BookManager.COMPRESSED_SCORE_EXTENSION)) {
                String filename = FileUtil.getNameSansExtension(filePath);
                exportPath = filePath.resolveSibling(filename);
            } else {
                exportPath = filePath;
            }

            return new StoreBookTask(sheet, exportPath);
        } else {
            return null;
        }
    }

    //---------------//
    // togglePersist //
    //---------------//
    /**
     * Action that toggles the persistency of manual assignments
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = MANUAL_PERSISTED)
    public void togglePersist (ActionEvent e)
    {
        logger.info("Persistency mode is {}", (isManualPersisted() ? "on" : "off"));
    }

    //---------------//
    // toggleRebuild //
    //---------------//
    /**
     * Action that toggles the rebuild of book on every user edition
     *
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
     * Write the currently selected book, as a PDF file
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SCORE_AVAILABLE)
    public Task<Void, Void> writeSheetPdf (ActionEvent e)
    {
        final Book book = BookController.getCurrentBook();

        if (book == null) {
            return null;
        }

        final Path pdfPath = book.getPrintPath();

        if (pdfPath != null) {
            return new WriteSheetPdfTask(book, pdfPath);
        } else {
            return writeSheetPdfAs(e);
        }
    }

    //-----------------//
    // writeSheetPdfAs //
    //-----------------//
    /**
     * Write the currently selected book, using PDF format,
     * to a user-provided file
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SCORE_AVAILABLE)
    public Task<Void, Void> writeSheetPdfAs (ActionEvent e)
    {
        final Book book = BookController.getCurrentBook();

        if (book == null) {
            return null;
        }

        // Let the user select a PDF output file
        File pdfFile = UIUtil.fileChooser(
                true,
                null,
                BookManager.getInstance().getDefaultPrintPath(book).toFile(),
                new OmrFileFilter("PDF files", new String[]{".pdf"}));

        if (pdfFile != null) {
            return new WriteSheetPdfTask(book, pdfFile.toPath());
        } else {
            return null;
        }
    }

    //-------------------//
    // applyUserSettings //
    //-------------------//
    /**
     * Prompts the user for interactive confirmation or modification of
     * book/page parameters
     *
     * @param sheet the current sheet, or null
     * @return true if parameters are applied, false otherwise
     */
    private static boolean applyUserSettings (final Sheet sheet)
    {
        try {
            final WrappedBoolean apply = new WrappedBoolean(false);
            final ScoreParameters scoreParams = new ScoreParameters(sheet);
            final JOptionPane optionPane = new JOptionPane(
                    scoreParams.getComponent(),
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
            final String frameTitle = (sheet != null)
                    ? (sheet.getBook().getRadix() + " parameters")
                    : "General parameters";
            final JDialog dialog = new JDialog(Main.getGui().getFrame(), frameTitle, true); // Modal flag
            dialog.setContentPane(optionPane);
            dialog.setName("scoreParams");

            optionPane.addPropertyChangeListener(
                    new PropertyChangeListener()
                    {
                        @Override
                        public void propertyChange (PropertyChangeEvent e)
                        {
                            String prop = e.getPropertyName();

                            if (dialog.isVisible()
                                && (e.getSource() == optionPane)
                                && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                                Object obj = optionPane.getValue();
                                int value = ((Integer) obj).intValue();
                                apply.set(value == JOptionPane.OK_OPTION);

                                // Exit only if user gives up or enters correct data
                                if (!apply.isSet() || scoreParams.commit(sheet)) {
                                    dialog.setVisible(false);
                                    dialog.dispose();
                                } else {
                                    // Incorrect data, so don't exit yet
                                    try {
                                        // TODO: Is there a more civilized way?
                                        optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    });

            dialog.pack();
            MainGui.getInstance().show(dialog);

            return apply.value;
        } catch (Exception ex) {
            logger.warn("Error in ScoreParameters", ex);

            return false;
        }
    }

    //----------------------------//
    // fillParametersWithDefaults //
    //----------------------------//
    /**
     * For some needed key parameters, fill them with default values if
     * they are not yet set.
     *
     * @param score the related book
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
        if (!score.hasVolume()) {
            score.setVolume(Score.getDefaultVolume());
        }

        return true;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------------//
    // WriteSheetPdfTask //
    //-------------------//
    public static class WriteSheetPdfTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Book book;

        final Path pdfPath;

        //~ Constructors ---------------------------------------------------------------------------
        public WriteSheetPdfTask (Book book,
                                  Path pdfPath)
        {
            this.book = book;
            this.pdfPath = pdfPath;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            Stepping.ensureBookStep(Steps.valueOf(Steps.SCORE), book);
            BookManager.getInstance().writePhysicalPdf(book, pdfPath);

            return null;
        }
    }

    //-----------//
    // BuildTask //
    //-----------//
    private static class BuildTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Sheet sheet;

        //~ Constructors ---------------------------------------------------------------------------
        public BuildTask (Sheet sheet)
        {
            this.sheet = sheet;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                Book book = sheet.getBook();
                Stepping.ensureBookStep(Steps.valueOf(Steps.SCORE), book);
            } catch (Exception ex) {
                logger.warn("Could not build score", ex);
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
        //~ Instance fields ------------------------------------------------------------------------

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
        //~ Instance fields ------------------------------------------------------------------------

        private final Sheet sheet;

        //~ Constructors ---------------------------------------------------------------------------
        public RebuildTask (Sheet sheet)
        {
            this.sheet = sheet;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                Stepping.reprocessSheet(Steps.valueOf(Steps.SYMBOLS), sheet, null, true);
            } catch (Exception ex) {
                logger.warn("Could not refresh score", ex);
            }

            return null;
        }
    }

    //---------------//
    // StoreBookTask //
    //---------------//
    private static class StoreBookTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Sheet sheet;

        final Book book;

        final Path exportPath;

        //~ Constructors ---------------------------------------------------------------------------
        public StoreBookTask (Sheet sheet,
                              Path exportPath)
        {
            this.sheet = sheet;
            this.book = sheet.getBook();
            this.exportPath = exportPath;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            book.setExportPath(exportPath);

            if (checkParameters(sheet)) {
                Stepping.ensureBookStep(Steps.valueOf(Steps.EXPORT), book);
            }

            return null;
        }
    }
}
