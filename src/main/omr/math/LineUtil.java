//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         L i n e U t i l                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class {@code LineUtil} is a collection of utilities related to lines.
 *
 * @author Hervé Bitteur
 */
public abstract class LineUtil
{
    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // bisector //
    //----------//
    /**
     * Return the bisector (French: médiatrice) of the segment defined by the two
     * provided points
     *
     * @param p1 first provided point
     * @param p2 second provided point
     * @return (a segment on) the bisector (p3p4 is p1p2 turned 90° clockwise)
     */
    public static Line2D bisector (Point2D p1,
                                   Point2D p2)
    {
        double x1 = p1.getX();
        double y1 = p1.getY();

        double hdx = (p2.getX() - x1) / 2;
        double hdy = (p2.getY() - y1) / 2;

        // Use middle as reference point
        double mx = x1 + hdx;
        double my = y1 + hdy;

        double x3 = mx + hdy;
        double y3 = my - hdx;
        double x4 = mx - hdy;
        double y4 = my + hdx;

        return new Line2D.Double(x3, y3, x4, y4);
    }

    //----------//
    // bisector //
    //----------//
    /**
     * Return the bisector (French: médiatrice) of the provided segment
     *
     * @param segment the provided segment
     * @return (a segment on) the bisector
     */
    public static Line2D bisector (Line2D segment)
    {
        return bisector(segment.getP1(), segment.getP2());
    }

    //------------------//
    // getInvertedSlope //
    //------------------//
    /**
     * Report the inverted slope of provided line.
     * Line is expected not to be horizontal
     *
     * @param line the provided line
     * @return tangent of angle with vertical
     */
    public static double getInvertedSlope (Line2D line)
    {
        return getInvertedSlope(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    //------------------//
    // getInvertedSlope //
    //------------------//
    /**
     * Report the inverted slope of line defined by two provided points.
     * Line is expected not to be horizontal
     *
     * @param start line start
     * @param stop  line stop
     * @return tangent of angle with vertical
     */
    public static double getInvertedSlope (Point2D start,
                                           Point2D stop)
    {
        return getInvertedSlope(start.getX(), start.getY(), stop.getX(), stop.getY());
    }

    //------------------//
    // getInvertedSlope //
    //------------------//
    /**
     * Report the inverted slope of line defined by two provided points.
     * Line is expected not to be horizontal
     *
     * @param x1 start abscissa
     * @param y1 start ordinate
     * @param x2 stop abscissa
     * @param y2 stop ordinate
     * @return tangent of angle with vertical
     */
    public static double getInvertedSlope (double x1,
                                           double y1,
                                           double x2,
                                           double y2)
    {
        return (x2 - x1) / (y2 - y1);
    }

    //----------//
    // getSlope //
    //----------//
    /**
     * Report the slope of provided line.
     * Line is expected not to be vertical
     *
     * @param line the provided line
     * @return tangent of angle with horizontal
     */
    public static double getSlope (Line2D line)
    {
        return (line.getY2() - line.getY1()) / (line.getX2() - line.getX1());
    }

    //----------//
    // getSlope //
    //----------//
    /**
     * Report the slope of line defined by two provided points.
     * Line is expected not to be vertical
     *
     * @param start line start
     * @param stop  line stop
     * @return tangent of angle with horizontal
     */
    public static double getSlope (Point2D start,
                                   Point2D stop)
    {
        return (stop.getY() - start.getY()) / (stop.getX() - start.getX());
    }

    //--------------//
    // intersection //
    //--------------//
    /**
     * Return the intersection point between the two infinite lines provided.
     *
     * @param l1 first line
     * @param l2 second line
     * @return the intersection point
     */
    public static Point2D.Double intersection (Line2D l1,
                                               Line2D l2)
    {
        return intersection(
                l1.getX1(),
                l1.getY1(),
                l1.getX2(),
                l1.getY2(),
                l2.getX1(),
                l2.getY1(),
                l2.getX2(),
                l2.getY2());
    }

    //--------------//
    // intersection //
    //--------------//
    /**
     * Return the intersection point between infinite line A defined by points p1 & p2
     * and the infinite line B defined by points p3 & p4.
     *
     * @param p1 first point of line A
     * @param p2 second point of line A
     * @param p3 first point of line B
     * @param p4 second point of line B
     * @return the intersection point
     */
    public static Point2D.Double intersection (Point2D p1,
                                               Point2D p2,
                                               Point2D p3,
                                               Point2D p4)
    {
        return intersection(
                p1.getX(),
                p1.getY(),
                p2.getX(),
                p2.getY(),
                p3.getX(),
                p3.getY(),
                p4.getX(),
                p4.getY());
    }

    //--------------//
    // intersection //
    //--------------//
    /**
     * Return the intersection point between infinite line A defined by points (x1,y1) &
     * (x2,y2) and the infinite line B defined by points (x3,y3) & (x4,y4).
     *
     * @param x1 x of first point of line A
     * @param y1 y of first point of line A
     * @param x2 x of second point of line A
     * @param y2 y of second point of line A
     * @param x3 x of first point of line B
     * @param y3 y of first point of line B
     * @param x4 x of second point of line B
     * @param y4 y of second point of line B
     * @return the intersection point
     */
    public static Point2D.Double intersection (double x1,
                                               double y1,
                                               double x2,
                                               double y2,
                                               double x3,
                                               double y3,
                                               double x4,
                                               double y4)
    {
        double den = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));

        double v12 = (x1 * y2) - (y1 * x2);
        double v34 = (x3 * y4) - (y3 * x4);

        double x = ((v12 * (x3 - x4)) - ((x1 - x2) * v34)) / den;
        double y = ((v12 * (y3 - y4)) - ((y1 - y2) * v34)) / den;

        return new Point2D.Double(x, y);
    }

