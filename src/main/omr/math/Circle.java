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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Jama.Matrix;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import static java.lang.Math.*;
import java.util.ArrayList;

/**
 * Class {@code Circle} handles a circle (or a portion of circle)
 * which approximates a collection of data points.
 * Besides usual characteristics of a circle (center, radius), and of a circle
 * arc (start and stop angles) it also defines the approximating bezier curve.
 *
 * @author Hervé Bitteur
 */
public class Circle
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Circle.class);

    /** Size for matrices used to compute the circle */
    private static final int dim = 4;

    //~ Instance fields --------------------------------------------------------
    /** Mean algebraic distance between circle and the defining points */
    private final double distance;

    // Circle characteristics
    /** Center */
    private Point2D.Double center;

    /** Radius */
    private Double radius;

    /** Starting limit of circle arc */
    private Double startAngle;

    /** Stopping limit of circle arc */
    private Double stopAngle;

    /** Bezier curve for circle arc */
    private CubicCurve2D.Double curve;

    // Bounding coordinates
    private double xMax = Double.MIN_VALUE;

    private double yMax = Double.MIN_VALUE;

    private double xMin = Double.MAX_VALUE;

    private double yMin = Double.MAX_VALUE;

    //~ Constructors -----------------------------------------------------------
    //--------//
    // Circle //
    //--------//
    /**
     * Creates a new instance of Circle, defined by a set of points.
     *
     * @param x array of abscissae
     * @param y array of ordinates
     */
    public Circle (double[] x,
                   double[] y)
    {
        fit(x, y);
        computeAngles(x, y);
        distance = computeDistance(x, y);
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
     * @param x      array of abscissae (including the defining points)
     * @param y      array of ordinates (including the defining points)
     */
    public Circle (Point2D left,
                   Point2D middle,
                   Point2D right,
                   double[] x,
                   double[] y)
    {
        defineCircle(left, middle, right);
        computeAngles(x, y);
        distance = computeDistance(x, y);
    }

    //~ Methods ----------------------------------------------------------------
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
    public CubicCurve2D.Double getCurve ()
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{Circle");
        sb.append(String.format(" dist=%g", distance));
        sb.append(String.format(" center[%g,%g]", center.x, center.y));
        sb.append(String.format(" radius=%g", radius));

        if ((startAngle != null) && (stopAngle != null)) {
            sb.append(
                    String.format(
                    " angles=(%g,%g)",
                    toDegrees(startAngle),
                    toDegrees(stopAngle)));
        }

        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // computeAngles //
    //---------------//
    /**
     * Compute the start and stop angles of a circle.
     */
    private void computeAngles (double[] x,
                                double[] y)
    {
        // Get all angles, split into buckets
        final int BUCKET_NB = 8;
        final int[] buckets = new int[BUCKET_NB];

        for (int i = 0; i < BUCKET_NB; i++) {
            buckets[i] = 0;
        }

        final double bucketSize = (2 * PI) / BUCKET_NB;
        ArrayList<Double> angles = new ArrayList<>();

        for (int i = 0; i < x.length; i++) {
            // Get an angle between 0 and 2*PI
            double angle = PI + atan2(y[i] - center.y, x[i] - center.x);
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

    //--------------//
    // computeCurve //
    //--------------//
    /**
     * Compute the bezier points for the circle arc.
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

        ///System.out.println("angleDeg/2=" + Math.toDegrees(theta));
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

        //        System.out.println(" P1=" + curve.getP1());
        //        System.out.println("CP1=" + curve.getCtrlP1());
        //        System.out.println("CP2=" + curve.getCtrlP2());
        //        System.out.println(" P2=" + curve.getP2());
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
            double delta = Math.hypot(
                    x[i] - getCenter().x,
                    y[i] - getCenter().y) - getRadius();
            sum += (delta * delta);
        }

        return Math.sqrt(sum) / nbPoints;
    }

    //--------------//
    // defineCircle //
    //--------------//
    /**
     * Define the circle by means of a sequence of 3 key points.
     *
     * @param left   precise left point
     * @param middle a point rather in the middle
     * @param right  precise right point
     */
    private void defineCircle (Point2D left,
                               Point2D middle,
                               Point2D right)
    {
        Line2D prevBisector = LineUtil.bisector(
                new Line2D.Double(left, middle));
        Line2D bisector = LineUtil.bisector(
                new Line2D.Double(middle, right));
        center = LineUtil.intersection(
                prevBisector.getP1(),
                prevBisector.getP2(),
                bisector.getP1(),
                bisector.getP2());
        radius = Math.hypot(
                center.getX() - right.getX(),
                center.getY() - right.getY());
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

        //  Build the design matrix, and remember the bounding box
        Matrix design = new Matrix(nbPoints, dim);

        for (int i = 0; i < nbPoints; i++) {
            final double tx = x[i];
            final double ty = y[i];
            design.set(i, 0, (tx * tx) + (ty * ty));
            design.set(i, 1, tx);
            design.set(i, 2, ty);
            design.set(i, 3, 1);

            if (tx > xMax) {
                xMax = tx;
            }

            if (tx < xMin) {
                xMin = tx;
            }

            if (ty > yMax) {
                yMax = ty;
            }

            if (ty < yMin) {
                yMin = ty;
            }
        }

        ///print(design, "design");

        //  Build the scatter matrix
        Matrix scatter = design.transpose().times(design);

        ///print(scatter, "scatter");

        //  Let's impose A = 1
        //  So let's swap the first column with the Lambda.C' column
        Matrix first = new Matrix(dim, 1);

        for (int i = 0; i < dim; i++) {
            first.set(i, 0, -scatter.get(0, i));
        }

        Matrix newScatter = new Matrix(dim, dim);

        for (int i = 0; i < dim; i++) {
            for (int j = 1; j < dim; j++) {
                newScatter.set(i, j - 1, scatter.get(i, j));
            }

            newScatter.set(i, dim - 1, 0.0);
        }

        newScatter.set(0, dim - 1, -0.5);

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
        radius = Math.sqrt(((center.x * center.x) + (center.y * center.y)) - F);
    }
}
