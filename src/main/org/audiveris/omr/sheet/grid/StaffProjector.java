//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t a f f P r o j e c t o r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.grid;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.AreaUtil.CoreData;
import org.audiveris.omr.math.GeoPath;
import org.audiveris.omr.math.Projection;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.InterlineScale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.grid.StaffPeak.Attribute;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Navigable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.jgrapht.Graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.WindowConstants;

/**
 * Class {@code StaffProjector} is in charge of analyzing a staff projection onto
 * x-axis, in order to retrieve barlines candidates as well as staff start and stop
 * abscissae.
 * <p>
 * To retrieve bar lines candidates, we analyze the vertical interior of staff because this is where
 * a barline must be present.
 * The potential bar portions outside staff height are much less typical of a barline.
 * <p>
 * A peak in staff projection can result from:<ol>
 * <li>A thick or thin <b>bar line</b>:<br>
 * <img alt="Image of bar lines"
 * src="http://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/Barlines.svg/400px-Barlines.svg.png">
 *
 * <li>A <b>bracket</b> portion:<br>
 * <img alt="Image of bracket"  width="250" height="216"
 * src="http://donrathjr.com/wp-content/uploads/2010/08/Brackets-and-Braces-4a.png">
 *
 * <li>A <b>brace</b> portion:<br>
 * <img alt="Image of brace"
 * src="http://upload.wikimedia.org/wikipedia/commons/thumb/2/28/Brace_(music).png/240px-Brace_(music).png">
 *
 * <li>An Alto <b>C-clef</b> portion:<br>
 * <img alt="Image of alto clef"
 * src="http://upload.wikimedia.org/wikipedia/commons/thumb/6/68/Alto_clef_with_ref.svg/90px-Alto_clef_with_ref.svg.png">
 * <br>
 * Such C-clef artifacts are detected later, based on their abscissa offset from the measure
 * start (be it bar-based start or lines-only start).
 *
 * <li>A <b>stem</b> (with note heads located outside the staff height).
 * <li>Just <b>garbage</b>.
 * </ol>
 * <p>
 * Before this class is used, staves are only defined by their lines made of long horizontal
 * sections.
 * This gives a good vertical definition (sufficient to allow the x-axis projection) but a very poor
 * horizontal definition.
 * To retrieve precise staff start and stop abscissae, the projection can tell which abscissa values
 * are outside the staff abscissa range, since the lack of staff lines results in a projection value
 * close to zero.
 * <p>
 * The projection also gives indication about lack of chunk (beam or head) on each side of a bar
 * candidate, but this indication is very weak and limited to the staff height portion.
 *
 * @author Hervé Bitteur
 */
