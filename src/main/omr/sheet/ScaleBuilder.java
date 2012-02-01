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
import omr.math.Histogram.MaxEntry;
import omr.math.Histogram.PeakEntry;

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

import java.awt.Point;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

/**
 * Class {@code ScaleBuilder} encapsulates the computation of a sheet
 * scale, by adding the most frequent foreground run length to the most
 * frequent background run length, since this gives the average
 * interline value.
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
 * @see Scale
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

    /** Absolute population percentage for validating an extremum */
    private final double quorumRatio = constants.quorumRatio.getValue();

    /** Relative population percentage for reading foreground spread */
    private final double foreSpreadRatio = constants.foreSpreadRatio.getValue();

    /** Relative population percentage for reading background spread */
    private final double backSpreadRatio = constants.backSpreadRatio.getValue();

    /** Foreground peak */
    private PeakEntry<Double> forePeak;

    /** Second frequent length of foreground runs found in the picture */
    private MaxEntry<Integer> beamEntry;

    /** Most frequent length of background runs found in the picture */
    private PeakEntry<Double> backPeak;

    /** Second frequent length of background runs found in the picture */
    private PeakEntry<Double> secondBackPeak;

    /** Resulting scale, if any */
    private Scale scale;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScaleBuilder //
    //--------------//
    /**
     * Constructor to enable scale computation on a given sheet.
     * @param sheet the sheet at hand
     */
    public ScaleBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Display the scale histograms.
     */
    public void displayChart ()
    {
        if (adapter != null) {
            adapter.writePlot();
        } else {
            logger.warning("No scale data available");
        }
    }

    //---------------//
    // retrieveScale //
    //---------------//
    /**
     * Retrieve the global scale values by processing the provided
     * picture runs, make decisions about the validity of current
     * picture as a music page and store the results as a {@link Scale}
     * instance in the related sheet.
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

        // Retrieve the various histograms peaks
        retrievePeaks();

        // Check this page looks like music staves. If not, throw StepException
        checkStaves();

        // Check we have acceptable resolution.  If not, throw StepException
        checkResolution();

        // Here, we keep going on scale data
        scale = new Scale(
            computeLine(),
            computeInterline(),
            computeBeam(),
            computeSecondInterline());

        logger.info(sheet.getLogPrefix() + scale);

        sheet.getBench()
             .recordScale(scale);

        sheet.setScale(scale);
    }

    //-----------------//
    // checkResolution //
    //-----------------//
    /**
     * Check global interline value, to detect pictures with too low
     * resolution or pictures which do not represent music staves.
     * @throws StepException if processing must stop on this sheet
     */
    void checkResolution ()
        throws StepException
    {
        if (forePeak == null) {
            throw new StepException("Missing black peak");
        }

        if (backPeak == null) {
            throw new StepException("Missing white peak");
        }

        int interline = (int) (forePeak.getKey().best + backPeak.getKey().best);

        if (interline < constants.minResolution.getValue()) {
            makeDecision(
                sheet.getId() + lineSeparator + "With an interline value of " +
                interline + " pixels," + lineSeparator +
                "either this page contains no staves," + lineSeparator +
                "or the picture resolution is too low.");
        }
    }

    //---------//
    // getPeak //
    //---------//
    private PeakEntry<Double> getPeak (Histogram histo,
                                       double    spreadRatio,
                                       int       index)
    {
        PeakEntry<Double>       peak = null;

        // Find peak(s) using quorum threshold
        List<PeakEntry<Double>> peaks = histo.getDoublePeaks(
            histo.getQuorumValue(quorumRatio));

        if (index < peaks.size()) {
            peak = peaks.get(index);

            // Refine peak using spread threshold
            peaks = histo.getDoublePeaks(
                histo.getQuorumValue(peak.getValue() * spreadRatio));

            if (index < peaks.size()) {
                peak = peaks.get(index);
            }
        }

        return peak;
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

        if ((forePeak == null) || (forePeak.getValue() < quorumRatio)) {
            error = "No significant black lines found.";
        } else if ((backPeak == null) || (backPeak.getValue() < quorumRatio)) {
            error = "No regularly spaced lines found.";
        }

        if (error != null) {
            makeDecision(
                sheet.getId() + lineSeparator + error + lineSeparator +
                "This sheet does not seem to contain staff lines.");
        }
    }

    //-------------//
    // computeBeam //
    //-------------//
    private Integer computeBeam ()
    {
        if (beamEntry != null) {
            return beamEntry.getKey();
        } else {
            if (backPeak != null) {
                logger.info("No beam peak found, computing a default value");

                return (int) Math.rint(0.7 * backPeak.getKey().best);
            } else {
                return null;
            }
        }
    }

    //------------------//
    // computeInterline //
    //------------------//
    private Scale.Range computeInterline ()
    {
        if ((forePeak != null) && (backPeak != null)) {
            int min = (int) Math.rint(
                forePeak.getKey().first + backPeak.getKey().first);
            int best = (int) Math.rint(
                forePeak.getKey().best + backPeak.getKey().best);
            int max = (int) Math.rint(
                forePeak.getKey().second + backPeak.getKey().second);

            return new Scale.Range(min, best, max);
        } else {
            return null;
        }
    }

    //-------------//
    // computeLine //
    //-------------//
    /**
     * Compute the range for line thickness.
     * The computation of line max is key for the rest of the application,
     * since it governs the threshold between horizontal and vertical lags.
     * @return the line range
     */
    private Scale.Range computeLine ()
    {
        if (forePeak != null) {
            int min = (int) Math.rint(forePeak.getKey().first);
            int best = (int) Math.rint(forePeak.getKey().best);
            int max = (int) Math.ceil(forePeak.getKey().second);

            return new Scale.Range(min, best, max);
        } else {
            return null;
        }
    }

    //------------------------//
    // computeSecondInterline //
    //------------------------//
    private Scale.Range computeSecondInterline ()
    {
        if (secondBackPeak != null) {
            int min = (int) Math.rint(
                forePeak.getKey().first + secondBackPeak.getKey().first);
            int best = (int) Math.rint(
                forePeak.getKey().best + secondBackPeak.getKey().best);
            int max = (int) Math.rint(
                forePeak.getKey().second + secondBackPeak.getKey().second);

            return new Scale.Range(min, best, max);
        } else {
            return null;
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

    //---------------//
    // retrievePeaks //
    //---------------//
    private void retrievePeaks ()
    {
        StringBuilder sb = new StringBuilder(sheet.getLogPrefix());

        // Foreground peak
        forePeak = getPeak(foreHisto, foreSpreadRatio, 0);
        sb.append("fore:")
          .append(forePeak);

        // Background peak
        backPeak = getPeak(backHisto, backSpreadRatio, 0);
        sb.append(" back:")
          .append(backPeak);

        // Second foreground peak (beam)? 
        if ((forePeak != null) && (backPeak != null)) {
            // Take first local max for which key (beam thickness) is larger
            // than twice the mean line thickness
            List<MaxEntry<Integer>> foreMaxima = foreHisto.getLocalMaxima();
            double                  minBeamLineRatio = constants.minBeamLineRatio.getValue();

            for (MaxEntry<Integer> max : foreMaxima) {
                if (max.getKey() <= (minBeamLineRatio * forePeak.getKey().best)) {
                    continue;
                }

                if (max.getKey() > backPeak.getKey().best) {
                    break;
                }

                beamEntry = max;
                sb.append(" beam:")
                  .append(beamEntry);

                break;
            }
        }

        // Second background peak?
        secondBackPeak = getPeak(backHisto, backSpreadRatio, 1);

        if (secondBackPeak != null) {
            sb.append(" secondBack:")
              .append(secondBackPeak);
        }

        if (logger.isFineEnabled()) {
            logger.info(sb.toString());
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Integer minResolution = new Constant.Integer(
            "Pixels",
            11,
            "Minimum resolution, expressed as number of pixels per interline");

        //
        final Constant.Ratio quorumRatio = new Constant.Ratio(
            0.1,
            "Absolute ratio of total pixels for peak acceptance");

        //
        final Constant.Ratio foreSpreadRatio = new Constant.Ratio(
            0.15,
            "Relative ratio of best count for foreground spread reading");

        //
        final Constant.Ratio backSpreadRatio = new Constant.Ratio(
            0.3,
            "Relative ratio of best count for background spread reading");

        //
        final Constant.Ratio spreadFactor = new Constant.Ratio(
            1.0,
            "Factor applied on line thickness spread");

        //
        final Constant.Ratio minBeamLineRatio = new Constant.Ratio(
            2.0,
            "Minimum ratio between beam thickness and line thickness");
    }

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
        @Implement(RunsRetriever.Adapter.class)
        public void terminate ()
        {
            if (logger.isFineEnabled()) {
                logger.info("fore values: " + Arrays.toString(fore));
                logger.info("back values: " + Arrays.toString(back));
            }

            // Create foreground & background histograms
            foreHisto = createHistogram(fore);
            backHisto = createHistogram(back);
        }

        //-----------//
        // writePlot //
        //-----------//
        public void writePlot ()
        {
            int upper = (int) Math.min(
                fore.length,
                ((backPeak != null) ? ((backPeak.getKey().best * 3) / 2) : 20));

            new Plotter(
                "black",
                fore,
                foreHisto,
                foreSpreadRatio,
                forePeak,
                null,
                upper).plot(new Point(0, 0));
            new Plotter(
                "white",
                back,
                backHisto,
                backSpreadRatio,
                backPeak,
                secondBackPeak,
                upper).plot(new Point(20, 20));
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

    //---------//
    // Plotter //
    //---------//
    /**
     * In charge of building and displaying a chart on provided runs collection
     */
    private class Plotter
    {
        //~ Instance fields ----------------------------------------------------

        private final String             name;
        private final int[]              values;
        private final Histogram<Integer> histo;
        private final double             spreadRatio;
        private final PeakEntry<Double>  peak;
        private final PeakEntry<Double>  secondPeak;
        private final int                upper;
        private final XYSeriesCollection dataset = new XYSeriesCollection();

        //~ Constructors -------------------------------------------------------

        public Plotter (String             name,
                        int[]              values,
                        Histogram<Integer> histo,
                        double             spreadRatio,
                        PeakEntry<Double>  peak,
                        PeakEntry<Double>  secondPeak, // if any
                        int                upper)
        {
            this.name = name;
            this.values = values;
            this.histo = histo;
            this.spreadRatio = spreadRatio;
            this.peak = peak;
            this.secondPeak = secondPeak;
            this.upper = upper;
        }

        //~ Methods ------------------------------------------------------------

        public void plot (Point upperLeft)
        {
            // All values, quorum line & spread line
            plotValues();
            plotQuorumLine();
            plotSpreadLine("", peak);

            // Second peak spread line?
            if (secondPeak != null) {
                plotSpreadLine("Second", secondPeak);
            }

            // Chart
            JFreeChart chart = ChartFactory.createXYLineChart(
                sheet.getId() + " (" + name + " runs)", // Title
                "Lengths " + ((scale != null) ? scale : "*no scale*"), // X-Axis label
                "Counts", // Y-Axis label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // orientation,
                true, // Show legend
                false, // Show tool tips
                false // urls
            );

            // Hosting frame
            ChartFrame frame = new ChartFrame(
                sheet.getId() + " - " + name + " runs",
                chart,
                true);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLocation(upperLeft);
            frame.setVisible(true);
        }

        private void plotQuorumLine ()
        {
            int      threshold = histo.getQuorumValue(quorumRatio);
            String   pc = (int) (quorumRatio * 100) + "%";
            XYSeries series = new XYSeries("Quorum@" + pc + ":" + threshold);
            series.add(0, threshold);
            series.add(upper, threshold);
            dataset.addSeries(series);
        }

        private void plotSpreadLine (String            prefix,
                                     PeakEntry<Double> peak)
        {
            if (peak != null) {
                int      threshold = histo.getQuorumValue(
                    peak.getValue() * spreadRatio);
                String   pc = (int) (spreadRatio * 100) + "%";
                XYSeries series = new XYSeries(
                    prefix + "Spread@" + pc + ":" + threshold);
                series.add((double) peak.getKey().first, threshold);
                series.add((double) peak.getKey().second, threshold);
                dataset.addSeries(series);
            }
        }

        private void plotValues ()
        {
            Integer key = null;
            Integer secKey = null;

            if (peak != null) {
                double mainKey = peak.getKey().best;
                key = (int) mainKey;
            }

            if (secondPeak != null) {
                double secondKey = secondPeak.getKey().best;
                secKey = (int) secondKey;
            }

            XYSeries series = new XYSeries(
                "Peak:" + key + "(" + (int) (peak.getValue() * 100) + "%)" +
                ((secondPeak != null) ? (" & " + secKey) : ""));

            for (int i = 0; i <= upper; i++) {
                series.add(i, values[i]);
            }

            dataset.addSeries(series);
        }
    }
}
