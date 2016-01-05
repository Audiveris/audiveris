//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t a f f P r o j e c t o r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.constant.ConstantSet;

import omr.math.AreaUtil;
import omr.math.AreaUtil.CoreData;
import omr.math.GeoPath;
import omr.math.Projection;

import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;

import omr.sig.GradeImpacts;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.Inter;

import omr.util.HorizontalSide;

import static omr.util.HorizontalSide.*;

import omr.util.Navigable;

import ij.process.ByteProcessor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.WindowConstants;

/**
 * Class {@code StaffProjector} is in charge of analyzing a staff projection onto
 * x-axis, in order to retrieve bar lines candidates as well as staff start and stop
 * abscissae.
 * <p>
 * To retrieve bar lines candidates, we analyze the vertical interior of staff because this is where
 * a bar line must be present.
 * The potential bar portions outside staff height are much less typical of a bar line.
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
 * <p>
 * The brace peak is rather similar to a thick bar line, although its first derivative is lower
 * and similarly the inter-staff connection is of low quality.
 *
 * <li>An Alto <b>C-clef</b> portion:<br>
 * <img alt="Image of alto clef"
 * src="http://upload.wikimedia.org/wikipedia/commons/thumb/6/68/Alto_clef_with_ref.svg/90px-Alto_clef_with_ref.svg.png">
 * <br>
 * Such C-clef artifacts are detected later, based on their abscissa offset from the measure
 * start (be it bar-based start or lines-only start).
 *
 * <li>A <b>stem</b> (with note heads located outside the staff height).
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
 * candidate, but this indication is limited to the staff height portion.
 *
 * @author Hervé Bitteur
 */
