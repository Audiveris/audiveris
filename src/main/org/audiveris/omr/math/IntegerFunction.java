//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  I n t e g e r F u n c t i o n                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.math;

import org.jfree.data.xy.XYSeries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code IntegerFunction} represents a function y = f(x), where x &amp; y are
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
    /** Minimum x value. */
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

    //--------//
    // argMax //
    //--------//
    /**
     * Report the (first) x for highest y value (within the provided x domain).
     *
     * @param x1 x at beginning of domain
     * @param x2 x at end of domain
     * @return the x for first highest y in domain
     */
    public int argMax (int x1,
                       int x2)
    {
        int main = x1;
        int maxY = getValue(main);

        for (int x = x1; x <= x2; x++) {
            int y = getValue(x);

            if (maxY < y) {
                maxY = y;
                main = x;
            }
        }

        return main;
    }

    //---------//
    // getArea //
    //---------//
    /**
     * Report the area above x axis.
     *
     * @return integration on [xMin .. xMax] domain
     */
    public int getArea ()
    {
        return getArea(xMin, xMax, 0);
    }

    //---------//
    // getArea //
    //---------//
    /**
     * Report the area between function and y line, from x1 to x2.
     *
     * @param x1 x at beginning of domain
     * @param x2 x at end of domain
     * @param y  minimum value
     * @return integration on [x1 .. x2] domain above y
     */
    public int getArea (int x1,
                        int x2,
                        int y)
    {
        int area = 0;

        for (int x = x1; x <= x2; x++) {
            area += Math.max(0, getValue(x) - y);
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

    //---------------------//
    // getDerivativeSeries //
    //---------------------//
    /**
     * Report the XY series for function derivatives between x1 and x2.
     *
     * @param x1 lower value for x (x1 must be &gt; xMin)
     * @param x2 upper value for x
     * @return the XY derivatives ready to plot
     */
    public XYSeries getDerivativeSeries (int x1,
                                         int x2)
    {
        XYSeries derivativeSeries = new XYSeries("Derivative", false); // No autosort

        for (int i = x1; i <= x2; i++) {
            derivativeSeries.add(i, getDerivative(i));
        }

        return derivativeSeries;
    }

    //---------------------//
    // getDerivativeSeries //
    //---------------------//
    /**
     * Report the XY series for function derivatives up to provided x2.
     *
     * @param x2 upper value for x
     * @return the XY derivatives ready to plot
     */
    public XYSeries getDerivativeSeries (int x2)
    {
        return getDerivativeSeries(xMin + 1, x2);
    }

    //---------------------//
    // getDerivativeSeries //
    //---------------------//
    /**
     * Report the XY series for function derivatives on whole x domain.
     *
     * @return the XY derivatives ready to plot
     */
    public XYSeries getDerivativeSeries ()
    {
        return getDerivativeSeries(xMin + 1, xMax);
    }

    //----------------//
    // getLocalMaxima //
    //----------------//
    /**
     * Report the local maximum points, sorted by decreasing value
     *
     * @param x1 minimum x value
     * @param x2 maximum x value
     * @return the collection of local maxima x, ordered by decreasing value
     */
    public List<Integer> getLocalMaxima (int x1,
                                         int x2)
    {
        // Check range
        x1 = Math.max(x1, xMin);
        x2 = Math.min(x2, xMax);

        final List<Integer> maxima = new ArrayList<Integer>();
        Integer prevX = null;
        int prevY = 0;
        boolean growing = false;

        for (int x = x1; x <= x2; x++) {
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

    //----------------//
    // getValueSeries //
    //----------------//
    /**
     * Report the XY series for function values from x1 to x2.
     *
     * @param x1 lower value for x
     * @param x2 upper value for x
     * @return the XY values ready to plot
     */
    public XYSeries getValueSeries (int x1,
                                    int x2)
    {
        XYSeries valueSeries = new XYSeries("Value", false); // No autosort

        for (int x = x1; x <= x2; x++) {
            valueSeries.add(x, getValue(x));
        }

        return valueSeries;
    }

    //----------------//
    // getValueSeries //
    //----------------//
    /**
     * Report the XY series for function values up to provided xMax.
     *
     * @param x2 upper value for x
     * @return the XY values ready to plot
     */
    public XYSeries getValueSeries (int x2)
    {
        return getValueSeries(xMin, x2);
    }

    //----------------//
    // getValueSeries //
    //----------------//
    /**
     * Report the XY series for function values on whole x domain.
     *
     * @return the XY values ready to plot
     */
    public XYSeries getValueSeries ()
    {
        return getValueSeries(xMin, xMax);
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

    //---------------//
    // getZeroSeries //
    //---------------//
    /**
     * Report 0 value on whole x domain.
     *
     * @return the zero series ready to plot
     */
    public XYSeries getZeroSeries ()
    {
        return getZeroSeries(xMin, xMax);
    }

    //---------------//
    // getZeroSeries //
    //---------------//
    /**
     * Report 0 value until provided x2.
     *
     * @param x2 upper value for x
     * @return the zero series ready to plot
     */
    public XYSeries getZeroSeries (int x2)
    {
        return getZeroSeries(xMin, x2);
    }

    //---------------//
    // getZeroSeries //
    //---------------//
    /**
     * Report 0 value from x1 to x2.
     *
     * @param x1 lower value for x
     * @param x2 upper value for x
     * @return the zero series ready to plot
     */
    public XYSeries getZeroSeries (int x1,
                                   int x2)
    {
        XYSeries zeroSeries = new XYSeries("Zero", false); // No autosort
        zeroSeries.add(x1, 0);
        zeroSeries.add(x2, 0);

        return zeroSeries;
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
}