    //-----------------//
    // intersectionAtX //
    //-----------------//
    /**
     * Return the intersection point between infinite line A defined by points p1 & p2
     * and the infinite vertical line at provided abscissa.
     *
     * @param p1 first point of line A
     * @param p2 second point of line A
     * @param x  provided abscissa
     * @return the intersection point
     */
    public static Point2D.Double intersectionAtX (Point2D p1,
                                                  Point2D p2,
                                                  double x)
    {
        return intersection(p1.getX(), p1.getY(), p2.getX(), p2.getY(), x, 0, x, 1000);
    }

    //-----------------//
    // intersectionAtX //
    //-----------------//
    /**
     * Return the intersection point between provided infinite line
     * and the infinite vertical line at provided abscissa.
     *
     * @param line provided line
     * @param x    provided abscissa
     * @return the intersection point
     */
    public static Point2D.Double intersectionAtX (Line2D line,
                                                  double x)
    {
        return intersection(line.getX1(), line.getY1(), line.getX2(), line.getY2(), x, 0, x, 1000);
    }

    //-----------------//
    // intersectionAtX //
    //-----------------//
    /**
     * Return the intersection point between infinite line defined by provided point and
     * slope and the infinite vertical line at provided abscissa.
     *
     * @param p1    provided line point
     * @param slope provided line slope
     * @param x     provided abscissa
     * @return the intersection point
     */
    public static Point2D.Double intersectionAtX (Point2D p1,
                                                  double slope,
                                                  double x)
    {
        return intersection(
                p1.getX(),
                p1.getY(),
                p1.getX() + 1000,
                p1.getY() + (1000 * slope),
                x,
                0,
                x,
                1000);
    }

    //-----------------//
    // intersectionAtY //
    //-----------------//
    /**
     * Return the intersection point between infinite line A defined by
     * points p1 & p2 and infinite horizontal line at provided ordinate.
     *
     * @param p1 first point of line A
     * @param p2 second point of line A
     * @param y  provided ordinate
     * @return the intersection point
     */
    public static Point2D.Double intersectionAtY (Point2D p1,
                                                  Point2D p2,
                                                  double y)
    {
        return intersection(p1.getX(), p1.getY(), p2.getX(), p2.getY(), 0, y, 1000, y);
    }

    //-----------------//
    // intersectionAtY //
    //-----------------//
    /**
     * Return the intersection point between line defined by provided point and
     * <b>inverted</b> slope and the infinite horizontal line at provided ordinate.
     *
     * @param p1            point of line A
     * @param invertedSlope inverted slope of line A (slope WRT vertical)
     * @param y             provided ordinate
     * @return the intersection point
     */
    public static Point2D.Double intersectionAtY (Point2D p1,
                                                  double invertedSlope,
                                                  double y)
    {
        return intersection(
                p1.getX(),
                p1.getY(),
                p1.getX() + (1000 * invertedSlope),
                p1.getY() + 1000,
                0,
                y,
                1000,
                y);
    }

