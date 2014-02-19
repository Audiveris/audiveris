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

import omr.glyph.ui.AttachmentHolder;
import omr.glyph.ui.BasicAttachmentHolder;

import omr.math.Circle;
import omr.math.LineUtil;
import static omr.sheet.SlurInfo.Arc;
import static omr.sheet.SlurInfo.Arc.ArcShape;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import omr.grid.FilamentLine;

/**
 * Class {@code SlursInfo} gathers physical description of a slur.
 * <p>
 * Short and medium slurs generally fit a global circle rather well.
 * A long slur may not, it may be closer to an ellipsis, hence the idea to use
 * local osculatory circles, one at start and one at end of slur.
 * The purpose is to be able to accurately evaluate slur extensions.
 * <p>
 * With one global circle or with two osculatory circles, we should be able to
 * compute a global bezier curve.
 *
 * @author Hervé Bitteur
 */
public class SlurInfo
    implements AttachmentHolder
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

    /** Approximating first side circle. */
    private Circle firstCircle;

    /** Approximating last side circle. */
    private Circle lastCircle;

    /** Number of points for side circles. */
    private final int sideLength;

    /** True for slur above heads, false for below. */
    private boolean above;

    /** Unity vector from segment middle to center. */
    private Point2D bisUnit;

    /** Junction, if any, at start of slur. */
    private Point firstJunction;

    /** Junction, if any, at stop of slur. */
    private Point lastJunction;

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

    /** Bézier curve for slur. */
    private CubicCurve2D curve;

    /** Potential attachments, lazily allocated. */
    private AttachmentHolder attachments;
    
    /** Staff line most recently crossed. */
    private FilamentLine crossedLine;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SlurInfo object.
     *
     * @param id         slur id
     * @param arcs       The sequence of arcs for this slur
     * @param circle     The approximating circle, perhaps null
     * @param sideLength length of side circles
     */
    public SlurInfo (int       id,
                     List<Arc> arcs,
                     Circle    circle,
                     int       sideLength)
    {
        this.id = id;
        this.arcs.addAll(arcs);
        setCircle(circle);
        this.sideLength = sideLength;
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // addAttachment //
    //---------------//
    @Override
    public void addAttachment (String         id,
                               java.awt.Shape attachment)
    {
        assert attachment != null : "Adding a null attachment";

        if (attachments == null) {
            attachments = new BasicAttachmentHolder();
        }

        attachments.addAttachment(id, attachment);
    }

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
    public void checkArcOrientation (Arc     arc,
                                     boolean reverse)
    {
        if (reverse) {
            // Normal orientation, check at slur start
            if (firstJunction != null) {
                if ((arc.getJunction(true) != null) &&
                    arc.getJunction(true).equals(firstJunction)) {
                    arc.reverse();
                }
            } else {
                // Slur with free ending, use shortest distance
                Point  slurEnd = getEnd(reverse);
                double toStart = slurEnd.distanceSq(arc.getEnd(true));
                double toStop = slurEnd.distanceSq(arc.getEnd(false));

                if (toStart < toStop) {
                    arc.reverse();
                }
            }
        } else {
            // Normal orientation, check at slur stop
            if (lastJunction != null) {
                // Slur ending at pivot
                if ((arc.getJunction(false) != null) &&
                    arc.getJunction(false).equals(lastJunction)) {
                    arc.reverse();
                }
            } else {
                // Slur with free ending, use shortest distance
                Point  slurEnd = getEnd(reverse);
                double toStart = slurEnd.distanceSq(arc.getEnd(true));
                double toStop = slurEnd.distanceSq(arc.getEnd(false));

                if (toStop < toStart) {
                    arc.reverse();
                }
            }
        }
    }

    //-------------------//
    // computeSideCircle //
    //-------------------//
    /**
     * Compute a side circle (on side designated by reverse) from the
     * provided sequence of arcs
     *
     * @param points  the full sequence of points
     * @param reverse desired side
     * @return the side circle, or null if unsuccessful
     */
    public Circle computeSideCircle (List<Point> points,
                                     boolean     reverse)
    {
        int np = points.size();

        if (np < sideLength) {
            return null;
        }

        if (reverse) {
            points = points.subList(0, sideLength);
        } else {
            points = points.subList(np - sideLength, np);
        }

        np = points.size();

        Point  p0 = points.get(0);
        Point  p1 = points.get(np / 2);
        Point  p2 = points.get(np - 1);

        Circle rough = new Circle(p0, p1, p2);

        if (rough.getRadius().isInfinite()) {
            return null;
        } else {
            return rough;
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
     * @param reverse desired side
     * @return proper sequence of all arcs
     */
    public List<Arc> getAllArcs (Arc     arc,
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

    //---------//
    // getArcs //
    //---------//
    /**
     * Report the sequence of arcs in slur
     *
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

    //---------//
    // getArea //
    //---------//
    public Area getArea (boolean reverse)
    {
        return reverse ? firstArea : lastArea;
    }

    //----------------//
    // getAttachments //
    //----------------//
    @Override
    public Map<String, java.awt.Shape> getAttachments ()
    {
        if (attachments != null) {
            return attachments.getAttachments();
        } else {
            return Collections.emptyMap();
        }
    }

    //----------------//
    // getBackupPoint //
    //----------------//
    /**
     * Report point, whose index is 'count' before slur end.
     *
     * @param count   how many points to backup
     * @param reverse desired end side
     * @return the point, a few positions before slur end, or null if there is
     *         not enough points available.
     */
    public Point getBackupPoint (int     count,
                                 boolean reverse)
    {
        int idx = -1;

        if (reverse) {
            for (Arc arc : arcs) {
                for (Point p : arc.points) {
                    idx++;

                    if (idx >= count) {
                        return p;
                    }
                }
            }
        } else {
            for (ListIterator<Arc> it = arcs.listIterator(arcs.size());
                 it.hasPrevious();) {
                Arc arc = it.previous();

                for (ListIterator<Point> itp = arc.points.listIterator(
                    arc.points.size()); itp.hasPrevious();) {
                    Point p = itp.previous();
                    idx++;

                    if (idx >= count) {
                        return p;
                    }
                }
            }
        }

        return null;
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

    /**
     * @return the last crossed Line
     */
    public FilamentLine getCrossedLine ()
    {
        return crossedLine;
    }

    /**
     * @param crossedLine the last crossed Line to set
     */
    public void setCrossedLine (FilamentLine crossedLine)
    {
        this.crossedLine = crossedLine;
    }

    //----------//
    // getCurve //
    //----------//
    /**
     * Report the left-to-right Bézier curve which best approximates
     * the slur.
     * <p>
     * It is built by combining the left half (point & ctrl point) of left
     * circle curve and the right half (ctrl point & point) of right circle
     * curve.
     * Vectors from point to related control point are applied a ratio extension
     * so that curve middle point (M) fits on slur middle point (M').
     * We apply the same ratio on both vectors, which may not be the best choice
     * but that's enough for a first version.
     * On a bezier curve, naming P the middle point of segment (P1,P2) and C the
     * middle point of segment (CP1,CP2), we always have vector PC = 4/3 of
     * vector PM.
     * Hence, (PC' - PC) = 4/3 (PM' - PM)
     * or (ratio - 1) * PC = 4/3 * deltaM, which gives ratio value.
     *
     * @return the bezier curve
     */
    public CubicCurve2D getCurve ()
    {
        if (curve == null) {
            Circle leftCircle = getSideCircle(true);
            Circle rightCircle = getSideCircle(false);

            if ((leftCircle == null) || (rightCircle == null)) {
                ///logger.warn("No side circle");
                return null;
            }

            CubicCurve2D left = leftCircle.getCurve();
            CubicCurve2D right = rightCircle.getCurve();

            if (left == right) {
                curve = left;
            } else {
                double      x1 = left.getX1();
                double      y1 = left.getY1();
                double      cx1 = left.getCtrlX1();
                double      cy1 = left.getCtrlY1();
                double      cx2 = right.getCtrlX2();
                double      cy2 = right.getCtrlY2();
                double      x2 = right.getX2();
                double      y2 = right.getY2();

                // Compute affinity ratio out of mid point translation
                List<Point> points = pointsOf(arcs);
                Point       midPt = points.get(points.size() / 2); // Approximately
                double      mx = (x1 + x2 + (3 * (cx1 + cx2))) / 8;
                double      my = (y1 + y2 + (3 * (cy1 + cy2))) / 8;
                double      deltaM = Math.hypot(midPt.x - mx, midPt.y - my);
                double      pc = Math.hypot(
                    (cx1 + cx2) - (x1 + x2),
                    (cy1 + cy2) - (y1 + y2)) / 2;
                double      ratio = 1 + ((4 * deltaM) / (3 * pc));

                // Apply ratio on vectors to control points
                curve = new CubicCurve2D.Double(
                    x1,
                    y1,
                    x1 + (ratio * (cx1 - x1)), // cx1'
                    y1 + (ratio * (cy1 - y1)), // cy1'
                    x2 + (ratio * (cx2 - x2)), // cx2'
                    y2 + (ratio * (cy2 - y2)), // cy2'
                    x2,
                    y2);
            }
        }

        return curve;
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
     * Report the ending arc on the desired side.
     *
     * @param reverse desired side
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

    //-------//
    // getId //
    //-------//
    /**
     * @return the slur id
     */
    public int getId ()
    {
        return id;
    }

    //-------------//
    // getJunction //
    //-------------//
    /**
     * Report the junction, if any, on the desired side.
     *
     * @param reverse desired side
     * @return the junction point if any, on the desired side
     */
    public Point getJunction (boolean reverse)
    {
        return reverse ? firstJunction : lastJunction;
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
        Point first = getEnd(true);
        Point last = getEnd(false);

        if (first.x < last.x) {
            return (side == LEFT) ? first : last;
        } else {
            return (side == LEFT) ? last : first;
        }
    }

    //---------------//
    // getSideCircle //
    //---------------//
    /**
     * Report the osculatory circle on the desired side.
     * Note that a small slur (a slur with not more than sidelength points)
     * has just one circle which is returned.
     *
     * @param reverse the desired side
     * @return the side circle on desired side
     */
    public Circle getSideCircle (boolean reverse)
    {
        if (reverse) {
            if (firstCircle == null) {
                if (getLength() <= sideLength) {
                    firstCircle = circle;
                } else {
                    firstCircle = computeSideCircle(pointsOf(arcs), reverse);
                }
            }

            return firstCircle;
        } else {
            if (lastCircle == null) {
                if (getLength() <= sideLength) {
                    lastCircle = circle;
                } else {
                    lastCircle = computeSideCircle(pointsOf(arcs), reverse);
                }
            }

            return lastCircle;
        }
    }

    //---------------//
    // getSidePoints //
    //---------------//
    /**
     * Build the sequence of points needed to define a side circle.
     *
     * @param arcs    sequence of arcs
     * @param reverse desired side
     * @return the sequence of defining points
     */
    public List<Point> getSidePoints (List<Arc> arcs,
                                      boolean   reverse)
    {
        Point[] seq = new Point[sideLength];

        //TODO: include inner junction points!
        if (reverse) {
            // Walk foreward
            int n = -1;
            Loop: 
            for (Arc arc : arcs) {
                for (Point point : arc.points) {
                    if (++n < sideLength) {
                        seq[n] = point;
                    } else {
                        break Loop;
                    }
                }
            }
        } else {
            // Walk backward
            int n = sideLength;
            Loop: 
            for (ListIterator<Arc> ita = arcs.listIterator(arcs.size());
                 ita.hasPrevious();) {
                Arc arc = ita.previous();

                for (ListIterator<Point> itp = arc.points.listIterator(
                    arc.points.size()); itp.hasPrevious();) {
                    Point point = itp.previous();

                    if (--n >= 0) {
                        seq[n] = point;
                    } else {
                        break Loop;
                    }
                }
            }
        }

        return Arrays.asList(seq);
    }

    //---------------//
    // hasSideCircle //
    //---------------//
    /**
     * Report whether the slur has an osculatory circle on the desired
     * side.
     *
     * @param reverse desired side
     * @return true if there is indeed a side circle, which is not the global
     *         circle
     */
    public boolean hasSideCircle (boolean reverse)
    {
        if (reverse) {
            return (firstCircle != null) && (firstCircle != circle);
        } else {
            return (lastCircle != null) && (lastCircle != circle);
        }
    }

    //---------//
    // isAbove //
    //---------//
    /**
     * Report whether the slur shape is /--\ rather than \--/.
     *
     * @return the above flag
     */
    public boolean isAbove ()
    {
        return above;
    }

    //--------------//
    // isHorizontal //
    //--------------//
    /**
     * Report whether slur is horizontal (rather than vertical)
     *
     * @return true if horizontal
     */
    public boolean isHorizontal ()
    {
        return horizontal;
    }

    //----------//
    // pointsOf //
    //----------//
    /**
     * Report the sequence of points defined by the slur arcs,
     * prepended or appended by the provided additional arc.
     *
     * @param additionalArc the arc to add to slur
     * @param reverse       desired side
     * @return the sequence of points (arcs & junctions)
     */
    public List<Point> pointsOf (Arc     additionalArc,
                                 boolean reverse)
    {
        return pointsOf(getAllArcs(additionalArc, reverse));
    }

    //----------//
    // pointsOf //
    //----------//
    /**
     * Report the sequence of arc points, including intermediate
     * junction points, from the provided list of arcs.
     *
     * @param arcs source arcs
     * @return the sequence of all defining points, including inner junctions
     *         but excluding outer junctions
     */
    public List<Point> pointsOf (List<Arc> arcs)
    {
        List<Point> allPoints = new ArrayList<Point>();

        for (int i = 0, na = arcs.size(); i < na; i++) {
            Arc arc = arcs.get(i);
            allPoints.addAll(arc.points);

            if ((i < (na - 1)) && (arc.getJunction(false) != null)) {
                allPoints.add(arc.getJunction(false));
            }
        }

        return allPoints;
    }

    //-------------------//
    // removeAttachments //
    //-------------------//
    @Override
    public int removeAttachments (String prefix)
    {
        if (attachments != null) {
            return attachments.removeAttachments(prefix);
        } else {
            return 0;
        }
    }

    //-------------------//
    // renderAttachments //
    //-------------------//
    @Override
    public void renderAttachments (Graphics2D g)
    {
        if (attachments != null) {
            attachments.renderAttachments(g);
        }
    }

    //-------------------//
    // retrieveJunctions //
    //-------------------//
    /**
     * Retrieve both ending junctions for the slur.
     */
    public void retrieveJunctions ()
    {
        // Check orientation of all arcs
        if (arcs.size() > 1) {
            for (int i = 0; i < (arcs.size() - 1); i++) {
                Arc   a0 = arcs.get(i);
                Arc   a1 = arcs.get(i + 1);
                Point common = junctionOf(a0, a1);

                if ((a1.getJunction(false) != null) &&
                    a1.getJunction(false).equals(common)) {
                    a1.reverse();
                }
            }
        }

        firstJunction = arcs.get(0).getJunction(true);
        lastJunction = arcs.get(arcs.size() - 1).getJunction(false);
    }

    //---------//
    // reverse //
    //---------//
    public void reverse ()
    {
        // Reverse junctions
        Point temp = firstJunction;
        firstJunction = lastJunction;
        lastJunction = temp;

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

    //---------//
    // setArea //
    //---------//
    /**
     * @param area    the Area to set
     * @param reverse desired end
     */
    public void setArea (Area    area,
                         boolean reverse)
    {
        if (reverse) {
            firstArea = area;
        } else {
            lastArea = area;
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

    //------------//
    // setExtArea //
    //------------//
    /**
     * Record extension area (to allow slur merge)
     *
     * @param area    the extension area on 'reverse' side
     * @param reverse which end
     */
    public void setExtArea (Area    area,
                            boolean reverse)
    {
        if (reverse) {
            firstExtArea = area;
        } else {
            lastExtArea = area;
        }
    }

    //---------------//
    // setHorizontal //
    //---------------//
    /**
     * @param horizontal the horizontal to set
     */
    public void setHorizontal (boolean horizontal)
    {
        this.horizontal = horizontal;
    }

    //---------------//
    // setSideCircle //
    //---------------//
    public void setSideCircle (Circle  circle,
                               boolean reverse)
    {
        if (reverse) {
            firstCircle = circle;
        } else {
            lastCircle = circle;
        }
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
        boolean ccw = circle.ccw() == 1;
        Line2D  bisector = LineUtil.bisector(getEnd(!ccw), getEnd(ccw));
        double  length = bisector.getP1().distance(bisector.getP2());

        return new Point2D.Double(
            (bisector.getX2() - bisector.getX1()) / length,
            (bisector.getY2() - bisector.getY1()) / length);
    }

    //---------------//
    // getFirstPoint //
    //---------------//
    private Point getFirstPoint ()
    {
        Arc firstArc = getEndArc(true);

        return (firstArc.getLength() > 0) ? firstArc.getEnd(true)
               : firstArc.getJunction(false);
    }

    //--------------//
    // getLastPoint //
    //--------------//
    private Point getLastPoint ()
    {
        Arc lastArc = getEndArc(false);

        return (lastArc.getLength() > 0) ? lastArc.getEnd(false)
               : lastArc.getJunction(true);
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
        s1.add(a1.getJunction(true));
        s1.add(a1.getJunction(false));

        List<Point> s2 = new ArrayList<Point>();
        s2.add(a2.getJunction(true));
        s2.add(a2.getJunction(false));
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
        public static enum ArcShape {
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
        List<Point>   points = new ArrayList<Point>();

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

        //~ Constructors -------------------------------------------------------

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
         * Void arc meant to link two touching junction points, with
         * no points in between.
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
         * @return the sequence of desired points, perhaps limited by the arc
         *         length itself.
         */
        public List<Point> getSidePoints (int     count,
                                          boolean reverse)
        {
            List<Point> seq = new ArrayList<Point>();
            int         n = 0;

            if (reverse) {
                for (Point p : points) {
                    if (++n > count) {
                        return seq;
                    }

                    seq.add(p);
                }
            } else {
                for (ListIterator<Point> itp = points.listIterator(
                    points.size()); itp.hasPrevious();) {
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
         * Set a junction point.
         *
         * @param junction the point to set
         * @param reverse  desired side
         */
        public void setJunction (Point   junction,
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
    }
}
