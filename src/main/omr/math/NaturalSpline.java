//----------------------------------------------------------------------------//
//                                                                            //
//                         N a t u r a l S p l i n e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.*;

/**
 * Class {@code NaturalSpline} defines a natural (cubic) spline interpolated
 * on a sequence of knots.
 * Internally the spline is composed of a sequence of curves, one
 * curve between two consecutive knots.
 * Each curve is a bezier curve defined by the 2 related knots separated by 2
 * control points (or just a quadratic or straight line).
 * <p>Cf http://www.cse.unsw.edu.au/~lambert/splines/
 *
 * @author Hervé Bitteur
 */
public class NaturalSpline
    extends Path2D.Double
{
    //~ Static fields/initializers ---------------------------------------------

    /** Stolen from Path2D internals (sorry...)
       0: SEG_MOVETO;
       1: SEG_LINETO;
       2: SEG_QUADTO;
       3: SEG_CUBICTO;
       4: SEG_CLOSE;
     */
    private static final int[] curvecoords = { 2, 2, 4, 6, 0 };
    private static final String[] curveLabels = {
                                                    "SEG_MOVETO", "SEG_LINETO",
                                                    "SEG_QUADTO", "SEG_CUBICTO",
                                                    "SEG_CLOSE"
                                                };

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // NaturalSpline //
    //---------------//
    /**
     * Creates a new NaturalSpline object from a sequence of connected shapes
     *
     * @param curves the smooth sequence of shapes (cubic curves expected)
     */
    private NaturalSpline (Shape... curves)
    {
        for (Shape shape : curves) {
            append(shape, true);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // interpolate //
    //-------------//
    /**
     * Computes the natural cubic spline that interpolates the provided knots
     * @param points the provided points
     * @return the resulting spline curve
     */
    public static NaturalSpline interpolate (Point... points)
    {
        // Check parameters
        if (points == null) {
            throw new IllegalArgumentException(
                "NaturalSpline cannot interpolate null arrays");
        }

        double[] xx = new double[points.length];
        double[] yy = new double[points.length];

        for (int i = 0; i < points.length; i++) {
            Point pt = points[i];
            xx[i] = pt.x + 0.5;
            yy[i] = pt.y + 0.5;
        }

        return interpolate(xx, yy);
    }

    //-------------//
    // interpolate //
    //-------------//
    /**
     * Computes the natural cubic spline that interpolates the provided knots
     * @param xx the abscissae of the provided points
     * @param yy the ordinates of the provided points
     * @return the resulting spline curve
     */
    public static NaturalSpline interpolate (double[] xx,
                                             double[] yy)
    {
        // Check parameters
        if ((xx == null) || (yy == null)) {
            throw new IllegalArgumentException(
                "NaturalSpline cannot interpolate null arrays");
        }

        if (xx.length != yy.length) {
            throw new IllegalArgumentException(
                "NaturalSpline interpolation needs consistent coordinates");
        }

        // Number of segments
        final int n = xx.length - 1;

        if (n < 1) {
            throw new IllegalArgumentException(
                "NaturalSpline interpolation needs at least 2 points");
        }

        if (n == 1) {
            // Use a Line
            return new NaturalSpline(
                new Line2D.Double(xx[0], yy[0], xx[1], yy[1]));
        } else if (n == 2) {
            // Use a Quadratic (TODO: check this formula...)
            //            double t = (xx[1] - xx[0]) / (xx[2] - xx[0]);
            //            double u = 1 - t;
            //            double cpx = (xx[1] - (u * u * xx[0]) - (t * t * xx[2])) / 2 * t * u;
            //            double cpy = (yy[1] - (u * u * yy[0]) - (t * t * yy[2])) / 2 * t * u;
            return new NaturalSpline(
                new QuadCurve2D.Double(
                    xx[0],
                    yy[0],
                    (2 * xx[1]) - ((xx[0] + xx[2]) / 2),
                    (2 * yy[1]) - ((yy[0] + yy[2]) / 2),
                    xx[2],
                    yy[2]));
        } else {
            // Use a sequence of cubics
            double[] dx = getCubicDerivatives(xx);
            double[] dy = getCubicDerivatives(yy);
            Shape[]  curves = new Shape[n];

            for (int i = 0; i < n; i++) {
                // Build each segment curve
                curves[i] = new CubicCurve2D.Double(
                    xx[i],
                    yy[i],
                    xx[i] + (dx[i] / 3),
                    yy[i] + (dy[i] / 3),
                    xx[i + 1] - (dx[i + 1] / 3),
                    yy[i + 1] - (dy[i + 1] / 3),
                    xx[i + 1],
                    yy[i + 1]);
            }

            return new NaturalSpline(curves);
        }
    }

    //--------------//
    // derivativeAt //
    //--------------//
    public double derivativeAt (double x)
    {
        double[]       buffer = new double[6];
        Point2D.Double p1 = new Point2D.Double();
        Point2D.Double p2 = new Point2D.Double();
        final int      currentSegment = getSegment(x, buffer, p1, p2);

        double         y1 = p1.y;
        double         x1 = p1.x;
        double         y2 = p2.y;
        double         x2 = p2.x;

        // Compute y value
        double deltaX = x2 - x1;
        double t = (x - x1) / deltaX;
        double u = 1 - t;

        // dy/dx = dy/dt * dt/dx
        // dt/dx = 1/(x2-x1)  
        switch (currentSegment) {
        case PathIterator.SEG_LINETO :

            //return y1 + (t * (y2 - y1));
            return (y2 - y1) / deltaX;

        case PathIterator.SEG_QUADTO : {
            double cpy = buffer[1];

            //return (y1 * u * u) + (2 * cpy * t * u) + (y2 * t * t);
            return ((-2 * y1 * u) + (2 * cpy * (1 - (2 * t))) + (2 * y2 * t)) / deltaX;
        }

        case PathIterator.SEG_CUBICTO : {
            double cpy1 = buffer[1];
            double cpy2 = buffer[3];

            //return (y1 * u * u * u) + (3 * cpy1 * t * u * u) + (3 * cpy2 * t * t * u) + (y2 * t * t * t);
            return ((-3 * y1 * u * u) + (3 * cpy1 * ((u * u) - (2 * u * t))) +
                   (3 * cpy2 * ((2 * t * u) - (t * t))) + (3 * y2 * t * t)) / deltaX;
        }

        default :
        }

        throw new RuntimeException("Illegal currentSegment " + currentSegment);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        double[] buffer = new double[6];

        for (PathIterator it = getPathIterator(null); !it.isDone();
             it.next()) {
            int currentSegment = it.currentSegment(buffer);

            sb.append(" ")
              .append(curveLabel(currentSegment))
              .append("(");

            int     coords = coordCount(currentSegment);

            boolean firstCoord = true;

            for (int ic = 0; ic < (coords - 1); ic += 2) {
                if (!firstCoord) {
                    sb.append(",");
                    firstCoord = false;
                }

                sb.append("[")
                  .append((float) buffer[ic])
                  .append(",")
                  .append((float) buffer[ic + 1])
                  .append("]");
            }

            sb.append(")");
        }

        sb.append("}");

        return sb.toString();
    }

    //    //-----//
    //    // yAt //
    //    //-----//
    //    /**
    //     * Report the ordinate of the spline at abscissa x (assuming true function)
    //     * @param x the given abscissa
    //     * @return the corresponding ordinate
    //     */
    //    public double yAt (double x)
    //    {
    //        double[]     buffer = new double[6];
    //        PathIterator it = getPathIterator(null);
    //        double       x1 = 0;
    //        double       y1 = 0;
    //
    //        while (!it.isDone()) {
    //            final int    currentSegment = it.currentSegment(buffer);
    //            final int    coords = coordCount(currentSegment);
    //            final double x2 = buffer[coords - 2];
    //            final double y2 = buffer[coords - 1];
    //
    //            if ((currentSegment == PathIterator.SEG_MOVETO) ||
    //                (currentSegment == PathIterator.SEG_CLOSE) ||
    //                (x > x2)) {
    //                // Move to next segment
    //                x1 = x2;
    //                y1 = y2;
    //                it.next();
    //
    //                continue;
    //            }
    //
    //            // Compute y value
    //            double t = (x - x1) / (x2 - x1);
    //            double u = 1 - t;
    //
    //            switch (currentSegment) {
    //            case PathIterator.SEG_LINETO : //
    //                return y1 + (t * (y2 - y1));
    //
    //            case PathIterator.SEG_QUADTO : {
    //                double cpy = buffer[1];
    //
    //                return (y1 * u * u) + (2 * cpy * t * u) + (y2 * t * t);
    //            }
    //
    //            case PathIterator.SEG_CUBICTO : {
    //                double cpy1 = buffer[1];
    //                double cpy2 = buffer[3];
    //
    //                return (y1 * u * u * u) + (3 * cpy1 * t * u * u) +
    //                       (3 * cpy2 * t * t * u) + (y2 * t * t * t);
    //            }
    //
    //            default :
    //            }
    //        }
    //
    //        // Not found
    //        throw new RuntimeException("Abscissa not in spline range: " + x);
    //    }

    //-----//
    // yAt //
    //-----//
    /**
     * Report the ordinate of the spline at abscissa x (assuming true function)
     * @param x the given abscissa
     * @return the corresponding ordinate
     */
    public double yAt (double x)
    {
        double[]       buffer = new double[6];
        Point2D.Double p1 = new Point2D.Double();
        Point2D.Double p2 = new Point2D.Double();
        final int      currentSegment = getSegment(x, buffer, p1, p2);

        double         y1 = p1.y;
        double         x1 = p1.x;
        double         y2 = p2.y;
        double         x2 = p2.x;

        // Compute y value
        double t = (x - x1) / (x2 - x1);
        double u = 1 - t;

        switch (currentSegment) {
        case PathIterator.SEG_LINETO : //
            return y1 + (t * (y2 - y1));

        case PathIterator.SEG_QUADTO : {
            double cpy = buffer[1];

            return (y1 * u * u) + (2 * cpy * t * u) + (y2 * t * t);
        }

        case PathIterator.SEG_CUBICTO : {
            double cpy1 = buffer[1];
            double cpy2 = buffer[3];

            return (y1 * u * u * u) + (3 * cpy1 * t * u * u) +
                   (3 * cpy2 * t * t * u) + (y2 * t * t * t);
        }

        default :
        }

        throw new RuntimeException("Illegal currentSegment " + currentSegment);
    }

    //------------//
    // coordCount //
    //------------//
    /**
     * Dirty hack to know how many coordinate values a path segment contains
     * @param curveKind the int-based segment kind
     * @return the number of coordinates values
     */
    static int coordCount (int curveKind)
    {
        return curvecoords[curveKind];
    }

    //------------//
    // curveLabel //
    //------------//
    /**
     * Dirty hack to report the kind of a curve
     * @param curveKind the int-based segment kind
     * @return the label for the curve
     */
    static String curveLabel (int curveKind)
    {
        return curveLabels[curveKind];
    }

    //---------------------//
    // getCubicDerivatives //
    //---------------------//
    /**
     * Computes the derivatives of natural cubic spline that interpolates the
     * provided knots
     * @param x the provided n knots
     * @return the corresponding array of derivative values
     */
    private static double[] getCubicDerivatives (double[] x)
    {
        // Number of segments
        final int n = x.length - 1;

        // First compute the derivative at each provided knot
        double[] D = new double[n + 1];

        /* Equation to solve:
           [2 1       ] [D[0]]   [3(x[1] - x[0])  ]
           |1 4 1     | |D[1]|   |3(x[2] - x[0])  |
           |  1 4 1   | | .  | = |      .         |
           |    ..... | | .  |   |      .         |
           |     1 4 1| | .  |   |3(x[n] - x[n-2])|
           [       1 2] [D[n]]   [3(x[n] - x[n-1])]
           by using row operations to convert the matrix to upper triangular
           and then back sustitution.
         */
        double[] gamma = new double[n + 1];
        gamma[0] = 1.0f / 2.0f;

        for (int i = 1; i < n; i++) {
            gamma[i] = 1 / (4 - gamma[i - 1]);
        }

        gamma[n] = 1 / (2 - gamma[n - 1]);

        double[] delta = new double[n + 1];
        delta[0] = 3 * (x[1] - x[0]) * gamma[0];

        for (int i = 1; i < n; i++) {
            delta[i] = ((3 * (x[i + 1] - x[i - 1])) - delta[i - 1]) * gamma[i];
        }

        delta[n] = ((3 * (x[n] - x[n - 1])) - delta[n - 1]) * gamma[n];

        D[n] = delta[n];

        for (int i = n - 1; i >= 0; i--) {
            D[i] = delta[i] - (gamma[i] * D[i + 1]);
        }

        return D;
    }

    //------------//
    // getSegment //
    //------------//
    /**
     * Retrieve the segment of the curve that contains the provided abscissa
     * @param x the provided abscissa
     * @param buffer output
     * @param p1 output: start of segment
     * @param p2 output: end of segment
     * @return the segment type
     */
    private int getSegment (double         x,
                            double[]       buffer,
                            Point2D.Double p1,
                            Point2D.Double p2)
    {
        PathIterator it = getPathIterator(null);
        double       x1 = 0;
        double       y1 = 0;

        while (!it.isDone()) {
            final int    currentSegment = it.currentSegment(buffer);
            final int    coords = coordCount(currentSegment);
            final double x2 = buffer[coords - 2];
            final double y2 = buffer[coords - 1];

            if ((currentSegment == PathIterator.SEG_MOVETO) ||
                (currentSegment == PathIterator.SEG_CLOSE) ||
                (x > x2)) {
                // Move to next segment
                x1 = x2;
                y1 = y2;
                it.next();

                continue;
            }

            p1.x = x1;
            p1.y = y1;
            p2.x = x2;
            p2.y = y2;

            return currentSegment;
        }

        // Not found
        throw new RuntimeException("Abscissa not in spline range: " + x);
    }
}
