//----------------------------------------------------------------------------//
//                                                                            //
//                           G r i d B u i l d e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphsModel;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.SymbolGlyphBoard;

import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.ui.SectionBoard;

import omr.log.Logger;
import static omr.run.Orientation.*;
import omr.run.RunBoard;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.sheet.Sheet;
import omr.sheet.ui.PixelBoard;

import omr.step.Step;
import omr.step.StepException;
import omr.step.Steps;

import omr.ui.BoardsPane;
import omr.ui.view.ScrollView;

import omr.util.StopWatch;

import java.util.List;

/**
 * Class <code>GridBuilder</code> computes the grid of systems of a sheet
 * picture, based on the retrieval of horizontal staff lines and of vertical
 * bar lines.
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
    private static final Logger logger = Logger.getLogger(GridBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** Companion in charge of staff lines */
    private final LinesRetriever linesRetriever;

    /** Companion in charge of bar lines */
    private final BarsRetriever barsRetriever;

    /** The grid display if any */
    private GridView gridView;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // GridBuilder //
    //-------------//
    /**
     * Retrieve the frames of all staff lines
     *
     * @param sheet the sheet to process
     */
    public GridBuilder (Sheet sheet)
    {
        this.sheet = sheet;

        barsRetriever = new BarsRetriever(sheet);
        linesRetriever = new LinesRetriever(sheet, barsRetriever);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // retrieveSystemBars //
    //-----------//
    /**
     * Compute and display the system frames of the sheet picture
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
            if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
                displayGridView();
            }

            // Retrieve the horizontal staff lines filaments
            watch.start("retrieveLines");
            linesRetriever.retrieveLines();

            // Retrieve the major vertical barlines and thus the systems
            watch.start("retrieveSystemBars");
            barsRetriever.retrieveSystemBars();

            // Complete the staff lines w/ short sections & filaments left over
            watch.start("completeLines");

            List<Section> newSections = linesRetriever.completeLines();

            // Retrieve minor barlines (for measures)
            barsRetriever.retrieveMeasureBars();

            // Adjust ending points of all systems (side) bars
            barsRetriever.adjustSystemBars();

            // Define the destination grid, if so desired
            if (constants.buildDewarpedTarget.isSet()) {
                watch.start("targetBuilder");

                /** Companion in charge of target grid */
                TargetBuilder targetBuilder = new TargetBuilder(sheet);

                sheet.setTargetBuilder(targetBuilder);
                targetBuilder.buildInfo();
            }
        } catch (Throwable ex) {
            logger.warning(sheet.getLogPrefix() + "Error in GridBuilder", ex);
            ex.printStackTrace();
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }

            if (gridView != null) {
                gridView.refresh();
            }
        }
    }

    //--------------//
    // buildAllLags //
    //--------------//
    /**
     * From the sheet picture, build the vertical lag (for barlines) and the
     * horizontal lag (for staff lines)
     */
    private void buildAllLags ()
    {
        final boolean   showRuns = constants.showRuns.getValue();
        final StopWatch watch = new StopWatch("buildAllLags");

        try {
            // Retrieve all foreground pixels into vertical runs
            watch.start("wholeVertTable");

            RunsTable wholeVertTable = new RunsTableFactory(
                VERTICAL,
                sheet.getPicture(),
                sheet.getPicture().getMaxForeground(),
                0).createTable("whole-vert");

            // Note: from that point on, we could simply discard the sheet picture
            // and save memory, since wholeVertTable contains all foreground pixels.
            // For the time being, it is kept alive for display purpose, and to
            // allow the dewarping of the initial picture.

            // View on the initial runs (just for information)
            if (showRuns) {
                // Add a view on runs table
                linesRetriever.addRunsTab(wholeVertTable);
            }

            // hLag creation
            watch.start("linesRetriever.buildLag");

            RunsTable purgedVertTable = linesRetriever.buildLag(
                wholeVertTable,
                showRuns);

            // vLag creation
            watch.start("barsRetriever.buildLag");
            barsRetriever.buildLag(purgedVertTable, showRuns);
        } finally {
            if (constants.printWatch.getValue()) {
                watch.print();
            }
        }
    }

    //-----------------//
    // displayGridView //
    //-----------------//
    private void displayGridView ()
    {
        Lag              hLag = sheet.getHorizontalLag();
        Lag              vLag = sheet.getVerticalLag();
        GlyphsController gController = new GlyphsController(
            new GlyphsModel(sheet, sheet.getNest(), Steps.valueOf(Steps.GRID)));

        // Create a view
        gridView = new GridView(
            sheet.getNest(),
            linesRetriever,
            hLag,
            barsRetriever,
            vLag,
            gController);
        gridView.setLocationService(sheet.getLocationService());

        // Boards
        BoardsPane boardsPane = new BoardsPane(
            new PixelBoard(sheet),
            new RunBoard(hLag, true),
            new SectionBoard(hLag, true),
            new RunBoard(vLag, true),
            new SectionBoard(vLag, true),
            new SymbolGlyphBoard(gController, true));

        // Create a hosting frame for the view
        ScrollView sv = new ScrollView(gridView);
        sheet.getAssembly()
             .addViewTab(Step.DATA_TAB, sv, boardsPane);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean displayFrame = new Constant.Boolean(
            true,
            "Should we display a frame?");

        //
        Constant.Boolean showRuns = new Constant.Boolean(
            false,
            "Should we show view on runs?");

        //
        Constant.Boolean printWatch = new Constant.Boolean(
            false,
            "Should we print out the stop watch?");

        //
        Constant.Boolean buildDewarpedTarget = new Constant.Boolean(
            false,
            "Should we build a dewarped target?");
    }
}
