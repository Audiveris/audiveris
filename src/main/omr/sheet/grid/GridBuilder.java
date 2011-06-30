//----------------------------------------------------------------------------//
//                                                                            //
//                           G r i d B u i l d e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphsModel;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphsController;

import omr.lag.ui.RunBoard;
import omr.lag.ui.ScrollLagView;
import omr.lag.ui.SectionBoard;

import omr.log.Logger;
import static omr.run.Orientation.*;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.sheet.Sheet;
import omr.sheet.ui.PixelBoard;

import omr.step.Step;
import omr.step.StepException;
import omr.step.Steps;

import omr.ui.BoardsPane;

import omr.util.StopWatch;

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

    /** Companion in charge of target grid */
    private final TargetBuilder targetBuilder;

    /** The grid display if any */
    private GridView gridView;

    /** The bars display if any */
    private BarsView barsView;

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

        linesRetriever = new LinesRetriever(sheet);
        barsRetriever = new BarsRetriever(sheet);
        targetBuilder = new TargetBuilder(sheet, linesRetriever, barsRetriever);

        sheet.setTargetBuilder(targetBuilder);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // buildInfo //
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
                displayBarsView();
                displayGridView();
            }

            // Retrieve the horizontal staff lines filaments
            watch.start("linesRetriever");
            linesRetriever.buildInfo();

            // Retrieve the vertical barlines
            watch.start("barsRetriever");
            barsRetriever.buildInfo(linesRetriever.getGlobalSlope());

            // Define the destination grid
            watch.start("targetBuilder");
            targetBuilder.buildInfo();
        } catch (Exception ex) {
            logger.warning("Error in GridBuilder", ex);
        } finally {
            if (gridView != null) {
                // Update the display
                gridView.refresh();
            }

            if (constants.printWatch.getValue()) {
                watch.print();
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
            if (showRuns) {
                // Add a view on runs table
                sheet.getAssembly()
                     .addRunsTab(wholeVertTable);
            }

            // hLag
            watch.start("linesRetriever.buildLag");

            RunsTable purgedVertTable = linesRetriever.buildLag(
                wholeVertTable,
                showRuns);

            // vLag
            watch.start("barsRetriever.buildLag");
            barsRetriever.buildLag(purgedVertTable, showRuns);
        } finally {
            if (constants.printWatch.getValue()) {
                watch.print();
            }
        }
    }

    //-----------------//
    // displayBarsView //
    //-----------------//
    private void displayBarsView ()
    {
        GlyphLag         vLag = barsRetriever.getLag();
        GlyphsController controller = new GlyphsController(
            new GlyphsModel(sheet, vLag, Steps.valueOf(Steps.GRID)));

        // Create a view
        barsView = new BarsView(vLag, barsRetriever, null, controller);
        barsView.colorizeAllSections();

        // Boards
        final String  unit = sheet.getId() + ":BarsBuilder";
        BoardsPane    boardsPane = new BoardsPane(
            new PixelBoard(unit, sheet),
            new RunBoard(unit, vLag, true),
            new SectionBoard(unit, vLag.getLastVertexId(), vLag, true),
            new GlyphBoard(unit, controller, null, false));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(barsView);
        sheet.getAssembly()
             .addViewTab("Bars", slv, boardsPane);
    }

    //-----------------//
    // displayGridView //
    //-----------------//
    private void displayGridView ()
    {
        GlyphLag         hLag = linesRetriever.getLag();
        GlyphLag         vLag = barsRetriever.getLag();
        GlyphsController controller = new GlyphsController(
            new GlyphsModel(sheet, hLag, Steps.valueOf(Steps.GRID)));

        // Create a view
        gridView = new GridView(
            linesRetriever,
            hLag,
            barsRetriever,
            vLag,
            null,
            controller);
        gridView.colorizeAllSections();

        // Boards
        final String  unit = sheet.getId() + ":GridBuilder";
        BoardsPane    boardsPane = new BoardsPane(
            new PixelBoard(unit, sheet),
            new RunBoard(unit, hLag, true),
            new SectionBoard(unit, hLag.getLastVertexId(), hLag, true),
            new GlyphBoard(unit, controller, null, false));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(gridView);
        sheet.getAssembly()
             .addViewTab(Step.GRID_TAB, slv, boardsPane);
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
        Constant.Boolean showRuns = new Constant.Boolean(
            false,
            "Should we show view on runs?");
        Constant.Boolean printWatch = new Constant.Boolean(
            false,
            "Should we print out the stop watch?");
    }
}
