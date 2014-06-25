//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       D m z B u i l d e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.ConstantSet;

import omr.grid.StaffInfo;

import omr.math.GeoOrder;

import omr.sig.BarGroupRelation;
import omr.sig.BarlineInter;
import omr.sig.Inter;
import omr.sig.Relation;
import omr.sig.SIGraph;
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
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.WindowConstants;

/**
 * Class {@code DmzBuilder} handles the DMZ at the beginning of a given system.
 * <p>
 * A staff always begins with the standard sequence of 3 kinds of components: (clef, key-sig?,
 * time-sig?). It is referred to as the "DMZ" (DeMilitarized Zone) in this program, because it
 * contains nothing but the 3 kinds of components.
 * <p>
 * Within the same system, the DMZ components (whether present or absent) are vertically aligned
 * across the system staves. This alignment feature is used to cross-validate the horizontal limits
 * of components among system staves, and even the horizontal limits of items slices within the
 * key-sig components.
 * <p>
 * Cross-validation at system level: <ul>
 * <li>Clefs:
 * A clef is mandatory at the beginning of each staff (in the DMZ) and may be different from one
 * staff to the other.
 * Outside the DMZ, a new clef (generally smaller in size) may appear anywhere, regardless of the
 * other staves.
 * <li>Key signatures:
 * A key signature may vary (and even be void) from one staff to the other.
 * If present, the alteration slices are aligned across staves.
 * <li>Time signatures:
 * Inside or outside DMZ, either a time signature is present and identical in every staff of the
 * system, or they are all absent.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class DmzBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(DmzBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    private final SIGraph sig;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Manager for column of clefs. */
    private final ClefBuilder.Column clefColumn;

    /** Manager for column of keys signatures. */
    private final KeyBuilder.Column keyColumn;

    /** Manager for column of time signatures. */
    private final TimeBuilder.Column timeColumn;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new DmzBuilder object.
     *
     * @param system the system to findClef
     */
    public DmzBuilder (SystemInfo system)
    {
        this.system = system;
        sig = system.getSig();
        sheet = system.getSheet();

        params = new Parameters(sheet.getScale());
        clefColumn = new ClefBuilder.Column(system);
        keyColumn = new KeyBuilder.Column(system);
        timeColumn = new TimeBuilder.Column(system);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // plot //
    //------//
    /**
     * Display the projection of the desired staff.
     *
     * @param staff the desired staff
     */
    public void plot (StaffInfo staff)
    {
        new ChartPlotter(staff).plot();
    }

    //------------//
    // processDmz //
    //------------//
    /**
     * Process the DMZ at the beginning of all staves in the system.
     */
    public void processDmz ()
    {
        logger.debug("processDmz for S#{}", system.getId());

        // Compute DMZ starts abscissae based on bar lines (or staff left abscissa)
        // and store the information in staves themselves.
        computeDmzStarts();

        // Retrieve DMZ clefs
        int clefOffset = clefColumn.retrieveClefs();
        refineDmz(clefOffset);

        // Retrieve DMZ key-sigs
        int keyOffset = keyColumn.retrieveKeys(params.maxDmzWidth);
        refineDmz(keyOffset);

        // Retrieve DMZ time-sigs
        int timeOffset = timeColumn.retrieveTime(params.maxDmzWidth);
        refineDmz(timeOffset);
    }

    //------------------//
    // computeDmzStarts //
    //------------------//
    /**
     * Computes the starting abscissa for each staff DMZ area, typically the point right
     * after the right-most bar line of the starting bar group.
     * TODO: could this be a more general routine in StaffInfo?
     *
     * @return measureStart at beginning of staff
     */
    private void computeDmzStarts ()
    {
        /** System bar lines, sorted on abscissa. */
        final List<Inter> systemBars = sig.inters(BarlineInter.class);
        Collections.sort(systemBars, Inter.byAbscissa);

        int margin = sheet.getScale().getInterline(); // Roughly

        for (StaffInfo staff : system.getStaves()) {
            Point2D leftPt = staff.getFirstLine().getEndPoint(LEFT);
            Rectangle luBox = new Rectangle(
                    (int) Math.floor(leftPt.getX()),
                    (int) Math.rint(leftPt.getY() + (staff.getHeight() / 2)),
                    margin,
                    0);
            luBox.grow(0, margin);

            TreeSet<Inter> bars = new TreeSet<Inter>(Inter.byAbscissa);
            bars.addAll(sig.intersectedInters(systemBars, GeoOrder.BY_ABSCISSA, luBox));

            if (bars.isEmpty()) {
                // No bar line found, so use the beginning abscissa of lines
                staff.setDmzStart((int) Math.rint(leftPt.getX()));
            } else {
                // Retrieve all bar lines grouped at beginning of staff
                Set<Inter> toAdd = new HashSet<Inter>();

                for (Inter inter : bars) {
                    Set<Relation> gRels = sig.getRelations(inter, BarGroupRelation.class);

                    for (Relation rel : gRels) {
                        toAdd.add(sig.getOppositeInter(inter, rel));
                    }
                }

                bars.addAll(toAdd);

                // Pick up the right-most bar line in the group
                BarlineInter last = (BarlineInter) bars.last();
                int right = last.getCenterRight().x + 1;
                logger.debug("Staff#{} right:{} bars: {}", staff.getId(), right, bars);

                staff.setDmzStart(right);
            }
        }
    }

    //-----------//
    // refineDmz //
    //-----------//
    /**
     * Refine the DMZ end at system level.
     * DMZ areas are vertically aligned within a system, even if key-sigs may vary between staves.
     * So we retrieve the largest offset since measureStart and use it to set the DMZ end of each
     * staff.
     */
    private void refineDmz (int largestOffset)
    {
        // Push this value to all staves
        if (largestOffset > 0) {
            for (StaffInfo staff : system.getStaves()) {
                staff.setDmzStop(staff.getDmzStart() + largestOffset);
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

        private final StaffInfo staff;

        private final XYSeriesCollection dataset = new XYSeriesCollection();

        // Chart
        private final JFreeChart chart;

        private final XYPlot plot;

        private final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        private final StringBuilder title = new StringBuilder();

        // Series index
        private int index = -1;

        //~ Constructors ---------------------------------------------------------------------------
        public ChartPlotter (StaffInfo staff)
        {
            this.staff = staff;
            title.append(sheet.getId()).append(" DMZ staff#").append(staff.getId());

            chart = ChartFactory.createXYLineChart(
                    title.toString(), // Title
                    "Abscissae " + sheet.getScale(), // X-Axis label
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
            // Draw the zero reference line
            {
                final int xMin = staff.getDmzStart();
                final int xMax = xMin + params.maxDmzWidth;

                XYSeries series = new XYSeries("Zero");
                series.add(xMin, -MARK);
                series.add(xMin, 0);
                series.add(xMax, 0);
                series.add(xMax, -MARK);
                add(series, Color.WHITE, false);
            }

            // Hosting frame
            if (keyString != null) {
                title.append(" ").append(keyString);
            }

            if (timeString != null) {
                title.append(" ").append(timeString);
            }

            chart.setTitle(title.toString());

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

        final Scale.Fraction maxDmzWidth = new Scale.Fraction(
                15.0,
                "Maximum DMZ width (from measure start to end of key-sig or time-sig)");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int maxDmzWidth;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxDmzWidth = scale.toPixels(constants.maxDmzWidth);
        }
    }
}
