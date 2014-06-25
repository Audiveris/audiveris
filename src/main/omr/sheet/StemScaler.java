//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S t e m S c a l e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.image.ImageUtil;

import omr.math.Histogram.PeakEntry;
import omr.math.IntegerHistogram;

import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.sig.Inter;
import omr.sig.SIGraph;

import omr.util.StopWatch;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code StemScaler} retrieves the typical thickness of stems in a sheet.
 * <p>
 * Computation is based on histogram of lengths of horizontal runs.
 * For precise results, we have to "remove" the detected bar lines (and connections) otherwise their
 * horizontal runs might impact stem thickness measurement.
 *
 * @author Hervé Bitteur
 */
public class StemScaler
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StemScaler.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Related sheet. */
    private final Sheet sheet;

    /** Keeper of horizontal run length histogram. */
    private HistoKeeper histoKeeper;

    /** Histogram on horizontal foreground runs. */
    private IntegerHistogram histo;

    /** Most frequent length of horizontal foreground runs found, if any. */
    private PeakEntry<Double> peak;

    /** Absolute population percentage for validating an extremum. */
    private final double quorumRatio = constants.quorumRatio.getValue();

    /** Relative population percentage for reading spread. */
    private final double spreadRatio = constants.spreadRatio.getValue();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new StemScaler object.
     *
     * @param sheet the sheet to be processed
     */
    public StemScaler (Sheet sheet)
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
        if (histoKeeper != null) {
            histoKeeper.writePlot();
        } else {
            logger.info("No stem data available yet");
        }
    }

    //--------------//
    // retrieveStem //
    //--------------//
    /**
     * Retrieve the global stem thickness for the sheet.
     */
    public void retrieveStem ()
    {
        StopWatch watch = new StopWatch("Stem scaler for " + sheet.getPage().getId());

        try {
            // Use a buffer with bar lines and connections removed
            watch.start("getBuffer");

            Picture picture = sheet.getPicture();
            ByteProcessor buffer = getBuffer();

            // Look at histogram for stem thickness
            watch.start("stem retrieval");

            RunsTableFactory runFactory = new RunsTableFactory(Orientation.HORIZONTAL, buffer, 0);
            RunsTable horiTable = runFactory.createTable("horiRuns");
            histoKeeper = new HistoKeeper(picture.getWidth() - 1);
            histoKeeper.buildHistograms(horiTable, picture.getHeight());
            computeStem();
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //-------------//
    // computeStem //
    //-------------//
    private void computeStem ()
    {
        peak = histo.getPeak(quorumRatio, spreadRatio, 0);
        final int mainStem;
        final int maxStem;

        if (peak != null) {
            mainStem = (int) Math.rint(peak.getKey().best);
            maxStem = (int) Math.rint(peak.getKey().second);
        } else {
            Scale scale = sheet.getScale();
            double ratio = constants.stemAsForeRatio.getValue();
            mainStem = (int) Math.rint(ratio * scale.getMainFore());
            maxStem = (int) Math.rint(ratio * scale.getMaxFore());
            logger.info("{}No stem peak found, computing defaults", sheet.getLogPrefix());
        }

        sheet.setMainStem((int) Math.rint(peak.getKey().best));
        sheet.setMaxStem((int) Math.rint(peak.getKey().second));
        logger.info("{}Stem main: {}, max: {}", sheet.getLogPrefix(), mainStem, maxStem);
    }

    //-----------//
    // getBuffer //
    //-----------//
    private ByteProcessor getBuffer ()
    {
        Picture picture = sheet.getPicture();
        ByteProcessor buf = picture.getSource(Picture.SourceKey.STAFF_LINE_FREE);
        BufferedImage img = buf.getBufferedImage();
        StemsEraser eraser = new StemsEraser(buf, img.createGraphics(), sheet);
        eraser.eraseShapes(
                Arrays.asList(
                        Shape.THICK_BARLINE,
                        Shape.THICK_CONNECTION,
                        Shape.THIN_BARLINE,
                        Shape.THIN_CONNECTION));
        buf = new ByteProcessor(img);
        buf.threshold(127);

        // Keep a copy on disk?
        if (constants.keepStemImage.isSet()) {
            ImageUtil.saveOnDisk(img, sheet.getPage().getId() + ".stem");
        }

        return buf;
    }

    //--------------//
    // retrievePeak //
    //--------------//
    private void retrievePeak ()
    {
        peak = histo.getPeak(quorumRatio, spreadRatio, 0);

        //        List<Histogram.PeakEntry<Integer>> horiMaxima = histo.getPreciseMaxima();
        //
        //        if (!horiMaxima.isEmpty()) {
        //            Histogram.MaxEntry<Integer> max = horiMaxima.get(0);
        //
        //            if (max.getValue() >= constants.quorumRatio.getValue()) {
        //                stemPeak = max;
        //                logger.debug("stem: {}", stemPeak);
        //            }
        //        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print the StopWatch on stem computation?");

        final Constant.Boolean keepStemImage = new Constant.Boolean(
                false,
                "Should we store stem images on disk?");

        final Constant.Boolean useDmz = new Constant.Boolean(
                true,
                "Should we erase the DMZ at system start");

        final Scale.Fraction systemVerticalMargin = new Scale.Fraction(
                2.0,
                "Margin erased above & below system DMZ area");

        final Constant.Ratio quorumRatio = new Constant.Ratio(
                0.1,
                "Absolute ratio of total pixels for peak acceptance");

        final Constant.Ratio spreadRatio = new Constant.Ratio(
                0.30,
                "Relative ratio of best count for stem spread reading");

        final Constant.Ratio stemAsForeRatio = new Constant.Ratio(
                1.0,
                "Default stem thickness defined as ratio of foreground peak");
    }

    //-------------//
    // HistoKeeper //
    //-------------//
    /**
     * Handles the histogram of horizontal foreground runs.
     */
    private class HistoKeeper
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final int[] fore; // (black) foreground runs

        // We are not interested of horizontal runs longer than this value
        private final int maxFore = 20;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create an instance of histoKeeper.
         *
         * @param wMax the maximum possible horizontal run length value
         */
        public HistoKeeper (int wMax)
        {
            // Allocate histogram counters
            fore = new int[wMax + 2];

            // Useful?
            Arrays.fill(fore, 0);
        }

        //~ Methods --------------------------------------------------------------------------------
        //
        //-----------//
        // writePlot //
        //-----------//
        public void writePlot ()
        {
            new HistogramPlotter(sheet, "horizontal black", fore, histo, peak, null, maxFore).plot(
                    new Point(80, 80),
                    "Lengths for stem",
                    spreadRatio,
                    quorumRatio);
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
            histo = createHistogram(fore);
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

    //-------------//
    // StemsEraser //
    //-------------//
    private class StemsEraser
            extends PageEraser
    {
        //~ Constructors ---------------------------------------------------------------------------

        public StemsEraser (ByteProcessor buffer,
                           Graphics2D g,
                           Sheet sheet)
        {
            super(buffer, g, sheet);
        }

        //~ Methods --------------------------------------------------------------------------------
        //-------------//
        // eraseShapes //
        //-------------//
        /**
         * Erase from image graphics all instances of provided shapes.
         *
         * @param shapes (input) the shapes to look for
         */
        public void eraseShapes (Collection<Shape> shapes)
        {
            for (SystemInfo system : sheet.getSystems()) {
                final SIGraph sig = system.getSig();
                final List<Inter> erased = new ArrayList<Inter>();

                for (Inter inter : sig.vertexSet()) {
                    if (!inter.isDeleted() && shapes.contains(inter.getShape())) {
                        if (canHide(inter)) {
                            erased.add(inter);
                        }
                    }
                }

                // Erase the inters
                for (Inter inter : erased) {
                    inter.accept(this);
                }

                // Erase system DMZ?
                if (constants.useDmz.isSet()) {
                    eraseSystemDmz(system, constants.systemVerticalMargin);
                }
            }
        }

        @Override
        protected boolean canHide (Inter inter)
        {
            return inter.isGood();
        }
    }
}
