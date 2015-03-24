//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                              A r c                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code Arc} represents a sequence of points, with perhaps a junction point at
 * start and perhaps a junction point at stop.
 * <p>
 * An arc has no "intrinsic" orientation, it is simply built according to the orientation of the
 * initial scanning of the arc points, and can be reversed via the reverse() method.
 * <p>
 * Two touching junction points can be joined by a "void" arc with no internal points.
 *
 * @author Hervé Bitteur
 */
public class Arc
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Arc.class);

    /** Comparison by decreasing length. */
    public static final Comparator<Arc> byReverseLength = new Comparator<Arc>()
    {
        @Override
        public int compare (Arc a1,
                            Arc a2)
        {
            return Integer.compare(a2.getLength(), a1.getLength());
        }
    };

    /** Comparison by decreasing x length. */
    public static final Comparator<Arc> byReverseXLength = new Comparator<Arc>()
    {
        @Override
        public int compare (Arc a1,
                            Arc a2)
        {
            return Double.compare(a2.getXLength(), a1.getXLength());
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sequence of arc points so far. */
    protected final List<Point> points = new ArrayList<Point>();

    /** Junction point, if any, before points sequence. */
    private Point firstJunction;

    /** Junction point, if any, after points sequence. */
    private Point lastJunction;

    /** Shape found for this arc. */
    private ArcShape shape;

    /** Related model, if any. */
    protected Model model;

    /** Already assigned to a curve?. */
    private boolean assigned;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an arc with perhaps a firstJunction.
     *
     * @param firstJunction start junction point, if any
     */
    public Arc (Point firstJunction)
    {
        if (firstJunction != null) {
            this.firstJunction = firstJunction;
        }
    }

    /**
     * Void arc meant to link two touching junction points, with no points in between.
     *
     * @param firstJunction first junction point, not null
     * @param lastJunction  second junction point, not null
     */
    public Arc (Point firstJunction,
                Point lastJunction)
    {
        this.firstJunction = firstJunction;
        this.lastJunction = lastJunction;
        shape = ArcShape.SHORT;
    }

    /**
     * Fully defined arc.
     *
     * @param firstJunction first junction point, if any
     * @param lastJunction  second junction point, if any
     * @param points        sequence of defining points
     * @param model         underlying model, if any
     */
    public Arc (Point firstJunction,
                Point lastJunction,
                List<Point> points,
                Model model)
    {
        this.firstJunction = firstJunction;
        this.lastJunction = lastJunction;
        this.points.addAll(points);
        this.model = model;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Make sure arc goes from left to right.
     */
    public void checkOrientation ()
    {
        if (!points.isEmpty()) {
            if (getEnd(true).x > getEnd(false).x) {
                reverse();
            }
        }
    }

    /**
     * Report ending point, if any, on desired side.
     *
     * @param reverse desired side
     * @return proper ending point, or null if none
     */
    public Point getEnd (boolean reverse)
    {
        if (points.isEmpty()) {
            return null;
        }

        if (reverse) {
            return points.get(0);
        } else {
            return points.get(points.size() - 1);
        }
    }

    /**
     * Report the (square of) distance from arc first point to arc last point
     *
     * @return the square of arc distance
     */
    public int getExtensionSq ()
    {
        Point p1 = points.get(0);
        Point p2 = points.get(points.size() - 1);
        int dx = p2.x - p1.x;
        int dy = p2.y - p1.y;

        return (dx * dx) + (dy * dy);
    }

    /**
     * Report the junction point, if any, on the desired side.
     *
     * @param reverse desired side
     * @return the proper junction point, perhaps null
     */
    public Point getJunction (boolean reverse)
    {
        if (reverse) {
            return firstJunction;
        } else {
            return lastJunction;
        }
    }

    /**
     * Report the arc length, as the number of points
     *
     * @return the arc length
     */
    public int getLength ()
    {
        return points.size();
    }

    /**
     * @return the model, if any
     */
    public Model getModel ()
    {
        return model;
    }

    /**
     * @return the points
     */
    public List<Point> getPoints ()
    {
        return points;
    }

    /**
     * @return the shape
     */
    public ArcShape getShape ()
    {
        return shape;
    }

    /**
     * Report the arc X length, as the abscissa delta between end points
     *
     * @return the arc X length
     */
    public int getXLength ()
    {
        return Math.abs(points.get(points.size() - 1).x - points.get(0).x);
    }

    /**
     * @return the assigned flag
     */
    public boolean isAssigned ()
    {
        return assigned;
    }

    /**
     * Reverse arc internal order (points & junction points).
     */
    public void reverse ()
    {
        // Reverse junctions
        Point temp = firstJunction;
        firstJunction = lastJunction;
        lastJunction = temp;

        // Reverse points
        Collections.reverse(points);

        // Reverse model
        if (model != null) {
            model.reverse();
        }
    }

    /**
     * @param assigned the assigned flag to set
     */
    public void setAssigned (boolean assigned)
    {
        this.assigned = assigned;
    }

    /**
     * Set a junction point.
     *
     * @param junction the point to set
     * @param reverse  desired side
     */
    public void setJunction (Point junction,
                             boolean reverse)
    {
        if (reverse) {
            firstJunction = junction;
        } else {
            lastJunction = junction;
        }
    }

    /**
     * @param model the model to set
     */
    public void setModel (Model model)
    {
        this.model = model;
    }

    /**
     * @param shape the shape to set
     */
    public void setShape (ArcShape shape)
    {
        this.shape = shape;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        sb.append(internals());
        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // internals //
    //-----------//
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder();

        if (!points.isEmpty()) {
            Point p1 = points.get(0);
            sb.append("[").append(p1.x).append(",").append(p1.y).append("]");

            if (points.size() > 1) {
                Point p2 = points.get(points.size() - 1);
                sb.append("[").append(p2.x).append(",").append(p2.y).append("]");
            }
        } else {
            sb.append(" VOID");
        }

        if (model != null) {
            sb.append(String.format(" dist:%.2f", model.getDistance()));
        }

        return sb.toString();
    }
}
