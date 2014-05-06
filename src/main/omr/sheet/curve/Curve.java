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

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphComposition;
import omr.glyph.ui.AttachmentHolder;
import omr.glyph.ui.BasicAttachmentHolder;

import omr.grid.FilamentLine;

import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;

import omr.run.Run;
import omr.run.RunsTable;

import omr.sheet.Sheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
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

    private static final Logger logger = LoggerFactory.getLogger(Curve.class);

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

    /** Underlying glyph that compose the curve. */
    protected Glyph glyph;

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
        Point curveJunction = getJunction(rev);

        if (curveJunction != null) {
            // Curve ending at pivot junction
            if ((arc.getJunction(rev) != null) && arc.getJunction(rev).equals(curveJunction)) {
                return new ArcView(arc, true);
            }
        } else {
            // Curve with free ending (or artificial pivot), use shortest distance
            Point curveEnd = getEnd(rev);

            Point arcEnd = arc.getEnd(rev);

            if (arcEnd != null) {
                if (curveEnd.distanceSq(arcEnd) < curveEnd.distanceSq(arc.getEnd(!rev))) {
                    return new ArcView(arc, true);
                }
            } else {
                Point arcP1 = arc.getJunction(rev);
                Point arcP2 = arc.getJunction(!rev);

                if ((arcP1 != null) && (arcP2 != null)) {
                    if (curveEnd.distanceSq(arcP1) < curveEnd.distanceSq(arcP2)) {
                        return new ArcView(arc, true);
                    }
                }
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

    //----------//
    // getGlyph //
    //----------//
    /**
     * Report the underlying glyph, if any.
     *
     * @return the underlying glyph (with relevant runs only)
     */
    public Glyph getGlyph ()
    {
        return glyph;
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

    //---------------//
    // retrieveGlyph //
    //---------------//
    /**
     * Retrieve the underlying glyph of a curve.
     * Based on each point of curve sequence of points, we find the containing run (either
     * horizontal or vertical), then the containing section.
     * <p>
     * However, we have to check that the containing section is actually compatible with the curve,
     * to avoid picking up sections that cannot be part of the curve.
     * Compatibility is checked for all vertices of section polygon.
     *
     * @param sheet          the containing sheet
     * @param maxRunDistance maximum distance from any section vertex to curve points
     * @return the curve glyph
     */
    public Glyph retrieveGlyph (Sheet sheet,
                                double maxRunDistance)
    {
        RunsTable table = sheet.getWholeVerticalTable();
        Lag hLag = sheet.getLag(Lags.HLAG);
        Lag vLag = sheet.getLag(Lags.VLAG);
        Set<Section> sectionsIn = new HashSet<Section>();
        Set<Section> sectionsOut = new HashSet<Section>();

        for (int index = 0; index < points.size(); index++) {
            Point point = points.get(index);
            Run wholeRun = table.getRunAt(point.x, point.y);

            for (int y = wholeRun.getStart(); y <= wholeRun.getStop(); y++) {
                Run run = hLag.getRunAt(point.x, y);

                if (run == null) {
                    run = vLag.getRunAt(point.x, y);
                }

                if (run != null) {
                    Section section = run.getSection();

                    if (section != null) {
                        if (!sectionsIn.contains(section) && !sectionsOut.contains(section)) {
                            if (isCloseToCurve(section, maxRunDistance, index)) {
                                sectionsIn.add(section);
                            } else {
                                sectionsOut.add(section);
                            }
                        }
                    }
                }
            }
        }

        if (!sectionsIn.isEmpty()) {
            GlyphNest nest = sheet.getNest();
            Glyph curveGlyph = nest.buildGlyph(
                    sectionsIn,
                    GlyphLayer.DEFAULT,
                    true,
                    GlyphComposition.Linking.NO_LINK);
            logger.debug("{} -> {}", this, curveGlyph);

            setGlyph(curveGlyph);
            return curveGlyph;
        } else {
            logger.debug("{} -> no glyph", this);

            return null;
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

    //----------//
    // setGlyph //
    //----------//
    /**
     * Assign the underlying glyph
     *
     * @param glyph the underlying glyph (with relevant runs only)
     */
    public void setGlyph (Glyph glyph)
    {
        this.glyph = glyph;
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

    //----------------//
    // isCloseToCurve //
    //----------------//
    /**
     * Check whether all run end points of the provided section are close enough to curve.
     *
     * @param section        the section to check
     * @param maxRunDistance maximum acceptable distance from a run end to nearest curve point
     * @param index          current position in points sequence
     * @return true for OK
     */
    private boolean isCloseToCurve (Section section,
                                    double maxRunDistance,
                                    int index)
    {
        final double maxD2 = maxRunDistance * maxRunDistance;
        final Polygon poly = section.getPolygon();

        for (int i = 0; i < poly.npoints; i++) {
            int x = poly.xpoints[i];
            int y = poly.ypoints[i];
            boolean close = false;

            for (int ip = index; ip < points.size(); ip++) {
                Point point = points.get(ip);
                double dx = point.x - x;
                double dy = point.y - y;
                double d2 = (dx * dx) + (dy * dy);

                if (d2 <= maxD2) {
                    close = true;

                    break;
                }
            }

            if (!close) {
                for (int ip = index - 1; ip >= 0; ip--) {
                    Point point = points.get(ip);
                    double dx = point.x - x;
                    double dy = point.y - y;
                    double d2 = (dx * dx) + (dy * dy);

                    if (d2 <= maxD2) {
                        close = true;

                        break;
                    }
                }
            }

            if (!close) {
                return false;
            }
        }

        return true;
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
