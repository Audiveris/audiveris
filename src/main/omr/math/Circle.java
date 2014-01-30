//----------------------------------------------------------------------------//
//                                                                            //
//                                C i r c l e                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import Jama.Matrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code Circle} handles a portion of circle which approximates
 * a collection of points.
 * Besides usual characteristics of a circle (center, radius), and of a circle
 * arc (start and stop angles), it also defines the approximating Bézier curve.
 * <p>
 * Start and stop angles are defined clock-wise, with value of stop angle
 * always greater than or equal to start angle.
 * <p>
 * Bézier curve is defined from left to right, so that left point is P1 and
 * right point is P2.
 * <p>
 * If arc shape is /--\, then startAngle relates to P1 and stopAngle to P2.
 * If arc shape is \--/, then startAngle relates to P2 and stopAngle to P1.
 *
 * @author Hervé Bitteur
 */
public class Circle
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Circle.class);

    /** Size for matrices used to compute the circle. */
    private static final int DIM = 4;

    //~ Instance fields --------------------------------------------------------
    /** Center. */
    private Point2D.Double center;

    /** Radius. */
    private Double radius;

    /** Mean algebraic distance between circle and defining points. */
    private double distance;

    /** Starting angle of circle arc. (stopAngle >= startAngle) */
    private Double startAngle;

    /** Stopping angle of circle arc. (stopAngle >= startAngle) */
    private Double stopAngle;

    /** Flag set when ending points were swapped. */
    private boolean isSwapped;

    /** Bézier curve for circle arc. */
    private CubicCurve2D curve;

    //~ Constructors -----------------------------------------------------------
    //--------//
    // Circle //
    //--------//
    /**
     * Creates a new instance of Circle, defined by a set of points.
     *
     * @param xx array of abscissae
     * @param yy array of ordinates
     */
    public Circle (double[] xx,
                   double[] yy)
    {
        fit(xx, yy);
        computeAngles(xx, yy);
        distance = computeDistance(xx, yy);
    }

    //--------//
    // Circle //
    //--------//
    /**
     * Creates a new instance of Circle, defined by a set of points.
     *
     * @param points the collection of points
     */
    public Circle (List<? extends Point2D> points)
    {
        double[] xx = new double[points.size()];
        double[] yy = new double[points.size()];

        for (int i = 0; i < yy.length; i++) {
            Point2D p = points.get(i);
            xx[i] = p.getX();
            yy[i] = p.getY();
        }

        fit(xx, yy);
        distance = computeDistance(xx, yy);

        Point2D first = points.get(0);
        Point2D middle = points.get(points.size() / 2);
        Point2D last = points.get(points.size() - 1);

        computeAngles(first, middle, last);
    }

    //--------//
    // Circle //
    //--------//
    /**
     * Creates a new instance of Circle, fitted to 3 defining points.
     * The provided collection of coordinates is used only to compute the
     * resulting distance.
     *
     * @param left   left defining point
     * @param middle middle defining point
     * @param right  right defining point
     * @param xx     array of abscissae (including the defining points)
     * @param yy     array of ordinates (including the defining points)
     */
    public Circle (Point2D left,
                   Point2D middle,
                   Point2D right,
                   double[] xx,
                   double[] yy)
    {
        defineCircle(left, middle, right);
        computeAngles(xx, yy);
        distance = computeDistance(xx, yy);
    }

    //--------//
    // Circle //
    //--------//
    /**
     * Creates a new instance of Circle, fitted to 3 defining points.
     *
     * @param first  first defining point
     * @param middle middle defining point
     * @param last   last defining point
     */
    public Circle (Point2D first,
                   Point2D middle,
                   Point2D last)
    {
        defineCircle(first, middle, last);
        computeAngles(first, middle, last);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // computeDistance //
    //-----------------//
    /**
     * Compute the mean quadratic distance of all points to the circle.
     *
     * @param points the collection of all defining points
     * @return the mean quadratic distance
     */
    public double computeDistance (Collection<? extends Point2D> points)
    {
        final int nbPoints = points.size();
        double sum = 0;

        for (Point2D point : points) {
            double delta = Math.hypot(
                    point.getX() - center.x,
                    point.getY() - center.y) - radius;
            sum += (delta * delta);
        }

        return distance = sqrt(sum) / nbPoints;
    }

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the circle center.
     *
     * @return the center of the circle
     */
    public Point2D.Double getCenter ()
    {
        return center;
    }

    //----------//
    // getCurve //
    //----------//
    /**
     * Report the Bezier curve which best approximates the circle arc.
     *
     * @return the Bezier curve
     */
    public CubicCurve2D getCurve ()
    {
        if (curve == null) {
            computeCurve();
        }

        return curve;
    }

    //-------------//
    // getDistance //
    //-------------//
    /**
     * Report the mean distance between the data points and the circle.
     *
     * @return the mean distance
     */
    public double getDistance ()
    {
        return distance;
    }

    //-----------//
    // getRadius //
    //-----------//
    /**
     * Report the circle radius.
     *
     * @return the circle radius
     */
    public Double getRadius ()
    {
        return radius;
    }

    //---------------//
    // getStartAngle //
    //---------------//
    /**
     * Report the angle at start of the circle arc.
     *
     * @return the starting angle, in radians
     */
    public Double getStartAngle ()
    {
        return startAngle;
    }

    //--------------//
    // getStopAngle //
    //--------------//
    /**
     * Report the angle at stop of the circle arc.
     *
     * @return the stopping angle, in radians
     */
    public Double getStopAngle ()
    {
        return stopAngle;
    }

    //-----------//
    // isSwapped //
    //-----------//
    /**
     * Tell whether the ending points had to be swapped.
     *
     * @return true if swapped
     */
    public boolean isSwapped ()
    {
        return isSwapped;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{Circle");
        sb.append(String.format(" dist=%.4f", distance));
        sb.append(String.format(" center[%.1f,%.1f]", center.x, center.y));
        sb.append(String.format(" radius=%.1f", radius));

        if ((startAngle != null) && (stopAngle != null)) {
            sb.append(
                    String.format(
                            " degrees=(%.0f,%.0f)",
                            toDegrees(startAngle),
                            toDegrees(stopAngle)));
        }

        sb.append("}");

        return sb.toString();
    }

    //---------//
    // angleOf //
    //---------//
    private double angleOf (double x,
                            double y)
    {
        return atan2(y - center.y, x - center.x);
    }

    //---------------//
    // computeAngles //
    //---------------//
    /**
     * Compute the start and stop angles of a circle, not knowing
     * where the circle arc begins and stops.
     */
    private void computeAngles (double[] xx,
                                double[] yy)
    {
        // Get all angles, split into buckets
        final int BUCKET_NB = 8;
        final int[] buckets = new int[BUCKET_NB];

        for (int i = 0; i < BUCKET_NB; i++) {
            buckets[i] = 0;
        }

        final double bucketSize = (2 * PI) / BUCKET_NB;
        ArrayList<Double> angles = new ArrayList<Double>();

        for (int i = 0; i < xx.length; i++) {
            // Get an angle between 0 and 2*PI
            double angle = PI + atan2(yy[i] - center.y, xx[i] - center.x);
            angles.add(angle);

            int idx = (int) (angle / bucketSize);

            if ((idx >= 0) && (idx < BUCKET_NB)) {
                buckets[idx] += 1;
            }
        }

        // Find an empty bucket
        int emptyIdx;

        for (emptyIdx = 0; emptyIdx < BUCKET_NB; emptyIdx++) {
            if (buckets[emptyIdx] == 0) {
                break;
            }
        }

        if (emptyIdx >= BUCKET_NB) {
            logger.debug("No empty sector in circle, this is not a slur");
        } else {
            final double bottom = emptyIdx * bucketSize;
            double start = 2 * PI;
            double stop = 0;

            for (double angle : angles) {
                angle -= bottom;

                if (angle < 0) {
                    angle += (2 * PI);
                }

                if (angle < start) {
                    start = angle;
                }

                if (angle > stop) {
                    stop = angle;
                }
            }

            stop += (bottom - PI);
            start += (bottom - PI);

            if (stop < start) {
                stop += (2 * PI);
            }

            startAngle = start;
            stopAngle = stop;

            //            System.out.println(
            //                "emptyIdx=" + emptyIdx + " startDeg=" +
            //                (float) toDegrees(start) + " stopDeg=" +
            //                (float) toDegrees(stop));
        }
    }

    //---------------//
    // computeAngles //
    //---------------//
    private void computeAngles (Point2D first,
                                Point2D middle,
                                Point2D last)
    {
        // Angles are in -PI .. + PI
        double fa = angleOf(first.getX(), first.getY());
        double ma = angleOf(middle.getX(), middle.getY());
        double la = angleOf(last.getX(), last.getY());

        if (la < fa) {
            la += (2 * PI);
        }

        if (ma < fa) {
            ma += (2 * PI);
        }

        if (ma > la) {
            // Swap first and last
            startAngle = la;
            stopAngle = fa;
            isSwapped = true;
        } else {
            startAngle = fa;
            stopAngle = la;
        }

        if (startAngle > PI) {
            startAngle -= (2 * PI);
        }

        if (stopAngle > PI) {
            stopAngle -= (2 * PI);
        }

        if (stopAngle < startAngle) {
            stopAngle += (2 * PI);
        }
    }

    //--------------//
    // computeCurve //
    //--------------//
    /**
     * Compute the bézier points for the circle arc.
     */
    private void computeCurve ()
    {
        // Make sure we do have an arc defined, rather than a full circle
        if (((stopAngle == null) || (stopAngle.isNaN()))
            || ((startAngle == null) || (startAngle.isNaN()))) {
            return;
        }

        // Bezier points for circle arc, centered at origin, with radius 1
        double arc = stopAngle - startAngle;
        double x0 = cos(arc / 2);
        double y0 = sin(arc / 2);
        double x1 = (4 - x0) / 3;
        double y1 = ((1 - x0) * (3 - x0)) / (3 * y0);
        double x2 = x1;
        double y2 = -y1;
        double x3 = x0;
        double y3 = -y0;

        // Rotation
        final double theta = (startAngle + stopAngle) / 2;

        ///System.out.println("angleDeg/2=" + toDegrees(theta));
        final Matrix rotation = new Matrix(
                new double[][]{
                    {cos(theta), -sin(theta), 0},
                    {sin(theta), cos(theta), 0},
                    {0, 0, 1}
                });

        // Scaling
        final Matrix scaling = new Matrix(
                new double[][]{
                    {radius, 0, 0},
                    {0, radius, 0},
                    {0, 0, 1}
                });

        // Translation
        final Matrix translation = new Matrix(
                new double[][]{
                    {1, 0, center.x},
                    {0, 1, center.y},
                    {0, 0, 1}
                });

        // Composite operation
        final Matrix op = translation.times(scaling)
                .times(rotation);

        final Matrix M0 = op.times(
                new Matrix(
                        new double[][]{
                            {x0},
                            {y0},
                            {1}
                        }));

        final Matrix M1 = op.times(
                new Matrix(
                        new double[][]{
                            {x1},
                            {y1},
                            {1}
                        }));

        final Matrix M2 = op.times(
                new Matrix(
                        new double[][]{
                            {x2},
                            {y2},
                            {1}
                        }));

        final Matrix M3 = op.times(
                new Matrix(
                        new double[][]{
                            {x3},
                            {y3},
                            {1}
                        }));

        // Bezier curve (make sure the curve goes from left to right)
        if (M0.get(0, 0) <= M3.get(0, 0)) {
            curve = new CubicCurve2D.Double(
                    M0.get(0, 0),
                    M0.get(1, 0),
                    M1.get(0, 0),
                    M1.get(1, 0),
                    M2.get(0, 0),
                    M2.get(1, 0),
                    M3.get(0, 0),
                    M3.get(1, 0));
        } else {
            curve = new CubicCurve2D.Double(
                    M3.get(0, 0),
                    M3.get(1, 0),
                    M2.get(0, 0),
                    M2.get(1, 0),
                    M1.get(0, 0),
                    M1.get(1, 0),
                    M0.get(0, 0),
                    M0.get(1, 0));
        }
    }

    //-----------------//
    // computeDistance //
    //-----------------//
    /**
     * Compute the mean quadratic distance of all points to the circle.
     *
     * @param x array of abscissae
     * @param y array of ordinates
     * @return the mean quadratic distance
     */
    private double computeDistance (double[] x,
                                    double[] y)
    {
        final int nbPoints = x.length;
        double sum = 0;

        for (int i = 0; i < nbPoints; i++) {
            double delta = hypot(x[i] - center.x, y[i] - center.y) - radius;
            sum += (delta * delta);
        }

        return sqrt(sum) / nbPoints;
    }

    //--------------//
    // defineCircle //
    //--------------//
    /**
     * Define the circle by means of a sequence of 3 key points.
     *
     * @param first  precise first point
     * @param middle a point rather in the middle
     * @param last   precise last point
     */
    private void defineCircle (Point2D first,
                               Point2D middle,
                               Point2D last)
    {
        Line2D bisector1 = LineUtil.bisector(first, middle);
        Line2D bisector2 = LineUtil.bisector(middle, last);
        center = LineUtil.intersection(bisector1, bisector2);
        radius = hypot(
                center.getX() - middle.getX(),
                center.getY() - middle.getY());
    }

    //-----//
    // fit //
    //-----//
    /**
     * Given a collection of points, determine the best approximating
     * circle.
     * The result is available in the center and radius variables.
     *
     * @param x the array of abscissae
     * @param y the array of ordinates
     */
    private void fit (double[] x,
                      double[] y)
    {
        //  Target is to minimize sum(||a(x2+y2) +dx +ey +f||) w/ constraint a=1
        //  That is DV, w/ D=Design matrix, and V= [a d e f]
        //  a = 1 can be written CV = 1, w/ C = [1 0 0 0]
        //  Function to minimize is V'D'DV - lambda*(CV -1)
        //  w/ lambda as the Lagrange multiplier
        //  At the extremum, the gradient of this function is nul, so:
        //  2D'DV -lambda.C' = 0 and a=1

        /** number of points */
        int nbPoints = x.length;

        if (nbPoints < 3) {
            throw new IllegalArgumentException("Less than 3 defining points");
        }

        //  Build the design matrix
        Matrix design = new Matrix(nbPoints, DIM);

        for (int i = 0; i < nbPoints; i++) {
            final double tx = x[i];
            final double ty = y[i];
            design.set(i, 0, (tx * tx) + (ty * ty));
            design.set(i, 1, tx);
            design.set(i, 2, ty);
            design.set(i, 3, 1);
        }

        ///print(design, "design");
        //  Build the scatter matrix
        Matrix scatter = design.transpose()
                .times(design);

        ///print(scatter, "scatter");
        //  Let's impose A = 1
        //  So let's swap the first column with the Lambda.C' column
        Matrix first = new Matrix(DIM, 1);

        for (int i = 0; i < DIM; i++) {
            first.set(i, 0, -scatter.get(0, i));
        }

        Matrix newScatter = new Matrix(DIM, DIM);

        for (int i = 0; i < DIM; i++) {
            for (int j = 1; j < DIM; j++) {
                newScatter.set(i, j - 1, scatter.get(i, j));
            }

            newScatter.set(i, DIM - 1, 0.0);
        }

        newScatter.set(0, DIM - 1, -0.5);

        ///print(newScatter, "newScatter");
        //  Solution [D E F lambda]
        Matrix newScatterInv = newScatter.inverse();

        ///print(newScatterInv, "newScatterInv");
        Matrix Solution = newScatterInv.times(first);

        ///print(Solution, "Solution [D E F Lambda]");
        // Coefficients of the algebraic equation
        // x**2 + y**2 + D*x + E*y + F = 0
        double D = Solution.get(0, 0);
        double E = Solution.get(1, 0);
        double F = Solution.get(2, 0);

        // Compute center & radius
        center = new Point2D.Double(-D / 2, -E / 2);
        radius = sqrt(((center.x * center.x) + (center.y * center.y)) - F);
    }
}
