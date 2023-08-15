//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        P o i n t U t i l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * Class <code>PointUtil</code> gathers utility methods for points (and vectors).
 *
 * @author Hervé Bitteur
 */
public abstract class PointUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** To compare points on abscissa. */
    public static final Comparator<Point> byAbscissa = (p1,
                                                        p2) -> Integer.compare(p1.x, p2.x);

    /** To compare points on ordinate. */
    public static final Comparator<Point> byOrdinate = (p1,
                                                        p2) -> Integer.compare(p1.y, p2.y);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Not meant to be instantiated.
     */
    private PointUtil ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    /**
     * Add to point p the provided (dx, dy) vector.
     *
     * @param p  point to be modified
     * @param dx abscissa to be added
     * @param dy ordinate to be added
     */
    public static void add (Point2D p,
                            double dx,
                            double dy)
    {
        p.setLocation(p.getX() + dx, p.getY() + dy);
    }

    //-----//
    // add //
    //-----//
    /**
     * Add to point p the provided vector.
     *
     * @param p      point to be modified
     * @param vector vector to be added
     */
    public static void add (Point2D p,
                            Point2D vector)
    {
        add(p, vector.getX(), vector.getY());
    }

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
    public static Point addition (Point p1,
                                  Point p2)
    {
        return new Point(p1.x + p2.x, p1.y + p2.y);
    }

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
     * Compute extension point, on line p1 &rarr; p2, at 'dist' distance beyond p2.
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

    //--------//
    // middle //
    //--------//
    /**
     * Report the middle point of a Line2D.
     *
     * @param line provided line2D
     * @return the middle point between p1 and p2
     */
    public static Point2D middle (Line2D line)
    {
        return new Point2D.Double(
                (line.getX1() + line.getX2()) / 2.0,
                (line.getY1() + line.getY2()) / 2.0);
    }

    //--------//
    // middle //
    //--------//
    /**
     * Report the middle point between the two provided points.
     *
     * @param p1 a point
     * @param p2 another point
     * @return the middle point between p1 and p2
     */
    public static Point2D middle (Point2D p1,
                                  Point2D p2)
    {
        return new Point2D.Double((p1.getX() + p2.getX()) / 2.0, (p1.getY() + p2.getY()) / 2.0);
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
        if (p == null) {
            return null;
        }

        return new Point((int) Math.rint(p.getX()), (int) Math.rint(p.getY()));
    }

    //-------------//
    // subtraction //
    //-------------//
    /**
     * Report the vector from p2 to p1 (that is p1 - p2).
     *
     * @param p1 a vector
     * @param p2 another vector
     * @return the geometric subtraction
     */
    public static Point subtraction (Point p1,
                                     Point p2)
    {
        Objects.requireNonNull(p1, "PointUtil.subtraction. p1 must be non-null");
        Objects.requireNonNull(p2, "PointUtil.subtraction. p2 must be non-null");

        return new Point(p1.x - p2.x, p1.y - p2.y);
    }

    //-------------//
    // subtraction //
    //-------------//
    /**
     * Report the vector from p2 to p1 (that is p1 - p2).
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
    /**
     * A toString() that can cope with null instance.
     *
     * @param p Point instance
     * @return string value
     */
    public static String toString (Point p)
    {
        if (p == null) {
            return "nullPoint";
        }

        return String.format("[%d,%df]", p.x, p.y);
    }

    //----------//
    // toString //
    //----------//
    /**
     * A toString() that can cope with null instance.
     *
     * @param p Point2D instance
     * @return string value
     */
    public static String toString (Point2D p)
    {
        if (p == null) {
            return "nullPoint";
        }

        return String.format("[%.1f,%.1f]", p.getX(), p.getY());
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Translate the provided Point along the (dx, dy) vector.
     *
     * @param point the point to translate
     * @param dx    translation in abscissa
     * @param dy    translation in ordinate
     */
    public static void translate (Point point,
                                  int dx,
                                  int dy)
    {
        point.setLocation(point.x + dx, point.y + dy);
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Translate the provided Point2D along the (dx, dy) vector.
     *
     * @param point the point to translate
     * @param dx    translation in abscissa
     * @param dy    translation in ordinate
     */
    public static void translate (Point2D point,
                                  double dx,
                                  double dy)
    {
        point.setLocation(point.getX() + dx, point.getY() + dy);
    }
}
