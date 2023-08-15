//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S t e m S c a l e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sheet.stem;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.image.ImageFormatException;
import org.audiveris.omr.image.ImageUtil;
import org.audiveris.omr.math.HiLoPeakFinder;
import org.audiveris.omr.math.HiLoPeakFinder.Quorum;
import org.audiveris.omr.math.IntegerFunction;
import org.audiveris.omr.math.Range;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.PageCleaner;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.StemScale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.util.ChartPlotter;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.process.ByteProcessor;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>StemScaler</code> retrieves the typical thickness of stems in a sheet.
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

    /** Related sheet. */
    private final Sheet sheet;

    /** Keeper of horizontal run length histogram. */
    private HistoKeeper histoKeeper;

    /** Most frequent length of horizontal foreground runs found, if any. */
    private Range peak;

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

    //-------------//
    // computeStem //
    //-------------//
    /**
     * Determine stem thickness (main and max values) from histogram of run lengths,
     * or use extrapolated values if computation fails.
     *
     * @return stem scale information
     */
    private StemScale computeStem ()
    {
        final int area = histoKeeper.function.getArea();
        histoKeeper.peakFinder.setQuorum(
                new Quorum((int) Math.rint(area * constants.minValueRatio.getValue())));

        final List<Range> stemPeaks = histoKeeper.peakFinder.findPeaks(
                1,
                (int) Math.rint(area * constants.minDerivativeRatio.getValue()),
                constants.minGainRatio.getValue());

        if (!stemPeaks.isEmpty()) {
            peak = stemPeaks.get(0);
        }

        final int mainStem;
        final int maxStem;

        if (peak != null) {
            mainStem = (int) Math.rint(peak.main);
            maxStem = (int) Math.rint(peak.max);
        } else {
            final Scale scale = sheet.getScale();
            final double ratio = constants.stemAsForeRatio.getValue();
            mainStem = (int) Math.rint(ratio * scale.getFore());
            maxStem = (int) Math.rint(ratio * scale.getMaxFore());
            logger.info("No stem peak found, computing defaults");
        }

        return new StemScale(mainStem, maxStem);
    }

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Display the stem scale histogram.
     */
    public void displayChart ()
    {
        final StemScale stemScale = retrieveStemWidth();

        if (histoKeeper != null) {
            histoKeeper.writePlot(stemScale);
        } else {
            logger.info("No stem data available yet");
        }
    }

    //-----------//
    // getBuffer //
    //-----------//
    /**
     * Prepare the sheet buffer to focus on stems only.
     * <p>
     * We need to focus strictly on staff area, to avoid any "false stem information" that might
     * come from text lines (such as lyric lines).
     * And within staff area, we even have to hide pixels that belong to barlines and barline
     * connectors.
     *
     * @return the buffer where the most frequent horizontal run lengths most likely belong to
     *         stems.
     */
    private ByteProcessor getBuffer ()
    {
        // Build a mask focused on sheet staff areas
        final BufferedImage mask = new BufferedImage(
                sheet.getWidth(),
                sheet.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = mask.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(Color.WHITE);

        final StaffManager mgr = sheet.getStaffManager();

        for (Staff staff : mgr.getStaves()) {
            g.fill(mgr.getCoreStaffPath(staff));
        }

        // Obtain image without staff lines and barlines
        final ByteProcessor buf = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);
        final BufferedImage img = buf.getBufferedImage();
        final StemsCleaner eraser = new StemsCleaner(buf, img.createGraphics(), sheet);
        eraser.eraseShapes(
                Arrays.asList(
                        Shape.THICK_BARLINE,
                        Shape.THICK_CONNECTOR,
                        Shape.THIN_BARLINE,
                        Shape.THIN_CONNECTOR));
        buf.threshold(127); // Binarize

        // Then draw image composite on top of mask
        g.setComposite(AlphaComposite.SrcAtop);
        g.drawImage(img, 0, 0, null);

        try {
            // We need a TYPE_BYTE_GRAY image for ByteProcessor
            BufferedImage image = Picture.adjustImageFormat(mask);
            ByteProcessor buffer = new ByteProcessor(image);

            // Keep a copy on disk?
            if (constants.saveStemImage.isSet()) {
                ImageUtil.saveOnDisk(image, sheet.getId(), "stems");
            }

            return buffer;
        } catch (ImageFormatException ex) {
            logger.warn("{}", ex);

            return null;
        }
    }

    //-------------------//
    // retrieveStemWidth //
    //-------------------//
    /**
     * Retrieve the global stem thickness for the sheet.
     *
     * @return the stem scale data
     */
    public StemScale retrieveStemWidth ()
    {
        StopWatch watch = new StopWatch("Stem scaler for " + sheet.getId());

        try {
            // Use a buffer focused on staff areas with barlines and connectors removed
            watch.start("getBuffer");

            ByteProcessor buffer = getBuffer();

            // Look at histogram for stem thickness
            watch.start("stem retrieval");

            RunTableFactory runFactory = new RunTableFactory(Orientation.HORIZONTAL);
            RunTable horiTable = runFactory.createTable(buffer);
            histoKeeper = new HistoKeeper(horiTable, sheet.getScale().getInterline());

            return computeStem();
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print the StopWatch on stem computation?");

        private final Constant.Boolean saveStemImage = new Constant.Boolean(
                false,
                "Should we save stem images on disk?");

        private final Constant.Boolean useHeader = new Constant.Boolean(
                true,
                "Should we erase the header at system start");

        private final Scale.Fraction systemVerticalMargin = new Scale.Fraction(
                2.0,
                "Margin erased above & below system header area");

        private final Constant.Ratio minValueRatio = new Constant.Ratio(
                0.1,
                "Absolute ratio of total pixels for peak acceptance");

        private final Constant.Ratio minDerivativeRatio = new Constant.Ratio(
                0.05,
                "Absolute ratio of total pixels for strong derivative");

        private final Constant.Ratio minGainRatio = new Constant.Ratio(
                0.1,
                "Minimum ratio of peak runs for stem peak extension");

        private final Constant.Ratio stemAsForeRatio = new Constant.Ratio(
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

        private final IntegerFunction function;

        private final HiLoPeakFinder peakFinder;

        /**
         * Create an instance of histoKeeper.
         *
         * @param horiTable horizontal run table
         * @param maxLength an upper bound for horizontal run length values
         */
        HistoKeeper (RunTable horiTable,
                     int maxLength)
        {
            function = new IntegerFunction(0, maxLength);
            populateFunction(horiTable);
            peakFinder = new HiLoPeakFinder("stem", function);
        }

        private void populateFunction (RunTable horiTable)
        {
            final int height = horiTable.getHeight();
            final int maxLength = function.getXMax();

            for (int y = 0; y < height; y++) {
                for (Iterator<Run> it = horiTable.iterator(y); it.hasNext();) {
                    final int blackLength = it.next().getLength();

                    if (blackLength <= maxLength) {
                        function.addValue(blackLength, 1);
                    }
                }
            }
        }

        public void writePlot (StemScale stemScale)
        {
            final String title = sheet.getId() + " " + peakFinder.name;
            ChartPlotter plotter = new ChartPlotter(
                    title,
                    "Stem thickness - Scale " + stemScale,
                    "Counts");
            peakFinder.plot(plotter, true);
            plotter.display(new Point(80, 80));
        }
    }

    //--------------//
    // StemsCleaner //
    //--------------//
    private static class StemsCleaner
            extends PageCleaner
    {

        StemsCleaner (ByteProcessor buffer,
                      Graphics2D g,
                      Sheet sheet)
        {
            super(buffer, g, sheet);
        }

        /**
         * Erase from image graphics all instances of provided shapes.
         *
         * @param shapes (input) the shapes to look for
         */
        public void eraseShapes (Collection<Shape> shapes)
        {
            for (SystemInfo system : sheet.getSystems()) {
                final SIGraph sig = system.getSig();
                final List<Inter> erased = new ArrayList<>();

                for (Inter inter : sig.vertexSet()) {
                    if (!inter.isRemoved() && shapes.contains(inter.getShape())) {
                        if (canHide(inter)) {
                            erased.add(inter);
                        }
                    }
                }

                // Erase the inters
                for (Inter inter : erased) {
                    inter.accept(this);
                }

                // Erase system header?
                if (constants.useHeader.isSet()) {
                    eraseSystemHeader(system, constants.systemVerticalMargin);
                }
            }
        }
    }
}
