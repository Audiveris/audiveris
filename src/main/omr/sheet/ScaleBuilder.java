//----------------------------------------------------------------------------//
//                                                                            //
//                          S c a l e B u i l d e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.math.Histogram;

import omr.run.Orientation;
import omr.run.RunsRetriever;

import omr.score.Score;
import omr.score.common.PixelRectangle;

import omr.sheet.picture.Picture;
import omr.sheet.ui.SheetsController;

import omr.step.StepException;

import omr.util.Implement;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

/**
 * Class <code>ScaleBuilder</code> encapsulates the computation of a sheet
 * scale, by adding the most frequent foreground run length to the most frequent
 * background run length, since this gives the average interline value.
 *
 * <p>If we have not been able to retrieve the main run length for background
 * or for foreground, then we suspect a wrong image format. In that case,
 * the safe action is to stop the processing, by throwing a StepException.</p>
 *
 * <p>Otherwise, if the main interline value is below a certain threshold
 * (see constants.minInterline), then we suspect that the picture is not
 * a music sheet (it may rather be an image, a page of text, ...). In that
 * case and if the sheet at hand is part of a multi-page score, we propose to
 * simply discard this sheet.</p>
 *
 * @see omr.sheet.Scale
 *
 * @author Hervé Bitteur
 */
