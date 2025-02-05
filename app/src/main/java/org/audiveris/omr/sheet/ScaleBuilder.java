//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S c a l e B u i l d e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.HiLoPeakFinder;
import org.audiveris.omr.math.HiLoPeakFinder.Quorum;
import org.audiveris.omr.math.IntegerFunction;
import org.audiveris.omr.math.Range;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Scale.BeamScale;
import org.audiveris.omr.sheet.Scale.Info;
import org.audiveris.omr.sheet.Scale.InterlineScale;
import org.audiveris.omr.sheet.Scale.LineScale;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.util.ChartPlotter;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>ScaleBuilder</code> computes the global scale of a given sheet by processing
 * the image vertical runs.
 * <p>
 * We build two histograms, one named 'black' for the length of black runs and
 * one named 'combo' for the combined length of a black run with its following white run
 * (or the combined length of a white run with its following black run).
 * <p>
 * For each histogram, black or combo, we look only at the highest peaks found (the peak height is
 * the number of occurrences for a given run length) and a threshold is applied to filter out peaks
 * that are not significant enough.
 * <dl>
 * <dt>Use of <b>BLACK</b> histogram:</dt>
 * <ol>
 * <li>The very high peak corresponds to staff line thickness.
 * <li>If any, the first next in height corresponds to the thickness of beams.
 * <li>And if any, the second next in height corresponds to the thickness of a second population of
 * beams.
 * <p>
 * NOTA: This second population of beams can exist only when there is a second population of
 * stave heights.
 * </ol>
 * <dt>Use of <b>COMBO</b> histogram:</dt>
 * <ol>
 * <li>The highest peak corresponds to staff interline value.
 * <li>If any, the next in height corresponds to the interline value of a second population of
 * staves.
 * </ol>
 * </dl>
 * NOTA: The notion of 'first' vs 'second' peak (for staff interline, for beam height) simply means
 * that we have found two distinct peaks and that 'first' key appears more <b>frequently</b> than
 * 'second' key.
 * But more frequent does not mean larger or smaller, which is given by the key itself.
 * <p>
 * Internally, additional validity checks are performed:
 * <ol>
 * <li>If we cannot retrieve black peak, we decide that the sheet does not contain significant
 * lines.
 * <li>If we cannot retrieve combo peak, we decide that the sheet does not contain regularly spaced
 * staff lines.
 * <li>Method {@link #checkResolution} looks at combo peak.
 * If the main interline value is below a certain threshold (see constants.minInterline), then we
 * suspect that the picture is not a music sheet (it may rather be an image, a page of text, etc).
 * </ol>
 * <p>
 * If we have doubts about the page at hand and if this page is part of a multi-page score, we
 * propose to simply discard this sheet. In batch, the page is discarded without asking for
 * confirmation.
 *
 * @see Scale
 * @author Hervé Bitteur
 */
public class ScaleBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ScaleBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Whole vertical run table. */
    private RunTable binary;

    /** Keeper of vertical run length histograms, for foreground and background. */
    private HistoKeeper histoKeeper;

    /** Most frequent length of vertical black runs found. */
    private Range blackPeak;

    /** Most frequent length of vertical combined runs found. */
    private Range comboPeak;

    /** Second frequent length of vertical combined runs found, if any. */
    private Range comboPeak2;

    /** Main beam thickness found, if any. */
    private Integer beamKey;

    /** Second beam thickness found, if any. */
    private Integer beamKey2;

    /** Guessed beam thickness if ever, for lack of 'beamKey'. */
    private Integer beamGuess;

    private Integer interlineKey;

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

    //-----------------//
    // checkResolution //
    //-----------------//
    /**
     * Check the global interline value, to detect pictures with too low
     * resolution or pictures which do not represent music staves.
     * <p>
     * <b>Coding note</b>:
     * The use of text blocks led to a parsing error by the <b>xsltproc</b> software
     * that is today mandatory to generate the documentation of .omr files.
     * Hence these text blocks have been replaced by plain String entities.
     *
     * @throws StepException if processing must stop on this sheet
     */
    private void checkResolution ()
        throws StepException
    {
        final int interline = comboPeak.main;

        if (interline < constants.minInterline.getValue()) {
            final String msg = String.format(
                    "%s\n\n With a too low interline value of %d pixels,\n"
                            + " either this sheet contains no multi-line staves,\n"
                            + " or the picture resolution is too low (try 300 DPI).\n\n"
                            + " This interline value is NOT RELIABLE!",
                    sheet.getId(),
                    interline);
            sheet.getStub().decideOnRemoval(msg, false);
        } else if (interline > constants.maxInterline.getValue()) {
            final String msg = String.format(
                    "%s\n\n With a too high interline value of %d pixels,\n"
                            + " this sheet does not seem to contain multi-line staves.\n\n"
                            + " This interline value is NOT RELIABLE!",
                    sheet.getId(),
                    interline);
            sheet.getStub().decideOnRemoval(msg, false);
        }
    }

    //-----------------//
    // computeBeamKeys //
    //-----------------//
    /**
     * Compute the beam scale information for the sheet, assigning values to
     * <code>beamKey</code> and <code>beamKey2</code> if possible,
     * or <code>beamGuess</code> if nothing else works.
     * <p>
     * We try to retrieve beam keys in black histogram, otherwise we extrapolate a probable beam
     * height based on minimum and maximum height values.
     *
     * @param verbose true for printing out computation details
     */
    private void computeBeamKeys (boolean verbose)
    {
        // Specific beam thickness from Book/Sheet parameters?
        final Integer specifiedBeam = sheet.getStub().getBeamSpecification();
        if ((specifiedBeam != null) && (specifiedBeam != 0)) {
            beamKey = specifiedBeam;

            if (verbose) {
                logger.info("User-specified beam height: {} pixels", beamKey);
            }

            return;
        }

        // Significant beam peak detected?
        if (beamKey != null) {
            if (verbose) {
                logger.info("Significant beam peak detected: {} pixels", beamKey);
            }

            return;
        }

        // Beam guess
        final int largerInterline = interlineKey != null ? interlineKey
                : (comboPeak2 == null) ? comboPeak.main : Math.max(comboPeak.main, comboPeak2.main);
        final int minHeight = Math.max(
                blackPeak.max,
                (int) Math.rint(constants.beamMinFraction.getValue() * largerInterline));
        final int maxHeight = Math.max(
                largerInterline - blackPeak.main,
                (int) Math.rint(constants.beamMaxFraction.getValue() * largerInterline));
        final double beamRatio = constants.beamRangeRatio.getValue();
        beamGuess = (int) Math.rint(maxHeight * beamRatio);

        if (verbose) {
            logger.info(
                    String.format(
                            "Beam  guessed height: %2d -- %.2f of %d interline",
                            beamGuess,
                            beamRatio,
                            maxHeight));
        }

        // Beam measurement
        final int quorum = histoKeeper.getBeamQuorum();

        // NOTA: At this point in time, blackFinder.findPeaks() has already been used for staff line.
        // Setting quorum for blackFinder here is just for the potential plot
        histoKeeper.blackFinder.setQuorum(new Quorum(quorum, minHeight, maxHeight));

        final List<Integer> peaks = histoKeeper.blackFunction.getLocalMaxima(minHeight, maxHeight);

        if (!peaks.isEmpty()) {
            final int peak = peaks.get(0);
            final double quorumRatio = (double) histoKeeper.blackFunction.getValue(peak) / quorum;
            final double rangeRatio = (double) (peak - minHeight) / (maxHeight - minHeight);

            if (verbose) {
                logger.info(
                        String.format(
                                "Beam measured height: %2d -- %.2f"
                                        + " of [%d..%d] range at %d%% of needed quorum",
                                peak,
                                rangeRatio,
                                minHeight,
                                maxHeight,
                                (int) Math.rint(quorumRatio * 100)));
            }

            // Quorum reached or measured value close to guess?
            final int diff = Math.abs(peak - beamGuess);

            if ((quorumRatio >= 1.0) || (diff <= constants.beamMaxDiff.getValue())) {
                beamKey = peak;
            }

            if ((comboPeak2 != null) && (peaks.size() > 1)) {
                final int peak2 = peaks.get(1);
                final double qRatio = (double) histoKeeper.blackFunction.getValue(peak2) / quorum;

                if (qRatio >= 1.0) {
                    beamKey2 = peak2;
                }
            }
        }

        if ((beamKey == null) && verbose) {
            logger.warn("No reliable beam height found, guessed value: {}", beamGuess);
        }
    }

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
                doRetrieveScale(true); // Dummy retrieval
            } catch (StepException ignored) {
            }
        }

        if (histoKeeper != null) {
            histoKeeper.writePlots();
        }
    }

    //-----------------//
    // doRetrieveScale //
    //-----------------//
    /**
     * Retrieve the global scale counts by processing the provided picture runs,
     * make decisions about the validity of current picture as a music page and store
     * the results as a {@link Scale} instance in the related sheet.
     *
     * @param dummy true for dummy retrieval (just for the chart)
     * @return scale data for the sheet
     * @throws StepException if processing must stop for this sheet.
     */
    private Scale doRetrieveScale (boolean dummy)
        throws StepException
    {
        // Specific interline from Book/Sheet specification?
        final Integer specifiedInterline = sheet.getStub().getInterlineSpecification();
        if ((specifiedInterline != null) && (specifiedInterline != 0)) {
            interlineKey = specifiedInterline;
            logger.info("User-specified interline: {} pixels", interlineKey);
        }

        final Scale scl = sheet.getScale();

        if (dummy || scl == null) {
            binary = sheet.getPicture().getVerticalTable(Picture.TableKey.BINARY);
            histoKeeper = new HistoKeeper();

            histoKeeper.buildBlacks();
            histoKeeper.retrieveLinePeak(); // -> blackPeak (or StepException), beamKey?

            histoKeeper.buildCombos();
            histoKeeper.retrieveInterlinePeaks(); // -> comboPeak (or StepException), comboPeak2?

            if (dummy) {
                computeBeamKeys(false); // Just for the chart

                return null;
            }
        }

        // Respect user-assigned scale info, if any
        if (scl != null) {
            logger.info("Sheet scale data already set.");
            return scl;
        }

        final LineScale lineScale = new LineScale(blackPeak);
        final InterlineScale interlineScale;
        InterlineScale smallInterlineScale = null;

        if (interlineKey != null) {
            interlineScale = new InterlineScale(interlineKey, interlineKey, interlineKey);
        } else {
            // Check we have acceptable resolution.
            checkResolution(); // StepException may be thrown here

            interlineScale = getInterlineScale();
            smallInterlineScale = getSmallInterlineScale();
        }

        BeamScale beamScale = null;
        BeamScale smallBeamScale = null;
        computeBeamKeys(true); // -> beamGuess, beamKey, beamKey2

        if (beamKey2 != null) {
            beamScale = new BeamScale(Math.max(beamKey, beamKey2), false);
            smallBeamScale = new BeamScale(Math.min(beamKey, beamKey2), false);
        } else if (beamKey != null) {
            beamScale = new BeamScale(beamKey, false);
        } else if (beamGuess != null) {
            beamScale = new BeamScale(beamGuess, true);
        }

        return new Scale(interlineScale, lineScale, beamScale, smallInterlineScale, smallBeamScale);
    }

    //-------------------//
    // getInterlineScale //
    //-------------------//
    private InterlineScale getInterlineScale ()
    {
        if ((comboPeak2 == null) || (comboPeak2.main < comboPeak.main)) {
            return new InterlineScale(comboPeak);
        } else {
            return new InterlineScale(comboPeak2);
        }
    }

    //------------------------//
    // getSmallInterlineScale //
    //------------------------//
    private InterlineScale getSmallInterlineScale ()
    {
        if (comboPeak2 == null) {
            return null;
        }

        final Range peak = (comboPeak2.main < comboPeak.main) ? comboPeak2 : comboPeak;

        return new InterlineScale(peak);
    }

    //---------------//
    // retrieveScale //
    //---------------//
    /**
     * Retrieve scale information on sheet.
     *
     * @return scale information
     * @throws StepException raised if some mandatory information cannot be retrieved.
     *                       For example if no staves are found.
     */
    public Scale retrieveScale ()
        throws StepException
    {
        return doRetrieveScale(false);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------------//
    // getMaxInterline //
    //-----------------//
    /**
     * Report the maximum possible interline value.
     *
     * @return maximum interline possible (in pixels)
     */
    public static int getMaxInterline ()
    {
        return constants.maxInterline.getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer minInterline = new Constant.Integer(
                "Pixels",
                11,
                "Minimum interline value (in pixels)");

        private final Constant.Integer maxInterline = new Constant.Integer(
                "Pixels",
                100,
                "Maximum interline value (in pixels)");

        private final Constant.Ratio minGainRatio = new Constant.Ratio(
                0.03,
                "Minimum ratio of peak runs for peak extension");

        private final Constant.Ratio minDerivativeRatio = new Constant.Ratio(
                0.025,
                "Minimum ratio of total runs for derivative acceptance");

        private final Constant.Ratio minBlackRatio = new Constant.Ratio(
                0.001,
                "Minimum ratio of foreground pixels in image");

        private final Constant.Ratio maxSecondRatio = new Constant.Ratio(
                1.9, // 2.0 led to a false second peak in merged grand staff
                "Maximum ratio between second and first combined peak");

        private final Constant.Ratio beamMinFraction = new Constant.Ratio(
                0.275,
                "Minimum ratio between beam height and interline");

        private final Constant.Ratio beamMaxFraction = new Constant.Ratio(
                1.0,
                "Maximum ratio between beam height and interline");

        private final Constant.Ratio beamMinCountRatio = new Constant.Ratio(
                0.01,
                "Minimum ratio of runs for beam height measurement (quorum)");

        private final Constant.Ratio beamRangeRatio = new Constant.Ratio(
                0.5,
                "Ratio of beam possible height range for guess");

        private final Constant.Integer beamMaxDiff = new Constant.Integer(
                "pixels",
                2,
                "Maximum difference between beam guess and measurement");

        private final Constant.Ratio maxWhiteHeightRatio = new Constant.Ratio(
                0.25,
                "Maximum white length, as ratio of image height");

        private final Constant.Ratio maxBlackHeightRatio = new Constant.Ratio(
                0.08,
                "Maximum black length, as ratio of image height");

        private final Constant.Ratio minCountRatio = new Constant.Ratio(
                0.5,
                "Minimum significant count, as ratio of highest count");
    }

    //-------------//
    // HistoKeeper //
    //-------------//
    /**
     * This class handles the histograms of vertical lengths for black runs (foreground)
     * and combo runs (black + white and white + black).
     * It retrieves the various peaks and is able to display a chart on the related populations.
     */
    private class HistoKeeper
    {
        // Upper bounds for run lengths (assuming sheet height >= staff height)
        final int maxBlack;

        final int maxWhite;

        final IntegerFunction blackFunction;

        final HiLoPeakFinder blackFinder;

        final IntegerFunction comboFunction;

        final HiLoPeakFinder comboFinder;

        HistoKeeper ()
        {
            // We assume at least one staff in sheet, hence some maximum values for relevant white
            // and black runs.
            final int imgHeight = binary.getHeight();
            maxBlack = (int) Math.rint(imgHeight * constants.maxBlackHeightRatio.getValue());
            maxWhite = (int) Math.rint(imgHeight * constants.maxWhiteHeightRatio.getValue());
            logger.debug(
                    "maxBlack:{}, maxWhite:{}, maxCombo:{}",
                    maxBlack,
                    maxWhite,
                    maxBlack + maxWhite);

            // Allocate histograms
            blackFunction = new IntegerFunction(0, maxBlack);
            blackFinder = new HiLoPeakFinder("black", blackFunction);

            comboFunction = new IntegerFunction(0, maxBlack + maxWhite);
            comboFinder = new HiLoPeakFinder("combo", comboFunction);
        }

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
                        blackFunction.addValue(black, 1);
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                blackFunction.print(System.out);
            }
        }

        //-------------//
        // buildCombos //
        //-------------//
        /**
         * Populate the combo histogram.
         * <p>
         * A combo is the total length of blackRun + next whiteRun or whiteRun + next blackRun,
         * provided that blackRun length and whiteRun length are in relevant ranges.
         * <p>
         * NOTA: Roughly, all counts are doubled by the way we count the combos.
         * This has no real impact, because integral value (which is used as the base for quorum)
         * and derivative values are also doubled.
         */
        public void buildCombos ()
        {
            for (int x = 0, width = binary.getWidth(); x < width; x++) {
                int yLast = 0; // Ordinate of first pixel not yet processed
                int lastBlack = 0; // Length of last valid black run

                for (Iterator<Run> it = binary.iterator(x); it.hasNext();) {
                    final Run run = it.next();
                    final int y = run.getStart();
                    final int black = run.getLength();

                    if ((black < blackPeak.min) || (black > blackPeak.max)) {
                        lastBlack = 0;
                    } else {
                        if (y > yLast) {
                            // Process the white run before this black run
                            final int white = y - yLast;

                            // A white run between valid black runs?: B1, W, B2
                            // Combo 1 is defined as B1 + W, that is [-----]
                            // Combo 2 is defined as W + B2, that is     [-----]
                            // combo1 + combo2 = 2 * (1/2 * B1 + W + 1/2 * B2) = 2 * combo
                            if ((white <= maxWhite) && (lastBlack != 0)) {
                                comboFunction.addValue(lastBlack + white, 1); // B1 + W
                                comboFunction.addValue(white + black, 1); // W + B2
                            }
                        }

                        lastBlack = black;
                    }

                    yLast = y + black;
                }
            }

            if (logger.isDebugEnabled()) {
                comboFunction.print(System.out);
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
            final int blackCount = getBlackCount();
            final int size = binary.getWidth() * binary.getHeight();
            final double blackRatio = (double) blackCount / size;
            logger.debug("blackRatio: {}", blackRatio);

            if (blackRatio < constants.minBlackRatio.getValue()) {
                sheet.getStub().decideOnRemoval("""
                        %s

                        Too few black pixels: %.2f%% of whole image.
                        This sheet is almost blank.
                        """.formatted(sheet.getId(), 100 * blackRatio), false);
            }
        }

        //---------------//
        // getBeamQuorum //
        //---------------//
        /**
         * Report the quorum needed to validate beam height measurement.
         *
         * @return beam quorum, specified in cumulated runs count
         */
        public int getBeamQuorum ()
        {
            // Quorum on height histo
            final int totalArea = blackFunction.getArea();
            final double ratio = constants.beamMinCountRatio.getValue();

            return (int) Math.rint(totalArea * ratio);
        }

        //---------------//
        // getBlackCount //
        //---------------//
        /**
         * Compute the total number of black pixels.
         * <p>
         * We use the fact that in 'blackHisto', x represents a black run length and y the number
         * of occurrences of this run length in the image.
         *
         * @return the count of black pixels
         */
        private int getBlackCount ()
        {
            final int xMin = blackFunction.getXMin();
            int total = 0;

            for (int i = xMin; i <= blackFunction.getXMax(); i++) {
                total += ((i - xMin) * blackFunction.getValue(i));
            }

            return total;
        }

        //------------------------//
        // retrieveInterlinePeaks //
        //------------------------//
        /**
         * Retrieve combo peak for interline (and interline2 if any).
         * <p>
         * FYI: If comboPeak and comboPeak2 get modified, they will appear as such in scale info,
         * but the chart will still display the (original) peaks found.
         */
        public void retrieveInterlinePeaks ()
            throws StepException
        {
            // Combo peak(s)
            final int area = comboFunction.getArea();
            List<Range> comboPeaks = comboFinder.findPeaks(
                    1,
                    (int) Math.rint(area * constants.minDerivativeRatio.getValue()),
                    constants.minGainRatio.getValue());

            if (interlineKey != null) {
                return;
            }

            if (comboPeaks.isEmpty()) {
                sheet.getStub().invalidate();
                throw new StepException("No regularly spaced lines found");
            }

            // Primary combo peak
            comboPeak = comboPeaks.get(0);
            logger.debug("comboPeak: {}", comboPeak);
            comboPeaks = comboPeaks.subList(1, comboPeaks.size());

            // Filter other peaks with respect to the primary one
            // They cannot be too far or too close
            for (Iterator<Range> it = comboPeaks.iterator(); it.hasNext();) {
                final Range p = it.next();
                final int min = Math.min(p.main, comboPeak.main);
                final int max = Math.max(p.main, comboPeak.main);

                if (((double) max / min) > constants.maxSecondRatio.getValue()) {
                    logger.debug("Other combo peak too different {}, discarded", p);
                    it.remove();
                } else if (Math.abs(p.main - comboPeak.main) < blackPeak.main) {
                    logger.debug("Merging two close combo peaks {} & {}", comboPeak, p);
                    comboPeak = new Range(min, (p.main + comboPeak.main) / 2, max);
                    it.remove();
                }
            }

            // Secondary combo peak?
            if (!comboPeaks.isEmpty()) {
                comboPeak2 = comboPeaks.get(0);
            }
        }

        //------------------//
        // retrieveLinePeak //
        //------------------//
        /**
         * Retrieve the black peak corresponding to line thickness.
         * <p>
         * What if several significant black peaks were found?
         * <ul>
         * <li>If the image contains multi-line staves, which is the general case, the contribution
         * from staff lines vastly outperforms the contribution from beams.
         * Therefore, we take the peak with the highest count.
         * <li>But in the rare case where the image contains only 1-line staves, we may have two
         * significant peaks, one from staff lines and one from beams.
         * In this case, since beams are thicker than lines, we take the peak with the lowest key.
         * </ul>
         * <p>
         * So, either we rely on user-declared switches regarding the staff lines,
         * or, preferably, we check for the existence of two significant black peaks.
         */
        public void retrieveLinePeak ()
            throws StepException
        {
            // Check we have enough foreground material. If not, throw StepException
            checkBlack();

            // Black peaks
            final int area = blackFunction.getArea();
            final List<Range> blackPeaks = blackFinder.findPeaks(
                    1,
                    (int) Math.rint(area * constants.minDerivativeRatio.getValue()),
                    constants.minGainRatio.getValue());

            if (blackPeaks.isEmpty()) {
                sheet.getStub().invalidate();
                throw new StepException("No significant black lines found");
            }

            // Check for two significant black peaks
            if (blackPeaks.size() > 1) {
                // Keep only the peaks with count >= ratio of highest peak
                final int bestCount = blackFunction.getValue(blackPeaks.get(0).main);
                final int minCount = (int) Math.rint(
                        bestCount * constants.minCountRatio.getValue());

                for (int i = 1; i < blackPeaks.size(); i++) {
                    final Range peak = blackPeaks.get(i);

                    if (blackFunction.getValue(peak.main) < minCount) {
                        blackPeaks.retainAll(blackPeaks.subList(0, i));
                    }
                }
            }

            if (blackPeaks.size() > 1) {
                final Range p0 = blackPeaks.get(0);
                final Range p1 = blackPeaks.get(1);
                blackPeak = (p1.main < p0.main) ? p1 : p0;
                beamKey = (p1.main < p0.main) ? p0.main : p1.main;
            } else {
                blackPeak = blackPeaks.get(0);
            }

            logger.debug("blackPeak: {} beamKey: {}", blackPeak, beamKey);
        }

        //------------//
        // writePlots //
        //------------//
        /**
         * Write a plot for black values (staff line, beam thickness) and
         * a plot for combo values (staff interline).
         */
        public void writePlots ()
        {
            // Determine suitable x upper values for combos
            int comboMax = blackPeak != null ? blackPeak.max : 30;

            if (comboPeak != null) {
                comboMax = Math.max(comboMax, comboPeak.max);
            }

            if (comboPeak2 != null) {
                comboMax = Math.max(comboMax, comboPeak2.max);
            }

            final Scale scale = sheet.getScale();

            try {
                // Black values (staff line, beam thickness)
                final String title = sheet.getId() + " " + blackFinder.name;
                final String xLabel = "Runs lengths - " + ((scale != null) ? scale.toString(
                        Info.BLACK) : "NO_SCALE");
                final String yLabel = "Runs counts - total:" + blackFunction.getArea()
                        + " - Beam quorum:" + getBeamQuorum();
                final ChartPlotter plotter = new ChartPlotter(title, xLabel, yLabel);
                blackFinder.plot(plotter, true, 0, maxBlack).display(new Point(20, 20));
            } catch (Throwable ex) {
                logger.warn("Error in plotting black", ex);
            }

            try {
                // Combo values (staff interline)
                final String title = sheet.getId() + " " + comboFinder.name;
                final String xLabel = "Runs lengths - " + ((scale != null) ? scale.toString(
                        Info.COMBO) : "NO_SCALE");
                final String yLabel = "Runs counts";
                final ChartPlotter plotter = new ChartPlotter(title, xLabel, yLabel);
                comboMax = Math.min((comboMax * 3) / 2, sheet.getWidth() - 1); // Add some margin
                comboFinder.plot(plotter, true, 0, comboMax).display(new Point(80, 80));
            } catch (Throwable ex) {
                logger.warn("Error in plotting combo", ex);
            }
        }
    }
}
