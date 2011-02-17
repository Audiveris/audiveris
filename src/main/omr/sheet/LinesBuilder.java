//----------------------------------------------------------------------------//
//                                                                            //
//                          L i n e s B u i l d e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.GlyphsModel;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;
import omr.glyph.ui.GlyphsController;

import omr.lag.HorizontalOrientation;
import omr.lag.JunctionDeltaPolicy;
import omr.lag.Run;
import omr.lag.SectionsBuilder;
import omr.lag.ui.RunBoard;
import omr.lag.ui.ScrollLagView;
import omr.lag.ui.SectionBoard;

import omr.log.Logger;

import omr.math.Population;

import omr.sheet.ui.PixelBoard;

import omr.step.Step;
import omr.step.StepException;
import omr.step.Steps;

import omr.stick.StickSection;

import omr.ui.BoardsPane;

import omr.util.Implement;
import omr.util.WeakPropertyChangeListener;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Action;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 * Class <code>LinesBuilder</code> is dedicated to the retrieval of the grid of
 * staff lines. The various series of staves lines are detected, and their lines
 * are carefully cleaned up when an object crosses them.
 *
 * <p>Note that staves are not yet gathered into Systems, this will be done in
 * the {@link SystemsBuilder} processing.
 *
 * @author Hervé Bitteur
 */
