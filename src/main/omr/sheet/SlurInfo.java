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
import omr.math.LineUtil;
import static omr.sheet.SlurInfo.Arc;
import static omr.sheet.SlurInfo.Arc.ArcShape;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Class {@code SlursInfo} gathers physical description of a slur.
 * <p>
 * Short and medium slurs generally fit a circle rather well.
 * A long slur may not, it may be closer to an ellipsis, hence the idea to use
 * local "side" circles, one at start and one at end of slur. 
 * The purpose is to be able to accurately evaluate slur extensions.
 * <p>
 * With one global circle or with two side circles, we should be able to adjust
 * a global bézier curve.
 *
 * @author Hervé Bitteur
 */
public class SlurInfo
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            SlurInfo.class);

    //~ Instance fields --------------------------------------------------------
    /** Unique slur id (in page). */
    private final int id;

    /** Sequence of arcs that compose this slur. */
    private final List<Arc> arcs = new ArrayList<Arc>();

    /** Approximating global circle. */
    private Circle circle;

    /** Approximating first circle. */
    private Circle firstCircle;

    /** Approximating last circle. */
    private Circle lastCircle;

    /** True for slur above heads, false for below. */
    private boolean above;

    /** Unity vector from segment middle to center. */
    private Point2D bisUnit;

    /** Junction, if any, at start of slur. */
    private Point startJunction;

    /** Junction, if any, at stop of slur. */
    private Point stopJunction;

    /** True for slur rather horizontal. */
    private boolean horizontal;

    /** Area for first extension. */
    private Area firstExtArea;

    /** Area for last extension. */
    private Area lastExtArea;

    /** Area for first notes. */
    private Area firstArea;

    /** Area for last notes. */
    private Area lastArea;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new SlurInfo object.
     *
     * @param id     slur id
     * @param arcs   The sequence of arcs for this slur
     * @param circle The approximating circle, perhaps null
     */
    public SlurInfo (int id,
                     List<Arc> arcs,
                     Circle circle)
    {
        this.id = id;
        this.arcs.addAll(arcs);
        setCircle(circle);
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // assign //
    //--------//
    /**
     * Flag the slur arcs as assigned.
     */
    public void assign ()
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
     * (Normal) orientation : SLUR - ARC : (arc start cannot be slur start)
     * Reverse. orientation : ARC - SLUR : (arc stop cannot be slur stop)
     *
     * @param arc     the arc to check
     * @param reverse orientation of slur extension
     */
    public void checkArcOrientation (Arc arc,
                                     boolean reverse)
    {
        if (reverse) {
            // Normal orientation, check at slur start
            if (startJunction != null) {
                if ((arc.getStartJunction() != null)
                    && arc.getStartJunction().equals(startJunction)) {
                    arc.reverse();
                }
            } else {
                // Slur with free ending, use shortest distance
                Point slurEnd = getEnd(reverse);
                double toStart = slurEnd.distanceSq(arc.getEnd(true));
                double toStop = slurEnd.distanceSq(arc.getEnd(false));

                if (toStart < toStop) {
                    arc.reverse();
                }
            }
        } else {
            // Normal orientation, check at slur stop
            if (stopJunction != null) {
                // Slur ending at pivot
                if ((arc.getStopJunction() != null)
                    && arc.getStopJunction().equals(stopJunction)) {
                    arc.reverse();
                }
            } else {
                // Slur with free ending, use shortest distance
                Point slurEnd = getEnd(reverse);
                double toStart = slurEnd.distanceSq(arc.getEnd(true));
                double toStop = slurEnd.distanceSq(arc.getEnd(false));

                if (toStop < toStart) {
                    arc.reverse();
                }
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
    public List<Arc> getAllArcs (Arc arc,
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

    /**
     * @return the arcs
     */
    public List<Arc> getArcs ()
    {
        return arcs;
    }

    //---------//
    // getArea //
    //---------//
    public Area getArea (HorizontalSide side)
    {
        if (getFirstPoint().x < getLastPoint().x) {
            return (side == LEFT) ? firstArea : lastArea;
        } else {
            return (side == LEFT) ? lastArea : firstArea;
        }
    }

    /**
     * @return the bisUnit
     */
    public Point2D getBisUnit ()
    {
        return bisUnit;
    }

    //-----------//
    // getBounds //
    //-----------//
    public Rectangle getBounds ()
    {
        return circle.getCurve().getBounds();
    }

    /**
     * @return the circle
     */
    public Circle getCircle ()
    {
        return circle;
    }

    //--------//
    // getEnd //
    //--------//
    public Point getEnd (boolean reverse)
    {
        if (reverse) {
            return getFirstPoint();
        } else {
            return getLastPoint();
        }
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
    public Arc getEndArc (boolean reverse)
    {
        if (reverse) {
            return arcs.get(0);
        } else {
            return arcs.get(arcs.size() - 1);
        }
    }

    //------------//
    // getExtArea //
    //------------//
    public Area getExtArea (boolean reverse)
    {
        if (reverse) {
            return firstExtArea;
        } else {
            return lastExtArea;
        }
    }

    /**
     * @return the firstCircle
     */
    public Circle getFirstCircle ()
    {
        return firstCircle;
    }

    /**
     * @param firstCircle the firstCircle to set
     */
    public void setFirstCircle (Circle firstCircle)
    {
        this.firstCircle = firstCircle;
    }

    //---------------//
    // getFirstPoint //
    //---------------//
    public Point getFirstPoint ()
    {
        Arc firstArc = getEndArc(true);

        return (firstArc.getLength() > 0) ? firstArc.getEnd(true)
                : firstArc.getStopJunction();
    }

    /**
     * @return the id
     */
    public int getId ()
    {
        return id;
    }

    /**
     * @return the lastCircle
     */
    public Circle getLastCircle ()
    {
        return lastCircle;
    }

    /**
     * @param lastCircle the lastCircle to set
     */
    public void setLastCircle (Circle lastCircle)
    {
        this.lastCircle = lastCircle;
    }

    //--------------//
    // getLastPoint //
    //--------------//
    public Point getLastPoint ()
    {
        Arc lastArc = getEndArc(false);

        return (lastArc.getLength() > 0) ? lastArc.getEnd(false)
                : lastArc.getStartJunction();
    }

    //-----------//
    // getLength //
    //-----------//
    /**
     * Report the number of points that compose the slur.
     *
     * @return the slur number of points
     */
    public int getLength ()
    {
        int length = 0;

        for (Arc arc : arcs) {
            length += arc.getLength();
        }

        return length;
    }

    //-------------//
    // getMidPoint //
    //-------------//
    public Point2D getMidPoint ()
    {
        return circle.getMiddlePoint();
    }

    //----------//
    // getPoint //
    //----------//
    /**
     * Convenient method to retrieve side point.
     *
     * @param side which absolute side is desired
     * @return the point on desired absolute side
     */
    public Point getPoint (HorizontalSide side)
    {
        Point first = getFirstPoint();
        Point last = getLastPoint();

        if (first.x < last.x) {
            return (side == LEFT) ? first : last;
        } else {
            return (side == LEFT) ? last : first;
        }
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
                    && a1.getStopJunction().equals(common)) {
                    a1.reverse();
                }
            }
        }

        startJunction = arcs.get(0).getStartJunction();
        stopJunction = arcs.get(arcs.size() - 1).getStopJunction();
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
     * @return the above value
     */
    public boolean isAbove ()
    {
        return above;
    }

    /**
     * @return the horizontal
     */
    public boolean isHorizontal ()
    {
        return horizontal;
    }

    public void reverse ()
    {
        // Reverse junctions
        Point temp = startJunction;
        startJunction = stopJunction;
        stopJunction = temp;

        // Reverse arc list
        List<Arc> rev = new ArrayList<Arc>(arcs.size());

        for (ListIterator<Arc> it = arcs.listIterator(arcs.size());
                it.hasPrevious();) {
            rev.add(it.previous());
        }

        arcs.clear();
        arcs.addAll(rev);

        // Reverse circle
        if (circle != null) {
            circle.reverse();
        }
    }

    //-----------//
    // setCircle //
    //-----------//
    public void setCircle (Circle circle)
    {
        if (circle != null) {
            this.circle = new Circle(circle);
            above = circle.isAbove();
            bisUnit = computeBisector();
        }
    }

    /**
     * Record extension area (to allow slur merge)
     *
     * @param area    the extension area on 'reverse' side
     * @param reverse which end
     */
    public void setExtArea (Area area,
                            boolean reverse)
    {
        if (reverse) {
            firstExtArea = area;
        } else {
            lastExtArea = area;
        }
    }

    /**
     * @param firstArea the firstArea to set
     */
    public void setFirstArea (Area firstArea)
    {
        this.firstArea = firstArea;
    }

    /**
     * @param horizontal the horizontal to set
     */
    public void setHorizontal (boolean horizontal)
    {
        this.horizontal = horizontal;
    }

    /**
     * @param lastArea the lastArea to set
     */
    public void setLastArea (Area lastArea)
    {
        this.lastArea = lastArea;
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
        sb.append("{Slur#").append(id);

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
                    sb.append(" <").append(j.x).append(",").append(j.y)
                            .append(">");
                }
            }

            sb.append(" ").append(arc);
        }

        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // computeBisector //
    //-----------------//
    /**
     * Compute bisector vector.
     */
    private Point2D.Double computeBisector ()
    {
        Point first = getFirstPoint();
        Point last = getLastPoint();
        Line2D bisector = (circle.ccw() == 1) ? LineUtil.bisector(last, first)
                : LineUtil.bisector(first, last);
        double length = bisector.getP1().distance(bisector.getP2());

        return new Point2D.Double(
                (bisector.getX2() - bisector.getX1()) / length,
                (bisector.getY2() - bisector.getY1()) / length);
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

    //~ Inner Classes ----------------------------------------------------------
    //-----//
    // Arc //
    //-----//
    /**
     * Represents an arc of points between two junction points.
     * <p>
     * An arc has no "intrinsic" orientation, it is simply set according to the
     * orientation of the initial scanning of the arc points, and can be
     * reversed via the reverse() method.
     * <p>
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
            //~ Instance fields ------------------------------------------------

            private final boolean forSlur; // OK for slur

            private final boolean forWedge; // OK for wedge

            //~ Constructors ---------------------------------------------------
            ArcShape (boolean forSlur,
                      boolean forWedge)
            {
                this.forSlur = forSlur;
                this.forWedge = forWedge;
            }

            //~ Methods --------------------------------------------------------
            public boolean isSlurRelevant ()
            {
                return forSlur;
            }

            public boolean isWedgeRelevant ()
            {
                return forWedge;
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
         * Report ending point in provided orientation.
         *
         * @param reverse provided orientation
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
         * Report the arc length, as the number of points
         *
         * @return the arc length
         */
        public int getLength ()
        {
            return points.size();
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
         * Reverse arc internal order (points & junction points).
         */
        public void reverse ()
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

            // Reverse circle
            if (circle != null) {
                circle.reverse();
            }
        }

        /**
         * @param startJunction the startJunction to set
         */
        public void setStartJunction (Point startJunction)
        {
            this.startJunction = startJunction;
        }

        /**
         * @param stopJunction the stopJunction to set
         */
        public void setStopJunction (Point stopJunction)
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
                sb.append("[").append(p0.x).append(",").append(p0.y).append(
                        "]");

                if (points.size() > 1) {
                    Point p2 = points.get(points.size() - 1);
                    sb.append("[").append(p2.x).append(",").append(p2.y)
                            .append("]");
                }
            } else {
                sb.append(" VOID");
            }

            return sb.toString();
        }

        /**
         * Report the junction point, if any, in the desired orientation.
         *
         * @param reverse desired orientation
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
    }
}
