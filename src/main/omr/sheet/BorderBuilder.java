//----------------------------------------------------------------------------//
//                                                                            //
//                         B o r d e r B u i l d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.Filament;
import omr.grid.FilamentLine;
import omr.grid.LineInfo;

import omr.log.Logger;

import omr.math.NaturalSpline;
import static omr.run.Orientation.*;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.util.BrokenLine;
import omr.util.Predicate;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code BorderBuilder} implements a smart approach
 * to define the border between two systems.
 *
 * <p>Strategy for glyph assignment: <ol>
 * <li>Identify boxes of all glyphs intersected by the intersystem gutter.</li>
 * <li>Elaborate box-based continuous limit of both staves.</li>
 * <li>Use the (not too small) glyphs from yellow zone as free boxes seeds.</li>
 * <li>Enlarge free boxes to come up with horizontal blobs.</li>
 * <li>Incrementally:
 * <ol>
 * <li>"Grow" free boxes in vertical directions (dy = 1)</li>
 * <li>Re-evaluate intersections (with other free boxes & limit boxes)</li>
 * <li>Intersection with a staff limit assigns the initial free box to it</li>
 * <li>Exit when no free box is left</li>
 * </ol></ol></p>
 *
 * <p>Strategy for border definition: <ol>
 * <li>Use ordinate middle between the two (enlarged) box-based limits.</li>
 * <li>Remove all unneeded intermediate points.</li>
 * </ol></p>
 *
 *
 * @author Hervé Bitteur
 */
