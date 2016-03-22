//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           C u r v e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import omr.glyph.BasicGlyph;
import omr.glyph.Glyph;
import static omr.run.Orientation.VERTICAL;
import omr.run.Run;
import omr.run.RunTable;

import omr.sheet.Picture;
import omr.sheet.Sheet;

import omr.ui.util.AttachmentHolder;
import omr.ui.util.BasicAttachmentHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** Arcs that compose this curve. */
    private final Set<Arc> parts = new HashSet<Arc>();

    /** Area for extension on first side. */
    protected Area firstExtArea;

    /** Area for extension on last side. */
    protected Area lastExtArea;

    /** Potential attachments, lazily allocated. */
    private AttachmentHolder attachments;

    /** Underlying glyph made of relevant runs. (generally thicker than the curve line) */
    protected Glyph glyph;

    /** Bounds of points that compose the curve line. */
    protected Rectangle bounds;

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
        if (bounds == null) {
            int xMin = Integer.MAX_VALUE;
            int xMax = Integer.MIN_VALUE;
            int yMin = Integer.MAX_VALUE;
            int yMax = Integer.MIN_VALUE;

            for (Point p : points) {
                final int x = p.x;

                if (x < xMin) {
                    xMin = x;
                }

                if (x > xMax) {
                    xMax = x;
                }

                final int y = p.y;

                if (y < yMin) {
                    yMin = y;
                }

                if (y > yMax) {
                    yMax = y;
                }
            }

            bounds = new Rectangle(xMin, yMin, xMax - xMin + 1, yMax - yMin + 1);
        }

        return new Rectangle(bounds);
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

    //-----------//
    // getPartAt //
    //-----------//
    /**
     * Report the part that contains the provided point.
     *
     * @param point the provided point
     * @return the part arc that contains the point, or null
     */
    public Arc getPartAt (Point point)
    {
        for (Arc arc : parts) {
            for (Point p : arc.getPoints()) {
                if (p.equals(point)) {
                    return arc;
                }
            }
        }

        return null;
    }

    /**
     * @return the parts
     */
    public Set<Arc> getParts ()
    {
        return parts;
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

    //---------------//
    // retrieveGlyph //
    //---------------//
    /**
     * Retrieve the underlying glyph of a curve.
     * <p>
     * This method works with runs rather than predefined sections.
     * Based on each point of curve sequence of points, we find the containing vertical run, and
     * check whether the run extrema remain close enough to curve.
     * If not, the run is not considered as part of the curve.
     * <p>
     * With accepted runs, we build (sections then) the whole glyph.
     *
     * @param sheet          the containing sheet
     * @param maxRunDistance maximum acceptable distance from any run extrema to curve points
     * @return the curve glyph, or null
     */
    public Glyph retrieveGlyph (Sheet sheet,
                                double maxRunDistance)
    {
        // Sheet global vertical run table
        RunTable sheetTable = sheet.getPicture().getTable(Picture.TableKey.BINARY);

        // Allocate a curve run table with proper dimension
        Rectangle fatBox = getBounds();
        fatBox.grow(0, (int) Math.ceil(maxRunDistance)); // Slight extension above & below

        if (fatBox.y < 0) {
            fatBox.height += fatBox.y;
            fatBox.y = 0;
        }

        RunTable curveTable = new RunTable(VERTICAL, fatBox.width, fatBox.height);

        // Populate the curve run table
        for (int index = 0; index < points.size(); index++) {
            final Point point = points.get(index); // Point of curve
            final Run run = sheetTable.getRunAt(point.x, point.y); // Containing run

            if (isCloseToCurve(point.x, run.getStart(), maxRunDistance, index)
                && ((run.getLength() <= 1)
                    || isCloseToCurve(point.x, run.getStop(), maxRunDistance, index))) {
                curveTable.addRun(point.x - fatBox.x, run.getStart() - fatBox.y, run.getLength());
            }
        }

        // Build glyph (TODO: table a bit too high, should be trimmed?)
        if (curveTable.getSize() > 0) {
            Glyph curveGlyph = sheet.getGlyphIndex().registerOriginal(
                    new BasicGlyph(fatBox.x, fatBox.y, curveTable));
            setGlyph(curveGlyph);
            logger.debug("{} -> {}", this, curveGlyph);

            return curveGlyph;
        } else {
            logger.debug("{} -> no glyph", this);

            return null;
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
     * Check whether the provided coordinates are close enough to curve.
     *
     * @param x              point abscissa
     * @param y              point ordinate
     * @param maxRunDistance maximum acceptable distance from a run end to nearest curve point
     * @param index          current position in points sequence
     * @return true for OK
     */
    private boolean isCloseToCurve (int x,
                                    int y,
                                    double maxRunDistance,
                                    int index)
    {
        final double maxD2 = maxRunDistance * maxRunDistance;

        for (int ip = index; ip < points.size(); ip++) {
            Point point = points.get(ip);
            double dx = point.x - x;
            double dy = point.y - y;
            double d2 = (dx * dx) + (dy * dy);

            if (d2 <= maxD2) {
                return true;
            }
        }

        for (int ip = index - 1; ip >= 0; ip--) {
            Point point = points.get(ip);
            double dx = point.x - x;
            double dy = point.y - y;
            double d2 = (dx * dx) + (dy * dy);

            if (d2 <= maxD2) {
                return true;
            }
        }

        return false;
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
