//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  I n t e g e r F u n c t i o n                                 //
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
package omr.math;

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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.WindowConstants;

/**
 * Class {@code IntegerFunction} represents a function y = f(x), where x & y are
 * integers.
 * <p>
 * For any x value between xMin and xMax, y is defined as value(x).
 * <p>
 * For any x value, between xMin+1 and xMax, a rough derivative is defined as value(x) - value(x-1)
 *
 * @author Hervé Bitteur
 */
public class IntegerFunction
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(IntegerFunction.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Minimum x value. Often 0. */
    protected final int xMin;

    /** Maximum x value. */
    protected final int xMax;

    /** Array of y value for each x. */
    private final int[] values;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates an instance of {@code IntegerFunction}.
     *
     * @param xMin minimum x value
     * @param xMax maximum x value
     */
    public IntegerFunction (int xMin,
                            int xMax)
    {
        this.xMin = xMin;
        this.xMax = xMax;
        values = new int[xMax - xMin + 1];
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addValue //
    //----------//
    /**
     * Add delta to y value at x
     *
     * @param x     provided x value
     * @param delta increment for y
     */
    public void addValue (int x,
                          int delta)
    {
        values[x - xMin] += delta;
    }

    //---------//
    // getArea //
    //---------//
    /**
     * Report the algebraic area between function and x axis.
     *
     * @return integration on [xMin .. xMax] range
     */
    public int getArea ()
    {
        int area = 0;

        for (int x = xMin; x <= xMax; x++) {
            area += getValue(x);
        }

        return area;
    }

    //---------------//
    // getDerivative //
    //---------------//
    /**
     * Compute (a kind of) derivative at provided x value.
     *
     * @param x provided x value
     * @return derivative for x
     */
    public int getDerivative (int x)
    {
        return getValue(x) - getValue(x - 1);
    }

    //----------------//
    // getLocalMaxima //
    //----------------//
    /**
     * Report the local maximum points, sorted by decreasing value
     *
     * @param xMin minimum x value
     * @param xMax maximum x value
     * @return the collection of local maxima x, ordered by decreasing value
     */
    public List<Integer> getLocalMaxima (int xMin,
                                         int xMax)
    {
        final List<Integer> maxima = new ArrayList<Integer>();
        Integer prevX = null;
        int prevY = 0;
        boolean growing = false;

        for (int x = xMin; x <= xMax; x++) {
            int y = getValue(x);

            if (prevX != null) {
                if (y >= prevY) {
                    growing = true;
                } else {
                    if (growing) {
                        maxima.add(prevX); // End of a local max
                    }

                    growing = false;
                }
            }

            prevX = x;
            prevY = y;
        }

        // Sort by decreasing y values
        Collections.sort(
                maxima,
                new Comparator<Integer>()
        {
            @Override
            public int compare (Integer x1,
                                Integer x2)
            {
                return Integer.compare(getValue(x2), getValue(x1)); // Reverse
            }
        });

        return maxima;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the y value at provided x value
     *
     * @param x provided x value
     * @return the y value for x
     */
    public int getValue (int x)
    {
        return values[x - xMin];
    }

    //---------//
    // getXMax //
    //---------//
    /**
     * Report the highest x
     *
     * @return xMax
     */
    public int getXMax ()
    {
        return xMax;
    }

    //---------//
    // getXMin //
    //---------//
    /**
     * Report the lowest x
     *
     * @return xMin
     */
    public int getXMin ()
    {
        return xMin;
    }

    //--------//
    // mainOf //
    //--------//
    /**
     * Report, within the provided range, the (first) x for highest y.
     *
     * @param xMin x at beginning of range
     * @param xMax x at end of range
     * @return the x for first highest y in range
     */
    public int mainOf (int xMin,
                       int xMax)
    {
        int main = xMin;
        int maxY = getValue(main);

        for (int x = xMin; x <= xMax; x++) {
            int y = getValue(x);

            if (maxY < y) {
                maxY = y;
                main = x;
            }
        }

        return main;
    }

    //-------//
    // print //
    //-------//
    /**
     * Print out the values: x, y, y' for each x.
     *
     * @param stream output stream
     */
    public void print (PrintStream stream)
    {
        stream.print("[\n");

        for (int x = xMin + 1; x <= xMax; x++) {
            int der = getDerivative(x);
            stream.format(" %d:%d/%+d\n", x, getValue(x), der);
        }

        stream.println("]");
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Assign the y value for the x value
     *
     * @param x provided x value
     * @param y provided y value
     */
    public void setValue (int x,
                          int y)
    {
        values[x - xMin] = y;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Plotter //
    //---------//
    /**
     * Class {@code Plotter} plots the integer function.
     */
    public class Plotter
    {
        //~ Instance fields ------------------------------------------------------------------------

        protected final String title;

        protected final int xMax;

        protected final XYSeriesCollection dataset = new XYSeriesCollection();

        protected final JFreeChart chart;

        protected final XYPlot plot;

        protected final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Series index
        protected int index = -1;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Plotter object.
         *
         * @param title  title for this histogram
         * @param xLabel label along x axis
         * @param xMax   maximum x value
         */
        public Plotter (String title,
                        String xLabel,
                        int xMax)
        {
            this.title = title;
            this.xMax = xMax;

            // Chart
            chart = ChartFactory.createXYLineChart(
                    title, // Title
                    xLabel, // X-Axis label
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
        /**
         * Build and display plot at provided screen location.
         *
         * @param location screen location
         */
        public void plot (Point location)
        {
            plot.setRenderer(renderer);

            plotLevelHigh();

            plotValues(); // -------------

            plotLevelMedium();

            plotDerivatives(); // --------

            plotLevelLow();

            plotZero(); // ---------------

            // Hosting frame
            ChartFrame frame = new ChartFrame(title, chart, true);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLocation(location);
            frame.setVisible(true);
        }

        protected void add (XYSeries series,
                            Color color,
                            boolean displayShapes)
        {
            dataset.addSeries(series);
            renderer.setSeriesPaint(++index, color);
            renderer.setSeriesShapesVisible(index, displayShapes);
        }

        protected void plotLevelHigh ()
        {
        }

        protected void plotLevelLow ()
        {
        }

        protected void plotLevelMedium ()
        {
        }

        protected void plotValues ()
        {
            XYSeries valueSeries = new XYSeries("Value");

            for (int x = xMin; x <= xMax; x++) {
                valueSeries.add(x, getValue(x));
            }

            add(valueSeries, Color.RED, false);
        }

        private void plotDerivatives ()
        {
            XYSeries derivative = new XYSeries("Derivative");

            for (int i = xMin + 1; i <= xMax; i++) {
                derivative.add(i, getDerivative(i));
            }

            add(derivative, Color.BLUE, false);
        }

        private void plotZero ()
        {
            XYSeries zeroSeries = new XYSeries("Zero");
            zeroSeries.add(xMin, 0);
            zeroSeries.add(xMax, 0);
            add(zeroSeries, Color.WHITE, false);
        }
    }
}
