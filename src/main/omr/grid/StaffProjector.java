//----------------------------------------------------------------------------//
//                                                                            //
//                         S t a f f P r o j e c t o r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.constant.ConstantSet;

import omr.image.PixelFilter;

import omr.math.AreaUtil;
import omr.math.AreaUtil.CoreData;
import omr.math.GeoPath;

import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.sig.BarlineInter;
import omr.sig.GradeImpacts;
import omr.sig.Inter;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.LEFT;
import static omr.util.HorizontalSide.RIGHT;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.WindowConstants;

/**
 * Class {@code StaffProjector} is in charge of analyzing a staff
 * projection onto horizontal, in order to retrieve bar lines
 * candidates as well as staff start and stop abscissae.
 * <p>
 * To do so, we analyze the interior of staff because this is where a bar line
 * must be present.
 * The potential bar portions outside staff height are much less typical of
 * a bar line.
 * <p>
 * The projection also gives information about which abscissa values are outside
 * the staff abscissa range, since the corresponding cumulated value gets close
 * to zero.
 * It also gives indication about lack of chunk (beam or head) on each side of
 * a bar candidate, but this indication is limited to the staff height portion.
 *
 * @author Hervé Bitteur
 */
public class StaffProjector
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            StaffProjector.class);

    //~ Instance fields --------------------------------------------------------
    /** Underlying sheet. */
    private final Sheet sheet;

    /** Related scale. */
    private final Scale scale;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Staff to analyze. */
    private final StaffInfo staff;

    /** Pixel source. */
    private final PixelFilter pixelFilter;

    /** All blank regions found. */
    private final List<Blank> allBlanks = new ArrayList<Blank>();

    /** Selected blank region on each staff side. */
    private final Map<HorizontalSide, Blank> blanks = new EnumMap<HorizontalSide, Blank>(
            HorizontalSide.class);

    /** Sequence of bar peaks found. */
    private final List<BarPeak> peaks = new ArrayList<BarPeak>();

    /** Count of cumulated foreground pixels, indexed by abscissa. */
    private int[] values;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new StaffProjector object.
     *
     * @param sheet containing sheet
     * @param staff staff to analyze
     */
    public StaffProjector (Sheet sheet,
                           StaffInfo staff)
    {
        this.sheet = sheet;
        this.staff = staff;

        Picture picture = sheet.getPicture();
        pixelFilter = (PixelFilter) picture.getSource(Picture.SourceKey.BINARY);

        scale = sheet.getScale();
        params = new Parameters(scale);
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // plot //
    //------//
    /**
     * Display a chart of the projection.
     */
    public void plot ()
    {
        if (values == null) {
            computeValues();
        }

        final XYSeriesCollection dataset = new XYSeriesCollection();
        final int xMin = 0;
        final int xMax = sheet.getWidth() - 1;

        {
            // Values
            XYSeries valueSeries = new XYSeries("Cumuls");

            for (int x = xMin; x <= xMax; x++) {
                valueSeries.add(x, values[x]);
            }

            dataset.addSeries(valueSeries);
        }

        {
            // Derivatives
            XYSeries derivativeSeries = new XYSeries("Derivatives");

            for (int x = xMin; x <= xMax; x++) {
                derivativeSeries.add(x, derivative(x));
            }

            dataset.addSeries(derivativeSeries);
        }

        {
            // BarPeak min threshold
            XYSeries minSeries = new XYSeries("MinHeight");
            minSeries.add(xMin, params.barThreshold);
            minSeries.add(xMax, params.barThreshold);

            dataset.addSeries(minSeries);
        }

        {
            // Chunk threshold (assuming a 5-line staff)
            XYSeries chunkSeries = new XYSeries("MaxChunk");
            chunkSeries.add(xMin, params.chunkThreshold);
            chunkSeries.add(xMax, params.chunkThreshold);
            dataset.addSeries(chunkSeries);
        }

        {
            // Theoretical staff height (assuming a 5-line staff)
            XYSeries heightSeries = new XYSeries("Height");
            int totalHeight = 4 * scale.getInterline();
            heightSeries.add(xMin, totalHeight);
            heightSeries.add(xMax, totalHeight);

            dataset.addSeries(heightSeries);
        }

        {
            final int nostaff = params.blankThreshold;
            XYSeries holeSeries = new XYSeries("NoStaff");
            holeSeries.add(xMin, nostaff);
            holeSeries.add(xMax, nostaff);
            dataset.addSeries(holeSeries);
        }

        {
            // Cumulated staff lines (assuming a 5-line staff)
            XYSeries linesSeries = new XYSeries("Lines");
            linesSeries.add(xMin, params.linesThreshold);
            linesSeries.add(xMax, params.linesThreshold);
            dataset.addSeries(linesSeries);
        }

        {
            // Zero
            XYSeries zeroSeries = new XYSeries("Zero");
            zeroSeries.add(xMin, 0);
            zeroSeries.add(xMax, 0);
            dataset.addSeries(zeroSeries);
        }

        // Chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                sheet.getId() + " staff#" + staff.getId(), // Title
                "Abscissae", // X-Axis label
                "Counts", // Y-Axis label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // orientation,
                true, // Show legend
                false, // Show tool tips
                false // urls
        );

        // Hosting frame
        ChartFrame frame = new ChartFrame(
                sheet.getId() + " staff#" + staff.getId(),
                chart,
                true);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLocation(new Point(20 * staff.getId(), 20 * staff.getId()));
        frame.setVisible(true);
    }

    //---------//
    // process //
    //---------//
    public List<BarPeak> process ()
    {
        logger.debug("Analyzing staff#{}", staff.getId());

        // Cumulate pixels for each abscissa
        computeValues();

        // Retrieve regions without staff lines
        findBlanks();

        // Retrieve barline candidates
        findPeaks();

        // Refine left and right abscissa values of staff
        for (HorizontalSide side : HorizontalSide.values()) {
            refineStaffAbscissa(side);
        }

        staff.setBarPeaks(peaks);

        return peaks;
    }

    //---------------//
    // computeValues //
    //---------------//
    /**
     * Compute, for each abscissa value, the pixels cumulated between
     * first line and last line of staff.
     */
    private void computeValues ()
    {
        values = new int[sheet.getWidth()];

        final FilamentLine firstLine = staff.getFirstLine();
        final FilamentLine lastLine = staff.getLastLine();
        final int dx = params.staffAbscissaMargin;
        final int xMin = xClamp(staff.getAbscissa(LEFT) - dx);
        final int xMax = xClamp(staff.getAbscissa(RIGHT) + dx);

        for (int x = xMin; x <= xMax; x++) {
            int yMin = firstLine.yAt(x);
            int yMax = lastLine.yAt(x);
            int count = 0;

            for (int y = yMin; y <= yMax; y++) {
                if (pixelFilter.isFore(x, y)) {
                    count++;
                }
            }

            values[x] = count;
        }
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
     * @return the created BarPeak instance or null if failed
     */
    private BarPeak createPeak (final int rawStart,
                                final int rawStop,
                                int value)
    {
        final int minValue = params.barThreshold;
        final int totalHeight = 4 * scale.getInterline();
        final double valueRange = totalHeight - minValue;

        // Compute precise start & stop abscissae
        final int start = refinePeakSide(rawStart, rawStop, -1);
        final int stop = refinePeakSide(rawStart, rawStop, +1);

        // Compute chunk if any
        final int leftChunk = getChunk(start, -1);
        final int rightChunk = getChunk(stop, +1);
        final int chunk = Math.max(leftChunk, rightChunk);
        final double chunkRange = params.chunkThreshold
                                  - params.linesThreshold;

        if (chunk > params.chunkThreshold) {
            return null;
        }

        // Compute largest white gap
        final int xMid = (start + stop) / 2;
        final int yTop = staff.getFirstLine()
                .yAt(xMid);
        final int yBottom = staff.getLastLine()
                .yAt(xMid);

        final GeoPath leftLine = new GeoPath(
                new Line2D.Double(start, yTop, start, yBottom));
        final GeoPath rightLine = new GeoPath(
                new Line2D.Double(stop, yTop, stop, yBottom));
        final CoreData data = AreaUtil.verticalCore(
                pixelFilter,
                leftLine,
                rightLine);

        if (data.gap > params.gapThreshold) {
            return null;
        }

        // Compute black core & impacts
        double coreImpact = (value - minValue) / valueRange;
        double beltImpact = (params.chunkThreshold - chunk) / chunkRange;
        double gapImpact = 1 - ((double) data.gap / params.gapThreshold);
        GradeImpacts impacts = new BarlineInter.Impacts(
                coreImpact,
                beltImpact,
                gapImpact);
        double grade = impacts.getGrade();

        if (grade >= Inter.minGrade) {
            boolean isThin = (stop - start + 1) <= params.maxThinWidth;
            BarPeak peak = new BarPeak(
                    staff,
                    yTop,
                    yBottom,
                    start,
                    stop,
                    isThin,
                    value,
                    chunk,
                    impacts);

            return peak;
        }

        return null;
    }

    //------------//
    // derivative //
    //------------//
    /**
     * Report the first derivative of cumulated values at abscissa x.
     * It's a very rough computation, but that's OK.
     *
     * @param x abscissa (assumed to be within sheet width)
     * @return computed derivative at x
     */
    private int derivative (int x)
    {
        if (x == 0) {
            return 0;
        }

        return values[x] - values[x - 1];
    }

    //------------//
    // findBlanks //
    //------------//
    /**
     * Look for all "blank" regions without staff lines.
     * <p>
     * We then select the region right before the staff and the region
     * right after the staff, and populate the blanks map with these two ones.
     */
    private void findBlanks ()
    {
        final int maxValue = params.blankThreshold;
        final int sheetWidth = sheet.getWidth();

        int start = -1;
        int stop = -1;

        for (int x = 0; x < sheetWidth; x++) {
            if (values[x] <= maxValue) {
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
                "Staff#{} left:{} right:{} {}",
                staff.getId(),
                staff.getAbscissa(LEFT),
                staff.getAbscissa(RIGHT),
                allBlanks);

        // Select regions for left & right staff sides
        for (HorizontalSide side : HorizontalSide.values()) {
            blanks.put(side, selectBlank(side, allBlanks));
        }

        logger.debug("Staff#{} {}", staff.getId(), blanks);
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

        final Blank leftBlank = blanks.get(LEFT);
        final int xMin = (leftBlank != null) ? leftBlank.stop : 0;

        final Blank rightBlank = blanks.get(RIGHT);
        final int xMax = (rightBlank != null) ? rightBlank.start
                : (sheet.getWidth() - 1);

        int start = -1;
        int stop = -1;
        int bestValue = 0;

        for (int x = xMin; x <= xMax; x++) {
            int value = values[x];

            if (value >= minValue) {
                if (start == -1) {
                    start = x;
                }

                stop = x;
                bestValue = Math.max(bestValue, value);
            } else {
                if (start != -1) {
                    BarPeak peak = createPeak(start, stop, bestValue);

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
            BarPeak peak = createPeak(start, stop, bestValue);

            if (peak != null) {
                peaks.add(peak);
            }
        }

        logger.debug("Staff#{} peaks: {}", staff.getId(), peaks);
    }

    //----------//
    // getChunk //
    //----------//
    /**
     * Check if peak side is free of pixels (except staff lines).
     *
     * @param xStart starting abscissa
     * @param dir    desired abscissa direction (-1 for LEFT, +1 for RIGHT)
     * @return the maximum number of pixels found on specified side.
     *         This number is composed of the staff line pixels plus the pixels
     *         from other stuff (generally beams or note heads) found.
     */
    private int getChunk (int xStart,
                          int dir)
    {
        final int dx = params.barAbscissaMargin;
        final int xEnd = xClamp(xStart + (dir * (1 + dx)));
        int minValue = Integer.MAX_VALUE;

        for (int x = xStart + dir; (dir * (xEnd - x)) >= 0; x += dir) {
            minValue = Math.min(minValue, values[x]);
        }

        return minValue;
    }

    //----------------//
    // refinePeakSide //
    //----------------//
    /**
     * Use extrema of first derivative to refine peak side abscissa.
     * Maximum for left side, minimum for right side.
     *
     * @param xStart raw abscissa that starts peak
     * @param xStop  raw abscissa that stops peak
     * @param dir    -1 for going left, +1 for going right
     */
    private int refinePeakSide (int xStart,
                                int xStop,
                                int dir)
    {
        // Additional check range
        final int dx = params.barAbscissaMargin;

        // Beginning and ending x values
        final int x1 = (dir > 0) ? xStart : xStop;
        final int x2 = (dir > 0) ? xClamp(xStop + dx) : xClamp(xStart - dx);
        int bestDer = 0; // Best derivative so far
        int bestX = -1; // X at best derivative

        for (int x = x1; (dir * (x2 - x)) >= 0; x += dir) {
            int der = derivative(x);

            if ((dir * (bestDer - der)) > 0) {
                bestDer = der;
                bestX = x;
            }
        }

        return (dir > 0) ? (bestX - 1) : bestX;
    }

    //---------------------//
    // refineStaffAbscissa //
    //---------------------//
    /**
     * Using rough abscissa (defined at line ending point), try to
     * extend it outwards.
     * We use the last bar line peak encountered if any before the blank
     * (no-line) region.
     * If no such peak is found, we stop right before the blank region.
     *
     * @param side which horizontal side to refine
     */
    private void refineStaffAbscissa (HorizontalSide side)
    {
        final int dir = (side == LEFT) ? (-1) : 1;
        final int staffEnd = staff.getAbscissa(side);

        if (!peaks.isEmpty()) {
            final BarPeak peak = peaks.get(
                    (side == LEFT) ? 0 : (peaks.size() - 1));
            final int peakMid = (peak.getStart() + peak.getStop()) / 2;

            // Check side position of peak wrt staff
            if ((dir * (peakMid - staffEnd)) > 0) {
                logger.debug(
                        "Staff#{} {} set at peak {} (vs {})",
                        staff.getId(),
                        side,
                        peakMid,
                        staffEnd);
                staff.setAbscissa(side, peakMid);

                return;
            }
        }

        // Here, no suitable peak was found, stop at blank region if any
        Blank blank = blanks.get(side);

        if (blank != null) {
            int x = (side == LEFT) ? (blank.stop + 1) : (blank.start - 1);
            logger.debug(
                    "Staff#{} {} set at blank {} (vs {})",
                    staff.getId(),
                    side,
                    x,
                    staffEnd);
            staff.setAbscissa(side, x);
        } else {
            logger.debug("Staff#{} no clear end on {}", staff.getId(), side);
        }
    }

    //-------------//
    // selectBlank //
    //-------------//
    /**
     * Report the relevant blank region on desired staff side.
     * <p>
     * We try to pickup a wide enough region if any.
     * If not, we simply select the first one encountered among the widest ones.
     * <p>
     * TODO: The selection could be revised in a second phase performed at sheet
     * level, since poor-quality staves may exhibit abnormal blank regions.
     *
     * @param side   desired side
     * @param blanks the list of blanks known
     * @return the relevant blank region found, null if none was found
     */
    private Blank selectBlank (HorizontalSide side,
                               List<Blank> blanks)
    {
        final int minWidth = params.minBlankWidth;
        final int dir = (side == LEFT) ? (-1) : 1;
        final int end = staff.getAbscissa(side) - dir;

        final int rInit = (side == LEFT) ? (blanks.size() - 1) : 0;
        final int rBreak = (side == LEFT) ? (-1) : blanks.size();

        // Find first blank of significant width, if any
        Blank widestBlank = null;
        int widestWidth = -1;

        for (int ir = rInit; ir != rBreak; ir += dir) {
            Blank blank = blanks.get(ir);
            int mid = (blank.start + blank.stop) / 2;

            // Make sure we are on desired side of the staff
            if ((dir * (mid - end)) > 0) {
                int width = blank.getWidth();

                // Stop on first significant blank
                if (width >= minWidth) {
                    return blank;
                }

                // Remember the first among widest small blanks
                if (widestWidth < width) {
                    widestWidth = width;
                    widestBlank = blank;
                }
            }
        }

        return widestBlank;
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

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Scale.Fraction maxThinWidth = new Scale.Fraction(
                0.4,
                "Max width for a thin bar line");

        final Scale.Fraction staffAbscissaMargin = new Scale.Fraction(
                10,
                "Abscissa margin for checks around staff");

        final Scale.Fraction barAbscissaMargin = new Scale.Fraction(
                0.25,
                "Abscissa margin for checks around bar");

        final Scale.Fraction barThreshold = new Scale.Fraction(
                3.0,
                "Minimum cumul value to detect bar peak");

        final Scale.Fraction gapThreshold = new Scale.Fraction(
                0.5,
                "Maximum vertical gap length in a bar");

        final Scale.Fraction chunkThreshold = new Scale.Fraction(
                0.25,
                "Maximum cumul value to detect chunk (on top of lines)");

        final Scale.LineFraction blankThreshold = new Scale.LineFraction(
                1,
                "Maximum cumul value (in LineFraction) to detect noline region");

        final Scale.Fraction minBlankWidth = new Scale.Fraction(
                0.5,
                "Minimum width for a blank region to end a staff side");

    }

    //-------//
    // Blank //
    //-------//
    /**
     * A abscissa region where no staff lines are detected and thus
     * indicates possible end of staff.
     */
    private static class Blank
    {
        //~ Instance fields ----------------------------------------------------

        /** First abscissa in region. */
        private final int start;

        /** Last abscissa in region. */
        private final int stop;

        //~ Constructors -------------------------------------------------------
        public Blank (int start,
                      int stop)
        {
            this.start = start;
            this.stop = stop;
        }

        //~ Methods ------------------------------------------------------------
        public int getWidth ()
        {
            return stop - start + 1;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Blank(")
                    .append(start)
                    .append("-")
                    .append(stop)
                    .append(")");

            return sb.toString();
        }
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final int maxThinWidth;

        final int staffAbscissaMargin;

        final int barAbscissaMargin;

        final int barThreshold;

        final int gapThreshold;

        final int linesThreshold;

        final int chunkThreshold;

        final int blankThreshold;

        final int minBlankWidth;

        //~ Constructors -------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxThinWidth = scale.toPixels(constants.maxThinWidth);
            staffAbscissaMargin = scale.toPixels(constants.staffAbscissaMargin);
            barAbscissaMargin = scale.toPixels(constants.barAbscissaMargin);
            barThreshold = scale.toPixels(constants.barThreshold);
            gapThreshold = scale.toPixels(constants.gapThreshold);
            linesThreshold = 4 * scale.getMainFore();
            blankThreshold = (int) Math.rint(
                    scale.getMaxFore() * constants.blankThreshold.getValue());
            minBlankWidth = scale.toPixels(constants.minBlankWidth);

            // For chunk threshold the strategy is to use the largest value
            // among (4 times max fore) and (4 times mean fore) + fraction
            chunkThreshold = Math.max(
                    4 * scale.getMaxFore(),
                    linesThreshold + scale.toPixels(constants.chunkThreshold));
        }
    }
}
