//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B o o k A c t i o n s                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.OMR;
import omr.WellKnowns;

import omr.classifier.SampleRepository;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.plugin.Plugin;
import omr.plugin.PluginManager;

import omr.score.ui.ScoreParameters;

import omr.script.InvalidateTask;
import omr.script.ResetTask;
import omr.script.Script;
import omr.script.ScriptManager;

import omr.sheet.BasicSheet;
import omr.sheet.Book;
import omr.sheet.BookManager;
import omr.sheet.ExportPattern;
import omr.sheet.Picture;
import omr.sheet.ScaleBuilder;
import omr.sheet.Sheet;
import omr.sheet.SheetStub;
import omr.sheet.Staff;
import omr.sheet.StaffManager;
import omr.sheet.grid.StaffProjector;
import omr.sheet.stem.StemScaler;

import static omr.sheet.ui.StubDependent.BOOK_IDLE;
import static omr.sheet.ui.StubDependent.STUB_AVAILABLE;
import static omr.sheet.ui.StubDependent.STUB_IDLE;

import omr.step.Step;

import omr.ui.BoardsPane;
import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtil;
import omr.ui.view.HistoryMenu;

import omr.util.BasicTask;
import omr.util.FileUtil;
import omr.util.Param;
import omr.util.PathTask;
import omr.util.WrappedBoolean;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.xml.bind.JAXBException;

/**
 * Class {@code BookActions} gathers all UI actions related to current book.
 *
 * @author Hervé Bitteur
 */
