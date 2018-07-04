//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S h e e t D i f f                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import ij.process.ByteProcessor;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.image.GlobalFilter;
import org.audiveris.omr.image.ImageUtil;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.ui.ImageView;
import org.audiveris.omr.sheet.ui.PixelBoard;
import org.audiveris.omr.sheet.ui.SheetResultPainter;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.util.ByteUtil;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.WeakPropertyChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Class {@code SheetDiff} measures the difference between input data (the input sheet
 * picture) and output data (the recognized entities).
 *
 * @author Hervé Bitteur
 */
public class SheetDiff
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SheetDiff.class);

    //~ Enumerations -------------------------------------------------------------------------------
    public static enum DiffKind
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /**
         * Non recognized entities.
         * Input data not found in output. */
        NEGATIVES,
        /**
         * Recognized entities.
         * Intersection of input and output. */
        POSITIVES,
        /**
         * False recognized entities.
         * Output data not found in input. */
        FALSE_POSITIVES;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Image of output entities. */
    private BufferedImage output;

    /** Cached number of foreground pixels in input image. */
    private Integer inputCount;

    //~ Constructors -------------------------------------------------------------------------------
    public SheetDiff (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // computeDiff //
    //-------------//
    /**
     * Computes the figure which best describes the level of
     * recognition reached on current sheet.
     *
     * @return the ratio of (filtered) different pixels with respect to the
     *         total foreground pixels of the input binary image.
     */
    public double computeDiff ()
    {
        final StopWatch watch = new StopWatch("computeDiff");
        final int width = sheet.getWidth();
        final int height = sheet.getHeight();
        final ByteProcessor in = new GlobalFilter(
                sheet.getPicture().getSource(Picture.SourceKey.BINARY),
                constants.binaryThreshold.getValue()).filteredImage();

        watch.start("count input");
        inputCount = getInputCount();

        watch.start("output");

        ByteProcessor out = new ByteProcessor(getOutput());
        out.threshold(constants.binaryThreshold.getValue());

        // Compute input XOR output
        watch.start("xor");

        final ByteProcessor xor = new ByteProcessor(width, height);
        ByteUtil.raz(xor); // xor.invert();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (in.get(x, y) != out.get(x, y)) {
                    xor.set(x, y, 0);
                }
            }
        }

        // Filter the data
        watch.start("median filter");

        xor.medianFilter();

        watch.start("filtered to disk");
        ImageUtil.saveOnDisk(xor.getBufferedImage(), sheet.getId() + ".filtered");
        watch.start("count filtered");

        // Count all filtered differences
        final int count = getForeCount(xor);
        final double ratio = (double) count / inputCount;

        logger.info(
                "Delta {}% ({} differences wrt {} input pixels)",
                String.format("%4.1f", 100 * ratio),
                count,
                inputCount);

        if (constants.printWatch.isSet()) {
            watch.print();
        }

        // Display the filtered differences
        if (OMR.gui != null) {
            sheet.getStub().getAssembly().addViewTab(
                    SheetTab.DIFF_TAB,
                    new ScrollView(new MyView(xor)),
                    new BoardsPane(new PixelBoard(sheet)));
        }

        return ratio;
    }

    //---------------//
    // computeRatios //
    //---------------//
    /**
     * Compute ratios related to negative, positive and false positive
     * pixels computed between input and output images.
     */
    public void computeRatios ()
    {
        final int total = sheet.getWidth() * sheet.getHeight();
        StopWatch watch = new StopWatch("computeRatios");

        watch.start("input");

        final ByteProcessor source = sheet.getPicture().getSource(Picture.SourceKey.BINARY);
        final BufferedImage input = source.getBufferedImage();

        watch.start("inputCount");
        getInputCount();
        logger.info(
                "INPUT count: {} ratio: {}% (out of {} image pixels)",
                inputCount,
                String.format("%.1f", (100d * inputCount) / total),
                total);

        watch.start("output");
        output = getOutput();

        watch.start("xor");

        BufferedImage xor = ImageUtil.invert(ImageUtil.xor(input, output));

        ///ImageUtil.saveOnDisk(xor, sheet.getPage().getId() + ".XOR");
        for (DiffKind kind : DiffKind.values()) {
            watch.start(kind.toString());

            int count = getCount(kind);
            logger.info(
                    "{}% ({} wrt {} input pixels)",
                    String.format("%15s ratio: %4.1f", kind, (100d * count) / inputCount),
                    count,
                    inputCount);
        }

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //----------//
    // getCount //
    //----------//
    /**
     * Report the count of pixels in the desired kind.
     *
     * @param kind the desired kind
     * @return the number of counted pixels
     */
    public int getCount (DiffKind kind)
    {
        BufferedImage img = getImage(kind);

        ImageUtil.saveOnDisk(img, sheet.getId() + "." + kind);

        final ByteProcessor source = new ByteProcessor(img);
        source.threshold(constants.binaryThreshold.getValue());

        final int count = getForeCount(source);

        return count;
    }

    //----------//
    // getImage //
    //----------//
    /**
     * Report the image of desired diff kind.
     *
     * @param kind the desired kind
     * @return the corresponding (gray level) image
     */
    public BufferedImage getImage (DiffKind kind)
    {
        final Color veryLight = new Color(222, 222, 200);
        final RunTable input = sheet.getPicture().getTable(Picture.TableKey.BINARY);
        final Point offset = new Point(0, 0);
        final BufferedImage img = new BufferedImage(
                sheet.getWidth(),
                sheet.getHeight(),
                BufferedImage.TYPE_INT_ARGB_PRE);
        final Graphics2D gbi = img.createGraphics();

        // Anti-aliasing
        gbi.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        gbi.setComposite(AlphaComposite.SrcOver);
        gbi.setColor(Color.BLACK);

        SheetResultPainter outputPainter = new SheetResultPainter(sheet, gbi, false, true, false);

        switch (kind) {
        case NEGATIVES:
            input.render(gbi, offset);
            gbi.setComposite(AlphaComposite.DstOut);
            outputPainter.process();

            break;

        case POSITIVES:
            gbi.setColor(veryLight); // Could be totally white...
            input.render(gbi, offset);
            gbi.setComposite(AlphaComposite.SrcIn);
            gbi.setColor(Color.BLACK);
            outputPainter.process();

            break;

        case FALSE_POSITIVES:
            outputPainter.process();
            gbi.setComposite(AlphaComposite.DstOut);
            input.render(gbi, offset);

            break;

        default:
            assert false : "Unhandled DeltaKind";
        }

        //        // Paint all the other pixels in white
        //        gbi.setComposite(AlphaComposite.SrcOut);
        //        gbi.setColor(Color.WHITE);
        //        gbi.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        //
        // No longer needed
        gbi.dispose();

        return img;
    }

    //--------------//
    // getForeCount //
    //--------------//
    /**
     * Count the number of foreground pixels in the provided image.
     *
     * @param filter the binary image
     * @return the number of foreground pixels
     */
    private int getForeCount (ByteProcessor filter)
    {
        final int width = filter.getWidth();
        final int height = filter.getHeight();
        int count = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (filter.get(x, y) == 0) {
                    count++;
                }
            }
        }

        return count;
    }

    //---------------//
    // getInputCount //
    //---------------//
    /**
     * Report the number of foreground pixels in the input image.
     *
     * @return count of foreground pixels in sheet input binary image
     */
    private int getInputCount ()
    {
        if (inputCount == null) {
            ByteProcessor source = sheet.getPicture().getSource(Picture.SourceKey.BINARY);
            inputCount = getForeCount(source);
        }

        return inputCount;
    }

    //-----------//
    // getOutput //
    //-----------//
    /**
     * Report the BufferedImage painted with recognized entities.
     *
     * @return the output image
     */
    private BufferedImage getOutput ()
    {
        if (output == null) {
            output = new BufferedImage(
                    sheet.getWidth(),
                    sheet.getHeight(),
                    BufferedImage.TYPE_BYTE_GRAY);

            Graphics2D gbi = output.createGraphics();
            gbi.setColor(Color.WHITE);
            gbi.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            gbi.setColor(Color.BLACK);

            //
            //            PagePhysicalPainter painter = new PagePhysicalPainter(gbi, false, true, false);
            //
            //            for (Page page : sheet.getPages()) {
            //                page.accept(painter);
            //            }
            new SheetResultPainter(sheet, gbi, false, true, false).process();

            gbi.dispose();

            ///ImageUtil.saveOnDisk(output, sheet.getPage().getId() + ".OUTPUT");
        }

        return output;
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
                "Should we print out the stop watch?");

        private final Constant.Integer binaryThreshold = new Constant.Integer(
                "gray level",
                127,
                "Global threshold to binarize delta results");
    }

    //--------//
    // MyView //
    //--------//
    private class MyView
            extends ImageView
            implements PropertyChangeListener
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyView (ByteProcessor filtered)
        {
            super(filtered.getBufferedImage());
            setModelSize(new Dimension(sheet.getWidth(), sheet.getHeight()));

            // Inject dependency of pixel location
            setLocationService(sheet.getLocationService());

            // Listen to all view parameters
            ViewParameters.getInstance()
                    .addPropertyChangeListener(new WeakPropertyChangeListener(this));
        }

        //~ Methods --------------------------------------------------------------------------------
        //----------------//
        // propertyChange //
        //----------------//
        @Override
        public void propertyChange (PropertyChangeEvent evt)
        {
            repaint();
        }
    }
}