public class LinesBuilder
    extends GlyphsModel
    implements StavesBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LinesBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** Series of horizontal peaks that signal staff areas */
    private final List<StaffInfo> staves = new ArrayList<StaffInfo>();

    /** Related scale */
    private Scale scale;

    /** Lag view on staff lines, if so desired */
    private GlyphLagView lagView;

    /** Data needed for displayChart */
    private int[] histo;
    private int    maxHisto;
    private double histoRatio;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // LinesBuilder //
    //--------------//
    /**
     * Just allocate the LinesBuilder
     *
     * @param sheet the sheet on which the analysis is performed.
     */
    public LinesBuilder (Sheet sheet)
    {
        super(
            sheet,
            new GlyphLag(
                "hLag",
                StickSection.class,
                new HorizontalOrientation()),
            Steps.valueOf(Steps.LINES));
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report the list of staves found in the sheet
     *
     * @return the collection of staves found
     */
    @Implement(StavesBuilder.class)
    public List<StaffInfo> getStaves ()
    {
        return staves;
    }

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Perform the retrieval of the various staves
     * @throws StepException is processing must stop
     */
    @Implement(StavesBuilder.class)
    public void buildInfo ()
        throws StepException
    {
        scale = sheet.getScale();

        // Populate the lag
        SectionsBuilder<GlyphLag, GlyphSection> lagBuilder;
        lagBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
            lag,
            new JunctionDeltaPolicy(scale.toPixels(constants.maxDeltaLength)));
        lagBuilder.createSections(sheet.getPicture(), 0); // 0 = minRunLength
        sheet.setHorizontalLag(lag);

        // This is the heart of staff lines detection ...
        try {
            // Retrieve all peaks in horizontal projection
            List<Peak>           peaks = retrievePeaks(
                sheet.getPicture().getHeight());

            // Retrieve staff candidates, as sequences of peaks
            List<StaffCandidate> candidates = retrieveStaffCandidates(peaks);

            // Build staves out of candidates
            buildStaves(candidates);

            // Clean up all the staff lines in the found staves.
            cleanupStaves();

            // Determine limits in ordinate for each staff area
            if (!staves.isEmpty()) {
                computeStaffLimits();
            }

            // User feedback
            if (staves.size() > 1) {
                logger.info(sheet.getLogPrefix() + staves.size() + " staves");
            } else if (!staves.isEmpty()) {
                logger.info(sheet.getLogPrefix() + staves.size() + " staff");
            } else {
                logger.warning(
                    sheet.getLogPrefix() + "No staff found." +
                    " Check Line plot." +
                    " Check Staff Lines ratio in score parameters.");
            }

            // Record step information
            sheet.getBench()
                 .recordStaveCount(staves.size());
        } finally {
            // Display the resulting lag if so asked for
            if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
                displayFrame();
            }
        }

        if (staves.isEmpty()) {
            throw new StepException("Cannot proceed without staves");
        }
    }

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Build and display the histogram of projections
     */
    @Implement(StavesBuilder.class)
    public void displayChart ()
    {
        writePlot(histo, maxHisto, histoRatio);
    }

    //-------------//
    // buildStaves //
    //-------------//
    /**
     * Build each staff out of its staff candidate
     *
     * @param staffCandidates the collection of staff candidates
     */
    private void buildStaves (List<StaffCandidate> staffCandidates)
        throws StepException
    {
        // Use a new staff retriever
        StaffBuilder staffBuilder = new StaffBuilder(sheet, lag);

        for (StaffCandidate candidate : staffCandidates) {
            StaffInfo info = staffBuilder.buildInfo(candidate);
            staves.add(info);
        }
    }

    //---------------//
    // cleanupStaves //
    //---------------//
    private void cleanupStaves ()
    {
        for (StaffInfo staff : staves) {
            staff.cleanup();
        }
    }

    //-----------------//
    // computeInterval //
    //-----------------//
    private double computeInterval (Peak from,
                                    Peak to)
    {
        return (double) ((to.getTop() + to.getBottom()) - from.getTop() -
               from.getBottom()) / 2;
    }

    //--------------------//
    // computeStaffLimits //
    //--------------------//
    private void computeStaffLimits ()
    {
        StaffInfo prevStaff = null;

        for (StaffInfo staff : staves) {
            // Very first staff
            if (prevStaff == null) {
                staff.setAreaTop(0);
            } else {
                // Top of staff area, defined as middle ordinate between
                // ordinate of last line of previous staff and ordinate of
                // first line of current staff
                int middle = (prevStaff.getLastLine()
                                       .yAt(prevStaff.getLeft()) +
                             staff.getFirstLine()
                                  .yAt(staff.getLeft())) / 2;
                prevStaff.setAreaBottom(middle);
                staff.setAreaTop(middle);
            }

            // Remember this staff for next one
            prevStaff = staff;
        }

        // Bottom of last staff
        prevStaff.setAreaBottom(Integer.MAX_VALUE);
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        // Sections that, as members of staff lines, will be treated as specific
        List<GlyphSection> specifics = new ArrayList<GlyphSection>();

        /*
         * Populate the specific sections, to hide or display the removed
         * line sections. This assumes StraightLineInfo implementation (?)
         */

        // Browse StaffInfos
        for (StaffInfo staff : staves) {
            // Browse LineInfos
            for (LineInfo line : staff.getLines()) {
                specifics.addAll(line.getSections());
            }
        }

        GlyphsController controller = new GlyphsController(this);
        lagView = new MyView(lag, specifics, controller);

        final String  unit = sheet.getId() + ":LinesBuilder";
        BoardsPane    boardsPane = new BoardsPane(
            new PixelBoard(unit, sheet),
            new RunBoard(unit, lag),
            new SectionBoard(unit, lag.getLastVertexId(), lag),
            new GlyphBoard(unit, controller, null));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(lagView);
        sheet.getAssembly()
             .addViewTab(Step.LINES_TAB, slv, boardsPane);
    }

    //---------------//
    // retrievePeaks //
    //---------------//
    /**
     * Peaks are detected in the horizontal projections. The resulting list of
     * peaks is made of all peaks higher than a given threshold, so they are not
     * all related to staff lines. This will be filtered later.
     *
     * @param height the picture height
     *
     * @return the raw list of detected peaks
     */
    private List<Peak> retrievePeaks (int height)
    {
        // Peaks found in horizontal histogram
        List<Peak> peaks = new ArrayList<Peak>();

        // Initialize histogram to zeroes
        histo = new int[height];
        Arrays.fill(histo, 0);

        // Visit all vertices (sections) of the lag
        for (GlyphSection section : lag.getVertices()) {
            int y = section.getFirstPos();

            for (Run run : section.getRuns()) {
                histo[y++] += run.getLength();
            }
        }

        // Determine histogram threshold
        maxHisto = 0;

        for (int i = histo.length - 1; i >= 0; i--) {
            if (histo[i] > maxHisto) {
                maxHisto = histo[i];
            }
        }

        histoRatio = sheet.getHistoRatio();

        // Write histo data if so asked for
        if (constants.plotting.getValue()) {
            displayChart();
        }

        int  threshold = (int) (maxHisto * histoRatio);

        // Determine peaks in the histogram
        Peak peak = null;

        for (int i = 0; i < histo.length; i++) {
            if (histo[i] >= threshold) {
                if (peak == null) { // Entering a peak
                    peak = new Peak(i, histo[i]);
                } else { // Extending a peak
                    peak.extend(i, histo[i]);
                }
            } else {
                if (peak != null) {
                    // Exiting a peak, let's record it
                    peaks.add(peak);
                    peak = null;
                }
            }
        }

        // Make sure we don't forget a last peak
        if (peak != null) {
            peaks.add(peak);
        }

        // Dump peaks for debugging
        if (logger.isFineEnabled()) {
            logger.fine("Peak nb = " + peaks.size());

            int i = 0;

            for (Peak pk : peaks) {
                if (logger.isFineEnabled()) {
                    logger.fine(i++ + " " + pk);
                }
            }
        }

        return peaks;
    }

    //-------------------------//
    // retrieveStaffCandidates //
    //-------------------------//
    /**
     * Out of the collection of detected horizontal peaks, retrieve sequences of
     * (5) peaks that are very likely to correspond to a staff.
     * @param peaks the whole collection of horizontal peaks
     * @return the collection of staff candidates
     */
    private List<StaffCandidate> retrieveStaffCandidates (List<Peak> peaks)
        throws StepException
    {
        List<StaffCandidate> candidateStaves = new ArrayList<StaffCandidate>();
        int                  staffCount = 0;

        // Maximum deviation accepted in the series of peaks in a staff
        final double maxDeviation = scale.toPixelsDouble(
            constants.maxInterlineDeviation);

        // Maximum difference in interval between a 6th line and the average
        // interval in the previous 5 lines
        final double maxDiff = scale.toPixelsDouble(
            constants.maxInterlineDiffFrac);

        // Desired length of series (TODO)
        final int  interlineNb = 4;

        int        firstPeak = 0;
        int        lastPeak = 0;
        Population intervals = new Population();
        LineBuilder.reset();

        // Browse through the peak list
        Peak prevPeak = null;

        for (ListIterator<Peak> li = peaks.listIterator(); li.hasNext();) {
            // Get peak at hand
            Peak peak = li.next();

            if (logger.isFineEnabled()) {
                logger.fine((li.nextIndex() - 1) + " " + peak);
            }

            // If very first one, we don't yet have intervals
            if (li.nextIndex() == 1) {
                prevPeak = peak;

                continue;
            }

            // Compute interval with previous peak
            double interval = computeInterval(prevPeak, peak);

            if (logger.isFineEnabled()) {
                logger.fine("interval=" + interval);
            }

            intervals.includeValue(interval);
            prevPeak = peak;

            // Check for regularity of current series
            if (intervals.getCardinality() > 1) {
                double stdDev = intervals.getStandardDeviation();

                if (logger.isFineEnabled()) {
                    logger.fine("stdDev=" + (float) stdDev);
                }

                if (stdDev > maxDeviation) {
                    if (logger.isFineEnabled()) {
                        logger.fine("Interval gap detected");
                    }

                    intervals.reset(interval);
                } else if (intervals.getCardinality() == interlineNb) {
                    if (logger.isFineEnabled()) {
                        logger.fine("End of staff");
                    }

                    // We have a suitable series.  However, let's look for a
                    // better sixth one if any on the other side of the staff
                    lastPeak = li.nextIndex() - 1;
                    firstPeak = lastPeak - interlineNb;

                    if (li.hasNext()) {
                        Peak nextPeak = li.next();
                        interval = computeInterval(peak, nextPeak);

                        if ((Math.abs(interval - intervals.getMeanValue()) <= maxDiff) // Good candidate, compare with first one
                             &&
                            (nextPeak.getMax() > peaks.get(firstPeak)
                                                      .getMax())) {
                            if (logger.isFineEnabled()) {
                                logger.fine("Moving to sixth line");
                            }

                            // Fix computation of interval value
                            intervals.excludeValue(
                                computeInterval(
                                    peaks.get(firstPeak),
                                    peaks.get(firstPeak + 1)));
                            intervals.includeValue(interval);

                            // Update indices
                            firstPeak++;
                            lastPeak++;
                        } else {
                            li.previous(); // Undo the move to the sixth peak
                        }
                    }

                    // We now have a set of peaks that signals a staff area
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Staff from peaks " + firstPeak + " to " +
                            lastPeak);
                    }

                    StaffCandidate candidate = new StaffCandidate(
                        ++staffCount,
                        peaks.subList(firstPeak, lastPeak + 1),
                        intervals.getMeanValue());
                    candidateStaves.add(candidate);

                    // Move to the next peak, candidate for starting a new
                    // staff
                    if (li.hasNext()) {
                        intervals.reset();
                        prevPeak = li.next();

                        if (logger.isFineEnabled()) {
                            logger.fine((li.nextIndex() - 1) + " " + prevPeak);
                        }
                    }
                }
            }
        }

        return candidateStaves;
    }

    //-----------//
    // writePlot //
    //-----------//
    private void writePlot (int[]  histo,
                            int    maxHisto,
                            double ratio)
    {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Threshold line
        XYSeries thresholdSeries = new XYSeries(
            "Staff ratio used" + " [" + ratio + "]");
        thresholdSeries.add(0, ratio);
        thresholdSeries.add(-histo.length + 1, ratio);
        dataset.addSeries(thresholdSeries);

        // Projection data
        XYSeries dataSeries = new XYSeries("Projections");

        for (int i = 0; i < histo.length; i++) {
            dataSeries.add(-i, histo[i] / (double) maxHisto);
        }

        dataset.addSeries(dataSeries);

        // Chart
        JFreeChart chart = ChartFactory.createXYLineChart(
            sheet.getId() + " (Horizontal Projections)", // Title
            "Ordinate",
            "Ratios of horizontal counts",
            dataset, // Dataset
            PlotOrientation.HORIZONTAL, // orientation,
            true, // Show legend
            false, // Show tool tips
            false // urls
        );

        // Hosting frame
        ChartFrame frame = new ChartFrame(
            sheet.getId() + " - Staff Lines",
            chart,
            true);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------------//
    // LinesParameters //
    //-----------------//
    public static class LinesParameters
        extends AbstractBean
    {
        //~ Static fields/initializers -----------------------------------------

        /** Should the original staff lines be painted */
        public static final String ORIGINAL_LINES_PAINTING = "linePainting";

        //~ Methods ------------------------------------------------------------

        //-------------//
        // getInstance //
        //-------------//
        public static LinesParameters getInstance ()
        {
            return Holder.INSTANCE;
        }

        //-----------------//
        // setLinePainting //
        //-----------------//
        public void setLinePainting (boolean value)
        {
            boolean oldValue = constants.displayOriginalStaffLines.getValue();
            constants.displayOriginalStaffLines.setValue(value);
            firePropertyChange(
                ORIGINAL_LINES_PAINTING,
                oldValue,
                constants.displayOriginalStaffLines.getValue());
        }

        //----------------//
        // isLinePainting //
        //----------------//
        public boolean isLinePainting ()
        {
            return constants.displayOriginalStaffLines.getValue();
        }

        //-------------//
        // toggleLines //
        //-------------//
        /**
         * Action that toggles the display of original pixels for the staff
         * lines
         * @param e the event that triggered this action
         */
        @Action(selectedProperty = ORIGINAL_LINES_PAINTING)
        public void toggleLines (ActionEvent e)
        {
        }

        //~ Inner Classes ------------------------------------------------------

        private static class Holder
        {
            //~ Static fields/initializers -------------------------------------

            public static final LinesParameters INSTANCE = new LinesParameters();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Should we display a frame on Lags ? */
        Constant.Boolean displayFrame = new Constant.Boolean(
            true,
            "Should we display a frame on Lags ?");

        /** Should we display original staff lines */
        Constant.Boolean displayOriginalStaffLines = new Constant.Boolean(
            false,
            "Should we display original staff lines?");

        /** Maximum difference in length of two consecutives runs in the same section */
        Scale.Fraction maxDeltaLength = new Scale.Fraction(
            0.2d,
            "Maximum difference in length of two consecutives runs in the same section");

        /** Maximum deviation in the series of interline values in a staff */
        Scale.Fraction maxInterlineDeviation = new Scale.Fraction(
            0.5d,
            "Maximum deviation in the series of interline values in a staff");

        /** Maximum difference between a new interline and the current staff value */
        Scale.Fraction maxInterlineDiffFrac = new Scale.Fraction(
            0.1d,
            "Maximum difference between a new interline and the current staff value");

        /** Should we produce a chart of computed data ? */
        Constant.Boolean plotting = new Constant.Boolean(
            false,
            "Should we produce a chart of computed data ?");
    }

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends GlyphLagView
    {
        //~ Constructors -------------------------------------------------------

        //--------//
        // MyView //
        //--------//
        public MyView (GlyphLag           lag,
                       List<GlyphSection> specifics,
                       GlyphsController   controller)
        {
            super(
                lag,
                specifics,
                constants.displayOriginalStaffLines,
                controller,
                null);
            setName("LinesBuilder-View");

            // (Weakly) listening on LineParameters properties
            LinesParameters.getInstance()
                           .addPropertyChangeListener(
                new WeakPropertyChangeListener(this));
        }

        //~ Methods ------------------------------------------------------------

        //-------------//
        // renderItems //
        //-------------//
        @Override
        protected void renderItems (Graphics2D g)
        {
            // Draw the line info, lineset by lineset
            g.setColor(Color.black);

            for (StaffInfo staff : staves) {
                staff.render(g);
            }
        }
    }
}
