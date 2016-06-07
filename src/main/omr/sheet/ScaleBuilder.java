//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S c a l e B u i l d e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import static omr.WellKnowns.LINE_SEPARATOR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.Histogram;
import omr.math.Histogram.MaxEntry;
import omr.math.Histogram.PeakEntry;
import omr.math.IntegerHistogram;

import omr.run.Run;
import omr.run.RunTable;

import omr.sheet.Scale.BeamScale;

import omr.step.StepException;

import omr.util.Navigable;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code ScaleBuilder} computes the global scale of a given sheet by processing
 * the image vertical runs.
 * Adding the most frequent foreground run length to the most frequent background run length,
 * would give the average interline value.
 * Instead of a posteriori addition, we analyze the total length of two combined runs in sequence.
 * <p>
 * A second foreground peak usually gives the average beam thickness.
 * And similarly, a second combined peak may indicate a series of staves with a different interline
 * than the main series.</p>
 * <p>
 * Internally, additional validity checks are performed:<ol>
 * <li>Method {@link #checkStaves} looks at foreground and combined peak populations.
 * <p>
 * If these counts are below quorum values (see constants.quorumRatio),
 * we can suspect that the page does not contain regularly spaced staff lines.
 * </p></li>
 * <li>Method {@link #checkResolution} looks at foreground and combined peak keys.
 * <p>
 * If we have not been able to retrieve the main foreground and combined peaks, then
 * we suspect a wrong image format. In that case, the safe action is to stop the processing, by
 * throwing a StepException.
 * If the main interline value is below a certain threshold (see constants.minResolution), then we
 * suspect that the picture is not a music sheet (it may rather be an image, a page of text, etc).
 * </p></li>
 * </ol>
 * <p>
 * If we have doubts about the page at hand and if this page is part of a multi-page score, we
 * propose to simply discard this sheet. In batch, the page is discarded without asking for
 * confirmation.</p>
 *
 * @see Scale
 *
 * @author Hervé Bitteur
 */
public class ScaleBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ScaleBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Keeper of vertical run length histograms, for foreground & background. */
    private VertHistoKeeper vertHistoKeeper;

    /** Histogram on vertical foreground runs. */
    private IntegerHistogram foreHisto;

    /** Histogram on vertical background runs. */
    private IntegerHistogram backHisto;

    /** Histogram on vertical pairs of runs. */
    private IntegerHistogram combinedHisto;

    /** Absolute population percentage for validating an extremum. */
    private final double quorumRatio = constants.quorumRatio.getValue();

    /** Absolute population percentage for validating a second extremum. */
    private final double secondQuorumRatio = constants.secondQuorumRatio.getValue();

    /** Most frequent length of vertical foreground runs found. */
    private PeakEntry<Double> forePeak;

    /** Most frequent length of vertical background runs found. */
    private PeakEntry<Double> backPeak;

    /** Most frequent length of vertical combined runs found. */
    private PeakEntry<Double> combinedPeak;

    /** Second frequent length of vertical foreground runs found, if any. */
    private MaxEntry<Integer> beamEntry;

    /** Second frequent length of vertical combined runs found, if any. */
    private PeakEntry<Double> combinedPeak2;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Constructor to enable scale computation on a given sheet.
     *
     * @param sheet the sheet at hand
     */
    public ScaleBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // displayChart //
    //--------------//
    /**
     * Display the scale histograms.
     */
    public void displayChart ()
    {
        if (vertHistoKeeper == null) {
            try {
                retrieveScale();
            } catch (StepException ignored) {
            }
        }

        if (vertHistoKeeper != null) {
            vertHistoKeeper.writePlot();
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
     * @return scale data for the sheet
     * @throws StepException if processing must stop for this sheet.
     */
    public Scale retrieveScale ()
            throws StepException
    {
        StopWatch watch = new StopWatch("Scale builder for " + sheet.getId());

        try {
            Picture picture = sheet.getPicture();

            // Build the two histograms
            watch.start("Vertical histograms");

            RunTable wholeVertTable = picture.getTable(Picture.TableKey.BINARY);
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

            // Here, we keep going on with scale data
            return new Scale(
                    computeLine(),
                    computeInterline(),
                    computeSecondInterline(),
                    computeBeam());
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

        if (combinedPeak == null) {
            throw new StepException("Missing combined peak");
        }

        int interline = (int) Math.rint(combinedPeak.getKey().best);

        if (interline == 0) {
            sheet.getStub().decideOnRemoval(
                    sheet.getId() + LINE_SEPARATOR + "Interline value is zero." + LINE_SEPARATOR
                    + "This sheet does not seem to contain staff lines.",
                    false);
        } else if (interline < constants.minResolution.getValue()) {
            sheet.getStub().decideOnRemoval(
                    sheet.getId() + LINE_SEPARATOR + "With an interline value of " + interline
                    + " pixels," + LINE_SEPARATOR + "either this sheet contains no staves,"
                    + LINE_SEPARATOR + "or the picture resolution is too low (try 300 DPI).",
                    false);
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
        } else if ((combinedPeak == null) || (combinedPeak.getValue() < quorumRatio)) {
            error = "No regularly spaced lines found.";
        }

        if (error != null) {
            sheet.getStub().decideOnRemoval(
                    sheet.getId() + LINE_SEPARATOR + error + LINE_SEPARATOR
                    + "This sheet does not seem to contain staff lines.",
                    false);
        }
    }

    //-------------//
    // computeBeam //
    //-------------//
    /**
     * Report the beam scale information for the sheet.
     *
     * @return the beam scale
     */
    private BeamScale computeBeam ()
    {
        if (beamEntry != null) {
            return new BeamScale(beamEntry.getKey(), false);
        }

        if (backPeak != null) {
            final int guess = (int) Math.rint(
                    constants.beamAsBackRatio.getValue() * backPeak.getKey().best);
            logger.info("No beam peak found, guessed value {}", guess);

            return new BeamScale(guess, true);
        }

        logger.warn("No global scale information available");

        return null;
    }

    //------------------//
    // computeInterline //
    //------------------//
    private Scale.Range computeInterline ()
    {
        if (combinedPeak != null) {
            int min = (int) Math.rint(combinedPeak.getKey().first);
            int best = (int) Math.rint(combinedPeak.getKey().best);
            int max = (int) Math.rint(combinedPeak.getKey().second);

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
        if (combinedPeak2 != null) {
            int min = (int) Math.rint(combinedPeak2.getKey().first);
            int best = (int) Math.rint(combinedPeak2.getKey().best);
            int max = (int) Math.rint(combinedPeak2.getKey().second);

            return new Scale.Range(min, best, max);
        } else {
            return null;
        }
    }

    //-------------------//
    // retrieveVertPeaks //
    //-------------------//
    private void retrieveVertPeaks ()
            throws StepException
    {
        StringBuilder sb = new StringBuilder();

        // Foreground peak
        forePeak = foreHisto.getPeak(quorumRatio, constants.foreSpreadRatio.getValue(), 0);
        sb.append("fore:").append(forePeak);

        // Background peak
        backPeak = backHisto.getPeak(quorumRatio, constants.backSpreadRatio.getValue(), 0);
        sb.append(" back:").append(backPeak);

        // Combined peak
        combinedPeak = combinedHisto.getPeak(
                quorumRatio,
                constants.combinedSpreadRatio.getValue(),
                0);

        if (combinedPeak != null) {
            // Second combined peak?
            combinedPeak2 = combinedHisto.getPeak(
                    secondQuorumRatio,
                    constants.combinedSpreadRatio.getValue(),
                    1);

            if (combinedPeak2 != null) {
                // Check whether we should merge with first combined peak
                // Test: Delta between peaks < line thickness
                Histogram.Peak<Double> p1 = combinedPeak.getKey();
                Histogram.Peak<Double> p2 = combinedPeak2.getKey();

                if (Math.abs(p1.best - p2.best) < forePeak.getKey().best) {
                    combinedPeak = new PeakEntry(
                            new Histogram.Peak<Double>(
                                    Math.min(p1.first, p2.first),
                                    (p1.best + p2.best) / 2,
                                    Math.max(p1.second, p2.second)),
                            (combinedPeak.getValue() + combinedPeak2.getValue()) / 2);
                    combinedPeak2 = null;
                    logger.info("Merged two close combined peaks");
                } else {
                    double min = Math.min(p1.best, p2.best);
                    double max = Math.max(p1.best, p2.best);

                    if ((max / min) > constants.maxSecondRatio.getValue()) {
                        logger.info("Second combined peak too different {}, ignored", p2.best);
                        combinedPeak2 = null;
                    }
                }
            }
        }

        sb.append(" combined:").append(combinedPeak);

        if (combinedPeak2 != null) {
            sb.append(" combined2:").append(combinedPeak2);
        }

        // Second foreground peak (beam)?
        if ((forePeak != null) && (backPeak != null)) {
            // Take most frequent local max for which key (beam thickness) is
            // larger than about twice the mean line thickness and smaller than
            // mean white gap between staff lines.
            List<MaxEntry<Integer>> foreMaxima = foreHisto.getPreciseMaxima();
            double minBeamLineRatio = constants.minBeamLineRatio.getValue();
            double minHeight = minBeamLineRatio * forePeak.getKey().best;
            double maxHeight = backPeak.getKey().best;

            for (MaxEntry<Integer> max : foreMaxima) {
                if ((max.getKey() >= minHeight) && (max.getKey() <= maxHeight)) {
                    beamEntry = max;
                    sb.append(" beam:").append(beamEntry);

                    break;
                }
            }
        }

        logger.debug(sb.toString());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print the StopWatch on scale computation?");

        private final Constant.Integer minResolution = new Constant.Integer(
                "Pixels",
                11,
                "Minimum resolution, expressed as number of pixels per interline");

        private final Constant.Ratio quorumRatio = new Constant.Ratio(
                0.1,
                "Absolute ratio of total pixels for peak acceptance");

        private final Constant.Ratio secondQuorumRatio = new Constant.Ratio(
                0.05,
                "Absolute ratio of total pixels for second peak acceptance");

        private final Constant.Ratio foreSpreadRatio = new Constant.Ratio(
                0.15,
                "Relative ratio of best count for foreground spread reading");

        private final Constant.Ratio backSpreadRatio = new Constant.Ratio(
                0.3,
                "Relative ratio of best count for background spread reading");

        private final Constant.Ratio combinedSpreadRatio = new Constant.Ratio(
                0.4,
                "Relative ratio of best count for combined spread reading");

        private final Constant.Ratio minBeamLineRatio = new Constant.Ratio(
                2.5,
                "Minimum ratio between beam thickness and line thickness");

        private final Constant.Ratio maxSecondRatio = new Constant.Ratio(
                2.0,
                "Maximum ratio between second and first combined peak");

        private final Constant.Ratio beamAsBackRatio = new Constant.Ratio(
                0.75,
                "Default beam height defined as ratio of background peak");
    }

    //-----------------//
    // VertHistoKeeper //
    //-----------------//
    /**
     * This class builds the precise vertical foreground and background run lengths,
     * it retrieves the various peaks and is able to display a chart on the related
     * populations.
     */
    private class VertHistoKeeper
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final int[] fore; // (black) foreground runs

        private final int[] back; // (white) background runs

        private final int[] combined; // Pairs of runs (back+fore and fore+back)

        //~ Constructors ---------------------------------------------------------------------------
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
            combined = new int[hMax + 2];

            // Useful?
            Arrays.fill(fore, 0);
            Arrays.fill(back, 0);
            Arrays.fill(combined, 0);
        }

        //~ Methods --------------------------------------------------------------------------------
        //
        //-----------//
        // writePlot //
        //-----------//
        public void writePlot ()
        {
            int upper = (int) Math.min(
                    fore.length,
                    (backPeak != null) ? (backPeak.getKey().best * 2) : 20);
            Scale scale = sheet.getScale();
            String xLabel = "Lengths - " + ((scale != null) ? scale : "*no scale*");

            try {
                new HistogramPlotter(
                        sheet,
                        "vertical black",
                        xLabel,
                        fore,
                        foreHisto,
                        forePeak,
                        null,
                        upper).plot(
                        new Point(0, 0),
                        constants.foreSpreadRatio.getValue(),
                        quorumRatio,
                        null);
            } catch (Throwable ex) {
                logger.warn("Error in plotting black", ex);
            }

            try {
                new HistogramPlotter(
                        sheet,
                        "vertical white",
                        xLabel,
                        back,
                        backHisto,
                        backPeak,
                        null,
                        upper).plot(
                        new Point(20, 20),
                        constants.backSpreadRatio.getValue(),
                        quorumRatio,
                        secondQuorumRatio);
            } catch (Throwable ex) {
                logger.warn("Error in plotting white", ex);
            }

            try {
                new HistogramPlotter(
                        sheet,
                        "vertical combined",
                        xLabel,
                        combined,
                        combinedHisto,
                        combinedPeak,
                        combinedPeak2,
                        upper).plot(
                        new Point(40, 40),
                        constants.combinedSpreadRatio.getValue(),
                        quorumRatio,
                        secondQuorumRatio);
            } catch (Throwable ex) {
                logger.warn("Error in plotting both", ex);
            }
        }

        //-----------------//
        // buildHistograms //
        //-----------------//
        private void buildHistograms (RunTable wholeVertTable,
                                      int width,
                                      int height)
        {
            // Upper bounds for run lengths
            final int maxBack = height / 4;
            final int maxFore = height / 16;

            for (int x = 0; x < width; x++) {
                int yLast = 0; // Ordinate of first pixel not yet processed
                int lastBackLength = 0; // Length of last valid background run
                int lastForeLength = 0; // Length of last valid foreground run

                for (Iterator<Run> it = wholeVertTable.iterator(x); it.hasNext();) {
                    Run run = it.next();
                    int y = run.getStart();

                    if (y > yLast) {
                        // Process the background run before this run
                        int backLength = y - yLast;

                        if (backLength <= maxBack) {
                            back[backLength]++;
                            lastBackLength = backLength;

                            if (lastForeLength != 0) {
                                combined[lastForeLength + lastBackLength]++;
                            }
                        } else {
                            lastForeLength = 0;
                            lastBackLength = 0;
                        }
                    }

                    // Process this foreground run
                    int foreLength = run.getLength();

                    if (foreLength <= maxFore) {
                        fore[foreLength]++;
                        lastForeLength = foreLength;

                        if (lastBackLength != 0) {
                            combined[lastForeLength + lastBackLength]++;
                        }
                    } else {
                        lastForeLength = 0;
                        lastBackLength = 0;
                    }

                    yLast = y + foreLength;
                }

                // Process a last background run, if any
                if (yLast < height) {
                    int backLength = height - yLast;

                    if (backLength <= maxBack) {
                        back[backLength]++;
                        lastBackLength = backLength;

                        if (lastForeLength != 0) {
                            combined[lastForeLength + lastBackLength]++;
                        }
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
            combinedHisto = createHistogram(combined);
        }

        //-----------------//
        // createHistogram //
        //-----------------//
        private IntegerHistogram createHistogram (int[] vals)
        {
            IntegerHistogram histo = new IntegerHistogram();

            for (int i = 0; i < vals.length; i++) {
                histo.increaseCount(i, vals[i]);
            }

            return histo;
        }
    }
}
