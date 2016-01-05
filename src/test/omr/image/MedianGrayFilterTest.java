//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             M e d i a n G r a y F i l t e r T e s t                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Class {@code MedianGrayFilterTest}
 *
 * @author Hervé Bitteur
 */
public class MedianGrayFilterTest
{
    //~ Instance fields ----------------------------------------------------------------------------

    final int width = 10;

    final int height = 4;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new MedianGrayFilterTest object.
     */
    public MedianGrayFilterTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //    @Test
    public void testProcess_black_1 ()
    {
        System.out.println("\nprocess_black_1");

        BufferedImage image = createBlackImage();
        dump(image, "initial black:");

        MedianGrayFilter instance = new MedianGrayFilter(1);
        BufferedImage expResult = image;
        BufferedImage result = instance.filter(image);
        dump(result, "result:");

        if (!areEqual(expResult, result)) {
            fail("Images are not equal");
        }
    }

    //    @Test
    public void testProcess_diag_1 ()
    {
        System.out.println("\nprocess_diag_1");

        BufferedImage image = createDiagImage();
        dump(image, "initial diag:");

        MedianGrayFilter instance = new MedianGrayFilter(1);
        BufferedImage expResult = image;
        BufferedImage result = instance.filter(image);
        dump(result, "result:");

        if (!areEqual(expResult, result)) {
            fail("Images are not equal");
        }
    }

    @Test
    public void testProcess_ex_1 ()
    {
        System.out.println("\nprocess_ex_1");

        final int[] pixel = new int[1];
        pixel[0] = 0;

        BufferedImage image = createWhiteImage(8, 8);
        final WritableRaster out = image.getRaster();

        out.setPixel(2, 5, pixel);
        out.setPixel(3, 2, pixel);
        out.setPixel(3, 3, pixel);
        out.setPixel(3, 4, pixel);
        out.setPixel(4, 2, pixel);
        out.setPixel(4, 3, pixel);

        dump(image, "initial ex:");

        MedianGrayFilter instance = new MedianGrayFilter(1);
        BufferedImage expResult = createWhiteImage(8, 8);
        final WritableRaster exp = expResult.getRaster();
        exp.setPixel(3, 3, pixel);
        exp.setPixel(4, 3, pixel);
        dump(expResult, "Expected:");

        BufferedImage result = instance.filter(image);
        dump(result, "result:");

        if (!areEqual(expResult, result)) {
            fail("Images are not equal");
        }
    }

    //    @Test
    public void testProcess_white_1 ()
    {
        System.out.println("\nprocess_white_1");

        BufferedImage image = createWhiteImage(width, height);
        dump(image, "initial white:");

        MedianGrayFilter instance = new MedianGrayFilter(1);
        BufferedImage expResult = image;
        BufferedImage result = instance.filter(image);
        dump(result, "result:");

        if (!areEqual(expResult, result)) {
            fail("Images are not equal");
        }
    }

    private boolean areEqual (BufferedImage one,
                              BufferedImage two)
    {
        final Raster oneRaster = one.getRaster();
        final Raster twoRaster = two.getRaster();
        final int width = one.getWidth();
        final int height = one.getHeight();
        final int[] onePixel = new int[1];
        final int[] twoPixel = new int[1];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                oneRaster.getPixel(x, y, onePixel);
                twoRaster.getPixel(x, y, twoPixel);

                if (onePixel[0] != twoPixel[0]) {
                    return false;
                }
            }
        }

        return true;
    }

    private BufferedImage createBlackImage ()
    {
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        final WritableRaster out = img.getRaster();
        final int[] pixel = new int[1];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel[0] = 0;
                out.setPixel(x, y, pixel);
            }
        }

        return img;
    }

    private BufferedImage createDiagImage ()
    {
        final BufferedImage img = createWhiteImage(width, height);

        final WritableRaster out = img.getRaster();
        final int[] pixel = new int[1];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == y) {
                    pixel[0] = 0;
                    out.setPixel(x, y, pixel);
                }
            }
        }

        return img;
    }

    private BufferedImage createWhiteImage (int width,
                                            int height)
    {
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        final WritableRaster out = img.getRaster();
        final int[] pixel = new int[1];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel[0] = 255;
                out.setPixel(x, y, pixel);
            }
        }

        return img;
    }

    private void dump (BufferedImage img,
                       String title)
    {
        final WritableRaster raster = img.getRaster();
        final int width = img.getWidth();
        final int height = img.getHeight();
        final int[] pixel = new int[1];

        System.out.println(title);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.getPixel(x, y, pixel);
                System.out.printf("%4d", pixel[0]);
            }

            System.out.println();
        }
    }
}
