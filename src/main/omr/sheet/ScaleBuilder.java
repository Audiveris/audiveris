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

import omr.math.Histogram;
import omr.math.Histogram.MaxEntry;
import omr.math.Histogram.PeakEntry;

import omr.run.FilterDescriptor;
import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.score.Score;

import omr.sheet.picture.Picture;
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
 *
 * <p>A second foreground peak usually gives the average beam thickness.
 * And similarly, a second background peak may indicate a series of staves
 * with a different interline than the main series.</p>
 *
 * <p>Internally, additional validity checks are performed:<ol>
 * <li>Method {@link #checkStaves} looks at foreground and background
 * peak populations.
 * <p>If these counts are below quorum values (see constants.quorumRatio),
 * we can suspect that the page does not contain regularly spaced staff lines.
 * </p></li>
 * <li>Method {@link #checkResolution} looks at foreground and background
 * peak keys.
 * <p>If we have not been able to retrieve the main run length for background
 * or for foreground, then we suspect a wrong image format. In that case,
 * the safe action is to stop the processing, by throwing a StepException.
 * If the main interline value is below a certain threshold
 * (see constants.minResolution), then we suspect that the picture is not
 * a music sheet (it may rather be an image, a page of text, ...).</p></li>
 * </ol>
 *
 * <p>If we have doubts about the page at hand and if this page is part of a
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
    private static final Logger logger = LoggerFactory.getLogger(ScaleBuilder.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Related sheet. */
    private Sheet sheet;

    /** Keeper of run length histograms, for foreground & background. */
    private HistoKeeper histoKeeper;

    /** Histogram on foreground runs. */
    private Histogram<Integer> foreHisto;

    /** Histogram on background runs. */
    private Histogram<Integer> backHisto;

    /** Absolute population percentage for validating an extremum. */
    private final double quorumRatio = constants.quorumRatio.getValue();

    /** Relative population percentage for reading foreground spread. */
    private final double foreSpreadRatio = constants.foreSpreadRatio.getValue();

    /** Relative population percentage for reading background spread. */
    private final double backSpreadRatio = constants.backSpreadRatio.getValue();

    /** Foreground peak. */
    private PeakEntry<Double> forePeak;

    /** Second frequent length of foreground runs found, if any. */
    private MaxEntry<Integer> beamEntry;

    /** Most frequent length of background runs found. */
    private PeakEntry<Double> backPeak;

    /** Second frequent length of background runs found, if any. */
    private PeakEntry<Double> secondBackPeak;

    /** Resulting scale, if any. */
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
        if (histoKeeper != null) {
            histoKeeper.writePlot();
        } else {
            logger.warn("No scale data available");
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
        Picture picture = sheet.getPicture();

        // Binarization: Retrieve the whole table of foreground runs
        histoKeeper = new HistoKeeper(picture.getHeight() - 1);
        FilterDescriptor desc = sheet.getPage().getFilterParam().getTarget();
        logger.info("{}{} {}", sheet.getLogPrefix(), "Binarization", desc);
        sheet.getPage().getFilterParam().setActual(desc);

        StopWatch watch = new StopWatch("Binarization "
                                        + sheet.getPage().getId() + " " + desc);
        watch.start("Vertical runs");

        RunsTableFactory factory = new RunsTableFactory(
                Orientation.VERTICAL,
                desc.getFilter(picture),
                0);
        RunsTable wholeVertTable = factory.createTable("whole");
        sheet.setWholeVerticalTable(wholeVertTable);
        factory = null; // To allow garbage collection ASAP

        if (constants.printWatch.isSet()) {
            watch.print();
        }

        // Build the two histograms
        histoKeeper.buildHistograms(
                wholeVertTable,
                picture.getWidth(),
                picture.getHeight());

        // Retrieve the various histograms peaks
        retrievePeaks();

        // Check this page looks like music staves. If not, throw StepException
        checkStaves();

        // Check we have acceptable resolution.  If not, throw StepException
        checkResolution();

        // Here, we keep going on with scale data
        scale = new Scale(
                computeLine(),
                computeInterline(),
                computeBeam(),
                computeSecondInterline());

        logger.info("{}{}", sheet.getLogPrefix(), scale);

        sheet.getBench().recordScale(scale);

        sheet.setScale(scale);
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
    private Integer computeBeam ()
    {
        if (beamEntry != null) {
            return beamEntry.getKey();
        } else {
            if (backPeak != null) {
                logger.info("{}{}", sheet.getLogPrefix(),
                        "No beam peak found, computing a default value");

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

    //---------//
    // getPeak //
    //---------//
    private PeakEntry<Double> getPeak (Histogram<?> histo,
                                       double spreadRatio,
                                       int index)
    {
        PeakEntry<Double> peak = null;

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
            SheetsController.getInstance().showAssembly(sheet);
        }

        if ((Main.getGui() == null)
            || (Main.getGui().displayModelessConfirm(
                msg + LINE_SEPARATOR + "OK for discarding this sheet?") == JOptionPane.OK_OPTION)) {
            if (score.isMultiPage()) {
                sheet.remove(false);
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
            throws StepException
    {
        StringBuilder sb = new StringBuilder(sheet.getLogPrefix());
        // Foreground peak
        forePeak = getPeak(foreHisto, foreSpreadRatio, 0);
        sb.append("fore:").append(forePeak);
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
                        new Histogram.Peak<>(
                        Math.min(p1.first, p2.first),
                        (p1.best + p2.best) / 2,
                        Math.max(p1.second, p2.second)),
                        (backPeak.getValue() + secondBackPeak.getValue()) / 2);
                secondBackPeak = null;
                logger.info("Merged two close background peaks");
            } else {
                // Check whether this second background peak can be an interline
                // We check that p2 is not too large, compared with p1
                if (p2.best > p1.best * constants.maxSecondRatio.getValue()) {
                    logger.info("Second background peak too large {}, ignored",
                            p2.best);
                    secondBackPeak = null;
                }
            }
        }

        sb.append(" back:").append(backPeak);

        if (secondBackPeak != null) {
            sb.append(" secondBack:").append(secondBackPeak);
        }

        // Second foreground peak (beam)?
        if ((forePeak != null) && (backPeak != null)) {
            // Take most frequent local max for which key (beam thickness) is 
            // larger than twice the mean line thickness and smaller than
            // mean white gap between staff lines.
            List<MaxEntry<Integer>> foreMaxima = foreHisto.getLocalMaxima();
            double minBeamLineRatio = constants.minBeamLineRatio.getValue();
            double minHeight = minBeamLineRatio * forePeak.getKey().best;
            double maxHeight = backPeak.getKey().best;

            for (MaxEntry<Integer> max : foreMaxima) {
                if (max.getKey() >= minHeight && max.getKey() <= maxHeight) {
                    beamEntry = max;
                    sb.append(" beam:").append(beamEntry);

                    break;
                }
            }
        }

        logger.debug(sb.toString());
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //-------------//
    // HistoKeeper //
    //-------------//
    /**
     * This class builds the precise foreground and background run
     * lengths, it retrieves the various peaks and is able to display a
     * chart on the related populations if so asked by the user.
     * It first builds the whole table of foreground vertical runs, which will
     * be reused in following step (GRID).
     */
    private class HistoKeeper
    {
        //~ Instance fields ----------------------------------------------------

        private final int[] fore; // (black) foreground runs

        private final int[] back; // (white) background runs

        //~ Constructors -------------------------------------------------------
        //
        //-------------//
        // HistoKeeper //
        //-------------//
        /**
         * Create an instance of histoKeeper.
         *
         * @param hMax the maximum possible run length
         */
        public HistoKeeper (int hMax)
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
            Histogram<Integer> histo = new Histogram<>();

            for (int i = 0; i < vals.length; i++) {
                histo.increaseCount(i, vals[i]);
            }

            return histo;
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
    }

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

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print the StopWatch on binarization?");

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

        private final String name;

        private final int[] values;

        private final Histogram<Integer> histo;

        private final double spreadRatio;

        private final PeakEntry<Double> peak;

        private final PeakEntry<Double> secondPeak;

        private final int upper;

        private final XYSeriesCollection dataset = new XYSeriesCollection();

        //~ Constructors -------------------------------------------------------
        public Plotter (String name,
                        int[] values,
                        Histogram<Integer> histo,
                        double spreadRatio,
                        PeakEntry<Double> peak,
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
                double mainKey = peak.getKey().best;
                key = (int) mainKey;
            }

            if (secondPeak != null) {
                double secondKey = secondPeak.getKey().best;
                secKey = (int) secondKey;
            }

            XYSeries series = new XYSeries(
                    "Peak:" + key + "(" + (int) (peak.getValue() * 100) + "%)"
                    + ((secondPeak != null) ? (" & " + secKey) : ""));

            for (int i = 0; i <= upper; i++) {
                series.add(i, values[i]);
            }

            dataset.addSeries(series);
        }
    }
}
