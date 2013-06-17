//----------------------------------------------------------------------------//
//                                                                            //
//                           G r i d B u i l d e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.SymbolsEditor;

import omr.run.RunsTable;

import omr.sheet.Sheet;

import omr.step.StepException;

import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Class {@code GridBuilder} computes the grid of systems of a sheet
 * picture, based on the retrieval of horizontal staff lines and of
 * vertical bar lines.
 *
 * <p>The actual processing is delegated to 3 companions:<ul>
 * <li>{@link LinesRetriever} for retrieving all horizontal staff lines.</li>
 * <li>{@link BarsRetriever} for retrieving main vertical bar lines.</li>
 * <li>{@link TargetBuilder} for building the target grid.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class GridBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            GridBuilder.class);

    //~ Instance fields --------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Companion in charge of staff lines. */
    private final LinesRetriever linesRetriever;

    /** Companion in charge of bar lines. */
    private final BarsRetriever barsRetriever;

    /** For runs display, if any. */
    private final RunsViewer runsViewer;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // GridBuilder //
    //-------------//
    /**
     * Retrieve the frames of all staff lines.
     *
     * @param sheet the sheet to process
     */
    public GridBuilder (Sheet sheet)
    {
        this.sheet = sheet;

        barsRetriever = new BarsRetriever(sheet);
        linesRetriever = new LinesRetriever(sheet, barsRetriever);

        runsViewer = (Main.getGui() != null)
                ? new RunsViewer(sheet, linesRetriever, barsRetriever) : null;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Compute and display the system frames of the sheet picture.
     */
    public void buildInfo ()
            throws StepException
    {
        StopWatch watch = new StopWatch("GridBuilder");

        try {
            // Build the vertical and horizontal lags
            watch.start("buildAllLags");
            buildAllLags();

            // Display
            if (Main.getGui() != null) {
                displayEditor();
            }

            // Retrieve the horizontal staff lines filaments
            watch.start("retrieveLines");
            linesRetriever.retrieveLines();

            // Retrieve the major vertical barlines and thus the systems
            watch.start("retrieveSystemBars");
            barsRetriever.retrieveSystemBars(
                    Collections.EMPTY_SET,
                    Collections.EMPTY_SET);

            // Complete the staff lines w/ short sections & filaments left over
            watch.start("completeLines");
            linesRetriever.completeLines();

            // Retrieve minor barlines (for measures)
            barsRetriever.retrieveMeasureBars();

            // Adjust ending points of all systems (side) bars
            barsRetriever.adjustSystemBars();

            /** Companion in charge of target grid */
            TargetBuilder targetBuilder = new TargetBuilder(sheet);
            sheet.setTargetBuilder(targetBuilder);

            // Define the destination grid, if so desired
            if (constants.buildDewarpedTarget.isSet()) {
                watch.start("targetBuilder");
                targetBuilder.buildInfo();
            }
        } catch (Throwable ex) {
            logger.warn(sheet.getLogPrefix() + "Error in GridBuilder", ex);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }

            if (Main.getGui() != null) {
                sheet.getSymbolsEditor()
                        .refresh();
            }
        }
    }

    //------------//
    // updateBars //
    //------------//
    /**
     * Update the collection of bar candidates, removing the discarded
     * ones and adding the new ones, and rebuild the barlines.
     *
     * @param oldSticks former glyphs to discard
     * @param newSticks new glyphs to take as manual bar sticks
     */
    public void updateBars (Collection<Glyph> oldSticks,
                            Collection<Glyph> newSticks)
    {
        logger.info("updateBars");
        logger.info("Old {}", Glyphs.toString(oldSticks));
        logger.info("New {}", Glyphs.toString(newSticks));

        try {
            barsRetriever.retrieveSystemBars(oldSticks, newSticks);
        } catch (Exception ex) {
            logger.warn("updateBars. retrieveSystemBars", ex);
        }

        barsRetriever.retrieveMeasureBars();
        barsRetriever.adjustSystemBars();
    }

    //--------------//
    // buildAllLags //
    //--------------//
    /**
     * From the sheet picture, build the vertical lag (for bar lines)
     * and the horizontal lag (for staff lines).
     */
    private void buildAllLags ()
    {
        final boolean showRuns = constants.showRuns.isSet()
                                 && (Main.getGui() != null);
        final StopWatch watch = new StopWatch("buildAllLags");

        try {
            // We already have all foreground pixels as vertical runs
            RunsTable wholeVertTable = sheet.getWholeVerticalTable();

            // Note: from that point on, we could simply discard the sheet picture
            // and save memory, since wholeVertTable contains all foreground pixels.
            // For the time being, it is kept alive for display purpose, and to
            // allow the dewarping of the initial picture.

            // View on the initial runs (just for information)
            if (showRuns) {
                runsViewer.display(wholeVertTable);
            }

            // hLag creation
            watch.start("linesRetriever.buildLag");

            RunsTable longVertTable = linesRetriever.buildLag(
                    wholeVertTable,
                    showRuns);

            // vLag creation
            watch.start("barsRetriever.buildLag");
            barsRetriever.buildLag(longVertTable);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //---------------//
    // displayEditor //
    //---------------//
    private void displayEditor ()
    {
        sheet.createSymbolsControllerAndEditor();

        SymbolsEditor editor = sheet.getSymbolsEditor();

        // Specific rendering for grid
        editor.addItemRenderer(linesRetriever);
        editor.addItemRenderer(barsRetriever);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean showRuns = new Constant.Boolean(
                false,
                "Should we show view on runs?");

        Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        Constant.Boolean buildDewarpedTarget = new Constant.Boolean(
                false,
                "Should we build a dewarped target?");

    }
}