public class StaffProjector
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            StaffProjector.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Related scale. */
    private final Scale scale;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Staff to analyze. */
    private final Staff staff;

    /** Pixel source. */
    private final ByteProcessor pixelFilter;

    /** Sequence of all blank regions found, whatever their width. */
    private final List<Blank> allBlanks = new ArrayList<Blank>();

    /** Selected (wide) ending blank region on each staff side. */
    private final Map<HorizontalSide, Blank> endingBlanks = new EnumMap<HorizontalSide, Blank>(
            HorizontalSide.class);

    /** Sequence of peaks found. */
    private final List<StaffPeak> peaks = new ArrayList<StaffPeak>();

    /** (Unmodifiable) view on peaks. */
    private final List<StaffPeak> peaksView = Collections.unmodifiableList(peaks);

    /** Graph of all peaks, linked by alignment/connection. */
    private final Graph<StaffPeak, BarAlignment> peakGraph;

    /** Count of cumulated foreground pixels, indexed by abscissa. */
    private Projection projection;

    /** Initial brace peak, if any. */
    private StaffPeak bracePeak;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StaffProjector} object.
     *
     * @param sheet     containing sheet
     * @param staff     staff to analyze
     * @param peakGraph sheet graph of peaks
     */
    public StaffProjector (Sheet sheet,
                           Staff staff,
                           PeakGraph peakGraph)
    {
        this.sheet = sheet;
        this.staff = staff;
        this.peakGraph = peakGraph;

        Picture picture = sheet.getPicture();
        pixelFilter = picture.getSource(Picture.SourceKey.BINARY);

        scale = sheet.getScale();
        params = new Parameters(scale, staff.getSpecificInterline());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // checkLinesRoot //
    //----------------//
    /**
     * Check for presence of lines roots right before first bar.
     * <p>
     * We cannot rely on current lines definition, since they are based only on long chunks.
     * Hence we use staff projection which, when no brace is present, should be below lines
     * threshold for some abscissa range.
     * <p>
     * If not, this means some portion of lines is present, hence the (group of) peaks found are not
     * bars (but perhaps peaks of C-Clef) so there is no start bar and staff left abscissa must be
     * defined by lines roots.
     */
    public void checkLinesRoot ()
    {
        if ((getBracePeak() != null) || peaks.isEmpty()) {
            return;
        }

        try {
            final int iStart = getStartPeakIndex();

            if (iStart != -1) {
                final StaffPeak firstPeak = peaks.get(0);

                // There must be a significant blank just before first peak
                Blank blank = selectBlank(LEFT, firstPeak.getStart(), params.minSmallBlankWidth);

                if (blank != null) {
                    int gap = firstPeak.getStart() - 1 - blank.stop;

                    if (gap > params.maxLeftExtremum) {
                        // Root portion found, so unset start peak and define true line start.
                        peaks.get(iStart).unset(Attribute.STAFF_LEFT_END);
                        staff.setAbscissa(LEFT, blank.stop + 1);
                    }
                } else {
                    logger.warn("Staff#{} no clear end on LEFT", staff.getId());
                }
            }
        } catch (Exception ex) {
            logger.warn("Error in checkLinesRoot on staff#{} {}", staff.getId(), ex.toString(), ex);
        }
    }

    //---------------//
    // findBracePeak //
    //---------------//
    /**
     * Try to find a brace-compatible peak on left side of provided abscissa.
     *
     * @param minLeft  provided minimum abscissa on left
     * @param maxRight provided maximum abscissa on right
     * @return a brace peak, or null
     */
    public StaffPeak findBracePeak (int minLeft,
                                    int maxRight)
    {
        final int minValue = params.braceThreshold;
        final Blank leftBlank = endingBlanks.get(LEFT);
        final int xMin;

        if (leftBlank != null) {
            if ((leftBlank.stop + 2) >= maxRight) { // +2 to cope with blank-peak gap
                // Large blank just before bar, look even farther on left
                maxRight = leftBlank.start - 1;

                Blank prevBlank = selectBlank(LEFT, maxRight, params.minWideBlankWidth);

                if (prevBlank != null) {
                    xMin = prevBlank.stop;
                } else {
                    xMin = minLeft;
                }
            } else {
                xMin = Math.max(minLeft, leftBlank.stop);
            }
        } else {
            xMin = Math.max(minLeft, 0);
        }

        int braceStop = -1;
        int braceStart = -1;
        int bestValue = 0;
        boolean valleyHit = false;

        // Browse from right to left
        // First finding valley left of bar, then brace peak if any
        for (int x = maxRight; x >= xMin; x--) {
            int value = projection.getValue(x);

            if (value >= minValue) {
                if (!valleyHit) {
                    continue;
                }

                if (braceStop == -1) {
                    braceStop = x;
                }

                braceStart = x;
                bestValue = Math.max(bestValue, value);
            } else if (!valleyHit) {
                valleyHit = true;
            } else if (braceStop != -1) {
                return createBracePeak(braceStart, braceStop, maxRight);
            }
        }

        // Brace peak on going (stuck on left side of image)?
        if (braceStart >= 0) {
            return createBracePeak(braceStart, braceStop, maxRight);
        }

        return null;
    }

    //--------------//
    // getBracePeak //
    //--------------//
    /**
     * @return the bracePeak
     */
    public StaffPeak getBracePeak ()
    {
        return bracePeak;
    }

    //-------------//
    // getLastPeak //
    //-------------//
    /**
     * Report the last peak in peaks sequence
     *
     * @return last peak or null
     */
    public StaffPeak getLastPeak ()
    {
        if (peaks.isEmpty()) {
            return null;
        }

        return peaks.get(peaks.size() - 1);
    }

    //----------//
    // getPeaks //
    //----------//
    /**
     * Get a view on projector peaks.
     *
     * @return the (unmodifiable) list of peaks
     */
    public List<StaffPeak> getPeaks ()
    {
        return peaksView;
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * Report the underlying staff for this projector.
     *
     * @return the staff
     */
    public Staff getStaff ()
    {
        return staff;
    }

    //-------------------//
    // getStartPeakIndex //
    //-------------------//
    /**
     * Report the index of the start peak, if any
     *
     * @return start peak index, or -1 if none
     */
    public int getStartPeakIndex ()
    {
        for (int i = 0; i < peaks.size(); i++) {
            if (peaks.get(i).isStaffEnd(LEFT)) {
                return i;
            }
        }

        return -1;
    }

    //------------------//
    // hasStandardBlank //
    //------------------//
    /**
     * Check whether there is a blank of at least standard width, within the provided
     * abscissa range.
     *
     * @param start range start
     * @param stop  range stop
     * @return true if standard blank was found
     */
    public boolean hasStandardBlank (int start,
                                     int stop)
    {
        if (stop <= start) {
            return false;
        }

        Blank blank = selectBlank(RIGHT, start, params.minStandardBlankWidth);

        return (blank != null) && (blank.start <= stop);
    }

    //------------//
    // insertPeak //
    //------------//
    /**
     * Insert a new peak right before an existing one.
     *
     * @param toInsert the new peak to insert
     * @param before   the existing peak before which insertion must be done
     */
    public void insertPeak (StaffPeak toInsert,
                            StaffPeak before)
    {
        int index = peaks.indexOf(before);

        if (index == -1) {
            throw new IllegalArgumentException("insertPeak() before a non-existing peak");
        }

        peaks.add(index, toInsert);
        peakGraph.addVertex(toInsert);
    }

    //------//
    // plot //
    //------//
    /**
     * Display a chart of the projection.
     */
    public void plot ()
    {
        if (projection == null) {
            computeProjection();
            computeLineThresholds();
        }

        new Plotter().plot();
    }

    //---------//
    // process //
    //---------//
    /**
     * Process the staff projection on x-axis to retrieve peaks that may represent bars.
     */
    public void process ()
    {
        logger.debug("StaffProjector analyzing staff#{}", staff.getId());

        // Cumulate pixels for each abscissa
        computeProjection();

        // Adjust thresholds according to actual line thicknesses in this staff
        computeLineThresholds();

        // Retrieve all regions without staff lines
        findAllBlanks();

        // Select the wide blanks that limit staff search in abscissa
        selectEndingBlanks();

        // Retrieve peaks as barline raw candidates
        findPeaks();
    }

    //----------------//
    // refineRightEnd //
    //----------------//
    /**
     * Try to use the extreme peak on staff right side, to refine the precise abscissa
     * where the staff ends.
     * <p>
     * When this method is called, the staff sides are defined only by the ends of the lines built
     * with long sections.
     * An extreme peak can be used as abscissa reference only if it is either beyond current staff
     * end or sufficiently close to the end.
     * If no such peak is found, we stop right before the blank region assuming that this is a
     * measure with no outside bar.
     */
    public void refineRightEnd ()
    {
        final int linesEnd = staff.getAbscissa(RIGHT); // As defined by end of long staff sections
        int staffEnd = linesEnd;
        StaffPeak endPeak = null;
        Integer peakEnd = null;

        // Look for a suitable peak
        if (!peaks.isEmpty()) {
            StaffPeak peak = peaks.get(peaks.size() - 1);

            if (peak != null) {
                // Check side position of peak wrt staff, it must be external
                final int peakMid = (peak.getStart() + peak.getStop()) / 2;
                final int toPeak = peakMid - linesEnd;

                if (toPeak >= 0) {
                    endPeak = peak;
                    peakEnd = endPeak.getStop();
                    staffEnd = peakEnd;
                }
            }
        }

        // Continue and stop at first small blank region encountered or image limit.
        // Then keep the additional line chunk if long enough.
        // If not, use peak mid as staff end.
        final Blank blank = selectBlank(RIGHT, staffEnd, params.minSmallBlankWidth);
        final int xMax = (blank != null) ? (blank.start - 1) : (sheet.getWidth() - 1);

        if (endPeak != null) {
            if ((xMax - peakEnd) > params.maxRightExtremum) {
                // We have significant line chunks beyond bar, hence peak is not the limit
                logger.debug(
                        "Staff#{} RIGHT set at blank {} (vs {})",
                        staff.getId(),
                        xMax,
                        linesEnd);
                staff.setAbscissa(RIGHT, xMax);
            } else {
                // No significant line chunks, ignore them and stay with peak as the limit
                final int peakMid = (endPeak.getStart() + endPeak.getStop()) >>> 1;
                logger.debug(
                        "Staff#{} RIGHT set at peak {} (vs {})",
                        staff.getId(),
                        peakMid,
                        linesEnd);
                staff.setAbscissa(RIGHT, peakMid);
                endPeak.setStaffEnd(RIGHT);
            }
        } else {
            logger.debug("Staff#{} RIGHT set at blank {} (vs {})", staff.getId(), xMax, linesEnd);
            staff.setAbscissa(RIGHT, xMax);
        }
    }

    //------------//
    // removePeak //
    //------------//
    /**
     * Remove a peak from the sequence of peaks.
     *
     * @param peak the peak to remove
     */
    public void removePeak (StaffPeak peak)
    {
        if (peak.isVip()) {
            logger.info("VIP {} removing {}", this, peak);
        }

        peaks.remove(peak);
        peakGraph.removeVertex(peak);
    }

    //-------------//
    // removePeaks //
    //-------------//
    /**
     * Remove some peaks from the sequence of peaks.
     *
     * @param toRemove the peaks to remove
     */
    public void removePeaks (Collection<? extends StaffPeak> toRemove)
    {
        for (StaffPeak peak : toRemove) {
            removePeak(peak);
        }
    }

    //--------------//
    // setBracePeak //
    //--------------//
    /**
     * @param bracePeak the bracePeak to set
     */
    public void setBracePeak (StaffPeak bracePeak)
    {
        this.bracePeak = bracePeak;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "StaffProjector#" + staff.getId();
    }

    //-------------//
    // browseRange //
    //-------------//
    /**
     * (Try to) create one or more relevant peaks at provided range.
     * <p>
     * This is governed by derivative peaks.
     * For the time being, this is just a wrapper on top of createPeak meant to address the case of
     * wide ranges above the bar threshold, which need to be further split.
     *
     * @param rangeStart starting abscissa of range
     * @param rangeStop  stopping abscissa of range
     * @return the sequence of created peak instances, perhaps empty
     */
    private List<StaffPeak> browseRange (final int rangeStart,
                                         final int rangeStop)
    {
        logger.debug("Staff#{} browseRange [{}..{}]", staff.getId(), rangeStart, rangeStop);

        final List<StaffPeak> list = new ArrayList<StaffPeak>();
        int start = rangeStart;
        int stop;

        for (int x = rangeStart; x <= rangeStop; x++) {
            final int der = projection.getDerivative(x);

            if (der >= params.minDerivative) {
                int maxDer = der;

                for (int xx = x + 1; xx <= rangeStop; xx++) {
                    int xxDer = projection.getDerivative(xx);

                    if (xxDer > maxDer) {
                        maxDer = xxDer;
                        x = xx;
                    } else {
                        break;
                    }
                }

                start = x;
            } else if (der <= -params.minDerivative) {
                int minDer = der;

                for (int xx = x + 1; xx <= xClamp(rangeStop + 1); xx++) {
                    int xxDer = projection.getDerivative(xx);

                    if (xxDer <= minDer) {
                        minDer = xxDer;
                        x = xx;
                    } else {
                        break;
                    }
                }

                if (x == rangeStop) {
                    x = rangeStop + 1;
                }

                stop = x;

                if ((start != -1) && (start < stop)) {
                    StaffPeak peak = createPeak(start, stop - 1);

                    if (peak != null) {
                        list.add(peak);
                    }

                    start = -1;
                }
            }
        }

        // A last peak?
        if (start != -1) {
            StaffPeak peak = createPeak(start, rangeStop);

            if (peak != null) {
                list.add(peak);
            }
        }

        return list;
    }

    //---------------------------//
    // computeCoreLinesThickness //
    //---------------------------//
    /**
     * Using the current definition on staff lines (made of only long filaments so far)
     * estimate the cumulated staff line thicknesses for the staff.
     * <p>
     * NOTA: Since we may have holes in lines, and short sections have been left apart, the
     * measurement is under-estimated.
     *
     * @return the estimate of cumulated lines heights
     */
    private double computeCoreLinesThickness ()
    {
        double linesHeight = 0;

        for (LineInfo line : staff.getLines()) {
            linesHeight += line.getThickness();
        }

        logger.debug("Staff#{} linesHeight: {}", staff.getId(), linesHeight);

        return linesHeight;
    }

    //-----------------------//
    // computeLineThresholds //
    //-----------------------//
    /**
     * Compute thresholds that closely depend on actual line thickness in this staff.
     */
    private void computeLineThresholds ()
    {
        final double linesCumul = computeCoreLinesThickness();
        final double lineThickness = linesCumul / staff.getLines().size();

        params.linesThreshold = (int) Math.rint(linesCumul);
        params.blankThreshold = (int) Math.rint(
                constants.blankThreshold.getValue() * lineThickness);
        params.chunkThreshold = (4 * scale.getMaxFore())
                                + InterlineScale.toPixels(
                        staff.getSpecificInterline(),
                        constants.chunkThreshold);
        logger.debug(
                "Staff#{} linesThreshold:{} chunkThreshold:{}",
                staff.getId(),
                params.linesThreshold,
                params.chunkThreshold);
    }

    //-------------------//
    // computeProjection //
    //-------------------//
    /**
     * Compute, for each abscissa value, the foreground pixels cumulated between
     * first line and last line of staff.
     */
    private void computeProjection ()
    {
        projection = new Projection.Short(0, sheet.getWidth() - 1);

        final LineInfo firstLine = staff.getFirstLine();
        final LineInfo lastLine = staff.getLastLine();
        final int dx = params.staffAbscissaMargin;
        final int xMin = xClamp(staff.getAbscissa(LEFT) - dx);
        final int xMax = xClamp(staff.getAbscissa(RIGHT) + dx);

        for (int x = xMin; x <= xMax; x++) {
            int yMin = firstLine.yAt(x);
            int yMax = lastLine.yAt(x);
            short count = 0;

            for (int y = yMin; y <= yMax; y++) {
                if (pixelFilter.get(x, y) == 0) {
                    count++;
                }
            }

            projection.increment(x, count);
        }
    }

    //-----------------//
    // createBracePeak //
    //-----------------//
    /**
     * Precisely define the bounds of a brace candidate peak.
     *
     * @param rawStart starting abscissa at peak threshold
     * @param rawStop  stopping abscissa at peak threshold
     * @param maxRight maximum abscissa on right
     * @return a peak with proper abscissa values, or null
     */
    private StaffPeak createBracePeak (int rawStart,
                                       int rawStop,
                                       int maxRight)
    {
        // Extend left abscissa until a blank (no-staff) or image left side is reached
        Blank leftBlank = null;

        for (Blank blank : allBlanks) {
            if (blank.stop >= rawStart) {
                break;
            }

            leftBlank = blank;
        }

        int start = (leftBlank != null) ? leftBlank.stop : rawStart;
        int val = projection.getValue(start);

        for (int x = start - 1; x >= 0; x--) {
            int nextVal = projection.getValue(x);

            if (nextVal < val) {
                val = nextVal;
                start = x;
            } else {
                break;
            }
        }

        // Perhaps there is no real blank between brace and bar, so use lowest point in valley
        int bestVal = Integer.MAX_VALUE;

        int stop = -1;

        for (int x = rawStop; x <= maxRight; x++) {
            val = projection.getValue(x);

            if (val < bestVal) {
                bestVal = val;
                stop = x;
            }
        }

        if (stop == -1) {
            return null;
        }

        final int xMid = (start + stop) / 2;

        final int yTop = staff.getFirstLine().yAt(xMid);

        final int yBottom = staff.getLastLine().yAt(xMid);

        StaffPeak brace = new StaffPeak(staff, yTop, yBottom, start, stop, null);

        brace.set(Attribute.BRACE);

        brace.computeDeskewedCenter(sheet.getSkew());

        return brace;
    }

    //------------//
    // createPeak //
    //------------//
    /**
     * (Try to) create a relevant peak at provided location.
     *
     * @param rawStart raw starting abscissa of peak
     * @param rawStop  raw stopping abscissa of peak
     * @return the created peak instance or null if failed
     */
    private StaffPeak createPeak (final int rawStart,
                                  final int rawStop)
    {
        final int minValue = params.barThreshold;
        final int totalHeight = 4 * staff.getSpecificInterline();
        final double valueRange = totalHeight - minValue;

        // Compute precise start & stop abscissae
        PeakSide newStart = refinePeakSide(rawStart, rawStop, -1);

        if (newStart == null) {
            return null;
        }

        final int start = newStart.abscissa;
        PeakSide newStop = refinePeakSide(rawStart, rawStop, +1);

        if (newStop == null) {
            return null;
        }

        final int stop = newStop.abscissa;

        // Check peak width is not huge
        if ((stop - start + 1) > params.maxBarWidth) {
            return null;
        }

        // Retrieve highest value
        int value = 0;

        for (int x = start; x <= stop; x++) {
            value = Math.max(value, projection.getValue(x));
        }

        // Compute largest white gap
        final int xMid = (start + stop) / 2;
        final int yTop = staff.getFirstLine().yAt(xMid);
        final int yBottom = staff.getLastLine().yAt(xMid);

        // If peak is very thin, thicken the lookup area
        final int width = stop - start + 1;
        final int dx = (width <= 2) ? 1 : 0;
        GeoPath leftLine = new GeoPath(
                new Line2D.Double(start - dx, yTop, start - dx, yBottom));
        GeoPath rightLine = new GeoPath(
                new Line2D.Double(stop + dx, yTop, stop + dx, yBottom));
        final CoreData data = AreaUtil.verticalCore(pixelFilter, leftLine, rightLine);

        if (data.gap > params.gapThreshold) {
            return null;
        }

        // Compute black core & impacts
        double coreImpact = (value - minValue) / valueRange;
        double gapImpact = 1 - ((double) data.gap / params.gapThreshold);
        GradeImpacts impacts = new BarlineInter.Impacts(
                coreImpact,
                gapImpact,
                newStart.grade,
                newStop.grade);
        double grade = impacts.getGrade();

        if (grade >= Grades.minInterGrade) {
            StaffPeak bar = new StaffPeak(staff, yTop, yBottom, start, stop, impacts);
            bar.computeDeskewedCenter(sheet.getSkew());
            logger.debug("Staff#{} {}", staff.getId(), bar);

            return bar;
        }

        return null;
    }

    //---------------//
    // findAllBlanks //
    //---------------//
    /**
     * Look for all "blank" regions (regions without staff lines).
     */
    private void findAllBlanks ()
    {
        final int maxValue = params.blankThreshold;
        final int sheetWidth = sheet.getWidth();

        int start = -1;
        int stop = -1;

        for (int x = 0; x < sheetWidth; x++) {
            if (projection.getValue(x) <= maxValue) {
                // No line detected
                if (start == -1) {
                    start = x;
                }

                stop = x;
            } else if (start != -1) {
                allBlanks.add(new Blank(start, stop));
                start = -1;
            }
        }

        // Finish ongoing region if any
        if (start != -1) {
            allBlanks.add(new Blank(start, stop));
        }

        logger.debug(
                "Staff#{} left:{} right:{} allBlanks:{}",
                staff.getId(),
                staff.getAbscissa(LEFT),
                staff.getAbscissa(RIGHT),
                allBlanks);
    }

    //-----------//
    // findPeaks //
    //-----------//
    /**
     * Retrieve the relevant (bar line) peaks in the staff projection.
     * This populates the 'peaks' sequence.
     */
    private void findPeaks ()
    {
        final int minValue = params.barThreshold;

        final Blank leftBlank = endingBlanks.get(LEFT);
        final int xMin = (leftBlank != null) ? leftBlank.stop : 0;

        final Blank rightBlank = endingBlanks.get(RIGHT);
        final int xMax = (rightBlank != null) ? rightBlank.start : (sheet.getWidth() - 1);

        int start = -1;
        int stop = -1;

        for (int x = xMin; x <= xMax; x++) {
            int value = projection.getValue(x);

            if (value >= minValue) {
                if (start == -1) {
                    start = x;
                }

                stop = x;
            } else if (start != -1) {
                for (StaffPeak peak : browseRange(start, stop)) {
                    peaks.add(peak);
                    peakGraph.addVertex(peak);

                    // Make sure peaks do not overlap
                    x = Math.max(x, peak.getStop());
                }

                start = -1;
            }
        }

        // Finish ongoing peak if any (case of a peak stuck to right side of image)
        if (start != -1) {
            StaffPeak peak = createPeak(start, stop);

            if (peak != null) {
                peaks.add(peak);
                peakGraph.addVertex(peak);
            }
        }

        logger.debug("Staff#{} peaks:{}", staff.getId(), peaks);
    }

    //----------------//
    // refinePeakSide //
    //----------------//
    /**
     * Use extrema of first derivative to refine peak side abscissa.
     * Maximum for left side, minimum for right side.
     * Absolute derivative value indicates if the peak side is really steep: this should exclude
     * most of: braces, arpeggiato, stems with heads on left or right side.
     * <p>
     * Update: the test on derivative absolute value is not sufficient to discard braces.
     *
     * @param xStart raw abscissa that starts peak
     * @param xStop  raw abscissa that stops peak
     * @param dir    -1 for going left, +1 for going right
     * @return the best peak side, or null if none
     */
    private PeakSide refinePeakSide (int xStart,
                                     int xStop,
                                     int dir)
    {
        // Additional check range
        final int dx = params.barRefineDx;

        // Beginning and ending x values
        final double mid = (xStop + xStart) / 2.0;
        final int x1 = (dir > 0) ? (int) Math.ceil(mid) : (int) Math.floor(mid);
        final int x2 = (dir > 0) ? xClamp(xStop + dx) : xClamp(xStart - dx);

        int bestDer = 0; // Best derivative so far
        Integer bestX = null; // Abscissa at best derivative

        for (int x = x1; (dir * (x2 - x)) >= 0; x += dir) {
            final int der = projection.getDerivative(x);

            if ((dir * (bestDer - der)) > 0) {
                bestDer = der;
                bestX = x;
            }
        }

        bestDer = Math.abs(bestDer);

        if ((bestDer >= params.minDerivative) && (bestX != null)) {
            int x = (dir > 0) ? (bestX - 1) : bestX;
            double derImpact = (double) bestDer / (params.barThreshold - params.minDerivative);

            return new PeakSide(x, derImpact);
        } else {
            // Perhaps we have reached image border?
            int border = (dir > 0) ? (sheet.getWidth() - 1) : 0;

            if (x2 == border) {
                final int der = projection.getValue(border);

                if (der >= params.minDerivative) {
                    double derImpact = (double) der / (params.barThreshold - params.minDerivative);

                    return new PeakSide(border, derImpact);
                }
            }

            return null; // Invalid
        }
    }

    //-------------//
    // selectBlank //
    //-------------//
    /**
     * Report the relevant blank region on desired staff side.
     * <p>
     * We try to pick up a wide enough region if any.
     * <p>
     * TODO: The selection could be revised in a second phase performed at sheet level, since
     * poor-quality staves may exhibit abnormal blank regions.
     *
     * @param side     desired side
     * @param start    abscissa for starting search
     * @param minWidth minimum blank width
     * @return the relevant blank region found, null if none was found
     */
    private Blank selectBlank (HorizontalSide side,
                               int start,
                               int minWidth)
    {
        final int dir = (side == LEFT) ? (-1) : 1;
        final int rInit = (side == LEFT) ? (allBlanks.size() - 1) : 0;
        final int rBreak = (side == LEFT) ? (-1) : allBlanks.size();

        for (int ir = rInit; ir != rBreak; ir += dir) {
            Blank blank = allBlanks.get(ir);
            int mid = (blank.start + blank.stop) / 2;

            // Make sure we are on desired side of the staff
            if ((dir * (mid - start)) > 0) {
                int width = blank.getWidth();

                // Stop on first significant blank
                if (width >= minWidth) {
                    return blank;
                }
            }
        }

        return null;
    }

    //--------------------//
    // selectEndingBlanks //
    //--------------------//
    /**
     * Select the pair of ending blanks that limit peak search.
     */
    private void selectEndingBlanks ()
    {
        if (allBlanks.isEmpty()) {
            return;
        }

        for (HorizontalSide side : HorizontalSide.values()) {
            // Look for the first really wide blank encountered
            Blank blank = selectBlank(side, staff.getAbscissa(side), params.minWideBlankWidth);

            if (blank != null) {
                endingBlanks.put(side, blank);
            }
        }

        logger.debug("Staff#{} endingBlanks:{}", staff.getId(), endingBlanks);
    }

    //--------//
    // xClamp //
    //--------//
    /**
     * Clamp the provided abscissa value within legal values that
     * are precisely [0..sheet.getWidth() -1].
     *
     * @param x the abscissa to clamp
     * @return the clamped abscissa
     */
    private int xClamp (int x)
    {
        if (x < 0) {
            return 0;
        }

        if (x > (sheet.getWidth() - 1)) {
            return sheet.getWidth() - 1;
        }

        return x;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // PeakSide //
    //----------//
    /**
     * Describes the (left or right) side of a peak.
     */
    static class PeakSide
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Precise side abscissa. */
        final int abscissa;

        /** Quality based on derivative absolute value. */
        final double grade;

        //~ Constructors ---------------------------------------------------------------------------
        public PeakSide (int abscissa,
                         double grade)
        {
            this.abscissa = abscissa;
            this.grade = grade;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction staffAbscissaMargin = new Scale.Fraction(
                15,
                "Abscissa margin for checks around staff");

        private final Scale.Fraction barChunkDx = new Scale.Fraction(
                0.4,
                "Abscissa margin for chunks check around bar");

        private final Scale.Fraction barRefineDx = new Scale.Fraction(
                0.25,
                "Abscissa margin for refining peak sides");

        private final Scale.Fraction minDerivative = new Scale.Fraction(
                0.4,
                "Minimum absolute derivative for peak side");

        private final Scale.Fraction barThreshold = new Scale.Fraction(
                2.5,
                "Minimum cumul value to detect bar peak");

        private final Scale.Fraction braceThreshold = new Scale.Fraction(
                1.1,
                "Minimum cumul value to detect brace peak");

        private final Scale.Fraction gapThreshold = new Scale.Fraction(
                0.85,
                "Maximum vertical gap length in a bar");

        private final Scale.Fraction chunkThreshold = new Scale.Fraction(
                0.8,
                "Maximum cumul value to detect chunk (on top of lines)");

        private final Scale.LineFraction blankThreshold = new Scale.LineFraction(
                2.5,
                "Maximum cumul value (in LineFraction) to detect no-line regions");

        private final Scale.Fraction minSmallBlankWidth = new Scale.Fraction(
                0.1,
                "Minimum width for a small blank region (right of lines)");

        private final Scale.Fraction minStandardBlankWidth = new Scale.Fraction(
                1.0,
                "Minimum width for a standard blank region (left of lines)");

        private final Scale.Fraction minWideBlankWidth = new Scale.Fraction(
                2.0,
                "Minimum width for a wide blank region (to limit peaks search)");

        private final Scale.Fraction maxBarWidth = new Scale.Fraction(1.5, "Maximum bar width");

        private final Scale.Fraction maxLeftExtremum = new Scale.Fraction(
                0.15,
                "Maximum length between actual lines left end and left ending bar");

        private final Scale.Fraction maxRightExtremum = new Scale.Fraction(
                0.3,
                "Maximum length between right ending bar and actual lines right end");
    }

    //-------//
    // Blank //
    //-------//
    /**
     * An abscissa region where no staff lines are detected and thus indicates possible
     * end of staff.
     */
    private static class Blank
            implements Comparable<Blank>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** First abscissa in region. */
        private final int start;

        /** Last abscissa in region. */
        private final int stop;

        //~ Constructors ---------------------------------------------------------------------------
        public Blank (int start,
                      int stop)
        {
            this.start = start;
            this.stop = stop;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int compareTo (Blank that)
        {
            // This is a total ordering of blanks (within the same staff projection)
            return Integer.compare(this.start, that.start);
        }

        @Override
        public boolean equals (Object obj)
        {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Blank) {
                return compareTo((Blank) obj) == 0;
            }

            return false;
        }

        public int getWidth ()
        {
            return stop - start + 1;
        }

        @Override
        public int hashCode ()
        {
            int hash = 3;
            hash = (79 * hash) + this.start;

            return hash;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Blank(").append(start).append("-").append(stop).append(")");

            return sb.toString();
        }
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int staffAbscissaMargin;

        final int barChunkDx;

        final int barRefineDx;

        final int minSmallBlankWidth;

        final int minStandardBlankWidth;

        final int minWideBlankWidth;

        final int maxBarWidth;

        final int maxLeftExtremum;

        final int maxRightExtremum;

        // Following thresholds depend of staff (specific?) interline scale
        final int minDerivative;

        final int barThreshold;

        final int braceThreshold;

        final int gapThreshold;

        // Following thresholds depend on actual line height within this staff
        int linesThreshold;

        int blankThreshold;

        int chunkThreshold;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale,
                           int staffSpecific)
        {
            {
                // Use sheet large interline value
                final InterlineScale large = scale.getInterlineScale();
                staffAbscissaMargin = large.toPixels(constants.staffAbscissaMargin);
                barChunkDx = large.toPixels(constants.barChunkDx);
                barRefineDx = large.toPixels(constants.barRefineDx);
                minSmallBlankWidth = large.toPixels(constants.minSmallBlankWidth);
                minStandardBlankWidth = large.toPixels(constants.minStandardBlankWidth);
                minWideBlankWidth = large.toPixels(constants.minWideBlankWidth);
                maxBarWidth = large.toPixels(constants.maxBarWidth);
                maxLeftExtremum = large.toPixels(constants.maxLeftExtremum);
                maxRightExtremum = large.toPixels(constants.maxRightExtremum);
            }

            {
                // Use staff specific interline value
                final InterlineScale specific = scale.getInterlineScale(staffSpecific);
                minDerivative = specific.toPixels(constants.minDerivative);
                barThreshold = specific.toPixels(constants.barThreshold);
                braceThreshold = specific.toPixels(constants.braceThreshold);
                gapThreshold = specific.toPixels(constants.gapThreshold);
            }
        }
    }

    //---------//
    // Plotter //
    //---------//
    /**
     * Handles the display of projection chart.
     */
    private class Plotter
    {
        //~ Instance fields ------------------------------------------------------------------------

        final XYSeriesCollection dataset = new XYSeriesCollection();

        // Chart
        final JFreeChart chart = ChartFactory.createXYLineChart(
                sheet.getId() + " staff#" + getStaff().getId(), // Title
                "Abscissae - staff interline:" + staff.getSpecificInterline(), // X-Axis label
                "Counts", // Y-Axis label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // orientation,
                true, // Show legend
                false, // Show tool tips
                false // urls
        );

        final XYPlot plot = (XYPlot) chart.getPlot();

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Series index
        int index = -1;

        //~ Methods --------------------------------------------------------------------------------
        public void plot ()
        {
            plot.setRenderer(renderer);

            final int xMin = 0;
            final int xMax = sheet.getWidth() - 1;

            {
                // Values
                XYSeries valueSeries = new XYSeries("Cumuls", false); // No autosort

                for (int x = xMin; x <= xMax; x++) {
                    valueSeries.add(x, projection.getValue(x));
                }

                add(valueSeries, Colors.CHART_VALUE, false);
            }

            {
                // Derivatives
                XYSeries derivativeSeries = new XYSeries("Derivatives", false); // No autosort

                for (int x = xMin; x <= xMax; x++) {
                    derivativeSeries.add(x, projection.getDerivative(x));
                }

                add(derivativeSeries, Colors.CHART_DERIVATIVE, false);
            }

            {
                // Derivatives positive threshold
                XYSeries derSeries = new XYSeries("Der+", false); // No autosort

                derSeries.add(xMin, params.minDerivative);
                derSeries.add(xMax, params.minDerivative);
                add(derSeries, Colors.CHART_DERIVATIVE, false);
            }

            {
                // Derivatives negative threshold
                XYSeries derSeries = new XYSeries("Der-", false); // No autosort

                derSeries.add(xMin, -params.minDerivative);
                derSeries.add(xMax, -params.minDerivative);
                add(derSeries, Colors.CHART_DERIVATIVE, false);
            }

            {
                // Theoretical staff height (assuming a 5-line staff)
                XYSeries heightSeries = new XYSeries("StaffHeight", false); // No autosort
                int totalHeight = 4 * staff.getSpecificInterline();
                heightSeries.add(xMin, totalHeight);
                heightSeries.add(xMax, totalHeight);
                add(heightSeries, Color.BLACK, true);
            }

            {
                // BarPeak min threshold
                XYSeries minSeries = new XYSeries("MinBar", false); // No autosort
                minSeries.add(xMin, params.barThreshold);
                minSeries.add(xMax, params.barThreshold);
                add(minSeries, Color.GREEN, true);
            }

            {
                // Chunk threshold (assuming a 5-line staff)
                XYSeries chunkSeries = new XYSeries("MaxChunk", false); // No autosort
                chunkSeries.add(xMin, params.chunkThreshold);
                chunkSeries.add(xMax, params.chunkThreshold);
                add(chunkSeries, Color.YELLOW, true);
            }

            {
                // BracePeak min threshold
                XYSeries minSeries = new XYSeries("MinBrace", false); // No autosort
                minSeries.add(xMin, params.braceThreshold);
                minSeries.add(xMax, params.braceThreshold);
                add(minSeries, Color.ORANGE, true);
            }

            {
                // Cumulated staff lines (assuming a 5-line staff)
                XYSeries linesSeries = new XYSeries("Lines", false); // No autosort
                linesSeries.add(xMin, params.linesThreshold);
                linesSeries.add(xMax, params.linesThreshold);
                add(linesSeries, Color.MAGENTA, true);
            }

            {
                // Threshold for no staff
                final int nostaff = params.blankThreshold;
                XYSeries holeSeries = new XYSeries("NoStaff", false); // No autosort
                holeSeries.add(xMin, nostaff);
                holeSeries.add(xMax, nostaff);
                add(holeSeries, Color.CYAN, true);
            }

            {
                // Zero
                XYSeries zeroSeries = new XYSeries("Zero", false); // No autosort
                zeroSeries.add(xMin, 0);
                zeroSeries.add(xMax, 0);
                add(zeroSeries, Colors.CHART_ZERO, true);
            }

            // Hosting frame
            ChartFrame frame = new ChartFrame(
                    sheet.getId() + " staff#" + getStaff().getId(),
                    chart,
                    true);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLocation(new Point(20 * getStaff().getId(), 20 * getStaff().getId()));
            frame.setVisible(true);
        }

        private void add (XYSeries series,
                          Color color,
                          boolean displayShapes)
        {
            dataset.addSeries(series);
            renderer.setSeriesPaint(++index, color);
            renderer.setSeriesShapesVisible(index, displayShapes);
        }
    }
}
