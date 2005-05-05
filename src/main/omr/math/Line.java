//-----------------------------------------------------------------------//
//                                                                       //
//                                L i n e                                //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.math;

import omr.util.Logger;

/**
 * Class <code>Line</code> handles the equation of a line, in the form y =
 * ax + b.
 *
 * <p> Note that this is not suitable for near vertical lines, which would
 * be better described by the VerticalLine class. </p>
 */
public class Line
        implements java.io.Serializable
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Line.class);

    //~ Instance variables ------------------------------------------------

    // Coeffs of y = a.x +b

    /** slope */
    protected double a;

    /** offset */
    protected double b;

    /** normalizing factor : sqrt(1 + (a**2) */
    protected double norm;

    // For incremental fitting

    /** Number of points */
    protected int n;

    /** Sigma (x) */
    protected double sx;

    /** Sigma (y) */
    protected double sy;

    /** Sigma (x**2) */
    protected double sx2;

    /** Sigma (y**2) */
    protected double sy2;

    /** Sigma (x*y) */
    protected double sxy;

    /** Flag to indicate that data needs to be recomputed */
    protected boolean dirty;

    //~ Constructors ------------------------------------------------------

    //------//
    // Line //
    //------//
    /**
     * Creates a line, with no data.
     */
    public Line ()
    {
        reset();
    }

    //------//
    // Line //
    //------//
    /**
     * Creates a line, for which we already know the coefficients
     *
     * @param a slope
     * @param b offset
     */
    public Line (double a,
                 double b)
    {
        reset();
        this.a = a;
        this.b = b;
        normalize();
        dirty = false;
    }

    //------//
    // Line //
    //------//
    /**
     * Creates a line (and its coefficients), as the least square fitted
     * line on the provided point population
     *
     * @param xVals abscissas of the points
     * @param yVals ordinates of the points
     */
    public Line (double[] xVals,
                 double[] yVals)
    {
        if (logger.isDebugEnabled()) {
            logger.logAssert
                (xVals.length == yVals.length,
                 "leastSquareFit. xVals & yVals have different lengths");
            logger.logAssert
                (xVals.length >= 2,
                 "leastSquareFit. Less than 2 points");
        }

        reset();

        for (int i = xVals.length - 1; i >= 0; i--) {
            include(xVals[i], yVals[i]);
        }

        compute();
    }

    //~ Methods -----------------------------------------------------------

    //-----------------//
    // getMeanDistance //
    //-----------------//
    /**
     * Return the mean distance of the defining population of point to the
     * line. This can be used to measure how well the line fits the points
     *
     * @return this distance
     */
    public double getMeanDistance ()
    {
        if (dirty) {
            compute();
        }

        return Math.sqrt(sy2 - (2 * a * sxy) - (2 * b * sy) + (a * a * sx2)
                         + (2 * a * b * sx) + (n * b * b)) / n;
    }

    //-----------//
    // getOffset //
    //-----------//
    /**
     * Return the initial ordinate (the offset) of the line, that is the y
     * value when x equals 0.
     *
     * @return the offset
     */
    public double getOffset ()
    {
        if (dirty) {
            compute();
        }

        return b;
    }

    //------------//
    // getPointNb //
    //------------//
    /**
     * Return the cardinality of the population of defining points
     *
     * @return the number of defining points so far
     */
    public int getPointNb ()
    {
        return n;
    }

    //----------//
    // getSlope //
    //----------//
    /**
     * Return the gain of the line, that is the y/x coefficient
     *
     * @return the slope
     */
    public double getSlope ()
    {
        if (dirty) {
            compute();
        }

        return a;
    }

    //------------//
    // distanceOf //
    //------------//
    /**
     * Compute the orthogonal distance between the line and the provided
     * point. Note that the distance may be negative.
     *
     * @param x the point abscissa
     * @param y the point ordinate
     *
     * @return the algebraic orthogonal distance
     */
    public double distanceOf (double x,
                              double y)
    {
        if (dirty) {
            compute();
        }

        return ((a * x) - y + b) / norm;
    }

    //---------//
    // include //
    //---------//
    /**
     * Add the coordinates of a point in the defining population of points
     *
     * @param x abscissa of the new point
     * @param y ordinate of the new point
     */
    public void include (double x,
                         double y)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("include x=" + x + " y=" + y);
        }

        n += 1;
        sx += x;
        sy += y;
        sx2 += (x * x);
        sy2 += (y * y); // For distance
        sxy += (x * y);
        dirty = true;
    }

    //---------//
    // include //
    //---------//
    /**
     * Add the whole population of another line, which results in merging
     * this other line with the line at hand.
     *
     * @param other the other line
     *
     * @return this auglented line, which permits to chain the additions
     */
    public Line include (Line other)
    {
        n += other.n;
        sx += other.sx;
        sy += other.sy;
        sx2 += other.sx2;
        sy2 += other.sy2;
        sxy += other.sxy;
        dirty = true;

        return this;
    }

    //-------//
    // reset //
    //-------//
    /**
     * Remove the whole population of points
     */
    public void reset ()
    {
        a = b = norm = 0d;
        n = 0;
        sx = sy = sx2 = sy2 = sxy = 0d;
        dirty = true;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Return a readable equation for the line
     *
     * @return the string
     */
    public String toString ()
    {
        if (dirty) {
            compute();
        }

        StringBuffer sb = new StringBuffer();
        sb
            .append("{Line")
            .append(" y = ")
            .append((float) a)
            .append("*x ");

        if (b >= 0) {
            sb.append("+");
        }

        sb
            .append((float) b)
            .append("}");

        return sb.toString();
    }

    //-----//
    // xAt //
    //-----//
    /**
     * Retrieve the abscissa where the line crosses the given ordinate y.
     * Beware of horizontal lines !!!
     *
     * @param y the imposed ordinate
     *
     * @return the corresponding x value
     */
    public double xAt (double y)
    {
        if (dirty) {
            compute();
        }

        return (y - b) / a;
    }

    //-----//
    // xAt //
    //-----//
    /**
     * Retrieve the abscissa where the line crosses the given ordinate y.
     * Beware of horizontal lines !!!
     *
     * @param y the imposed ordinate
     *
     * @return the corresponding x value
     */
    public int xAt (int y)
    {
        return (int) Math.rint(xAt((double) y));
    }

    //-----//
    // yAt //
    //-----//
    /**
     * Retrieve the ordinate where the line crosses the given abscissa x.
     * Beware of vertical lines !!!
     *
     * @param x the imposed abscissa
     *
     * @return the corresponding y value
     */
    public double yAt (double x)
    {
        if (dirty) {
            compute();
        }

        return (a * x) + b;
    }

    //-----//
    // yAt //
    //-----//
    /**
     * Retrieve the ordinate where the line crosses the given abscissa x.
     * Beware of vertical lines !!!
     *
     * @param x the imposed abscissa
     *
     * @return the corresponding y value
     */
    public int yAt (int x)
    {
        return (int) Math.rint(yAt((double) x));
    }

    //---------//
    // compute //
    //---------//
    /**
     * Compute the line equation, based on the cumulated number of points
     */
    protected void compute ()
    {
        if (logger.isDebugEnabled()) {
            logger.logAssert(n >= 2, "compute. Less than 2 points");
        }

        double den = (n * sx2) - (sx * sx);

        if (logger.isDebugEnabled()) {
            logger.debug("den=" + den);
        }

        a = ((n * sxy) - (sx * sy)) / den;
        b = ((sy * sx2) - (sx * sxy)) / den;
        normalize();
        dirty = false;
    }

    //-----------//
    // normalize //
    //-----------//
    /**
     * Compute the normalizing factor
     */
    protected void normalize ()
    {
        norm = Math.sqrt(1 + (a * a));
    }
}
