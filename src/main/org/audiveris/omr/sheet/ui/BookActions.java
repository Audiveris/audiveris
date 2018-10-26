//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B o o k A c t i o n s                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.ui;

import ij.process.ByteProcessor;

import org.audiveris.omr.OMR;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.ui.SampleBrowser;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.plugin.Plugin;
import org.audiveris.omr.plugin.PluginsManager;
import org.audiveris.omr.score.ui.ScoreParameters;
import org.audiveris.omr.score.ui.SheetParameters;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.sheet.ExportPattern;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.ScaleBuilder;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.grid.StaffProjector;
import org.audiveris.omr.sheet.stem.StemScaler;
import static org.audiveris.omr.sheet.ui.StubDependent.BOOK_IDLE;
import static org.audiveris.omr.sheet.ui.StubDependent.STUB_AVAILABLE;
import static org.audiveris.omr.sheet.ui.StubDependent.STUB_IDLE;
import org.audiveris.omr.sig.ui.InterController;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.util.CursorController;
import org.audiveris.omr.ui.util.OmrFileFilter;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.view.HistoryMenu;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.PathTask;
import org.audiveris.omr.util.VoidTask;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.param.Param;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Class {@code BookActions} gathers all UI actions related to current book.
 * <p>
 * Swing EDT processes Runnable instances found in its event queue.
 * Via {@link SwingUtilities} new Runnable instances can further be appended to the event queue.
 * Unless log context is explicitly started and stopped in such Runnable, there is no reliable way
 * to set log context for the EDT.
 * <p>
 * By definition, all actions defined in this class are initiated on Swing EDT.
 * Hence, if log context handling is wanted, the action must delegate processing to a separate task
 * (log context can easily be handled in a non-EDT thread).
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

    /** Sub-menu on images history. */
    private final HistoryMenu imageHistoryMenu;

    /** Sub-menu on books history. */
    private final HistoryMenu bookHistoryMenu;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BookActions object.
     */
    private BookActions ()
    {
        final BookManager mgr = BookManager.getInstance();
        imageHistoryMenu = new HistoryMenu(mgr.getImageHistory(), LoadImageTask.class);
        bookHistoryMenu = new HistoryMenu(mgr.getBookHistory(), LoadBookTask.class);
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
    // checkStored //
    //-------------//
    /**
     * Check whether the provided book has been saved if needed
     * (and therefore, if it can be closed)
     *
     * @param book the book to check
     * @return true if close is allowed, false if not
     */
    public static boolean checkStored (Book book)
    {
        if (book.isModified() && defaultPrompt.getValue()) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.gui.getFrame(),
                    "Save modified book " + book.getRadix() + "?");

            if (answer == JOptionPane.YES_OPTION) {
                Path bookPath;

                if (book.getBookPath() == null) {
                    // Find a suitable target file
                    bookPath = BookManager.getDefaultSavePath(book);

                    // Check the target is fine
                    if (!confirmed(bookPath)) {
                        // Let the user select an alternate output file
                        bookPath = selectBookPath(true, BookManager.getDefaultSavePath(book));

                        if ((bookPath == null) || !confirmed(bookPath)) {
                            return false; // No suitable target found
                        }
                    }
                } else {
                    bookPath = book.getBookPath();
                }

                try {
                    // Save the book to target file
                    book.store(bookPath, false);

                    return true; // Book successfully saved
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

    //--------------//
    // annotateBook //
    //--------------//
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> annotateBook (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        return new VoidTask()
        {
            @Override
            protected Void doInBackground ()
                    throws InterruptedException
            {
                try {
                    LogUtil.start(book);
                    book.annotate();
                } finally {
                    LogUtil.stopBook();
                }

                return null;
            }
        };
    }

    //---------------//
    // annotateSheet //
    //---------------//
    @Action(enabledProperty = STUB_IDLE)
    public Task<Void, Void> annotateSheet (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return null;
        }

        final Sheet sheet = stub.getSheet();

        return new VoidTask()
        {
            @Override
            protected Void doInBackground ()
                    throws InterruptedException
            {
                try {
                    LogUtil.start(sheet.getStub());
                    sheet.annotate();
                } catch (Exception ex) {
                    logger.warn("Annotations failed {}", ex);
                } finally {
                    LogUtil.stopBook();
                }

                return null;
            }
        };
    }

    //-------------//
    // bookHistory //
    //-------------//
    @Action
    public void bookHistory (ActionEvent e)
    {
        logger.info("bookHistory");
    }

    //------------//
    // browseBook //
    //------------//
    /**
     * Launch the tree display of the current book.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void browseBook (ActionEvent e)
    {
        OmrGui.getApplication().show(StubsController.getCurrentBook().getBrowserFrame());
    }

    //-----------//
    // closeBook //
    //-----------//
    /**
     * Action that handles the closing of the currently selected book.
     *
     * @param e the event that triggered this action
     * @return the task which will close the book
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public Task<Void, Void> closeBook (ActionEvent e)
    {
        Book book = StubsController.getCurrentBook();

        if ((book != null) && checkStored(book)) {
            // Pre-select the suitable "next" book tab
            // TODO? should not do this (we are not on EDT). Don't use a task!
            StubsController.getInstance().selectOtherBook(book);

            // Now close the book (+ related tab)
            return new CloseBookTask(book);
        }

        return null;
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

    //-----------------------//
    // defineSheetParameters //
    //-----------------------//
    /**
     * Launch the dialog to set up sheet parameters.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void defineSheetParameters (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        try {
            final WrappedBoolean apply = new WrappedBoolean(false);
            final SheetParameters sheetParams = new SheetParameters(stub.getSheet());
            final JOptionPane optionPane = new JOptionPane(
                    sheetParams.getComponent(),
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
            final String frameTitle = stub.getId() + " parameters";
            final JDialog dialog = new JDialog(OMR.gui.getFrame(), frameTitle, true); // Modal flag
            dialog.setContentPane(optionPane);
            dialog.setName("sheetParams");

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
                        int value = (Integer) obj;
                        apply.set(value == JOptionPane.OK_OPTION);

                        // Exit only if user gives up or enters correct data
                        if (!apply.isSet() || sheetParams.commit()) {
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
            OmrGui.getApplication().show(dialog);
        } catch (Exception ex) {
            logger.warn("Error in SheetParameters", ex);
        }
    }

    //--------------------//
    // displayAnnotations //
    //--------------------//
    /**
     * Action that allows to display the view on annotations
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void displayAnnotations (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub.isDone(Step.ANNOTATIONS)) {
            final SheetAssembly assembly = stub.getAssembly();
            final SheetTab tab = SheetTab.ANNOTATION_TAB;

            if (assembly.getPane(tab.label) == null) {
                stub.getSheet().displayAnnotationTab();
            } else {
                assembly.selectViewTab(tab);
            }
        } else {
            logger.info("No annotations available yet.");
        }
    }

    //---------------//
    // displayBinary //
    //---------------//
    /**
     * Action that allows to display the view on binary image
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void displayBinary (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub.isDone(Step.BINARY)) {
            final SheetAssembly assembly = stub.getAssembly();
            final SheetTab tab = SheetTab.BINARY_TAB;

            if (assembly.getPane(tab.label) == null) {
                stub.getSheet().createBinaryView();
            } else {
                assembly.selectViewTab(tab);
            }
        } else {
            logger.info("No binary image available yet.");
        }
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
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub.isDone(Step.GRID)) {
            final SheetAssembly assembly = stub.getAssembly();
            final SheetTab tab = SheetTab.DATA_TAB;

            if (assembly.getPane(tab.label) == null) {
                stub.getSheet().displayDataTab();
            } else {
                assembly.selectViewTab(tab);
            }
        } else {
            logger.info("No data buffer available yet.");
        }
    }

    //----------------//
    // displayInitial //
    //----------------//
    /**
     * Action that allows to display the view on initial image.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void displayInitial (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();
        final SheetAssembly assembly = stub.getAssembly();
        final SheetTab tab = SheetTab.INITIAL_TAB;

        if (assembly.getPane(tab.label) == null) {
            stub.getSheet().createInitialView();
        } else {
            assembly.selectViewTab(tab);
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
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub.isDone(Step.GRID)) {
            final SheetAssembly assembly = stub.getAssembly();
            final SheetTab tab = SheetTab.NO_STAFF_TAB;

            if (assembly.getPane(tab.label) == null) {
                Sheet sheet = stub.getSheet(); // This may load the sheet...
                ByteProcessor noStaff = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);
                assembly.addViewTab(
                        tab,
                        new ScrollImageView(sheet, new ImageView(noStaff.getBufferedImage())),
                        new BoardsPane(new PixelBoard(sheet)));
            } else {
                assembly.selectViewTab(tab);
            }
        } else {
            logger.info("No staff lines available yet.");
        }
    }

    //------------------------//
    // displayStaffLineGlyphs //
    //------------------------//
    /**
     * Action that allows to display the view on staff underlying glyphs.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void displayStaffLineGlyphs (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub.isDone(Step.GRID)) {
            final SheetAssembly assembly = stub.getAssembly();
            final SheetTab tab = SheetTab.STAFF_LINE_TAB;

            if (assembly.getPane(tab.label) == null) {
                Sheet sheet = stub.getSheet(); // This may load the sheet...
                assembly.addViewTab(
                        tab,
                        new ScrollImageView(
                                sheet,
                                new ImageView(sheet.getPicture().buildStaffLineGlyphsImage())),
                        new BoardsPane(new PixelBoard(sheet)));
            } else {
                assembly.selectViewTab(tab);
            }
        } else {
            logger.info("No staff lines available yet.");
        }
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

        final Path exportPathSansExt = BookManager.getDefaultExportPathSansExt(book);

        if (exportPathSansExt != null) {
            //TODO: check/prompt for overwrite??? (perhaps several files)
            return new ExportBookTask(book, exportPathSansExt);
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
            return null;
        }

        // Let user select book export target
        //TODO: if we have several movements in this book, export will result in several files...
        //TODO: so, how can we check/prompt for overwrite?
        final String ext = BookManager.getExportExtension();
        final Path sansExt = BookManager.getDefaultExportPathSansExt(book);
        final Path targetPath = Paths.get(sansExt + ext);
        final Path bookPath = UIUtil.pathChooser(
                true,
                OMR.gui.getFrame(),
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
                OMR.gui.getFrame(),
                defaultSheetPath,
                filter(ext),
                "Choose sheet export target");

        if ((sheetPath == null) || !confirmed(sheetPath)) {
            return null;
        }

        return new ExportSheetTask(stub.getSheet(), sheetPath);
    }

    //--------------------//
    // getBookHistoryMenu //
    //--------------------//
    public HistoryMenu getBookHistoryMenu ()
    {
        return bookHistoryMenu;
    }

    //---------------------//
    // getInputHistoryMenu //
    //---------------------//
    public HistoryMenu getImageHistoryMenu ()
    {
        return imageHistoryMenu;
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
                    OMR.gui.getFrame(),
                    "About to set sheet " + stub.getId() + " as invalid." + "\nDo you confirm?");

            if (answer == JOptionPane.YES_OPTION) {
                final Sheet sheet = stub.getSheet();
                final StubsController controller = StubsController.getInstance();

                if (ViewParameters.getInstance().isInvalidSheetDisplay() == false) {
                    controller.removeAssembly(sheet.getStub());
                } else {
                    controller.callAboutStub(sheet.getStub());
                }
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
     * @return the task to launch in background
     */
    @Action(enabledProperty = STUB_IDLE)
    public Task<Void, Void> invokeDefaultPlugin (ActionEvent e)
    {
        Plugin defaultPlugin = PluginsManager.getInstance().getDefaultPlugin();

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

    //------------------//
    // isRebuildAllowed //
    //------------------//
    public boolean isRebuildAllowed ()
    {
        return rebuildAllowed;
    }

    //----------//
    // openBook //
    //----------//
    /**
     * Action that let the user select a book.
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public LoadBookTask openBook (ActionEvent e)
    {
        final Path path = selectBookPath(false, BookManager.getBaseFolder());

        if (path != null) {
            if (Files.exists(path)) {
                return new LoadBookTask(path);
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
    public LoadImageTask openImageFile (ActionEvent e)
    {
        String suffixes = constants.validImageExtensions.getValue();
        String allSuffixes = suffixes + " " + suffixes.toUpperCase();
        Path path = UIUtil.pathChooser(
                false,
                OMR.gui.getFrame(),
                Paths.get(BookManager.getDefaultImageFolder()),
                new OmrFileFilter(
                        "Major image files" + " (" + suffixes + ")",
                        allSuffixes.split("\\s")));

        if (path != null) {
            if (!Files.exists(path)) {
                logger.warn("{} not found.", path);
            } else if (Files.isDirectory(path)) {
                logger.warn("{} is a directory.", path);
            } else {
                return new LoadImageTask(path);
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
            if (stub.isDone(Step.BINARY)) {
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
     * Action that allows to display the horizontal projection of a selected staff.
     * We need a sub-menu to select proper staff.
     * TODO: this is really a dirty hack!
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void plotStaves (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return;
        }

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
                new StaffProjector(sheet, staff, null).plot();
            }
        };

        // Populate popup
        for (Staff staff : staffManager.getStaves()) {
            JMenuItem item = new JMenuItem("" + staff.getId());
            item.addActionListener(listener);
            popup.add(item);
        }

        // Display popup menu
        JFrame frame = OMR.gui.getFrame();
        popup.show(frame, frame.getWidth() / 6, frame.getHeight() / 4);
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

        if (stub == null) {
            return;
        }

        if (stub.isDone(Step.STEM_SEEDS)) {
            new StemScaler(stub.getSheet()).displayChart();
        } else {
            logger.warn("Cannot display stem plot, for lack of stem data");
        }
    }

    //-----------//
    // printBook //
    //-----------//
    /**
     * Print the currently selected book, as a PDF file
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

        final Path bookPrintPath = BookManager.getDefaultPrintPath(book);

        if ((bookPrintPath != null) && confirmed(bookPrintPath)) {
            return new PrintBookTask(book, bookPrintPath);
        }

        return printBookAs(e);
    }

    //------------------------//
    // printBookAnnotationsAs //
    //------------------------//
    /**
     * Write annotations of the currently selected book, using PDF format, to a
     * user-provided file.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> printBookAnnotationsAs (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        // Select target book print path
        final Path bookPrintPath = choosePrintPath(book, ".annotations");

        if ((bookPrintPath == null) || !confirmed(bookPrintPath)) {
            return null;
        }

        return new PrintBookAnnotationTask(book, bookPrintPath);
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
        final Path bookPrintPath = choosePrintPath(book, "");

        if ((bookPrintPath == null) || !confirmed(bookPrintPath)) {
            return null;
        }

        return new PrintBookTask(book, bookPrintPath);
    }

    //-------------------------//
    // printSheetAnnotationsAs //
    //-------------------------//
    /**
     * Write annotations of the currently selected sheet, using PDF format, to a
     * user-provided location.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = STUB_IDLE)
    public Task<Void, Void> printSheetAnnotationsAs (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return null;
        }

        if (stub.getLatestStep().compareTo(Step.ANNOTATIONS) < 0) {
            logger.info("Annotations are not yet available");

            return null;
        }

        // Let the user select a PDF output file
        final Path sheetPrintPath = choosePrintPath(stub, ".annotations");

        if ((sheetPrintPath == null) || !confirmed(sheetPrintPath)) {
            return null;
        }

        return new PrintSheetAnnotationTask(stub.getSheet(), sheetPrintPath);
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
        final Path sheetPrintPath = choosePrintPath(stub, "");

        if ((sheetPrintPath == null) || !confirmed(sheetPrintPath)) {
            return null;
        }

        return new PrintSheetTask(stub.getSheet(), sheetPrintPath);
    }

    //-----------------//
    // printSheetMixAs //
    //-----------------//
    /**
     * Write the currently selected sheet, both input and output, to a user-provided
     * location.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = STUB_IDLE)
    public Task<Void, Void> printSheetMixAs (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return null;
        }

        // Let the user select a PNG output file
        final Path sheetPrintPath = choosePngPath(stub, "");

        if ((sheetPrintPath == null) || !confirmed(sheetPrintPath)) {
            return null;
        }

        return new PrintSheetMixTask(stub.getSheet(), sheetPrintPath);
    }

    //------//
    // redo //
    //------//
    /**
     * Action to redo undone user modification.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = REDOABLE)
    public void redo (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return;
        }

        Sheet sheet = stub.getSheet();
        InterController controller = sheet.getInterController();
        controller.redo();
    }

    //-----------//
    // resetBook //
    //-----------//
    /**
     * Action that resets the currently selected book.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = BOOK_IDLE)
    public void resetBook (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book != null) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.gui.getFrame(),
                    "About to reset all valid sheets of " + book.getRadix()
                    + " to their initial state." + "\nDo you confirm?");

            if (answer == JOptionPane.YES_OPTION) {
                book.reset();
            }
        }
    }

    //------------------------//
    // resetBookToAnnotations //
    //------------------------//
    /**
     * Action that tries to reset to ANNOTATIONS all valid sheets of selected book.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = BOOK_ANNOTATED)
    public void resetBookToAnnotations (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book != null) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.gui.getFrame(),
                    "About to reset all valid sheets of " + book.getRadix()
                    + " to their ANNOTATIONS step." + "\nDo you confirm?");

            if (answer == JOptionPane.YES_OPTION) {
                book.resetToAnnotations();
            }
        }
    }

    //-------------------//
    // resetBookToBinary //
    //-------------------//
    /**
     * Action that resets to BINARY all valid sheets of selected book.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = BOOK_IDLE)
    public void resetBookToBinary (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book != null) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.gui.getFrame(),
                    "About to reset all valid sheets of " + book.getRadix()
                    + " to their BINARY state." + "\nDo you confirm?");

            if (answer == JOptionPane.YES_OPTION) {
                book.resetToBinary();
            }
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
    @Action(enabledProperty = STUB_IDLE)
    public void resetSheet (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub != null) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.gui.getFrame(),
                    "About to reset sheet " + stub.getId() + " to its initial state."
                    + "\nDo you confirm?");

            if (answer == JOptionPane.YES_OPTION) {
                stub.reset();
            }
        }
    }

    //-------------------------//
    // resetSheetToAnnotations //
    //-------------------------//
    /**
     * Action that resets the currently selected sheet to the annotations step.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_ANNOTATED)
    public void resetSheetToAnnotations (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub != null) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.gui.getFrame(),
                    "About to reset sheet " + stub.getId() + " to its ANNOTATIONS step."
                    + "\nDo you confirm?");

            if (answer == JOptionPane.YES_OPTION) {
                stub.resetToAnnotations();
            }
        }
    }

    //--------------------//
    // resetSheetToBinary //
    //--------------------//
    /**
     * Action that resets the currently selected sheet to the binary step.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_IDLE)
    public void resetSheetToBinary (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub != null) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.gui.getFrame(),
                    "About to reset sheet " + stub.getId() + " to its BINARY state."
                    + "\nDo you confirm?");

            if (answer == JOptionPane.YES_OPTION) {
                stub.resetToBinary();
            }
        }
    }

    //------------//
    // sampleBook //
    //------------//
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> sampleBook (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        return new SampleBookTask(book);
    }

    //-------------//
    // sampleSheet //
    //-------------//
    @Action(enabledProperty = STUB_IDLE)
    public Task<Void, Void> sampleSheet (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return null;
        }

        return new SampleSheetTask(stub.getSheet());
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

        final Path bookPath = BookManager.getDefaultSavePath(book);

        if ((book.getBookPath() != null)
            && (bookPath.toAbsolutePath().equals(book.getBookPath().toAbsolutePath())
                || confirmed(bookPath))) {
            return new StoreBookTask(book, bookPath);
        }

        return saveBookAs(e);
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

        // Let the user select a book output file
        final Path defaultBookPath = BookManager.getDefaultSavePath(book);
        final Path targetPath = selectBookPath(true, defaultBookPath);
        final Path ownPath = book.getBookPath();

        if ((targetPath != null)
            && (((ownPath != null) && ownPath.toAbsolutePath().equals(targetPath.toAbsolutePath()))
                || confirmed(targetPath))) {
            return new StoreBookTask(book, targetPath);
        }

        return null;
    }

    //--------------------//
    // saveBookRepository //
    //--------------------//
    /**
     * Action to save the separate repository of the currently selected book.
     *
     * @param e the event that triggered this action
     * @return the UI task to perform
     */
    @Action(enabledProperty = BOOK_MODIFIED)
    public Task<Void, Void> saveBookRepository (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        if (book.hasAllocatedRepository()) {
            SampleRepository repo = book.getSampleRepository();

            if (repo.isModified()) {
                repo.storeRepository();
            }
        }

        return null;
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
        Book book = StubsController.getCurrentBook();

        if (book == null) {
            return;
        }

        book.swapAllSheets();
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

    //----------------//
    // transcribeBook //
    //----------------//
    /**
     * Launch or complete the transcription of all sheets and merge them at book level.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_TRANSCRIBABLE)
    public Task<Void, Void> transcribeBook (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return null;
        }

        return new TranscribeBookTask(stub);
    }

    //-----------------//
    // transcribeSheet //
    //-----------------//
    /**
     * Launch or complete sheet transcription.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = STUB_TRANSCRIBABLE)
    public Task<Void, Void> transcribeSheet (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return null;
        }

        return new TranscribeSheetTask(stub.getSheet());
    }

    //------//
    // undo //
    //------//
    /**
     * Action to undo last user modification.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = UNDOABLE)
    public void undo (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return;
        }

        Sheet sheet = stub.getSheet();
        InterController controller = sheet.getInterController();
        controller.undo();
    }

    //--------------------//
    // viewBookRepository //
    //--------------------//
    /**
     * Action to view the separate repository of the currently selected book.
     *
     * @param e the event that triggered this action
     * @return the UI task to perform
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public Task<Void, Void> viewBookRepository (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book != null) {
            if (book.hasSpecificRepository()) {
                CursorController.launchWithDelayedMessage(
                        "Launching sample browser...",
                        new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        try {
                            SampleBrowser.getInstance(book).setVisible();
                        } catch (Throwable ex) {
                            logger.warn("Could not launch sample browser. " + ex, ex);
                        }
                    }
                });
            } else {
                logger.info("No specific sample repository for {}", book);
            }
        }

        return null;
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

        ScrollView scrollView = assembly.getSelectedView();

        if (scrollView == null) {
            return;
        }

        scrollView.fitHeight();
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

        ScrollView scrollView = assembly.getSelectedView();

        if (scrollView == null) {
            return;
        }

        scrollView.fitWidth();
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
            final JDialog dialog = new JDialog(OMR.gui.getFrame(), frameTitle, true); // Modal flag
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
                        int value = (Integer) obj;
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
            OmrGui.getApplication().show(dialog);

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
        return (!Files.exists(target)) || OMR.gui.displayConfirmation("Overwrite " + target + "?");
    }

    //--------//
    // filter //
    //--------//
    private static OmrFileFilter filter (String ext)
    {
        return new OmrFileFilter(ext, new String[]{ext});
    }

    //----------------//
    // selectBookPath //
    //----------------//
    /**
     * Let the user interactively select a book path
     *
     * @param path default path
     * @param save true for write, false for read
     * @return the selected path or null
     */
    private static Path selectBookPath (boolean save,
                                        Path path)
    {
        Path prjPath = UIUtil.pathChooser(
                save,
                OMR.gui.getFrame(),
                path,
                filter(OMR.BOOK_EXTENSION));

        return (prjPath == null) ? null : prjPath;
    }

    //---------------//
    // choosePngPath //
    //---------------//
    private Path choosePngPath (SheetStub stub,
                                String preExt)
    {
        final String PNG_EXTENSION = ".png";
        final Book book = stub.getBook();
        final String ext = preExt + PNG_EXTENSION;
        final Path defaultBookPath = BookManager.getDefaultPrintPath(book);
        final Path bookSansExt = FileUtil.avoidExtensions(defaultBookPath, OMR.PRINT_EXTENSION);
        final String sheetSuffix = book.isMultiSheet() ? (OMR.SHEET_SUFFIX + stub.getNumber()) : "";
        final Path defaultSheetPath = Paths.get(bookSansExt + sheetSuffix + ext);

        return UIUtil.pathChooser(
                true,
                OMR.gui.getFrame(),
                defaultSheetPath,
                filter(ext),
                "Choose sheet png target");
    }

    //-----------------//
    // choosePrintPath //
    //-----------------//
    private Path choosePrintPath (Book book,
                                  String preExt)
    {
        final String ext = preExt + OMR.PRINT_EXTENSION;
        Path defaultBookPath = BookManager.getDefaultPrintPath(book);
        Path bookSansExt = FileUtil.avoidExtensions(defaultBookPath, OMR.PRINT_EXTENSION);

        if (!preExt.isEmpty()) {
            bookSansExt = FileUtil.avoidExtensions(bookSansExt, preExt);
        }

        defaultBookPath = Paths.get(bookSansExt + ext);

        return UIUtil.pathChooser(
                true,
                OMR.gui.getFrame(),
                defaultBookPath,
                filter(ext),
                "Choose book print target");
    }

    //-----------------//
    // choosePrintPath //
    //-----------------//
    private Path choosePrintPath (SheetStub stub,
                                  String preExt)
    {
        final Book book = stub.getBook();
        final String ext = preExt + OMR.PRINT_EXTENSION;
        final Path defaultBookPath = BookManager.getDefaultPrintPath(book);
        final Path bookSansExt = FileUtil.avoidExtensions(defaultBookPath, OMR.PRINT_EXTENSION);
        final String sheetSuffix = book.isMultiSheet() ? (OMR.SHEET_SUFFIX + stub.getNumber()) : "";
        final Path defaultSheetPath = Paths.get(bookSansExt + sheetSuffix + ext);

        return UIUtil.pathChooser(
                true,
                OMR.gui.getFrame(),
                defaultSheetPath,
                filter(ext),
                "Choose sheet print target");
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // LoadBookTask //
    //--------------//
    /**
     * Task that loads a book (.omr project) file.
     */
    public static class LoadBookTask
            extends PathTask
    {
        //~ Constructors ---------------------------------------------------------------------------

        public LoadBookTask (Path path)
        {
            super(path);
        }

        public LoadBookTask ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            if (Files.exists(path)) {
                try {
                    // Actually open the book
                    Book book = OMR.engine.loadBook(path);

                    if (book != null) {
                        LogUtil.start(book);
                        book.createStubsTabs(null); // Tabs are now accessible

                        // Focus on first valid stub in book, if any
                        SheetStub firstValid = book.getFirstValidStub();

                        if (firstValid != null) {
                            StubsController.invokeSelect(firstValid);
                        }
                    }
                } finally {
                    LogUtil.stopBook();
                }
            } else {
                logger.warn("Path {} does not exist", path);
            }

            return null;
        }
    }

    //---------------//
    // LoadImageTask //
    //---------------//
    /**
     * Task that opens an image file.
     */
    public static class LoadImageTask
            extends PathTask
    {
        //~ Constructors ---------------------------------------------------------------------------

        public LoadImageTask (Path path)
        {
            super(path);
        }

        public LoadImageTask ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                // Actually open the image file
                Book book = OMR.engine.loadInput(path);
                LogUtil.start(book);
                book.createStubs(null);
                book.createStubsTabs(null); // Tabs are now accessible

                // Focus on first valid stub in book, if any
                SheetStub firstValid = book.getFirstValidStub();

                if (firstValid != null) {
                    StubsController.invokeSelect(firstValid);
                }
            } catch (Exception ex) {
                logger.warn("Error opening path " + path + " " + ex, ex);
            } finally {
                LogUtil.stopBook();
            }

            return null;
        }
    }

    //-------------------------//
    // PrintBookAnnotationTask //
    //-------------------------//
    public static class PrintBookAnnotationTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Book book;

        final Path bookPrintPath;

        //~ Constructors ---------------------------------------------------------------------------
        public PrintBookAnnotationTask (Book book,
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
            try {
                LogUtil.start(book);
                book.printAnnotations(bookPrintPath);
            } finally {
                LogUtil.stopBook();
            }

            return null;
        }
    }

    //---------------//
    // PrintBookTask //
    //---------------//
    public static class PrintBookTask
            extends VoidTask
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
            try {
                LogUtil.start(book);
                book.setPrintPath(bookPrintPath);
                book.print();
            } finally {
                LogUtil.stopBook();
            }

            return null;
        }
    }

    //--------------------------//
    // PrintSheetAnnotationTask //
    //--------------------------//
    public static class PrintSheetAnnotationTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Sheet sheet;

        final Path sheetPrintPath;

        //~ Constructors ---------------------------------------------------------------------------
        public PrintSheetAnnotationTask (Sheet sheet,
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
            try {
                LogUtil.start(sheet.getStub());
                sheet.printAnnotations(sheetPrintPath);
            } finally {
                LogUtil.stopStub();
            }

            return null;
        }
    }

    //-------------------//
    // PrintSheetMixTask //
    //-------------------//
    public static class PrintSheetMixTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Sheet sheet;

        final Path sheetPrintPath;

        //~ Constructors ---------------------------------------------------------------------------
        public PrintSheetMixTask (Sheet sheet,
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
            try {
                LogUtil.start(sheet.getStub());
                sheet.printMix(sheetPrintPath);
            } finally {
                LogUtil.stopStub();
            }

            return null;
        }
    }

    //----------------//
    // PrintSheetTask //
    //----------------//
    public static class PrintSheetTask
            extends VoidTask
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
            try {
                LogUtil.start(sheet.getStub());
                sheet.print(sheetPrintPath);
            } finally {
                LogUtil.stopStub();
            }

            return null;
        }
    }

    //-----------------//
    // SampleSheetTask //
    //-----------------//
    public static class SampleSheetTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Sheet sheet;

        //~ Constructors ---------------------------------------------------------------------------
        public SampleSheetTask (Sheet sheet)
        {
            this.sheet = sheet;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                LogUtil.start(sheet.getStub());
                sheet.sample();
            } finally {
                LogUtil.stopStub();
            }

            return null;
        }
    }

    //---------------//
    // CloseBookTask //
    //---------------//
    private static class CloseBookTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Book book;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create an asynchronous task to close the book.
         *
         * @param book the book to close
         */
        public CloseBookTask (Book book)
        {
            this.book = book;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                LogUtil.start(book);
                book.close();
            } finally {
                LogUtil.stopBook();
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
                "Should we ask confirmation for closing an unsaved book?");
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
            if (isSpecific()) {
                return getValue();
            } else {
                return null;
            }
        }

        @Override
        public Boolean getValue ()
        {
            return constants.closeConfirmation.getValue();
        }

        @Override
        public boolean isSpecific ()
        {
            return !constants.closeConfirmation.isSourceValue();
        }

        @Override
        public boolean setSpecific (Boolean specific)
        {
            if (!getValue().equals(specific)) {
                constants.closeConfirmation.setValue(specific);
                logger.info(
                        "You will {} be prompted to save book when closing",
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
            extends VoidTask
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
            try {
                LogUtil.start(book);

                if (checkParameters(book)) {
                    book.export();
                }
            } finally {
                LogUtil.stopBook();
            }

            return null;
        }
    }

    //-----------------//
    // ExportSheetTask //
    //-----------------//
    private static class ExportSheetTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Sheet sheet;

        final Path sheetExportPath;

        //~ Constructors ---------------------------------------------------------------------------
        public ExportSheetTask (Sheet sheet,
                                Path sheetExportPath)
        {
            this.sheet = sheet;
            this.sheetExportPath = sheetExportPath;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                LogUtil.start(sheet.getStub());

                if (checkParameters(sheet)) {
                    sheet.getStub().reachStep(Step.PAGE, false);
                    sheet.export(sheetExportPath);
                }
            } finally {
                LogUtil.stopStub();
            }

            return null;
        }
    }

    //-------------//
    // RebuildTask //
    //-------------//
    private static class RebuildTask
            extends VoidTask
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

    //----------------//
    // SampleBookTask //
    //----------------//
    private static class SampleBookTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Book book;

        //~ Constructors ---------------------------------------------------------------------------
        public SampleBookTask (Book book)
        {
            this.book = book;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                LogUtil.start(book);
                book.sample();
            } finally {
                LogUtil.stopBook();
            }

            return null;
        }
    }

    //---------------//
    // StoreBookTask //
    //---------------//
    private static class StoreBookTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Book book;

        final Path bookPath;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create an asynchronous task to store the book.
         *
         * @param book     the book to export
         * @param bookPath (non-null) the target to store book path
         */
        public StoreBookTask (Book book,
                              Path bookPath)
        {
            this.book = book;
            this.bookPath = bookPath;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                LogUtil.start(book);
                book.store(bookPath, false);
                BookActions.getInstance().setBookModified(false);
            } finally {
                LogUtil.stopBook();
            }

            return null;
        }
    }

    //--------------------//
    // TranscribeBookTask //
    //--------------------//
    private static class TranscribeBookTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final SheetStub stub;

        private final Book book;

        //~ Constructors ---------------------------------------------------------------------------
        public TranscribeBookTask (SheetStub stub)
        {
            this.stub = stub;
            this.book = stub.getBook();
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                LogUtil.start(book);
                book.transcribe();
            } catch (Exception ex) {
                logger.warn("Could not transcribe book", ex);
            } finally {
                LogUtil.stopBook();
            }

            return null;
        }

        @Override
        protected void finished ()
        {
            StubsController.getInstance().callAboutStub(stub);
        }
    }

    //---------------------//
    // TranscribeSheetTask //
    //---------------------//
    private static class TranscribeSheetTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Sheet sheet;

        //~ Constructors ---------------------------------------------------------------------------
        public TranscribeSheetTask (Sheet sheet)
        {
            this.sheet = sheet;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            try {
                LogUtil.start(sheet.getStub());
                sheet.getStub().transcribe();
            } catch (Exception ex) {
                logger.warn("Could not transcribe sheet", ex);
            } finally {
                LogUtil.stopStub();
            }

            return null;
        }
    }
}
