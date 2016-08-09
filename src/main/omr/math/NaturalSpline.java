//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   N a t u r a l S p l i n e                                    //
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

import static omr.math.GeoPath.countOf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.Collection;
import java.util.Objects;

/**
 * Class {@code NaturalSpline} defines a natural (cubic) spline interpolated on a
 * sequence of knots.
 * <p>
 * Internally the spline is composed of a sequence of curves, one curve between two consecutive
 * knots. Each curve is a bezier curve defined by the 2 related knots separated by 2 control
 * points.</p>
 * <p>
 * At each knot, continuity in ensured up to the second derivative.
 * The second derivative is set to zero at first and last knots of the whole spline.</p>
 * <p>
 * Degenerated cases: When the sequence of knots contains only 3 or 2 points, the spline degenerates
 * to a quadratic or a straight line respectively. If less than two points are provided, the spline
 * cannot be created.</p>
 * <p>
 * Cf <a href="http://www.cse.unsw.edu.au/~lambert/splines/">
 * http://www.cse.unsw.edu.au/~lambert/splines/</a></p>
 *
 * @author Hervé Bitteur
 */
public class NaturalSpline
        extends GeoPath
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(NaturalSpline.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private Point2D first; // Cached for faster access. Really useful???

    private Point2D last; // Cached for faster access. Really useful???

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new NaturalSpline object from a sequence of connected shapes.
     *
     * @param curves the smooth sequence of shapes (cubic curves expected)
     */
    private NaturalSpline (Shape... curves)
    {
        for (Shape shape : curves) {
            append(shape, true);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // interpolate //
    //-------------//
    /**
     * Create the natural spline that interpolates the provided knots
     *
     * @param points the provided points
     * @return the resulting spline curve
     */
    public static NaturalSpline interpolate (Collection<? extends Point2D> points)
    {
        Objects.requireNonNull(points, "NaturalSpline cannot interpolate null arrays");

        double[] xx = new double[points.size()];
        double[] yy = new double[points.size()];

        int i = -1;

        for (Point2D pt : points) {
            xx[++i] = pt.getX();
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
        Objects.requireNonNull(xx, "NaturalSpline cannot interpolate null arrays");
        Objects.requireNonNull(yy, "NaturalSpline cannot interpolate null arrays");

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
            return new NaturalSpline(new Line2D.Double(xx[0], yy[0], xx[1], yy[1]));
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

    //---------------//
    // getFirstPoint //
    //---------------//
    @Override
    public Point2D getFirstPoint ()
    {
        if (first == null) {
            first = super.getFirstPoint();
        }

        return new Point2D.Double(first.getX(), first.getY());
    }

    //--------------//
    // getLastPoint //
    //--------------//
    @Override
    public Point2D getLastPoint ()
    {
        if (last == null) {
            last = super.getLastPoint();
        }

        return new Point2D.Double(last.getX(), last.getY());
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint the spline on the provided environment, perhaps with its defining points.
     *
     * @param g          the graphics context
     * @param showPoints true to show the defining points
     * @param pointWidth width for any displayed defining point
     */
    public void render (Graphics2D g,
                        boolean showPoints,
                        double pointWidth)
    {
        final Rectangle clip = g.getClipBounds();

        if (clip != null) {
            final Rectangle bounds = getBounds();
            bounds.grow(1, 1); // Since interior of a perfect vertical or horizontal line is void!

            if (!clip.intersects(bounds)) {
                return;
            }
        }

        // The spline itself
        g.draw(this);

        // Then the defining points?
        if (showPoints) {
            Color oldColor = g.getColor();
            g.setColor(Color.RED);

            final double r = pointWidth / 2; // Point radius
            final Ellipse2D ellipse = new Ellipse2D.Double();
            final double[] coords = new double[6];
            final PathIterator it = getPathIterator(null);

            while (!it.isDone()) {
                final int segmentKind = it.currentSegment(coords);
                final int count = countOf(segmentKind);
                final double x = coords[count - 2];
                final double y = coords[count - 1];
                ellipse.setFrame(x - r, y - r, 2 * r, 2 * r);
                g.fill(ellipse);
                it.next();
            }

            g.setColor(oldColor);
        }
    }

    //------//
    // xAtY //
    //------//
    public int xAtY (int y)
    {
        return (int) Math.rint(xAtY((double) y));
    }

    //----------------//
    // xDerivativeAtY //
    //----------------//
    /**
     * Report the abscissa derivative value of the spline at provided ordinate
     * (assuming true function).
     *
     * @param y the provided ordinate
     * @return the x derivative value at this ordinate
     */
    public double xDerivativeAtY (double y)
    {
        final double[] coords = new double[6];
        final Point2D.Double p1 = new Point2D.Double();
        final Point2D.Double p2 = new Point2D.Double();
        final int segmentKind = getYSegment(y, coords, p1, p2);
        final double deltaY = p2.y - p1.y;
        final double t = (y - p1.y) / deltaY;
        final double u = 1 - t;

        // dx/dy = dx/dt * dt/dy
        // dt/dy = 1/deltaY
        switch (segmentKind) {
        case SEG_LINETO:
            return (p2.x - p1.x) / deltaY;

        case SEG_QUADTO: {
            double cpx = coords[0];

            return ((-2 * p1.x * u) + (2 * cpx * (1 - (2 * t))) + (2 * p2.x * t)) / deltaY;
        }

        case SEG_CUBICTO: {
            double cpx1 = coords[0];
            double cpx2 = coords[2];

            return ((-3 * p1.x * u * u) + (3 * cpx1 * ((u * u) - (2 * u * t)))
                    + (3 * cpx2 * ((2 * t * u) - (t * t))) + (3 * p2.x * t * t)) / deltaY;
        }

        default:
            throw new RuntimeException("Illegal currentSegment " + segmentKind);
        }
    }

    //------//
    // yAtX //
    //------//
    public int yAtX (int x)
    {
        return (int) Math.rint(yAtX((double) x));
    }

    //----------------//
    // yDerivativeAtX //
    //----------------//
    /**
     * Report the ordinate derivative value of the spline at provided abscissa
     * (assuming true function).
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

            return ((-2 * p1.y * u) + (2 * cpy * (1 - (2 * t))) + (2 * p2.y * t)) / deltaX;
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
     * Computes the derivatives of natural cubic spline that interpolates the provided
     * knots.
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

        /**
         * <pre>
         * Equation to solve.
         *       [2 1       ] [D[0]]   [3(z[1] - z[0])  ]
         *       |1 4 1     | |D[1]|   |3(z[2] - z[0])  |
         *       |  1 4 1   | | .  | = |      .         |
         *       |    ..... | | .  |   |      .         |
         *       |     1 4 1| | .  |   |3(z[n] - z[n-2])|
         *       [       1 2] [D[n]]   [3(z[n] - z[n-1])]
         * </pre>
         * by using row operations to convert the matrix to upper triangular
         * and then back substitution.
         */
        double[] gamma = new double[n + 1];
        gamma[0] = 0.5;

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
