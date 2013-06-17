//----------------------------------------------------------------------------//
//                                                                            //
//                             B a s i c L i n e                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.lang.Math.*;

/**
 * Class {@code BasicLine} is a basic Line implementation which switches
 * between horizontal and vertical equations when computing the points
 * regression
 *
 * @author Hervé Bitteur
 */
public class BasicLine
        implements Line
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            BasicLine.class);

    //~ Instance fields --------------------------------------------------------
    /** Flag to indicate that data needs to be recomputed */
    private boolean dirty;

    /** Orientation indication */
    private boolean isRatherVertical = false;

    /** x coeff in Line equation */
    private double a;

    /** y coeff in Line equation */
    private double b;

    /** 1 coeff in Line equation */
    private double c;

    /** Sigma (x) */
    private double sx;

    /** Sigma (x**2) */
    private double sx2;

    /** Sigma (x*y) */
    private double sxy;

    /** Sigma (y) */
    private double sy;

    /** Sigma (y**2) */
    private double sy2;

    /** For regression : Number of points */
    private int n;

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // BasicLine //
    //-----------//
    /**
     * Creates a line, with no data. The line is no yet usable, except for
     * including further defining points.
     */
    public BasicLine ()
    {
        reset();
    }

    //-----------//
    // BasicLine //
    //-----------//
    /**
     * Creates a line, for which we already know the coefficients. The
     * coefficients don't have to be normalized, the constructor takes care of
     * this. This line is not meant to be modified by including additional
     * points (although this is doable), since it contains no defining points.
     *
     * @param a xCoeff
     * @param b yCoeff
     * @param c 1Coeff
     */
    public BasicLine (double a,
                      double b,
                      double c)
    {
        this();

        this.a = a;
        this.b = b;
        this.c = c;

        normalize();
        dirty = false;

        checkLineParameters();
    }

    //-----------//
    // BasicLine //
    //-----------//
    /**
     * Creates a line (and immediately compute its coefficients), as the least
     * square fitted line on the provided points population.
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
            throw new IllegalArgumentException(
                    "Provided arrays may not be null");
        }

        // Checks for parameter validity
        if (xVals.length != yVals.length) {
            throw new IllegalArgumentException(
                    "Provided arrays have different lengths");
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

    //~ Methods ----------------------------------------------------------------
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

    //------------------//
    // getInvertedSlope //
    //------------------//
    @Override
    public double getInvertedSlope ()
    {
        checkLineParameters();

        return -b / a;
    }

    //-----------------//
    // getMeanDistance //
    //-----------------//
    @Override
    public double getMeanDistance ()
    {
        // Check we have at least 2 points
        if (n < 2) {
            throw new UndefinedLineException(
                    "Not enough defining points : " + n);
        }

        checkLineParameters();

        // abs is used in case of rounding errors
        return sqrt(
                abs(
                (a * a * sx2) + (b * b * sy2) + (c * c * n)
                + (2 * a * b * sxy) + (2 * a * c * sx) + (2 * b * c * sy)) / n);
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
        } else {
            throw new RuntimeException("Combining inconsistent lines");
        }

        dirty = true;

        return this;
    }

    //--------------//
    // includePoint //
    //--------------//
    @Override
    public void includePoint (double x,
                              double y)
    {
        logger.debug("includePoint x={} y={}", x, y);

        n += 1;
        sx += x;
        sy += y;
        sx2 += (x * x);
        sy2 += (y * y);
        sxy += (x * y);

        dirty = true;
    }

    //--------------//
    // isHorizontal //
    //--------------//
    @Override
    public boolean isHorizontal ()
    {
        checkLineParameters();

        return a == 0d;
    }

    //------------//
    // isVertical //
    //------------//
    @Override
    public boolean isVertical ()
    {
        checkLineParameters();

        return b == 0d;
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

        dirty = false;
    }

    //--------------------//
    // swappedCoordinates //
    //--------------------//
    /**
     * Return a new line whose coordinates are swapped with respect to this one
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

            sb.append((float) a)
                    .append("*x ");

            if (b >= 0) {
                sb.append("+");
            }

            sb.append((float) b)
                    .append("*y ");

            if (c >= 0) {
                sb.append("+");
            }

            sb.append((float) c)
                    .append("}");

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

    //---------------------//
    // checkLineParameters //
    //---------------------//
    /**
     * Make sure the line parameters are usable.
     */
    private void checkLineParameters ()
    {
        // Recompute parameters based on points if so needed
        if (dirty) {
            compute();
        }

        // Make sure the parameters are available
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(c)) {
            throw new UndefinedLineException(
                    "Line parameters not properly set");
        }
    }

    //---------//
    // compute //
    //---------//
    /**
     * Compute the line equation, based on the cumulated number of points
     */
    private void compute ()
    {
        if (n < 2) {
            throw new UndefinedLineException(
                    "Not enough defining points : " + n);
        }

        // Make a choice between horizontal vs vertical
        double hDen = (n * sx2) - (sx * sx);
        double vDen = (n * sy2) - (sy * sy);
        logger.debug("hDen={} vDen={}", hDen, vDen);

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
        dirty = false;
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
