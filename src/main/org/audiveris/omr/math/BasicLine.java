//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B a s i c L i n e                                        //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import static java.lang.Math.*;
import java.util.Collection;

/**
 * Class {@code BasicLine} is a basic Line implementation which switches between
 * horizontal and vertical equations when computing the points regression
 *
 * @author Hervé Bitteur
 */
public class BasicLine
        implements Line
{

    private static final Logger logger = LoggerFactory.getLogger(BasicLine.class);

    /** Flag to indicate that data needs to be recomputed. */
    private boolean dirty;

    /** Orientation indication. */
    private boolean isRatherVertical = false;

    /** x coeff in Line equation. */
    private double a;

    /** y coeff in Line equation. */
    private double b;

    /** 1 coeff in Line equation. */
    private double c;

    /** Sigma (x). */
    private double sx;

    /** Sigma (x**2). */
    private double sx2;

    /** Sigma (x*y). */
    private double sxy;

    /** Sigma (y). */
    private double sy;

    /** Sigma (y**2). */
    private double sy2;

    /** For regression : Number of points. */
    private int n;

    /** Minimum abscissa among all defining points. */
    private double xMin = Double.MAX_VALUE;

    /** Maximum abscissa among all defining points. */
    private double xMax = Double.MIN_VALUE;

    /** Minimum ordinate among all defining points. */
    private double yMin = Double.MAX_VALUE;

    /** Maximum ordinate among all defining points. */
    private double yMax = Double.MIN_VALUE;

    /**
     * Creates a line, with no data.
     * The line is no yet usable, except for including further defining points.
     */
    public BasicLine ()
    {
        reset();
    }

    /**
     * Create a line (and immediately compute its coefficients), as the least square
     * fitted line on the provided points.
     *
     * @param xVals abscissas of the points
     * @param yVals ordinates of the points
     */
    public BasicLine (double[] xVals,
                      double[] yVals)
    {
        this();

        // Checks for parameter validity
        if ((xVals == null) || (yVals == null)) {
            throw new IllegalArgumentException("Provided arrays may not be null");
        }

        // Checks for parameter validity
        if (xVals.length != yVals.length) {
            throw new IllegalArgumentException("Provided arrays have different lengths");
        }

        // Checks for parameter validity
        if (xVals.length < 2) {
            throw new IllegalArgumentException("Provided arrays are too short");
        }

        // Include all defining points
        for (int i = xVals.length - 1; i >= 0; i--) {
            includePoint(xVals[i], yVals[i]);
        }

        checkLineParameters();
    }

    /**
     * Create a line (and immediately compute its coefficients), as the least square
     * fitted line on the provided points.
     *
     * @param points collection of points
     */
    public BasicLine (Collection<? extends Point2D> points)
    {
        this();

        // Checks for parameter validity
        if ((points == null) || (points.size() < 2)) {
            throw new IllegalArgumentException("Points collection is null or too small");
        }

        // Include all defining points
        for (Point2D point : points) {
            includePoint(point.getX(), point.getY());
        }

        checkLineParameters();
    }

    //---------------------//
    // checkLineParameters //
    //---------------------//
    /**
     * Make sure the line parameters are usable.
     */
    public void checkLineParameters ()
    {
        // Recompute parameters based on points if so needed
        if (dirty) {
            compute();
        }

        // Make sure the parameters are available
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(c)) {
            throw new UndefinedLineException("Line parameters not properly set");
        }
    }

    //------------//
    // distanceOf //
    //------------//
    @Override
    public double distanceOf (double x,
                              double y)
    {
        checkLineParameters();

        return (a * x) + (b * y) + c;
    }

    //------------//
    // distanceOf //
    //------------//
    public double distanceOf (Point2D point)
    {
        return distanceOf(point.getX(), point.getY());
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        return toDouble().getBounds();
    }

    //------------------//
    // getInvertedSlope //
    //------------------//
    @Override
    public double getInvertedSlope ()
    {
        checkLineParameters();

        return -b / a;
    }

    //----------------//
    // getMaxAbscissa //
    //----------------//
    /**
     * @return the xMax
     */
    public double getMaxAbscissa ()
    {
        return xMax;
    }

    //----------------//
    // getMaxOrdinate //
    //----------------//
    /**
     * @return the yMax
     */
    public double getMaxOrdinate ()
    {
        return yMax;
    }

    //-----------------//
    // getMeanDistance //
    //-----------------//
    @Override
    public double getMeanDistance ()
    {
        // Check we have at least 2 points
        if (n < 2) {
            throw new UndefinedLineException("Not enough defining points: " + n);
        }

        checkLineParameters();

        double distSq = ((a * a * sx2) + (b * b * sy2) + (c * c * n) + (2 * a * b * sxy) + (2 * a
                                                                                                    * c
                                                                                            * sx)
                                 + (2 * b * c * sy)) / n;

        if (distSq < 0) {
            distSq = 0;
        }

        return sqrt(distSq);
    }

    //----------------//
    // getMinAbscissa //
    //----------------//
    /**
     * @return the xMin
     */
    public double getMinAbscissa ()
    {
        return xMin;
    }

    //----------------//
    // getMinOrdinate //
    //----------------//
    /**
     * @return the yMin
     */
    public double getMinOrdinate ()
    {
        return yMin;
    }

    //-------------------//
    // getNumberOfPoints //
    //-------------------//
    @Override
    public int getNumberOfPoints ()
    {
        return n;
    }

    //----------//
    // getSlope //
    //----------//
    @Override
    public double getSlope ()
    {
        checkLineParameters();

        return -a / b;
    }

    //-------------//
    // includeLine //
    //-------------//
    @Override
    public Line includeLine (Line other)
    {
        if (other instanceof BasicLine) {
            BasicLine o = (BasicLine) other;

            n += o.n;
            sx += o.sx;
            sy += o.sy;
            sx2 += o.sx2;
            sy2 += o.sy2;
            sxy += o.sxy;

            xMin = Math.min(xMin, o.xMin);
            xMax = Math.max(xMax, o.xMax);
            yMin = Math.min(yMin, o.yMin);
            yMax = Math.max(yMax, o.yMax);
        } else {
            throw new RuntimeException("Combining inconsistent lines");
        }

        dirty = true;

        return this;
    }

    //--------------//
    // includePoint //
    //--------------//
    public void includePoint (Point2D point)
    {
        includePoint(point.getX(), point.getY());
    }

    //--------------//
    // includePoint //
    //--------------//
    @Override
    public void includePoint (double x,
                              double y)
    {
        logger.trace("includePoint x={} y={}", x, y);

        n += 1;
        sx += x;
        sy += y;
        sx2 += (x * x);
        sy2 += (y * y);
        sxy += (x * y);

        xMin = Math.min(xMin, x);
        xMax = Math.max(xMax, x);
        yMin = Math.min(yMin, y);
        yMax = Math.max(yMax, y);

        dirty = true;
    }

    //-------//
    // reset //
    //-------//
    @Override
    public void reset ()
    {
        a = b = c = Double.NaN;
        n = 0;
        sx = sy = sx2 = sy2 = sxy = 0d;
        xMin = yMin = Double.MAX_VALUE;
        xMax = yMax = Double.MIN_VALUE;

        dirty = false;
    }

    //--------------------//
    // swappedCoordinates //
    //--------------------//
    /**
     * Return a new line whose coordinates are swapped with respect to this one.
     *
     * @return a new X/Y swapped line
     */
    @Override
    public Line swappedCoordinates ()
    {
        BasicLine that = new BasicLine();

        that.n = n;
        that.sx = sy;
        that.sy = sx;
        that.sx2 = sy2;
        that.sy2 = sx2;
        that.sxy = sxy;

        that.dirty = true;

        return that;
    }

    //----------//
    // toDouble //
    //----------//
    /**
     * Generate a Line2D.Double instance that matches line end points
     *
     * @return a Line2D.Double instance
     */
    public Line2D.Double toDouble ()
    {
        try {
            checkLineParameters();

            if (isRatherVertical) {
                return new Line2D.Double(xAtY(yMin), yMin, xAtY(yMax), yMax);
            } else {
                return new Line2D.Double(xMin, yAtX(xMin), xMax, yAtX(xMax));
            }
        } catch (UndefinedLineException ulex) {
            return null; // Not enough points
        }
    }

    //--------//
    // toPath //
    //--------//
    @Override
    public GeoPath toPath ()
    {
        return new GeoPath(toDouble());
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        try {
            if (dirty) {
                compute();
            }

            StringBuilder sb = new StringBuilder();

            if (isRatherVertical) {
                sb.append("{VLine ");
            } else {
                sb.append("{HLine ");
            }

            if (a >= 0) {
                sb.append(" ");
            }

            sb.append((float) a).append("*x ");

            if (b >= 0) {
                sb.append("+");
            }

            sb.append((float) b).append("*y ");

            if (c >= 0) {
                sb.append("+");
            }

            sb.append((float) c).append("}");

            return sb.toString();
        } catch (UndefinedLineException ex) {
            return "INVALID LINE";
        }
    }

    //------//
    // xAtY //
    //------//
    @Override
    public double xAtY (double y)
    {
        if (n == 1) {
            return sx;
        }

        checkLineParameters();

        if (a != 0d) {
            return -((b * y) + c) / a;
        } else {
            throw new NonInvertibleLineException("Line is horizontal");
        }
    }

    //------//
    // xAtY //
    //------//
    @Override
    public int xAtY (int y)
    {
        return (int) rint(xAtY((double) y));
    }

    //---------//
    // xAtYExt //
    //---------//
    @Override
    public double xAtYExt (double y)
    {
        return xAtY(y);
    }

    //------//
    // yAtX //
    //------//
    @Override
    public double yAtX (double x)
    {
        if (n == 1) {
            return sy;
        }

        checkLineParameters();

        if (b != 0d) {
            return ((-a * x) - c) / b;
        } else {
            throw new NonInvertibleLineException("Line is vertical");
        }
    }

    //------//
    // yAtX //
    //------//
    @Override
    public int yAtX (int x)
    {
        return (int) rint(yAtX((double) x));
    }

    //---------//
    // yAtXExt //
    //---------//
    @Override
    public double yAtXExt (double x)
    {
        return yAtX(x);
    }

    //------//
    // getA // Meant for test
    //------//
    double getA ()
    {
        checkLineParameters();

        return a;
    }

    //------//
    // getB // Meant for test
    //------//
    double getB ()
    {
        checkLineParameters();

        return b;
    }

    //------//
    // getC // Meant for test
    //------//
    double getC ()
    {
        checkLineParameters();

        return c;
    }

    //---------//
    // compute //
    //---------//
    /**
     * Compute the line equation, based on the cumulated number of points
     */
    private void compute ()
    {
        dirty = false;

        if (n < 2) {
            throw new UndefinedLineException("Not enough defining points : " + n);
        }

        // Make a choice between horizontal vs vertical
        double hDen = (n * sx2) - (sx * sx);
        double vDen = (n * sy2) - (sy * sy);
        logger.trace("hDen={} vDen={}", hDen, vDen);

        if (abs(hDen) >= abs(vDen)) {
            // Use a rather horizontal orientation, y = mx +p
            isRatherVertical = false;
            a = ((n * sxy) - (sx * sy)) / hDen;
            b = -1d;
            c = ((sy * sx2) - (sx * sxy)) / hDen;
        } else {
            // Use a rather vertical orientation, x = my +p
            isRatherVertical = true;
            a = -1d;
            b = ((n * sxy) - (sx * sy)) / vDen;
            c = ((sx * sy2) - (sy * sxy)) / vDen;
        }

        normalize();
    }

    //-----------//
    // normalize //
    //-----------//
    /**
     * Compute the distance normalizing factor
     */
    private void normalize ()
    {
        double norm = hypot(a, b);
        a /= norm;
        b /= norm;
        c /= norm;
    }
}