public class ScaleBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScaleBuilder.class);

    /** Line separator */
    private static String lineSeparator = System.getProperty("line.separator");

    //~ Instance fields --------------------------------------------------------

    /** Adapter for reading runs */
    private Adapter adapter;

    /** Related sheet */
    private Sheet sheet;

    /** Histogram on foreground runs */
    private Histogram<Integer> foreHisto;

    /** Histogram on background runs */
    private Histogram<Integer> backHisto;

    /** Minimum population percentage for validating an extremum */
    private final double quorumRatio = constants.minExtremaRatio.getValue();

    /** Most frequent length of foreground runs found in the picture */
    private Peak forePeak;

    /** Most frequent length of background runs found in the picture */
    private Peak backPeak;

    /** Second frequent length of background runs found in the picture */
    private Peak secondBackPeak;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScaleBuilder //
    //--------------//
    /**
     * (package private) constructor, to enable scale computation on a given
     * sheet
     *
     * @param sheet the sheet at hand
     */
    ScaleBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Display the scale histograms
     */
    public void displayChart ()
    {
        if (adapter != null) {
            adapter.writePlot();
        } else {
            logger.warning("No scale adapter available");
        }
    }

    //---------------//
    // retrieveScale //
    //---------------//
    /**
     * Retrieve the global scale values by processing the provided picture runs,
     * make decisions about the validity of current picture as a music page and
     * store the results as a {@link Scale} instance in the related sheet.
     * @throws StepException if processing must stop for this sheet.
     */
    public void retrieveScale ()
        throws StepException
    {
        Picture picture = sheet.getPicture();
        adapter = new Adapter(picture.getHeight() - 1);

        // Read the picture runs and retrieve the key run peaks
        RunsRetriever runsBuilder = new RunsRetriever(
            Orientation.VERTICAL,
            adapter);
        runsBuilder.retrieveRuns(
            new PixelRectangle(0, 0, picture.getWidth(), picture.getHeight()));

        // Check this page looks like music staves
        // If not, StepException is raised
        checkStaves();

        // Check we have acceptable resolution
        // If not, StepException is raised
        checkResolution();

        // Here, we keep going. Let's compute derived data
        int           interline = forePeak.value + backPeak.value;

        // Report results to the user
        StringBuilder sb = new StringBuilder(sheet.getLogPrefix());
        sb.append("Scale main black is ")
          .append(forePeak.value);

        sb.append(", white is ")
          .append(backPeak.value);

        if (secondBackPeak != null) {
            sb.append(", second white is ")
              .append(secondBackPeak.value);
        }

        sb.append(", main interline is ")
          .append(interline);

        Integer secondInterline = null;

        if (secondBackPeak != null) {
            secondInterline = forePeak.value + secondBackPeak.value;
            sb.append(", second interline is ")
              .append(secondInterline);
        }

        logger.info(sb.toString());

        // Register computation results
        Scale scale = new Scale(interline, forePeak.value, secondInterline);
        sheet.getBench()
             .recordScale(scale);
        sheet.setScale(scale);
    }

    //-----------------//
    // checkResolution //
    //-----------------//
    /**
     * Check global interline value, to detect pictures with too low
     * resolution or pictures which do not represent music staves
     * @param interline the retrieved interline value
     * @throws StepException if processing must stop on this sheet
     */
    void checkResolution ()
        throws StepException
    {
        int interline = forePeak.value + backPeak.value;

        if (interline < constants.minInterline.getValue()) {
            makeDecision(
                sheet.getId() + lineSeparator + "With an interline value of " +
                interline + " pixels," + lineSeparator +
                "either this page contains no staves," + lineSeparator +
                "or the picture resolution is too low.");
        }
    }

    //-------------//
    // checkStaves //
    //-------------//
    /**
     * Check we have foreground and background run peaks, with significant
     * percentage of runs population, otherwise we are not looking at staves
     * and the picture represents something else.
     * @throws StepException if processing must stop on this sheet
     */
    private void checkStaves ()
        throws StepException
    {
        String error = null;

        if (forePeak == null) {
            error = "No foreground runs found.";
        } else if (forePeak.ratio < quorumRatio) {
            error = "No regular foreground lines found.";
        } else if (backPeak == null) {
            error = "No background runs found.";
        } else if (backPeak.ratio < quorumRatio) {
            error = "No regularly spaced lines found.";
        }

        if (error != null) {
            makeDecision(
                sheet.getId() + lineSeparator + error + lineSeparator +
                "This sheet does not seem to contain staff lines.");
        }
    }

    //--------------//
    // makeDecision //
    //--------------//
    /**
     * An abnormal situation has been  found, as detailed in provided msg,
     * now how should we proceed, depending on batch mode or user answer.
     * @param msg the problem description
     * @throws StepException thrown when processing must stop
     */
    private void makeDecision (String msg)
        throws StepException
    {
        logger.warning(msg.replaceAll(lineSeparator, " "));

        Score score = sheet.getScore();

        if (Main.getGui() != null) {
            // Make sheet visible to the user
            SheetsController.getInstance()
                            .showAssembly(sheet);
        }

        if ((Main.getGui() == null) ||
            (Main.getGui()
                 .displayModelessConfirm(
            msg + lineSeparator + "OK for discarding this sheet?") == JOptionPane.OK_OPTION)) {
            if (score.isMultiPage()) {
                sheet.remove();
                throw new StepException("Sheet removed");
            } else {
                throw new StepException("Sheet ignored");
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Adapter //          
    //---------//
    /**
     * This adapter customizes RunsRetriever for our scaling purpose.
     * It handles the precise foreground and background run lengths retrieves
     * the various peaks and is able to display a chart on the related
     * populations if so asked by the user.
     */
    private class Adapter
        implements RunsRetriever.Adapter
    {
        //~ Instance fields ----------------------------------------------------

        private final Picture picture;
        private final int[]   fore; // (black) foreground runs
        private final int[]   back; // (white) background runs
        private int           maxForeground; // Threshold black / white

        //~ Constructors -------------------------------------------------------

        //---------//
        // Adapter //
        //---------//
        public Adapter (int hMax)
        {
            picture = sheet.getPicture();

            if (picture.getMaxForeground() != -1) {
                maxForeground = picture.getMaxForeground();
            } else {
                maxForeground = sheet.getMaxForeground();
            }

            // Allocate histogram counters
            fore = new int[hMax + 2];
            back = new int[hMax + 2];

            // Useful? 
            Arrays.fill(fore, 0);
            Arrays.fill(back, 0);
        }

        //~ Methods ------------------------------------------------------------

        //--------//
        // isFore //
        //--------//
        @Implement(RunsRetriever.Adapter.class)
        public boolean isFore (int level)
        {
            // Assuming black=0, white=255
            return level <= maxForeground;
        }

        //----------//
        // getLevel //
        //----------//
        @Implement(RunsRetriever.Adapter.class)
        public int getLevel (int coord,
                             int pos)
        {
            // Swap pos & coord since we work on vertical runs
            return picture.getPixel(pos, coord);
        }

        //---------//
        // backRun //
        //---------//
        @Implement(RunsRetriever.Adapter.class)
        public void backRun (int w,
                             int h,
                             int length)
        {
            back[length]++;
        }

        //---------//
        // foreRun //
        //---------//
        @Implement(RunsRetriever.Adapter.class)
        public void foreRun (int w,
                             int h,
                             int length,
                             int cumul)
        {
            fore[length]++;
        }

        //-----------//
        // terminate //
        //-----------//
        /**
         * Just compute scaling data from histograms, no decision yet.
         */
        @Implement(RunsRetriever.Adapter.class)
        public void terminate ()
        {
            if (logger.isFineEnabled()) {
                logger.info("fore: " + Arrays.toString(fore));
                logger.info("back: " + Arrays.toString(back));
            }

            StringBuilder sb = new StringBuilder(sheet.getLogPrefix());

            // Foreground peak
            foreHisto = createHistogram(fore);
            forePeak = Peak.createPeak(foreHisto);
            sb.append("fore: ")
              .append(forePeak);

            // Background peak
            backHisto = createHistogram(back);
            backPeak = Peak.createPeak(backHisto);
            sb.append(" back: ")
              .append(backPeak);

            // Second background peak?
            List<Entry<Integer, Integer>> backMaxima = backHisto.getMaxima(
                quorumRatio);

            if (backMaxima.size() > 1) {
                Entry<Integer, Integer> entry = backMaxima.get(1);
                secondBackPeak = new Peak(
                    entry.getKey(),
                    entry.getValue() / (double) backHisto.getTotalCount());
                sb.append(" secondBack: ")
                  .append(secondBackPeak);
            }

            logger.info(sb.toString());
        }

        //-----------//
        // writePlot //
        //-----------//
        public void writePlot ()
        {
            XYSeriesCollection dataset = new XYSeriesCollection();
            int                upper = Math.min(
                fore.length,
                ((backPeak != null) ? ((backPeak.value * 3) / 2) : 20));

            // Foreground
            int      foreThreshold = foreHisto.getQuorumValue(quorumRatio);
            XYSeries foreSeries = new XYSeries("Foreground: " + forePeak.value);

            for (int i = 0; i <= upper; i++) {
                foreSeries.add(i, fore[i]);
            }

            dataset.addSeries(foreSeries);

            // Background
            int      backThreshold = backHisto.getQuorumValue(quorumRatio);
            XYSeries backSeries = new XYSeries(
                "Background: " + backPeak.value +
                ((secondBackPeak != null) ? (" & " + secondBackPeak.value) : ""));

            for (int i = 0; i <= upper; i++) {
                backSeries.add(i, back[i]);
            }

            dataset.addSeries(backSeries);

            // Fore threshold line
            XYSeries foreThresholdSeries = new XYSeries(
                "Fore threshold: " + foreThreshold);
            foreThresholdSeries.add(0, foreThreshold);
            foreThresholdSeries.add(upper, foreThreshold);
            dataset.addSeries(foreThresholdSeries);

            // Back threshold line
            XYSeries backThresholdSeries = new XYSeries(
                "Back threshold: " + backThreshold);
            backThresholdSeries.add(0, backThreshold);
            backThresholdSeries.add(upper, backThreshold);
            dataset.addSeries(backThresholdSeries);

            // Chart
            JFreeChart chart = ChartFactory.createXYLineChart(
                sheet.getId() + " (Runs)", // Title
                "Lengths", // X-Axis label
                "Counts", // Y-Axis label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // orientation,
                true, // Show legend
                false, // Show tool tips
                false // urls
            );

            // Hosting frame
            ChartFrame frame = new ChartFrame(
                sheet.getId() + " - Runs",
                chart,
                true);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            RefineryUtilities.centerFrameOnScreen(frame);
            frame.setVisible(true);
        }

        //-----------------//
        // createHistogram //
        //-----------------//
        private Histogram<Integer> createHistogram (int... vals)
        {
            Histogram<Integer> histo = new Histogram<Integer>();

            for (int i = 0; i < vals.length; i++) {
                histo.increaseCount(i, vals[i]);
            }

            return histo;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Should we produce a chart on computed scale data ? */
        final Constant.Boolean plotting = new Constant.Boolean(
            false,
            "Should we produce a chart on computed scale data ?");

        /** Minimum number of pixels per interline */
        final Constant.Integer minInterline = new Constant.Integer(
            "Pixels",
            11,
            "Minimum number of pixels per interline");

        /** Minimum ratio of pixels for extrema acceptance */
        final Constant.Ratio minExtremaRatio = new Constant.Ratio(
            0.1,
            "Minimum ratio of pixels for extrema acceptance ");
    }

    //------//
    // Peak //
    //------//
    /**
     * Record a specific run length, together with its representative ratio.
     */
    private static final class Peak
    {
        //~ Instance fields ----------------------------------------------------

        final int    value; // Related run length
        final double ratio; // Ratio of this run length WRT all run lengths

        //~ Constructors -------------------------------------------------------

        public Peak (int    value,
                     double ratio)
        {
            this.value = value;
            this.ratio = ratio;
        }

        //~ Methods ------------------------------------------------------------

        public static Peak createPeak (Histogram histo)
        {
            Entry<Integer, Integer> entry = histo.getMaximum();

            return new Peak(
                entry.getKey(),
                entry.getValue() / (double) histo.getTotalCount());
        }

        @Override
        public String toString ()
        {
            return value + "(" + (int) (100 * ratio) + "%)";
        }
    }
}
