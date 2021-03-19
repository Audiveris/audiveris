//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B e a m S t r u c t u r e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sheet.beam;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.lag.JunctionRatioPolicy;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.SectionFactory;
import org.audiveris.omr.math.Barycenter;
import org.audiveris.omr.math.BasicLine;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.sheet.beam.BeamsBuilder.ItemParameters;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.*;
import org.audiveris.omr.util.Vip;

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
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class {@code BeamStructure} handles one or several {@link BeamLine} instances,
 * all retrieved from a single glyph.
 * This is a private working companion of {@link BeamsBuilder}.
 *
 * @author Hervé Bitteur
 */
public class BeamStructure
        implements Vip
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BeamStructure.class);

    /** Comparator on abscissae. */
    public static final Comparator<BeamStructure> byAbscissa = (BeamStructure b1, BeamStructure b2)
            -> Integer.compare(b1.getGlyph().getBounds().x, b2.getGlyph().getBounds().x);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying glyph. */
    private final Glyph glyph;

    /** Lag, if any, to index glyph section. */
    private final Lag spotLag;

    /** Sections built out of glyph. */
    private List<Section> glyphSections;

    /** Glyph centroid. */
    private final Point center;

    /** Parameters. */
    private final ItemParameters params;

    /** Sequence of lines retrieved for the same glyph, from top to bottom. */
    private final List<BeamLine> lines = new ArrayList<>();

    /** VIP flag. */
    private boolean vip;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BeamItems object.
     *
     * @param glyph   the candidate glyph
     * @param spotLag lag for sections, perhaps null
     * @param params  context-dependent parameters
     */
    public BeamStructure (Glyph glyph,
                          Lag spotLag,
                          ItemParameters params)
    {
        this.glyph = glyph;
        this.spotLag = spotLag;
        this.params = params;
        center = glyph.getCentroid();
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
        // TODO: skip really too small sections on left or right?
        // Say total of sections height < typical height /2
        Rectangle glyphBox = glyph.getBounds();
        double gLeft = glyphBox.x;
        double gRight = (glyphBox.x + glyphBox.width) - 1;

        for (BeamLine line : lines) {
            final Line2D median = line.median;

            // Check left
            if ((median.getX1() - gLeft) < params.minBeamWidthLow) {
                final Point2D newPt = LineUtil.intersectionAtX(median, gLeft);
                median.setLine(newPt, median.getP2());
            }

            // Check right
            if ((gRight - median.getX2()) < params.minBeamWidthLow) {
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
            double width = median.getX2() - median.getX1();

            // Discard too short line, its slope is not reliable enough
            if (width > params.maxHookWidth) {
                double itemSlope = LineUtil.getSlope(median);

                if (prevItemSlope != null) {
                    double beamSlopeGap = Math.abs(itemSlope - prevItemSlope);
                    maxItemGap = Math.max(maxItemGap, beamSlopeGap);
                }

                prevItemSlope = itemSlope;
            }
        }

        return maxItemGap;
    }

    //---------------//
    // computeJitter //
    //---------------//
    /**
     * Report a measure of border variation around its theoretical line.
     * <p>
     * Because the structure element used to retrieve beam spots is shaped like a disk, all the
     * spots exhibit rounded corners.
     * Hence, the check of border "straightness" is performed on the border length minus some
     * abscissa margin on left and rights ends.
     *
     * @param beamLine the structure beamLine to measure
     * @param side     upper or lower side of the beamLine
     * @return ratio of jitter distance normalized by glyph width
     */
    public double computeJitter (BeamLine beamLine,
                                 VerticalSide side)
    {
        // Determine abscissa margin according to beam typical height
        final int dx = (int) Math.rint(params.cornerMargin);
        final Line2D median = beamLine.median;
        final int x1 = (int) Math.rint(median.getX1() + dx);
        final int x2 = (int) Math.rint(median.getX2() - dx);
        final BasicLine sectionLine = new BasicLine();
        int width = 0;

        for (Section section : getGlyphSections()) {
            Rectangle sctBox = section.getBounds();
            Point sctCenter = GeoUtil.center(sctBox);
            int y = (int) Math.rint(LineUtil.yAtX(median, sctCenter.x));

            if (section.contains(sctCenter.x, y)) {
                int x = section.getFirstPos();
                width += sctBox.width;

                for (Run run : section.getRuns()) {
                    if ((x >= x1) && (x <= x2)) {
                        int end = (side == VerticalSide.TOP) ? run.getStart() : run.getStop();
                        sectionLine.includePoint(x, end);
                    }

                    x++;
                }
            }
        }

        if (glyph.isVip()) {
            logger.info(
                    "{} {} pts:{} width:{} gWidth:{}",
                    side,
                    sectionLine.getMeanDistance(),
                    sectionLine.getNumberOfPoints(),
                    width,
                    glyph.getWidth());
        }

        return sectionLine.getMeanDistance() / glyph.getWidth();
    }

    //--------------//
    // computeLines //
    //--------------//
    /**
     * Populate the lines from the retrieved border lines, and measure straightness.
     *
     * @return mean distance from border points to their lines, or null if border pairs are not
     *         consistent
     */
    public Double computeLines ()
    {
        if (glyph.isVip()) {
            logger.info("VIP computeLines for {}", glyph);
        }

        List<BasicLine> topLines = getBorderLines(glyph, TOP);
        List<BasicLine> bottomLines = getBorderLines(glyph, BOTTOM);

        if (topLines.isEmpty() || bottomLines.isEmpty()) {
            return null;
        }

        // Check straightness
        List<BasicLine> allLines = new ArrayList<>();
        allLines.addAll(topLines);
        allLines.addAll(bottomLines);

        double globalDistance = computeGlobalDistance(allLines);

        // Complete border lines
        double globalSlope = slopeOfLongest(allLines);
        SortedMap<Double, Line2D> topMap = getLinesMap(globalSlope, topLines);
        SortedMap<Double, Line2D> bottomMap = getLinesMap(globalSlope, bottomLines);
        completeBorderLines(+1, globalSlope, topMap, bottomMap);
        completeBorderLines(-1, globalSlope, bottomMap, topMap);

        if (topMap.size() != bottomMap.size()) {
            return null; // This should never happen!
        }

        // Loop on beam lines
        Iterator<Entry<Double, Line2D>> topIt = topMap.entrySet().iterator();
        Iterator<Entry<Double, Line2D>> botIt = bottomMap.entrySet().iterator();

        while (topIt.hasNext()) {
            // Impose one median line per line and a fixed height
            Line2D top = topIt.next().getValue();
            Line2D bot = botIt.next().getValue();
            double x1 = min(top.getX1(), bot.getX1());
            double x2 = max(top.getX2(), bot.getX2());
            double yt1 = LineUtil.yAtX(top, x1);
            double yb1 = LineUtil.yAtX(bot, x1);
            double yt2 = LineUtil.yAtX(top, x2);
            double yb2 = LineUtil.yAtX(bot, x2);

            double height = ((yb1 - yt1) + (yb2 - yt2)) / 2;
            Line2D median = new Line2D.Double(x1, (yt1 + yb1) / 2, x2, (yt2 + yb2) / 2);
            BeamLine line = new BeamLine(median, height);
            retrieveItems(line);

            if (glyph.isVip()) {
                line.setVip(true);
            }

            lines.add(line);
        }

        if (glyph.isVip()) {
            logger.info(String.format("VIP %s globalDist:%.2f", this, globalDistance));
        }

        return globalDistance;
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
    public void setVip (boolean vip)
    {
        this.vip = vip;
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
        final double ratio = meanHeight / params.typicalHeight;
        final int targetCount = (int) Math.rint(ratio);

        // Typical case: 2 beams are stuck (beamCount = 1, targetCount = 2)
        // TODO: what if beamCount = 1 and targetCount = 3 or more?
        // TODO: what if beamCount = 2 and targetCount = 3 or more?
        if ((lines.size() > 1) || (targetCount <= lines.size())) {
            return;
        }

        // Create the middle lines with proper vertical gap
        BeamLine line = lines.get(0);
        double gutter = line.height - (2 * params.typicalHeight);

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

    //---------------------//
    // completeBorderLines //
    //---------------------//
    /**
     * Add or extend observed border lines.
     * Beam items may be merged due to stuck pixels, resulting in missing (portions of) borders.
     * In theory, we should have pairs of top & bottom borders with identical length, each pair
     * corresponding to a beam item.
     *
     * @param yDir        -1 for going upward, +1 downward
     * @param globalSlope global structure slope
     * @param baseMap     (input/output) configuration of base lines
     * @param otherMap    (input/output) configuration of other lines
     */
    private void completeBorderLines (double yDir,
                                      double globalSlope,
                                      SortedMap<Double, Line2D> baseMap,
                                      SortedMap<Double, Line2D> otherMap)
    {
        double dy = yDir * params.typicalHeight;

        // For each base border line, look for corresponding other border line
        for (Entry<Double, Line2D> baseEntry : baseMap.entrySet()) {
            Line2D base = baseEntry.getValue();
            double targetY = baseEntry.getKey() + dy;
            Entry<Double, Line2D> otherEntry = lookupLine(targetY, otherMap);

            if (otherEntry == null) {
                // Create a brand new map entry
                otherMap.put(
                        targetY,
                        new Line2D.Double(
                                base.getX1(),
                                base.getY1() + dy,
                                base.getX2(),
                                base.getY2() + dy));
            } else {
                // Extend the map entry if needed
                Line2D other = otherEntry.getValue();
                double xMid = (other.getX1() + other.getX2()) / 2;
                double yMid = (other.getY1() + other.getY2()) / 2;
                double height = yMid - LineUtil.yAtX(base, xMid);
                Point2D p1 = (base.getX1() < other.getX1())
                        ? new Point2D.Double(base.getX1(), base.getY1() + height) : other.getP1();
                Point2D p2 = (base.getX2() > other.getX2())
                        ? new Point2D.Double(base.getX2(), base.getY2() + height) : other.getP2();
                double x = (p1.getX() + p2.getX()) / 2;
                double y = LineUtil.yAtX(p1, p2, x);
                double offset = y - LineUtil.yAtX(center, globalSlope, x);

                otherMap.remove(otherEntry.getKey());
                otherMap.put(offset, new Line2D.Double(p1, p2));
            }
        }
    }

    //-----------------------//
    // computeGlobalDistance //
    //-----------------------//
    /**
     * Compute the observed average distance to border line
     *
     * @param lines all border lines (top & bottom)
     * @return the average distance to straight line
     */
    private double computeGlobalDistance (List<BasicLine> lines)
    {
        int sumPoints = 0; // Number of points measured
        double sumDist = 0; // Cumulated distance

        for (BasicLine line : lines) {
            sumPoints += line.getNumberOfPoints();
            sumDist += (line.getMeanDistance() * line.getNumberOfPoints());
        }

        return sumDist / sumPoints;
    }

    //--------------------//
    // computeGlobalSlope //
    //--------------------//
    /**
     * Compute the leading slope among all borders found.
     * We consider the section by decreasing width, and compute the mean slope on first sections.
     * We stop as soon as a section border diverges strongly from the mean slope.
     *
     * @param sectionBorders the various sections borders (on one side)
     * @return the most probable global slope
     */
    private double computeGlobalSlope (List<SectionBorder> sectionBorders)
    {
        // Use mean slope of longest sections
        Collections.sort(sectionBorders, SectionBorder.byReverseLength);

        double sumSlope = 0;
        int sumPoints = 0;

        for (SectionBorder border : sectionBorders) {
            BasicLine line = border.line;
            double lineSlope = line.getSlope();

            if (sumPoints > 0) {
                // Check if this line diverges from current mean slope value
                double meanSlope = sumSlope / sumPoints;

                if (Math.abs(lineSlope - meanSlope) > constants.maxSectionSlopeGap.getValue()) {
                    break;
                }
            }

            sumPoints += line.getNumberOfPoints();
            sumSlope += (line.getNumberOfPoints() * line.getSlope());
        }

        return sumSlope / sumPoints;
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
        if (glyph.isVip()) {
            logger.info("VIP getBorderLines glyph#{} side:{}", glyph.getId(), side);
        }

        // All sections are vertical, retrieve their border (top or bottom)
        List<SectionBorder> sectionBorders = new ArrayList<>();

        for (Section section : getGlyphSections()) {
            final Rectangle sectionBox = section.getBounds();

            // Discard too narrow sections
            if (sectionBox.width >= params.coreSectionWidth) {
                final BasicLine sectionLine = new BasicLine();
                int x = section.getFirstPos();

                for (Run run : section.getRuns()) {
                    sectionLine.includePoint(
                            x,
                            (side == VerticalSide.TOP) ? run.getStart() : (run.getStop() + 1));
                    x++;
                }

                sectionBorders.add(new SectionBorder(section, sectionLine));
            }
        }

        // Retrieve global slope
        double globalSlope = computeGlobalSlope(sectionBorders);
        purgeSectionSlopes(globalSlope, sectionBorders);

        // Compute each section vertical offset WRT the refLine
        for (SectionBorder border : sectionBorders) {
            double x = GeoUtil.center2D(border.section.getBounds()).getX();
            double y = border.line.yAtX(x);
            double dy = y - LineUtil.yAtX(center, globalSlope, x);
            border.setOffset(dy);
        }

        Collections.sort(sectionBorders, SectionBorder.byOrdinateOffset);

        // Retrieve groups of offset values, roughly separated by beam height
        // Each group will correspond to a separate beam line
        final double delta = params.typicalHeight * constants.maxBorderJitter.getValue();
        final List<BasicLine> borderLines = new ArrayList<>();
        Barycenter dys = new Barycenter();
        BasicLine currentLine = null;

        for (SectionBorder border : sectionBorders) {
            if (currentLine == null) {
                borderLines.add(currentLine = new BasicLine());
                dys = new Barycenter();
            } else if ((border.dy - dys.getY()) > delta) {
                borderLines.add(currentLine = new BasicLine());
                dys = new Barycenter();
            }

            dys.include(border.line.getNumberOfPoints(), 0, border.dy);
            currentLine.includeLine(border.line);
        }

        // Purge too short lines (shorter than a hook)
        for (Iterator<BasicLine> it = borderLines.iterator(); it.hasNext();) {
            Line2D l = it.next().toDouble();

            if ((l.getX2() - l.getX1()) < params.minHookWidthLow) {
                it.remove();
            }
        }

        return borderLines;
    }

    //------------------//
    // getGlyphSections //
    //------------------//
    private List<Section> getGlyphSections ()
    {
        if (glyphSections == null) {
            glyphSections = new SectionFactory(spotLag, JunctionRatioPolicy.DEFAULT).createSections(
                    glyph.getRunTable(),
                    glyph.getTopLeft(),
                    false);
        }

        return glyphSections;
    }

    //-------------//
    // getLinesMap //
    //-------------//
    private SortedMap<Double, Line2D> getLinesMap (double globalSlope,
                                                   List<BasicLine> topLines)
    {
        SortedMap<Double, Line2D> map = new TreeMap<>();

        // Use refined value of global slope and flag each line WRT reference line
        for (BasicLine l : topLines) {
            Line2D line = l.toDouble();
            double x = (line.getX1() + line.getX2()) / 2;
            double y = l.yAtX(x);
            double dy = y - LineUtil.yAtX(center, globalSlope, x);
            map.put(dy, line);
        }

        return map;
    }

    //------------//
    // lookupLine //
    //------------//
    /**
     * Search among the provided lines a line compatible with the provided target.
     * Compatibility uses ordinate gap WRT reference line
     *
     * @param offset target offset WRT reference line
     * @param lines  map of available lines
     * @return the entry found or null
     */
    private Entry<Double, Line2D> lookupLine (double offset,
                                              SortedMap<Double, Line2D> lines)
    {
        final double delta = params.typicalHeight * constants.maxBorderJitter.getValue();

        for (Entry<Double, Line2D> entry : lines.entrySet()) {
            if (Math.abs(entry.getKey() - offset) <= delta) {
                return entry;
            }
        }

        return null;
    }

    //--------------------//
    // purgeSectionSlopes //
    //--------------------//
    /**
     * Discard sections whose slope value is too far from globalSlope.
     *
     * @param globalSlope    global slope
     * @param sectionBorders collection to be purged
     */
    private void purgeSectionSlopes (double globalSlope,
                                     List<SectionBorder> sectionBorders)
    {
        for (Iterator<SectionBorder> it = sectionBorders.iterator(); it.hasNext();) {
            SectionBorder border = it.next();
            double slope = border.line.getSlope();

            if (Math.abs(slope - globalSlope) > constants.maxSectionSlopeGap.getValue()) {
                it.remove();
            }
        }
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
        Integer start = null; // Starting abscissa of item being built
        Integer stop = null; // Current abscissa right after item being built

        // Sections are ordered by starting abscissa
        for (Section section : getGlyphSections()) {
            Rectangle sctBox = section.getBounds();
            Point2D sctCenter = GeoUtil.center2D(sctBox);
            double y = LineUtil.yAtX(median, sctCenter.getX());

            if (section.contains(sctCenter.getX(), y)) {
                // Extend current item or start a new one?
                if (stop != null) {
                    int dx = sctBox.x - stop;

                    if (dx > params.maxItemXGap) {
                        // End current item, start a new one
                        items.add(
                                new BeamItem(
                                        new Line2D.Double(
                                                LineUtil.intersectionAtX(median, start),
                                                LineUtil.intersectionAtX(median, stop)),
                                        beamLine.height));
                        start = sctBox.x;
                    }

                    stop = Math.max(stop, sctBox.x + sctBox.width);
                } else {
                    start = sctBox.x;
                    stop = sctBox.x + sctBox.width;
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

    //----------------//
    // slopeOfLongest //
    //----------------//
    private double slopeOfLongest (List<BasicLine> lines)
    {
        BasicLine bestLine = null;
        double bestLength = 0;

        for (BasicLine line : lines) {
            double length = line.getMaxAbscissa() - line.getMinAbscissa() + 1;

            if ((bestLine == null) || (bestLength < length)) {
                bestLength = length;
                bestLine = line;
            }
        }

        return bestLine.getSlope();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Double maxSectionSlopeGap = new Constant.Double(
                "tangent",
                0.3, // 0.2,
                "Maximum delta slope between sections of same border");

        private final Constant.Ratio maxBorderJitter = new Constant.Ratio(
                0.8,
                "Maximum border vertical jitter, specified as ratio of typical beam height");
    }

    //---------------//
    // SectionBorder //
    //---------------//
    /**
     * Gathers info about a border line (top or bottom) of a section.
     */
    private static class SectionBorder
    {

        // Sort by increasing ordinate offset WRT glyph reference line
        static Comparator<SectionBorder> byOrdinateOffset = (SectionBorder o1, SectionBorder o2)
                -> Double.compare(o1.dy, o2.dy);

        static Comparator<SectionBorder> byReverseLength = (SectionBorder o1, SectionBorder o2)
                -> Integer.compare(o2.section.getRunCount(), o1.section.getRunCount());

        final Section section; // Underlying section

        final BasicLine line; // Border line (top or bottom)

        double dy; // Ordinate offset WRT glyph reference line

        SectionBorder (Section section,
                       BasicLine line)
        {
            this.section = section;
            this.line = line;
        }

        public void setOffset (double dy)
        {
            this.dy = dy;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append("{");
            sb.append(" dy:").append(dy);
            sb.append(" lg:").append(line.getNumberOfPoints());
            sb.append(" line:").append(line);
            sb.append(" section:").append(section);
            sb.append("}");

            return sb.toString();
        }
    }
}
