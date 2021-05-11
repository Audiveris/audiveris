//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          G e o U t i l                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.math;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code GeoUtil} gathers simple utilities related to geometry.
 *
 * @author Hervé Bitteur
 */
public abstract class GeoUtil
{
    //~ Constructors -------------------------------------------------------------------------------

    // Not meant to be instantiated.
    private GeoUtil ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // center2D //
    //----------//
    /**
     * Report the center Point2D of the provided rectangle
     *
     * @param rect the provided rectangle
     * @return the geometric rectangle center
     */
    public static Point2D center2D (Rectangle2D rect)
    {
        return new Point2D.Double(rect.getX() + (rect.getWidth() / 2.0),
                                  rect.getY() + (rect.getHeight() / 2.0));
    }

    //-------------//
    // translate2D //
    //-------------//
    /**
     * Translate the Rectangle2D according to vector (dx, dy).
     *
     * @param rect rectangle to translate
     * @param dx   abscissa translation
     * @param dy   ordinate translation
     */
    public static void translate2D (Rectangle2D rect,
                                    double dx,
                                    double dy)
    {
        rect.setRect(rect.getX() + dx, rect.getY() + dy, rect.getWidth(), rect.getHeight());
    }

    //--------//
    // center //
    //--------//
    /**
     * Report the center Point of the provided rectangle
     *
     * @param rect the provided rectangle
     * @return the geometric rectangle center
     */
    public static Point center (Rectangle rect)
    {
        return new Point(rect.x + (rect.width / 2), rect.y + (rect.height / 2));
    }

    //--------------//
    // ptDistanceSq //
    //--------------//
    /**
     * Report the minimum square distance from a point to a rectangle.
     *
     * @param r provided rectangle
     * @param x abscissa of point
     * @param y ordinate of point
     * @return square of minimum distance
     */
    public static double ptDistanceSq (Rectangle r,
                                       double x,
                                       double y)
    {
        if (r.contains(x, y)) {
            return 0;
        }

        double d = Double.MAX_VALUE;

        final int x1 = r.x;
        final int x2 = (r.x + r.width) - 1;
        final int y1 = r.y;
        final int y2 = (r.y + r.height) - 1;

        d = Math.min(d, Line2D.ptSegDistSq(x1, y1, x2, y1, x, y));
        d = Math.min(d, Line2D.ptSegDistSq(x1, y2, x2, y2, x, y));
        d = Math.min(d, Line2D.ptSegDistSq(x1, y1, x1, y2, x, y));
        d = Math.min(d, Line2D.ptSegDistSq(x2, y1, x2, y2, x, y));

        return d;
    }

    //-------//
    // touch //
    //-------//
    /**
     * Report whether the two provided rectangles intersect or touch one another.
     *
     * @param r1 first provided rectangle
     * @param r2 the other provided rectangle
     * @return true if there is a horizontal or vertical connection (diagonals are not accepted)
     */
    public static boolean touch (Rectangle r1,
                                 Rectangle r2)
    {
        int x1 = Math.max(r1.x, r2.x);
        int x2 = Math.min(r1.x + r1.width, r2.x + r2.width);
        int xOver = x2 - x1;

        if (xOver < 0) {
            return false;
        }

        int y1 = Math.max(r1.y, r2.y);
        int y2 = Math.min(r1.y + r1.height, r2.y + r2.height);
        int yOver = y2 - y1;

        if (yOver < 0) {
            return false;
        }

        return (xOver > 0) || (yOver > 0);
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

    //-----------//
    // xEmbraces //
    //-----------//
    /**
     * Check whether the abscissae of the provided rectangle embrace
     * the provided abscissa value.
     *
     * @param rect the provided rectangle
     * @param x    the abscissa value
     * @return true if x is within rectangle abscissae
     */
    public static boolean xEmbraces (Rectangle rect,
                                     double x)
    {
        return (x >= rect.x) && (x < (rect.x + rect.width));
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
     * Check whether the ordinates of the provided line embrace
     * the provided ordinate value (assuming line points are defined
     * in increasing ordinate order).
     *
     * @param line the provided line
     * @param y    the ordinate value
     * @return true if x is within line ordinates
     */
    public static boolean yEmbraces (Line2D line,
                                     double y)
    {
        return (y >= line.getY1()) && (y <= line.getY2());
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
