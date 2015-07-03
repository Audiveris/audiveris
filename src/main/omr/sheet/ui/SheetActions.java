//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S h e e t A c t i o n s                                     //
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

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphRepository;

import omr.script.RemoveTask;

import omr.sheet.Book;
import omr.sheet.BookManager;
import omr.sheet.ScaleBuilder;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.StaffManager;
import omr.sheet.grid.StaffProjector;
import omr.sheet.stem.StemScaler;

import omr.step.Step;

import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtil;

import omr.util.BasicTask;
import omr.util.NameSet;
import omr.util.Param;

import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

/**
 * Class {@code SheetActions} simply gathers UI actions related to sheet handling.
 *
 * @author Hervé Bitteur
 */
public class SheetActions
        extends SheetDependent
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SheetActions.class);

    /** Singleton. */
    private static SheetActions INSTANCE;

    /** Default parameter. */
    public static final Param<Boolean> defaultPrompt = new Default();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SheetActions object.
     */
    public SheetActions ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
                // Find a suitable target file
                Path projectPath = BookManager.getDefaultProjectPath(book);

                // Check the target is fine
                if (!book.hasFileSystem()) {
                    if (!canWrite(projectPath)) {
                        // Let the user select an alternate output file
                        projectPath = selectProjectPath(
                                true,
                                BookManager.getDefaultProjectPath(book));

                        if (!canWrite(projectPath)) {
                            return false; // No suitable target found
                        }
                    }
                }

                // Save the project to target file
                try {
                    book.store(projectPath);

                    return true; // Project successfully saved
                } catch (Exception ex) {
                    logger.warn("Error saving book", ex);

                    return false; // Saving failed
                }
            }

            if (answer == JOptionPane.NO_OPTION) {
                // Here user specifically chose NOT to save the book
                return true;
            }

            // Here user said Oops!, cancelling the current close request
            return false;
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
    public static synchronized SheetActions getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SheetActions();
        }

        return INSTANCE;
    }

    //-----------//
    // closeBook //
    //-----------//
    /**
     * Action that handles the closing of the currently selected book.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void closeBook (ActionEvent e)
    {
        Book book = SheetsController.getCurrentBook();

        if (book != null) {
            if (checkStored(book)) {
                book.close();
            }
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
                return new OpenProjectTask(path.toFile());
            } else {
                logger.warn("File not found {}", path);
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
        File file = UIUtil.fileChooser(
                false,
                OMR.getGui().getFrame(),
                new File(BookManager.getDefaultInputFolder()),
                new OmrFileFilter(
                        "Major image files" + " (" + suffixes + ")",
                        allSuffixes.split("\\s")));

        if (file != null) {
            if (file.exists()) {
                return new OpenTask(file);
            } else {
                logger.warn("File not found {}", file);
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
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void plotScale (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            if (sheet.isDone(Step.SCALE)) {
                new ScaleBuilder(sheet).displayChart();
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
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void plotStaves (ActionEvent e)
    {
        final Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
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
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void plotStem (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            if (sheet.isDone(Step.STEM_SEEDS)) {
                new StemScaler(sheet).displayChart();
            } else {
                logger.warn("Cannot display stem plot, for lack of stem data");
            }
        }
    }

    //--------------//
    // recordGlyphs //
    //--------------//
    @Action(enabledProperty = SHEET_AVAILABLE)
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

    //-------------//
    // removeSheet //
    //-------------//
    /**
     * Action that handles the removal of the currently selected sheet.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void removeSheet (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            int answer = JOptionPane.showConfirmDialog(
                    OMR.getGui().getFrame(),
                    "Do you confirm the removal of this sheet from its containing score ?");

            if (answer == JOptionPane.YES_OPTION) {
                new RemoveTask(sheet).launch(sheet);
            }
        }
    }

    //-----------//
    // storeBook //
    //-----------//
    /**
     * Action to store the current of the currently selected book.
     *
     * @param e the event that triggered this action
     * @return the UI task to perform
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public Task<Void, Void> storeBook (ActionEvent e)
    {
        final Book book = SheetsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        // Ask user confirmation for overwriting if file already exists
        final Path projectPath = BookManager.getDefaultProjectPath(book);

        if (book.hasFileSystem() || canWrite(projectPath)) {
            return new StoreBookTask(book, projectPath);
        } else {
            return storeBookAs(e);
        }
    }

    //-------------//
    // storeBookAs //
    //-------------//
    @Action(enabledProperty = SHEET_AVAILABLE)
    public Task<Void, Void> storeBookAs (ActionEvent e)
    {
        final Book book = SheetsController.getCurrentBook();

        if (book == null) {
            return null;
        }

        // Let the user select a project output file
        final Path projectPath = selectProjectPath(true, BookManager.getDefaultProjectPath(book));

        if (canWrite(projectPath)) {
            return new StoreBookTask(book, projectPath);
        }

        return null;
    }

    //------------//
    // zoomHeight //
    //------------//
    /**
     * Action that allows to adjust the display zoom, so that the full
     * height is shown.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void zoomHeight (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet == null) {
            return;
        }

        SheetAssembly assembly = sheet.getAssembly();

        if (assembly == null) {
            return;
        }

        assembly.getSelectedView().fitHeight();
    }

    //-----------//
    // zoomWidth //
    //-----------//
    /**
     * Action that allows to adjust the display zoom, so that the full
     * width is shown.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void zoomWidth (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet == null) {
            return;
        }

        SheetAssembly assembly = sheet.getAssembly();

        if (assembly == null) {
            return;
        }

        assembly.getSelectedView().fitWidth();
    }

    //----------//
    // canWrite //
    //----------//
    /**
     * Report whether we are allowed to (over)write the provided file
     *
     * @param path the provided file path
     * @return true if allowed
     */
    private static boolean canWrite (Path path)
    {
        return (path != null)
               && (!Files.exists(path)
                   || OMR.getGui().displayConfirmation(path + " already exists. \nOverwrite?"));
    }

    //-------------------//
    // selectProjectPath //
    //-------------------//
    /**
     * Let the user interactively select a project file
     *
     * @param path default path
     * @param save true for write, false for read
     * @return
     */
    private static Path selectProjectPath (boolean save,
                                           Path path)
    {
        File file = UIUtil.fileChooser(
                save,
                OMR.getGui().getFrame(),
                path.toFile(),
                new OmrFileFilter("Audiveris project files", new String[]{OMR.PROJECT_EXTENSION}));

        return (file == null) ? null : file.toPath();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // HistoryMenu //
    //-------------//
    /**
     * Handles the menu of sheet history.
     */
    public static class HistoryMenu
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static HistoryMenu INSTANCE;

        //~ Instance fields ------------------------------------------------------------------------
        /** Concrete menu. */
        private JMenu menu;

        //~ Constructors ---------------------------------------------------------------------------
        private HistoryMenu ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        public static HistoryMenu getInstance ()
        {
            if (INSTANCE == null) {
                INSTANCE = new HistoryMenu();
            }

            return INSTANCE;
        }

        public JMenu getMenu ()
        {
            if (menu == null) {
                NameSet history = BookManager.getInstance().getHistory();
                menu = history.menu("Sheet History", new HistoryListener());
                menu.setEnabled(!history.isEmpty());

                menu.setName("historyMenu");

                ResourceMap resource = OMR.getApplication().getContext()
                        .getResourceMap(SheetActions.class);
                resource.injectComponents(menu);
            }

            return menu;
        }

        public void setEnabled (boolean bool)
        {
            getMenu().setEnabled(bool);
        }

        //~ Inner Classes --------------------------------------------------------------------------
        /**
         * Class {@code HistoryListener} is used to reload an image file,
         * when selected from the history of previous image files.
         */
        private static class HistoryListener
                implements ActionListener
        {
            //~ Methods ----------------------------------------------------------------------------

            @Override
            public void actionPerformed (ActionEvent e)
            {
                final String name = e.getActionCommand().trim();

                if (!name.isEmpty()) {
                    File file = new File(name);
                    new OpenTask(file).execute();
                }
            }
        }
    }

    //-----------------//
    // OpenProjectTask //
    //-----------------//
    /**
     * Task that opens a book project file.
     */
    public static class OpenProjectTask
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final File file;

        //~ Constructors ---------------------------------------------------------------------------
        public OpenProjectTask (File file)
        {
            this.file = file;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            if (file.exists()) {
                // Actually open the project
                Book book = OMR.getEngine().loadProject(file.toPath());

                if (book != null) {
                    // Show first sheet
                    List<Sheet> sheets = book.getSheets();

                    if (!sheets.isEmpty()) {
                        Sheet firstSheet = book.getSheets().get(0);
                        SheetsController.getInstance().showAssembly(firstSheet);
                    } else {
                        logger.info("No sheet in {}", book);
                    }
                }
            } else {
                logger.warn("File {} does not exist", file);
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
            extends BasicTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final File file;

        //~ Constructors ---------------------------------------------------------------------------
        public OpenTask (File file)
        {
            this.file = file;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            if (file.exists()) {
                // Actually open the image file
                Book book = OMR.getEngine().loadInput(file.toPath());
                book.createSheets(null); // So that sheets are visible
            } else {
                logger.warn("File {} does not exist", file);
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

        private final Constant.String validImageExtensions = new Constant.String(
                ".bmp .gif .jpg .png .tiff .tif .pdf",
                "Valid image file extensions, whitespace-separated");

        private final Constant.Boolean closeConfirmation = new Constant.Boolean(
                true,
                "Should we ask confirmation for closing an unsaved project?");
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
            return constants.closeConfirmation.getValue();
        }

        @Override
        public boolean setSpecific (Boolean specific)
        {
            if (!getSpecific().equals(specific)) {
                constants.closeConfirmation.setValue(specific);
                logger.info(
                        "You will {} be prompted to save project when" + " closing",
                        specific ? "now" : "no longer");

                return true;
            } else {
                return false;
            }
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
            Sheet sheet = SheetsController.getCurrentSheet();
            GlyphRepository.getInstance().recordSheetGlyphs(sheet, false);

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

            return null;
        }
    }
}
