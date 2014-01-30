//----------------------------------------------------------------------------//
//                                                                            //
//                               S l u r I n f o                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.math.Circle;
import static omr.sheet.SlurInfo.Arc;
import static omr.sheet.SlurInfo.Arc.ArcShape;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Class {@code SlursInfo} gathers physical description of a slur.
 *
 * @author Hervé Bitteur
 */
public class SlurInfo
{
    //~ Instance fields --------------------------------------------------------

    /** Unique slur id (in page). */
    private final int id;

    /** Sequence of arcs that compose this slur. */
    private final List<Arc> arcs = new ArrayList<Arc>();

    /** Approximating circle portion. */
    private final Circle circle;

    /** Junction, if any, at start of slur. */
    private Point startJunction;

    /** Junction, if any, at stop of slur. */
    private Point stopJunction;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new SlurInfo object.
     *
     * @param id     slur id
     * @param arcs   The sequence of arcs for this slur
     * @param circle The approximating circle, or null
     */
    public SlurInfo (int id,
                     List<Arc> arcs,
                     Circle circle)
    {
        this.id = id;
        this.arcs.addAll(arcs);
        this.circle = circle;

        if ((circle != null) && circle.isSwapped()) {
            // Swap the slur, so that all points & arcs are ordered clockwise.
            reverse();
        }
    }

    //~ Methods ----------------------------------------------------------------
    /**
     * @return the arcs
     */
    public List<Arc> getArcs ()
    {
        return arcs;
    }

    //-----------//
    // getBounds //
    //-----------//
    public Rectangle getBounds ()
    {
        return circle.getCurve()
                .getBounds();
    }

    /**
     * @return the circle
     */
    public Circle getCircle ()
    {
        return circle;
    }

    //------------------//
    // getSlurJunctions //
    //------------------//
    /**
     * Retrieve both ending junctions for the slur.
     */
    public void getSlurJunctions ()
    {
        // Check orientation of all arcs
        if (arcs.size() > 1) {
            for (int i = 0; i < (arcs.size() - 1); i++) {
                Arc a0 = arcs.get(i);
                Arc a1 = arcs.get(i + 1);
                Point common = junctionOf(a0, a1);

                if ((a1.getStopJunction() != null)
                    && a1.getStopJunction()
                        .equals(common)) {
                    a1.reverse();
                }
            }
        }

        startJunction = arcs.get(0)
                .getStartJunction();
        stopJunction = arcs.get(arcs.size() - 1)
                .getStopJunction();
    }

    /**
     * @return the startJunction
     */
    public Point getStartJunction ()
    {
        return startJunction;
    }

    /**
     * @return the stopJunction
     */
    public Point getStopJunction ()
    {
        return stopJunction;
    }

    /**
     * @param startJunction the startJunction to set
     */
    public void setStartJunction (Point startJunction)
    {
        this.startJunction = startJunction;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Slur#")
                .append(id);

        if (circle != null) {
            sb.append(String.format(" dist:%.3f", circle.getDistance()));
        }

        boolean first = true;

        for (Arc arc : arcs) {
            if (first) {
                first = false;
            } else {
                Point j = arc.getJunction(true);

                if (j != null) {
                    sb.append(" <")
                            .append(j.x)
                            .append(",")
                            .append(j.y)
                            .append(">");
                }
            }

            sb.append(" ")
                    .append(arc);
        }

        sb.append("}");

        return sb.toString();
    }

    //--------//
    // assign //
    //--------//
    /**
     * Flag the slur arcs as assigned and remember this (good) slur.
     */
    void assign ()
    {
        for (Arc arc : arcs) {
            arc.assigned = true;
        }
    }

    //---------------------//
    // checkArcOrientation //
    //---------------------//
    /**
     * Make the orientation of extension arc compatible with slur
     * orientation.
     * Normal orientation. : SLUR : ARC : (arc start cannot be slur start)
     * Reverse orientation : ARC : SLUR : (arc stop cannot be slur stop)
     *
     * @param arc     the arc to check
     * @param reverse desired orientation
     */
    void checkArcOrientation (Arc arc,
                              boolean reverse)
    {
        // Need to reverse arc to conform with arcs list?
        if (reverse) {
            if ((arc.getStartJunction() != null)
                && arc.getStartJunction()
                    .equals(startJunction)) {
                arc.reverse();
            }
        } else {
            if ((arc.getStopJunction() != null)
                && arc.getStopJunction()
                    .equals(stopJunction)) {
                arc.reverse();
            }
        }
    }

    //------------//
    // getAllArcs //
    //------------//
    /**
     * Report the sequence of slur arcs, augmented by the provided
     * arc.
     *
     * @param arc     additional arc
     * @param reverse desired orientation
     * @return proper sequence of all arcs
     */
    List<Arc> getAllArcs (Arc arc,
                          boolean reverse)
    {
        List<Arc> allArcs = new ArrayList<Arc>(arcs);

        if (reverse) {
            allArcs.add(0, arc);
        } else {
            allArcs.add(arc);
        }

        return allArcs;
    }

    //-----------//
    // getEndArc //
    //-----------//
    /**
     * Report the ending arc in the orientation desired.
     *
     * @param reverse desired orientation
     * @return the proper ending arc
     */
    Arc getEndArc (boolean reverse)
    {
        if (reverse) {
            return arcs.get(0);
        } else {
            return arcs.get(arcs.size() - 1);
        }
    }

