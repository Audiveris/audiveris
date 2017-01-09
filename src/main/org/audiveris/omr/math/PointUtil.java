//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        P o i n t U t i l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Comparator;

/**
 * Class {@code PointUtil} gathers utility methods for points (and vectors).
 *
 * @author Hervé Bitteur
 */
public abstract class PointUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** To compare points on abscissa. */
    public static final Comparator<Point> byAbscissa = new Comparator<Point>()
    {
        @Override
        public int compare (Point p1,
                            Point p2)
        {
            return Integer.compare(p1.x, p2.x);
        }
    };

    /** To compare points on ordinate. */
    public static final Comparator<Point> byOrdinate = new Comparator<Point>()
    {
        @Override
        public int compare (Point p1,
                            Point p2)
        {
            return Integer.compare(p1.y, p2.y);
        }
    };

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addition //
    //----------//
    /**
     * Report the vector which is the addition of the 2 vectors.
     *
     * @param p1 a vector
     * @param p2 another vector
     * @return the geometric addition
     */
    public static Point2D addition (Point2D p1,
                                    Point2D p2)
    {
        return new Point2D.Double(p1.getX() + p2.getX(), p1.getY() + p2.getY());
    }

    //----------//
    // boundsOf //
    //----------//
    /**
     * Report the bounding box for all points
     *
     * @param points the provided points
     * @return the smallest rectangle that really contains all the points.
     */
    public static Rectangle boundsOf (Collection<? extends Point> points)
    {
        if ((points == null) || points.isEmpty()) {
            return null;
        }

        Rectangle bounds = null;

        for (Point point : points) {
            if (bounds == null) {
                bounds = new Rectangle(point);
            } else {
                bounds.add(point);
            }
        }

        if (bounds != null) {
            // So that points located on right or bottom borders get really contained by the bounds
            bounds.width++;
            bounds.height++;
        }

        return bounds;
    }

    //------------//
    // dotProduct //
    //------------//
    /**
     * Report the dot product between two vectors.
     *
     * @param p1 a vector
     * @param p2 another vector
     * @return |p1|.|p2|.cos(theta)
     */
    public static double dotProduct (Point2D p1,
                                     Point2D p2)
    {
        return (p1.getX() * p2.getX()) + (p1.getY() * p2.getY());
    }

    //-----------//
    // extension //
    //-----------//
    /**
     * Compute extension point, on line p1 -> p2, at 'dist' distance beyond p2.
     *
     * @param p1   first point
     * @param p2   second point
     * @param dist distance beyond p2
     * @return the extension point
     */
    public static Point2D extension (Point2D p1,
                                     Point2D p2,
                                     double dist)
    {
        double seg = p1.distance(p2);

        return new Point2D.Double(
                p2.getX() + ((dist * (p2.getX() - p1.getX())) / seg),
                p2.getY() + ((dist * (p2.getY() - p1.getY())) / seg));
    }

    //--------//
    // length //
    //--------//
    /**
     * Report the length of a vector
     *
     * @param p a vector
     * @return the vector module
     */
    public static double length (Point2D p)
    {
        return Math.hypot(p.getX(), p.getY());
    }

    //---------//
    // rounded //
    //---------//
    /**
     * Report a point with integer coordinates
     *
     * @param p provided Point2D instance
     * @return Point instance
     */
    public static Point rounded (Point2D p)
    {
        return new Point((int) Math.rint(p.getX()), (int) Math.rint(p.getY()));
    }

    //-------------//
    // subtraction //
    //-------------//
    /**
     * Report the vector which represent p2 - p1.
     *
     * @param p1 a vector
     * @param p2 another vector
     * @return the geometric subtraction
     */
    public static Point2D subtraction (Point2D p1,
                                       Point2D p2)
    {
        return new Point2D.Double(p1.getX() - p2.getX(), p1.getY() - p2.getY());
    }

    //-------//
    // times //
    //-------//
    /**
     * Report the multiplication of a vector by the provided coefficient
     *
     * @param p     a vector
     * @param coeff coefficient applied to vector components
     * @return the resulting vector
     */
    public static Point2D times (Point2D p,
                                 double coeff)
    {
        return new Point2D.Double(p.getX() * coeff, p.getY() * coeff);
    }

    //----------//
    // toString //
    //----------//
    public static String toString (Point p)
    {
        StringBuilder sb = new StringBuilder("[");
        sb.append(p.x).append(",").append(p.y).append("]");

        return sb.toString();
    }
}
