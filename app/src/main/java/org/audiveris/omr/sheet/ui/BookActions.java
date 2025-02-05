//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B o o k A c t i o n s                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.Main;
import org.audiveris.omr.OMR;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.ui.SampleBrowser;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.plugin.Plugin;
import org.audiveris.omr.plugin.PluginsManager;
import org.audiveris.omr.score.ui.BookParameters;
import org.audiveris.omr.score.ui.SheetScaling;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.sheet.ExportPattern;
import org.audiveris.omr.sheet.ScaleBuilder;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.grid.StaffProjector;
import org.audiveris.omr.sheet.stem.StemScaler;
import org.audiveris.omr.sig.ui.InterController;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.util.OmrFileFilter;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.util.UserOpt;
import org.audiveris.omr.ui.util.WaitingTask;
import org.audiveris.omr.ui.view.HistoryMenu;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.PathListTask;
import org.audiveris.omr.util.SheetPath;
import org.audiveris.omr.util.SheetPathHistory;
import org.audiveris.omr.util.VoidTask;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.param.Param;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationAction;
import org.jdesktop.application.ApplicationActionMap;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.text.MessageFormat.format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Class <code>BookActions</code> gathers all UI actions related to current book.
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

    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(BookActions.class);

    private static final String doYouConfirm = "\n" + resources.getString("doYouConfirm");

    /** Default parameter. */
    public static final Param<Boolean> defaultPromptOnClosingUnsaved = new PromptOnClosingUnsaved();

    //~ Instance fields ----------------------------------------------------------------------------

    /** Sub-menu on images history. */
    private final HistoryMenu imageHistoryMenu;

    /** Sub-menu on books history. */
    private final HistoryMenu bookHistoryMenu;

    /** The action that toggles repetitive input mode. */
    private final ApplicationAction repetitiveInputAction;

    /** The action that toggles sheet validity. */
    private final ApplicationAction toggleValidityAction;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BookActions object.
     */
    private BookActions ()
    {
        final BookManager mgr = BookManager.getInstance();
        imageHistoryMenu = new HistoryMenu(mgr.getImageHistory(), LoadImageTask.class);
        bookHistoryMenu = new HistoryMenu(mgr.getBookHistory(), LoadBookTask.class);

        ApplicationActionMap actionMap = OmrGui.getApplication().getContext().getActionMap(this);
        repetitiveInputAction = (ApplicationAction) actionMap.get("toggleRepetitiveInput");
        toggleValidityAction = (ApplicationAction) actionMap.get("toggleSheetValidity");
        updateSheetValidity(null);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // annotateBook //
    //--------------//
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> annotateBook (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if ((book == null) || !hasValidSelectedSheets(book)) {
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
                    book.annotate(book.getValidSelectedStubs());
                } catch (Throwable ex) {
                    logger.warn("Error in annotateBook {}", ex.toString(), ex);
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
                } catch (Throwable ex) {
                    logger.warn("Error in annotateSheet {}", ex.toString(), ex);
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

    //-------------------//
    // browseBookSamples //
    //-------------------//
    /**
     * Action to browse the separate sample repository of the current book.
     *
     * @param e the event that triggered this action
     * @return the UI task to perform
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public Task browseBookSamples (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book != null) {
            if (book.hasSpecificRepository()) {
                return new SampleBrowser.Waiter(resources.getString("launchingBookSampleBrowser"))
                {
                    @Override
                    protected SampleBrowser doInBackground ()
                        throws Exception
                    {
                        return SampleBrowser.getInstance(book);
                    }
                };
            } else {
                logger.info(format(resources.getString("noBookRepo.pattern"), book.getRadix()));
            }
        }

        return null;
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
                resources.getString("chooseBookPrint"));
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
                resources.getString("chooseSheetPrint"));
    }

    //-----------//
    // closeBook //
    //-----------//
    /**
     * Action that handles the closing of the current book.
     *
     * @param e the event that triggered this action
     * @return the task which will close the book
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public Task<Void, Void> closeBook (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub != null) {
            final Book book = stub.getBook();

            if (checkStored(book)) {
                // Pre-select the suitable "next" book tab, if any.
                StubsController.getInstance().selectOtherBook(book);

                // Now close the book (+ related tab)
                return new CloseBookTask(stub);
            }
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

    //--------------------//
    // defineSheetScaling //
    //--------------------//
    /**
     * Launch the dialog to check and modify sheet scaling data.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_AVAILABLE)
    public void defineSheetScaling (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        try {
            final WrappedBoolean apply = new WrappedBoolean(false);
            final SheetScaling sheetScaling = new SheetScaling(stub.getSheet());
            final JOptionPane optionPane = new JOptionPane(
                    sheetScaling.getComponent(),
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
            final String frameTitle = sheetScaling.getTitle();
            final JDialog dialog = new JDialog(OMR.gui.getFrame(), frameTitle, true); // Modal flag
            dialog.setContentPane(optionPane);
            dialog.setName("SheetScalingDialog"); // For SAF life cycle

            optionPane.addPropertyChangeListener( (PropertyChangeEvent e1) -> {
                String prop = e1.getPropertyName();
                if (dialog.isVisible() && (e1.getSource() == optionPane) && (prop.equals(
                        JOptionPane.VALUE_PROPERTY))) {
                    Object obj = optionPane.getValue();
                    int value = (Integer) obj;
                    apply.set(value == JOptionPane.OK_OPTION);

                    // Exit only if user gives up or enters correct data
                    if (!apply.isSet() || sheetScaling.commit()) {
                        dialog.setVisible(false);
                        dialog.dispose();
                    } else {
                        // Incorrect data, so don't exit yet
                        try {
                            // TODO: Is there a more civilized way?
                            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                        } catch (Exception ignored) {}
                    }
                }
            });

            dialog.pack();
            OmrGui.getApplication().show(dialog);
        } catch (Throwable ex) {
            logger.warn("Error in defineSheetScaling {}", ex.toString(), ex);
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
    @Action(enabledProperty = BINARY_AVAILABLE)
    public void displayBinary (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        try {
            if (stub.isDone(OmrStep.BINARY)) {
                final SheetAssembly assembly = stub.getAssembly();
                final SheetTab tab = SheetTab.BINARY_TAB;

                if (assembly.getView(tab.label) == null) {
                    stub.getSheet().createBinaryView();
                } else {
                    assembly.selectViewTab(tab);
                }
            } else {
                logger.info(resources.getString("noBinaryImage"));
            }
        } catch (Throwable ex) {
            logger.warn("Error in displayBinary {}", ex.toString(), ex);
        }
    }

    //-------------//
    // displayData //
    //-------------//
    /**
     * Action that allows to display the view on data.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = GRID_AVAILABLE)
    public void displayData (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        try {
            if (stub.isDone(OmrStep.GRID)) {
                final SheetAssembly assembly = stub.getAssembly();
                final SheetTab tab = SheetTab.DATA_TAB;

                if (assembly.getView(tab.label) == null) {
                    stub.getSheet().displayDataTab();
                } else {
                    assembly.selectViewTab(tab);
                }
            } else {
                logger.info(resources.getString("noDataTab"));
            }
        } catch (Throwable ex) {
            logger.warn("Error in displayData {}", ex.toString(), ex);
        }
    }

    //-------------//
    // displayGray //
    //-------------//
    /**
     * Action that allows to display the view on initial gray image.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = GRAY_AVAILABLE)
    public void displayGray (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();
        final SheetAssembly assembly = stub.getAssembly();
        final SheetTab tab = SheetTab.GRAY_TAB;

        if (assembly.getView(tab.label) == null) {
            stub.getSheet().createGrayView();
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
    @Action(enabledProperty = GRID_AVAILABLE)
    public void displayNoStaff (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub.isDone(OmrStep.GRID)) {
            final SheetAssembly assembly = stub.getAssembly();
            final SheetTab tab = SheetTab.NO_STAFF_TAB;

            if (assembly.getView(tab.label) == null) {
                Sheet sheet = stub.getSheet(); // This may load the sheet...
                assembly.addViewTab(
                        tab,
                        new ScrollImageView(sheet, new NoStaffView(sheet)),
                        new BoardsPane(new PixelBoard(sheet)));
            } else {
                assembly.selectViewTab(tab);
            }
        } else {
            logger.info(resources.getString("noStaffLines"));
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
    @Action(enabledProperty = GRID_AVAILABLE)
    public void displayStaffLineGlyphs (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub.isDone(OmrStep.GRID)) {
            final SheetAssembly assembly = stub.getAssembly();
            final SheetTab tab = SheetTab.STAFF_LINE_TAB;

            if (assembly.getView(tab.label) == null) {
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
            logger.info(resources.getString("noStaffLines"));
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
     * Action to dump the content of all event services
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
     * Export the current book using MusicXML format
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> exportBook (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if ((book == null) || !hasValidSelectedSheets(book)) {
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
     * Export the current book, using MusicXML format, to a user-provided location.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> exportBookAs (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if ((book == null) || !hasValidSelectedSheets(book)) {
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
                resources.getString("chooseBookExport"));

        if ((bookPath == null) || !isTargetConfirmed(bookPath)) {
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
                resources.getString("chooseSheetExport"));

        if ((sheetPath == null) || !isTargetConfirmed(sheetPath)) {
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

    //------------------------//
    // hasValidSelectedSheets //
    //------------------------//
    private boolean hasValidSelectedSheets (Book book)
    {
        final List<SheetStub> stubs = book.getValidSelectedStubs();

        if (stubs.isEmpty()) {
            logger.warn("No valid selected sheets in {}", book);

            return false;
        } else {
            return true;
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

    //-----------//
    // openBooks //
    //-----------//
    /**
     * Action that let the user select one or several books.
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public LoadFilesTask openBooks (ActionEvent e)
    {
        final Path[] paths = selectBookPaths(false, Paths.get(BookManager.getDefaultBookFolder()));

        if (paths.length > 0) {
            return new LoadFilesTask(Arrays.asList(paths));
        }

        return null;
    }

    //----------------//
    // openImageFiles //
    //----------------//
    /**
     * Action that let the user select one or several image files interactively.
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public LoadFilesTask openImageFiles (ActionEvent e)
    {
        final Path[] paths = selectImagePaths();

        if (paths.length > 0) {
            return new LoadFilesTask(Arrays.asList(paths));
        }

        return null;
    }

    //----------------//
    // openRecentBook //
    //----------------//
    /**
     * Action that let the user open the book most recently closed.
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public LoadBookTask openRecentBook (ActionEvent e)
    {
        final SheetPathHistory bookHistory = BookManager.getInstance().getBookHistory();

        while (!bookHistory.isEmpty()) {
            final SheetPath sheetPath = bookHistory.getFirst();

            if (sheetPath != null) {
                final Path path = sheetPath.getBookPath();

                if (Files.exists(path)) {
                    return new LoadBookTask(sheetPath);
                } else {
                    logger.warn(format(resources.getString("fileNotFound.pattern"), path));
                    bookHistory.remove(sheetPath);
                }
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
            if (stub.isDone(OmrStep.BINARY)) {
                new ScaleBuilder(stub.getSheet()).displayChart();
            } else {
                logger.info(resources.getString("noScaleData"));
            }
        }
    }

    //------------//
    // plotStaves //
    //------------//
    /**
     * Action that allows to display the horizontal projection of a selected staff.
     * <p>
     * We need a sub-menu to select proper staff.
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
            logger.info(resources.getString("noStaffData"));

            return;
        }

        final JPopupMenu popup = new JPopupMenu("Staves IDs");

        // Menu title
        final JMenuItem title = new JMenuItem("Select staff ID:");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setEnabled(false);
        popup.add(title);
        popup.addSeparator();

        final ActionListener listener = (ActionEvent e1) -> {
            final int index = Integer.decode(e1.getActionCommand()) - 1;
            final Staff staff = staffManager.getStaff(index);
            new StaffProjector(sheet, staff, null).plot();
        };

        // Populate popup
        for (Staff staff : staffManager.getStaves()) {
            final JMenuItem item = new JMenuItem("" + staff.getId());
            item.addActionListener(listener);
            popup.add(item);
        }

        // Display popup menu
        final JFrame frame = OMR.gui.getFrame();
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

        if (stub.isDone(OmrStep.STEM_SEEDS)) {
            new StemScaler(stub.getSheet()).displayChart();
        } else {
            logger.info(resources.getString("noStemData"));
        }
    }

    //-----------//
    // printBook //
    //-----------//
    /**
     * Print the current book, as a PDF file
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> printBook (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if ((book == null) || !hasValidSelectedSheets(book)) {
            return null;
        }

        final Path bookPrintPath = BookManager.getDefaultPrintPath(book);

        if ((bookPrintPath != null) && isTargetConfirmed(bookPrintPath)) {
            return new PrintBookTask(book, bookPrintPath);
        }

        return printBookAs(e);
    }

    //-------------//
    // printBookAs //
    //-------------//
    /**
     * Write the current book, using PDF format, to a user-provided file.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> printBookAs (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if ((book == null) || !hasValidSelectedSheets(book)) {
            return null;
        }

        // Select target book print path
        final Path bookPrintPath = choosePrintPath(book, "");

        if ((bookPrintPath == null) || !isTargetConfirmed(bookPrintPath)) {
            return null;
        }

        return new PrintBookTask(book, bookPrintPath);
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

        if ((sheetPrintPath == null) || !isTargetConfirmed(sheetPrintPath)) {
            return null;
        }

        return new PrintSheetTask(stub.getSheet(), sheetPrintPath);
    }

    //---------------//
    // rebuildScores //
    //---------------//
    /**
     * Action to rebuild book score(s) from scratch.
     */
    @Action(enabledProperty = BOOK_IDLE)
    public void rebuildScores ()
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return;
        }

        final String msg = format(resources.getString("rebuildScores.pattern"), book.getRadix());

        if (!OMR.gui.displayConfirmation(msg + doYouConfirm)) {
            return;
        }

        book.clearScores();
        book.updateScores(null);
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
    private Task<Void, Void> resetBook (OmrStep step)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        final String msg = format(
                resources.getString("resetBookToEnd.pattern"),
                book.getRadix(),
                step);

        if (!OMR.gui.displayConfirmation(msg + doYouConfirm)) {
            return null;
        }

        return new ResetBookTask(book, step);
    }

    //-------------------//
    // resetBookToBinary //
    //-------------------//
    /**
     * Action that resets to BINARY all (valid selected) sheets of current book.
     *
     * @param e the event that triggered this action
     * @return the background task
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> resetBookToBinary (ActionEvent e)
    {
        return resetBook(OmrStep.BINARY);
    }

    //-----------------//
    // resetBookToGray //
    //-----------------//
    /**
     * Action that resets all (valid selected) sheets of current book to gray.
     *
     * @param e the event that triggered this action
     * @return the background task
     */
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> resetBookToGray (ActionEvent e)
    {
        // Check book input path still exists
        final Book book = StubsController.getCurrentBook();

        if (book != null) {
            final Path inputPath = book.getInputPath(); // Null for a compound book

            if ((inputPath != null) && !Files.exists(inputPath)) {
                OMR.gui.displayWarning("Cannot find " + inputPath, "Source images not available");
                return null;
            }
        }

        return resetBook(OmrStep.LOAD);
    }

    //------------//
    // sampleBook //
    //------------//
    @Action(enabledProperty = BOOK_IDLE)
    public Task<Void, Void> sampleBook (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if ((book == null) || !hasValidSelectedSheets(book)) {
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
     * Action to save the internals of the current book.
     *
     * @param e the event that triggered this action
     * @return the UI task to perform
     */
    @Action(enabledProperty = BOOK_MODIFIED_OR_UPGRADED)
    public Task<Void, Void> saveBook (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        try {
            final Path bookPath = BookManager.getDefaultSavePath(book);

            if ((book.getBookPath() != null) && (bookPath.toAbsolutePath().equals(
                    book.getBookPath().toAbsolutePath()) || isTargetConfirmed(bookPath))) {
                return new StoreBookTask(book, bookPath);
            }

            return saveBookAs(e);
        } catch (Throwable ex) {
            logger.warn("Error in saveBook {}", ex.toString(), ex);
        }

        return null;
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

        try {
            // Let the user select a book output file
            final Path defaultBookPath = BookManager.getDefaultSavePath(book);
            final Path targetPath = selectBookPath(true, defaultBookPath);
            final Path ownPath = book.getBookPath();

            if ((targetPath != null) && (((ownPath != null) && ownPath.toAbsolutePath().equals(
                    targetPath.toAbsolutePath())) || isTargetConfirmed(targetPath))) {
                return new StoreBookTask(book, targetPath);
            }
        } catch (Throwable ex) {
            logger.warn("Error in saveBookAs {}", ex.toString(), ex);
        }

        return null;
    }

    //--------------------//
    // saveBookRepository //
    //--------------------//
    /**
     * Action to save the separate repository of the current book.
     *
     * @param e the event that triggered this action
     * @return the UI task to perform
     */
    @Action(enabledProperty = BOOK_MODIFIED_OR_UPGRADED)
    public Task<Void, Void> saveBookRepository (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        try {
            if (book.hasAllocatedRepository()) {
                SampleRepository repo = book.getSampleRepository();

                if (repo.isModified()) {
                    repo.storeRepository();
                }
            }
        } catch (Throwable ex) {
            logger.warn("Error in saveBookRepository {}", ex.toString(), ex);
        }

        return null;
    }

    //--------------//
    // selectSheets //
    //--------------//
    /**
     * Specify sheets selection.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = BOOK_IDLE)
    public void selectSheets (ActionEvent e)
    {
        Book book = StubsController.getCurrentBook();

        if (book == null) {
            return;
        }

        try {
            if (new StubsSelection(book).getSheetsSpec()) {
                book.clearScores();
            }
        } catch (Throwable ex) {
            logger.warn("Error in selectSheets {}", ex.toString(), ex);
        }
    }

    //---------------//
    // splitAndMerge //
    //---------------//
    /**
     * Action that let the user build a new book from various pieces.
     *
     * @param e the event that triggered this action
     */
    @Action
    public void splitAndMerge (ActionEvent e)
    {
        OmrGui.getApplication().show(new SplitAndMerge(null).getComponent());
    }

    //---------------//
    // splitAndMerge //
    //---------------//
    /**
     * Action that let the user build a new book from various pieces.
     *
     * @param playListPath path to the provided playList to start with
     */
    public void splitAndMerge (Path playListPath)
    {
        OmrGui.getApplication().show(new SplitAndMerge(playListPath).getComponent());
    }

    //-------------------//
    // stopTranscription //
    //-------------------//
    /**
     * Stop the book transcription ASAP.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = BOOK_PAUSABLE)
    public void stopTranscription (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book == null) {
            return;
        }

        book.setPauseRequired(true);
        logger.info("Pause required for book {} ...", book.getRadix());
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

        try {
            book.swapAllSheets();
        } catch (Throwable ex) {
            logger.warn("Error in swapSheets {}", ex.toString(), ex);
        }
    }

    //-----------------------//
    // toggleRepetitiveInput //
    //-----------------------//
    /**
     * Toggle repetitive input mode.
     * <p>
     * When this mode is active, focus is on manual insertion of items, with as few user actions as
     * possible.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = REPETITIVE_INPUT_SELECTABLE)
    public void toggleRepetitiveInput (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return;
        }

        final SheetEditor sheetEditor = stub.getSheet().getSheetEditor();
        sheetEditor.toggleRepetitiveInputMode();
        repetitiveInputAction.setSelected(sheetEditor.isRepetitiveInputMode());
    }

    //---------------------//
    // toggleSheetValidity //
    //---------------------//
    /**
     * Action that toggles the validity of currently selected sheet.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = STUB_IDLE)
    public void toggleSheetValidity (ActionEvent e)
    {
        final StubsController controller = StubsController.getInstance();
        final SheetStub stub = controller.getSelectedStub();

        if (stub != null) {
            final boolean isValid = stub.isValid();
            final String pattern = resources.getString(
                    isValid ? "setSheetInvalid.pattern" : "setSheetValid.pattern");
            final String msg = format(pattern, stub.getId());

            if (!OMR.gui.displayConfirmation(msg + doYouConfirm)) {
                return;
            }

            if (isValid) {
                stub.invalidate();

                if (ViewParameters.getInstance().isInvalidSheetDisplay() == false) {
                    controller.removeAssembly(stub);
                }
            } else {
                stub.validate();
            }
        }
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

        boolean swap = false;

        if (swapProcessedSheets() || Main.getCli().isSwap()) {
            // Make sure we will be able to save book incrementally
            final Book book = stub.getBook();
            final Path defPath = BookManager.getDefaultSavePath(book);
            final Path bookPath = book.getBookPath();

            if (((bookPath != null) && (defPath.toAbsolutePath().equals(bookPath.toAbsolutePath()))
                    || isTargetConfirmed(defPath))) {
                swap = true;
                logger.info("Processed sheets will be swapped out");
            } else {
                logger.info("No swap out for processed sheets");
            }
        }

        return new TranscribeBookTask(stub, swap);
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

    //-----------------------//
    // updateRepetitiveInput //
    //-----------------------//
    /**
     * Update checkbox of repetitiveInput mode according to the current sheet
     *
     * @param sheet the current sheet
     */
    public void updateRepetitiveInput (Sheet sheet)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == sheet.getStub()) {
            repetitiveInputAction.setSelected(sheet.getSheetEditor().isRepetitiveInputMode());
        }
    }

    //---------------------//
    // updateSheetValidity //
    //---------------------//
    /**
     * Update menu item according to sheet validity status.
     *
     * @param stub the sheet stub to process
     */
    public final void updateSheetValidity (SheetStub stub)
    {
        if (stub == null) {
            toggleValidityAction.putValue(
                    javax.swing.Action.SMALL_ICON,
                    resources.getImageIcon("toggleSheetValidity.Action.icon.true"));
            toggleValidityAction.putValue(
                    javax.swing.Action.NAME,
                    resources.getString("toggleSheetValidity.Action.text.none"));
            toggleValidityAction.putValue(
                    javax.swing.Action.SHORT_DESCRIPTION,
                    resources.getString("toggleSheetValidity.Action.shortDescription.none"));
        } else if (stub == StubsController.getCurrentStub()) {
            final boolean isValid = stub.isValid();
            toggleValidityAction.putValue(
                    javax.swing.Action.SMALL_ICON,
                    resources.getImageIcon("toggleSheetValidity.Action.icon." + isValid));
            toggleValidityAction.putValue(
                    javax.swing.Action.NAME,
                    resources.getString("toggleSheetValidity.Action.text." + isValid));
            toggleValidityAction.putValue(
                    javax.swing.Action.SHORT_DESCRIPTION,
                    resources.getString("toggleSheetValidity.Action.shortDescription." + isValid));
        }
    }

    //-------------//
    // upgradeBook //
    //-------------//
    /**
     * Complete the upgrade of all sheets.
     *
     * @param e the event that triggered this action
     * @return the task to launch in background
     */
    @Action(enabledProperty = BOOK_UPGRADABLE)
    public Task<Void, Void> upgradeBook (ActionEvent e)
    {
        final SheetStub stub = StubsController.getCurrentStub();

        if (stub == null) {
            return null;
        }

        return new UpgradeBookTask(stub.getBook());
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

        ScrollView scrollView = assembly.getSelectedScrollView();

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

        ScrollView scrollView = assembly.getSelectedScrollView();

        if (scrollView == null) {
            return;
        }

        scrollView.fitWidth();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------------//
    // applyUserSettings //
    //-------------------//
    /**
     * Prompt the user for interactive confirmation or modification of book/sheet
     * parameters.
     *
     * @param stub the current sheet stub, or null
     */
    public static void applyUserSettings (final SheetStub stub)
    {
        final Book book = (stub != null) ? stub.getBook() : null;

        if (book != null) {
            // Dialog already active?
            final JDialog activeDialog = book.getParameterDialog();

            if (activeDialog != null) {
                activeDialog.setVisible(true);

                return;
            }
        }

        // Create a brand new dialog
        try {
            final BookParameters bookParameters = new BookParameters(stub);
            final String frameTitle = bookParameters.getTitle();
            final JDialog dialog = new JDialog(OMR.gui.getFrame(), frameTitle, false); // Non modal

            // For SAF life cycle (to save dialog size and location across application runs)
            dialog.setName("ScoreParamsDialog");

            // To avoid memory leak when user closes window via the upper right cross
            dialog.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing (WindowEvent we)
                {
                    if (book != null) {
                        book.setParameterDialog(null);
                    }
                    dialog.dispose();
                }
            });

            if (book != null) {
                book.setParameterDialog(dialog);
            }

            // User actions on buttons OK, Apply, Cancel
            final JOptionPane optionPane = new JOptionPane(
                    bookParameters.getComponent(),
                    JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.DEFAULT_OPTION,
                    null,
                    new Object[] { UserOpt.OK, UserOpt.Apply, UserOpt.Cancel });
            optionPane.addPropertyChangeListener(e -> {
                if (dialog.isVisible() && (e.getSource() == optionPane) && (e.getPropertyName()
                        .equals(JOptionPane.VALUE_PROPERTY))) {
                    final Object choice = optionPane.getValue();
                    final boolean exit;

                    if (choice == UserOpt.Cancel) {
                        exit = true;
                    } else if (choice == UserOpt.Apply) {
                        bookParameters.commit(book);
                        exit = false;
                    } else if (choice == UserOpt.OK) {
                        exit = bookParameters.commit(book);
                    } else {
                        exit = false;
                    }

                    if (exit) {
                        if (book != null) {
                            book.setParameterDialog(null);
                        }
                        dialog.setVisible(false);
                        dialog.dispose();
                    } else {
                        optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                    }
                }
            });

            dialog.setContentPane(optionPane);
            dialog.pack();
            OmrGui.getApplication().show(dialog);
        } catch (Exception ex) {
            logger.warn("Error in BookParameters {}", ex.toString(), ex);
        }
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
        //            return true;
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
     * @param sheet the provided sheet
     * @return true if OK, false otherwise
     */
    public static boolean checkParameters (Sheet sheet)
    {
        //        if (constants.promptParameters.getValue()) {
        //            return applyUserSettings(sheet);
        //        } else {
        //            return true;
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
        final String bookStatus = book.isModified() ? resources.getString("modified")
                : (book.isUpgraded() ? resources.getString("upgraded") : null);

        if ((bookStatus != null) && defaultPromptOnClosingUnsaved.getValue()) {
            final int answer = JOptionPane.showConfirmDialog(
                    OMR.gui.getFrame(),
                    format(resources.getString("saveBook.pattern"), bookStatus, book.getRadix()));

            if (answer == JOptionPane.YES_OPTION) {
                Path bookPath;

                if (book.getBookPath() == null) {
                    // Find a suitable target file
                    bookPath = BookManager.getDefaultSavePath(book);

                    // Check the target is fine
                    if (!isTargetConfirmed(bookPath)) {
                        // Let the user select an alternate output file
                        bookPath = selectBookPath(true, BookManager.getDefaultSavePath(book));

                        if ((bookPath == null) || !isTargetConfirmed(bookPath)) {
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

    //--------//
    // filter //
    //--------//
    public static OmrFileFilter filter (String ext)
    {
        return new OmrFileFilter(ext, new String[] { ext });
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of BookActions in the application.
     *
     * @return the instance
     */
    public static BookActions getInstance ()
    {
        return LazySingleton.INSTANCE;
    }

    //-------------------//
    // isTargetConfirmed //
    //-------------------//
    /**
     * Check whether we have user confirmation to overwrite the target path.
     * <p>
     * This is a no-op if target does not already exist.
     *
     * @param target the path to be checked
     * @return false if explicitly not isTargetConfirmed, true otherwise
     */
    public static boolean isTargetConfirmed (Path target)
    {
        return (!Files.exists(target)) || OMR.gui.displayConfirmation(
                format(resources.getString("overwriteFile.pattern"), target));
    }

    //-----------------------//
    // preOpenBookParameters //
    //-----------------------//
    /**
     * Check whether we should pre-open book parameters dialog at any book creation.
     *
     * @return true if so
     */
    public static boolean preOpenBookParameters ()
    {
        return constants.preOpenBookParameters.isSet();
    }

    //------------------------//
    // selectBookOrImagePaths //
    //------------------------//
    /**
     * Let the user interactively select book/image paths for reading.
     *
     * @param startPath starting path
     * @return the array of selected paths, perhaps empty but not null
     */
    public static Path[] selectBookOrImagePaths (Path startPath)
    {
        final String suffixes = constants.validImageExtensions.getValue();
        final String allSuffixes = OMR.BOOK_EXTENSION + " " + suffixes + " " + suffixes
                .toUpperCase();
        final OmrFileFilter filter = new OmrFileFilter(
                resources.getString("bookAnd") + " " + resources.getString("majorImageFiles") + " ("
                        + OMR.BOOK_EXTENSION + " " + suffixes + ")",
                allSuffixes.split("\\s"));

        return selectPaths(false, startPath, filter);
    }

    //----------------//
    // selectBookPath //
    //----------------//
    /**
     * Let the user interactively select a book path.
     *
     * @param save      true for write, false for read
     * @param startPath starting path
     * @return the selected path or null
     */
    public static Path selectBookPath (boolean save,
                                       Path startPath)
    {
        return selectPath(save, startPath, filter(OMR.BOOK_EXTENSION));
    }

    //-----------------//
    // selectBookPaths //
    //-----------------//
    /**
     * Let the user interactively select book paths.
     *
     * @param save      true for write, false for read
     * @param startPath starting path
     * @return the array of selected paths, perhaps empty but not null
     */
    public static Path[] selectBookPaths (boolean save,
                                          Path startPath)
    {
        return selectPaths(save, startPath, filter(OMR.BOOK_EXTENSION));
    }

    //-----------------//
    // selectImagePath //
    //-----------------//
    public static Path selectImagePath ()
    {
        final String suffixes = constants.validImageExtensions.getValue();
        final String allSuffixes = suffixes + " " + suffixes.toUpperCase();
        final OmrFileFilter filter = new OmrFileFilter(
                resources.getString("majorImageFiles") + " (" + suffixes + ")",
                allSuffixes.split("\\s"));

        return selectPath(false, Paths.get(BookManager.getDefaultImageFolder()), filter);
    }

    //------------------//
    // selectImagePaths //
    //------------------//
    public static Path[] selectImagePaths ()
    {
        final String suffixes = constants.validImageExtensions.getValue();
        final String allSuffixes = suffixes + " " + suffixes.toUpperCase();
        final OmrFileFilter filter = new OmrFileFilter(
                resources.getString("majorImageFiles") + " (" + suffixes + ")",
                allSuffixes.split("\\s"));

        return selectPaths(false, Paths.get(BookManager.getDefaultImageFolder()), filter);
    }

    //------------//
    // selectPath //
    //------------//
    /**
     * Let the user interactively select a path.
     *
     * @param save      true for write, false for read
     *                  (for read, path will be checked to exist and not be a directory)
     * @param startPath the starting path
     * @param filter    filter on file extensions
     * @return the selected path or null
     */
    public static Path selectPath (boolean save,
                                   Path startPath,
                                   OmrFileFilter filter)
    {
        final Path path = UIUtil.pathChooser(save, OMR.gui.getFrame(), startPath, filter);

        if (path == null) {
            return null;
        }

        if (save) {
            return path;
        }

        if (!Files.exists(path)) {
            logger.warn(format(resources.getString("fileNotFound.pattern"), path));
            return null;
        }

        if (Files.isDirectory(path)) {
            logger.warn(format(resources.getString("isDirectory.pattern"), path));
            return null;
        }

        return path;
    }

    //-------------//
    // selectPaths //
    //-------------//
    /**
     * Let the user interactively select an array of paths.
     *
     * @param save      true for write, false for read
     *                  (for read, path will be checked to exist and not be a directory)
     * @param startPath the starting path
     * @param filter    filter on file extensions
     * @return the array of selected paths, perhaps empty but not null
     */
    public static Path[] selectPaths (boolean save,
                                      Path startPath,
                                      OmrFileFilter filter)
    {
        final Path[] paths = UIUtil.pathsChooser(save, OMR.gui.getFrame(), startPath, filter);

        if (paths.length == 0) {
            return paths;
        }

        if (save) {
            return paths;
        }

        for (Path path : paths) {
            if (!Files.exists(path)) {
                logger.warn(format(resources.getString("fileNotFound.pattern"), path));
                return new Path[0];
            }

            if (Files.isDirectory(path)) {
                logger.warn(format(resources.getString("isDirectory.pattern"), path));
                return new Path[0];
            }
        }

        return paths;
    }

    //---------------------//
    // swapProcessedSheets //
    //---------------------//
    /**
     * Report whether we should swap out any processed sheet.
     *
     * @return true if so
     */
    public static boolean swapProcessedSheets ()
    {
        return constants.swapProcessedSheets.isSet();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------------//
    // CloseBookTask //
    //---------------//
    private static class CloseBookTask
            extends VoidTask
    {
        final SheetStub stub;

        /**
         * Create an asynchronous task to close the book.
         *
         * @param stub the current stub of the book to close
         */
        CloseBookTask (SheetStub stub)
        {
            this.stub = stub;
        }

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            final Book book = stub.getBook();

            try {
                LogUtil.start(book);
                book.close(stub.getNumber());
            } catch (Throwable ex) {
                logger.warn("Error in CloseBookTask {}", ex.toString(), ex);
            } finally {
                LogUtil.stopBook();
            }

            return null;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.String validImageExtensions = new Constant.String(
                ".bmp .gif .jpg .jpeg .png .tiff .tif .pdf",
                "Valid image file extensions, whitespace-separated");

        private final Constant.Boolean closeConfirmation = new Constant.Boolean(
                true,
                "Should we ask confirmation for closing an unsaved book?");

        private final Constant.Boolean preOpenBookParameters = new Constant.Boolean(
                false,
                "Automatically open book parameters dialog at book creation?");

        private final Constant.Boolean swapProcessedSheets = new Constant.Boolean(
                false,
                "Automatically swap out sheets once they are processed?");
    }

    //----------------//
    // ExportBookTask //
    //----------------//
    private static class ExportBookTask
            extends VoidTask
    {
        final Book book;

        /**
         * Create an asynchronous task to export the book.
         *
         * @param book            the book to export
         * @param bookPathSansExt (non-null) the target export book path with no extension
         */
        ExportBookTask (Book book,
                        Path bookPathSansExt)
        {
            this.book = book;
            book.setExportPathSansExt(bookPathSansExt);
        }

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                LogUtil.start(book);

                if (checkParameters(book)) {
                    book.export(book.getValidSelectedStubs(), book.getScores());
                }
            } catch (Throwable ex) {
                logger.warn("Error in ExportBookTask {}", ex.toString(), ex);
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
        final Sheet sheet;

        final Path sheetExportPath;

        ExportSheetTask (Sheet sheet,
                         Path sheetExportPath)
        {
            this.sheet = sheet;
            this.sheetExportPath = sheetExportPath;
        }

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                LogUtil.start(sheet.getStub());

                if (checkParameters(sheet)) {
                    sheet.getStub().reachStep(OmrStep.PAGE, false);
                    sheet.export(sheetExportPath);
                }
            } catch (Throwable ex) {
                logger.warn("Error in ExportSheetTask {}", ex.toString(), ex);
            } finally {
                LogUtil.stopStub();
            }

            return null;
        }
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {
        static final BookActions INSTANCE = new BookActions();

        private LazySingleton ()
        {
        }
    }

    //--------------//
    // LoadBookTask //
    //--------------//
    /**
     * Task that loads a book (.omr project) file in background.
     * <p>
     * If load is successful, GUI allocates all stub tabs and then focuses on:
     * <ol>
     * <li>Either the desired stub if its number is provided,
     * <li>Or the first valid stub encountered.
     * </ol>
     */
    public static class LoadBookTask
            extends WaitingTask<Book, Void>
    {
        /** Underlying path. */
        protected Path path;

        /** Desired sheet number, if any. */
        protected Integer sheetNumber;

        // Constructor needed for creation of HistoryMenu
        public LoadBookTask ()
        {
            super(OmrGui.getApplication(), "Loading");
        }

        /**
         * Creates a new <code>LoadBookTask</code>object, with a plain book path
         *
         * @param path plain book path
         */
        public LoadBookTask (Path path)
        {
            super(OmrGui.getApplication(), "Loading " + path + " ...");
            this.path = path;
        }

        /**
         * Creates a new <code>LoadBookTask</code>object, with a desired sheet number.
         *
         * @param sheetPath the desired sheet number to focus upon
         */
        public LoadBookTask (SheetPath sheetPath)
        {
            super(OmrGui.getApplication(), "Loading " + sheetPath.getBookPath() + " ...");
            path = sheetPath.getBookPath();
            sheetNumber = sheetPath.getSheetNumber();
        }

        @Override
        protected Book doInBackground ()
            throws Exception
        {
            Book book = null;

            try {
                // Actually open the book zip system
                book = OMR.engine.loadBook(path);
            } catch (Throwable ex) {
                logger.warn("Error in {} {}", getClass().getSimpleName(), ex.toString(), ex);
            }

            return book;
        }

        /**
         * Set the path value.
         *
         * @param path the path used by the task
         */
        public void setPath (Path path)
        {
            this.path = path;
        }

        /**
         * Set the sheet path value (path#sheet) .
         *
         * @param sheetPath the sheet path used by the task
         */
        public void setPath (SheetPath sheetPath)
        {
            setPath(sheetPath.getBookPath());
            sheetNumber = sheetPath.getSheetNumber();
        }

        @Override
        protected void succeeded (Book book)
        {
            if (book != null) {
                final StubsController controller = StubsController.getInstance();

                // Insert all tabs in GUI stubsPane
                controller.displayStubs(book, sheetNumber);

                // Select stub
                final SheetStub stub = (sheetNumber != null) ? book.getStub(sheetNumber)
                        : book.getFirstValidStub();
                controller.selectAssembly(stub);
            }
        }
    }

    //---------------//
    // LoadFilesTask //
    //---------------//
    /**
     * Task that loads a list of files (possibly a mix of book files and input files).
     */
    public static class LoadFilesTask
            extends PathListTask<List<Book>, Void>
    {
        public LoadFilesTask (Collection<? extends Path> paths)
        {
            super(paths);
        }

        @Override
        protected List<Book> doInBackground ()
            throws Exception
        {
            final List<Book> bookList = new ArrayList<>();

            for (Path path : pathList) {
                Book book = null;

                try {
                    final String ext = FileUtil.getExtension(path.getFileName());

                    if (ext.equalsIgnoreCase(OMR.BOOK_EXTENSION)) {
                        // Book file
                        book = OMR.engine.loadBook(path);
                    } else {
                        // Assumed image file
                        book = OMR.engine.loadInput(path);
                        book.createStubs(); // Read just the count of sheets in input file
                    }
                } catch (Throwable ex) {
                    logger.warn("Error in {} {}", getClass().getSimpleName(), ex.toString(), ex);
                }

                bookList.add(book); // book can be null
            }

            return bookList;
        }

        @Override
        protected void succeeded (List<Book> bookList)
        {
            final StubsController controller = StubsController.getInstance();

            for (Book book : bookList) {
                if (book != null) {
                    try {
                        // Insert all tabs in GUI stubsPane
                        controller.displayStubs(book, null);

                        // Select stub
                        controller.selectAssembly(book.getFirstValidStub());

                        if (book.isImage()) {
                            // Pre-open Book Parameters dialog on this brand new book?
                            if (preOpenBookParameters()) {
                                BookActions.applyUserSettings(book.getFirstValidStub());
                            }
                        }
                    } catch (Throwable ex) {
                        logger.warn(
                                "Error in {} {}",
                                getClass().getSimpleName(),
                                ex.toString(),
                                ex);
                    }
                }
            }
        }
    }

    //---------------//
    // LoadImageTask //
    //---------------//
    /**
     * Task that opens an image file.
     */
    public static class LoadImageTask
            extends LoadBookTask
    {
        // Constructor needed for creation of HistoryMenu
        public LoadImageTask ()
        {
        }

        /**
         * Creates a <code>LoadBookTask</code> object on an input file assumed to exist.
         *
         * @param path the path to input file
         */
        public LoadImageTask (Path path)
        {
            super(path);
        }

        @Override
        protected Book doInBackground ()
            throws InterruptedException
        {
            try {
                // Actually open the image file
                Book book = OMR.engine.loadInput(path);
                LogUtil.start(book);
                book.createStubs(); // Read just the count of sheets in input file

                return book;
            } catch (Exception ex) {
                logger.warn("Error opening path " + path + " " + ex, ex);
            } finally {
                LogUtil.stopBook();
            }

            return null;
        }

        @Override
        protected void succeeded (Book book)
        {
            if (book != null) {
                try {
                    super.succeeded(book);

                    // Pre-open Book Parameters dialog on this brand new book?
                    if (preOpenBookParameters()) {
                        BookActions.applyUserSettings(book.getFirstValidStub());
                    }
                } catch (Throwable ex) {
                    logger.warn("Error in {} {}", getClass().getSimpleName(), ex.toString(), ex);
                }
            }
        }
    }

    //---------------//
    // PrintBookTask //
    //---------------//
    public static class PrintBookTask
            extends VoidTask
    {
        final Book book;

        final Path bookPrintPath;

        public PrintBookTask (Book book,
                              Path bookPrintPath)
        {
            this.book = book;
            this.bookPrintPath = bookPrintPath;
        }

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                LogUtil.start(book);
                book.setPrintPath(bookPrintPath);
                book.print(book.getValidSelectedStubs());
            } catch (Throwable ex) {
                logger.warn("Error in PrintBookTask {}", ex.toString(), ex);
            } finally {
                LogUtil.stopBook();
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
        final Sheet sheet;

        final Path sheetPrintPath;

        public PrintSheetTask (Sheet sheet,
                               Path sheetPrintPath)
        {
            this.sheet = sheet;
            this.sheetPrintPath = sheetPrintPath;
        }

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                LogUtil.start(sheet.getStub());
                sheet.print(sheetPrintPath);
            } catch (Throwable ex) {
                logger.warn("Error in PrintSheetTask {}", ex.toString(), ex);
            } finally {
                LogUtil.stopStub();
            }

            return null;
        }
    }

    //------------------------//
    // PromptOnClosingUnsaved //
    //------------------------//
    private static class PromptOnClosingUnsaved
            extends Param<Boolean>
    {
        public PromptOnClosingUnsaved ()
        {
            super(GLOBAL_SCOPE);
        }

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
                logger.info(resources.getString("promptOnClose." + specific));

                return true;
            } else {
                return false;
            }
        }
    }

    //---------------//
    // ResetBookTask //
    //---------------//
    /**
     * Task that resets all valid selected sheets of book to gray or binary.
     */
    private class ResetBookTask
            extends VoidTask
    {
        final Book book;

        final OmrStep step;

        public ResetBookTask (Book book,
                              OmrStep step)
        {
            this.book = book;
            this.step = step;
        }

        @Override
        protected Void doInBackground ()
            throws Exception
        {
            book.resetTo(step);
            setBookTranscribable(true);
            setBookModifiedOrUpgraded(true);

            return null;
        }
    }

    //----------------//
    // SampleBookTask //
    //----------------//
    private static class SampleBookTask
            extends VoidTask
    {
        final Book book;

        SampleBookTask (Book book)
        {
            this.book = book;
        }

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                LogUtil.start(book);
                book.sample(book.getValidSelectedStubs());
            } catch (Throwable ex) {
                logger.warn("Error in SampleBookTask {}", ex.toString(), ex);
            } finally {
                LogUtil.stopBook();
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
        final Sheet sheet;

        public SampleSheetTask (Sheet sheet)
        {
            this.sheet = sheet;
        }

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                LogUtil.start(sheet.getStub());
                sheet.sample();
            } catch (Throwable ex) {
                logger.warn("Error in SampleSheetTask {}", ex.toString(), ex);
            } finally {
                LogUtil.stopStub();
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
        final Book book;

        final Path bookPath;

        /**
         * Create an asynchronous task to store the book.
         *
         * @param book     the book to export
         * @param bookPath (non-null) the target to store book path
         */
        StoreBookTask (Book book,
                       Path bookPath)
        {
            this.book = book;
            this.bookPath = bookPath;
        }

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                LogUtil.start(book);
                final Path oldBookPath = book.getBookPath();
                book.store(bookPath, false);
                BookActions.getInstance().setBookModifiedOrUpgraded(false);

                // Update books map
                OMR.engine.renameBook(book, oldBookPath);
            } catch (Throwable ex) {
                logger.warn("Error in StoreBookTask {}", ex.toString(), ex);
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
        private final SheetStub stub;

        private final Book book;

        private final boolean swap;

        TranscribeBookTask (SheetStub stub,
                            boolean swap)
        {
            this.stub = stub;
            this.book = stub.getBook();
            this.swap = swap;
        }

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                LogUtil.start(book);

                if (swap) {
                    book.store(BookManager.getDefaultSavePath(book), false);
                }

                book.transcribe(book.getValidSelectedStubs(), book.getScores(), swap);
            } catch (Throwable ex) {
                logger.warn("Could not transcribe book {}", ex.toString(), ex);
            } finally {
                LogUtil.stopBook();
            }

            return null;
        }

        @Override
        protected void succeeded (Void result)
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
        private final Sheet sheet;

        TranscribeSheetTask (Sheet sheet)
        {
            this.sheet = sheet;
        }

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                LogUtil.start(sheet.getStub());
                sheet.getStub().transcribe();
            } catch (Throwable ex) {
                logger.warn("Could not transcribe sheet {}", ex.toString(), ex);
            } finally {
                LogUtil.stopStub();
            }

            return null;
        }
    }

    //-----------------//
    // UpgradeBookTask //
    //-----------------//
    private static class UpgradeBookTask
            extends VoidTask
    {
        private final Book book;

        UpgradeBookTask (Book book)
        {
            this.book = book;
        }

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            try {
                LogUtil.start(book);
                book.upgradeStubs();
            } catch (Throwable ex) {
                logger.warn("Error while upgrading book {}", ex.toString(), ex);
            } finally {
                LogUtil.stopBook();
            }

            return null;
        }
    }
}
