//----------------------------------------------------------------------------//
//                                                                            //
//                              S h e e t D i f f                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.image.BufferedSource;
import omr.image.GlobalFilter;
import omr.image.ImageUtil;
import omr.image.MedianGrayFilter;
import omr.image.PixelBuffer;
import omr.image.PixelFilter;
import omr.image.PixelSource;

import omr.run.RunsTable;

import omr.score.entity.Page;
import omr.score.ui.PagePhysicalPainter;
import omr.score.ui.PaintingParameters;

import omr.sheet.ui.PixelBoard;

import omr.step.Step;

import omr.ui.BoardsPane;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import omr.util.StopWatch;
import omr.util.WeakPropertyChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Class {@code SheetDiff} measures the difference between input
 * data (the input sheet picture) and output data (the recognized
 * entities).
 *
 * @author Hervé Bitteur
 */
public class SheetDiff
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SheetDiff.class);

    //~ Enumerations -----------------------------------------------------------
    public static enum DiffKind
    {
        //~ Enumeration constant initializers ----------------------------------

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

    //~ Instance fields --------------------------------------------------------
    /** The related sheet. */
    private final Sheet sheet;

    /** Image of output entities. */
    private BufferedImage output;

    /** Cached number of foreground pixels in input image. */
    private Integer inputCount;

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // SheetDiff //
    //-----------//
    public SheetDiff (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------
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
        final PixelFilter in = new GlobalFilter(
                sheet.getPicture().getSource(Picture.SourceKey.BINARY),
                constants.binaryThreshold.getValue());

        watch.start("count input");
        inputCount = getInputCount();

        watch.start("output");

        final PixelFilter out = new GlobalFilter(
                new BufferedSource(getOutput()),
                constants.binaryThreshold.getValue());

        // Compute input XOR output
        watch.start("xor");

        final PixelBuffer xor = new PixelBuffer(new Dimension(width, height));

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (in.isFore(x, y) != out.isFore(x, y)) {
                    xor.setValue(x, y, 0);
                }
            }
        }

        // Filter the data
        watch.start("median filter");

        final PixelBuffer filtered = new PixelBuffer(
                new Dimension(width, height));
        new MedianGrayFilter(1).filter(xor, filtered);

        //        watch.start("filtered to disk");
        //        ImageUtil.saveOnDisk(filtered.toBufferedImage(), sheet.getPage().getId() + ".filtered");
        watch.start("count filtered");

        // Count all filtered differences
        final int count = getForeCount(filtered);
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
        if (Main.getGui() != null) {
            sheet.getAssembly()
                    .addViewTab(
                            Step.DIFF_TAB,
                            new ScrollView(new MyView(filtered)),
                            new BoardsPane(new PixelBoard(sheet)));
        }

        sheet.getBench()
                .recordDelta(ratio);

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

        final PixelSource source = sheet.getPicture()
                .getSource(Picture.SourceKey.BINARY);
        final BufferedImage input = ((PixelBuffer) source).toBufferedImage();

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
                    String.format(
                    "%15s ratio: %4.1f",
                    kind,
                    (100d * count) / inputCount),
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

        ///ImageUtil.saveOnDisk(img, sheet.getPage().getId() + "." + kind);
        final BufferedSource source = new BufferedSource(img);
        final PixelFilter filter = new GlobalFilter(
                source,
                constants.binaryThreshold.getValue());

        final int count = getForeCount(filter);

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
        final RunsTable input = sheet.getWholeVerticalTable();
        final Page page = sheet.getPage();
        final BufferedImage img = new BufferedImage(
                sheet.getWidth(),
                sheet.getHeight(),
                BufferedImage.TYPE_INT_ARGB_PRE);
        final Graphics2D gbi = img.createGraphics();

        // Anti-aliasing
        gbi.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        gbi.setComposite(AlphaComposite.SrcOver);
        gbi.setColor(Color.BLACK);

        final PagePhysicalPainter outputPainter = new PagePhysicalPainter(
                gbi,
                false,
                true,
                false);

        switch (kind) {
        case NEGATIVES:
            input.render(gbi);
            gbi.setComposite(AlphaComposite.DstOut);
            page.accept(outputPainter);

            break;

        case POSITIVES:
            gbi.setColor(veryLight); // Could be totally white...
            input.render(gbi);
            gbi.setComposite(AlphaComposite.SrcIn);
            gbi.setColor(Color.BLACK);
            page.accept(outputPainter);

            break;

        case FALSE_POSITIVES:
            page.accept(outputPainter);
            gbi.setComposite(AlphaComposite.DstOut);
            input.render(gbi);

            break;

        default:
            assert false : "Unhandled DeltaKind";
        }

        //        // Paint all the other pixels in white
        //        gbi.setComposite(AlphaComposite.SrcOut);
        //        gbi.setColor(Color.WHITE);
        //        gbi.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
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
    private int getForeCount (PixelFilter filter)
    {
        final int width = filter.getWidth();
        final int height = filter.getHeight();
        int count = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (filter.isFore(x, y)) {
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
            PixelSource source = sheet.getPicture()
                    .getSource(Picture.SourceKey.BINARY);
            PixelFilter filter = (PixelFilter) source;
            inputCount = getForeCount(filter);
        }

        return inputCount;
    }

    //-----------//
    // getOutput //
    //-----------//
    /**
     * Report the BufferedImage painted with recongized entities.
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
            sheet.getPage()
                    .accept(new PagePhysicalPainter(gbi, false, true, false));
            gbi.dispose();

            ///ImageUtil.saveOnDisk(output, sheet.getPage().getId() + ".OUTPUT");
        }

        return output;
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
                "Should we print out the stop watch?");

        Constant.Integer binaryThreshold = new Constant.Integer(
                "gray level",
                127,
                "Global threshold to binarize delta results");

    }

    //--------//
    // MyView //
    //--------//
    private class MyView
            extends RubberPanel
            implements PropertyChangeListener
    {
        //~ Instance fields ----------------------------------------------------

        private final BufferedImage img;

        //~ Constructors -------------------------------------------------------
        public MyView (PixelBuffer filtered)
        {
            setModelSize(sheet.getDimension());

            // Inject dependency of pixel location
            setLocationService(sheet.getLocationService());

            // Listen to all painting parameters
            PaintingParameters.getInstance()
                    .addPropertyChangeListener(
                            new WeakPropertyChangeListener(this));

            img = filtered.toBufferedImage();
        }

        //~ Methods ------------------------------------------------------------
        //----------------//
        // propertyChange //
        //----------------//
        @Override
        public void propertyChange (PropertyChangeEvent evt)
        {
            repaint();
        }

        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics2D g)
        {
            g.drawImage(img, null, 0, 0);
        }
    }
}
