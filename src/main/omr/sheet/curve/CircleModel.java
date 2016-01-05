//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      C i r c l e M o d e l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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
