//----------------------------------------------------------------------------//
//                                                                            //
//                         N a t u r a l S p l i n e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.Shape;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import static java.awt.geom.PathIterator.*;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;

/**
 * Class {@code NaturalSpline} defines a natural (cubic) spline
 * interpolated on a sequence of knots.
 *
 * <p>Internally the spline is composed of a sequence of curves, one
 * curve between two consecutive knots.
 * Each curve is a bezier curve defined by the 2 related knots separated by 2
 * control points.</p>
 *
 * <p>At each knot, continuity in ensured up to the second derivative.
 * The second derivative is set to zero at first and last knots of the whole
 * spline.</p>
 *
 * <p>Degenerated cases: When the sequence of knots contains only 3 or 2 points,
 * the spline degenerates to a quadratic or a straight line respectively.
 * If less than two points are provided, the spline cannot be created.</p>
 *
 * <p>Cf <a href="http://www.cse.unsw.edu.au/~lambert/splines/">
 * http://www.cse.unsw.edu.au/~lambert/splines/</a></p>
 *
 * @author Hervé Bitteur
 */
public class NaturalSpline
        extends GeoPath
        implements Line
{
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
     * Create the natural spline that interpolates the provided knots
     *
     * @param points the provided points
     * @return the resulting spline curve
     */
    public static NaturalSpline interpolate (Point2D... points)
    {
        // Check parameters
        if (points == null) {
            throw new IllegalArgumentException(
                    "NaturalSpline cannot interpolate null arrays");
        }

        double[] xx = new double[points.length];
        double[] yy = new double[points.length];

        for (int i = 0; i < points.length; i++) {
            Point2D pt = points[i];
            xx[i] = pt.getX();
            yy[i] = pt.getY();
        }

        return interpolate(xx, yy);
    }

    //-------------//
    // interpolate //
    //-------------//
    /**
     * Create the natural spline that interpolates the provided knots
     *
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
            Shape[] curves = new Shape[n];

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

    @Override
    public double distanceOf (double x,
                              double y)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getInvertedSlope ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getMeanDistance ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNumberOfPoints ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getSlope ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Line includeLine (Line other)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void includePoint (double x,
                              double y)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isHorizontal ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isVertical ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //    //------------//
    //    // renderLine //
    //    //------------//
    //    /**
    //     * Specific rendering of the curved line
    //     * @param g graphical context
    //     * @param r radius for control and defining points
    //     */
    //    public void renderLine (Graphics2D g,
    //                            double     r)
    //    {
    //        // Draw the curved line itself
    //        g.draw(this);
    //
    //        // Draw the control & defining points on top of it
    //        Color     oldColor = g.getColor();
    //        Ellipse2D ellipse = new Ellipse2D.Double();
    //        double[]  buffer = new double[6];
    //
    //        for (PathIterator it = getPathIterator(null); !it.isDone();
    //             it.next()) {
    //            int     segmentKind = it.currentSegment(buffer);
    //            int     coords = countOf(segmentKind);
    //            boolean control = false;
    //
    //            for (int ic = coords - 2; ic >= 0; ic -= 2) {
    //                ellipse.setFrame(
    //                    buffer[ic] - r,
    //                    buffer[ic + 1] - r,
    //                    2 * r,
    //                    2 * r);
    //                g.setColor(control ? Color.PINK : Color.BLUE);
    //                g.fill(ellipse);
    //
    //                control = true;
    //            }
    //        }
    //
    //        g.setColor(oldColor);
    //    }
    @Override
    public Line swappedCoordinates ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int xAtY (int y)
    {
        return (int) Math.rint(xAtY((double) y));
    }

    //----------------//
    // xDerivativeAtY //
    //----------------//
    /**
     * Report the abscissa derivative value of the spline at provided ordinate
     * (assuming true function)
     *
     * @param y the provided ordinate
     * @return the x derivative value at this ordinate
     */
    public double xDerivativeAtY (double y)
    {
        final double[] buffer = new double[6];
        final Point2D.Double p1 = new Point2D.Double();
        final Point2D.Double p2 = new Point2D.Double();
        final int segmentKind = getYSegment(y, buffer, p1, p2);
        final double deltaY = p2.y - p1.y;
        final double t = (y - p1.y) / deltaY;
        final double u = 1 - t;

        // dx/dy = dx/dt * dt/dy
        // dt/dy = 1/deltaY
        switch (segmentKind) {
        case SEG_LINETO:
            return (p2.x - p1.x) / deltaY;

        case SEG_QUADTO: {
            double cpx = buffer[0];

            return ((-2 * p1.x * u) + (2 * cpx * (1 - (2 * t)))
                    + (2 * p2.x * t)) / deltaY;
        }

        case SEG_CUBICTO: {
            double cpx1 = buffer[0];
            double cpx2 = buffer[2];

            return ((-3 * p1.x * u * u) + (3 * cpx1 * ((u * u) - (2 * u * t)))
                    + (3 * cpx2 * ((2 * t * u) - (t * t))) + (3 * p2.x * t * t)) / deltaY;
        }

        default:
            throw new RuntimeException("Illegal currentSegment " + segmentKind);
        }
    }

    @Override
    public int yAtX (int x)
    {
        return (int) Math.rint((double) x);
    }

    //----------------//
    // yDerivativeAtX //
    //----------------//
    /**
     * Report the ordinate derivative value of the spline at provided abscissa
     * (assuming true function)
     *
     * @param x the provided abscissa
     * @return the y derivative value at this abscissa
     */
    public double yDerivativeAtX (double x)
    {
        final double[] buffer = new double[6];
        final Point2D.Double p1 = new Point2D.Double();
        final Point2D.Double p2 = new Point2D.Double();
        final int segmentKind = getXSegment(x, buffer, p1, p2);
        final double deltaX = p2.x - p1.x;
        final double t = (x - p1.x) / deltaX;
        final double u = 1 - t;

        // dy/dx = dy/dt * dt/dx
        // dt/dx = 1/deltaX
        switch (segmentKind) {
        case SEG_LINETO:
            return (p2.y - p1.y) / deltaX;

        case SEG_QUADTO: {
            double cpy = buffer[1];

            return ((-2 * p1.y * u) + (2 * cpy * (1 - (2 * t)))
                    + (2 * p2.y * t)) / deltaX;
        }

        case SEG_CUBICTO: {
            double cpy1 = buffer[1];
            double cpy2 = buffer[3];

            return ((-3 * p1.y * u * u) + (3 * cpy1 * ((u * u) - (2 * u * t)))
                    + (3 * cpy2 * ((2 * t * u) - (t * t))) + (3 * p2.y * t * t)) / deltaX;
        }

        default:
            throw new RuntimeException("Illegal currentSegment " + segmentKind);
        }
    }

    //---------------------//
    // getCubicDerivatives //
    //---------------------//
    /**
     * Computes the derivatives of natural cubic spline that interpolates the
     * provided knots
     *
     * @param z the provided n knots
     * @return the corresponding array of derivative values
     */
    private static double[] getCubicDerivatives (double[] z)
    {
        // Number of segments
        final int n = z.length - 1;

        // Compute the derivative at each provided knot
        double[] D = new double[n + 1];

        /* Equation to solve:
         * [2 1 ] [D[0]] [3(z[1] - z[0]) ]
         * |1 4 1 | |D[1]| |3(z[2] - z[0]) |
         * | 1 4 1 | | . | = | . |
         * | ..... | | . | | . |
         * | 1 4 1| | . | |3(z[n] - z[n-2])|
         * [ 1 2] [D[n]] [3(z[n] - z[n-1])]
         * by using row operations to convert the matrix to upper triangular
         * and then back sustitution.
         */
        double[] gamma = new double[n + 1];
        gamma[0] = 1.0f / 2.0f;

        for (int i = 1; i < n; i++) {
            gamma[i] = 1 / (4 - gamma[i - 1]);
        }

        gamma[n] = 1 / (2 - gamma[n - 1]);

        double[] delta = new double[n + 1];
        delta[0] = 3 * (z[1] - z[0]) * gamma[0];

        for (int i = 1; i < n; i++) {
            delta[i] = ((3 * (z[i + 1] - z[i - 1])) - delta[i - 1]) * gamma[i];
        }

        delta[n] = ((3 * (z[n] - z[n - 1])) - delta[n - 1]) * gamma[n];

        D[n] = delta[n];

        for (int i = n - 1; i >= 0; i--) {
            D[i] = delta[i] - (gamma[i] * D[i + 1]);
        }

        return D;
    }
}
