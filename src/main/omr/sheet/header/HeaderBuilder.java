//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     H e a d e r B u i l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.header;

import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.ClefInter;
import omr.sig.relation.BarGroupRelation;
import omr.sig.relation.Relation;
import static omr.util.HorizontalSide.LEFT;
import omr.util.Navigable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

import javax.swing.WindowConstants;

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
    private final KeyBuilder.Column keyColumn;

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
        keyColumn = new KeyBuilder.Column(system);
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
        new ChartPlotter(staff).plot();
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

        // We should be able here to select the best clef for each staff
        clefColumn.selectClefs();

        // Purge barline inters found within headers
        purgeBarlines();
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
                header.stop = header.clefRange.systemStop = header.start + largestOffset;
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
                header.stop = header.keyRange.systemStop = header.start + largestOffset;
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
                header.stop = header.timeRange.systemStop = header.start + largestOffset;
            }
        }
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //---------//
    // Plotter //
    //---------//
    public interface Plotter
    {
        //~ Static fields/initializers -------------------------------------------------------------

        /** Height of marks below zero line. */
        static double MARK = 2.5;

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Add a point series to the plotter.
         *
         * @param series        the series to add
         * @param color         display color
         * @param displayShapes true to display a shape at each point location
         */
        void add (XYSeries series,
                  Color color,
                  boolean displayShapes);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // ChartPlotter //
    //--------------//
    /**
     * Handles the display of projection chart.
     */
    private class ChartPlotter
            implements Plotter
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Staff staff;

        private final XYSeriesCollection dataset = new XYSeriesCollection();

        // Chart
        private final JFreeChart chart;

        private final XYPlot plot;

        private final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        private final StringBuilder title = new StringBuilder();

        // Series index
        private int index = -1;

        //~ Constructors ---------------------------------------------------------------------------
        public ChartPlotter (Staff staff)
        {
            this.staff = staff;

            Sheet sheet = system.getSheet();
            title.append(sheet.getId()).append(" header staff#").append(staff.getId());

            chart = ChartFactory.createXYLineChart(
                    title.toString(), // Title
                    "Abscissae - staff interline:" + staff.getSpecificInterline(), // X-Axis label
                    "Counts", // Y-Axis label
                    dataset, // Dataset
                    PlotOrientation.VERTICAL, // orientation
                    true, // Show legend
                    false, // Show tool tips
                    false // urls
            );

            plot = (XYPlot) chart.getPlot();
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void add (XYSeries series,
                         Color color,
                         boolean displayShapes)
        {
            dataset.addSeries(series);
            renderer.setSeriesPaint(++index, color);
            renderer.setSeriesShapesVisible(index, displayShapes);
        }

        public void plot ()
        {
            plot.setRenderer(renderer);

            // Draw time sig portion
            String timeString = timeColumn.addPlot(this, staff);

            // Draw key sig portion
            String keyString = keyColumn.addPlot(this, staff);

            // Get clef info
            ClefInter clef = staff.getHeader().clef;
            Shape clefShape = (clef != null) ? clef.getShape() : null;
            String clefString = (clef != null)
                    ? (clefShape
                       + ((clefShape == Shape.C_CLEF) ? (" " + clef.getKind()) : "")) : null;

            // Draw the zero reference line
            {
                final int xMin = staff.getHeaderStart();
                final int xMax = xMin + maxHeaderWidth;

                XYSeries series = new XYSeries("Zero");
                series.add(xMin, -MARK);
                series.add(xMin, 0);
                series.add(xMax, 0);
                series.add(xMax, -MARK);
                add(series, Color.WHITE, false);
            }

            if (clefString != null) {
                title.append(" ").append(clefString);
            }

            if (keyString != null) {
                title.append(" ").append(keyString);
            }

            if (timeString != null) {
                title.append(" ").append(timeString);
            }

            chart.setTitle(title.toString());

            // Hosting frame
            ChartFrame frame = new ChartFrame(title.toString(), chart, true);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLocation(new Point(20 * staff.getId(), 20 * staff.getId()));
            frame.setVisible(true);
        }
    }

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
