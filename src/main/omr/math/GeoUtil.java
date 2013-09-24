//----------------------------------------------------------------------------//
//                                                                            //
//                                G e o U t i l                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;

/**
 * Class {@code GeoUtil} gathers simple utilities related to geometry.
 *
 * @author Hervé Bitteur
 */
public class GeoUtil
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report whether the two provided rectangles are identical
     *
     * @param one first provided rectangle
     * @param two the other provided rectangle
     * @return true if identical
     */
    public static boolean areIdentical (Rectangle one,
                                        Rectangle two)
    {
        return (one.x == two.x) && (one.y == two.y)
               && (one.width == two.width) && (one.height == two.height);
    }

    //----------//
    // centerOf //
    //----------//
    /**
     * Report the center of the provided rectangle
     *
     * @param rect the provided rectangle
     * @return the geometric rectangle center
     */
    public static Point centerOf (Rectangle rect)
    {
        return new Point(rect.x + (rect.width / 2), rect.y + (rect.height / 2));
    }

    //----------//
    // vectorOf //
    //----------//
    /**
     * Report the vector that goes from 'from' point to 'to' point.
     *
     * @param from the origin point
     * @param to   the target point
     * @return the vector from origin to target
     */
    public static Point vectorOf (Point from,
                                  Point to)
    {
        return new Point(to.x - from.x, to.y - from.y);
    }

    //-----------//
    // xEmbraces //
    //-----------//
    /**
     * Check whether the abscissae of the provided line embrace
     * the provided abscissa value (assuming line points are defined
     * in increasing abscissa order).
     *
     * @param line the provided line
     * @param x    the abscissa value
     * @return true if x is within line abscissae
     */
    public static boolean xEmbraces (Line2D line,
                                     double x)
    {
        return (x >= line.getX1()) && (x <= line.getX2());
    }

    //------//
    // xGap //
    //------//
    /**
     * Report the abscissa gap between the provided rectangles
     *
     * @param one first provided rectangle
     * @param two the other provided rectangle
     * @return a negative value if the rectangles overlap horizontally or a
     *         positive value if there is a true horizontal gap
     */
    public static int xGap (Rectangle one,
                            Rectangle two)
    {
        return -xOverlap(one, two);
    }

    //----------//
    // xOverlap //
    //----------//
    /**
     * Report the abscissa overlap between the provided rectangles
     *
     * @param one first provided rectangle
     * @param two the other provided rectangle
     * @return a positive value if the rectangles overlap horizontally or a
     *         negative value if there is a true horizontal gap
     */
    public static int xOverlap (Rectangle one,
                                Rectangle two)
    {
        final int commonLeft = Math.max(one.x, two.x);
        final int commonRight = Math.min(one.x + one.width, two.x + two.width);

        return commonRight - commonLeft;
    }

    //-----------//
    // yEmbraces //
    //-----------//
    /**
     * Check whether the ordinates of the provided rectangle embrace
     * the provided ordinate value.
     *
     * @param rect the provided rectangle
     * @param y    the ordinate value
     * @return true if y is within rectangle ordinates
     */
    public static boolean yEmbraces (Rectangle rect,
                                     double y)
    {
        return (y >= rect.y) && (y < (rect.y + rect.height));
    }

    //------//
    // yGap //
    //------//
    /**
     * Report the ordinate gap between the provided rectangles
     *
     * @param one first provided rectangle
     * @param two the other provided rectangle
     * @return a negative value if the rectangles overlap vertically or a
     *         positive value if there is a true vertical gap
     */
    public static int yGap (Rectangle one,
                            Rectangle two)
    {
        return -yOverlap(one, two);
    }

    //----------//
    // yOverlap //
    //----------//
    /**
     * Report the ordinate overlap between the provided rectangles
     *
     * @param one first provided rectangle
     * @param two the other provided rectangle
     * @return a positive value if the rectangles overlap vertically or a
     *         negative value if there is a true vertical gap
     */
    public static int yOverlap (Rectangle one,
                                Rectangle two)
    {
        final int commonTop = Math.max(one.y, two.y);
        final int commonBot = Math.min(one.y + one.height, two.y + two.height);

        return commonBot - commonTop;
    }
}
