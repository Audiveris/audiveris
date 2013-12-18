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

import omr.glyph.facets.Glyph;

import omr.run.RunsTable;

import omr.sheet.Sheet;

import omr.sig.ui.InterBoard;

import omr.step.Step;
import omr.step.StepException;

import omr.util.Navigable;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code GridBuilder} computes the grid of systems of a sheet
 * picture, based on the retrieval of horizontal staff lines and of
 * vertical bar lines.
 * <p>
 * The actual processing is delegated to 3 companions:<ul>
 * <li>{@link LinesRetriever} for retrieving horizontal staff lines.</li>
 * <li>{@link BarsRetriever} for retrieving vertical bar lines.</li>
 * <li>Optionally, {@link TargetBuilder} for building the target grid.</li>
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
    @Navigable(false)
    private final Sheet sheet;

    /** Companion in charge of staff lines. */
    public final LinesRetriever linesRetriever;

    /** Companion in charge of bar lines. */
    public final BarsRetriever barsRetriever;

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
        sheet.addItemRenderer(barsRetriever);

        linesRetriever = new LinesRetriever(sheet, barsRetriever);
        sheet.addItemRenderer(linesRetriever);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Compute and display the system frames of the sheet picture.
     *
     * @throws StepException
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

                // Inter board
                sheet.getAssembly()
                        .addBoard(Step.DATA_TAB, new InterBoard(sheet));
            }

            // Retrieve the horizontal staff lines filaments with long sections
            watch.start("retrieveLines");
            linesRetriever.retrieveLines();

            // Complete horizontal lag with short sections
            linesRetriever.createShortSections();

            // Retrieve the vertical barlines and thus the systems
            watch.start("retrieveBarlines");
            barsRetriever.retrieveBarlines();

            // Complete the staff lines w/ short sections & filaments left over
            watch.start("completeLines");
            linesRetriever.completeLines();

            /** Companion in charge of target grid */
            // Define the destination grid, if so desired
            if (constants.buildDewarpedTarget.isSet()) {
                watch.start("targetBuilder");

                TargetBuilder targetBuilder = new TargetBuilder(sheet);
                sheet.addItemRenderer(targetBuilder);
                targetBuilder.buildInfo();
            }
        } catch (Throwable ex) {
            logger.warn(sheet.getLogPrefix() + "Error in GridBuilder", ex);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
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
        //        logger.info("updateBars");
        //        logger.info("Old {}", Glyphs.toString(oldSticks));
        //        logger.info("New {}", Glyphs.toString(newSticks));
        //
        //        try {
        //            barsRetriever.retrieveBarlines(oldSticks, newSticks);
        //        } catch (Exception ex) {
        //            logger.warn("updateBars. retrieveBarlines", ex);
        //        }
        //
        //        barsRetriever.retrieveMeasureBars();
        //        barsRetriever.adjustSystemBars();
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

            // hLag creation
            watch.start("buildHorizontalLag");

            RunsTable longVertTable = linesRetriever.buildHorizontalLag(
                    wholeVertTable,
                    showRuns);

            // vLag creation
            watch.start("buildVerticalLag");
            barsRetriever.buildVerticalLag(longVertTable);
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
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        Constant.Boolean showRuns = new Constant.Boolean(
                false,
                "Should we show view on runs?");

        Constant.Boolean buildDewarpedTarget = new Constant.Boolean(
                false,
                "Should we build a dewarped target?");

    }
}