public class BookActions
        extends StubDependent
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BookActions.class);

    /** Singleton. */
    private static BookActions INSTANCE;

    /** Should we rebuild the book on each user action. */
    private static final String REBUILD_ALLOWED = "rebuildAllowed";

    /** Should we persist any manual assignment (for later training). */
    private static final String MANUAL_PERSISTED = "manualPersisted";

    /** Default parameter. */
    public static final Param<Boolean> defaultPrompt = new Default();

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Flag to allow automatic book rebuild on every user edition action. */
    private boolean rebuildAllowed = true;

    /** Flag to indicate that manual assignments must be persisted. */
    private boolean manualPersisted = false;

    /** Sub-menu on inputs history. */
    private final HistoryMenu inputHistoryMenu;

    /** Sub-menu on projects history. */
    private final HistoryMenu projectHistoryMenu;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BookActions object.
     */
    private BookActions ()
    {
        final BookManager mgr = BookManager.getInstance();
        inputHistoryMenu = new HistoryMenu(mgr.getInputHistory(), OpenTask.class);
        projectHistoryMenu = new HistoryMenu(mgr.getProjectHistory(), OpenProjectTask.class);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // browseBook //
    //------------//
    /**
     * Launch the tree display of the current book.
     *
     * @param e
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void browseBook (ActionEvent e)
    {
        OMR.getApplication().show(StubsController.getCurrentBook().getBrowserFrame());
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
        Book book = StubsController.getCurrentStub().getBook();

        // Check if (valid) sheets still need to be processed
        int todo = 0;

        for (SheetStub stub : book.getValidStubs()) {
            if (!stub.isDone(Step.PAGE)) {
                todo++;
            }
        }

        if (todo > 0) {
            return new BuildBookTask(book);
        } else {
            logger.info("No sheet transcription needed");

            return null;
        }
    }

    //-------------//
    // buildScores //
    //-------------//
    /**
     * Build all scores at book level.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> buildScores (ActionEvent e)
    {
        Book book = StubsController.getCurrentStub().getBook();

        return new BuildScoresTask(book);
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
    @Action(enabledProperty = STUB_IDLE)
    public Task<Void, Void> buildSheet (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub.isDone(Step.PAGE)) {
            return new RebuildTask(stub.getSheet());
        } else {
            return new BuildSheetTask(stub.getSheet());
        }
    }

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
    // checkStored //
    //-------------//
    /**
     * Check whether the provided script has been safely saved if needed
     * (and therefore, if the sheet can be closed)
     *
     * @param script the script to check
     * @return true if close is allowed, false if not
     */
    public static boolean checkStored (Script script)
    {
        if (script.isModified() && defaultPrompt.getSpecific()) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.getGui().getFrame(),
                    "Save script for book " + script.getBook().getRadix() + "?");

            if (answer == JOptionPane.YES_OPTION) {
                Task<Void, Void> task = getInstance().storeScript(null);

                if (task != null) {
                    task.execute();
                }

                // Here user has saved the script
                return true;
            }

            if (answer == JOptionPane.NO_OPTION) {
                // Here user specifically chooses NOT to save the script
                return true;
            }

            // // Here user says Oops!, cancelling the current close request
            return false;
        } else {
            return true;
        }
    }

    //-------------//
    // checkStored //
    //-------------//
    /**
     * Check whether the provided book has been safely saved if needed
     * (and therefore, if it can be closed)
     *
     * @param book the book to check
     * @return true if close is allowed, false if not
     */
    public static boolean checkStored (Book book)
    {
        if (book.isModified() && defaultPrompt.getSpecific()) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.getGui().getFrame(),
                    "Save modified book " + book.getRadix() + "?");

            if (answer == JOptionPane.YES_OPTION) {
                Path projectPath;

                if (book.getProjectPath() == null) {
                    // Find a suitable target file
                    projectPath = BookManager.getDefaultProjectPath(book);

                    // Check the target is fine
                    if (!confirmed(projectPath)) {
                        // Let the user select an alternate output file
                        projectPath = selectProjectPath(
                                true,
                                BookManager.getDefaultProjectPath(book));

                        if (!confirmed(projectPath)) {
                            return false; // No suitable target found
                        }
                    }
                } else {
                    projectPath = book.getProjectPath();
                }

                try {
                    // Save the project to target file
                    book.store(projectPath);

                    return true; // Project successfully saved
                } catch (Exception ex) {
                    logger.warn("Error saving book", ex);

                    return false; // Saving failed
                }
            }

            // Check whether user specifically chose NOT to save the book
            return answer == JOptionPane.NO_OPTION;
        } else {
            return true;
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
    @Action(enabledProperty = STUB_AVAILABLE)
    public void cleanBook (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();
        stub.getBook().deleteExport();
    }

    //------------//
    // cleanSheet //
    //------------//
    /**
     * Delete the exported MusicXML for the current sheet.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void cleanSheet (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();
        logger.warn("Check implementation of cleanSheet");

        ///stub.deleteExport();
    }

    //-----------//
    // closeBook //
    //-----------//
    /**
     * Action that handles the closing of the currently selected book.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void closeBook (ActionEvent e)
    {
        Book book = StubsController.getCurrentBook();

        if (book != null) {
            if (checkStored(book)) {
                book.close();
            }
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
        applyUserSettings(StubsController.getCurrentStub());
    }

    //-------------//
    // displayData //
    //-------------//
    /**
     * Action that allows to display the view on image or binary table
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void displayData (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if ((stub != null) && stub.isDone(Step.GRID)) {
            stub.getSheet().displayDataTab();
        }
    }

    //----------------//
    // displayNoStaff //
    //----------------//
    /**
     * Action that allows to display the view on no-staff buffer
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void displayNoStaff (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub != null) {
            SheetAssembly assembly = stub.getAssembly();

            if (assembly.getPane(SheetTab.NO_STAFF_TAB.label) == null) {
                Sheet sheet = stub.getSheet(); // This may load the sheet...
                assembly.addViewTab(
                        SheetTab.NO_STAFF_TAB,
                        new ScrollImageView(
                                sheet,
                                new ImageView(
                                        sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF)
                                        .getBufferedImage())),
                        new BoardsPane(new PixelBoard(sheet)));
            }
        }
    }

    //----------------//
    // displayPicture //
    //----------------//
    /**
     * Action that allows to display the view on image (or binary table)
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void displayPicture (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();
        ((BasicSheet) stub.getSheet()).createPictureView();
    }

    //----------//
    // dumpBook //
    //----------//
    /**
     * Dump the internals of a book to system output.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
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
    @Action(enabledProperty = STUB_AVAILABLE)
    public void dumpCurrentScript ()
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub != null) {
            Script script = stub.getBook().getScript();

            if (script != null) {
                script.dump();
            }
        }
    }

    //-------------------//
    // dumpEventServices //
    //-------------------//
    /**
     * Action to erase the dump the content of all event services
     *
     * @param e the event which triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void dumpEventServices (ActionEvent e)
    {
        StubsController.getInstance().dumpCurrentSheetServices();
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
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        final Path exportPathSsansExt = book.getExportPathSansExt();

        if (exportPathSsansExt != null) {
            return new ExportBookTask(book, exportPathSsansExt);
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
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> exportBookAs (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null; // Not likely to happen, but safer
        }

        // Let user select book export target
        final String ext = BookManager.getExportExtension();
        final Path sansExt = BookManager.getDefaultExportPathSansExt(book);
        final Path targetPath = Paths.get(sansExt + ext);
        final Path bookPath = UIUtil.pathChooser(
                true,
                OMR.getGui().getFrame(),
                targetPath,
                filter(ext),
                "Choose book export target");

        if ((bookPath == null) || !confirmed(bookPath)) {
            return null;
        }

        // Remove extensions if any (.opus.mxl, .mxl, .xml, .mvt#.mxl, .mvt#.xml)
        final Path bookPathSansExt = ExportPattern.getPathSansExt(bookPath);

        return new ExportBookTask(book, bookPathSansExt);
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
    @Action(enabledProperty = STUB_IDLE)
    public Task<Void, Void> exportSheetAs (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return null; // Not likely to happen, but safer
        }

        // Let user select sheet export target
        final Book book = stub.getBook();
        final Path bookSansExt = BookManager.getDefaultExportPathSansExt(book);
        final boolean compressed = BookManager.useCompression();
        final String ext = compressed ? OMR.COMPRESSED_SCORE_EXTENSION : OMR.SCORE_EXTENSION;
        final String suffix = book.isMultiSheet() ? (OMR.SHEET_SUFFIX + stub.getNumber()) : "";
        final Path defaultSheetPath = Paths.get(bookSansExt + suffix + ext);
        final Path sheetPath = UIUtil.pathChooser(
                true,
                OMR.getGui().getFrame(),
                defaultSheetPath,
                filter(ext),
                "Choose sheet export target");

        if ((sheetPath == null) || !confirmed(sheetPath)) {
            return null;
        }

        // Remove .mxl/.xml extension if any
        final Path sheetPathSansExt = ExportPattern.getPathSansExt(sheetPath);

        return new ExportSheetTask(stub.getSheet(), sheetPathSansExt);
    }

    //---------------------//
    // getInputHistoryMenu //
    //---------------------//
    public HistoryMenu getInputHistoryMenu ()
    {
        return inputHistoryMenu;
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the singleton
     *
     * @return the unique instance of this class
     */
    public static synchronized BookActions getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new BookActions();
        }

        return INSTANCE;
    }

    //-----------------------//
    // getProjectHistoryMenu //
    //-----------------------//
    public HistoryMenu getProjectHistoryMenu ()
    {
        return projectHistoryMenu;
    }

    //-----------------//
    // invalidateSheet //
    //-----------------//
    /**
     * Action that flags the currently selected sheet as invalid.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_VALID)
    public void invalidateSheet (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub != null) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.getGui().getFrame(),
                    "Do you confirm sheet " + stub.getId() + " is invalid?");

            if (answer == JOptionPane.YES_OPTION) {
                final Sheet sheet = stub.getSheet();
                new InvalidateTask(sheet).launch(sheet);
            }
        }
    }

    //---------------------//
    // invokeDefaultPlugin //
    //---------------------//
    /**
     * Action to invoke the default score external editor
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_IDLE)
    public Task<Void, Void> invokeDefaultPlugin (ActionEvent e)
    {
        Plugin defaultPlugin = PluginManager.getInstance().getDefaultPlugin();

        if (defaultPlugin == null) {
            logger.warn("No default plugin defined");

            return null;
        }

        // Current score export file
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        } else {
            return defaultPlugin.getTask(book);
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

    //------------//
    // loadScript //
    //------------//
    @Action
    public Task<Void, Void> loadScript (ActionEvent e)
    {
        final Path path = UIUtil.pathChooser(
                false,
                OMR.getGui().getFrame(),
                Paths.get(constants.defaultScriptDirectory.getValue()),
                new OmrFileFilter("Score script files", new String[]{OMR.SCRIPT_EXTENSION}));

        if (path != null) {
            return new LoadScriptTask(path);
        } else {
            return null;
        }
    }

    //----------//
    // openBook //
    //----------//
    /**
     * Action that let the user select a book project.
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public OpenProjectTask openBook (ActionEvent e)
    {
        final String dir = BookManager.getDefaultProjectFolder();
        final Path path = selectProjectPath(false, Paths.get(dir));

        if (path != null) {
            if (Files.exists(path)) {
                return new OpenProjectTask(path);
            } else {
                logger.warn("Path not found {}", path);
            }
        }

        return null;
    }

    //---------------//
    // openImageFile //
    //---------------//
    /**
     * Action that let the user select an image file interactively.
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public OpenTask openImageFile (ActionEvent e)
    {
        String suffixes = constants.validImageExtensions.getValue();
        String allSuffixes = suffixes + " " + suffixes.toUpperCase();
        Path path = UIUtil.pathChooser(
                false,
                OMR.getGui().getFrame(),
                Paths.get(BookManager.getDefaultInputFolder()),
                new OmrFileFilter(
                        "Major image files" + " (" + suffixes + ")",
                        allSuffixes.split("\\s")));

        if (path != null) {
            if (Files.exists(path)) {
                return new OpenTask(path);
            } else {
                logger.warn("File not found {}", path);
            }
        }

        return null;
    }

    //-----------//
    // plotScale //
    //-----------//
    /**
     * Action that allows to display the plot of Scale Builder.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void plotScale (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub != null) {
            if (stub.isDone(Step.SCALE)) {
                new ScaleBuilder(stub.getSheet()).displayChart();
            } else {
                logger.warn("Cannot display scale plot, for lack of scale data");
            }
        }
    }

    //------------//
    // plotStaves //
    //------------//
    /**
     * Action that allows to display the horizontal projection of a
     * selected staff.
     * We need a sub-menu to select proper staff.
     * TODO: this is really a dirty hack!
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void plotStaves (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub != null) {
            final Sheet sheet = stub.getSheet();
            final StaffManager staffManager = sheet.getStaffManager();

            if (staffManager.getStaffCount() == 0) {
                logger.info("No staff data available yet");

                return;
            }

            JPopupMenu popup = new JPopupMenu("Staves IDs");

            // Menu title
            JMenuItem title = new JMenuItem("Select staff ID:");
            title.setHorizontalAlignment(SwingConstants.CENTER);
            title.setEnabled(false);
            popup.add(title);
            popup.addSeparator();

            ActionListener listener = new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    int index = Integer.decode(e.getActionCommand()) - 1;
                    Staff staff = staffManager.getStaff(index);
                    new StaffProjector(sheet, staff).plot();
                }
            };

            // Populate popup
            for (Staff staff : staffManager.getStaves()) {
                JMenuItem item = new JMenuItem("" + staff.getId());
                item.addActionListener(listener);
                popup.add(item);
            }

            // Display popup menu
            JFrame frame = OMR.getGui().getFrame();
            popup.show(frame, frame.getWidth() / 6, frame.getHeight() / 4);
        }
    }

    //----------//
    // plotStem //
    //----------//
    /**
     * Action that allows to display the plot of stem scaler.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void plotStem (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub != null) {
            if (stub.isDone(Step.STEM_SEEDS)) {
                new StemScaler(stub.getSheet()).displayChart();
            } else {
                logger.warn("Cannot display stem plot, for lack of stem data");
            }
        }
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
        final Book book = StubsController.getCurrentBook();

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
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> printBookAs (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        // Select target book print path
        final Path bookPath = UIUtil.pathChooser(
                true,
                OMR.getGui().getFrame(),
                BookManager.getDefaultPrintPath(book),
                new OmrFileFilter(OMR.PDF_EXTENSION),
                "Choose book print target");

        if (bookPath == null) {
            return null;
        }

        return new PrintBookTask(book, bookPath);
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
    @Action(enabledProperty = STUB_IDLE)
    public Task<Void, Void> printSheetAs (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return null;
        }

        // Let the user select a PDF output file
        final Book book = stub.getBook();
        final String ext = OMR.PDF_EXTENSION;
        final Path defaultBookPath = BookManager.getDefaultPrintPath(book);
        final Path bookSansExt = FileUtil.avoidExtensions(defaultBookPath, OMR.PDF_EXTENSION);
        final String suffix = book.isMultiSheet() ? (OMR.SHEET_SUFFIX + stub.getNumber()) : "";
        final Path defaultSheetPath = Paths.get(bookSansExt + suffix + ext);

        final Path sheetPath = UIUtil.pathChooser(
                true,
                OMR.getGui().getFrame(),
                defaultSheetPath,
                filter(ext),
                "Choose sheet print target");

        if ((sheetPath == null) || !confirmed(sheetPath)) {
            return null;
        }

        return new PrintSheetTask(stub.getSheet(), sheetPath);
    }

    //----------------//
    // projectHistory //
    //----------------//
    @Action
    public void projectHistory (ActionEvent e)
    {
        logger.info("projectHistory");
    }

    //--------------//
    // recordGlyphs //
    //--------------//
    @Action(enabledProperty = STUB_AVAILABLE)
    public RecordGlyphsTask recordGlyphs ()
    {
        int answer = JOptionPane.showConfirmDialog(
                OMR.getGui().getFrame(),
                "Are you sure of all the symbols of this sheet ?");

        if (answer == JOptionPane.YES_OPTION) {
            return new RecordGlyphsTask();
        } else {
            return null;
        }
    }

    //------------//
    // resetSheet //
    //------------//
    /**
     * Action that resets the currently selected sheet.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void resetSheet (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub != null) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.getGui().getFrame(),
                    "Do you confirm resetting sheet " + stub.getId() + " to its initial state?");

            if (answer == JOptionPane.YES_OPTION) {
                final Sheet sheet = stub.getSheet();
                new ResetTask(sheet).launch(sheet);
            }
        }
    }

    //----------//
    // saveBook //
    //----------//
    /**
     * Action to save the internals of the currently selected book.
     *
     * @param e the event that triggered this action
     * @return the UI task to perform
     */
    @Action(enabledProperty = BOOK_MODIFIED)
    public Task<Void, Void> saveBook (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        // Ask user confirmation for overwriting if file already exists
        final Path projectPath = BookManager.getDefaultProjectPath(book);

        if ((book.getProjectPath() != null) || confirmed(projectPath)) {
            return new StoreBookTask(book, projectPath);
        } else {
            return saveBookAs(e);
        }
    }

    //------------//
    // saveBookAs //
    //------------//
    @Action(enabledProperty = STUB_AVAILABLE)
    public Task<Void, Void> saveBookAs (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        // Let the user select a project output file
        final Path projectPath = selectProjectPath(true, BookManager.getDefaultProjectPath(book));
        final Path bookPath = book.getProjectPath();

        if ((projectPath != null)
            && (((bookPath != null) && bookPath.toAbsolutePath().equals(projectPath.toAbsolutePath()))
                || confirmed(projectPath))) {
            return new StoreBookTask(book, projectPath);
        }

        return null;
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

    //-------------//
    // storeScript //
    //-------------//
    @Action(enabledProperty = STUB_AVAILABLE)
    public Task<Void, Void> storeScript (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        final Path scriptPath = book.getScriptPath();

        if (scriptPath != null) {
            return new StoreScriptTask(book.getScript(), scriptPath);
        } else {
            return storeScriptAs(e);
        }
    }

    //---------------//
    // storeScriptAs //
    //---------------//
    @Action(enabledProperty = STUB_AVAILABLE)
    public Task<Void, Void> storeScriptAs (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        // Let the user select a script output file
        Path scriptPath = UIUtil.pathChooser(
                true,
                OMR.getGui().getFrame(),
                getDefaultScriptPath(book),
                new OmrFileFilter("Script files", new String[]{OMR.SCRIPT_EXTENSION}));

        if (scriptPath != null) {
            return new StoreScriptTask(book.getScript(), scriptPath);
        } else {
            return null;
        }
    }

    //------------//
    // swapSheets //
    //------------//
    /**
     * Swap out (most of) book sheets.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = BOOK_IDLE)
    public void swapSheets (ActionEvent e)
    {
        Book book = StubsController.getCurrentStub().getBook();

        book.swapAllSheets();
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

    //------------//
    // zoomHeight //
    //------------//
    /**
     * Action that allows to adjust the display zoom, so that the full height is shown.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void zoomHeight (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return;
        }

        SheetAssembly assembly = stub.getAssembly();

        if (assembly == null) {
            return;
        }

        assembly.getSelectedView().fitHeight();
    }

    //-----------//
    // zoomWidth //
    //-----------//
    /**
     * Action that allows to adjust the display zoom, so that the full width is shown.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void zoomWidth (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return;
        }

        SheetAssembly assembly = stub.getAssembly();

        if (assembly == null) {
            return;
        }

        assembly.getSelectedView().fitWidth();
    }

    //-------------------//
    // applyUserSettings //
    //-------------------//
    /**
     * Prompts the user for interactive confirmation or modification of
     * book/page parameters
     *
     * @param stub the current sheet stub, or null
     * @return true if parameters are applied, false otherwise
     */
    private static boolean applyUserSettings (final SheetStub stub)
    {
        try {
            final WrappedBoolean apply = new WrappedBoolean(false);
            final ScoreParameters scoreParams = new ScoreParameters(stub);
            final JOptionPane optionPane = new JOptionPane(
                    scoreParams.getComponent(),
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
            final String frameTitle = (stub != null)
                    ? (stub.getBook().getRadix() + " parameters")
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
                        if (!apply.isSet() || scoreParams.commit(stub)) {
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

    //-----------//
    // confirmed //
    //-----------//
    /**
     * Check whether we have user confirmation to overwrite the target path.
     * This is a no-op if target does not already exist.
     *
     * @param target the path to be checked
     * @return false if explicitly not confirmed, true otherwise
     */
    private static boolean confirmed (Path target)
    {
        return (!Files.exists(target))
               || OMR.getGui().displayConfirmation("Overwrite " + target + "?");
    }

    //--------//
    // filter //
    //--------//
    private static OmrFileFilter filter (String ext)
    {
        return new OmrFileFilter(ext, new String[]{ext});
    }

    //-------------------//
    // selectProjectPath //
    //-------------------//
    /**
     * Let the user interactively select a project path
     *
     * @param path default path
     * @param save true for write, false for read
     * @return the selected path or null
     */
    private static Path selectProjectPath (boolean save,
                                           Path path)
    {
        Path prjPath = UIUtil.pathChooser(
                save,
                OMR.getGui().getFrame(),
                path,
                filter(OMR.PROJECT_EXTENSION));

        return (prjPath == null) ? null : prjPath;
    }

    //----------------------//
    // getDefaultScriptPath //
    //----------------------//
    /**
     * Report the default path where the script should be written to
     *
     * @param book the containing book
     * @return the default path for saving the script
     */
    private Path getDefaultScriptPath (Book book)
    {
        return (book.getScriptPath() != null) ? book.getScriptPath()
                : Paths.get(
                        constants.defaultScriptDirectory.getValue(),
                        book.getRadix() + OMR.SCRIPT_EXTENSION);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------------//
    // OpenProjectTask //
    //-----------------//
    /**
     * Task that opens a book project file.
     */
    public static class OpenProjectTask
            extends PathTask
    {
        //~ Constructors ---------------------------------------------------------------------------

        public OpenProjectTask (Path path)
        {
            super(path);
        }

        public OpenProjectTask ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            if (Files.exists(path)) {
                // Actually open the project
                Book book = OMR.getEngine().loadProject(path);
            } else {
                logger.warn("Path {} does not exist", path);
            }

            return null;
        }
    }

    //----------//
    // OpenTask //
    //----------//
    /**
     * Task that opens a book image file.
     */
    public static class OpenTask
            extends PathTask
    {
        //~ Constructors ---------------------------------------------------------------------------

        public OpenTask (Path path)
        {
            super(path);
        }

        public OpenTask ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            if (Files.exists(path)) {
                try {
                    // Actually open the image file
                    Book book = OMR.getEngine().loadInput(path);
                    book.createStubs(null);
                    book.setModified(true);
                } catch (Exception ex) {
                    logger.warn("Error opening path " + path + " " + ex, ex);
                }
            } else {
                logger.warn("Path {} does not exist", path);
            }

            return null;
        }
    }

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
            //            for (Sheet sheet : book.getStubs()) {
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

        final Path sheetPrintPath;

        //~ Constructors ---------------------------------------------------------------------------
        public PrintSheetTask (Sheet sheet,
                               Path sheetPrintPath)
        {
            this.sheet = sheet;
            this.sheetPrintPath = sheetPrintPath;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            sheet.print(sheetPrintPath);

            return null;
        }
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
                for (SheetStub stub : book.getValidStubs()) {
                    stub.ensureStep(Step.PAGE);
                }
            } catch (Exception ex) {
                logger.warn("Could not build book", ex);
            }

            return null;
        }
    }

    //-----------------//
    // BuildScoresTask //
    //-----------------//
    private static class BuildScoresTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Book book;

        //~ Constructors ---------------------------------------------------------------------------
        public BuildScoresTask (Book book)
        {
            this.book = book;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                for (SheetStub stub : book.getValidStubs()) {
                    stub.ensureStep(Step.PAGE);
                }

                book.buildScores();
            } catch (Exception ex) {
                logger.warn("Could not build book, " + ex, ex);
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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean promptParameters = new Constant.Boolean(
                false,
                "Should we prompt the user for score parameters?");

        private final Constant.String validImageExtensions = new Constant.String(
                ".bmp .gif .jpg .png .tiff .tif .pdf",
                "Valid image file extensions, whitespace-separated");

        private final Constant.Boolean closeConfirmation = new Constant.Boolean(
                true,
                "Should we ask confirmation for closing an unsaved project?");

        private final Constant.String defaultScriptDirectory = new Constant.String(
                WellKnowns.DEFAULT_SCRIPTS_FOLDER.toString(),
                "Default directory for saved scripts");
    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<Boolean>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public Boolean getSpecific ()
        {
            boolean val = constants.closeConfirmation.getValue();
            logger.debug("closeConfirmation: {}", val);

            return val;
        }

        @Override
        public boolean setSpecific (Boolean specific)
        {
            if (!getSpecific().equals(specific)) {
                constants.closeConfirmation.setValue(specific);
                logger.info(
                        "You will {} be prompted to save project when closing",
                        specific ? "now" : "no longer");

                return true;
            } else {
                return false;
            }
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
         * @param book            the book to export
         * @param bookPathSansExt (non-null) the target export book path with no extension
         */
        public ExportBookTask (Book book,
                               Path bookPathSansExt)
        {
            this.book = book;
            book.setExportPathSansExt(bookPathSansExt);
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

        final Path bookExportPathSansExt;

        //~ Constructors ---------------------------------------------------------------------------
        public ExportSheetTask (Sheet sheet,
                                Path bookExportPathSansExt)
        {
            this.sheet = sheet;
            this.bookExportPathSansExt = bookExportPathSansExt;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            sheet.getBook().setExportPathSansExt(bookExportPathSansExt);

            if (checkParameters(sheet)) {
                sheet.ensureStep(Step.PAGE);
                sheet.export();
            }

            return null;
        }
    }

    //----------------//
    // LoadScriptTask //
    //----------------//
    private static class LoadScriptTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Path path;

        //~ Constructors ---------------------------------------------------------------------------
        LoadScriptTask (Path path)
        {
            this.path = path;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            // Actually run the script
            logger.info("Running script file {} ...", path);

            FileInputStream is = null;

            try {
                is = new FileInputStream(path.toFile());

                final Script script = ScriptManager.getInstance().load(is);

                if (logger.isDebugEnabled()) {
                    script.dump();
                }

                // Remember (even across runs) the parent directory
                constants.defaultScriptDirectory.setValue(path.getParent().toString());
                script.run();
            } catch (FileNotFoundException ex) {
                logger.warn("Cannot find script file {}", path);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        logger.warn("Error closing script file {}", path);
                    }
                }
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

    //------------------//
    // RecordGlyphsTask //
    //------------------//
    private static class RecordGlyphsTask
            extends BasicTask
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            SheetStub stub = StubsController.getCurrentStub();
            SampleRepository.getInstance().recordSheetGlyphs(stub.getSheet(), false);

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

        final Book book;

        final Path projectPath;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create an asynchronous task to store the book.
         *
         * @param book        the book to export
         * @param projectPath (non-null) the target to store book path
         */
        public StoreBookTask (Book book,
                              Path projectPath)
        {
            this.book = book;
            this.projectPath = projectPath;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            book.store(projectPath);
            BookActions.getInstance().setBookModified(false);

            return null;
        }
    }

    //-----------------//
    // StoreScriptTask //
    //-----------------//
    private static class StoreScriptTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Script script;

        private final Path path;

        //~ Constructors ---------------------------------------------------------------------------
        StoreScriptTask (Script script,
                         Path path)
        {
            this.script = script;
            this.path = path;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            FileOutputStream fos = null;

            try {
                Path folder = path.getParent();

                if (!Files.exists(folder)) {
                    Files.createDirectories(folder);
                    logger.info("Creating folder {}", folder);
                }

                fos = new FileOutputStream(path.toFile());
                omr.script.ScriptManager.getInstance().store(script, fos);
                logger.info("Script stored as {}", path);
                constants.defaultScriptDirectory.setValue(folder.toString());
                script.getBook().setScriptPath(path);
            } catch (FileNotFoundException ex) {
                logger.warn("Cannot find script file " + path + ", " + ex, ex);
            } catch (JAXBException ex) {
                logger.warn("Cannot marshal script, " + ex, ex);
            } catch (Throwable ex) {
                logger.warn("Error storing script, " + ex, ex);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            return null;
        }
    }
}