    //-----------------//
    // intersectionAtY //
    //-----------------//
    /**
     * Return the intersection point between provided infinite line and infinite
     * horizontal line at provided ordinate.
     *
     * @param line provided line
     * @param y    provided ordinate
     * @return the intersection point
     */
    public static Point2D.Double intersectionAtY (Line2D line,
                                                  double y)
    {
        return intersection(line.getX1(), line.getY1(), line.getX2(), line.getY2(), 0, y, 1000, y);
    }

    //----------//
    // rotation //
    //----------//
    /**
     * Computation of rotation from first to last point, with middle as approximate
     * middle point of the curve.
     *
     * @param first  starting point of curve
     * @param last   ending point of curve
     * @param middle middle point of curve
     * @return central rotation angle (in radians) from first to last.
     */
    public static double rotation (Point2D first,
                                   Point2D last,
                                   Point2D middle)
    {
        return rotation(new Line2D.Double(first, last), middle);
    }

    //----------//
    // rotation //
    //----------//
    /**
     * Computation of rotation from first to last point, with middle as approximate
     * middle point of the curve.
     *
     * @param line   straight line from curve start to curve stop
     * @param middle middle point of curve
     * @return central rotation angle (in radians) from curve start to curve stop.
     */
    public static double rotation (Line2D line,
                                   Point2D middle)
    {
        double dx = line.getX2() - line.getX1();
        double dy = line.getY2() - line.getY1();
        double halfChordLengthSq = ((dx * dx) + (dy * dy)) / 4;
        double sagittaSq = line.ptLineDistSq(middle);

        return 4 * Math.atan(Math.sqrt(sagittaSq / halfChordLengthSq));
    }

    //------//
    // xAtY //
    //------//
    /**
     * Return the abscissa of intersection between infinite line A defined by
     * points p1 & p2 and infinite horizontal line at provided ordinate.
     *
     * @param p1 first point of line A
     * @param p2 second point of line A
     * @param y  provided ordinate
     * @return the intersection abscissa
     */
    public static double xAtY (Point2D p1,
                               Point2D p2,
                               double y)
    {
        return intersection(p1.getX(), p1.getY(), p2.getX(), p2.getY(), 0, y, 1000, y).x;
    }

    //------//
    // xAtY //
    //------//
    /**
     * Return the abscissa of intersection between provided infinite line and infinite
     * horizontal line at provided ordinate.
     *
     * @param line provided line
     * @param y    provided ordinate
     * @return the intersection abscissa
     */
    public static double xAtY (Line2D line,
                               double y)
    {
        return intersection(line.getX1(), line.getY1(), line.getX2(), line.getY2(), 0, y, 1000, y).x;
    }

    //------//
    // yAtX //
    //------//
    /**
     * Return the ordinate of intersection between infinite line defined by provided
     * point and slope and the infinite vertical line at provided abscissa.
     *
     * @param p1    provided line point
     * @param slope provided line slope
     * @param x     provided abscissa
     * @return the intersection ordinate
     */
    public static double yAtX (Point2D p1,
                               double slope,
                               double x)
    {
        return intersection(
                p1.getX(),
                p1.getY(),
                p1.getX() + 1000,
                p1.getY() + (1000 * slope),
                x,
                0,
                x,
                1000).y;
    }

    //------//
    // yAtX //
    //------//
    /**
     * Return the ordinate of intersection between provided infinite line
     * and the infinite vertical line at provided abscissa.
     *
     * @param line provided line
     * @param x    provided abscissa
     * @return the intersection ordinate
     */
    public static double yAtX (Line2D line,
                               double x)
    {
        return intersection(line.getX1(), line.getY1(), line.getX2(), line.getY2(), x, 0, x, 1000).y;
    }

    //------//
    // yAtX //
    //------//
    /**
     * Return the ordinate of intersection between infinite line A defined by points
     * p1 & p2 and the infinite vertical line at provided abscissa.
     *
     * @param p1 first point of line A
     * @param p2 second point of line A
     * @param x  provided abscissa
     * @return the intersection ordinate
     */
    public static double yAtX (Point2D p1,
                               Point2D p2,
                               double x)
    {
        return intersection(p1.getX(), p1.getY(), p2.getX(), p2.getY(), x, 0, x, 1000).y;
    }
}
