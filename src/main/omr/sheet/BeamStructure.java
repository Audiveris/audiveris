//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B e a m S t r u c t u r e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.math.Barycenter;
import omr.math.BasicLine;
import omr.math.GeoUtil;
import omr.math.LineUtil;

import omr.run.Orientation;
import omr.run.Run;

import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;
import omr.util.Vip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code BeamStructure} handles one or several {@link BeamLine} instances,
 * all retrieved from a single glyph.
 * This is a private working companion of {@link BeamsBuilder}.
 */
public class BeamStructure
        implements Vip
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            BeamStructure.class);

    /** Comparator on abscissae. */
    public static final Comparator<BeamStructure> byAbscissa = new Comparator<BeamStructure>()
    {
        @Override
        public int compare (BeamStructure b1,
                            BeamStructure b2)
        {
            return Integer.compare(b1.getGlyph().getBounds().x, b2.getGlyph().getBounds().x);
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying glyph. */
    private final Glyph glyph;

    /** Minimum acceptable width for a beam. */
    private final int minBeamWidth;

    /** The typical beam height. */
    private final int typicalBeamHeight;

    /** maximum internal abscissa gap within a beam item. */
    private final int maxItemXGap;

    /** Sequence of lines retrieved for the same glyph, from top to bottom. */
    private final List<BeamLine> lines = new ArrayList<BeamLine>();

    /** VIP flag. */
    private boolean vip;

    //~ Constructors -------------------------------------------------------------------------------
    //---------------//
    // BeamStructure //
    //---------------//
    /**
     * Creates a new BeamItems object.
     *
     * @param glyph             the candidate glyph
     * @param minBeamWidth      minimum width for a beam (in pixels)
     * @param typicalBeamHeight typical height for a beam (in pixels)
     * @param maxItemXGap       maximum internal abscissa gap within a beam item
     */
    public BeamStructure (Glyph glyph,
                          int minBeamWidth,
                          int typicalBeamHeight,
                          int maxItemXGap)
    {
        this.glyph = glyph;
        this.minBeamWidth = minBeamWidth;
        this.typicalBeamHeight = typicalBeamHeight;
        this.maxItemXGap = maxItemXGap;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // adjustSides //
    //-------------//
    /**
     * Adjust abscissa of horizontal sides.
     * Do this only for limits touching left or right side of the glyph.
     * In practice, if a limit is close to glyph side, it's a full beam, we extend it to glyph side.
     * Otherwise, it's not a full beam so we leave this side as it is.
     */
    public void adjustSides ()
    {
        // TODO: skip really too small sections on left or right
        // say total of sections height < typical height /4
        Rectangle glyphBox = glyph.getBounds();
        double gLeft = glyphBox.x;
        double gRight = (glyphBox.x + glyphBox.width) - 1;

        for (BeamLine line : lines) {
            final Line2D median = line.median;

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

    //---------------//
    // compareSlopes //
    //---------------//
    /**
     * Compare the slopes of beams (when there are several lines)
     *
     * @return max slope gap between consecutive beams
     */
    public double compareSlopes ()
    {
        double maxItemGap = 0;
        Double prevItemSlope = null;

        for (BeamLine line : lines) {
            Line2D median = line.median;
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
     * Populate the lines from the retrieved border lines, and measure how straight they
     * are.
     *
     * @return mean distance from border points to their lines, or null if border pairs are not
     *         consistent
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

        // Loop on beam lines
        for (int i = 0; i < topLines.size(); i++) {
            // Impose one median line per line and a fixed height
            BasicLine top = topLines.get(i);
            BasicLine bot = bottomLines.get(i);
            double xMin = min(top.getMinAbscissa(), bot.getMinAbscissa());
            double xMax = max(top.getMaxAbscissa(), bot.getMaxAbscissa());
            double ytl = top.yAtX(xMin);
            double ybl = bot.yAtX(xMin);
            double ytr = top.yAtX(xMax);
            double ybr = bot.yAtX(xMax);
            double height = ((ybl - ytl) + (ybr - ytr)) / 2;
            Line2D median = new Line2D.Double(xMin, (ytl + ybl) / 2, xMax, (ytr + ybr) / 2);
            BeamLine line = new BeamLine(median, height);
            retrieveItems(line);

            if (glyph.isVip()) {
                line.setVip();
            }

            lines.add(line);
        }

        if (glyph.isVip()) {
            logger.info(String.format("VIP %s globalDist:%.2f", this, meanDist));
        }

        return meanDist;
    }

    //-------------------//
    // extendMiddleLines //
    //-------------------//
    /**
     * Extend middle lines if necessary.
     * This may happen for aggregates of 3 beams or more, where the middle line(s) have very poor
     * borders, generally too small.
     */
    public void extendMiddleLines ()
    {
        if (lines.size() < 3) {
            return;
        }

        double xLeft = Double.MAX_VALUE;
        double xRight = Double.MIN_VALUE;

        for (BeamLine line : lines) {
            xLeft = Math.min(xLeft, line.median.getX1());
            xRight = Math.max(xRight, line.median.getX2());
        }

        for (BeamLine line : lines) {
            Line2D median = line.median;

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
    // getLines //
    //----------//
    /**
     * @return the lines
     */
    public List<BeamLine> getLines ()
    {
        return lines;
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

        for (BeamLine line : lines) {
            xLeft = Math.min(xLeft, line.median.getX1());
            xRight = Math.max(xRight, line.median.getX2());
        }

        return xRight - xLeft + 1;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip ()
    {
        vip = true;
    }

    //------------//
    // splitLines //
    //------------//
    /**
     * Look for several beams stuck in a single line and split them if necessary.
     */
    public void splitLines ()
    {
        final double meanHeight = glyph.getMeanThickness(Orientation.HORIZONTAL);
        final double ratio = meanHeight / typicalBeamHeight;
        final int targetCount = (int) Math.rint(ratio);

        // Typical case: 2 beams are stuck (beamCount = 1, targetCount = 2)
        // TODO: what if beamCount = 1 and targetCount = 3 or more?
        // TODO: what if beamCount = 2 and targetCount = 3 or more?
        if ((lines.size() > 1) || (targetCount <= lines.size())) {
            return;
        }

        // Create the middle lines with proper vertical gap
        BeamLine line = lines.get(0);
        double gutter = line.height - (2 * typicalBeamHeight);

        if (gutter < 0) {
            if (glyph.isVip()) {
                logger.info("VIP glyph#{} not enough room for 2 beams", glyph.getId());
            }

            return;
        }

        double newHeight = (line.height - gutter) / 2;
        final Line2D median = line.median;

        if (logger.isDebugEnabled()) {
            logger.debug(
                    String.format(
                            "Stuck beams #%d %d vs %.2f, gutter:%.1f",
                            glyph.getId(),
                            lines.size(),
                            ratio,
                            gutter));
        }

        // Insert new lines
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
        lines.clear();
        lines.add(new BeamLine(topMedian, newHeight));
        lines.add(new BeamLine(botMedian, newHeight));
        logger.debug("Adjusted {}", this);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("beamGlyph#").append(glyph.getId());

        for (BeamLine line : lines) {
            sb.append(" [").append(line).append("]");
        }

        return sb.toString();
    }

    //----------------//
    // getBorderLines //
    //----------------//
    /**
     * Compute the lines that approximates borders on desired side.
     * There can be several lines on a given side if the glyph represents a double beam or larger.
     * So, we group lines by beam ordinates.
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
                            (side == VerticalSide.TOP) ? run.getStart() : run.getStop());
                    x++;
                }

                sectionBorders.add(new SectionBorder(section, sectionLine));
            }
        }

        // Compute general slope of section borders
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
            int x = GeoUtil.centerOf(border.section.getBounds()).x;
            int y = border.line.yAtX(x);
            int dy = y - refLine.yAtX(x);
            border.setOffset(dy);
        }

        Collections.sort(sectionBorders); // By distance to ref line

        // Retrieve groups of dy values, roughly separated by beam height
        // Each group will correspond to a separate beam line
        final double delta = typicalBeamHeight * 0.75; //TODO: use a constant?
        final List<BasicLine> borderLines = new ArrayList<BasicLine>();
        Barycenter dys = new Barycenter();
        BasicLine line = null;

        for (SectionBorder border : sectionBorders) {
            if ((line == null) || ((border.dy - dys.getY()) > delta)) {
                borderLines.add(line = new BasicLine());
                dys = new Barycenter();
            }

            dys.include(border.line.getNumberOfPoints(), 0, border.dy);
            line.includeLine(border.line);
        }

        // Purge too small lines
        for (Iterator<BasicLine> it = borderLines.iterator(); it.hasNext();) {
            if (it.next().getNumberOfPoints() < minBeamWidth) {
                it.remove();
            }
        }

        return borderLines;
    }

    //---------------//
    // retrieveItems //
    //---------------//
    /**
     * Populate this beam line with the items found along the median
     *
     * @param beamLine the BeamLine to populate
     */
    private void retrieveItems (BeamLine beamLine)
    {
        List<BeamItem> items = beamLine.getItems();
        Line2D median = beamLine.median;
        BasicLine line = new BasicLine(median);
        Integer start = null; // Starting abscissa of item being built
        Integer stop = null; // Current abscissa end of item being built

        // Sections are ordered by starting abscissa
        for (Section section : glyph.getMembers()) {
            Rectangle sctBox = section.getBounds();
            Point sctCenter = GeoUtil.centerOf(sctBox);
            int y = line.yAtX(sctCenter.x);

            if (section.contains(sctCenter.x, y)) {
                // Extend current item or start a new one?
                if (stop != null) {
                    int dx = sctBox.x - stop;

                    if (dx > maxItemXGap) {
                        // End current item, start a new one
                        items.add(
                                new BeamItem(
                                        new Line2D.Double(
                                                LineUtil.intersectionAtX(median, start),
                                                LineUtil.intersectionAtX(median, stop)),
                                        beamLine.height));
                        start = sctBox.x;
                    }

                    stop = Math.max(stop, (sctBox.x + sctBox.width) - 1);
                } else {
                    start = sctBox.x;
                    stop = (sctBox.x + sctBox.width) - 1;
                }
            }
        }

        if (stop != null) {
            items.add(
                    new BeamItem(
                            new Line2D.Double(
                                    LineUtil.intersectionAtX(median, start),
                                    LineUtil.intersectionAtX(median, stop)),
                            beamLine.height));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------//
    // SectionBorder //
    //---------------//
    /**
     * Gathers info about a border line (top or bottom) of a section.
     */
    private static class SectionBorder
            implements Comparable<SectionBorder>
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Section section; // Underlying section

        final BasicLine line; // Border line (top or bottom)

        int dy; // Ordinate offset WRT glyph reference line

        //~ Constructors ---------------------------------------------------------------------------
        public SectionBorder (Section section,
                              BasicLine line)
        {
            this.section = section;
            this.line = line;
        }

        //~ Methods --------------------------------------------------------------------------------
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
            sb.append(" dy:").append(dy);
            sb.append(" lg:").append(line.getNumberOfPoints());
            sb.append(" line:").append(line);
            sb.append(" section:").append(section);
            sb.append("}");

            return sb.toString();
        }
    }
}
