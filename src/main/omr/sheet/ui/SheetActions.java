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

import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

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

    /** Singleton */
    private static SheetActions INSTANCE;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SheetActions object.
     */
    public SheetActions ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
            book.close();
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
                new File(BookManager.getDefaultInputDirectory()),
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
                null,
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
                    null,
                    "Do you confirm the removal of this sheet" + " from its containing score ?");

            if (answer == JOptionPane.YES_OPTION) {
                new RemoveTask(sheet).launch(sheet);
            }
        }
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

        /** Valid extensions for image files */
        Constant.String validImageExtensions = new Constant.String(
                ".bmp .gif .jpg .png .tiff .tif .pdf",
                "Valid image file extensions, whitespace-separated");
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
}