    //------------//
    // junctionOf //
    //------------//
    /**
     * Report the junction point between arcs a1 and a2.
     *
     * @param a1 first arc
     * @param a2 second arc
     * @return the common junction point if any, otherwise null
     */
    private Point junctionOf (Arc a1,
                              Arc a2)
    {
        List<Point> s1 = new ArrayList<Point>();
        s1.add(a1.getStartJunction());
        s1.add(a1.getStopJunction());

        List<Point> s2 = new ArrayList<Point>();
        s2.add(a2.getStartJunction());
        s2.add(a2.getStopJunction());
        s1.retainAll(s2);

        if (!s1.isEmpty()) {
            return s1.get(0);
        } else {
            return null;
        }
    }

    //---------//
    // reverse //
    //---------//
    /**
     * Reverse the slur components.
     */
    private void reverse ()
    {
        // Reverse arcs list and each arc
        List<Arc> rev = new ArrayList<Arc>(arcs.size());

        for (ListIterator<Arc> it = arcs.listIterator(arcs.size());
                it.hasPrevious();) {
            Arc arc = it.previous();
            arc.reverse();
            rev.add(arc);
        }

        arcs.clear();
        arcs.addAll(rev);

        // Swap junctions
        Point temp = startJunction;
        startJunction = stopJunction;
        stopJunction = temp;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----//
    // Arc //
    //-----//
    /**
     * Represents an arc of points between two junction points.
     * An arc has no "intrinsic" orientation, it is simply set according to the
     * orientation of the initial scanning of the arc points, and can be
     * reversed via the reverse() method.
     * Two touching junction points can be joined by a "void" arc with no
     * internal points.
     */
    public static class Arc
    {
        //~ Enumerations -------------------------------------------------------

        /**
         * Shape detected for arc.
         */
        public static enum ArcShape
        {
            //~ Enumeration constant initializers ------------------------------

            /** Not yet known. */
            UNKNOWN,
            /** Short arc. Can be tested as slur or wedge extension. */
            SHORT,
            /** Long portion of slur. Can be part of slur only. */
            SLUR,
            /** Long straight line. Can be part of wedge (and slur). */
            LINE,
            /** Long portion of staff line. Cannot be part of slur or wedge. */
            STAFF_LINE,
            /** Long arc, but no shape detected. Cannot be part of slur/wedge */
            IRRELEVANT;
            //~ Methods --------------------------------------------------------

            public boolean isLineRelevant ()
            {
                return (this == SHORT) || (this == LINE);
            }

            public boolean isSlurRelevant ()
            {
                return (this == SHORT) || (this == SLUR) || (this == LINE);
            }
        }

        //~ Instance fields ----------------------------------------------------
        /** Sequence of arc points so far. */
        List<Point> points = new ArrayList<Point>();

        /** Junction point, if any, at beginning of points. */
        private Point startJunction;

        /** Junction point, if any, at end of points. */
        private Point stopJunction;

        /** Shape found for this arc. */
        ArcShape shape;

        /** Related circle, if any. */
        Circle circle;

        /** Assigned to a slur?. */
        boolean assigned;

        //~ Constructors -------------------------------------------------------
        /**
         * Create an arc with perhaps a startJunction.
         *
         * @param startJunction start junction point, if any
         */
        public Arc (Point startJunction)
        {
            if (startJunction != null) {
                this.startJunction = startJunction;
            }
        }

        /**
         * Void arc meant to link two touching junction points, with
         * no points in between.
         *
         * @param startJunction first junction point, not null
         * @param stopJunction  second junction point, not null
         */
        public Arc (Point startJunction,
                    Point stopJunction)
        {
            this.startJunction = startJunction;
            this.stopJunction = stopJunction;
            shape = ArcShape.SHORT;
        }

        //~ Methods ------------------------------------------------------------
        /**
         * Report ending point in provided orientation.
         *
         * @param reverse provided orientation
         * @return proper ending point, or null if none
         */
        public final Point getEnd (boolean reverse)
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
         * Report the arc length, as the number of points
         *
         * @return the arc length
         */
        public final int getLength ()
        {
            return points.size();
        }

        /**
         * @return the startJunction
         */
        public final Point getStartJunction ()
        {
            return startJunction;
        }

        /**
         * @return the stopJunction
         */
        public final Point getStopJunction ()
        {
            return stopJunction;
        }

        /**
         * @param startJunction the startJunction to set
         */
        public final void setStartJunction (Point startJunction)
        {
            this.startJunction = startJunction;
        }

        /**
         * @param stopJunction the stopJunction to set
         */
        public final void setStopJunction (Point stopJunction)
        {
            this.stopJunction = stopJunction;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Arc");

            if (!points.isEmpty()) {
                Point p0 = points.get(0);
                sb.append("[")
                        .append(p0.x)
                        .append(",")
                        .append(p0.y)
                        .append("]");

                if (points.size() > 1) {
                    Point p2 = points.get(points.size() - 1);
                    sb.append("[")
                            .append(p2.x)
                            .append(",")
                            .append(p2.y)
                            .append("]");
                }
            } else {
                sb.append(" VOID");
            }

            return sb.toString();
        }

        /**
         * Report the junction point, if any, in the provided orientation.
         *
         * @param reverse provided orientation
         * @return the proper junction point, perhaps null
         */
        private Point getJunction (boolean reverse)
        {
            if (reverse) {
                return startJunction;
            } else {
                return stopJunction;
            }
        }

        /**
         * Reverse arc internal order (points & junction points).
         */
        private void reverse ()
        {
            // Reverse junctions
            Point temp = startJunction;
            startJunction = stopJunction;
            stopJunction = temp;

            // Reverse points list
            List<Point> rev = new ArrayList<Point>(points.size());

            for (ListIterator<Point> it = points.listIterator(points.size());
                    it.hasPrevious();) {
                rev.add(it.previous());
            }

            points.clear();
            points.addAll(rev);
        }
    }
}
