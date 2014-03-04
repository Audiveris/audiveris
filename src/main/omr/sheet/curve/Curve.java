//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C u r v e I n f o                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import omr.glyph.ui.AttachmentHolder;
import omr.glyph.ui.BasicAttachmentHolder;

import omr.grid.FilamentLine;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code Curve} is a shaped curve built from arcs of points, with the ability
 * to merge instances into larger ones according to underlying shape model(s).
 *
 * @author Hervé Bitteur
 */
public abstract class Curve
        extends Arc
        implements AttachmentHolder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Comparison by left abscissa. */
    public static final Comparator<Curve> byLeftAbscissa = new Comparator<Curve>()
    {
        @Override
        public int compare (Curve c1,
                            Curve c2)
        {
            return Integer.compare(c1.getEnd(true).x, c2.getEnd(true).x);
        }
    };

    /** Comparison by right abscissa. */
    public static final Comparator<Curve> byRightAbscissa = new Comparator<Curve>()
    {
        @Override
        public int compare (Curve c1,
                            Curve c2)
        {
            return Integer.compare(c1.getEnd(false).x, c2.getEnd(false).x);
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Unique id. (within containing sheet) */
    protected final int id;

    /** Arcs that compose this curve. (Needed only when setting the arcs as assigned) */
    private final Set<Arc> parts = new HashSet<Arc>();

    /** Area for extension on first side. */
    protected Area firstExtArea;

    /** Area for extension on last side. */
    protected Area lastExtArea;

    /** Potential attachments, lazily allocated. */
    private AttachmentHolder attachments;

    /** Staff line most recently crossed, if any. */
    private FilamentLine crossedLine;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Curve object.
     *
     * @param id            curve id
     * @param firstJunction first junction point, if any
     * @param lastJunction  second junction point, if any
     * @param points        sequence of defining points
     * @param model         underlying model, if any
     * @param parts         all arcs used for this curve
     */
    public Curve (int id,
                  Point firstJunction,
                  Point lastJunction,
                  List<Point> points,
                  Model model,
                  Collection<Arc> parts)
    {
        super(firstJunction, lastJunction, points, model);
        this.id = id;
        this.parts.addAll(parts);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------------//
    // getAbscissaComparator //
    //-----------------------//
    /**
     * Report a comparator on abscissa
     *
     * @param reverse which side is concerned
     * @return the comparator ready for use
     */
    public static Comparator<Curve> getAbscissaComparator (final boolean reverse)
    {
        return new Comparator<Curve>()
        {
            @Override
            public int compare (Curve a1,
                                Curve a2)
            {
                return Integer.compare(a1.getEnd(reverse).x, a2.getEnd(reverse).x);
            }
        };
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
     * Flag the sequence arcs as assigned.
     */
    public void assign ()
    {
        for (Arc part : parts) {
            part.setAssigned(true);
        }
    }

    //    //------------//
    //    // getAllArcs //
    //    //------------//
    //    /**
    //     * Report the sequence of sequence arcs, augmented by the provided arc.
    //     *
    //     * @param arc     additional arc
    //     * @param reverse desired side
    //     * @return proper sequence of all arcs
    //     */
    //    public List<Arc> getAllArcs (Arc arc,
    //                                 boolean reverse)
    //    {
    //        List<Arc> allArcs = new ArrayList<Arc>(parts);
    //
    //        if (reverse) {
    //            allArcs.add(0, arc);
    //        } else {
    //            allArcs.add(arc);
    //        }
    //
    //        return allArcs;
    //    }
    //--------------//
    // getAllPoints //
    //--------------//
    /**
     * Report the sequence of all points, augmented by the provided extension arc, and
     * the inner junction point if any.
     *
     * @param arcView properly oriented view on extension arc
     * @param reverse side for extension
     * @return the sequence of all points
     */
    public List<Point> getAllPoints (ArcView arcView,
                                     boolean reverse)
    {
        List<Point> pts = new ArrayList<Point>();

        if (reverse) {
            pts.addAll(arcView.getPoints());
        } else {
            pts.addAll(points);
        }

        Point junction = arcView.getJunction(!reverse);

        if (junction != null) {
            pts.add(junction);
        }

        if (reverse) {
            pts.addAll(points);
        } else {
            pts.addAll(arcView.getPoints());
        }

        return pts;
    }

    //------------//
    // getArcView //
    //------------//
    /**
     * Return a view of extension arc which is compatible with curve.
     * (Normal) orientation : CURVE - ARC : (arc start cannot be curve start)
     * Reverse. orientation : ARC - CURVE : (arc stop cannot be curve stop)
     *
     * @param arc the arc to check
     * @param rev side for slur extension
     * @return the proper view on extension arc
     */
    public ArcView getArcView (Arc arc,
                               boolean rev)
    {
        //        if (reverse) {
        //            // Normal orientation, check at curve start
        //            if (getJunction(true) != null) {
        //                if ((arc.getJunction(true) != null)
        //                    && arc.getJunction(true).equals(getJunction(true))) {
        //                    return new ArcView(arc, true);
        //                }
        //            } else {
        //                // Curve with free ending, use shortest distance
        //                Point slurEnd = getEnd(reverse);
        //                double toStart = slurEnd.distanceSq(arc.getEnd(true));
        //                double toStop = slurEnd.distanceSq(arc.getEnd(false));
        //
        //                if (toStart < toStop) {
        //                    return new ArcView(arc, true);
        //                }
        //            }
        //        } else {
        //            // Normal orientation, check at curve stop
        //            if (getJunction(false) != null) {
        //                // Curve ending at pivot
        //                if ((arc.getJunction(false) != null)
        //                    && arc.getJunction(false).equals(getJunction(false))) {
        //                    return new ArcView(arc, true);
        //                }
        //            } else {
        //                // Curve with free ending, use shortest distance
        //                Point slurEnd = getEnd(reverse);
        //                double toStart = slurEnd.distanceSq(arc.getEnd(true));
        //                double toStop = slurEnd.distanceSq(arc.getEnd(false));
        //
        //                if (toStop < toStart) {
        //                    return new ArcView(arc, true);
        //                }
        //            }
        //        }
        Point curveJunction = getJunction(rev);

        if (curveJunction != null) {
            // Curve ending at pivot junction
            if ((arc.getJunction(rev) != null) && arc.getJunction(rev).equals(curveJunction)) {
                return new ArcView(arc, true);
            }
        } else {
            // Curve with free ending, use shortest distance
            Point curveEnd = getEnd(rev);

            if (curveEnd.distanceSq(arc.getEnd(rev)) < curveEnd.distanceSq(arc.getEnd(!rev))) {
                return new ArcView(arc, true);
            }
        }

        // Use a direct view
        return new ArcView(arc, false);
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

    //-----------//
    // getBounds //
    //-----------//
    public Rectangle getBounds ()
    {
        return getModel().getBounds();
    }

    /**
     * @return the last crossed Line
     */
    public FilamentLine getCrossedLine ()
    {
        return crossedLine;
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
     * @return the sequence id
     */
    public int getId ()
    {
        return id;
    }

    /**
     * @return the parts
     */
    public Set<Arc> getParts ()
    {
        return parts;
    }

    //
    //    //----------//
    //    // pointsOf //
    //    //----------//
    //    /**
    //     * Report the list of points, prepended or appended by the provided additional arc.
    //     *
    //     * @param additionalArc the arc to add to curve
    //     * @param reverse       desired side
    //     * @return the sequence of points (points and inner junctions)
    //     */
    //    public List<Point> pointsOf (Arc additionalArc,
    //                                 boolean reverse)
    //    {
    //        return pointsOf(getAllArcs(additionalArc, reverse));
    //    }
    //
    //    //----------//
    //    // pointsOf //
    //    //----------//
    //    /**
    //     * Report the sequence of arc points, including intermediate junction points, from
    //     * the provided list of arcs.
    //     *
    //     * @param arcs source arcs
    //     * @return the sequence of all defining points, including inner junctions but excluding
    //     *         outer
    //     *         junctions
    //     */
    //    public List<Point> pointsOf (List<Arc> arcs)
    //    {
    //        List<Point> allPoints = new ArrayList<Point>();
    //
    //        for (int i = 0, na = arcs.size(); i < na; i++) {
    //            Arc arc = arcs.get(i);
    //            allPoints.addAll(arc.getPoints());
    //
    //            if ((i < (na - 1)) && (arc.getJunction(false) != null)) {
    //                allPoints.add(arc.getJunction(false));
    //            }
    //        }
    //
    //        return allPoints;
    //    }
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

    //    //-------------------//
    //    // retrieveJunctions //
    //    //-------------------//
    //    /**
    //     * Retrieve both ending junctions for the sequence.
    //     */
    //    public void retrieveJunctions ()
    //    {
    //        // Check orientation of all arcs
    //        if (parts.size() > 1) {
    //            for (int i = 0; i < (parts.size() - 1); i++) {
    //                Arc a0 = parts.get(i);
    //                Arc a1 = parts.get(i + 1);
    //                Point common = junctionOf(a0, a1);
    //
    //                if ((a1.getJunction(false) != null) && a1.getJunction(false).equals(common)) {
    //                    a1.reverse();
    //                }
    //            }
    //        }
    //
    //        firstJunction = parts.get(0).getJunction(true);
    //        lastJunction = parts.get(parts.size() - 1).getJunction(false);
    //    }
    //
    //----------------//
    // setCrossedLine //
    //----------------//
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
//
//    //----------//
//    // toString //
//    //----------//
//    @Override
//    public String toString ()
//    {
//        StringBuilder sb = new StringBuilder();
//        sb.append(getClass().getSimpleName());
//
//        boolean first = true;
//
//        for (Arc arc : parts) {
//            if (first) {
//                first = false;
//            } else {
//                Point j = arc.getJunction(true);
//
//                if (j != null) {
//                    sb.append(" <").append(j.x).append(",").append(j.y).append(">");
//                }
//            }
//
//            sb.append(" ").append(arc);
//        }
//
//        sb.append(internals());
//
//        sb.append("}");
//
//        return sb.toString();
//    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(id);
        sb.append(super.internals());

        return sb.toString();
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