public class StaffProjector
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StaffProjector.class);

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

    /** Count of cumulated foreground pixels, indexed by abscissa. */
    private Projection projection;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StaffProjector} object.
     *
     * @param sheet containing sheet
     * @param staff staff to analyze
     */
    public StaffProjector (Sheet sheet,
                           Staff staff)
    {
        this.sheet = sheet;
        this.staff = staff;

        Picture picture = sheet.getPicture();
        pixelFilter = picture.getSource(Picture.SourceKey.BINARY);

        scale = sheet.getScale();
        params = new Parameters(scale);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // findBracePeak //
    //---------------//
    /**
     * Try to find a brace-compatible on left side of provided abscissa.
     *
     * @param minLeft  provided minimum abscissa on left
     * @param maxRight provided maximum abscissa on right
     * @return a brace peak, or null
     */
    public StaffPeak.Brace findBracePeak (int minLeft,
                                          int maxRight)
    {
        final int minValue = params.braceThreshold;
        final Blank leftBlank = endingBlanks.get(LEFT);
        final int xMin = Math.max(minLeft, (leftBlank != null) ? leftBlank.stop : 0);

        int braceStop = -1;
        int braceStart = -1;
        int bestValue = 0;

        // Browse from right to left
        for (int x = maxRight; x >= xMin; x--) {
            int value = projection.getValue(x);

            if (value >= minValue) {
                if (braceStop == -1) {
                    braceStop = x;
                }

                braceStart = x;
                bestValue = Math.max(bestValue, value);
            } else {
                if (braceStop != -1) {
                    return createBracePeak(braceStart, braceStop, maxRight);
                }
            }
        }

        return null;
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * @return the staff
     */
    public Staff getStaff ()
    {
        return staff;
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
     *
     * @return the sequence of peaks found
     */
    public List<StaffPeak> process ()
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
        staff.setBarPeaks(peaks);

        return peaks;
    }

    //------------------//
    // refineStaffSides //
    //------------------//
    /**
     * Use remaining peaks to refine staff sides accordingly.
     */
    public void refineStaffSides ()
    {
        for (HorizontalSide side : HorizontalSide.values()) {
            refineStaffSide(side);
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

    //---------------------------//
    // computeCoreLinesThickness //
    //---------------------------//
    /**
     * Using the current definition on staff lines (made of only long filaments sofar)
     * estimate the cumulated staff line thicknesses for the staff.
     * Since we may have holes in lines, and short sections have been left apart, the measurement
     * is under-estimated.
     *
     * @return the estimate of cumulated lines heights
     */
    private double computeCoreLinesThickness ()
    {
        double linesHeight = 0;

        for (LineInfo line : staff.getLines()) {
            linesHeight += line.getThickness();
        }

        logger.debug("Staff#{} linesheight: {}", staff.getId(), linesHeight);

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

        // For chunk threshold the strategy is to use the largest value
        // among (4 times max fore) and (linesCumul + fraction)
        params.linesThreshold = (int) Math.rint(linesCumul); ///4 * scale.getMainFore();
        params.blankThreshold = (int) Math.rint(
                constants.blankThreshold.getValue() * lineThickness);
        params.chunkThreshold = Math.max(
                4 * scale.getMaxFore(),
                params.linesThreshold + scale.toPixels(constants.chunkThreshold));
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
    private StaffPeak.Brace createBracePeak (int rawStart,
                                             int rawStop,
                                             int maxRight)
    {
        // Extend left abscissa until a blank (no-staff) is reached
        Blank leftBlank = null;

        for (Blank blank : allBlanks) {
            if (blank.stop >= rawStart) {
                break;
            }

            leftBlank = blank;
        }

        if (leftBlank == null) {
            return null;
        }

        int start = leftBlank.stop;
        int val = projection.getValue(start);
        int nextVal = projection.getValue(start - 1);

        while (nextVal < val) {
            start = start - 1;
            val = nextVal;
            nextVal = projection.getValue(start - 1);
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

        return new StaffPeak.Brace(staff, yTop, yBottom, start, stop);
    }

    //------------//
    // createPeak //
    //------------//
    /**
     * (Try to) create a relevant peak at provided location.
     *
     * @param rawStart raw starting abscissa of peak
     * @param rawStop  raw stopping abscissa of peak
     * @param value    highest peak value
     * @return the created peak instance or null if failed
     */
    private StaffPeak.Bar createPeak (final int rawStart,
                                      final int rawStop,
                                      int value)
    {
        final int minValue = params.barThreshold;
        final int totalHeight = 4 * scale.getInterline();
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

        // Compute chunk if relevant
        final Integer leftChunk = getChunk(start, -1);
        final Integer rightChunk = getChunk(stop, +1);
        final int chunk;

        if ((leftChunk != null) && (rightChunk != null)) {
            chunk = Math.max(leftChunk, rightChunk);
        } else if (leftChunk != null) {
            chunk = leftChunk;
        } else if (rightChunk != null) {
            chunk = rightChunk;
        } else {
            return null;
        }

        if (chunk > params.chunkThreshold) {
            return null;
        }

        // Compute largest white gap
        final int xMid = (start + stop) / 2;
        final int yTop = staff.getFirstLine().yAt(xMid);
        final int yBottom = staff.getLastLine().yAt(xMid);

        final GeoPath leftLine = new GeoPath(new Line2D.Double(start, yTop, start, yBottom));
        final GeoPath rightLine = new GeoPath(new Line2D.Double(stop, yTop, stop, yBottom));
        final CoreData data = AreaUtil.verticalCore(pixelFilter, leftLine, rightLine);

        if (data.gap > params.gapThreshold) {
            return null;
        }

        // Compute black core & impacts
        double coreImpact = (value - minValue) / valueRange;
        final double chunkRange = params.chunkThreshold - params.linesThreshold;
        double beltImpact = (params.chunkThreshold - chunk) / chunkRange;
        double gapImpact = 1 - ((double) data.gap / params.gapThreshold);
        GradeImpacts impacts = new BarlineInter.Impacts(
                coreImpact,
                beltImpact,
                gapImpact,
                newStart.grade,
                newStop.grade);
        double grade = impacts.getGrade();

        if (grade >= Inter.minGrade) {
            return new StaffPeak.Bar(staff, yTop, yBottom, start, stop, impacts);
        }

        return null;
    }

    //---------------//
    // findAllBlanks //
    //---------------//
    /**
     * Look for all "blank" regions without staff lines.
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
            } else {
                // Lines detected
                if (start != -1) {
                    allBlanks.add(new Blank(start, stop));
                    start = -1;
                }
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
        int bestValue = 0;

        for (int x = xMin; x <= xMax; x++) {
            int value = projection.getValue(x);

            if (value >= minValue) {
                if (start == -1) {
                    start = x;
                }

                stop = x;
                bestValue = Math.max(bestValue, value);
            } else {
                if (start != -1) {
                    StaffPeak peak = createPeak(start, stop, bestValue);

                    if (peak != null) {
                        peaks.add(peak);
                    }

                    start = -1;
                    bestValue = 0;
                }
            }
        }

        // Finish ongoing peak if any (this is very unlikely...)
        if (start != -1) {
            StaffPeak peak = createPeak(start, stop, bestValue);

            if (peak != null) {
                peaks.add(peak);
            }
        }

        logger.debug("Staff#{} peaks:{}", staff.getId(), peaks);
    }

    //----------//
    // getChunk //
    //----------//
    /**
     * Check if peak side is free of pixels (except staff lines).
     *
     * @param xStart starting abscissa
     * @param dir    desired abscissa direction (-1 for LEFT, +1 for RIGHT)
     * @return the maximum number of pixels found on specified side, null if not relevant.
     *         This number is composed of the staff line pixels plus the pixels
     *         from other stuff (generally beams or note heads) found.
     *         When two peaks are very close, this test is not reliable between the peaks.
     */
    private Integer getChunk (int xStart,
                              int dir)
    {
        final int xEnd = xClamp(xStart + (dir * (1 + params.barChunkDx)));
        int minValue = Integer.MAX_VALUE;

        for (int x = xStart + dir; (dir * (xEnd - x)) >= 0; x += dir) {
            int proj = projection.getValue(x);

            // If we encounter a peak, chunk test may not be reliable because of risk of pixels stuck
            if (proj >= params.barThreshold) {
                if (minValue <= params.chunkThreshold) {
                    return minValue;
                } else {
                    return null;
                }
            }

            minValue = Math.min(minValue, proj);
        }

        return minValue;
    }

    //----------------//
    // refinePeakSide //
    //----------------//
    /**
     * Use extrema of first derivative to refine peak side abscissa.
     * Maximum for left side, minimum for right side.
     * Absolute derivative value indicates if the peak side is really steep: this should exclude
     * most of: braces, arpeggiates, stems with heads on left or right side.
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
        final int x1 = (dir > 0) ? xStop : xStart;
        final int x2 = (dir > 0) ? xClamp(xStop + dx) : xClamp(xStart - dx);

        // X at best derivative
        int bestX = -1;

        // Best derivative so far
        int bestDer = 0;

        // True if other peak found close to this one
        boolean closePeakFound = false;

        for (int x = x1; (dir * (x2 - x)) >= 0; x += dir) {
            int der = projection.getDerivative(x);

            if ((dir * (bestDer - der)) > 0) {
                bestDer = der;
                bestX = x;
            }

            // Check we are still higher than chunk level
            int val = projection.getValue(x);

            if (val < params.chunkThreshold) {
                break;
            } else if ((val >= params.barThreshold) && (x != x1)) {
                closePeakFound = true;
            }
        }

        bestDer = Math.abs(bestDer);

        if ((bestDer >= params.minDerivative) || closePeakFound) {
            int x = (dir > 0) ? (bestX - 1) : bestX;
            double derImpact = (double) bestDer / (params.barThreshold - params.minDerivative);

            return new PeakSide(x, derImpact);
        } else {
            return null; // Invalid
        }
    }

    //-----------------//
    // refineStaffSide //
    //-----------------//
    /**
     * Try to use the extreme peak on a given staff side, to refine the precise abscissa
     * where the staff ends.
     * <p>
     * When this method is called, the staff sides are defined only by the ends of the lines built
     * with long sections.
     * An extreme peak (first or last peak according to side) can be used as abscissa reference
     * only if it is either beyond current staff end or sufficiently close to the end.
     * If no such peak is found, we stop right before the blank region assuming that this is a
     * measure with no outside bar.
     *
     * @param side which horizontal side to refine
     */
    private void refineStaffSide (HorizontalSide side)
    {
        final int dir = (side == LEFT) ? (-1) : 1;
        final int linesEnd = staff.getAbscissa(side); // As defined by end of long staff lines
        int staffEnd = linesEnd;
        StaffPeak endPeak = null;
        Integer peakEnd = null;

        // Look for a suitable peak
        StaffPeak bracket = null; // Last bracket encountered on left side if any

        if (!peaks.isEmpty()) {
            StaffPeak peak = null;

            if (side == LEFT) {
                for (StaffPeak p : peaks) {
                    if (p.isBracket()) {
                        bracket = p;

                        continue;
                    }

                    peak = p;

                    break;
                }
            } else {
                peak = peaks.get(peaks.size() - 1);

                // Just in case
                if (peak.isBracket()) {
                    peak = null;
                }
            }

            if (peak != null) {
                // Check side position of peak wrt staff, it must be external
                final int peakMid = (peak.getStart() + peak.getStop()) / 2;
                final int toPeak = peakMid - linesEnd;

                if ((dir * toPeak) >= 0) {
                    endPeak = peak;
                    peakEnd = (side == LEFT) ? endPeak.getStart() : endPeak.getStop();
                    staffEnd = peakEnd;
                }
            }
        }

        // Continue and stop at first small blank region (or bracket) encountered if any.
        // Then keep the additional line chunk if long enough.
        // If not, use peak mid as staff end.
        Blank blank = selectBlank(side, staffEnd, params.minSmallBlankWidth);

        if (blank != null) {
            int x = (side == LEFT) ? (blank.stop + 1) : (blank.start - 1);

            if (endPeak != null) {
                if ((bracket == null) && ((dir * (x - peakEnd)) > params.maxBarToEnd)) {
                    // We have significant line chunks beyond bar, hence peak is not the limit
                    logger.debug(
                            "Staff#{} {} set at blank {} (vs {})",
                            staff.getId(),
                            side,
                            x,
                            linesEnd);
                    staff.setAbscissa(side, x);
                } else {
                    // No significant line chunks, ignore them and stay with peak as the limit
                    final int peakMid = (endPeak.getStart() + endPeak.getStop()) / 2;
                    logger.debug(
                            "Staff#{} {} set at peak {} (vs {})",
                            staff.getId(),
                            side,
                            peakMid,
                            linesEnd);
                    staff.setAbscissa(side, peakMid);
                    endPeak.setStaffEnd(side);
                }
            } else {
                logger.debug(
                        "Staff#{} {} set at blank {} (vs {})",
                        staff.getId(),
                        side,
                        x,
                        linesEnd);
                staff.setAbscissa(side, x);
            }
        } else {
            logger.warn("Staff#{} no clear end on {}", staff.getId(), side);
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
        for (HorizontalSide side : HorizontalSide.values()) {
            // Look for the first really wide blank encountered
            Blank blank = selectBlank(side, staff.getAbscissa(side), params.minWideBlankWidth);

            if (blank == null) {
                // No wide blank has been found, simply pick up the one farthest from staff
                blank = (side == LEFT) ? allBlanks.get(0) : allBlanks.get(allBlanks.size() - 1);
            }

            endingBlanks.put(side, blank);
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
                10,
                "Abscissa margin for checks around staff");

        private final Scale.Fraction barChunkDx = new Scale.Fraction(
                0.4,
                "Abscissa margin for chunks check around bar");

        private final Scale.Fraction barRefineDx = new Scale.Fraction(
                0.25,
                "Abscissa margin for refining peak sides");

        private final Scale.Fraction minDerivative = new Scale.Fraction(
                0.75,
                "Minimum absolute derivative for peak side");

        private final Scale.Fraction barThreshold = new Scale.Fraction(
                3.5,
                "Minimum cumul value to detect bar peak");

        private final Scale.Fraction braceThreshold = new Scale.Fraction(
                2,
                "Minimum cumul value to detect brace peak");

        private final Scale.Fraction gapThreshold = new Scale.Fraction(
                0.5,
                "Maximum vertical gap length in a bar");

        private final Scale.Fraction chunkThreshold = new Scale.Fraction(
                0.4,
                "Maximum cumul value to detect chunk (on top of lines)");

        private final Scale.LineFraction blankThreshold = new Scale.LineFraction(
                2.5,
                "Maximum cumul value (in LineFraction) to detect no-line regions");

        private final Scale.Fraction minWideBlankWidth = new Scale.Fraction(
                1.0,
                "Minimum width for a wide blank region (to limit peaks search)");

        private final Scale.Fraction minSmallBlankWidth = new Scale.Fraction(
                0.1,
                "Minimum width for a small blank region (to end a staff side)");

        private final Scale.Fraction maxBarWidth = new Scale.Fraction(1.0, "Maximum bar width");

        private final Scale.Fraction maxBarToEnd = new Scale.Fraction(
                0.15,
                "Maximum dx between bar and end of staff");
    }

    //-------//
    // Blank //
    //-------//
    /**
     * A abscissa region where no staff lines are detected and thus indicates possible
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
            return Integer.compare(this.start, that.start);
        }

        public int getWidth ()
        {
            return stop - start + 1;
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

        final int minDerivative;

        final int barThreshold;

        final int braceThreshold;

        final int gapThreshold;

        final int minWideBlankWidth;

        final int minSmallBlankWidth;

        final int maxBarWidth;

        final int maxBarToEnd;

        // Following threshold values depend on actual line height within this staff
        int linesThreshold;

        int blankThreshold;

        int chunkThreshold;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            staffAbscissaMargin = scale.toPixels(constants.staffAbscissaMargin);
            barChunkDx = scale.toPixels(constants.barChunkDx);
            barRefineDx = scale.toPixels(constants.barRefineDx);
            minDerivative = scale.toPixels(constants.minDerivative);
            barThreshold = scale.toPixels(constants.barThreshold);
            braceThreshold = scale.toPixels(constants.braceThreshold);
            gapThreshold = scale.toPixels(constants.gapThreshold);
            minWideBlankWidth = scale.toPixels(constants.minWideBlankWidth);
            minSmallBlankWidth = scale.toPixels(constants.minSmallBlankWidth);
            maxBarWidth = scale.toPixels(constants.maxBarWidth);
            maxBarToEnd = scale.toPixels(constants.maxBarToEnd);
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
                "Abscissae", // X-Axis label
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
                XYSeries valueSeries = new XYSeries("Cumuls");

                for (int x = xMin; x <= xMax; x++) {
                    valueSeries.add(x, projection.getValue(x));
                }

                add(valueSeries, Color.RED, false);
            }

            {
                // Derivatives
                XYSeries derivativeSeries = new XYSeries("Derivatives");

                for (int x = xMin; x <= xMax; x++) {
                    derivativeSeries.add(x, projection.getDerivative(x));
                }

                add(derivativeSeries, Color.BLUE, false);
            }

            {
                // Derivatives positive threshold
                XYSeries derSeries = new XYSeries("Der+");

                derSeries.add(xMin, params.minDerivative);
                derSeries.add(xMax, params.minDerivative);
                add(derSeries, Color.BLUE, false);
            }

            {
                // Derivatives negative threshold
                XYSeries derSeries = new XYSeries("Der-");

                derSeries.add(xMin, -params.minDerivative);
                derSeries.add(xMax, -params.minDerivative);
                add(derSeries, Color.BLUE, false);
            }

            {
                // Theoretical staff height (assuming a 5-line staff)
                XYSeries heightSeries = new XYSeries("StaffHeight");
                int totalHeight = 4 * scale.getInterline();
                heightSeries.add(xMin, totalHeight);
                heightSeries.add(xMax, totalHeight);
                add(heightSeries, Color.BLACK, true);
            }

            {
                // BarPeak min threshold
                XYSeries minSeries = new XYSeries("BarThreshold");
                minSeries.add(xMin, params.barThreshold);
                minSeries.add(xMax, params.barThreshold);
                add(minSeries, Color.GREEN, true);
            }

            {
                // BracePeak min threshold
                XYSeries minSeries = new XYSeries("BraceThreshold");
                minSeries.add(xMin, params.braceThreshold);
                minSeries.add(xMax, params.braceThreshold);
                add(minSeries, Color.ORANGE, true);
            }

            {
                // Chunk threshold (assuming a 5-line staff)
                XYSeries chunkSeries = new XYSeries("MaxChunk");
                chunkSeries.add(xMin, params.chunkThreshold);
                chunkSeries.add(xMax, params.chunkThreshold);
                add(chunkSeries, Color.YELLOW, true);
            }

            {
                // Cumulated staff lines (assuming a 5-line staff)
                XYSeries linesSeries = new XYSeries("Lines");
                linesSeries.add(xMin, params.linesThreshold);
                linesSeries.add(xMax, params.linesThreshold);
                add(linesSeries, Color.MAGENTA, true);
            }

            {
                // Threshold for no staff
                final int nostaff = params.blankThreshold;
                XYSeries holeSeries = new XYSeries("NoStaff");
                holeSeries.add(xMin, nostaff);
                holeSeries.add(xMax, nostaff);
                add(holeSeries, Color.CYAN, true);
            }

            {
                // Zero
                XYSeries zeroSeries = new XYSeries("Zero");
                zeroSeries.add(xMin, 0);
                zeroSeries.add(xMax, 0);
                add(zeroSeries, Color.WHITE, true);
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
