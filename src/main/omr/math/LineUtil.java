//----------------------------------------------------------------------------//
//                                                                            //
//                               L i n e U t i l                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class {@code LineUtil} is a collection of utilities related to
 * lines.
 *
 * @author Hervé Bitteur
 */
public class LineUtil
{
    //~ Constructors -----------------------------------------------------------

    /** Not meant to be instantiated. */
    private LineUtil ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // bisector //
    //----------//
    /**
     * Return the bisector (french: médiatrice) of the segment defined
     * by the two provided points
     *
     * @param p1 first provided point
     * @param p2 second provided point
     * @return (a segment on) the bisector
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
     * Return the bisector (french: médiatrice) of the provided segment
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
        return (line.getX2() - line.getX1()) / (line.getY2() - line.getY1());
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
        return (stop.getX() - start.getX()) / (stop.getY() - start.getY());
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
     * Return the intersection point between the two infinite lines
     * provided.
     *
     * @param l1 first line
     * @param l2 second line
     * @return the intersection point
     */
    public static Point2D.Double intersection (Line2D l1,
                                               Line2D l2)
    {
        return intersection(l1.getP1(), l1.getP2(), l2.getP1(), l2.getP2());
    }

    //--------------//
    // intersection //
    //--------------//
    /**
     * Return the intersection point between infinite line A defined by
     * points p1 & p2 and infinite line B defined by points p3 & p4.
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
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();
        double x3 = p3.getX();
        double y3 = p3.getY();
        double x4 = p4.getX();
        double y4 = p4.getY();

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
     * Return the intersection point between infinite line A defined by
     * points p1 & p2 and infinite vertical line at provided abscissa.
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
        Point2D p3 = new Point2D.Double(x, 0);
        Point2D p4 = new Point2D.Double(x, 1000);

        return intersection(p1, p2, p3, p4);
    }

    //-----------------//
    // intersectionAtX //
    //-----------------//
    /**
     * Return the intersection point between provided infinite line
     * and infinite vertical line at provided abscissa.
     *
     * @param line provided line
     * @param x    provided abscissa
     * @return the intersection point
     */
    public static Point2D.Double intersectionAtX (Line2D line,
                                                  double x)
    {
        Point2D p3 = new Point2D.Double(x, 0);
        Point2D p4 = new Point2D.Double(x, 1000);

        return intersection(line.getP1(), line.getP2(), p3, p4);
    }

    //-----------------//
    // intersectionAtX //
    //-----------------//
    /**
     * Return the intersection point between infinite line defined by
     * provided point and slope and infinite vertical line at
     * provided abscissa.
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
        Point2D p2 = new Point2D.Double(
                p1.getX() + 1000,
                p1.getY() + (1000 * slope));
        Point2D p3 = new Point2D.Double(x, 0);
        Point2D p4 = new Point2D.Double(x, 1000);

        return intersection(p1, p2, p3, p4);
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
        Point2D p3 = new Point2D.Double(0, y);
        Point2D p4 = new Point2D.Double(1000, y);

        return intersection(p1, p2, p3, p4);
    }

    //-----------------//
    // intersectionAtY //
    //-----------------//
    /**
     * Return the intersection point between line defined by provided
     * point and <b>inverted</> slope and infinite horizontal line at
     * provided ordinate.
     *
     * @param p1            point of line A
     * @param invertedSlope inverted slope of line A (slope WRT vertical line)
     * @param y             provided ordinate
     * @return the intersection point
     */
    public static Point2D.Double intersectionAtY (Point2D p1,
                                                  double invertedSlope,
                                                  double y)
    {
        Point2D p2 = new Point2D.Double(
                p1.getX() + (1000 * invertedSlope),
                p1.getY() + 1000);
        Point2D p3 = new Point2D.Double(0, y);
        Point2D p4 = new Point2D.Double(1000, y);

        return intersection(p1, p2, p3, p4);
    }

    //-----------------//
    // intersectionAtY //
    //-----------------//
    /**
     * Return the intersection point between provided infinite line
     * and infinite horizontal line at provided ordinate.
     *
     * @param line provided line
     * @param y    provided ordinate
     * @return the intersection point
     */
    public static Point2D.Double intersectionAtY (Line2D line,
                                                  double y)
    {
        Point2D p3 = new Point2D.Double(0, y);
        Point2D p4 = new Point2D.Double(1000, y);

        return intersection(line.getP1(), line.getP2(), p3, p4);
    }
}
