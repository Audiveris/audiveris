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
package omr.skeleton;

import omr.math.Circle;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Represents an arc of points between two junction points.
 * <p>
 * An arc has no "intrinsic" orientation, it is simply set according to the orientation of the
 * initial scanning of the arc points, and can be reversed via the reverse() method.
 * <p>
 * Two touching junction points can be joined by a "void" arc with no internal points.
 */
public class Arc
{
    //~ Enumerations -------------------------------------------------------------------------------

    /**
     * Shape detected for arc.
     */
    public static enum ArcShape
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /**
         * Not yet known.
         */
        UNKNOWN(false, false),
        /**
         * Short arc.
         * Can be tested as slur or wedge extension.
         */
        SHORT(true, true),
        /**
         * Long portion of slur.
         * Can be part of slur only.
         */
        SLUR(true, false),
        /**
         * Long straight line.
         * Can be part of wedge (and slur).
         */
        LINE(true, true),
        /**
         * Short portion of staff line.
         * Can be part of slur only.
         */
        STAFF_ARC(true, false),
        /**
         * Long arc, but no shape detected.
         * Cannot be part of slur/wedge
         */
        IRRELEVANT(false, false);
        //~ Instance fields ------------------------------------------------------------------------

        private final boolean forSlur; // OK for slur

        private final boolean forWedge; // OK for wedge

        //~ Constructors ---------------------------------------------------------------------------
        ArcShape (boolean forSlur,
                  boolean forWedge)
        {
            this.forSlur = forSlur;
            this.forWedge = forWedge;
        }

        //~ Methods --------------------------------------------------------------------------------
        public boolean isSlurRelevant ()
        {
            return forSlur;
        }

        public boolean isWedgeRelevant ()
        {
            return forWedge;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sequence of arc points so far. */
    List<Point> points = new ArrayList<Point>();

    /** Junction point, if any, at beginning of points. */
    private Point firstJunction;

    /** Junction point, if any, at end of points. */
    private Point lastJunction;

    /** Shape found for this arc. */
    ArcShape shape;

    /** Related circle, if any. */
    Circle circle;

    /** Assigned to a slur?. */
    boolean assigned;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an arc with perhaps a firstJunction.
     *
     * @param startJunction start junction point, if any
     */
    public Arc (Point startJunction)
    {
        if (startJunction != null) {
            this.firstJunction = startJunction;
        }
    }

    /**
     * Void arc meant to link two touching junction points, with no points in between
     *
     * @param startJunction first junction point, not null
     * @param stopJunction  second junction point, not null
     */
    public Arc (Point startJunction,
                Point stopJunction)
    {
        this.firstJunction = startJunction;
        this.lastJunction = stopJunction;
        shape = ArcShape.SHORT;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Make sure arc goes from left to right.
     * (This is checked only for arcs with sufficient length).
     */
    public void checkOrientation ()
    {
        if (getEnd(true).x > getEnd(false).x) {
            reverse();
        }
    }

    /**
     * Report ending point on desired side.
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
     * Report the sequence of 'count' points on desired side of arc.
     *
     * @param count   the number of points to retrieve
     * @param reverse desired arc side
     * @return the sequence of desired points, perhaps limited by the arc length itself.
     */
    public List<Point> getSidePoints (int count,
                                      boolean reverse)
    {
        List<Point> seq = new ArrayList<Point>();
        int n = 0;

        if (reverse) {
            for (Point p : points) {
                if (++n > count) {
                    return seq;
                }

                seq.add(p);
            }
        } else {
            for (ListIterator<Point> itp = points.listIterator(points.size()); itp.hasPrevious();) {
                Point p = itp.previous();

                if (++n > count) {
                    return seq;
                }

                seq.add(p);
            }
        }

        return seq;
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

        // Reverse points list
        List<Point> rev = new ArrayList<Point>(points.size());

        for (ListIterator<Point> it = points.listIterator(points.size()); it.hasPrevious();) {
            rev.add(it.previous());
        }

        points.clear();
        points.addAll(rev);

        // Reverse circle
        if (circle != null) {
            circle.reverse();
        }
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

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Arc");

        if (!points.isEmpty()) {
            Point p0 = points.get(0);
            sb.append("[").append(p0.x).append(",").append(p0.y).append("]");

            if (points.size() > 1) {
                Point p2 = points.get(points.size() - 1);
                sb.append("[").append(p2.x).append(",").append(p2.y).append("]");
            }
        } else {
            sb.append(" VOID");
        }

        return sb.toString();
    }
}
