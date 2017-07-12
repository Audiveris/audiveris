//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     H e a d e r B u i l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sheet.header;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.relation.BarGroupRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.util.ChartPlotter;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import org.audiveris.omr.util.Navigable;

import org.jfree.data.xy.XYSeries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;

/**
 * Class {@code HeaderBuilder} handles the header at the beginning of a given system.
 * <p>
 * A staff always begins with the standard sequence of 3 kinds of components: (clef, key-sig?,
 * time-sig?).
 * <p>
 * Within the same system, the header components (whether present or absent) are vertically aligned
 * across the system staves. This alignment feature is used to cross-validate the horizontal limits
 * of components among system staves, and even the horizontal limits of items slices within the
 * key-sig components.
 * <p>
 * Cross-validation at system level: <ul>
 * <li>Clefs:
 * A clef is mandatory at the beginning of each staff (in the header) and may be different from one
 * staff to the other.
 * Outside the header, a new clef (generally smaller in size) may appear anywhere, regardless of the
 * other staves.
 * <li>Key signatures:
 * A key signature may vary (and even be void) from one staff to the other.
 * If present, the alteration slices are aligned across staves.
 * <li>Time signatures:
 * Inside or outside header, either a time signature is present and identical in every staff of the
 * system, or they are all absent.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class HeaderBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(HeaderBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    private final SIGraph sig;

    /** Maximum header width. */
    private final int maxHeaderWidth;

    /** Manager for column of clefs. */
    private final ClefBuilder.Column clefColumn;

    /** Manager for column of keys signatures. */
    private final KeyColumn keyColumn;

    /** Manager for column of time signatures. */
    private final TimeBuilder.HeaderColumn timeColumn;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new HeaderBuilder object.
     *
     * @param system the system to process
     */
    public HeaderBuilder (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
        maxHeaderWidth = system.getSheet().getScale().toPixels(constants.maxHeaderWidth);
        clefColumn = new ClefBuilder.Column(system);
        keyColumn = new KeyColumn(system);
        timeColumn = new TimeBuilder.HeaderColumn(system);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // plot //
    //------//
    /**
     * Display for end user the projection of the desired staff.
     *
     * @param staff the desired staff
     */
    public void plot (Staff staff)
    {
        final Sheet sheet = system.getSheet();
        final String frameTitle = sheet.getId() + " header staff#" + staff.getId();
        final ChartPlotter plotter = new ChartPlotter(
                frameTitle,
                "Abscissae - staff interline:" + staff.getSpecificInterline(),
                "Counts");

        // Draw time sig portion
        String timeString = timeColumn.addPlot(plotter, staff);

        // Draw key sig portion
        String keyString = keyColumn.addPlot(plotter, staff, maxHeaderWidth);

        // Get clef info
        ClefInter clef = staff.getHeader().clef;
        String clefString = (clef != null) ? clef.getKind().toString() : null;

        {
            // Draw the zero reference line
            final int xMin = staff.getHeaderStart();
            final int xMax = xMin + maxHeaderWidth;

            XYSeries series = new XYSeries("Zero", false); // No autosort
            series.add(xMin, 0);
            series.add(xMax, 0);
            plotter.add(series, Colors.CHART_ZERO, true);
        }

        // Build chart title: clef + key + time
        StringBuilder chartTitle = new StringBuilder(frameTitle);

        if (clefString != null) {
            chartTitle.append(" ").append(clefString);
        }

        if (keyString != null) {
            chartTitle.append(" ").append(keyString);
        }

        if (timeString != null) {
            chartTitle.append(" ").append(timeString);
        }

        plotter.setChartTitle(chartTitle.toString());

        // Display frame
        plotter.display(frameTitle, new Point(20 * staff.getId(), 20 * staff.getId()));
    }

    //---------------//
    // processHeader //
    //---------------//
    /**
     * Process the header at the beginning of all staves in the system.
     */
    public void processHeader ()
    {
        logger.debug("header processing for S#{}", system.getId());

        // Compute header start abscissae based on bar lines (or staff left abscissa)
        // and store the information in staves themselves.
        computeHeaderStarts();

        // Retrieve header clefs
        int clefOffset = clefColumn.retrieveClefs();
        setSystemClefStop(clefOffset);

        // Retrieve header key-sigs
        int keyOffset = keyColumn.retrieveKeys(maxHeaderWidth);
        setSystemKeyStop(keyOffset);

        // Retrieve header time-sigs
        int timeOffset = timeColumn.retrieveTime();
        setSystemTimeStop(timeOffset);

        // We should be able now to select the best clef for each staff
        clefColumn.selectClefs();

        // Purge barline inters found within headers
        purgeBarlines();

        // Freeze header inters
        freezeHeaders();
    }

    //---------------------//
    // computeHeaderStarts //
    //---------------------//
    /**
     * Computes the starting abscissa for each staff header area, typically the point
     * right after the right-most bar line of the starting bar group.
     *
     * @return measureStart at beginning of staff
     */
    private void computeHeaderStarts ()
    {
        for (Staff staff : system.getStaves()) {
            BarlineInter leftBar = staff.getSideBar(LEFT);

            if (leftBar == null) {
                // No left bar line found, so use the beginning abscissa of lines
                staff.setHeader(new StaffHeader(staff.getAbscissa(LEFT)));
            } else {
                // Retrieve all bar lines grouped at beginning of staff
                // And pick up the last (right-most) barline in the group
                BarlineInter lastBar = leftBar;
                boolean moved;

                do {
                    moved = false;

                    for (Relation rel : sig.getRelations(lastBar, BarGroupRelation.class)) {
                        if (lastBar == sig.getEdgeSource(rel)) {
                            lastBar = (BarlineInter) sig.getEdgeTarget(rel);
                            moved = true;
                        }
                    }
                } while (moved);

                int start = lastBar.getCenterRight().x + 1;
                logger.debug("Staff#{} start:{} bar:{}", staff.getId(), start, lastBar);

                staff.setHeader(new StaffHeader(start));
            }
        }
    }

    //---------------//
    // freezeHeaders //
    //---------------//
    /**
     * Freeze headers components.
     */
    private void freezeHeaders ()
    {
        for (Staff staff : system.getStaves()) {
            staff.getHeader().freeze();
        }
    }

    //---------------//
    // purgeBarlines //
    //---------------//
    /**
     * Delete any barline inter found within headers areas.
     */
    private void purgeBarlines ()
    {
        for (Staff staff : system.getStaves()) {
            final int start = staff.getHeaderStart();
            final int stop = staff.getHeaderStop();

            for (BarlineInter bar : new ArrayList<BarlineInter>(staff.getBars())) {
                final Point center = bar.getCenter();

                if ((center.x > start) && (center.x < stop) && !bar.isStaffEnd(LEFT)) {
                    if (bar.isVip()) {
                        logger.info("Deleting {} in staff#{} header", bar, staff.getId());
                    }

                    bar.delete();
                }
            }
        }
    }

    //-------------------//
    // setSystemClefStop //
    //-------------------//
    /**
     * Refine the header end at system level based on clef info.
     */
    private void setSystemClefStop (int largestOffset)
    {
        // Push this value to all staves
        if (largestOffset > 0) {
            for (Staff staff : system.getStaves()) {
                StaffHeader header = staff.getHeader();
                header.stop = header.start + largestOffset;
            }
        }
    }

    //------------------//
    // setSystemKeyStop //
    //------------------//
    /**
     * Refine the header end at system level based on key info.
     */
    private void setSystemKeyStop (int largestOffset)
    {
        // Push this value to all staves
        if (largestOffset > 0) {
            for (Staff staff : system.getStaves()) {
                StaffHeader header = staff.getHeader();
                header.stop = header.start + largestOffset;
            }
        }
    }

    //-------------------//
    // setSystemTimeStop //
    //-------------------//
    /**
     * Refine the header end at system level based on time info.
     */
    private void setSystemTimeStop (int largestOffset)
    {
        // Push this value to all staves
        if (largestOffset > 0) {
            for (Staff staff : system.getStaves()) {
                StaffHeader header = staff.getHeader();
                header.stop = header.start + largestOffset;
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction maxHeaderWidth = new Scale.Fraction(
                15.0,
                "Maximum header width (from measure start to end of key-sig or time-sig)");
    }
}
