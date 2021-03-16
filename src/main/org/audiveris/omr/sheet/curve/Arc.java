//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                              A r c                                             //
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
package org.audiveris.omr.sheet.curve;

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

    /** Sequence of arc points so far. */
    protected final List<Point> points = new ArrayList<>();

    /** Related model, if any. */
    protected Model model;

    /** Junction point, if any, before points sequence. */
    private Point firstJunction;

    /** Junction point, if any, after points sequence. */
    private Point lastJunction;

    /** Shape found for this arc. */
    private ArcShape shape;

    /** Already assigned to a curve?. */
    private boolean assigned;

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

    //------------------//
    // checkOrientation //
    //------------------//
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

    //--------//
    // getEnd //
    //--------//
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

    //-------------//
    // getJunction //
    //-------------//
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

    //-----------//
    // getLength //
    //-----------//
    /**
     * Report the arc length, as the number of points
     *
     * @return the arc length
     */
    public int getLength ()
    {
        return points.size();
    }

    //----------//
    // getModel //
    //----------//
    /**
     * @return the model, if any
     */
    public Model getModel ()
    {
        return model;
    }

    //----------//
    // setModel //
    //----------//
    /**
     * Assign the arc model.
     *
     * @param model the model to set
     */
    public void setModel (Model model)
    {
        this.model = model;
    }

    //-----------//
    // getPoints //
    //-----------//
    /**
     * @return the points
     */
    public List<Point> getPoints ()
    {
        return points;
    }

    //--------------//
    // getSegmentSq //
    //--------------//
    /**
     * Report the (square of) distance from arc first point to arc last point
     *
     * @return the square of arc distance
     */
    public int getSegmentSq ()
    {
        Point p1 = points.get(0);
        Point p2 = points.get(points.size() - 1);
        int dx = p2.x - p1.x;
        int dy = p2.y - p1.y;

        return (dx * dx) + (dy * dy);
    }

    //----------//
    // getShape //
    //----------//
    /**
     * @return the shape
     */
    public ArcShape getShape ()
    {
        return shape;
    }

    //----------//
    // setShape //
    //----------//
    /**
     * Assign the arc shape.
     *
     * @param shape the shape to set
     */
    public void setShape (ArcShape shape)
    {
        this.shape = shape;
    }

    //------------//
    // getXLength //
    //------------//
    /**
     * Report the arc X length, as the abscissa delta between end points
     *
     * @return the arc X length
     */
    public int getXLength ()
    {
        return Math.abs(points.get(points.size() - 1).x - points.get(0).x);
    }

    //------------//
    // isAssigned //
    //------------//
    /**
     * @return the assigned flag
     */
    public boolean isAssigned ()
    {
        return assigned;
    }

    //-------------//
    // setAssigned //
    //-------------//
    /**
     * Set the assigned fleg.
     *
     * @param assigned the assigned flag to set
     */
    public void setAssigned (boolean assigned)
    {
        this.assigned = assigned;
    }

    //---------//
    // reverse //
    //---------//
    /**
     * Reverse arc internal order (points &amp; junction points).
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

    //-------------//
    // setJunction //
    //-------------//
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        sb.append(internals());
        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // internals //
    //-----------//
    /**
     * Report a string description of class internals
     *
     * @return string description of internals
     */
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
