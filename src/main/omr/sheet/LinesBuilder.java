//-----------------------------------------------------------------------//
//                                                                       //
//                        L i n e s B u i l d e r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.sheet;

import omr.Main;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.glyph.Glyph;
import omr.glyph.GlyphDirectory;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;
import omr.glyph.ui.GlyphBoard;
import omr.lag.HorizontalOrientation;
import omr.lag.JunctionDeltaPolicy;
import omr.lag.LagBuilder;
import omr.lag.Run;
import omr.math.Cumul;
import omr.stick.Stick;
import omr.stick.StickSection;
import omr.stick.StickView;
import omr.ui.BoardsPane;
import omr.ui.PixelBoard;
import omr.ui.ScrollLagView;
import omr.ui.SectionBoard;
import omr.ui.ToggleHandler;
import omr.util.Clock;
import omr.util.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>LinesBuilder</code> is dedicated to the retrieval of the
 * grid of stave lines. The various series of staves lines are detected,
 * and their lines are carefully cleaned up when an object crosses
 * them. Note that staves are not yet gathered into Systems, this will be
 * done in the BarsBuilder processing.
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

    // Series of horizontal peaks that signal stave areas
    private List<StaveInfo> staves = new ArrayList<StaveInfo>();

    // Cached data
    private final Scale scale;
    private final Sheet sheet;
    private final int width;

    // Lag view on staff lines, if so desired
    private MyLagView lagView;

    // Show the (removed) former line sections
    private boolean showLines = false;

    // Specific model on section id
    private SpinnerModel idModel;

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
        this.width = picture.getWidth();

        // Retrieve the horizontal lag of runs
        hLag = new GlyphLag(new HorizontalOrientation());
        hLag.setId("hLag");
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

        // Determine limits in ordinate for each stave area
        computeStaveLimits();

        logger.info(staves.size() + " stave(s) found");

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

    public List<StaveInfo> getStaves ()
    {
        return staves;
    }

    //---------//
    // cleanup //
    //---------//
    private void cleanup ()
    {
        for (StaveInfo stave : staves) {
            stave.cleanup();
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
    // computeStaveLimits //
    //--------------------//
    private void computeStaveLimits ()
    {
        StaveInfo prevStave = null;

        for (StaveInfo stave : staves) {
            // Very first stave
            if (prevStave == null) {
                stave.setAreaTop(0);
            } else {
                // Top of stave area, defined as middle ordinate between
                // ordinate of last line of previous stave and ordinate of
                // first line of current stave
                int middle = (prevStave.getLastLine().getLine().yAt
                              (prevStave.getLeft())
                              + stave.getFirstLine().getLine().yAt
                              (stave.getLeft())) / 2;
                prevStave.setAreaBottom(middle);
                stave.setAreaTop(middle);
            }

            // Remember this stave for next one
            prevStave = stave;
        }

        // Bottom of last stave
        prevStave.setAreaBottom(Integer.MAX_VALUE);
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
        // Browse StaveInfos
        for (StaveInfo stave : staves) {
            // Browse LineInfos
            for (LineInfo line : stave.getLines()) {
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
            (lagView,
             new PixelBoard(),
             new SectionBoard(hLag.getLastVertexId()),
             new GlyphBoard(hLag.getLastGlyphId(), knownIds));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(lagView);
        sheet.getAssembly().addViewTab("Lines",  slv, boardsPane);
        slv.addAncestorListener
            (new ToggleHandler("Lines", lagView,
                               "Toggle before & after staff removal"));
    }

    //---------------//
    // retrieveStaves //
    //---------------//

    /**
     * Stave are detected in the list of (raw) peaks, simply by looking for
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
        final double maxDeviation = scale.fracToPixelsDouble(constants.maxInterlineDeviation);

        // Maximum difference in interval between a 6th line and the
        // average interval in the previous 5 lines
        final double maxDiff = scale.fracToPixelsDouble(constants.maxInterlineDiffFrac);

        // Desired length of series (TBD)
        final int interlineNb = 4;

        int firstPeak = 0;
        int lastPeak = 0;
        Cumul intervals = new Cumul();
        LineBuilder.reset();

        // Use a new stave retriever
        StaveBuilder staveBuilder = new StaveBuilder(sheet, hLag, vi);

        // Browse through the peak list
        Peak prevPeak = null;

        for (ListIterator<Peak> li = peaks.listIterator(); li.hasNext();) {
            // Get peak at hand
            Peak peak = li.next();

            if (logger.isDebugEnabled()) {
                logger.debug((li.nextIndex() - 1) + " " + peak);
            }

            // If very first one, we don't yet have intervals
            if (li.nextIndex() == 1) {
                prevPeak = peak;

                continue;
            }

            // Compute interval with previous peak
            double interval = computeInterval(prevPeak, peak);

            if (logger.isDebugEnabled()) {
                logger.debug("interval=" + interval);
            }

            intervals.include(interval);
            prevPeak = peak;

            // Check for regularity of current series
            if (intervals.getNumber() > 1) {
                double stdDev = intervals.getStdDeviation();

                if (logger.isDebugEnabled()) {
                    logger.debug("stdDev=" + (float) stdDev);
                }

                if (stdDev > maxDeviation) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Interval gap detected");
                    }

                    intervals.reset(interval);
                } else if (intervals.getNumber() == interlineNb) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("End of staff");
                    }

                    // We have a suitable series.  However, let's look for
                    // a better sixth one if any on the other side of the
                    // staff
                    lastPeak = li.nextIndex() - 1;
                    firstPeak = lastPeak - interlineNb;

                    if (li.hasNext()) {
                        Peak nextPeak = li.next();
                        interval = computeInterval(peak, nextPeak);

                        if ((Math.abs(interval - intervals.getMean()) <= maxDiff) // Good candidate, compare with first one

                            && (nextPeak.getMax() > peaks.get(firstPeak).getMax())) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Moving to sixth line");
                            }

                            // Fix computation of interval value
                            intervals.exclude
                                (computeInterval(peaks.get(firstPeak),
                                                 peaks.get(firstPeak + 1)));
                            intervals.include(interval);

                            // Update indices
                            firstPeak++;
                            lastPeak++;
                        } else {
                            li.previous(); // Undo the move to the sixth peak
                        }
                    }

                    // We now have a set of peaks that signals a stave area
                    if (logger.isDebugEnabled()) {
                        logger.debug("Staff from peaks " + firstPeak + " to "
                                     + lastPeak);
                    }

                    staves.add
                        (staveBuilder.buildInfo(peaks.subList(firstPeak,
                                                              lastPeak + 1),
                                                intervals.getMean()));

                    if (logger.isDebugEnabled()) {
                        System.out.println();
                    }

                    // Move to the next peak, candidate for starting a new
                    // staff
                    if (li.hasNext()) {
                        intervals.reset();
                        prevPeak = li.next();

                        if (logger.isDebugEnabled()) {
                            logger.debug((li.nextIndex() - 1) + " "
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
        if (logger.isDebugEnabled()) {
            logger.debug("Peak nb = " + peaks.size());

            int i = 0;
            for (Peak pk : peaks) {
                if (logger.isDebugEnabled()) {
                    logger.debug(i++ + " " + pk);
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
            (sheet.getName() + " Horizontal Projections", // Title
             "Ordinate",
             "Horizontal counts",
             dataset,                   // Dataset
             PlotOrientation.HORIZONTAL, // orientation,
             true,                      // Show legend
             false,                     // Show tool tips
             false                      // urls
             );

        // Hosting frame
        ChartFrame frame = new ChartFrame(sheet.getName() + " Staff Lines",
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
            extends StickView
    {
        //~ Constructors --------------------------------------------------

        //-----------//
        // MyLagView //
        //-----------//
        public MyLagView (GlyphLag           lag,
                          List<GlyphSection> specifics)
        {
            super(lag, specifics, LinesBuilder.this);
        }

        //~ Methods -------------------------------------------------------

        //-------------//
        // renderItems //
        //-------------//
        protected void renderItems (Graphics g)
        {
            // Draw the line info, lineset by lineset
            g.setColor(Color.black);

            for (StaveInfo stave : staves) {
                stave.render(g, getZoom());
            }
        }

        //--------//
        // toggle //
        //--------//
        /**
         * Override the toggle method, to react on the toggle event since
         * the pointed section may be different (between a staff line and a
         * patch section)
         */
        @Override
            public void toggle ()
        {
            super.toggle();

            // Use rubber information if any
            Rectangle rect = rubber.getRectangle();
            if (rect == null) {
                return;
            }
            if ((rect.width != 0) || (rect.height != 0)) {
                rectangleUpdated(null, rect);
            } else {
                pointUpdated(null, rubber.getCenter());
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
                 "Maximum deviation in the series of interline values in a stave");

        Scale.Fraction maxInterlineDiffFrac = new Scale.Fraction
                (0.1d,
                 "Maximum difference between a new interline and the current stave value");

        Constant.Boolean plotting = new Constant.Boolean
                (false,
                 "Should we produce a GnuPlot file of computed data ?");

        Constants ()
        {
            initialize();
        }
    }
}
