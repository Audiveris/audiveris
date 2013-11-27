//----------------------------------------------------------------------------//
//                                                                            //
//                             S h e e t D e l t a                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.image.BufferedSource;
import omr.image.GlobalFilter;
import omr.image.ImageUtil;
import omr.image.PixelBuffer;
import omr.image.PixelFilter;
import omr.image.PixelSource;

import omr.run.RunsTable;

import omr.score.entity.Page;
import omr.score.ui.PagePhysicalPainter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Class {@code SheetDelta} measures the differences between input
 * data (the input sheet picture) and output data (the recognized
 * entities) to come up with negative, positive and false positives
 * results.
 *
 * @author Hervé Bitteur
 */
public class SheetDelta
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SheetDelta.class);

    //~ Enumerations -----------------------------------------------------------
    public static enum DeltaKind
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

    /** Cached number of foreground pixels in input image. */
    private Integer inputCount;

    //~ Constructors -----------------------------------------------------------
    public SheetDelta (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // computeRatios //
    //---------------//
    public void computeRatios ()
    {
        final int total = sheet.getWidth() * sheet.getHeight();
        getInputCount();
        logger.info(
                "INPUT count: {} ratio: {}% (out of {} image pixels)",
                inputCount,
                String.format("%.1f", (100d * inputCount) / total),
                total);

        for (DeltaKind kind : DeltaKind.values()) {
            int count = getCount(kind);
            logger.info(
                    "{}% ({} wrt {} input pixels)",
                    String.format("%15s ratio: %4.1f", kind, (100d * count) / inputCount),
                    count,
                    inputCount);
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
    public int getCount (DeltaKind kind)
    {
        BufferedImage img = getImage(kind);
        ImageUtil.saveOnDisk(img, sheet.getPage().getId() + "." + kind);

        final BufferedSource source = new BufferedSource(img);
        final PixelFilter filter = new GlobalFilter(
                source,
                constants.binaryThreshold.getValue());

        //        {
        //            // DEBUG START
        //            Point[] points = new Point[]{
        //                new Point(100, 100), new Point(241, 363),
        //                new Point(408, 390), new Point(439, 1189)
        //            };
        //
        //            for (Point p : points) {
        //                logger.info(
        //                        "{} {} pixel:{} fore:{}",
        //                        kind,
        //                        p,
        //                        source.getPixel(p.x, p.y),
        //                        filter.isFore(p.x, p.y));
        //            }
        //
        //            // DEBUG STOP
        //        }
        //
        final int count = getForeCount(filter);

        return count;
    }

    //----------//
    // getImage //
    //----------//
    /**
     * Report the image of desired delta kind.
     *
     * @param kind the desired kind
     * @return the corresponding (gray level) image
     */
    public BufferedImage getImage (DeltaKind kind)
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
    private int getInputCount ()
    {
        if (inputCount == null) {
            PixelSource source = sheet.getPicture()
                    .getSource(Picture.SourceKey.BINARY);

            // Store buffer on disk for further manual analysis
            BufferedImage bi = ((PixelBuffer) source).toBufferedImage();
            ImageUtil.saveOnDisk(bi, sheet.getPage().getId() + ".INPUT");

            PixelFilter filter = (PixelFilter) source;
            inputCount = getForeCount(filter);
        }

        return inputCount;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer binaryThreshold = new Constant.Integer(
                "gray level",
                127,
                "Global threshold to binarize delta results");

    }
}