public class BorderBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BorderBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** System above */
    private final SystemInfo prevSystem;

    /** System below */
    private final SystemInfo system;

    // GlyphRect-based limits 
    private Limit           topLimit;
    private Limit           botLimit;

    /** Free blobs */
    private List<GlyphRect> blobs = new ArrayList<GlyphRect>();

    // Scale-dependent parameters
    private final double flatness;
    private final int    minGlyphWeight;
    private final int    xMargin;
    private final int    yMargin;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // BorderBuilder //
    //---------------//
    /**
     * Creates a new BorderBuilder object.
     * @param sheet the related sheet
     * @param prevSystem system on north side
     * @param system system on south side
     */
    public BorderBuilder (Sheet      sheet,
                          SystemInfo prevSystem,
                          SystemInfo system)
    {
        this.sheet = sheet;
        this.prevSystem = prevSystem;
        this.system = system;

        Scale scale = sheet.getScale();
        flatness = scale.toPixelsDouble(constants.lineFlatness);
        minGlyphWeight = scale.toPixels(constants.minGlyphWeight);
        xMargin = scale.toPixels(constants.xMargin);
        yMargin = scale.toPixels(constants.yMargin);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // buildBorder //
    //-------------//
    public BrokenLine buildBorder ()
    {
        // First, retrieve glyphs intersected by intersystem rectangle
        LineInfo       topLine = prevSystem.getLastStaff()
                                           .getLastLine();
        LineInfo       botLine = system.getFirstStaff()
                                       .getFirstLine();
        PixelRectangle box = topLine.getContourBox();
        box.add(botLine.getContourBox());

        Set<Glyph> glyphs = sheet.getNest()
                                 .lookupIntersectedGlyphs(box);

        // Remove small glyphs and the staff lines themselves
        Glyphs.purge(
            glyphs,
            new Predicate<Glyph>() {
                    public boolean check (Glyph glyph)
                    {
                        return (glyph.getShape() == Shape.STAFF_LINE) ||
                               (glyph.getWeight() < minGlyphWeight);
                    }
                });

        // Split the set between staff limits and free glyphs in the middle
        topLimit = buildLimit(topLine, glyphs, -1);
        botLimit = buildLimit(botLine, glyphs, +1);

        if (logger.isFineEnabled()) {
            logger.fine("topLimit: " + topLimit);
            logger.fine("botLimit: " + botLimit);
        }

        // Aggregate free glyphs into fewer blobs
        buildFreeBlobs(glyphs);

        // Assign each free blob to proper limit
        assignFreeBlobs();

        // Build a raw border out of the two limits
        BrokenLine rawBorder = getRawBorder();

        if (false) {
            return rawBorder;
        } else {
            // Return the refined border
            return getRefinedBorder(rawBorder);
        }
    }

    //--------------//
    // getRawBorder //
    //--------------//
    /**
     * Build a broken line as the "middle" between top & bottom limits
     * @return the (raw) border
     */
    private BrokenLine getRawBorder ()
    {
        // Smoothens the border, but may lead to impossible borders
        topLimit.grow(xMargin, 0);
        botLimit.grow(xMargin, 0);

        //
        BrokenLine line = new BrokenLine();
        int        yPrev = -1;
        int        xPrev = -1;

        for (int x = 0, xMax = sheet.getWidth(); x <= xMax; x++) {
            int top = topLimit.getY(x, -1);
            int bot = botLimit.getY(x, +1);

            if (top > bot) {
                logger.warning("Border closed at x: " + x);
            }

            int y = (top + bot) / 2;

            if (x == xMax) {
                // Very last point
                line.addPoint(new Point(x, y));
            } else if (y != yPrev) {
                if (x > (xPrev + 1)) {
                    line.addPoint(new Point(x - 1, yPrev));
                }

                line.addPoint(new Point(x, y));
                xPrev = x;
                yPrev = y;
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine("Raw border: " + line);
        }

        return line;
    }

    //------------------//
    // getRefinedBorder //
    //------------------//
    /**
     * Refine the raw border as much as possible
     * @param line the initial (raw) border
     * @return the refined border
     */
    private BrokenLine getRefinedBorder (BrokenLine line)
    {
        // 1/Start with left point
        // 2/Try to skip the following points until a limit is intersected
        // 3/Backup to previous point and keep it
        // 4/Goto 2/
        // Complete with right side
        int lastIndex = 0;

        Removal: 
        while (true) {
            Point lastPoint = line.getPoint(lastIndex);

            for (int index = lastIndex + 1; index < line.size(); index++) {
                Point pt = line.getPoint(index);

                if (topLimit.intersects(lastPoint, pt) ||
                    botLimit.intersects(lastPoint, pt)) {
                    // Backup 
                    for (int i = lastIndex + 1, iBreak = index - 1; i < iBreak;
                         i++) {
                        Point p = line.getPoint(lastIndex + 1);
                        line.removePoint(p);

                        if (logger.isFineEnabled()) {
                            logger.fine("Removed " + p);
                        }
                    }

                    lastIndex++;

                    continue Removal;
                }
            }

            break;
        }

        for (int i = lastIndex + 1, iBreak = line.size() - 1; i < iBreak;
             i++) {
            Point p = line.getPoint(lastIndex + 1);
            line.removePoint(p);
        }

        logger.info(
            sheet.getLogPrefix() + "Smart S" + prevSystem.getId() + "-S" +
            system.getId() + " system border: " + line);

        return line;
    }

    //-----------------//
    // assignFreeBlobs //
    //-----------------//
    /**
     * Iterate on growing free blobs, until they get assigned to a limit
     */
    private void assignFreeBlobs ()
    {
        int delta = system.getTop() - prevSystem.getBottom();

        for (int i = 1; i < delta; i++) {
            if (logger.isFineEnabled()) {
                logger.fine("i:" + i);
            }

            // Thicken free blobs and revaluate position WRT limits
            int index = -1;

            for (Iterator<GlyphRect> it = blobs.iterator(); it.hasNext();) {
                Rectangle blob = it.next();
                index++;

                Rectangle rect = new Rectangle(blob);
                rect.grow(0, i);

                if (topLimit.intersects(rect)) {
                    topLimit.add(blob);

                    if (logger.isFineEnabled()) {
                        logger.info("topLimit <- " + blob);
                    }

                    it.remove();
                } else if (botLimit.intersects(rect)) {
                    botLimit.add(blob);

                    if (logger.isFineEnabled()) {
                        logger.info("botLimit <- " + blob);
                    }

                    it.remove();
                } else {
                    // Fusion with another blob?
                    if (index < (blobs.size() - 1)) {
                        for (Rectangle b : blobs.subList(
                            index + 1,
                            blobs.size())) {
                            if (b.intersects(rect)) {
                                if (logger.isFineEnabled()) {
                                    logger.info(b + " + " + blob);
                                }

                                b.add(blob);
                                it.remove();

                                break;
                            }
                        }
                    }
                }
            }

            if (blobs.isEmpty()) {
                break;
            }
        }
    }

    //----------------//
    // buildFreeBlobs //
    //----------------//
    /**
     * Aggregate the free glyphs into a reduced number of horizontal rectangles
     * @param glyphs the free glyphs
     */
    private void buildFreeBlobs (Collection<Glyph> glyphs)
    {
        for (Glyph glyph : glyphs) {
            Rectangle rect = glyph.getContourBox();
            rect.grow(xMargin, yMargin);

            boolean aggregated = false;

            // Check if we can aggregate to an existing blob
            for (GlyphRect blob : blobs) {
                if (rect.intersects(blob)) {
                    blob.add(glyph);
                    aggregated = true;

                    break;
                }
            }

            if (!aggregated) {
                // Create a brand new blob
                blobs.add(new GlyphRect(glyph));
            }
        }

        if (logger.isFineEnabled()) {
            for (Rectangle blob : blobs) {
                logger.fine("free: " + blob);
            }
        }
    }

    //------------//
    // buildLimit //
    //------------//
    /**
     * Pickup the glyphs intersected by the provided line, and purge the
     * provided collection of these intersecting glyphs, as well as the non
     * intersecting glyphs which are located in the 'dir' direction with
     * respect to the line
     * @param lineInfo the line to intersect
     * @param glyphs the initial collection of glyphs
     * @param dir the direction in which glyphs are removed
     * @return the limit made of glyphs on the line
     */
    private Limit buildLimit (LineInfo          lineInfo,
                              Collection<Glyph> glyphs,
                              int               dir)
    {
        List<Glyph>   lineGlyphs = new ArrayList<Glyph>();
        FilamentLine  filamentLine = (FilamentLine) lineInfo;
        Filament      fil = filamentLine.getFilament();
        NaturalSpline line = (NaturalSpline) fil.getLine();

        for (Iterator<Glyph> it = glyphs.iterator(); it.hasNext();) {
            Glyph glyph = it.next();

            if (line.intersects(glyph.getContourBox(), flatness)) {
                lineGlyphs.add(glyph);
                it.remove();
            } else {
                // Check glyph position WRT line
                PixelPoint center = glyph.getAreaCenter();
                int        y = filamentLine.yAt(center.x);

                if (((center.y - y) * dir) > 0) {
                    it.remove();
                }
            }
        }

        // Now build the line-based limit
        Limit limit = new Limit();
        int   x1 = 0;

        for (Glyph glyph : lineGlyphs) {
            PixelRectangle contour = glyph.getContourBox();

            if (contour.x > x1) {
                // We need to insert an artificial box, based on line segment
                int y1 = (int) Math.rint(fil.positionAt(x1, HORIZONTAL));
                int y2 = (int) Math.rint(fil.positionAt(contour.x, HORIZONTAL));
                limit.boxes.add(new LineRect(x1, y1, contour.x, y2, dir));
            }

            // Insert the current glyph box
            limit.boxes.add(new GlyphRect(glyph));
            x1 = contour.x + contour.width;
        }

        // Complete the limit
        int y1 = (int) Math.rint(fil.positionAt(x1, HORIZONTAL));
        int y2 = (int) Math.rint(fil.positionAt(sheet.getWidth(), HORIZONTAL));
        limit.boxes.add(new LineRect(x1, y1, sheet.getWidth(), y2, dir));

        return limit;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction     lineFlatness = new Scale.Fraction(
            0.5,
            "Maximum flattening distance");

        //
        Scale.Fraction     xMargin = new Scale.Fraction(
            2,
            "Inter blob horizontal margin");

        //
        Scale.Fraction     yMargin = new Scale.Fraction(
            0,
            "Inter blob vertical margin");

        //
        Scale.AreaFraction minGlyphWeight = new Scale.AreaFraction(
            0.1,
            "Minimum weight for free glyph");
    }

    //-----------//
    // GlyphRect // 
    //-----------//
    /**
     * A standard rectangle, which keeps track of its building glyphs
     */
    private static class GlyphRect
        extends PixelRectangle
    {
        //~ Instance fields ----------------------------------------------------

        /** Related glyphs (just for debug) */
        final List<Glyph> glyphs = new ArrayList<Glyph>();

        //~ Constructors -------------------------------------------------------

        public GlyphRect (Glyph glyph)
        {
            super(glyph.getContourBox());
            glyphs.add(glyph);
        }

        //~ Methods ------------------------------------------------------------

        public void add (Glyph glyph)
        {
            add(glyph.getContourBox());
            glyphs.add(glyph);
        }

        public void add (GlyphRect that)
        {
            super.add(that);
            glyphs.addAll(that.glyphs);
        }

        @Override
        public String toString ()
        {
            return Glyphs.toString("B", glyphs);
        }
    }

    //-------//
    // Limit //
    //-------//
    /**
     * Handles the limit of a system as a continuous sequence of rectangles
     * made of intersected glyphs and staff line segments.
     */
    private static class Limit
    {
        //~ Instance fields ----------------------------------------------------

        /** Horizontal sequence of boxes */
        List<Rectangle> boxes = new ArrayList<Rectangle>();

        //~ Methods ------------------------------------------------------------

        public Rectangle getBounds ()
        {
            Rectangle bounds = null;

            for (Rectangle rect : boxes) {
                if (bounds == null) {
                    bounds = new Rectangle(rect);
                } else {
                    bounds.add(rect);
                }
            }

            return bounds;
        }

        public int getY (int x,
                         int dir)
        {
            Integer bestY = null;

            for (Rectangle r : boxes) {
                if ((x >= r.x) && (x <= (r.x + r.width))) {
                    int y = (dir < 0) ? (r.y + r.height) : r.y;

                    if (bestY == null) {
                        bestY = y;
                    } else {
                        if (((y - bestY) * dir) < 0) {
                            bestY = y;
                        }
                    }
                }
            }

            return bestY;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{Limit [");

            for (Rectangle rect : boxes) {
                if (rect instanceof LineRect) {
                    sb.append("-")
                      .append(rect.height)
                      .append("-");
                } else {
                    sb.append(rect);
                }
            }

            sb.append("] bounds:")
              .append(getBounds());
            sb.append("}");

            return sb.toString();
        }

        private void add (Rectangle blob)
        {
            boxes.add(blob);
        }

        private void grow (int h,
                           int v)
        {
            for (Rectangle rect : boxes) {
                rect.grow(h, v);
            }
        }

        private boolean intersects (Rectangle rectangle)
        {
            Rectangle prevRect = null;

            for (Rectangle rect : boxes) {
                if (rect.intersects(rectangle)) {
                    return true;
                }

                prevRect = rect;
            }

            return false;
        }

        private boolean intersects (Point p1,
                                    Point p2)
        {
            for (Rectangle rect : boxes) {
                if (rect.intersectsLine(p1.x, p1.y, p2.x, p2.y)) {
                    if (logger.isFineEnabled()) {
                        logger.info(
                            rect + " intersects from " + p1 + " to " + p2);
                    }

                    return true;
                }
            }

            return false;
        }
    }

    //----------//
    // LineRect //
    //----------//
    /**
     * A Rectangle built from a segment of rather horizontal staff line, and
     * ensured to be non-empty to allow intersection computation.
     */
    private static class LineRect
        extends Rectangle
    {
        //~ Constructors -------------------------------------------------------

        public LineRect (int x1,
                         int y1,
                         int x2,
                         int y2,
                         int dir)
        {
            super(x1, Math.min(y1, y2), x2 - x1, Math.abs(y2 - y1));

            // Make sure this (line-based) rectangle is non empty
            if (y2 == y1) {
                if (dir < 0) {
                    y--;
                }

                height++;
            }
        }
    }
}
