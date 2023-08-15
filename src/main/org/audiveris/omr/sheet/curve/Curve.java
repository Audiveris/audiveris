//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           C u r v e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.util.AttachmentHolder;
import org.audiveris.omr.ui.util.BasicAttachmentHolder;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Class <code>Curve</code> is a shaped curve built from arcs of points, with the ability
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
    public static final Comparator<Curve> byLeftAbscissa = (c1,
                                                            c2) -> Integer.compare(
                                                                    c1.getEnd(true).x,
                                                                    c2.getEnd(true).x);

    /** Comparison by right abscissa. */
    public static final Comparator<Curve> byRightAbscissa = (c1,
                                                             c2) -> Integer.compare(
                                                                     c1.getEnd(false).x,
                                                                     c2.getEnd(false).x);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Unique id. (within containing sheet) */
    protected final int id;

    /** Area for extension on first side. */
    protected Area firstExtArea;

    /** Area for extension on last side. */
    protected Area lastExtArea;

    /** Underlying glyph made of relevant runs. (generally thicker than the curve line) */
    protected Glyph glyph;

    /** Bounds of points that compose the curve line. */
    protected Rectangle bounds;

    /** Arcs that compose this curve. */
    private final Set<Arc> parts = new LinkedHashSet<>();

    /** Potential attachments, lazily allocated. */
    private AttachmentHolder attachments;

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
        Objects.requireNonNull(attachment, "Adding a null attachment");

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
        List<Point> pts = new ArrayList<>();

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
    /**
     * Report the bounding box for this curve.
     *
     * @return bounding box
     */
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
    /**
     * Report the extension lookup area on provided side.
     *
     * @param reverse desired direction
     * @return lookup area
     */
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
        List<Point> s1 = new ArrayList<>();
        s1.add(a1.getJunction(true));
        s1.add(a1.getJunction(false));

        List<Point> s2 = new ArrayList<>();
        s2.add(a2.getJunction(true));
        s2.add(a2.getJunction(false));
        s1.retainAll(s2);

        if (!s1.isEmpty()) {
            return s1.get(0);
        } else {
            return null;
        }
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
     * This method is based on runs rather than predefined sections.
     * Based on each point of curve sequence of points, we find the containing vertical run, and
     * check whether the run extrema remain close enough to curve.
     * If not, the run is not considered as part of the curve.
     * <p>
     * With accepted runs, we build (sections then) the whole glyph.
     *
     * @param sheet          the containing sheet
     * @param maxRunDistance maximum acceptable distance from any run extrema to curve points
     * @param minRunRatio    minimum count of runs with respect to curve width
     * @return the curve glyph, or null
     */
    public Glyph retrieveGlyph (Sheet sheet,
                                double maxRunDistance,
                                double minRunRatio)
    {
        // Sheet global vertical run table
        final RunTable sheetTable = sheet.getPicture().getTable(Picture.TableKey.BINARY);

        // Allocate a curve run table with proper dimension
        final Rectangle fatBox = getBounds();
        final int curveWidth = fatBox.width;
        fatBox.grow(0, (int) Math.ceil(maxRunDistance)); // Slight extension above & below

        if (fatBox.y < 0) {
            fatBox.height += fatBox.y;
            fatBox.y = 0;
        }

        final RunTable curveTable = new RunTable(VERTICAL, fatBox.width, fatBox.height);

        // Populate the curve run table
        for (int index = 0; index < points.size(); index++) {
            final Point point = points.get(index); // Point of curve
            final Run run = sheetTable.getRunAt(point.x, point.y); // Containing run

            // @formatter:off
            if (isCloseToCurve(point.x, run.getStart(), maxRunDistance, index)
                && ((run.getLength() <= 1) ||
                    isCloseToCurve(point.x, run.getStop(), maxRunDistance, index))) {
                curveTable.addRun(point.x - fatBox.x, run.getStart() - fatBox.y, run.getLength());
            }
            // @formatter:on
        }

        // Build glyph (TODO: table a bit too high, should be trimmed?)
        final int minRunCount = (int) Math.rint(minRunRatio * curveWidth);
        if (curveTable.getTotalRunCount() >= minRunCount) {
            final Glyph curveGlyph = sheet.getGlyphIndex().registerOriginal(
                    new Glyph(fatBox.x, fatBox.y, curveTable));
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

    //~ Static Methods -----------------------------------------------------------------------------

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
        return (Curve a1,
                Curve a2) -> Integer.compare(a1.getEnd(reverse).x, a2.getEnd(reverse).x);
    }
}
