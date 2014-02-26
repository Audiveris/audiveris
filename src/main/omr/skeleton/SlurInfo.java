//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S l u r I n f o                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.skeleton;

import omr.glyph.ui.AttachmentHolder;
import omr.glyph.ui.BasicAttachmentHolder;

import omr.grid.FilamentLine;

import omr.math.Circle;
import omr.math.LineUtil;

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
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Class {@code SlursInfo} gathers physical description of a slur.
 * <p>
 * Short and medium slurs generally fit a global circle rather well.
 * But a long slur may be closer to an ellipsis, hence the use of local osculatory circles, one at
 * the start and one at the end of slur, in order to more accurately evaluate slur extensions.
 *
 * @author Hervé Bitteur
 */
public class SlurInfo
        implements AttachmentHolder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SlurInfo.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Unique slur id. (within containing page) */
    private final int id;

    /** Sequence of arcs that compose this slur. */
    private final List<Arc> arcs = new ArrayList<Arc>();

    /** Approximating global model. */
    private Model globalModel;

    /** Approximating first side model. */
    private Model firstModel;

    /** Approximating last side model. */
    private Model lastModel;

    /** Number of points for side circles. */
    private final int sideLength;

    /** Above heads, below heads or flat. */
    private int above;

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

    /** Global Bézier curve for slur. */
    private CubicCurve2D curve;

    /** Potential attachments, lazily allocated. */
    private AttachmentHolder attachments;

    /** Staff line most recently crossed. */
    private FilamentLine crossedLine;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SlurInfo object.
     *
     * @param id         slur id
     * @param arcs       The sequence of arcs for this slur
     * @param model      The approximating model, perhaps null
     * @param sideLength length of side circles
     */
    public SlurInfo (int id,
                     List<Arc> arcs,
                     Model model,
                     int sideLength)
    {
        this.id = id;
        this.arcs.addAll(arcs);
        setModel(model);
        this.sideLength = sideLength;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // isAbove //
    //---------//
    /**
     * Report whether the slur shape is /--\ rather than \--/.
     *
     * @return the above flag
     */
    public int above ()
    {
        return above;
    }

    //---------------//
    // addAttachment //
    //---------------//
    @Override
    public void addAttachment (String id,
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
    public void checkArcOrientation (Arc arc,
                                     boolean reverse)
    {
        if (reverse) {
            // Normal orientation, check at slur start
            if (firstJunction != null) {
                if ((arc.getJunction(true) != null) && arc.getJunction(true).equals(firstJunction)) {
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
            if (lastJunction != null) {
                // Slur ending at pivot
                if ((arc.getJunction(false) != null)
                    && arc.getJunction(false).equals(lastJunction)) {
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

    //------------------//
    // computeSideModel //
    //------------------//
    /**
     * Compute a side model (on side designated by reverse) out of the arcs sequence.
     *
     * @param points  the full sequence of points
     * @param reverse desired side
     * @return the side model, or null if unsuccessful
     */
    public Model computeSideModel (List<Point> points,
                                   boolean reverse)
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

        Point p0 = points.get(0);
        Point p1 = points.get(np / 2);
        Point p2 = points.get(np - 1);

        // Choose a circle-model, otherwise a line-model
        CircleModel rough = CircleModel.create(p0, p1, p2);

        if (rough != null) {
            return rough;
        } else {
            return new LineModel(points);
        }
    }

    //------------//
    // getAllArcs //
    //------------//
    /**
     * Report the sequence of slur arcs, augmented by the provided arc.
     *
     * @param arc     additional arc
     * @param reverse desired side
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
    public Point getBackupPoint (int count,
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
            for (ListIterator<Arc> it = arcs.listIterator(arcs.size()); it.hasPrevious();) {
                Arc arc = it.previous();

                for (ListIterator<Point> itp = arc.points.listIterator(arc.points.size());
                        itp.hasPrevious();) {
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
        return globalModel.getCurve().getBounds();
    }

    /**
     * @return the last crossed Line
     */
    public FilamentLine getCrossedLine ()
    {
        return crossedLine;
    }

    //----------//
    // getCurve //
    //----------//
    /**
     * Report the left-to-right Bézier curve which best approximates the slur.
     * <p>
     * It is built by combining the left half (point & control point) of left circle curve and the
     * right half (control point & point) of right circle curve.
     * Vectors from point to related control point are applied a ratio extension so that curve
     * middle point (M) fits on slur middle point (M').
     * We apply the same ratio on both vectors, which may not be the best choice but that's enough
     * for a first version.
     * On a bezier curve, naming P the middle point of segment (P1,P2) and C the middle point of
     * segment (CP1,CP2), we always have vector PC = 4/3 of vector PM.
     * So, (PC' - PC) = 4/3 (PM' - PM) or (ratio - 1) * PC = 4/3 * deltaM, which gives ratio value.
     *
     * @return the bezier curve
     */
    public CubicCurve2D getCurve ()
    {
        if (curve == null) {
            Model leftModel = getSideModel(true);
            Model rightModel = getSideModel(false);

            if ((leftModel == null) || (rightModel == null)) {
                ///logger.warn("No side circle");
                return null;
            }

            // Assume we have circle models on both ends
            if (!(leftModel instanceof CircleModel) || !(leftModel instanceof CircleModel)) {
                return null;
            }

            CubicCurve2D left = (CubicCurve2D) leftModel.getCurve();
            CubicCurve2D right = (CubicCurve2D) rightModel.getCurve();

            if (left == right) {
                curve = left;
            } else {
                double x1 = left.getX1();
                double y1 = left.getY1();
                double cx1 = left.getCtrlX1();
                double cy1 = left.getCtrlY1();
                double cx2 = right.getCtrlX2();
                double cy2 = right.getCtrlY2();
                double x2 = right.getX2();
                double y2 = right.getY2();

                // Compute affinity ratio out of mid point translation
                List<Point> points = pointsOf(arcs);
                Point midPt = points.get(points.size() / 2); // Approximately
                double mx = (x1 + x2 + (3 * (cx1 + cx2))) / 8;
                double my = (y1 + y2 + (3 * (cy1 + cy2))) / 8;
                double deltaM = Math.hypot(midPt.x - mx, midPt.y - my);
                double pc = Math.hypot((cx1 + cx2) - (x1 + x2), (cy1 + cy2) - (y1 + y2)) / 2;
                double ratio = 1 + ((4 * deltaM) / (3 * pc));

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

        //        if (curve == null) {
        //            // Pickup the 4 points at t = 0, 1/3, 2/3 & 1
        //            double r = 0.28;
        //            List<Point> points = pointsOf(arcs);
        //            int         n = points.size();
        //            int ir = (int) Math.rint(r*n);
        //            Point       s0 = points.get(0);
        //            //Point       s1 = points.get(n / 3); // Wrong!
        //            Point       s1 = points.get(ir); // bof!
        //            //Point       s2 = points.get((2 * n) / 3); // Wrong!
        //            Point       s2 = points.get(n-1-ir); // bof!
        //            Point       s3 = points.get(n - 1);
        //            curve = new CubicCurve2D.Double(
        //                s0.x,
        //                s0.y,
        //                (-5 * s0.x + 18 * s1.x - 9 * s2.x + 2 * s3.x) / 6,
        //                (-5 * s0.y + 18 * s1.y - 9 * s2.y + 2 * s3.y) / 6,
        //                (2 * s0.x - 9 * s1.x + 18 * s2.x - 5 * s3.x) / 6,
        //                (2 * s0.y - 9 * s1.y + 18 * s2.y - 5 * s3.y) / 6,
        //                s3.x,
        //                s3.y);
        //        }
        //
        //        return curve;
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

    /**
     * @return the global model
     */
    public Model getGlobalModel ()
    {
        return globalModel;
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
        return globalModel.getMidPoint();
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

    //--------------//
    // getSideModel //
    //--------------//
    /**
     * Report the osculatory model on the desired side.
     * Note that a small slur (a slur with not more than sideLength points) has just one global
     * model which is returned.
     *
     * @param reverse the desired side
     * @return the side model on desired side
     */
    public Model getSideModel (boolean reverse)
    {
        if (reverse) {
            if (firstModel == null) {
                if (getLength() <= sideLength) {
                    firstModel = globalModel;
                } else {
                    firstModel = computeSideModel(pointsOf(arcs), reverse);
                }
            }

            return firstModel;
        } else {
            if (lastModel == null) {
                if (getLength() <= sideLength) {
                    lastModel = globalModel;
                } else {
                    lastModel = computeSideModel(pointsOf(arcs), reverse);
                }
            }

            return lastModel;
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
                                      boolean reverse)
    {
        List<Point> seq = new ArrayList<Point>();

        //TODO: include inner junction points!
        if (reverse) {
            // Walk foreward
            int n = -1;
            Loop:
            for (Arc arc : arcs) {
                for (Point point : arc.points) {
                    if (++n < sideLength) {
                        seq.add(point);
                    } else {
                        break Loop;
                    }
                }
            }
        } else {
            // Walk backward
            int n = sideLength;
            Loop:
            for (ListIterator<Arc> ita = arcs.listIterator(arcs.size()); ita.hasPrevious();) {
                Arc arc = ita.previous();

                for (ListIterator<Point> itp = arc.points.listIterator(arc.points.size());
                        itp.hasPrevious();) {
                    Point point = itp.previous();

                    if (--n >= 0) {
                        seq.add(0, point);
                    } else {
                        break Loop;
                    }
                }
            }
        }

        return seq;
    }

    //--------------//
    // hasSideModel //
    //--------------//
    /**
     * Report whether the slur has a specific model on the desired side.
     *
     * @param reverse desired side
     * @return true if there is indeed a side model, which is not the global one
     */
    public boolean hasSideModel (boolean reverse)
    {
        if (reverse) {
            return (firstModel != null) && (firstModel != globalModel);
        } else {
            return (lastModel != null) && (lastModel != globalModel);
        }
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
     * Report the sequence of points defined by the slur arcs, prepended or appended by
     * the provided additional arc.
     *
     * @param additionalArc the arc to add to slur
     * @param reverse       desired side
     * @return the sequence of points (arcs & junctions)
     */
    public List<Point> pointsOf (Arc additionalArc,
                                 boolean reverse)
    {
        return pointsOf(getAllArcs(additionalArc, reverse));
    }

    //----------//
    // pointsOf //
    //----------//
    /**
     * Report the sequence of arc points, including intermediate junction points, from
     * the provided list of arcs.
     *
     * @param arcs source arcs
     * @return the sequence of all defining points, including inner junctions but excluding outer
     *         junctions
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
                Arc a0 = arcs.get(i);
                Arc a1 = arcs.get(i + 1);
                Point common = junctionOf(a0, a1);

                if ((a1.getJunction(false) != null) && a1.getJunction(false).equals(common)) {
                    a1.reverse();
                }
            }
        }

        firstJunction = arcs.get(0).getJunction(true);
        lastJunction = arcs.get(arcs.size() - 1).getJunction(false);
    }

    //    //---------//
    //    // reverse //
    //    //---------//
    //    public void reverse ()
    //    {
    //        // Reverse junctions
    //        Point temp = firstJunction;
    //        firstJunction = lastJunction;
    //        lastJunction = temp;
    //
    //        // Reverse arc list
    //        List<Arc> rev = new ArrayList<Arc>(arcs.size());
    //
    //        for (ListIterator<Arc> it = arcs.listIterator(arcs.size()); it.hasPrevious();) {
    //            rev.add(it.previous());
    //        }
    //
    //        arcs.clear();
    //        arcs.addAll(rev);
    //
    //        // Reverse circle
    //        if (globalModel != null) {
    //            globalModel.reverse();
    //        }
    //    }
    //
    //---------//
    // setArea //
    //---------//
    /**
     * @param area    the Area to set
     * @param reverse desired end
     */
    public void setArea (Area area,
                         boolean reverse)
    {
        if (reverse) {
            firstArea = area;
        } else {
            lastArea = area;
        }
    }

    /**
     * @param crossedLine the last crossed Line to set
     */
    public void setCrossedLine (FilamentLine crossedLine)
    {
        this.crossedLine = crossedLine;
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
    public void setExtArea (Area area,
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

    //----------//
    // setModel //
    //----------//
    public void setModel (Model model)
    {
        if (model != null) {
            globalModel = model; // TODO: Should we replicate the model?
            above = model.above();
            bisUnit = computeBisector();
        }
    }

    //---------------//
    // setSideModel //
    //---------------//
    public void setSideModel (Model model,
                              boolean reverse)
    {
        if (reverse) {
            firstModel = model;
        } else {
            lastModel = model;
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

        if (globalModel != null) {
            sb.append(String.format(" dist:%.3f", globalModel.getDistance()));
        }

        boolean first = true;

        for (Arc arc : arcs) {
            if (first) {
                first = false;
            } else {
                Point j = arc.getJunction(true);

                if (j != null) {
                    sb.append(" <").append(j.x).append(",").append(j.y).append(">");
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
        boolean ccw = globalModel.ccw() == 1;
        Line2D bisector = LineUtil.bisector(getEnd(!ccw), getEnd(ccw));
        double length = bisector.getP1().distance(bisector.getP2());

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

        return (firstArc.getLength() > 0) ? firstArc.getEnd(true) : firstArc.getJunction(false);
    }

    //--------------//
    // getLastPoint //
    //--------------//
    private Point getLastPoint ()
    {
        Arc lastArc = getEndArc(false);

        return (lastArc.getLength() > 0) ? lastArc.getEnd(false) : lastArc.getJunction(true);
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
}
