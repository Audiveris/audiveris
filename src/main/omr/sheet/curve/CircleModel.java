//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      C i r c l e M o d e l                                     //
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
package omr.sheet.curve;

import omr.math.Circle;

import java.awt.Rectangle;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code CircleModel} is a slur model based on Circle curve.
 *
 * @author Hervé Bitteur
 */
public class CircleModel
        implements Model
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Underlying circle. */
    private final Circle circle;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new CircleModel object.
     *
     * @param points the sequence of defining points
     */
    public CircleModel (List<? extends Point2D> points)
    {
        circle = new Circle(points);
    }

    /**
     * Creates a new CircleModel object.
     *
     * @param first  first point
     * @param middle a point rather in the middle
     * @param last   last point
     */
    public CircleModel (Point2D first,
                        Point2D middle,
                        Point2D last)
    {
        circle = new Circle(first, middle, last);
    }

    /**
     * Creates a new CircleModel object.
     *
     * @param circle the provided circle
     */
    public CircleModel (Circle circle)
    {
        this.circle = circle;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public int above ()
    {
        return circle.isAbove() ? 1 : (-1);
    }

    @Override
    public int ccw ()
    {
        return circle.ccw();
    }

    @Override
    public double computeDistance (Collection<? extends Point2D> points)
    {
        return circle.computeDistance(points);
    }

    /**
     * Factory method to try to create a valuable circle model.
     *
     * @param first  first point
     * @param middle a point rather in the middle
     * @param last   last point
     * @return the CircleModel instance if OK, null otherwise
     */
    public static CircleModel create (Point2D first,
                                      Point2D middle,
                                      Point2D last)
    {
        CircleModel model = new CircleModel(first, middle, last);

        if (model.circle.getRadius().isInfinite()) {
            return null;
        } else {
            return model;
        }
    }

    @Override
    public Double getAngle (boolean reverse)
    {
        return circle.getAngle(reverse);
    }

    @Override
    public Rectangle getBounds ()
    {
        return getCurve().getBounds();
    }

    /**
     * Access to underlying circle.
     *
     * @return the circle
     */
    public Circle getCircle ()
    {
        return circle;
    }

    @Override
    public CubicCurve2D getCurve ()
    {
        return circle.getCurve();
    }

    @Override
    public double getDistance ()
    {
        return circle.getDistance();
    }

    @Override
    public Point2D getEndVector (boolean reverse)
    {
        int dir = reverse ? ccw() : (-ccw());
        double angle = getAngle(reverse);

        return new Point2D.Double(-dir * sin(angle), dir * cos(angle));
    }

    @Override
    public Point2D getMidPoint ()
    {
        return circle.getMiddlePoint();
    }

    @Override
    public void reverse ()
    {
        circle.reverse();
    }

    @Override
    public void setDistance (double dist)
    {
        circle.setDistance(dist);
    }
}
