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

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.step.Step;

import omr.ui.util.FileFilter;
import omr.ui.util.UIUtilities;

import omr.util.Implement;
import omr.util.Logger;

import org.jdesktop.swingworker.SwingWorker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

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
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetActions.class);

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // SheetActions //
    //--------------//
    private SheetActions ()
    {
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------------//
    // CloseAction //
    //-------------//
    /**
     * Class <code>CloseAction</code> handles the closing of the currently
     * selected sheet.
     */
    @Plugin(type = SHEET_EXPORT, dependency = SHEET_AVAILABLE, onToolbar = true)
    public static class CloseAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
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
    }

    //----------------//
    // LinePlotAction //
    //----------------//
    /**
     * Class <code>LinePlotAction</code> allows to display the plot of Line
     * Builder.
     */
    @Plugin(type = SHEET_EXPORT, dependency = SHEET_AVAILABLE, onToolbar = false)
    public static class LinePlotAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
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
    }

    //------------//
    // OpenAction //
    //------------//
    /**
     * Class <code>OpenAction</code> let the user select a sheet file
     * interactively.
     */
    @Plugin(type = SHEET_IMPORT, dependency = NONE, onToolbar = true)
    public static class OpenAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            File file = UIUtilities.fileChooser(
                false,
                Main.getGui().getFrame(),
                new File(constants.defaultSheetDirectory.getValue()),
                new FileFilter(
                    "Major image files",
                    new String[] { ".bmp", ".gif", ".jpg", ".png", ".tif" }));

            if (file != null) {
                if (file.exists()) {
                    // Register that as a user target
                    try {
                        Main.getGui()
                            .setTarget(file.getCanonicalPath());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    // Actually load the sheet picture
                    Step.LOAD.performParallel(null, file);

                    // Remember (even across runs) the parent directory
                    constants.defaultSheetDirectory.setValue(file.getParent());
                } else {
                    logger.warning("File not found " + file);
                }
            }
        }
    }

    //--------------//
    // RecordAction //
    //--------------//
    @Plugin(type = SHEET_EXPORT, dependency = SHEET_AVAILABLE)
    public static class RecordAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            int answer = JOptionPane.showConfirmDialog(
                null,
                "Are you sure of all the symbols of this sheet ?");

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }

            final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground ()
                {
                    Sheet sheet = SheetManager.getSelectedSheet();
                    GlyphRepository.getInstance()
                                   .recordSheetGlyphs(
                        sheet, /* emptyStructures => */
                        sheet.isOnSymbols());

                    return null;
                }
            };

            worker.execute();
        }
    }

    //-----------------//
    // ScalePlotAction //
    //-----------------//
    /**
     * Class <code>ScalePlotAction</code> allows to display the plot of Scale
     * Builder.
     */
    @Plugin(type = SHEET_EXPORT, dependency = SHEET_AVAILABLE)
    public static class ScalePlotAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Sheet sheet = SheetManager.getSelectedSheet();

            if (sheet != null) {
                sheet.getScale()
                     .displayChart();
            }
        }
    }

    //----------------//
    // SkewPlotAction //
    //----------------//
    /**
     * Class <code>SkewPlotAction</code> allows to display the plot of Skew
     * Builder.
     */
    @Plugin(type = SHEET_EXPORT, dependency = SHEET_AVAILABLE)
    public static class SkewPlotAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Sheet sheet = SheetManager.getSelectedSheet();

            if (sheet != null) {
                if (sheet.getSkewBuilder() != null) {
                    sheet.getSkewBuilder()
                         .displayChart();
                } else {
                    logger.warning(
                        "Data from skew builder" + " is not available");
                }
            }
        }
    }

    //------------------//
    // ZoomHeightAction //
    //------------------//
    /**
     * Class <code>ZoomHeightAction</code> allows to adjust the display zoom, so
     * that the full height is shown.
     */
    @Plugin(type = SHEET_EDIT, dependency = SHEET_AVAILABLE, onToolbar = true)
    public static class ZoomHeightAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
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
    }

    //-----------------//
    // ZoomWidthAction //
    //-----------------//
    /**
     * Class <code>ZoomWidthAction</code> allows to adjust the display zoom, so
     * that the full width is shown.
     */
    @Plugin(type = SHEET_EDIT, dependency = SHEET_AVAILABLE, onToolbar = true)
    public static class ZoomWidthAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
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

        /** Initial zoom ratio for displayed sheet pictures */
        Constant.Ratio initialZoomRatio = new Constant.Ratio(
            1d,
            "Initial zoom ratio for displayed sheet pictures");
    }
}
