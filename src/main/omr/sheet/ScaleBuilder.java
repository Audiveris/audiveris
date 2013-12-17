//----------------------------------------------------------------------------//
//                                                                            //
//                          S c a l e B u i l d e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;
import static omr.WellKnowns.LINE_SEPARATOR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.image.PixelFilter;

import omr.math.Histogram;
import omr.math.Histogram.HistoEntry;
import omr.math.Histogram.MaxEntry;
import omr.math.Histogram.PeakEntry;

import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.score.Score;

import omr.sheet.Picture.SourceKey;
import omr.sheet.ui.SheetsController;

import omr.step.StepException;

import omr.util.StopWatch;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p>
 * A second foreground peak usually gives the average beam thickness.
 * And similarly, a second background peak may indicate a series of staves
 * with a different interline than the main series.</p>
 * <p>
 * Internally, additional validity checks are performed:<ol>
 * <li>Method {@link #checkStaves} looks at foreground and background
 * peak populations.
 * <p>
 * If these counts are below quorum values (see constants.quorumRatio),
 * we can suspect that the page does not contain regularly spaced staff lines.
 * </p></li>
 * <li>Method {@link #checkResolution} looks at foreground and background
 * peak keys.
 * <p>
 * If we have not been able to retrieve the main run length for background
 * or for foreground, then we suspect a wrong image format. In that case,
 * the safe action is to stop the processing, by throwing a StepException.
 * If the main interline value is below a certain threshold
 * (see constants.minResolution), then we suspect that the picture is not
 * a music sheet (it may rather be an image, a page of text, ...).</p></li>
 * </ol>
 * <p>
 * If we have doubts about the page at hand and if this page is part of a
 * multi-page score, we propose to simply discard this sheet. In batch, the
 * page is discarded without asking for confirmation.</p>
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
    private static final Logger logger = LoggerFactory.getLogger(
            ScaleBuilder.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Related sheet. */
    private final Sheet sheet;

    /** Keeper of vertical run length histograms, for foreground & background. */
    private VertHistoKeeper vertHistoKeeper;

    /** Keeper of horizontal run length histogram. */
    private HoriHistoKeeper horiHistoKeeper;

    /** Histogram on vertical foreground runs. */
    private Histogram<Integer> foreHisto;

    /** Histogram on vertical background runs. */
    private Histogram<Integer> backHisto;

    /** Histogram on horizontal foreground runs. */
    private Histogram<Integer> horiHisto;

    /** Absolute population percentage for validating an extremum. */
    private final double quorumRatio = constants.quorumRatio.getValue();

    /** Relative population percentage for reading foreground spread. */
    private final double foreSpreadRatio = constants.foreSpreadRatio.getValue();

    /** Relative population percentage for reading background spread. */
    private final double backSpreadRatio = constants.backSpreadRatio.getValue();

    /** Most frequent length of vertical foreground runs found. */
    private PeakEntry<Double> forePeak;

    /** Most frequent length of vertical background runs found. */
    private PeakEntry<Double> backPeak;

    /** Second frequent length of vertical foreground runs found, if any. */
    private MaxEntry<Integer> beamEntry;

    /** Most frequent length of horizontal foreground runs found, if any. */
    private MaxEntry<Integer> stemEntry;

    /** Second frequent length of vertical background runs found, if any. */
    private PeakEntry<Double> secondBackPeak;

    /** Resulting scale, if everything goes well. */
    private Scale scale;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // ScaleBuilder //
    //--------------//
    /**
     * Constructor to enable scale computation on a given sheet.
     *
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
        if (vertHistoKeeper != null) {
            vertHistoKeeper.writePlot();

            if (horiHistoKeeper != null) {
                horiHistoKeeper.writePlot();
            }
        } else {
            logger.info("No scale data available yet");
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
     *
     * @throws StepException if processing must stop for this sheet.
     */
    public void retrieveScale ()
            throws StepException
    {
        StopWatch watch = new StopWatch(
                "Scale builder for " + sheet.getPage().getId());

        try {
            Picture picture = sheet.getPicture();

            // Retrieve the whole table of foreground runs
            PixelFilter binaryFilter = (PixelFilter) picture.getSource(
                    Picture.SourceKey.BINARY);

            watch.start("Global vertical lag");

            RunsTableFactory vertFactory = new RunsTableFactory(
                    Orientation.VERTICAL,
                    binaryFilter,
                    0);
            RunsTable wholeVertTable = vertFactory.createTable(
                    "vertBinary");
            sheet.setWholeVerticalTable(wholeVertTable);
            vertFactory = null; // To allow garbage collection ASAP

            // Note: from that point on, we could simply discard the sheet picture
            // and save memory, since wholeVertTable contains all foreground pixels.
            // For the time being, it is kept alive for display purpose, and to
            // allow the dewarping of the initial picture.
            if (constants.disposeImage.isSet()) {
                picture.disposeSource(SourceKey.INITIAL); // To discard image
            }

            // Build the two histograms
            watch.start("Vertical histograms");
            vertHistoKeeper = new VertHistoKeeper(picture.getHeight() - 1);
            vertHistoKeeper.buildHistograms(
                    wholeVertTable,
                    picture.getWidth(),
                    picture.getHeight());

            // Retrieve the various histograms peaks
            retrieveVertPeaks();

            // Check this page looks like music staves. If not, throw StepException
            checkStaves();

            // Check we have acceptable resolution.  If not, throw StepException
            checkResolution();

            // Look at horizontal histo for stem thickness
            RunsTableFactory horiFactory = new RunsTableFactory(
                    Orientation.HORIZONTAL,
                    binaryFilter,
                    0);
            RunsTable horiTable = horiFactory.createTable("horiBinary");
            horiFactory = null; // To allow garbage collection ASAP
            horiHistoKeeper = new HoriHistoKeeper(picture.getWidth() - 1);
            horiHistoKeeper.buildHistograms(horiTable, picture.getHeight());
            retrieveHoriPeak();

            // Here, we keep going on with scale data
            scale = new Scale(
                    computeLine(),
                    computeInterline(),
                    computeBeam(),
                    computeStem(),
                    computeSecondInterline());

            logger.info("{}{}", sheet.getLogPrefix(), scale);
            sheet.setScale(scale);
            sheet.getBench()
                    .recordScale(scale);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //-----------------//
    // checkResolution //
    //-----------------//
    /**
     * Check global interline value, to detect pictures with too low
     * resolution or pictures which do not represent music staves.
     *
     * @throws StepException if processing must stop on this sheet
     */
    private void checkResolution ()
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
                    sheet.getId() + LINE_SEPARATOR + "With an interline value of "
                    + interline + " pixels," + LINE_SEPARATOR
                    + "either this page contains no staves," + LINE_SEPARATOR
                    + "or the picture resolution is too low (try 300 DPI).");
        }
    }

    //-------------//
    // checkStaves //
    //-------------//
    /**
     * Check we have foreground and background run peaks, with
     * significant percentage of runs population, otherwise we are not
     * looking at staves and the picture represents something else.
     *
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
                    sheet.getId() + LINE_SEPARATOR + error + LINE_SEPARATOR
                    + "This sheet does not seem to contain staff lines.");
        }
    }

    //-------------//
    // computeBeam //
    //-------------//
    private int computeBeam ()
    {
        if (beamEntry != null) {
            return beamEntry.getKey();
        }

        if (backPeak != null) {
            logger.info(
                    "{}No beam peak found, computing a default value",
                    sheet.getLogPrefix());

            return (int) Math.rint(
                    constants.beamAsBackRatio.getValue() * backPeak.getKey().best);
        }

        return -1;
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
     *
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

    //-------------//
    // computeStem //
    //-------------//
    private int computeStem ()
    {
        if (stemEntry != null) {
            return stemEntry.getKey();
        }

        if (forePeak != null) {
            logger.info(
                    "{}No stem peak found, computing a default value",
                    sheet.getLogPrefix());

            return (int) Math.rint(
                    constants.stemAsForeRatio.getValue() * forePeak.getKey().best);
        }

        return -1;
    }

    //---------//
    // getPeak //
    //---------//
    private PeakEntry<Double> getPeak (Histogram<?> histo,
                                       Double spreadRatio,
                                       int index)
    {
        PeakEntry<Double> peak = null;

        // Find peak(s) using quorum threshold
        List<PeakEntry<Double>> peaks = histo.getDoublePeaks(
                histo.getQuorumValue(quorumRatio));

        if (index < peaks.size()) {
            peak = peaks.get(index);

            // Refine peak using spread threshold?
            if (spreadRatio != null) {
                peaks = histo.getDoublePeaks(
                        histo.getQuorumValue(peak.getValue() * spreadRatio));

                if (index < peaks.size()) {
                    peak = peaks.get(index);
                }
            }
        }

        return peak;
    }

    //--------------//
    // makeDecision //
    //--------------//
    /**
     * An abnormal situation has been found, as detailed in provided msg,
     * now how should we proceed, depending on batch mode or user answer.
     *
     * @param msg the problem description
     * @throws StepException thrown when processing must stop
     */
    private void makeDecision (String msg)
            throws StepException
    {
        logger.warn(msg.replaceAll(LINE_SEPARATOR, " "));

        Score score = sheet.getScore();

        if (Main.getGui() != null) {
            // Make sheet visible to the user
            SheetsController.getInstance()
                    .showAssembly(sheet);
        }

        if ((Main.getGui() == null)
            || (Main.getGui()
                .displayModelessConfirm(
                        msg + LINE_SEPARATOR + "OK for discarding this sheet?") == JOptionPane.OK_OPTION)) {
            if (score.isMultiPage()) {
                sheet.remove(false);
                throw new StepException("Sheet removed");
            } else {
                throw new StepException("Sheet ignored");
            }
        }
    }

    //------------------//
    // retrieveHoriPeak //
    //------------------//
    private void retrieveHoriPeak ()
            throws StepException
    {
        List<MaxEntry<Integer>> horiMaxima = horiHisto.getLocalMaxima();

        if (!horiMaxima.isEmpty()) {
            MaxEntry<Integer> max = horiMaxima.get(0);

            if (max.getValue() >= quorumRatio) {
                stemEntry = max;
                logger.debug(" stem: {}", stemEntry);
            }
        }
    }

    //-------------------//
    // retrieveVertPeaks //
    //-------------------//
    private void retrieveVertPeaks ()
            throws StepException
    {
        StringBuilder sb = new StringBuilder(sheet.getLogPrefix());
        // Foreground peak
        forePeak = getPeak(foreHisto, foreSpreadRatio, 0);
        sb.append("fore:")
                .append(forePeak);

        if (forePeak.getValue() == 1d) {
            String msg = "All image pixels are foreground."
                         + " Check binarization parameters";
            logger.warn(msg);
            throw new StepException(msg);
        }

        // Background peak
        backPeak = getPeak(backHisto, backSpreadRatio, 0);

        if (backPeak.getValue() == 1d) {
            String msg = "All image pixels are background."
                         + " Check binarization parameters";
            logger.warn(msg);
            throw new StepException(msg);
        }

        // Second background peak?
        secondBackPeak = getPeak(backHisto, backSpreadRatio, 1);

        if (secondBackPeak != null) {
            // Check whether we should merge with first foreground peak
            // Test: Delta between peaks <= line thickness
            Histogram.Peak<Double> p1 = backPeak.getKey();
            Histogram.Peak<Double> p2 = secondBackPeak.getKey();

            if (Math.abs(p1.best - p2.best) <= forePeak.getKey().best) {
                backPeak = new PeakEntry(
                        new Histogram.Peak<Double>(
                                Math.min(p1.first, p2.first),
                                (p1.best + p2.best) / 2,
                                Math.max(p1.second, p2.second)),
                        (backPeak.getValue() + secondBackPeak.getValue()) / 2);
                secondBackPeak = null;
                logger.info("Merged two close background peaks");
            } else {
                // Check whether this second background peak can be an interline
                // We check that p2 is not too large, compared with p1
                if (p2.best > (p1.best * constants.maxSecondRatio.getValue())) {
                    logger.info(
                            "Second background peak too large {}, ignored",
                            p2.best);
                    secondBackPeak = null;
                }
            }
        }

        sb.append(" back:")
                .append(backPeak);

        if (secondBackPeak != null) {
            sb.append(" secondBack:")
                    .append(secondBackPeak);
        }

        // Second foreground peak (beam)?
        if ((forePeak != null) && (backPeak != null)) {
            // Take most frequent local max for which key (beam thickness) is 
            // larger than about twice the mean line thickness and smaller than
            // mean white gap between staff lines.
            List<MaxEntry<Integer>> foreMaxima = foreHisto.getLocalMaxima();
            double minBeamLineRatio = constants.minBeamLineRatio.getValue();
            double minHeight = minBeamLineRatio * forePeak.getKey().best;
            double maxHeight = backPeak.getKey().best;

            for (MaxEntry<Integer> max : foreMaxima) {
                if ((max.getKey() >= minHeight) && (max.getKey() <= maxHeight)) {
                    beamEntry = max;
                    sb.append(" beam:")
                            .append(beamEntry);

                    break;
                }
            }
        }

        logger.debug(sb.toString());
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print the StopWatch on binarization?");

        final Constant.Integer minResolution = new Constant.Integer(
                "Pixels",
                11,
                "Minimum resolution, expressed as number of pixels per interline");

        final Constant.Ratio quorumRatio = new Constant.Ratio(
                0.1,
                "Absolute ratio of total pixels for peak acceptance");

        final Constant.Ratio foreSpreadRatio = new Constant.Ratio(
                0.15,
                "Relative ratio of best count for foreground spread reading");

        final Constant.Ratio backSpreadRatio = new Constant.Ratio(
                0.3,
                "Relative ratio of best count for background spread reading");

        final Constant.Ratio spreadFactor = new Constant.Ratio(
                1.0,
                "Factor applied on line thickness spread");

        final Constant.Ratio minBeamLineRatio = new Constant.Ratio(
                2.5,
                "Minimum ratio between beam thickness and line thickness");

        final Constant.Ratio maxSecondRatio = new Constant.Ratio(
                2.0,
                "Maximum ratio between second and first background peak");

        final Constant.Boolean disposeImage = new Constant.Boolean(
                false,
                "Should we dispose of original image once binarized?");

        final Constant.Ratio beamAsBackRatio = new Constant.Ratio(
                0.8,
                "Default beam height defined as ratio of background peak");

        final Constant.Ratio stemAsForeRatio = new Constant.Ratio(
                1.0,
                "Default stem thickness defined as ratio of foreground peak");

    }

    //-----------------//
    // HoriHistoKeeper //
    //-----------------//
    /**
     * Handles the histogram of horizontal foreground runs.
     */
    private class HoriHistoKeeper
    {
        //~ Instance fields ----------------------------------------------------

        private final int[] fore; // (black) foreground runs

        // We are not interested of horizontal runs longer than this value
        private final int maxFore = 20;

        //~ Constructors -------------------------------------------------------
        /**
         * Create an instance of histoKeeper.
         *
         * @param wMax the maximum possible horizontal run length value
         */
        public HoriHistoKeeper (int wMax)
        {
            // Allocate histogram counters
            fore = new int[wMax + 2];

            // Useful?
            Arrays.fill(fore, 0);
        }

        //~ Methods ------------------------------------------------------------
        //
        //-----------//
        // writePlot //
        //-----------//
        public void writePlot ()
        {
            new Plotter(
                    "horizontal black",
                    fore,
                    horiHisto,
                    null,
                    stemEntry,
                    null,
                    maxFore).plot(new Point(80, 80));
        }

        //-----------------//
        // buildHistograms //
        //-----------------//
        private void buildHistograms (RunsTable horiTable,
                                      int height)
        {
            for (int y = 0; y < height; y++) {
                List<Run> runSeq = horiTable.getSequence(y);

                for (Run run : runSeq) {
                    // Process this foreground run
                    int foreLength = run.getLength();

                    if (foreLength <= maxFore) {
                        fore[foreLength]++;
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("fore values: {}", Arrays.toString(fore));
            }

            // Create foreground histogram
            horiHisto = createHistogram(fore);
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
     * In charge of building and displaying a chart on provided
     * collection of lengths values.
     */
    private class Plotter
    {
        //~ Instance fields ----------------------------------------------------

        private final String name;

        private final int[] values;

        private final Histogram<Integer> histo;

        private final Double spreadRatio;

        private final HistoEntry<? extends Number> peak;

        private final PeakEntry<Double> secondPeak;

        private final int upper;

        private final XYSeriesCollection dataset = new XYSeriesCollection();

        //~ Constructors -------------------------------------------------------
        public Plotter (String name,
                        int[] values,
                        Histogram<Integer> histo,
                        Double spreadRatio,
                        HistoEntry<? extends Number> peak,
                        PeakEntry<Double> secondPeak, // if any
                        int upper)
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

            if (spreadRatio != null) {
                plotSpreadLine("", (PeakEntry) peak);
            }

            // Second peak spread line?
            if (secondPeak != null) {
                plotSpreadLine("Second", secondPeak);
            }

            // Chart
            JFreeChart chart = ChartFactory.createXYLineChart(
                    sheet.getId() + " (" + name + " runs)", // Title
                    "Lengths - " + ((scale != null) ? scale : "*no scale*"), // X-Axis label
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
            int threshold = histo.getQuorumValue(quorumRatio);
            String pc = (int) (quorumRatio * 100) + "%";
            XYSeries series = new XYSeries("Quorum@" + pc + ":" + threshold);
            series.add(0, threshold);
            series.add(upper, threshold);
            dataset.addSeries(series);
        }

        private void plotSpreadLine (String prefix,
                                     PeakEntry<Double> peak)
        {
            if (peak != null) {
                int threshold = histo.getQuorumValue(
                        peak.getValue() * spreadRatio);
                String pc = (int) (spreadRatio * 100) + "%";
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
                double mainKey = peak.getBest()
                        .doubleValue();
                key = (int) mainKey;
            }

            if (secondPeak != null) {
                double secondKey = secondPeak.getBest();
                secKey = (int) secondKey;
            }

            XYSeries series = new XYSeries(
                    "Peak:" + key + "(" + (int) (peak.getValue() * 100) + "%)"
                    + ((secondPeak != null) ? (" & " + secKey) : ""));

            for (int i = 0; i <= upper; i++) {
                series.add(i, values[i]);
            }

            dataset.addSeries(series);

            // Derivative (just a try)
            XYSeries derivative = new XYSeries("Derivative");
            derivative.add(0, 0);

            for (int i = 1; i <= upper; i++) {
                derivative.add(i, values[i] - values[i - 1]);
            }

            dataset.addSeries(derivative);
        }
    }

    //-----------------//
    // VertHistoKeeper //
    //-----------------//
    /**
     * This class builds the precise vertical foreground and background
     * run lengths, it retrieves the various peaks and is able to
     * display a chart on the related populations.
     */
    private class VertHistoKeeper
    {
        //~ Instance fields ----------------------------------------------------

        private final int[] fore; // (black) foreground runs

        private final int[] back; // (white) background runs

        //~ Constructors -------------------------------------------------------
        /**
         * Create an instance of histoKeeper.
         *
         * @param hMax the maximum possible run length value
         */
        public VertHistoKeeper (int hMax)
        {
            // Allocate histogram counters
            fore = new int[hMax + 2];
            back = new int[hMax + 2];

            // Useful?
            Arrays.fill(fore, 0);
            Arrays.fill(back, 0);
        }

        //~ Methods ------------------------------------------------------------
        //
        //-----------//
        // writePlot //
        //-----------//
        public void writePlot ()
        {
            int upper = (int) Math.min(
                    fore.length,
                    ((backPeak != null) ? ((backPeak.getKey().best * 3) / 2) : 20));

            new Plotter(
                    "vertical black",
                    fore,
                    foreHisto,
                    foreSpreadRatio,
                    forePeak,
                    null,
                    upper).plot(new Point(0, 0));
            new Plotter(
                    "vertical white",
                    back,
                    backHisto,
                    backSpreadRatio,
                    backPeak,
                    secondBackPeak,
                    upper).plot(new Point(40, 40));
        }

        //-----------------//
        // buildHistograms //
        //-----------------//
        private void buildHistograms (RunsTable wholeVertTable,
                                      int width,
                                      int height)
        {
            // Upper bounds for run lengths
            final int maxBack = height / 4;
            final int maxFore = height / 16;

            for (int x = 0; x < width; x++) {
                List<Run> runSeq = wholeVertTable.getSequence(x);

                // Ordinate of first pixel not yet processed
                int yLast = 0;

                for (Run run : runSeq) {
                    int y = run.getStart();

                    if (y > yLast) {
                        // Process the background run before this run
                        int backLength = y - yLast;

                        if (backLength <= maxBack) {
                            back[backLength]++;
                        }
                    }

                    // Process this foreground run
                    int foreLength = run.getLength();

                    if (foreLength <= maxFore) {
                        fore[foreLength]++;
                    }

                    yLast = y + foreLength;
                }

                // Process a last background run, if any
                if (yLast < height) {
                    int backLength = height - yLast;

                    if (backLength <= maxBack) {
                        back[backLength]++;
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("fore values: {}", Arrays.toString(fore));
                logger.debug("back values: {}", Arrays.toString(back));
            }

            // Create foreground & background histograms
            foreHisto = createHistogram(fore);
            backHisto = createHistogram(back);
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
}
