//----------------------------------------------------------------------------//
//                                                                            //
//                          S h e e t A c t i o n s                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphRepository;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ScoresManager;
import omr.score.ui.ScoreController;

import omr.script.RemoveTask;

import omr.sheet.ScaleBuilder;
import omr.sheet.Sheet;

import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtilities;

import omr.util.BasicTask;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JOptionPane;

/**
 * Class {@code SheetActions} simply gathers UI actions related to sheet
 * handling. These methods are ready to be picked up by the plugins mechanism.
 *
 * @author Hervé Bitteur
 */
public class SheetActions
    extends SheetDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetActions.class);

    /** Singleton */
    private static SheetActions INSTANCE;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // SheetActions //
    //--------------//
    /**
     * Creates a new SheetActions object.
     */
    public SheetActions ()
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
    public static synchronized SheetActions getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SheetActions();
        }

        return INSTANCE;
    }

    //------------//
    // closeScore //
    //------------//
    /**
     * Action that handles the closing of the currently selected score.
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void closeScore (ActionEvent e)
    {
        Score score = ScoreController.getCurrentScore();

        if (score != null) {
            score.close();
        }
    }

    //---------------//
    // openImageFile //
    //---------------//
    /**
     * Action that let the user select a image file interactively.
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public Task openImageFile (ActionEvent e)
    {
        String suffixes = constants.validImageFiles.getValue();
        String allSuffixes = suffixes + " " + suffixes.toUpperCase();
        File   file = UIUtilities.fileChooser(
            false,
            Main.getGui().getFrame(),
            new File(ScoresManager.getInstance().getDefaultInputDirectory()),
            new OmrFileFilter(
                "Major image files" + " (" + suffixes + ")",
                allSuffixes.split("\\s")));

        if (file != null) {
            if (file.exists()) {
                return new OpenTask(file);
            } else {
                logger.warning("File not found " + file);
            }
        }

        return null;
    }

    //-----------//
    // plotScale //
    //-----------//
    /**
     * Action that allows to display the plot of Scale Builder.
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void plotScale (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            ScaleBuilder scaleBuilder = sheet.getScaleBuilder();

            if (scaleBuilder != null) {
                scaleBuilder.displayChart();
            } else {
                logger.warning(
                    "Cannot display scale plot, for lack of scale data");
            }
        }
    }

    //--------------//
    // recordGlyphs //
    //--------------//
    @Action(enabledProperty = SHEET_AVAILABLE)
    public Task recordGlyphs ()
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
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
    public void removeSheet (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            int answer = JOptionPane.showConfirmDialog(
                null,
                "Do you confirm the removal of this sheet" +
                " from its containing score ?");

            if (answer == JOptionPane.YES_OPTION) {
                new RemoveTask(sheet).launch(sheet);
            }
        }
    }

    //------------//
    // zoomHeight //
    //------------//
    /**
     * Action that allows to adjust the display zoom, so that the full height is
     * shown.
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

        assembly.getSelectedView()
                .fitHeight();
    }

    //-----------//
    // zoomWidth //
    //-----------//
    /**
     * Action that allows to adjust the display zoom, so that the full width is
     * shown.
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

        assembly.getSelectedView()
                .fitWidth();
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------//
    // OpenTask //
    //----------//
    public static class OpenTask
        extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        private final File file;

        //~ Constructors -------------------------------------------------------

        public OpenTask (File file)
        {
            this.file = file;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            // Actually load the image file
            Score score = new Score(file);
            score.createPages();

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

        /** Default directory for selection of image files */
        Constant.String defaultImageDirectory = new Constant.String(
            System.getProperty("user.home"),
            "Default directory for selection of input image files");

        /** Valid extensions for image files */
        Constant.String validImageFiles = new Constant.String(
            ".bmp .gif .jpg .png .tiff .tif .pdf",
            "Valid image file extensions, whitespace-separated");
    }

    //------------------//
    // RecordGlyphsTask //
    //------------------//
    private static class RecordGlyphsTask
        extends BasicTask
    {
        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            Sheet sheet = SheetsController.getCurrentSheet();
            GlyphRepository.getInstance()
                           .recordSheetGlyphs(
                sheet, /* emptyStructures => */
                sheet.isOnSymbols());

            return null;
        }
    }
}
