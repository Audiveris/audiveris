//----------------------------------------------------------------------------//
//                                                                            //
//                          S h e e t A c t i o n s                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.ui.GlyphRepository;
import static omr.plugin.Dependency.*;
import omr.plugin.Plugin;
import static omr.plugin.PluginType.*;

import omr.score.Score;

import omr.selection.SelectionObserver;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.step.Step;

import omr.ui.util.FileFilter;
import omr.ui.util.UIUtilities;

import omr.util.Implement;
import omr.util.Logger;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractAction;
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

    //---------//
    // getName //
    //---------//
    @Implement(SelectionObserver.class)
    @Override
    public String getName ()
    {
        return "SheetActions";
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
        Sheet sheet = SheetManager.getSelectedSheet();

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
        Sheet sheet = SheetManager.getSelectedSheet();

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
        Sheet sheet = SheetManager.getSelectedSheet();

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
        Sheet sheet = SheetManager.getSelectedSheet();

        if (sheet != null) {
            if (sheet.getSkewBuilder() != null) {
                sheet.getSkewBuilder()
                     .displayChart();
            } else {
                logger.warning("Data from skew builder is not available");
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
    @Action(enabledProperty = "sheetAvailable")
    public void zoomHeight (ActionEvent e)
    {
        Sheet sheet = SheetManager.getSelectedSheet();

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
        Sheet sheet = SheetManager.getSelectedSheet();

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

    //~ Inner Classes ----------------------------------------------------------

    //-------------//
    // CloseAction //
    //-------------//
    @Deprecated
    @Plugin(type = SHEET_EXPORT, dependency = SHEET_AVAILABLE, onToolbar = true)
    public static class CloseAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Action(name = "closeSheet")
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .closeSheet(e);
        }
    }

    //----------------//
    // LinePlotAction //
    //----------------//
    @Deprecated
    @Plugin(type = SHEET_EXPORT, dependency = SHEET_AVAILABLE, onToolbar = false)
    public static class LinePlotAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .plotLine(e);
        }
    }

    //------------//
    // OpenAction //
    //------------//
    @Deprecated
    @Plugin(type = SHEET_IMPORT, dependency = NONE, onToolbar = true)
    public static class OpenAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .openSheet(e);
        }
    }

    //--------------//
    // RecordAction //
    //--------------//
    @Deprecated
    @Plugin(type = SHEET_EXPORT, dependency = SHEET_AVAILABLE)
    public static class RecordAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Task task = getInstance()
                            .recordGlyphs();

            if (task != null) {
                task.execute();
            }
        }
    }

    //-----------------//
    // ScalePlotAction //
    //-----------------//
    @Deprecated
    @Plugin(type = SHEET_EXPORT, dependency = SHEET_AVAILABLE)
    public static class ScalePlotAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .plotScale(e);
        }
    }

    //----------------//
    // SkewPlotAction //
    //----------------//
    @Deprecated
    @Plugin(type = SHEET_EXPORT, dependency = SHEET_AVAILABLE)
    public static class SkewPlotAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .plotSkew(e);
        }
    }

    //------------------//
    // ZoomHeightAction //
    //------------------//
    @Deprecated
    @Plugin(type = SHEET_EDIT, dependency = SHEET_AVAILABLE, onToolbar = true)
    public static class ZoomHeightAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .zoomHeight(e);
        }
    }

    //-----------------//
    // ZoomWidthAction //
    //-----------------//
    @Deprecated
    @Plugin(type = SHEET_EDIT, dependency = SHEET_AVAILABLE, onToolbar = true)
    public static class ZoomWidthAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .zoomWidth(e);
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

    //----------//
    // OpenTask //
    //----------//
    private static class OpenTask
        extends Task<Void, Void>
    {
        //~ Instance fields ----------------------------------------------------

        private final File file;

        //~ Constructors -------------------------------------------------------

        OpenTask (File file)
        {
            super(Main.getInstance());
            this.file = file;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            // Actually load the sheet picture
            Step.LOAD.performSerial(null, file);

            // Remember (even across runs) the parent directory
            constants.defaultSheetDirectory.setValue(file.getParent());

            return null;
        }
    }

    //------------------//
    // RecordGlyphsTask //
    //------------------//
    private static class RecordGlyphsTask
        extends Task<Void, Void>
    {
        //~ Constructors -------------------------------------------------------

        RecordGlyphsTask ()
        {
            super(Main.getInstance());
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            Sheet sheet = SheetManager.getSelectedSheet();
            GlyphRepository.getInstance()
                           .recordSheetGlyphs(
                sheet, /* emptyStructures => */
                sheet.isOnSymbols());

            return null;
        }
    }
}
