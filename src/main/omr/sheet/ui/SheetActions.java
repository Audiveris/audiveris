//----------------------------------------------------------------------------//
//                                                                            //
//                          S h e e t A c t i o n s                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.ui.GlyphRepository;

import omr.log.Logger;

import omr.score.Score;

import omr.sheet.*;
import omr.sheet.Sheet;
import omr.sheet.SheetsManager;

import omr.step.Step;

import omr.ui.util.FileFilter;
import omr.ui.util.UIUtilities;

import omr.util.BasicTask;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JOptionPane;

/**
 * Class <code>SheetActions</code> simply gathers UI actions related to sheet
 * handling. These static member classes are ready to be picked by the plugins
 * mechanism.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
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
    // closeSheet //
    //------------//
    /**
     * Action that handles the closing of the currently selected sheet.
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = "sheetAvailable")
    public void closeSheet (ActionEvent e)
    {
        Sheet sheet = SheetsController.selectedSheet();

        if (sheet != null) {
            Score score = sheet.getScore();

            if (score != null) {
                score.close();
            }

            sheet.close();
        }
    }

    //-----------//
    // openSheet //
    //-----------//
    /**
     * Action that let the user select a sheet file interactively.
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public Task openSheet (ActionEvent e)
    {
        File file = UIUtilities.fileChooser(
            false,
            Main.getGui().getFrame(),
            new File(constants.defaultSheetDirectory.getValue()),
            new FileFilter(
                "Major image files",
                constants.validImageFiles.getValue().split("\\s")));

        if (file != null) {
            if (file.exists()) {
                return new OpenTask(file);
            } else {
                logger.warning("File not found " + file);
            }
        }

        return null;
    }

    //----------//
    // plotLine //
    //----------//
    /**
     * Action that allows to display the plot of Line Builder.
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = "sheetAvailable")
    public void plotLine (ActionEvent e)
    {
        Sheet sheet = SheetsController.selectedSheet();

        if (sheet != null) {
            if (sheet.getLinesBuilder() != null) {
                sheet.getLinesBuilder()
                     .displayChart();
            } else {
                logger.warning(
                    "Data from staff line builder" + " is not available");
            }
        }
    }

    //-----------//
    // plotScale //
    //-----------//
    /**
     * Action that allows to display the plot of Scale Builder.
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = "sheetAvailable")
    public void plotScale (ActionEvent e)
    {
        Sheet sheet = SheetsController.selectedSheet();

        if (sheet != null) {
            sheet.getScale()
                 .displayChart();
        }
    }

    //----------//
    // plotSkew //
    //----------//
    /**
     * Action that allows to display the plot of Skew Builder
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = "sheetAvailable")
    public void plotSkew (ActionEvent e)
    {
        Sheet sheet = SheetsController.selectedSheet();

        if (sheet != null) {
            if (sheet.getSkewBuilder() != null) {
                sheet.getSkewBuilder()
                     .displayChart();
            } else {
                logger.warning("Data from skew builder is not available");
            }
        }
    }

    //--------------//
    // recordGlyphs //
    //--------------//
    @Action(enabledProperty = "sheetAvailable")
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

    //------------//
    // zoomHeight //
    //------------//
    /**
     * Action that allows to adjust the display zoom, so that the full height is
     * shown.
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = "sheetAvailable")
    public void zoomHeight (ActionEvent e)
    {
        Sheet sheet = SheetsController.selectedSheet();

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
    @Action(enabledProperty = "sheetAvailable")
    public void zoomWidth (ActionEvent e)
    {
        Sheet sheet = SheetsController.selectedSheet();

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
            // Actually load the sheet picture
            Step.LOAD.performUntil(null, file);

            // Remember (even across runs) the parent directory
            constants.defaultSheetDirectory.setValue(file.getParent());

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

        /** Default directory for selection of sheet image files */
        Constant.String defaultSheetDirectory = new Constant.String(
            "",
            "Default directory for selection of sheet image files");

        /** Valid extensions for image files */
        Constant.String validImageFiles = new Constant.String(
            ".bmp .gif .jpg .png .tif .pdf",
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
            Sheet sheet = SheetsController.selectedSheet();
            GlyphRepository.getInstance()
                           .recordSheetGlyphs(
                sheet, /* emptyStructures => */
                sheet.isOnSymbols());

            return null;
        }
    }
}
