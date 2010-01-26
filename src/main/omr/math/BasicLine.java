//----------------------------------------------------------------------------//
//                                                                            //
//                             B a s i c L i n e                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import omr.log.Logger;

import omr.util.Implement;
import static java.lang.Math.*;

/**
 * Class <code>BasicLine</code> is a basic Line implementation which switches
 * between horizontal and vertical equations when computing the points
 * regression
 *
 * @author Herv&eacute; Bitteur
 */
public class BasicLine
    implements Line
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Line.class);

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
     * coeeficients don't have to be normalized, the constructor takes care of
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

    //------//
    // getA //
    //------//
    @Implement(Line.class)
    public double getA ()
    {
        checkLineParameters();

        return a;
    }

    //------//
    // getB //
    //------//
    @Implement(Line.class)
    public double getB ()
    {
        checkLineParameters();

        return b;
    }

    //------//
    // getC //
    //------//
    @Implement(Line.class)
    public double getC ()
    {
        checkLineParameters();

        return c;
    }

    //--------------//
    // isHorizontal //
    //--------------//
    @Implement(Line.class)
    public boolean isHorizontal ()
    {
        checkLineParameters();

        return a == 0d;
    }

    //------------------//
    // getInvertedSlope //
    //------------------//
    @Implement(Line.class)
    public double getInvertedSlope ()
    {
        checkLineParameters();

        return -b / a;
    }

    //-----------------//
    // getMeanDistance //
    //-----------------//
    @Implement(Line.class)
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
                (a * a * sx2) + (b * b * sy2) + (c * c * n) +
                (2 * a * b * sxy) + (2 * a * c * sx) + (2 * b * c * sy)) / n);
    }

    //-------------------//
    // getNumberOfPoints //
    //-------------------//
    @Implement(Line.class)
    public int getNumberOfPoints ()
    {
        return n;
    }

    //----------//
    // getSlope //
    //----------//
    @Implement(Line.class)
    public double getSlope ()
    {
        checkLineParameters();

        return -a / b;
    }

    //------------//
    // isVertical //
    //------------//
    @Implement(Line.class)
    public boolean isVertical ()
    {
        checkLineParameters();

        return b == 0d;
    }

    //------------//
    // distanceOf //
    //------------//
    @Implement(Line.class)
    public double distanceOf (double x,
                              double y)
    {
        checkLineParameters();

        return (a * x) + (b * y) + c;
    }

    //-------------//
    // includeLine //
    //-------------//
    @Implement(Line.class)
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
    @Implement(Line.class)
    public void includePoint (double x,
                              double y)
    {
        if (logger.isFineEnabled()) {
            logger.fine("includePoint x=" + x + " y=" + y);
        }

        n += 1;
        sx += x;
        sy += y;
        sx2 += (x * x);
        sy2 += (y * y);
        sxy += (x * y);

        dirty = true;
    }

    //-------//
    // reset //
    //-------//
    @Implement(Line.class)
    public void reset ()
    {
        a = b = c = Double.NaN;
        n = 0;
        sx = sy = sx2 = sy2 = sxy = 0d;

        dirty = false;
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

            StringBuffer sb = new StringBuffer();

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

    //-----//
    // xAt //
    //-----//
    @Implement(Line.class)
    public double xAt (double y)
    {
        checkLineParameters();

        if (a != 0d) {
            return -((b * y) + c) / a;
        } else {
            throw new RuntimeException("Line is horizontal");
        }
    }

    //-----//
    // xAt //
    //-----//
    @Implement(Line.class)
    public int xAt (int y)
    {
        return (int) rint(xAt((double) y));
    }

    //-----//
    // yAt //
    //-----//
    @Implement(Line.class)
    public double yAt (double x)
    {
        checkLineParameters();

        if (b != 0d) {
            return ((-a * x) - c) / b;
        } else {
            throw new RuntimeException("Line is vertical");
        }
    }

    //-----//
    // yAt //
    //-----//
    @Implement(Line.class)
    public int yAt (int x)
    {
        return (int) rint(yAt((double) x));
    }

    //---------------------//
    // checkLineParameters //
    //---------------------//
    /**
     * Make sure the line parameters are usable.
     */
    private final void checkLineParameters ()
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

        if (logger.isFineEnabled()) {
            logger.fine("hDen=" + hDen + " vDen=" + vDen);
        }

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
