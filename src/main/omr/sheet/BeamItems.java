//----------------------------------------------------------------------------//
//                                                                            //
//                              B e a m I t e m s                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.facets.Glyph;

import omr.image.AreaMask;
import omr.image.Picture;
import omr.image.PixelFilter;

import omr.lag.Section;

import omr.math.Barycenter;
import omr.math.BasicLine;
import omr.math.GeoUtil;
import omr.math.LineUtil;

import omr.run.Orientation;
import omr.run.Run;

import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;
import omr.util.WrappedInteger;
import omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code BeamItems} handles one or several beam items, all
 * retrieved from a single glyph.
 * This is a private working companion of {@link BeamsBuilder}.
 */
public class BeamItems
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            BeamItems.class);

    public static final Comparator<BeamItems> byAbscissa = new Comparator<BeamItems>()
    {
        @Override
        public int compare (BeamItems b1,
                            BeamItems b2)
        {
            return Integer.compare(
                    b1.getGlyph()
                    .getBounds().x,
                    b2.getGlyph()
                    .getBounds().x);
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** Containing system. */
    private final SystemInfo system;

    /** Minimum acceptable width for a beam in this sheet. */
    private final int minBeamWidth;

    /** Underlying glyph. */
    private final Glyph glyph;

    /** The typical beam height in the sheet. */
    private final int typicalHeight;

    /** Sequence of items retrieved for the same glyph. */
    private final List<BeamItem> items = new ArrayList<BeamItem>();

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // BeamItems //
    //-----------//
    /**
     * Creates a new BeamItems object.
     *
     * @param system       containing system
     * @param glyph        the candidate glyph
     * @param minBeamWidth minimum width for a beam (in pixels)
     */
    public BeamItems (SystemInfo system,
                      Glyph glyph,
                      int minBeamWidth)
    {
        this.system = system;
        this.glyph = glyph;
        this.minBeamWidth = minBeamWidth;

        typicalHeight = system.getSheet()
                .getScale()
                .getMainBeam();
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // adjustSides //
    //-------------//
    /**
     * Adjust abscissa of horizontal sides.
     * Do this only for limits touching left or right side of the glyph.
     * In practice, if a limit is close to glyph side,
     * it's a full beam, we extend it to glyph side.
     * Otherwise, it's not a full beam so leave this side as it is.
     */
    public void adjustSides ()
    {
        // TODO: skip really too small sections on left or right
        // say total of sections height < typical height /4
        Rectangle glyphBox = glyph.getBounds();
        double gLeft = glyphBox.x;
        double gRight = (glyphBox.x + glyphBox.width) - 1;

        for (BeamItem item : items) {
            final Line2D median = item.median;

            // Check left
            if ((median.getX1() - gLeft) < minBeamWidth) {
                final Point2D newPt = LineUtil.intersectionAtX(median, gLeft);
                median.setLine(newPt, median.getP2());
            }

            // Check right
            if ((gRight - median.getX2()) < minBeamWidth) {
                final Point2D newPt = LineUtil.intersectionAtX(median, gRight);
                median.setLine(median.getP1(), newPt);
            }
        }
    }

//    //------------//
//    // checkMasks //
//    //------------//
//    public void checkMasks (Wrapper<Double> meanCoreRatio,
//                            Wrapper<Double> meanBeltRatio)
//    {
//        final PixelFilter pixelFilter = system.getSheet()
//                .getPicture()
//                .getBuffer(
//                        Picture.BufferKey.STAFF_FREE);
//        int sumCoreCount = 0;
//        int sumCoreFore = 0;
//        int sumBeltCount = 0;
//        int sumBeltFore = 0;
//
//        if (glyph.isVip()) {
//            logger.info("VIP checkMasks for {}", glyph);
//        }
//
//        for (BeamItem item : items) {
//            final int idx = items.indexOf(item);
//            final Area coreArea = item.getCoreArea();
//            glyph.addAttachment("c" + idx, coreArea);
//
//            final AreaMask coreMask = new AreaMask(coreArea);
//            final WrappedInteger core = new WrappedInteger(0);
//            final int coreCount = coreMask.fore(core, pixelFilter);
//
//            sumCoreCount += coreCount;
//            sumCoreFore += core.value;
//
//            // Build belt path
//            final int dx = 5; // Horizontal margin
//            // Test is not relevant for gutter between 2 aggregated beams
//
//            final int topDy = (idx == 0) ? 2 : 0;
//            final int bottomDy = (idx == (items.size() - 1)) ? 2 : 0;
//            Area beltArea = item.getBeltArea(
//                    coreArea,
//                    dx,
//                    topDy,
//                    bottomDy);
//            glyph.addAttachment("b" + idx, beltArea);
//
//            final AreaMask beltMask = new AreaMask(beltArea);
//            final WrappedInteger belt = new WrappedInteger(0);
//            final int beltCount = beltMask.fore(belt, pixelFilter);
//
//            sumBeltCount += beltCount;
//            sumBeltFore += belt.value;
//        }
//
//        meanCoreRatio.value = (double) sumCoreFore / sumCoreCount;
//        meanBeltRatio.value = (double) sumBeltFore / sumBeltCount;
//    }

    //---------------//
    // compareSlopes //
    //---------------//
    /**
     * Compare the slopes of beams (when there are several beams)
     *
     * @return max slope gap between consecutive beams
     */
    public double compareSlopes ()
    {
        double maxItemGap = 0;
        Double prevItemSlope = null;

        for (BeamItem item : items) {
            Line2D median = item.median;
            double itemSlope = LineUtil.getSlope(median);

            if (prevItemSlope != null) {
                double beamSlopeGap = Math.abs(itemSlope - prevItemSlope);
                maxItemGap = Math.max(maxItemGap, beamSlopeGap);
            }

            prevItemSlope = itemSlope;
        }

        return maxItemGap;
    }

    //--------------//
    // computeLines //
    //--------------//
    /**
     * Populate the items from the retrieved border lines, and measure
     * how straight they are.
     *
     * @return mean distance from border points to their lines, or null if
     *         border pairs are not consistent
     */
    public Double computeLines ()
    {
        List<BasicLine> topLines = getBorderLines(glyph, TOP);
        List<BasicLine> bottomLines = getBorderLines(glyph, BOTTOM);

        // Check straightness
        int sumPoints = 0; // Number of points measured
        double sumDist = 0; // Cumulated distance

        for (BasicLine line : topLines) {
            sumPoints += line.getNumberOfPoints();
            sumDist += (line.getMeanDistance() * line.getNumberOfPoints());
        }

        for (BasicLine line : bottomLines) {
            sumPoints += line.getNumberOfPoints();
            sumDist += (line.getMeanDistance() * line.getNumberOfPoints());
        }

        final double meanDist = sumDist / sumPoints;

        // Check same size
        if (topLines.size() != bottomLines.size()) {
            return null;
        }

        // Loop on beam items
        for (int i = 0; i < topLines.size(); i++) {
            // Impose one median line per item and a fixed height
            BasicLine top = topLines.get(i);
            BasicLine bot = bottomLines.get(i);
            double xMin = min(top.getMinAbscissa(), bot.getMinAbscissa());
            double xMax = max(top.getMaxAbscissa(), bot.getMaxAbscissa());
            double ytl = top.yAtX(xMin);
            double ybl = bot.yAtX(xMin);
            double ytr = top.yAtX(xMax);
            double ybr = bot.yAtX(xMax);
            double height = ((ybl - ytl) + (ybr - ytr)) / 2;
            Line2D median = new Line2D.Double(
                    xMin,
                    (ytl + ybl) / 2,
                    xMax,
                    (ytr + ybr) / 2);
            BeamItem item = new BeamItem(median, height);

            if (glyph.isVip()) {
                item.setVip();
            }

            items.add(item);
        }

        if (glyph.isVip()) {
            logger.info(toString());
        }

        logger.debug(
                "Beam#{} globalDist:{}",
                glyph.getId(),
                String.format("%.2f", meanDist));

        return meanDist;
    }

    //-------------------//
    // extendMiddleLines //
    //-------------------//
    /**
     * Extend middle lines if necessary.
     * This may happen for aggregates of 3 beams or more, where the middle
     * beam(s) have very poor borders, generally too small.
     */
    public void extendMiddleLines ()
    {
        if (items.size() < 3) {
            return;
        }

        double xLeft = Double.MAX_VALUE;
        double xRight = Double.MIN_VALUE;

        for (BeamItem item : items) {
            xLeft = Math.min(xLeft, item.median.getX1());
            xRight = Math.max(xRight, item.median.getX2());
        }

        for (BeamItem item : items) {
            Line2D median = item.median;

            // Extend to left & to right
            Point2D leftPt = LineUtil.intersectionAtX(median, xLeft);
            Point2D rightPt = LineUtil.intersectionAtX(median, xRight);
            median.setLine(leftPt, rightPt);
        }
    }

    //----------//
    // getGlyph //
    //----------//
    /**
     * @return the glyph
     */
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //----------//
    // getItems //
    //----------//
    /**
     * @return the items
     */
    public List<BeamItem> getItems ()
    {
        return items;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Compute the structure width, based on borders points only.
     *
     * @return a better width
     */
    public double getWidth ()
    {
        double xLeft = Double.MAX_VALUE;
        double xRight = Double.MIN_VALUE;

        for (BeamItem item : items) {
            xLeft = Math.min(xLeft, item.median.getX1());
            xRight = Math.max(xRight, item.median.getX2());
        }

        return xRight - xLeft + 1;
    }

    //------------//
    // splitItems //
    //------------//
    /**
     * Look for several beams stuck in a single item and split them
     * if necessary.
     */
    public void splitItems ()
    {
        final double meanHeight = glyph.getMeanThickness(
                Orientation.HORIZONTAL);
        final double ratio = meanHeight / typicalHeight;
        final int targetCount = (int) Math.rint(ratio);

        // Typical case: 2 beams are stuck (beamCount = 1, targetCount = 2)
        // TODO: what if beamCount = 1 and targetCount = 3 or more?
        // TODO: what if beamCount = 2 and targetCount = 3 or more?
        if ((items.size() > 1) || (targetCount <= items.size())) {
            return;
        }

        // Create the middle lines with proper vertical gap
        BeamItem item = items.get(0);
        Line2D median = item.median;
        double gutter = max(0, item.height - (2 * typicalHeight));
        double newHeight = (item.height - gutter) / 2;

        if (logger.isDebugEnabled()) {
            logger.debug(
                    String.format(
                            "Stuck beams #%d %d vs %.2f, gutter:%.1f",
                            glyph.getId(),
                            items.size(),
                            ratio,
                            gutter));
        }

        // Insert new items
        double dy = (newHeight + gutter) / 2;
        Line2D topMedian = new Line2D.Double(
                median.getX1(),
                median.getY1() - dy,
                median.getX2(),
                median.getY2() - dy);
        Line2D botMedian = new Line2D.Double(
                median.getX1(),
                median.getY1() + dy,
                median.getX2(),
                median.getY2() + dy);
        items.clear();
        items.add(new BeamItem(topMedian, newHeight));
        items.add(new BeamItem(botMedian, newHeight));
        logger.debug("Adjusted {}", this);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("beamGlyph#")
                .append(glyph.getId());

        for (BeamItem item : items) {
            sb.append(" ")
                    .append(item);
        }

        return sb.toString();
    }

    //----------------//
    // getBorderLines //
    //----------------//
    /**
     * Compute the lines that approximates borders on desired side.
     * There can be several lines on a given side if the glyph represents a
     * double beam or larger. So, we group lines by beam ordinates.
     *
     * @param glyph spot to analyze
     * @param side  TOP or BOTTOM
     * @return the computed line(s) for the desired side
     */
    private List<BasicLine> getBorderLines (Glyph glyph,
                                            VerticalSide side)
    {
        // All sections are vertical, retrieve their border (top or bottom)
        List<SectionBorder> sectionBorders = new ArrayList<SectionBorder>();

        for (Section section : glyph.getMembers()) {
            final Rectangle sectionBox = section.getBounds();

            // Discard too narrow sections
            if (sectionBox.width >= 3) {
                //TODO: use constant
                final BasicLine sectionLine = new BasicLine();
                int x = section.getFirstPos();

                for (Run run : section.getRuns()) {
                    sectionLine.includePoint(
                            x,
                            (side == VerticalSide.TOP) ? run.getStart()
                            : run.getStop());
                    x++;
                }

                sectionBorders.add(new SectionBorder(section, sectionLine));
            }
        }

        // Compute general slope
        double sumSlope = 0;
        int sumPoints = 0;

        for (SectionBorder border : sectionBorders) {
            BasicLine line = border.line;
            sumPoints += line.getNumberOfPoints();
            sumSlope += (line.getNumberOfPoints() * line.getSlope());
        }

        double globalSlope = sumSlope / sumPoints;

        // Define reference line
        BasicLine refLine = new BasicLine();
        Point center = glyph.getCentroid();
        refLine.includePoint(center.x, center.y);
        refLine.includePoint(center.x + 100, center.y + (100 * globalSlope));

        // Compute each section vertical offset WRT the refLine
        for (SectionBorder border : sectionBorders) {
            final Rectangle sectionBox = border.section.getBounds();
            BasicLine line = border.line;
            int x = GeoUtil.centerOf(sectionBox).x;
            int y = line.yAtX(x);
            int dy = y - refLine.yAtX(x);
            border.setOffset(dy);
        }

        Collections.sort(sectionBorders); // By distance to ref line
        // Retrieve groups of dy values, roughly separated by beam height
        // Each group will correspond to a separate beam

        final double delta = typicalHeight * 0.75; //TODO: use a constant?
        final List<BasicLine> lines = new ArrayList<BasicLine>();
        Barycenter dys = new Barycenter();
        BasicLine line = null;

        for (SectionBorder border : sectionBorders) {
            if ((line != null) && ((border.dy - dys.getY()) <= delta)) {
                dys.include(border.line.getNumberOfPoints(), 0, border.dy);
            } else {
                lines.add(line = new BasicLine());
                dys = new Barycenter();
                dys.include(border.line.getNumberOfPoints(), 0, border.dy);
            }

            line.includeLine(border.line);
        }

        // Purge too small lines
        for (Iterator<BasicLine> it = lines.iterator(); it.hasNext();) {
            BasicLine l = it.next();

            if (l.getNumberOfPoints() < minBeamWidth) {
                it.remove();
            }
        }

        return lines;
    }

    //~ Inner Classes ----------------------------------------------------------
    //---------------//
    // SectionBorder //
    //---------------//
    /**
     * Gathers info about a border line (top or bottom) of a section.
     */
    private static class SectionBorder
            implements Comparable<SectionBorder>
    {
        //~ Instance fields ----------------------------------------------------

        final Section section; // Underlying section

        final BasicLine line; // Border line (top or bottom)

        int dy; // Ordinate offset WRT glyph mean line

        //~ Constructors -------------------------------------------------------
        public SectionBorder (Section section,
                              BasicLine line)
        {
            this.section = section;
            this.line = line;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int compareTo (SectionBorder that)
        {
            // Sort by increasing ordinate
            return Integer.compare(this.dy, that.dy);
        }

        public void setOffset (int dy)
        {
            this.dy = dy;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{");
            sb.append(getClass().getSimpleName());
            sb.append(" dy:")
                    .append(dy);
            sb.append(" lg:")
                    .append(line.getNumberOfPoints());
            sb.append(" line:")
                    .append(line);
            sb.append(" section:")
                    .append(section);
            sb.append("}");

            return sb.toString();
        }
    }
}
