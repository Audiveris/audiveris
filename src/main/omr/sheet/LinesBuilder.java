//----------------------------------------------------------------------------//
//                                                                            //
//                          L i n e s B u i l d e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphsModel;
import omr.glyph.GlyphSection;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;

import omr.lag.HorizontalOrientation;
import omr.lag.JunctionDeltaPolicy;
import omr.lag.Run;
import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.Section;
import omr.lag.SectionBoard;
import omr.lag.SectionsBuilder;

import omr.log.Logger;

import omr.math.Population;

import omr.sheet.picture.Picture;
import omr.sheet.ui.PixelBoard;

import omr.step.StepException;

import omr.stick.Stick;
import omr.stick.StickSection;

import omr.ui.BoardsPane;

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
 * are carefully cleaned up when an object crosses them. Note that staves are
 * not yet gathered into Systems, this will be done in the BarsBuilder
 * processing.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LinesBuilder
    extends GlyphsModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LinesBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** Series of horizontal peaks that signal staff areas */
    private List<StaffInfo> staves = new ArrayList<StaffInfo>();

    /** Related scale */
    private final Scale scale;

    /** Lag view on staff lines, if so desired */
    private GlyphLagView lagView;

    /** Histogram eeded for displayChart */
    private int[] histo;

    /** Threshold eeded for displayChart */
    private int threshold;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // LinesBuilder //
    //--------------//
    /**
     * This performs the retrieval of the various staves.
     *
     * @param sheet the sheet on which the analysis is performed.
     * @throws StepException when processing must be interrupted
     */
    public LinesBuilder (Sheet sheet)
        throws StepException
    {
        super(
            sheet,
            new GlyphLag(
                "hLag",
                StickSection.class,
                new HorizontalOrientation()));

        // Check output needed from previous steps
        scale = sheet.getScale(); // Will run Scale if not yet done
        sheet.getSkew(); // Will run Skew  if not yet done

        Picture                                 picture = sheet.getPicture();

        // Populate the lag
        SectionsBuilder<GlyphLag, GlyphSection> lagBuilder;
        lagBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
            lag,
            new JunctionDeltaPolicy(scale.toPixels(constants.maxDeltaLength)));
        lagBuilder.createSections(picture, 0); // 0 = minRunLength
        sheet.setHorizontalLag(lag);

        retrieveStaves(retrievePeaks(picture.getHeight()));

        // Clean up the staff lines in the found staves.
        cleanup();

        // Determine limits in ordinate for each staff area
        computeStaffLimits();

        // User feedback
        if (staves.size() > 1) {
            logger.info(staves.size() + " staves");
        } else if (staves.size() > 0) {
            logger.info(staves.size() + " staff");
        } else {
            logger.warning("No staff found");
        }

        // Display the resulting lag is so asked for
        if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
            displayFrame();
        }
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
    public List<StaffInfo> getStaves ()
    {
        return staves;
    }

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Build and display the histogram of projections
     */
    public void displayChart ()
    {
        writePlot(histo, threshold);
    }

    //---------//
    // cleanup //
    //---------//
    private void cleanup ()
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
        List<GlyphSection> members = new ArrayList<GlyphSection>();

        /*
         * Populate the specific sections, to hide or display the removed
         * line sections. This assumes StraightLineInfo implementation (?)
         */

        // Browse StaffInfos
        for (StaffInfo staff : staves) {
            // Browse LineInfos
            for (LineInfo line : staff.getLines()) {
                members.addAll(line.getSections());
            }
        }

        lagView = new MyView(lag, members);

        final String  unit = sheet.getRadix() + ":LinesBuilder";
        BoardsPane    boardsPane = new BoardsPane(
            sheet,
            lagView,
            new PixelBoard(unit, sheet),
            new RunBoard(unit, lag),
            new SectionBoard(unit, lag.getLastVertexId(), lag),
            new GlyphBoard(unit, this, null));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(lagView);
        sheet.getAssembly()
             .addViewTab("Lines", slv, boardsPane);
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

        for (int i = histo.length - 1; i >= 0; i--) {
            histo[i] = 0;
        }

        // Visit all vertices (sections) of the lag
        for (GlyphSection section : lag.getVertices()) {
            int y = section.getFirstPos();

            for (Run run : section.getRuns()) {
                histo[y++] += run.getLength();
            }
        }

        // Determine histogram threshold
        int maxHisto = 0;

        for (int i = histo.length - 1; i >= 0; i--) {
            if (histo[i] > maxHisto) {
                maxHisto = histo[i];
            }
        }

        threshold = (int) ((double) maxHisto * constants.histoThresholdFrac.getValue());

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

        // Write histo data if so asked for
        if (constants.plotting.getValue()) {
            displayChart();
        }

        return peaks;
    }

    //----------------//
    // retrieveStaves //
    //----------------//
    /**
     * Staff are detected in the list of (raw) peaks, simply by looking for
     * regular series of peaks.
     *
     * @param peaks the raw list of peaks found
     */
    private void retrieveStaves (List<Peak> peaks)
        throws StepException
    {
        // One single iterator, since from peak area to peak area, we keep
        // moving forward in an ordered list of vertices
        ArrayList<GlyphSection> vertices = new ArrayList<GlyphSection>(
            lag.getVertices());
        Collections.sort(vertices, Section.idComparator);

        ListIterator<GlyphSection> vi = vertices.listIterator();

        // Maximum deviation accepted in the series of peaks in a staff
        final double maxDeviation = scale.toPixelsDouble(
            constants.maxInterlineDeviation);

        // Maximum difference in interval between a 6th line and the average
        // interval in the previous 5 lines
        final double maxDiff = scale.toPixelsDouble(
            constants.maxInterlineDiffFrac);

        // Desired length of series (TBD)
        final int  interlineNb = 4;

        int        firstPeak = 0;
        int        lastPeak = 0;
        Population intervals = new Population();
        LineBuilder.reset();

        // Use a new staff retriever
        StaffBuilder staffBuilder = new StaffBuilder(sheet, lag, vi);

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

                    staves.add(
                        staffBuilder.buildInfo(
                            peaks.subList(firstPeak, lastPeak + 1),
                            intervals.getMeanValue()));

                    if (logger.isFineEnabled()) {
                        System.out.println();
                    }

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
    }

    //-----------//
    // writePlot //
    //-----------//
    private void writePlot (int[] histo,
                            int   threshold)
    {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Threshold
        XYSeries thresholdSeries = new XYSeries(
            "Threshold" + " [" + threshold + "]");
        thresholdSeries.add(0, threshold);
        thresholdSeries.add(-histo.length + 1, threshold);
        dataset.addSeries(thresholdSeries);

        // Projection data
        XYSeries dataSeries = new XYSeries("Projections");

        for (int i = 0; i < histo.length; i++) {
            dataSeries.add(-i, histo[i]);
        }

        dataset.addSeries(dataSeries);

        // Chart
        JFreeChart chart = ChartFactory.createXYLineChart(
            sheet.getRadix() + " (Horizontal Projections)", // Title
            "Ordinate",
            "Horizontal counts",
            dataset, // Dataset
            PlotOrientation.HORIZONTAL, // orientation,
            true, // Show legend
            false, // Show tool tips
            false // urls
        );

        // Hosting frame
        ChartFrame frame = new ChartFrame(
            sheet.getRadix() + " - Staff Lines",
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

        /** Singleton */
        private static volatile LinesParameters INSTANCE;

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
                "linePainting",
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
        @Action(selectedProperty = "linePainting")
        public void toggleLines (ActionEvent e)
        {
            // Trigger a repaint if needed
            Sheet currentSheet = SheetManager.getSelectedSheet();

            if (currentSheet != null) {
                LinesBuilder builder = currentSheet.getLinesBuilder();

                if ((builder != null) && (builder.lagView != null)) {
                    builder.lagView.repaint();
                }
            }
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
            false,
            "Should we display a frame on Lags ?");

        /** Should we display original staff lines */
        Constant.Boolean displayOriginalStaffLines = new Constant.Boolean(
            false,
            "Should we display original staff lines?");

        /** Peak threshold stated as a ratio of maximum histogram value */
        Constant.Ratio histoThresholdFrac = new Constant.Ratio(
            0.5d,
            "Peak threshold stated as a ratio of maximum histogram value");

        /** Maximum difference in length of two consecutives runs in the same section */
        Scale.Fraction maxDeltaLength = new Scale.Fraction(
            0.2d,
            "Maximum difference in length of two consecutives runs in the same section");

        /** Maximum deviation in the series of interline values in a staff */
        Scale.Fraction maxInterlineDeviation = new Scale.Fraction(
            0.15d,
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
                       List<GlyphSection> specifics)
        {
            super(
                lag,
                specifics,
                constants.displayOriginalStaffLines,
                LinesBuilder.this,
                null);
            setName("LinesBuilder-View");
        }

        //~ Methods ------------------------------------------------------------

        //-------------//
        // renderItems //
        //-------------//
        @Override
        protected void renderItems (Graphics g)
        {
            // Draw the line info, lineset by lineset
            g.setColor(Color.black);

            for (StaffInfo staff : staves) {
                staff.render(g);
            }
        }
    }
}
