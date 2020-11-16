//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t a f f P r o j e c t o r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
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
import org.audiveris.omr.util.ChartPlotter;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Navigable;

import org.jfree.data.xy.XYSeries;

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

/**
 * Class {@code StaffProjector} is in charge of analyzing a staff projection onto
 * x-axis, in order to retrieve barlines candidates as well as staff start and stop
 * abscissae.
 * <p>
 * To retrieve bar lines candidates, we analyze the vertical interior of staff because this is where
 * a barline must be present.
 * The potential bar portions outside staff height are much less typical of a barline.
 * <p>
 * A peak in staff projection can result from:
 * <ol>
 * <li>A thick or thin <b>bar line</b>:<br>
 * <img alt="Image of bar lines"
 * src=
 * "http://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/Barlines.svg/400px-Barlines.svg.png">
 * <p>
 * or
 * <li>A <b>bracket</b> portion:<br>
 * <img alt="Image of bracket" width="250" height="216"
 * src="http://donrathjr.com/wp-content/uploads/2010/08/Brackets-and-Braces-4a.png">
 * <p>
 * or
 * <li>A <b>brace</b> portion:<br>
 * <img alt="Image of brace"
 * src=
 * "http://upload.wikimedia.org/wikipedia/commons/thumb/2/28/Brace_(music).png/240px-Brace_(music).png">
 * <p>
 * or
 * <li>An Alto <b>C-clef</b> portion:<br>
 * <img alt="Image of alto clef"
 * src=
 * "http://upload.wikimedia.org/wikipedia/commons/thumb/6/68/Alto_clef_with_ref.svg/90px-Alto_clef_with_ref.svg.png">
 * <br>
 * Such C-clef artifacts are detected later, based on their abscissa offset from the measure
 * start (be it barline-based start or lines-only start).
 * <p>
 * or
 * <li>A <b>stem</b> (with note heads located outside the staff height).
 * <p>
 * or
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

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StaffProjector.class);

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
    private final List<Blank> allBlanks = new ArrayList<>();

    /** Selected (wide) ending blank region on each staff side. */
    private final Map<HorizontalSide, Blank> endingBlanks = new EnumMap<>(HorizontalSide.class);

    /** Sequence of peaks found. */
    private final List<StaffPeak> peaks = new ArrayList<>();

    /** (Unmodifiable) view on peaks. */
    private final List<StaffPeak> peaksView = Collections.unmodifiableList(peaks);

    /** Graph of all peaks, linked by alignment/connection. */
    private final Graph<StaffPeak, BarAlignment> peakGraph;

    /** Count of cumulated foreground pixels, indexed by abscissa. */
    private Projection projection;

    /** Minimum absolute derivative to detect peak sides. */
    private int derivativeThreshold;

    /** Initial brace peak, if any. */
    private StaffPeak bracePeak;

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
        params = new Parameters(sheet, staff.getSpecificInterline());
    }

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

    //--------------//
    // setBracePeak //
    //--------------//
    /**
     * Assign the brace peak.
     *
     * @param bracePeak the bracePeak to set
     */
    public void setBracePeak (StaffPeak bracePeak)
    {
        this.bracePeak = bracePeak;
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

        final String title = sheet.getId() + " staff#" + staff.getId();
        final ChartPlotter plotter = new ChartPlotter(
                title, // Title
                "Abscissae - "
                        + staff.getClass().getSimpleName()
                        + " lines:" + staff.getLineCount()
                        + " interline:" + staff.getSpecificInterline(), // X-Axis label
                "Counts"); // Y-Axis label

        final int xMin = 0;
        final int xMax = sheet.getWidth() - 1;

        {
            // Values
            XYSeries valueSeries = new XYSeries("Cumuls", false); // No autosort

            for (int x = xMin; x <= xMax; x++) {
                valueSeries.add(x, projection.getValue(x));
            }

            plotter.add(valueSeries, Colors.CHART_VALUE, false);
        }

        {
            // Derivatives
            XYSeries derivativeSeries = new XYSeries("Derivatives", false); // No autosort

            for (int x = xMin; x <= xMax; x++) {
                derivativeSeries.add(x, projection.getDerivative(x));
            }

            plotter.add(derivativeSeries, Colors.CHART_DERIVATIVE, false);
        }

        {
            // Derivatives positive threshold
            XYSeries derSeries = new XYSeries("Der+", false); // No autosort

            derSeries.add(xMin, derivativeThreshold);
            derSeries.add(xMax, derivativeThreshold);
            plotter.add(derSeries, Colors.CHART_DERIVATIVE, false);
        }

        {
            // Derivatives negative threshold
            XYSeries derSeries = new XYSeries("Der-", false); // No autosort

            derSeries.add(xMin, -derivativeThreshold);
            derSeries.add(xMax, -derivativeThreshold);
            plotter.add(derSeries, Colors.CHART_DERIVATIVE, false);
        }

        {
            // Theoretical staff height (adapted to staff line count)
            XYSeries heightSeries = new XYSeries("StaffHeight", false); // No autosort
            int n = staff.getLineCount();
            int totalHeight = staff.getSpecificInterline() * (n > 1 ? n - 1 : 4);
            heightSeries.add(xMin, totalHeight);
            heightSeries.add(xMax, totalHeight);
            plotter.add(heightSeries, Color.BLACK, true);
        }

        {
            // BarPeak min threshold
            XYSeries minSeries = new XYSeries("MinBar", false); // No autosort
            minSeries.add(xMin, params.barThreshold);
            minSeries.add(xMax, params.barThreshold);
            plotter.add(minSeries, Color.GREEN, true);
        }

        {
            // Chunk threshold (adapted to staff line count)
            XYSeries chunkSeries = new XYSeries("MaxChunk", false); // No autosort
            chunkSeries.add(xMin, params.chunkThreshold);
            chunkSeries.add(xMax, params.chunkThreshold);
            plotter.add(chunkSeries, Color.YELLOW, true);
        }

        {
            // BracePeak min threshold
            XYSeries minSeries = new XYSeries("MinBrace", false); // No autosort
            minSeries.add(xMin, params.braceThreshold);
            minSeries.add(xMax / 10, params.braceThreshold); // (limited to left part of staff)
            plotter.add(minSeries, Color.ORANGE, true);
        }

        {
            // Cumulated staff lines (adapted to staff line count)
            XYSeries linesSeries = new XYSeries("Lines", false); // No autosort
            linesSeries.add(xMin, params.linesThreshold);
            linesSeries.add(xMax, params.linesThreshold);
            plotter.add(linesSeries, Color.MAGENTA, true);
        }

        {
            // Threshold for no staff (adapted to staff line count)
            final int nostaff = params.blankThreshold;
            XYSeries holeSeries = new XYSeries("NoStaff", false); // No autosort
            holeSeries.add(xMin, nostaff);
            holeSeries.add(xMax, nostaff);
            plotter.add(holeSeries, Color.CYAN, true);
        }

        {
            // Zero
            XYSeries zeroSeries = new XYSeries("Zero", false); // No autosort
            zeroSeries.add(xMin, 0);
            zeroSeries.add(xMax, 0);
            plotter.add(zeroSeries, Colors.CHART_ZERO, true);
        }

        // Display frame
        plotter.display(title, new Point(20 * staff.getId(), 20 * staff.getId()));

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
     * @param halfMode   true when dealing with initial peak of a OneLineStaff
     * @return the sequence of created peak instances, perhaps empty
     */
    private List<StaffPeak> browseRange (final int rangeStart,
                                         final int rangeStop,
                                         final boolean halfMode)
    {
        logger.debug("Staff#{} browseRange [{}..{}]", staff.getId(), rangeStart, rangeStop);

        final int minDerivative = halfMode ? derivativeThreshold / 2 : derivativeThreshold;
        final List<StaffPeak> list = new ArrayList<>();
        int start = rangeStart;
        int stop;

        for (int x = rangeStart; x <= rangeStop; x++) {
            final int der = projection.getDerivative(x);

            if (der >= minDerivative) {
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
            } else if (der <= -minDerivative) {
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
                    StaffPeak peak = createPeak(start, stop - 1, halfMode);

                    if (peak != null) {
                        list.add(peak);
                    }

                    start = -1;
                }
            }
        }

        // A last peak?
        if (start != -1) {
            StaffPeak peak = createPeak(start, rangeStop, halfMode);

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
     * estimate the cumulated staff line thicknesses for the staff projection.
     * <p>
     * NOTA: Since we may have holes in lines, and short sections have been left apart, the
     * measurement is under-estimated.
     *
     * @return the estimate of cumulated lines heights
     */
    private double computeCoreLinesThickness ()
    {
        double cumul = 0;

        for (LineInfo line : staff.getLines()) {
            cumul += line.getThickness();
        }

        final int n = staff.getLineCount();
        if (n > 1) {
            cumul *= (n - 1) / (double) n;
        }

        logger.debug("Staff#{} linesHeight: {}", staff.getId(), cumul);

        return cumul;
    }

    //-----------------------//
    // computeLineThresholds //
    //-----------------------//
    /**
     * Compute thresholds that closely depend on actual line thickness and on actual
     * number of lines in this staff.
     */
    private void computeLineThresholds ()
    {
        final int lineCount = staff.getLines().size();
        final double linesCumul = computeCoreLinesThickness();

        // blankThreshold is used for plots and for detecting regions without staff lines.
        // It is meant to detect absence of lines
        params.blankThreshold = (int) Math.rint(constants.blankThreshold.getValue() * linesCumul);

        // linesThreshold is used for plots and for detecting chunks
        params.linesThreshold = (int) Math.rint(linesCumul);

        // chunkThreshold is used for plots and for detecting chunks of objects over lines
        params.chunkThreshold = ((lineCount - 1) * scale.getFore())
                                        + InterlineScale.toPixels(staff.getSpecificInterline(),
                                                                  constants.chunkThreshold);
        logger.debug(
                "Staff#{} thresholds blank:{} lines:{} chunk:{}",
                staff.getId(),
                params.blankThreshold,
                params.linesThreshold,
                params.chunkThreshold);
    }

    //-------------------//
    // computeProjection //
    //-------------------//
    /**
     * Compute, for each abscissa value, the foreground pixels cumulated between
     * first line and last line of staff.
     * <p>
     * For a OneLineStaff, we extrapolate ordinates based on a standard 5-line staff.
     * <p>
     * We also compute derivative threshold for this staff.
     */
    private void computeProjection ()
    {
        projection = new Projection.Short(0, sheet.getWidth() - 1);

        final ArrayList<Integer> derivatives = new ArrayList<>();
        final LineInfo firstLine = staff.getFirstLine();
        final LineInfo lastLine = staff.getLastLine();
        final int dx = params.staffAbscissaMargin;
        final int xMin = xClamp(staff.getAbscissa(LEFT) - dx);
        final int xMax = xClamp(staff.getAbscissa(RIGHT) + dx);

        // Correction for ordinates of a 1-line staff
        final int dy = staff.isOneLineStaff() ? 2 * scale.getInterline() : 0;

        // Populating projection data
        for (int x = xMin; x <= xMax; x++) {
            int yMin = firstLine.yAt(x) - dy;
            int yMax = lastLine.yAt(x) - 1 + dy;
            short count = 0;

            for (int y = yMin; y <= yMax; y++) {
                if (pixelFilter.get(x, y) == 0) {
                    count++;
                }
            }

            projection.increment(x, count);

            if (x > xMin) {
                derivatives.add(Math.abs(projection.getDerivative(x)));
            }
        }

        // Computing minDerivative from observed top values
        Collections.sort(derivatives);
        final int size = derivatives.size();
        final int top = constants.topDerivativeNumber.getValue();
        int derCumul = 0;

        for (int i = 1; i <= top; i++) {
            derCumul += derivatives.get(size - i);
        }

        final double eliteDer = (double) derCumul / top;
        derivativeThreshold = (int) Math.rint(eliteDer * constants.minDerivativeRatio.getValue());
        logger.debug("eliteDerivative:{} derivativeThreshold:{} ", eliteDer, derivativeThreshold);
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
     * @param halfMode true when dealing with initial peak of a OneLineStaff
     * @return the created peak instance or null if failed
     */
    private StaffPeak createPeak (final int rawStart,
                                  final int rawStop,
                                  final boolean halfMode)
    {
        final int minValue = halfMode ? params.barThreshold / 2 : params.barThreshold;
        final int totalHeight = staff.getSpecificInterline() * (staff.isOneLineStaff()
                ? 4 : staff.getLineCount() - 1);
        final double valueRange = (halfMode ? totalHeight / 2 : totalHeight) - minValue;

        // Compute precise start & stop abscissae
        PeakSide newStart = refinePeakSide(rawStart, rawStop, -1, halfMode);

        if (newStart == null) {
            return null;
        }

        final int start = newStart.abscissa;
        PeakSide newStop = refinePeakSide(rawStart, rawStop, +1, halfMode);

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
        GeoPath leftLine = new GeoPath(new Line2D.Double(start - dx, yTop, start - dx, yBottom));
        GeoPath rightLine = new GeoPath(new Line2D.Double(stop + dx, yTop, stop + dx, yBottom));
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
                newStart.derGrade,
                newStop.derGrade,
                newStart.chunkGrade,
                newStop.chunkGrade);
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
     * <p>
     * In the specific case of a OneLineStaff, the starting barline may be a full-size barline
     * (when the staff is surrounded by other staves) or just a half-size barline (when the staff
     * is the first (?) or last in system/page).
     * So, beside the standard barThreshold we also use a 1/2 one (barThreshold/2) and check for
     * a new peak found before the first full-size peak.
     * If this new peak is really a half-size peak with all its pixels on one half of staff height,
     * then this new peak is inserted as the true first peak for the one-line staff.
     * The corresponding derivative threshold is also halved.
     */
    private void findPeaks ()
    {
        final boolean oneLine = staff.isOneLineStaff();

        final Blank leftBlank = endingBlanks.get(LEFT);
        final int xMin = (leftBlank != null) ? leftBlank.stop : 0;

        final Blank rightBlank = endingBlanks.get(RIGHT);
        final int xMax = (rightBlank != null) ? rightBlank.start : (sheet.getWidth() - 1);

        int start = -1;
        int stop = -1;
        boolean halfMode = false;

        for (int x = xMin; x <= xMax; x++) {
            final int value = projection.getValue(x);
            halfMode = oneLine && peaks.isEmpty();
            final int minBar = halfMode ? params.barThreshold / 2 : params.barThreshold;

            if (value >= minBar) {
                if (start == -1) {
                    start = x;
                }

                stop = x;
            } else if (start != -1) {
                for (StaffPeak peak : browseRange(start, stop, halfMode)) {
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
            StaffPeak peak = createPeak(start, stop, halfMode);

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
     * Update: the test on derivative absolute value is not sufficient to discard braces nor
     * stems with attached heads within staff height.
     * Thus, to detect attached heads, we add a test on cumulated black pixels just outside the
     * peak (the chunks).
     *
     * @param xStart   raw abscissa that starts peak
     * @param xStop    raw abscissa that stops peak
     * @param dir      -1 for going left, +1 for going right
     * @param halfMode when dealing with first peak of a OneLineStaff
     * @return the best peak side, or null if none
     */
    private PeakSide refinePeakSide (int xStart,
                                     int xStop,
                                     int dir,
                                     boolean halfMode)
    {
        final int minDerivative = halfMode ? derivativeThreshold / 2 : derivativeThreshold;
        final int minBar = halfMode ? params.barThreshold / 2 : params.barThreshold;
        final int minChunk = halfMode ? params.linesThreshold / 2 : params.linesThreshold;
        final int maxChunk = halfMode ? params.chunkThreshold / 2 : params.chunkThreshold;

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

        if ((bestDer >= minDerivative) && (bestX != null)) {
            final int x = (dir > 0) ? (bestX - 1) : bestX;
            final double derImpact = (double) bestDer / (minBar - minDerivative);

            // Presence of chunks next to peak?
            final int chunk = getChunk(x, dir);
            double chunkImpact = (chunk < minChunk) ? 1.0
                    : ((chunk > maxChunk) ? 0.0
                            : (double) (maxChunk - chunk) / (maxChunk - minChunk));

            return new PeakSide(x, derImpact, chunkImpact);
        } else {
            // Perhaps we have reached image border?
            int border = (dir > 0) ? (sheet.getWidth() - 1) : 0;

            if (x2 == border) {
                final int der = projection.getValue(border);

                if (der >= minDerivative) {
                    double derImpact = (double) der / (minBar - minDerivative);

                    return new PeakSide(border, derImpact, 1.0); // No chunk possible!
                }
            }

            return null; // Invalid
        }
    }

    //----------//
    // getChunk //
    //----------//
    /**
     * Report the minimum chunk cumulated pixels next to the provided abscissa.
     *
     * @param x0  provided abscissa (peak precise start or stop abscissa)
     * @param dir abscissa direction to check from x0
     * @return minimum chunk cumulated height on peak external side
     */
    private int getChunk (int x0,
                          int dir)
    {
        // Beginning of range, close to peak side
        final int x1 = x0 + dir * (1 + params.chunkOffset);

        // End of range, far from peak side
        final int x2 = x1 + dir * (params.chunkWidth - 1);

        // If outside of image, there can't be any chunk
        if (x2 < 0 || x2 > sheet.getWidth() - 1) {
            return 0;
        }

        int chunk = Integer.MAX_VALUE;

        for (int x = x1; dir * (x2 - x) >= 0; x += dir) {
            chunk = Math.min(chunk, projection.getValue(x));
        }

        return chunk;
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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction staffAbscissaMargin = new Scale.Fraction(
                15,
                "Abscissa margin for checks around staff");

        private final Scale.Fraction barChunkDx = new Scale.Fraction(
                0.4,
                "Abscissa margin for chunks check around bar");

        private final Scale.Fraction barRefineDx = new Scale.Fraction(
                0.25,
                "Abscissa margin for refining peak sides");

        private final Constant.Integer topDerivativeNumber = new Constant.Integer(
                "count",
                5,
                "Top number of best derivatives");

        private final Constant.Ratio minDerivativeRatio = new Constant.Ratio(
                0.4,
                "Minimum absolute derivative as ratio of elite derivative");

        private final Scale.Fraction barThreshold = new Scale.Fraction(
                2.5,
                "Minimum cumul value to detect bar peak");

        private final Scale.Fraction braceThreshold = new Scale.Fraction(
                1.1,
                "Minimum cumul value to detect brace peak");

        private final Scale.Fraction gapThreshold = new Scale.Fraction(
                0.6,
                "Maximum vertical gap length in a bar");

        private final Scale.Fraction chunkThreshold = new Scale.Fraction(
                0.45, //0.25,
                "Maximum cumul value to detect chunk (on top of staff lines)");

        private final Constant.Ratio blankThreshold = new Constant.Ratio(
                0.5,
                "Maximum ratio of lines cumul to detect no-line regions");

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

        private final Constant.Integer chunkOffset = new Constant.Integer(
                "pixels",
                1,
                "Abscissa offset (>=0) on bar peak side, before checking for chunk");

        private final Constant.Integer chunkWidth = new Constant.Integer(
                "pixels",
                2,
                "Abscissa with (>=1) on bar peak side where chunk is measured");
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

        /** First abscissa in region. */
        private final int start;

        /** Last abscissa in region. */
        private final int stop;

        Blank (int start,
               int stop)
        {
            this.start = start;
            this.stop = stop;
        }

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

        final int chunkOffset;

        final int chunkWidth;

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
        final int barThreshold;

        final int braceThreshold;

        final int gapThreshold;

        // Following thresholds depend on actual line thickness within this staff
        int linesThreshold;

        int blankThreshold;

        int chunkThreshold;

        Parameters (Sheet sheet,
                    int staffSpecific)
        {
            chunkOffset = Math.max(0, constants.chunkOffset.getValue());
            chunkWidth = Math.max(1, constants.chunkWidth.getValue());

            final Scale scale = sheet.getScale();

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
                final InterlineScale specific = (staffSpecific == 0)
                        ? scale.getInterlineScale()
                        : scale.getInterlineScale(staffSpecific);
                barThreshold = specific.toPixels(constants.barThreshold);
                braceThreshold = specific.toPixels(constants.braceThreshold);
                gapThreshold = specific.toPixels(constants.gapThreshold);
            }
        }
    }

    //----------//
    // PeakSide //
    //----------//
    /**
     * Describes the (left or right) side of a peak.
     */
    private static class PeakSide
    {

        /** Precise side abscissa. */
        final int abscissa;

        /** Quality based on derivative absolute value. */
        final double derGrade;

        /** Quality based on chunk value. */
        final double chunkGrade;

        PeakSide (int abscissa,
                  double derGrade,
                  double chunkGrade)
        {
            this.abscissa = abscissa;
            this.derGrade = derGrade;
            this.chunkGrade = chunkGrade;
        }
    }
}
