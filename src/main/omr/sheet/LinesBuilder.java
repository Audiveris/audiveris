//-----------------------------------------------------------------------//
//                                                                       //
//                        L i n e s B u i l d e r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.Main;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.glyph.Glyph;
import omr.glyph.GlyphDirectory;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.ui.GlyphBoard;
import omr.lag.HorizontalOrientation;
import omr.lag.JunctionDeltaPolicy;
import omr.lag.LagBuilder;
import omr.lag.Run;
import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;
import omr.math.Population;
import omr.stick.Stick;
import omr.stick.StickSection;
import omr.stick.StickView;
import omr.ui.BoardsPane;
import omr.ui.PixelBoard;
import omr.ui.ToggleHandler;
import omr.util.Logger;

import static omr.selection.SelectionTag.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import java.awt.*;
import java.util.List;
import java.util.*;
import javax.swing.*;

/**
 * Class <code>LinesBuilder</code> is dedicated to the retrieval of the
 * grid of staff lines. The various series of staves lines are detected,
 * and their lines are carefully cleaned up when an object crosses
 * them. Note that staves are not yet gathered into Systems, this will be
 * done in the BarsBuilder processing.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LinesBuilder
    implements GlyphDirectory
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(LinesBuilder.class);

    //~ Instance variables ------------------------------------------------

    // Lag of horizontal runs
    private GlyphLag hLag;

    // Series of horizontal peaks that signal staff areas
    private List<StaffInfo> staves = new ArrayList<StaffInfo>();

    // Cached data
    private final Scale scale;
    private final Sheet sheet;

    // Lag view on staff lines, if so desired
    private MyLagView lagView;

    // Needed for displayChart
    private int[] histo;
    private int threshold;

    //~ Constructors ------------------------------------------------------

    //--------------//
    // LinesBuilder //
    //--------------//
    /**
     * This performs the retrieval of the various staves.
     *
     * @param sheet the sheet on which the analysis is performed.
     */
    public LinesBuilder (Sheet sheet)
            throws omr.ProcessingException
    {
        // Check output needed from previous steps
        this.sheet = sheet;
        this.scale = sheet.getScale();  // Will run Scale if not yet done
        sheet.getSkew();                // Will run Skew  if not yet done

        Picture picture = sheet.getPicture();

        // Retrieve the horizontal lag of runs
        hLag = new GlyphLag(new HorizontalOrientation());
        hLag.setName("hLag");
        hLag.setVertexClass(StickSection.class);

        new LagBuilder<GlyphLag, GlyphSection>().rip
                (hLag,
                 picture,
                 0, // minRunLength
                 new JunctionDeltaPolicy(constants.maxDeltaLength.getValue())); // maxDeltaLength
        sheet.setHorizontalLag(hLag);

        retrieveStaves(retrievePeaks(picture.getHeight()));

        // Clean up the staff lines in the found staves.
        cleanup();

        // Determine limits in ordinate for each staff area
        computeStaffLimits();

        logger.info(staves.size() + " staff(s) found");

        // Display the resulting lag is so asked for
        if (constants.displayFrame.getValue() &&
            Main.getJui() != null) {
            displayFrame();
        }
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Build and display the histogram of projections
     */
    public void displayChart()
    {
        writePlot(histo, threshold);
    }

    //-----------//
    // getEntity //
    //-----------//
    public Glyph getEntity (Integer id)
    {
        return hLag.getGlyph(id);
    }

    //--------//
    // getLag //
    //--------//
    /**
     * Report the underlying lag
     *
     * @return the horizontal lag
     */
    public GlyphLag getLag ()
    {
        return hLag;
    }

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
        return (double) ((to.getTop() + to.getBottom()) - from.getTop()
                         - from.getBottom()) / 2;
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
                int middle = (prevStaff.getLastLine().getLine().yAt
                              (prevStaff.getLeft())
                              + staff.getFirstLine().getLine().yAt
                              (staff.getLeft())) / 2;
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
        // Sections that, as members of staff lines, will be treated as
        // specific
        List<GlyphSection> members = new ArrayList<GlyphSection>();

        // Populate the lineMembers
        List<Integer> knownIds = new ArrayList<Integer>();
        knownIds.add(GlyphBoard.NO_VALUE);
        // Browse StaffInfos
        for (StaffInfo staff : staves) {
            // Browse LineInfos
            for (LineInfo line : staff.getLines()) {
                // Browse Sticks
                for (Stick stick : line.getSticks()) {
                    knownIds.add(new Integer(stick.getId()));
                    // Browse member sections
                    for (GlyphSection section : stick.getMembers()) {
                        members.add(section);
                    }
                }
            }
        }

        lagView = new MyLagView(hLag, members);

        BoardsPane boardsPane = new BoardsPane
            (sheet, lagView,
             new PixelBoard("LinesBuilder-PixelBoard"),
             new RunBoard(sheet.getSelection(HORIZONTAL_RUN),
                          "LinesBuilder-RunBoard"),
             new SectionBoard(sheet.getSelection(HORIZONTAL_SECTION),
                              sheet.getSelection(HORIZONTAL_SECTION_ID),
                              sheet.getSelection(PIXEL),
                              hLag.getLastVertexId(),
                              "LinesBuilder-SectionBoard"),
             new GlyphBoard(hLag.getLastGlyphId(), knownIds,
                            "LinesBuilder-GlyphBoard",
                sheet.getSelection(HORIZONTAL_GLYPH),
                sheet.getSelection(HORIZONTAL_GLYPH_ID)));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(lagView);
        sheet.getAssembly().addViewTab("Lines",  slv, boardsPane);
        slv.getComponent().addAncestorListener
            (new ToggleHandler("Lines", lagView,
                               "Toggle before & after staff removal"));
    }

    //---------------//
    // retrieveStaves //
    //---------------//
    /**
     * Staff are detected in the list of (raw) peaks, simply by looking for
     * regular series of peaks.
     *
     * @param peaks the raw list of peaks found
     */
    private void retrieveStaves (List<Peak> peaks)
            throws omr.ProcessingException
    {
        // One single iterator, since from peak area to peak area, we
        // keep moving forward in the ordered list of vertices
        ArrayList<GlyphSection> vertices = new ArrayList<GlyphSection>(hLag.getVertices());
        ListIterator<GlyphSection> vi = vertices.listIterator();

        // Maximum deviation accepted in the series of peaks in a staff
        final double maxDeviation = scale.toPixelsDouble(constants.maxInterlineDeviation);

        // Maximum difference in interval between a 6th line and the
        // average interval in the previous 5 lines
        final double maxDiff = scale.toPixelsDouble(constants.maxInterlineDiffFrac);

        // Desired length of series (TBD)
        final int interlineNb = 4;

        int firstPeak = 0;
        int lastPeak = 0;
        Population intervals = new Population();
        LineBuilder.reset();

        // Use a new staff retriever
        StaffBuilder staffBuilder = new StaffBuilder(sheet, hLag, vi);

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

                    // We have a suitable series.  However, let's look for
                    // a better sixth one if any on the other side of the
                    // staff
                    lastPeak = li.nextIndex() - 1;
                    firstPeak = lastPeak - interlineNb;

                    if (li.hasNext()) {
                        Peak nextPeak = li.next();
                        interval = computeInterval(peak, nextPeak);

                        if ((Math.abs(interval - intervals.getMeanValue()) <= maxDiff) // Good candidate, compare with first one

                            && (nextPeak.getMax() > peaks.get(firstPeak).getMax())) {
                            if (logger.isFineEnabled()) {
                                logger.fine("Moving to sixth line");
                            }

                            // Fix computation of interval value
                            intervals.excludeValue
                                (computeInterval(peaks.get(firstPeak),
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
                        logger.fine("Staff from peaks " + firstPeak + " to "
                                     + lastPeak);
                    }

                    staves.add
                        (staffBuilder.buildInfo(peaks.subList(firstPeak,
                                                              lastPeak + 1),
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
                            logger.fine((li.nextIndex() - 1) + " "
                                         + prevPeak);
                        }
                    }
                }
            }
        }
    }

    //---------------//
    // retrievePeaks //
    //---------------//
    /**
     * Peaks are detected in the horizontal projections. The resulting list
     * of peaks is made of all peaks higher than a given threshold, so they
     * are not all related to staff lines. This will be filtered later.
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
        for (GlyphSection section : hLag.getVertices()) {
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

        threshold = (int)
            ((double) maxHisto * constants.histoThresholdFrac.getValue());

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

    //-----------//
    // writePlot //
    //-----------//
    private void writePlot (int[] histo,
                            int threshold)
    {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Threshold
        XYSeries thresholdSeries = new XYSeries("Threshold" +
                                                " [" + threshold + "]");
        thresholdSeries.add(0,                threshold);
        thresholdSeries.add(-histo.length +1, threshold);
        dataset.addSeries(thresholdSeries);

        // Projection data
        XYSeries dataSeries = new XYSeries("Projections");
        for (int i = 0; i < histo.length; i++) {
            dataSeries.add(-i, histo[i]);
        }
        dataset.addSeries(dataSeries);

        // Chart
        JFreeChart chart = ChartFactory.createXYLineChart
            (sheet.getRadix() + " - Horizontal Projections", // Title
             "Ordinate",
             "Horizontal counts",
             dataset,                   // Dataset
             PlotOrientation.HORIZONTAL, // orientation,
             true,                      // Show legend
             false,                     // Show tool tips
             false                      // urls
             );

        // Hosting frame
        ChartFrame frame = new ChartFrame(sheet.getRadix() + " - Staff Lines",
                                          chart, true) ;
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    //~ Classes -----------------------------------------------------------

    //-----------//
    // MyLagView //
    //-----------//
    private class MyLagView
        extends StickView<Stick>
    {
        //~ Constructors --------------------------------------------------

        //-----------//
        // MyLagView //
        //-----------//
        public MyLagView (GlyphLag           lag,
                          List<GlyphSection> specifics)
        {
            super(lag, specifics, LinesBuilder.this);
            setName("LinesBuilder-View");

            setLocationSelection(sheet.getSelection(PIXEL));
            setSpecificSelections(sheet.getSelection(HORIZONTAL_RUN),
                                  sheet.getSelection(HORIZONTAL_SECTION));
            setGlyphSelection(sheet.getSelection(HORIZONTAL_GLYPH));

            // Other input
            sheet.getSelection(HORIZONTAL_SECTION_ID).addObserver(this);
        }

        //~ Methods -------------------------------------------------------

        //-------------//
        // renderItems //
        //-------------//
        @Override
            protected void renderItems (Graphics g)
        {
            // Draw the line info, lineset by lineset
            g.setColor(Color.black);

            for (StaffInfo staff : staves) {
                staff.render(g, getZoom());
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Constant.Boolean displayFrame = new Constant.Boolean
                (false,
                 "Should we display a frame on Lags ?");

        Constant.Double histoThresholdFrac = new Constant.Double
                (0.5d,
                 "Peak threshold stated as a ratio of maximum histogram value");

        Constant.Integer maxDeltaLength = new Constant.Integer
                (4,
                 "Maximum difference in length of two consecutives runs in the same section");

        Scale.Fraction maxInterlineDeviation = new Scale.Fraction
                (0.15d,
                 "Maximum deviation in the series of interline values in a staff");

        Scale.Fraction maxInterlineDiffFrac = new Scale.Fraction
                (0.1d,
                 "Maximum difference between a new interline and the current staff value");

        Constant.Boolean plotting = new Constant.Boolean
                (false,
                 "Should we produce a GnuPlot file of computed data ?");

        Constants ()
        {
            initialize();
        }
    }
}
