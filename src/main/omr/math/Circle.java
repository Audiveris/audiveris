//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          C i r c l e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
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
 * <p>
 * Besides usual characteristics of a circle (center, radius), and of a circle arc (start and stop
 * angles defined in -PI..+PI), it also defines the rotation from first to last as counter-clockwise
 * (ccw) or not. First and last angles can be swapped via reverse() method.
 * <p>
 * The approximating Bézier curve is always defined from left to right, so that left point is P1 and
 * right point is P2.
 * <p>
 * If arc shape is /--\ it is said "above".
 * If arc shape is \--/ it is said "below".
 * <p>
 *
 * @author Hervé Bitteur
 */
public class Circle
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Circle.class);

    /** Size for matrices used to compute the circle. */
    private static final int DIM = 4;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Center. */
    private Point2D.Double center;

    /** Radius. */
    private Double radius;

    /** Mean algebraic distance between circle and defining points. */
    private double distance;

    /** Starting angle of circle arc. */
    private Double firstAngle;

    /** Stopping angle of circle arc. */
    private Double lastAngle;

    /** Turning counter-clockwise when going from first to last?. */
    private int ccw;

    /** Above or below. */
    private boolean above;

    /** Bézier curve for circle arc. */
    private CubicCurve2D curve;

    //~ Constructors -------------------------------------------------------------------------------
    //--------//
    // Circle //
    //--------//
    /**
     * Clone a circle
     *
     * @param that the original circle
     */
    public Circle (Circle that)
    {
        if (that.center != null) {
            center = new Point2D.Double(that.center.getX(), that.center.getY());
        }

        radius = that.radius;
        distance = that.distance;
        firstAngle = that.firstAngle;
        lastAngle = that.lastAngle;
        ccw = that.ccw;
        above = that.above;
        curve = that.curve;
    }

    //--------//
    // Circle //
    //--------//
    /**
     * Creates a new instance of Circle, defined by a set of points.
     *
     * @param xx array of abscissae
     * @param yy array of ordinates
     */
    @Deprecated
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
    @Deprecated
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

    //~ Methods ------------------------------------------------------------------------------------
    //-----//
    // ccw //
    //-----//
    /**
     * Report +1 for an arc turning counter-clockwise, -1 clockwise.
     *
     * @return +1 for CCW, -1 for not CCW
     */
    public int ccw ()
    {
        return ccw;
    }

    //-----------------//
    // computeDistance //
    //-----------------//
    /**
     * Compute the mean quadratic distance of all provided points to the circle.
     *
     * @param points the provided points
     * @return the mean quadratic distance
     */
    public double computeDistance (Collection<? extends Point2D> points)
    {
        final int nbPoints = points.size();
        double sum = 0;

        for (Point2D point : points) {
            double delta = Math.hypot(point.getX() - center.x, point.getY() - center.y) - radius;
            sum += (delta * delta);
        }

        return distance = sqrt(sum / nbPoints);
    }

    //----------//
    // getAngle //
    //----------//
    /**
     * Report the angle at desired end of the circle arc.
     *
     * @return the angle, in radians within -PI..PI
     */
    public Double getAngle (boolean reverse)
    {
        if (reverse) {
            return firstAngle;
        } else {
            return lastAngle;
        }
    }

    //-------------//
    // getArcAngle //
    //-------------//
    /**
     * Report the positive length of arc from first to last.
     *
     * @return the positive arc, in radians within 0..2*PI
     */
    public double getArcAngle ()
    {
        double arc = (ccw == 1) ? (firstAngle - lastAngle) : (lastAngle - firstAngle);

        if (arc < 0) {
            arc += (2 * PI);
        }

        return arc;
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
     * Report the left-to-right Bézier curve which best approximates
     * the circle arc.
     *
     * @return the Bézier curve
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

    //---------------//
    // getFirstAngle //
    //---------------//
    /**
     * Report the angle at start of the circle arc.
     *
     * @return the starting angle, in radians within -PI..PI
     */
    public Double getFirstAngle ()
    {
        return firstAngle;
    }

    //--------------//
    // getLastAngle //
    //--------------//
    /**
     * Report the angle at stop of the circle arc.
     *
     * @return the stopping angle, in radians within -PI..PI
     */
    public Double getLastAngle ()
    {
        return lastAngle;
    }

    //-------------//
    // getMidAngle //
    //-------------//
    /**
     * Report the angle at middle of arc
     *
     * @return the mid angle, in radians within -PI..PI
     */
    public double getMidAngle ()
    {
        double halfArc = getArcAngle() / 2;
        double mid = (ccw == 1) ? (firstAngle - halfArc) : (firstAngle + halfArc);

        if (mid < -PI) {
            mid += (2 * PI);
        }

        if (mid > PI) {
            mid -= (2 * PI);
        }

        return mid;
    }

    //----------------//
    // getMiddlePoint //
    //----------------//
    /**
     * Report the half-way point on Bézier curve.
     *
     * @return the middle of the curve arc
     */
    public Point2D getMiddlePoint ()
    {
        CubicCurve2D c = getCurve();

        return new Point2D.Double(
                (c.getX1() + (3 * c.getCtrlX1()) + (3 * c.getCtrlX2()) + c.getX2()) / 8,
                (c.getY1() + (3 * c.getCtrlY1()) + (3 * c.getCtrlY2()) + c.getY2()) / 8);
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

    /**
     * @return the above
     */
    public boolean isAbove ()
    {
        return above;
    }

    //---------//
    // reverse //
    //---------//
    /**
     * Swap first and last angles (and CCW accordingly).
     */
    public void reverse ()
    {
        Double temp = firstAngle;
        firstAngle = lastAngle;
        lastAngle = temp;

        ccw = -ccw;
    }

    //-------------//
    // getDistance //
    //-------------//
    /**
     * Record the mean distance (useful for 2-point definitions)
     *
     * @param distance the computed mean distance
     */
    public void setDistance (double distance)
    {
        this.distance = distance;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{Circle");
        sb.append(" ccw=").append(ccw);
        sb.append(String.format(" dist=%.4f", distance));
        sb.append(String.format(" center[%.1f,%.1f]", center.x, center.y));
        sb.append(String.format(" radius=%.1f", radius));

        if ((firstAngle != null) && (lastAngle != null)) {
            sb.append(
                    String.format(" degrees=(%.0f,%.0f)", toDegrees(firstAngle), toDegrees(lastAngle)));
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

            firstAngle = start;
            lastAngle = stop;

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
        // Angles are in -PI .. +PI
        firstAngle = angleOf(first.getX(), first.getY());
        lastAngle = angleOf(last.getX(), last.getY());
        ccw = new Line2D.Double(first, middle).relativeCCW(last);
        above = (first.getX() < last.getX()) ? (ccw == -1) : (ccw == 1);
    }

    //--------------//
    // computeCurve //
    //--------------//
    /**
     * Compute the bézier points for the circle arc.
     * The Bézier curves always goes from left to right.
     */
    private void computeCurve ()
    {
        // Make sure we do have an arc defined, rather than a full circle
        if (((lastAngle == null) || (lastAngle.isNaN()))
            || ((firstAngle == null) || (firstAngle.isNaN()))) {
            return;
        }

        // Bezier points for circle arc, centered at origin, with radius 1
        double arc = getArcAngle();
        double x0 = cos(arc / 2);
        double y0 = sin(arc / 2);
        double x1 = (4 - x0) / 3;
        double y1 = ((1 - x0) * (3 - x0)) / (3 * y0);
        double x2 = x1;
        double y2 = -y1;
        double x3 = x0;
        double y3 = -y0;

        // Rotation
        final double theta = getMidAngle();

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
        final Matrix op = translation.times(scaling).times(rotation);

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
        radius = hypot(center.getX() - middle.getX(), center.getY() - middle.getY());
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
        Matrix scatter = design.transpose().times(design);

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
