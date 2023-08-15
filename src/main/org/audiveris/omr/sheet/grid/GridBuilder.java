//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G r i d B u i l d e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.grid;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.glyph.dynamic.FilamentBoard;
import org.audiveris.omr.glyph.dynamic.FilamentView;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.PixelBoard;
import org.audiveris.omr.sheet.ui.SheetAssembly;
import org.audiveris.omr.sheet.ui.SheetTab;
import static org.audiveris.omr.sheet.ui.SheetTab.FILAMENT_TAB;
import org.audiveris.omr.sig.ui.InterBoard;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class <code>GridBuilder</code> computes the grid of systems of a sheet picture, based on
 * the retrieval of horizontal staff lines and of vertical bar lines.
 * <p>
 * The actual processing is delegated to 3 companions:
 * <ul>
 * <li>{@link LinesRetriever} for retrieving horizontal staff lines.</li>
 * <li>{@link BarsRetriever} for retrieving vertical bar lines.</li>
 * <li>Optionally, {@link TargetBuilder} for building the target grid.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class GridBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(GridBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Companion in charge of staff lines. */
    public final LinesRetriever linesRetriever;

    /** Companion in charge of bar lines. */
    public final BarsRetriever barsRetriever;

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------

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

        if (constants.showGrid.isSet() && (OMR.gui != null)) {
            sheet.addItemRenderer(barsRetriever);
            sheet.addItemRenderer(linesRetriever);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // buildAllLags //
    //--------------//
    /**
     * From the BINARY table, build the horizontal lag (for staff lines) and the
     * vertical lag (for barlines).
     */
    private void buildAllLags ()
    {
        final StopWatch watch = new StopWatch("buildAllLags");

        try {
            // We already have all foreground pixels as vertical runs

            // hLag creation
            watch.start("buildHorizontalLag");

            RunTable longVertTable = linesRetriever.buildHorizontalLag();

            // vLag creation
            watch.start("buildVerticalLag");
            sheet.getLagManager().buildVerticalLag(longVertTable);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Compute and display the system frames of the sheet picture.
     *
     * @throws StepException if step was stopped
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
            if (OMR.gui != null) {
                sheet.getSheetEditor().refresh();

                final SheetAssembly assembly = sheet.getStub().getAssembly();

                // Inter board
                assembly.addBoard(SheetTab.DATA_TAB, new InterBoard(sheet));

                // Filament board
                EntityService<Filament> fService = sheet.getFilamentIndex().getEntityService();
                assembly.addBoard(SheetTab.DATA_TAB, new FilamentBoard(fService, false));

                // Filament view?
                if (constants.displayFilaments.isSet()) {
                    final FilamentView view = new FilamentView(fService);
                    view.setLocationService(sheet.getLocationService());
                    assembly.addViewTab(
                            FILAMENT_TAB,
                            new ScrollView(view),
                            new BoardsPane(
                                    new PixelBoard(sheet),
                                    new FilamentBoard(fService, true)));
                }
            }

            // Retrieve the horizontal staff lines filaments with long sections
            watch.start("retrieveLines");
            linesRetriever.retrieveLines();

            // Complete horizontal lag with short sections
            linesRetriever.createShortSections();

            // Retrieve the vertical barlines and thus the systems
            watch.start("retrieveBarlines");
            barsRetriever.process();

            // Complete the staff lines w/ sections & filaments left over
            watch.start("completeLines");
            linesRetriever.completeLines();
        } catch (StepException se) {
            throw se;
        } catch (Throwable ex) {
            logger.warn("Error in GridBuilder: {}", ex.toString(), ex);
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
        //            barsRetriever.process(oldSticks, newSticks);
        //        } catch (Exception ex) {
        //            logger.warn("updateBars. process", ex);
        //        }
        //
        //        barsRetriever.retrieveMeasureBars();
        //        barsRetriever.adjustSystemBars();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Boolean showGrid = new Constant.Boolean(
                false,
                "Should we show the details of grid?");

        private final Constant.Boolean displayFilaments = new Constant.Boolean(
                false,
                "Should we show the filaments view?");
    }
}
