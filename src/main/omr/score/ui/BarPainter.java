//----------------------------------------------------------------------------//
//                                                                            //
//                            B a r P a i n t e r                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.grid.LineInfo;
import omr.grid.StaffInfo;

import omr.math.BasicLine;
import omr.math.Line;

import omr.score.entity.SystemPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Class {code BarPainter} handles the painting of a barline, according
 * to its shape.
 *
 * @author Hervé Bitteur
 */
public class BarPainter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            BarPainter.class);

    /** Thin barline item. */
    private static final BarItem thin = new BarItem(0.2);

    /** Thick barline item. */
    private static final BarItem thick = new BarItem(0.5);

    /** Repeat dots item. */
    private static final BarItem dots = new DotItem(0.4);

    /** Typical abscissa gap between two vertical lines. */
    private static final double delta = 0.2;

    /** Typical abscissa gap when a repeat dot is involved. */
    private static final double dotDelta = 0.3;

    // Predefined painters, created once for all
    private static final BarPainter THIN_BP = new BarPainter(thin);

    private static final BarPainter THICK_BP = new BarPainter(thick);

    private static final BarPainter DOUBLE_BP = new BarPainter(thin, thin);

    private static final BarPainter FINAL_BP = new BarPainter(thin, thick);

    private static final BarPainter REVERSE_FINAL_BP = new BarPainter(
            thick,
            thin);

    private static final BarPainter LEFT_REPEAT_BP = new BarPainter(
            thick,
            thin,
            dots);

    private static final BarPainter RIGHT_REPEAT_BP = new BarPainter(
            dots,
            thin,
            thick);

    private static final BarPainter B2B_REPEAT_BP = new BarPainter(
            dots,
            thin,
            thick,
            thin,
            dots);

    //~ Instance fields --------------------------------------------------------
    //
    /** Sequence of items to paint. */
    private final BarItem[] items;

    //~ Constructors -----------------------------------------------------------
    //
    //------------//
    // BarPainter //
    //------------//
    private BarPainter (BarItem... items)
    {
        this.items = items;
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // draw //
    //------//
    /**
     * Perform the drawing of the barline structure.
     *
     * @param g         graphics context
     * @param topCenter center on top line
     * @param botCenter center on bottom line
     * @param part      containing part
     */
    public void draw (Graphics2D g,
                      Point2D topCenter,
                      Point2D botCenter,
                      SystemPart part)
    {
        double offset = -getGlobalWidth() / 2;
        BarItem prev = null;

        for (BarItem item : items) {
            // Translate to beginning of current item
            offset += gap(prev, item);

            // Draw current item
            item.draw(g, topCenter, botCenter, part, offset);

            // Move to end of current item
            offset += item.width;
            prev = item;
        }
    }

    //
    //---------------//
    // getBarPainter //
    //---------------//
    /**
     * Factory method to get a BarPainter instance suitable for the
     * provided barline shape.
     *
     * @param shape the provided barline shape
     * @return the proper BarPainter instance
     */
    public static BarPainter getBarPainter (Shape shape)
    {
        switch (shape) {
        case PART_DEFINING_BARLINE:
        case THIN_BARLINE:
            return THIN_BP;

        case THICK_BARLINE:
            return THICK_BP;

        case DOUBLE_BARLINE:
            return DOUBLE_BP;

        case FINAL_BARLINE:
            return FINAL_BP;

        case REVERSE_FINAL_BARLINE:
            return REVERSE_FINAL_BP;

        case LEFT_REPEAT_SIGN:
            return LEFT_REPEAT_BP;

        case RIGHT_REPEAT_SIGN:
            return RIGHT_REPEAT_BP;

        case BACK_TO_BACK_REPEAT_SIGN:
            return B2B_REPEAT_BP;

        default:
            logger.error("Illegal barline shape " + shape);

            return null;
        }
    }

    //-----//
    // gap //
    //-----//
    /**
     * Compute the abscissa gap between {@code prev} item and
     * {@code current} item.
     *
     * @param prev    previous item
     * @param current current item
     * @return the x gap between these two items
     */
    private double gap (BarItem prev,
                        BarItem current)
    {
        if (prev != null) {
            if ((prev == dots) || (current == dots)) {
                return dotDelta;
            } else {
                return delta;
            }
        } else {
            return 0;
        }
    }

    //----------------//
    // getGlobalWidth //
    //----------------//
    /**
     * Compute the global width of this barline structure.
     *
     * @return the global width (expressed in interline fraction)
     */
    private double getGlobalWidth ()
    {
        double w = 0;
        BarItem prev = null;

        for (BarItem item : items) {
            w += gap(prev, item);
            w += item.width;
            prev = item;
        }

        return w;
    }

    //~ Inner Classes ----------------------------------------------------------
    //---------//
    // BarItem //
    //---------//
    /**
     * Handles the drawing of a bar, with provided width.
     */
    private static class BarItem
    {
        //~ Instance fields ----------------------------------------------------

        /** Typical item width, expressed in interline fraction. */
        final double width;

        //~ Constructors -------------------------------------------------------
        public BarItem (double width)
        {
            this.width = width;
        }

        //~ Methods ------------------------------------------------------------
        public void draw (Graphics2D g,
                          Point2D topCenter,
                          Point2D botCenter,
                          SystemPart part,
                          double offset)
        {
            int il = part.getScale()
                    .getInterline();

            // Use a line stroke (=> problem with clipping)
            //            g.setStroke(new BasicStroke((float) (il * width)));
            //            Line2D line = new Line2D.Double(
            //                    new Point2D.Double(
            //                    topCenter.getX() + il * (offset + width / 2),
            //                    topCenter.getY()),
            //                    new Point2D.Double(
            //                    botCenter.getX() + il * (offset + width / 2),
            //                    botCenter.getY()));
            //            g.draw(line);

            // Use a polygon (no need to play with clipping)
            Polygon poly = new Polygon();
            poly.addPoint(
                    (int) Math.rint(topCenter.getX() + (il * offset)),
                    (int) Math.rint(topCenter.getY()));
            poly.addPoint(
                    (int) Math.rint(topCenter.getX() + (il * (offset + width))),
                    (int) Math.rint(topCenter.getY()));
            poly.addPoint(
                    (int) Math.rint(botCenter.getX() + (il * (offset + width))),
                    (int) Math.rint(botCenter.getY()));
            poly.addPoint(
                    (int) Math.rint(botCenter.getX() + (il * offset)),
                    (int) Math.rint(botCenter.getY()));
            g.fill(poly);
        }
    }

    //---------//
    // DotItem //
    //---------//
    /**
     * Handles the drawing of a pair of repeat dots.
     */
    private static class DotItem
            extends BarItem
    {
        //~ Constructors -------------------------------------------------------

        public DotItem (double width)
        {
            super(width);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void draw (Graphics2D g,
                          Point2D topCenter,
                          Point2D botCenter,
                          SystemPart part,
                          double offset)
        {
            Line bar = new BasicLine(
                    new double[]{topCenter.getX(), botCenter.getX()},
                    new double[]{topCenter.getY(), botCenter.getY()});

            int il = part.getScale()
                    .getInterline();

            for (StaffInfo staff : part.getInfo()
                    .getStaves()) {
                // Compute staff-based center
                List<LineInfo> lines = staff.getLines();
                LineInfo staffMidLine = lines.get(lines.size() / 2);
                Point2D inter = staffMidLine.verticalIntersection(bar);

                // Draw each point
                int scaledWidth = (int) Math.rint(il * width);

                for (int i = -1; i <= 1; i += 2) {
                    g.fillOval(
                            (int) Math.rint(inter.getX() + (il * offset)),
                            (int) Math.rint(
                            inter.getY() + ((il * (i - width)) / 2)),
                            scaledWidth,
                            scaledWidth);
                }
            }
        }
    }
}
