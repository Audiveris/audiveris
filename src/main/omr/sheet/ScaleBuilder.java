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

import omr.math.IntegerHistogram;
import omr.math.Range;

import omr.run.Run;
import omr.run.RunTable;

import omr.sheet.Scale.BeamScale;
import omr.sheet.Scale.InterlineScale;
import omr.sheet.Scale.LineScale;

import omr.step.StepException;

import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code ScaleBuilder} computes the global scale of a given sheet by processing
 * the image vertical runs.
 * <p>
 * Adding the most frequent black run length to the most frequent white run length,
 * would give the average interline value.
 * Instead of a posteriori addition, we analyze the total length of two combo runs in sequence.
 * <p>
 * A second black peak usually gives the average beam thickness.
 * And similarly, a second combo peak may indicate a series of staves with a different interline
 * than the main series.</p>
 * <p>
 * Internally, additional validity checks are performed:<ol>
 * <li>If we cannot retrieve black peak, we decide that the sheet does not contain significant
 * lines.</li>
 * <li>If we cannot retrieve combo peak, we decide that the sheet does not contain regularly spaced
 * staff lines.</li>
 * <li>Method {@link #checkResolution} looks at combo peak.
 * If the main interline value is below a certain threshold (see constants.minResolution), then we
 * suspect that the picture is not a music sheet (it may rather be an image, a page of text, etc).
 * </li>
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

    /** Whole vertical run table. */
    private RunTable binary;

    /** Keeper of vertical run length histograms, for foreground & background. */
    private HistoKeeper histoKeeper;

    /** Histogram on vertical black runs. */
    private IntegerHistogram blackHisto;

    /** Histogram on vertical pairs of runs (black+white and white+black). */
    private IntegerHistogram comboHisto;

    /** Most frequent length of vertical black runs found. */
    private Range blackPeak;

    /** Most frequent length of vertical combined runs found. */
    private Range comboPeak;

    /** Second frequent length of vertical combined runs found, if any. */
    private Range comboPeak2;

    /** Main beam thickness found, if any. */
    private Integer beamKey;

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
        if (histoKeeper == null) {
            try {
                retrieveScale();
            } catch (StepException ignored) {
            }
        }

        if (histoKeeper != null) {
            histoKeeper.writePlot();
        }
    }

    //---------------//
    // retrieveScale //
    //---------------//
    /**
     * Retrieve the global scale counts by processing the provided picture runs,
     * make decisions about the validity of current picture as a music page and store
     * the results as a {@link Scale} instance in the related sheet.
     *
     * @return scale data for the sheet
     * @throws StepException if processing must stop for this sheet.
     */
    public Scale retrieveScale ()
            throws StepException
    {
        binary = sheet.getPicture().getTable(Picture.TableKey.BINARY);
        histoKeeper = new HistoKeeper();

        histoKeeper.buildBlacks();
        histoKeeper.retrieveLinePeak(); // -> blackPeak (or StepException thrown)

        histoKeeper.buildCombos();
        histoKeeper.retrieveInterlinePeaks(); // -> comboPeak (or StepException thrown), comboPeak2?

        histoKeeper.retrieveBeamKey(); // -> beamKey?

        // Check we have acceptable resolution.  If not, throw StepException
        checkResolution();

        // Here, we keep going on with scale data
        return new Scale(
                new LineScale(blackPeak),
                new InterlineScale(comboPeak),
                (comboPeak2 != null) ? new InterlineScale(comboPeak2) : null,
                computeBeam());
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
        int interline = comboPeak.main;

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
    // computeBeam //
    //-------------//
    /**
     * Report the beam scale information for the sheet.
     * We use the retrieved beam key if any, otherwise we extrapolate a probable beam height as a
     * ratio of main white length.
     *
     * @return the beam scale
     */
    private BeamScale computeBeam ()
    {
        if (beamKey != null) {
            return new BeamScale(beamKey, false);
        }

        if (comboPeak != null) {
            final int guess = (int) Math.rint(
                    constants.beamAsWhiteRatio.getValue() * (comboPeak.main - blackPeak.main));
            logger.info("No beam key found, guessed value {}", guess);

            return new BeamScale(guess, true);
        }

        logger.warn("No global scale information available");

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer minResolution = new Constant.Integer(
                "Pixels",
                11,
                "Minimum resolution, expressed as number of pixels per interline");

        private final Constant.Ratio minCountRatio = new Constant.Ratio(
                0.1,
                "Ratio of total runs for peak acceptance");

        private final Constant.Ratio minGainRatio = new Constant.Ratio(
                0.03,
                "Minimum ratio of peak runs for peak extension");

        private final Constant.Ratio minDerivativeRatio = new Constant.Ratio(
                0.025,
                "Ratio of total runs for derivative acceptance");

        private final Constant.Ratio minBeamLineRatio = new Constant.Ratio(
                2.5,
                "Minimum ratio between beam thickness and line thickness");

        private final Constant.Ratio maxSecondRatio = new Constant.Ratio(
                2.0,
                "Maximum ratio between second and first combined peak");

        private final Constant.Ratio beamAsWhiteRatio = new Constant.Ratio(
                0.75,
                "Default beam height defined as ratio of background peak");

        private final Constant.Ratio minBlackRatio = new Constant.Ratio(
                0.001,
                "Minimum ratio of foreground pixels in image");
    }

    //-------------//
    // HistoKeeper //
    //-------------//
    /**
     * This class builds the precise vertical foreground and background run lengths,
     * it retrieves the various peaks and is able to display a chart on the related
     * populations.
     */
    private class HistoKeeper
    {
        //~ Instance fields ------------------------------------------------------------------------

        // Upper bounds for run lengths (assuming sheet height >= staff height)
        final int maxWhite;

        final int maxBlack;

        //~ Constructors ---------------------------------------------------------------------------
        public HistoKeeper ()
        {
            maxBlack = binary.getHeight() / 16;
            maxWhite = binary.getHeight() / 4;
            logger.debug(
                    "maxBlack:{}, maxWhite:{}, maxCombo:{}",
                    maxBlack,
                    maxWhite,
                    maxBlack + maxWhite);

            // Allocate histograms
            blackHisto = new IntegerHistogram(
                    "black",
                    maxBlack,
                    constants.minGainRatio.getValue(),
                    constants.minCountRatio.getValue(),
                    constants.minDerivativeRatio.getValue());
            comboHisto = new IntegerHistogram(
                    "combo",
                    maxBlack + maxWhite,
                    constants.minGainRatio.getValue(),
                    null,
                    constants.minDerivativeRatio.getValue());
        }

        //~ Methods --------------------------------------------------------------------------------
        //-------------//
        // buildBlacks //
        //-------------//
        /**
         * Populate the black histogram.
         */
        public void buildBlacks ()
        {
            for (int x = 0, width = binary.getWidth(); x < width; x++) {
                for (Iterator<Run> it = binary.iterator(x); it.hasNext();) {
                    int black = it.next().getLength();

                    if (black <= maxBlack) {
                        blackHisto.increaseCount(black, 1);
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                blackHisto.print(System.out);
            }
        }

        //-------------//
        // buildCombos //
        //-------------//
        /**
         * Populate the combo histogram.
         */
        public void buildCombos ()
        {
            for (int x = 0, width = binary.getWidth(); x < width; x++) {
                int yLast = 0; // Ordinate of first pixel not yet processed
                int lastBlack = 0; // Length of last valid black run

                for (Iterator<Run> it = binary.iterator(x); it.hasNext();) {
                    Run run = it.next();
                    final int y = run.getStart();
                    final int black = run.getLength();

                    if ((black < blackPeak.min) || (black > blackPeak.max)) {
                        lastBlack = 0;
                    } else {
                        if (y > yLast) {
                            // Process the white run before this black run
                            int white = y - yLast;

                            // A white run between valid black runs?: B1, W, B2
                            // Combo 1 is defined as B1 + W, that is [-----]
                            // Combo 2 is defined as W + B2, that is     [-----]
                            // combo1 + combo2 = 2 * (1/2 * B1 + W + 1/2 * B2) = 2 * combo
                            if ((white <= maxWhite) && (lastBlack != 0)) {
                                comboHisto.increaseCount(lastBlack + white, 1); // B1 + W
                                comboHisto.increaseCount(white + black, 1); // W + B2
                            }
                        }

                        lastBlack = black;
                    }

                    yLast = y + black;
                }
            }

            if (logger.isDebugEnabled()) {
                comboHisto.print(System.out);
            }
        }

        //-----------------//
        // retrieveBeamKey //
        //-----------------//
        /**
         * Try to retrieve a suitable beam key.
         * <p>
         * Take most frequent black local max for which key (beam thickness) is larger than about
         * twice the main line thickness and smaller than mean white gap between staff lines.
         */
        public void retrieveBeamKey ()
        {
            double minBeamLineRatio = constants.minBeamLineRatio.getValue();
            int minHeight = (int) Math.floor(minBeamLineRatio * blackPeak.main);
            int maxHeight = comboPeak.main - blackPeak.main;

            List<Integer> localMaxima = blackHisto.getLocalMaxima(minHeight - 1, maxHeight + 1);

            for (int local : localMaxima) {
                if ((local >= minHeight) && (local <= maxHeight)) {
                    beamKey = local;

                    break;
                }
            }
        }

        //------------------------//
        // retrieveInterlinePeaks //
        //------------------------//
        /**
         * Retrieve combo peak for interline (and interline2 if any).
         */
        public void retrieveInterlinePeaks ()
                throws StepException
        {
            // Combo peak(s)
            List<Range> comboPeaks = comboHisto.getHiLoPeaks();

            if (comboPeaks.isEmpty()) {
                sheet.getStub().invalidate();
                throw new StepException("No regularly spaced lines found");
            }

            // First combo peak
            comboPeak = comboPeaks.get(0);
            logger.debug("comboPeak: {}", comboPeak);

            // Second combo peak?
            if (comboPeaks.size() > 1) {
                comboPeak2 = comboPeaks.get(1);

                Range p1 = comboPeak;
                Range p2 = comboPeak2;

                // If delta between the two combo peaks is too small, we merge them
                if (Math.abs(p1.main - p2.main) < blackPeak.main) {
                    logger.info("Merging two close combo peaks {} & {}", comboPeak, comboPeak2);
                    comboPeak = new Range(
                            Math.min(p1.min, p2.min),
                            (p1.main + p2.main) / 2,
                            Math.max(p1.max, p2.max));
                    comboPeak2 = null;
                } else {
                    // If delta between the two combo peaks is too large, we ignore the second
                    double min = Math.min(p1.main, p2.main);
                    double max = Math.max(p1.main, p2.main);

                    if ((max / min) > constants.maxSecondRatio.getValue()) {
                        logger.info("Second combo peak too different {}, ignored", p2);
                        comboPeak2 = null;
                    }
                }
            }
        }

        //------------------//
        // retrieveLinePeak //
        //------------------//
        /**
         * Retrieve black peak for line thickness.
         */
        public void retrieveLinePeak ()
                throws StepException
        {
            // Check we have enough foreground material. If not, throw StepException
            checkBlack();

            // Black peaks
            List<Range> blackPeaks = blackHisto.getHiLoPeaks();

            if (blackPeaks.isEmpty()) {
                sheet.getStub().invalidate();
                throw new StepException("No significant black lines found");
            }

            blackPeak = blackPeaks.get(0);
            logger.debug("blackPeak: {}", blackPeak);
        }

        //-----------//
        // writePlot //
        //-----------//
        public void writePlot ()
        {
            // Determine a suitable upper value for x
            int upper;

            if (blackPeak != null) {
                upper = blackPeak.max;
            } else {
                upper = 30;
            }

            if (comboPeak != null) {
                upper = Math.max(upper, comboPeak.max);
            }

            if (comboPeak2 != null) {
                upper = Math.max(upper, comboPeak2.max);
            }

            upper = (upper * 5) / 4; // Add some margin

            Scale scale = sheet.getScale();
            String xLabel = "Lengths - " + ((scale != null) ? scale : "NO_SCALE");

            try {
                final String title = sheet.getId() + " " + blackHisto.name;
                blackHisto.new Plotter(title, xLabel, blackPeak, null, upper).plot(
                        new Point(20, 20));
            } catch (Throwable ex) {
                logger.warn("Error in plotting black", ex);
            }

            try {
                final String title = sheet.getId() + " " + comboHisto.name;
                comboHisto.new Plotter(title, xLabel, comboPeak, comboPeak2, upper).plot(
                        new Point(80, 80));
            } catch (Throwable ex) {
                logger.warn("Error in plotting combo", ex);
            }
        }

        //------------//
        // checkBlack //
        //------------//
        /**
         * Check we have a significant number of black pixels WRT image size,
         * otherwise the sheet is mostly blank and contains no music.
         *
         * @throws StepException if processing must stop on this sheet
         */
        private void checkBlack ()
                throws StepException
        {
            int blackCount = blackHisto.getWeight();
            int size = binary.getWidth() * binary.getHeight();
            double blackRatio = (double) blackCount / size;
            logger.debug("blackRatio: {}", blackRatio);

            if (blackRatio < constants.minBlackRatio.getValue()) {
                sheet.getStub().decideOnRemoval(
                        sheet.getId() + LINE_SEPARATOR + "Too few black pixels: "
                        + String.format("%.4f%%", 100 * blackRatio) + LINE_SEPARATOR
                        + "This sheet is almost blank.",
                        false);
            }
        }
    }
}
