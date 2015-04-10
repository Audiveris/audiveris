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

import omr.OMR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.Score;
import omr.score.entity.LogicalPart;

import omr.script.Script;

import omr.sheet.Book;
import omr.sheet.BookManager;
import omr.sheet.Sheet;
import omr.sheet.ui.SheetDependent;
import omr.sheet.ui.SheetsController;

import omr.step.Step;

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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * Class {@code ScoreActions} gathers user actions related to scores
 *
 * @author Hervé Bitteur
 */
public class ScoreActions
        extends SheetDependent
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

    /** .mxl filter for user selection. */
    private static final OmrFileFilter MXL_FILTER = new OmrFileFilter(
            "MXL files",
            new String[]{OMR.COMPRESSED_SCORE_EXTENSION});

    /** .xml filter for user selection. */
    private static final OmrFileFilter XML_FILTER = new OmrFileFilter(
            "XML files",
            new String[]{OMR.SCORE_EXTENSION});

    /** .pdf filter for user selection. */
    private static final OmrFileFilter PDF_FILTER = new OmrFileFilter(
            "PDF files",
            new String[]{OMR.PDF_EXTENSION});

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Flag to allow automatic book rebuild on every user edition action */
    private boolean rebuildAllowed = true;

    /** Flag to indicate that manual assignments must be persisted */
    private boolean manualPersisted = false;

    //~ Constructors -------------------------------------------------------------------------------
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
        //        if (constants.promptParameters.getValue()) {
        //            return applyUserSettings(sheet);
        //        } else {
        //            return true; /////////////////////////////////////////////////////////////////////////////////////////////
        //            ///return fillParametersWithDefaults(sheet.getBook());
        //        }
        return true;
    }

    //-----------------//
    // checkParameters //
    //-----------------//
    /**
     * Make sure that the book parameters are properly set up, even by
     * prompting the user for them, otherwise return false
     *
     * @param book the provided book
     * @return true if OK, false otherwise
     */
    public static boolean checkParameters (Book book)
    {
        //        if (constants.promptParameters.getValue()) {
        //            return applyUserSettings(sheet);
        //        } else {
        //            return true; /////////////////////////////////////////////////////////////////////////////////////////////
        //            ///return fillParametersWithDefaults(sheet.getBook());
        //        }
        return true;
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

    //------------//
    // browseBook //
    //------------//
    /**
     * Launch the tree display of the current book.
     *
     * @param e
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void browseBook (ActionEvent e)
    {
        OMR.getApplication().show(SheetsController.getCurrentBook().getBrowserFrame());
    }

    //-----------//
    // buildBook //
    //-----------//
    /**
     * Launch or complete the transcription of all sheets and merge them at book level.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> buildBook (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();
        Book book = sheet.getBook();

        return new BuildBookTask(book);
    }

    //------------//
    // buildSheet //
    //------------//
    /**
     * Launch sheet transcription.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SHEET_IDLE)
    public Task<Void, Void> buildSheet (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet.isDone(Step.PAGE)) {
            return new RebuildTask(sheet);
        } else {
            return new BuildSheetTask(sheet);
        }
    }

    //-----------//
    // cleanBook //
    //-----------//
    /**
     * Delete the exported MusicXML for the whole current book.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void cleanBook (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();
        sheet.getBook().deleteExport();
    }

    //------------//
    // cleanSheet //
    //------------//
    /**
     * Delete the exported MusicXML for the current sheet.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void cleanSheet (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();
        sheet.deleteExport();
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
     * Dump the internals of a book to system output.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void dumpBook (ActionEvent e)
    {
        logger.error("dumpBook() is not yet implemented.");

        ///BookController.getCurrentBook().dump();
    }

    //-------------------//
    // dumpCurrentScript //
    //-------------------//
    /**
     * Dump the script of the sheet currently selected.
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

    //------------//
    // exportBook //
    //------------//
    /**
     * Export the currently selected book using MusicXML format
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> exportBook (ActionEvent e)
    {
        final Book book = SheetsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        final Path exportPath = book.getExportPath();

        if (exportPath != null) {
            return new ExportBookTask(book, exportPath);
        } else {
            return exportBookAs(e);
        }
    }

    //--------------//
    // exportBookAs //
    //--------------//
    /**
     * Export the currently selected book, using MusicXML format, to a user-provided
     * location.
     * <p>
     * NOTA: This action is disabled for any single-sheet book, see exportSheetAs() instead.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> exportBookAs (ActionEvent e)
    {
        final Book book = SheetsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        // Select book folder name to be used as radix (since this method assumes a multi-sheet book)
        final File defaultBookFile = BookManager.getDefaultExportPath(book).toFile();
        final String ext = BookManager.getExportExtension();
        String title = "Choose target book radix [no extension]";
        final File bookFile = UIUtil.directoryChooser(null, defaultBookFile, title);

        if (bookFile == null) {
            return null;
        }

        // Remove .mxl/.xml extension if any
        final Path bookPath = FileUtil.avoidExtension(bookFile.toPath(), ext);

        return new ExportBookTask(book, bookPath);
    }

    //-------------//
    // exportSheet //
    //-------------//
    /**
     * Export the currently selected sheet using MusicXML format.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SHEET_IDLE)
    public Task<Void, Void> exportSheet (ActionEvent e)
    {
        final Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet == null) {
            return null;
        }

        final Path exportPath = sheet.getBook().getExportPath();

        if (exportPath != null) {
            return new ExportSheetTask(sheet, exportPath);
        } else {
            return exportSheetAs(e);
        }
    }

    //---------------//
    // exportSheetAs //
    //---------------//
    /**
     * Export the currently selected sheet using MusicXML format, to a user-provided
     * location.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SHEET_IDLE)
    public Task<Void, Void> exportSheetAs (ActionEvent e)
    {
        final Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet == null) {
            return null;
        }

        // Let the user select book output
        final Book book = sheet.getBook();
        final File defaultBookFile = BookManager.getDefaultExportPath(book).toFile();
        final String ext = BookManager.getExportExtension();
        final File bookFile;

        if (book.isMultiSheet()) {
            // Select book folder name (to be used as radix)
            String title = "Choose target book radix [no extension]";
            bookFile = UIUtil.directoryChooser(null, defaultBookFile, title);
        } else {
            // Select book file name
            final OmrFileFilter filter = BookManager.useCompression() ? MXL_FILTER : XML_FILTER;
            final String title = "Choose target book radix [" + ext
                                 + " extension is optional]";
            bookFile = UIUtil.fileChooser(true, null, defaultBookFile, filter, title);
        }

        if (bookFile == null) {
            return null;
        }

        // Remove .mxl/.xml extension if any
        final Path bookPath = FileUtil.avoidExtension(bookFile.toPath(), ext);

        // Make sure book folder is created
        if (book.isMultiSheet() && !checkBookFolder(bookPath)) {
            return null;
        }

        return new ExportSheetTask(sheet, bookPath);
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

    //-----------//
    // printBook //
    //-----------//
    /**
     * Write the currently selected book, as a PDF file
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> printBook (ActionEvent e)
    {
        final Book book = SheetsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        final Path bookPrintPath = book.getPrintPath();

        if (bookPrintPath != null) {
            return new PrintBookTask(book, bookPrintPath);
        } else {
            return printBookAs(e);
        }
    }

    //-------------//
    // printBookAs //
    //-------------//
    /**
     * Write the currently selected book, using PDF format, to a user-provided file.
     * <p>
     * NOTA: This action is disabled for any single-sheet book, see printSheetAs() instead.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> printBookAs (ActionEvent e)
    {
        final Book book = SheetsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        // Select book folder name (to be used as radix)
        // (since this method assumes a multi-sheet book)
        final File defaultBookFile = BookManager.getDefaultPrintPath(book).toFile();
        String title = "Choose target book radix [no extension]";
        final File bookFile = UIUtil.directoryChooser(null, defaultBookFile, title);

        if (bookFile == null) {
            return null;
        }

        // Remove .pdf extension if any
        final Path bookPath = FileUtil.avoidExtension(bookFile.toPath(), OMR.PDF_EXTENSION);

        return new PrintBookTask(book, bookPath);
    }

    //------------//
    // printSheet //
    //------------//
    /**
     * Write the currently selected sheet, as a PDF file
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SHEET_IDLE)
    public Task<Void, Void> printSheet (ActionEvent e)
    {
        final Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet == null) {
            return null;
        }

        final Book book = sheet.getBook();
        final Path bookPrintPath = book.getPrintPath();

        if (bookPrintPath != null) {
            return new PrintSheetTask(sheet, bookPrintPath);
        } else {
            return printSheetAs(e);
        }
    }

    //--------------//
    // printSheetAs //
    //--------------//
    /**
     * Write the currently selected sheet, using PDF format, to a user-provided location.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = SHEET_IDLE)
    public Task<Void, Void> printSheetAs (ActionEvent e)
    {
        final Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet == null) {
            return null;
        }

        // Let the user select a PDF output file
        final Book book = sheet.getBook();
        final File defaultBookFile = BookManager.getDefaultPrintPath(book).toFile();
        final File bookFile;

        if (book.isMultiSheet()) {
            // Select book folder name (to be used as radix)
            String title = "Choose target book radix [no extension]";
            bookFile = UIUtil.directoryChooser(null, defaultBookFile, title);
        } else {
            // Select book file name
            String title = "Choose target book radix [.pdf extension is optional]";
            bookFile = UIUtil.fileChooser(true, null, defaultBookFile, PDF_FILTER, title);
        }

        if (bookFile == null) {
            return null;
        }

        // Remove .pdf extension if any
        final Path bookPath = FileUtil.avoidExtension(bookFile.toPath(), OMR.PDF_EXTENSION);

        // Make sure book folder is created
        if (book.isMultiSheet() && !checkBookFolder(bookPath)) {
            return null;
        }

        return new PrintSheetTask(sheet, bookPath);
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
            final JDialog dialog = new JDialog(OMR.getGui().getFrame(), frameTitle, true); // Modal flag
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
            OMR.getApplication().show(dialog);

            return apply.value;
        } catch (Exception ex) {
            logger.warn("Error in ScoreParameters", ex);

            return false;
        }
    }

    private boolean checkBookFolder (Path bookPath)
    {
        // Make sure book folder is created
        try {
            if (!Files.exists(bookPath)) {
                Files.createDirectories(bookPath);
            }
        } catch (IOException ex) {
            logger.warn("Could not create folder " + bookPath, ex);

            return false;
        }

        return true;
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
        if (score.getLogicalParts() != null) {
            for (LogicalPart logicalPart : score.getLogicalParts()) {
                // Part name
                if (logicalPart.getName() == null) {
                    logicalPart.setName(logicalPart.getDefaultName());
                }

                // Part midi program
                if (logicalPart.getMidiProgram() == null) {
                    logicalPart.setMidiProgram(logicalPart.getDefaultProgram());
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
    //---------------//
    // PrintBookTask //
    //---------------//
    public static class PrintBookTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Book book;

        final Path bookPrintPath;

        //~ Constructors ---------------------------------------------------------------------------
        public PrintBookTask (Book book,
                              Path bookPrintPath)
        {
            this.book = book;
            this.bookPrintPath = bookPrintPath;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            book.setPrintPath(bookPrintPath);
            //
            //            for (Sheet sheet : book.getSheets()) {
            //                sheet.ensureStep(Step.PAGE);
            //            }
            //
            book.print();

            return null;
        }
    }

    //----------------//
    // PrintSheetTask //
    //----------------//
    public static class PrintSheetTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Sheet sheet;

        final Path bookPrintPath;

        //~ Constructors ---------------------------------------------------------------------------
        public PrintSheetTask (Sheet sheet,
                               Path bookPrintPath)
        {
            this.sheet = sheet;
            this.bookPrintPath = bookPrintPath;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            sheet.getBook().setPrintPath(bookPrintPath);
            sheet.ensureStep(Step.PAGE);
            sheet.print();

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

    //---------------//
    // BuildBookTask //
    //---------------//
    private static class BuildBookTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Book book;

        //~ Constructors ---------------------------------------------------------------------------
        public BuildBookTask (Book book)
        {
            this.book = book;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                for (Sheet sheet : book.getSheets()) {
                    sheet.ensureStep(Step.PAGE);
                }
            } catch (Exception ex) {
                logger.warn("Could not build book", ex);
            }

            return null;
        }
    }

    //----------------//
    // BuildSheetTask //
    //----------------//
    private static class BuildSheetTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Sheet sheet;

        //~ Constructors ---------------------------------------------------------------------------
        public BuildSheetTask (Sheet sheet)
        {
            this.sheet = sheet;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                sheet.ensureStep(Step.PAGE);
            } catch (Exception ex) {
                logger.warn("Could not build page", ex);
            }

            return null;
        }
    }

    //----------------//
    // ExportBookTask //
    //----------------//
    private static class ExportBookTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Book book;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create an asynchronous task to export the book.
         *
         * @param book           the book to export
         * @param bookExportPath (non-null) the target export book path
         */
        public ExportBookTask (Book book,
                               Path bookExportPath)
        {
            this.book = book;
            book.setExportPath(bookExportPath);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            if (checkParameters(book)) {
                book.export();
            }

            return null;
        }
    }

    //-----------------//
    // ExportSheetTask //
    //-----------------//
    private static class ExportSheetTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Sheet sheet;

        final Path bookExportPath;

        //~ Constructors ---------------------------------------------------------------------------
        public ExportSheetTask (Sheet sheet,
                                Path bookExportPath)
        {
            this.sheet = sheet;
            this.bookExportPath = bookExportPath;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            sheet.getBook().setExportPath(bookExportPath);

            if (checkParameters(sheet)) {
                sheet.ensureStep(Step.PAGE);
                sheet.export();
            }

            return null;
        }
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
            // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
            //            try {
            //                Stepping.reprocessSheet(Step.SYMBOLS, sheet, null, true);
            //            } catch (Exception ex) {
            //                logger.warn("Could not refresh score", ex);
            //            }
            //
            return null;
        }
    }
}
