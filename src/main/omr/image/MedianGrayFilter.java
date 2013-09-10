//----------------------------------------------------------------------------//
//                                                                            //
//                            M e d i a n F i l t e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

/**
 * Class {@code MedianGrayFilter} allows to run a median filter on an
 * input image, assumed to contain only gray values [0..255].
 *
 * @author Hervé Bitteur
 */
public class MedianGrayFilter
{
    //~ Instance fields --------------------------------------------------------

    /** Desired radius for the filter. */
    private final int radius;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new MedianGrayFilter object.
     *
     * @param radius desired radius for the filter (1 for 3x3 filter, 2 for 5x5,
     *               etc)
     */
    public MedianGrayFilter (int radius)
    {
        this.radius = radius;
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // filter //
    //--------//
    /**
     * Apply this filter on a given input image.
     *
     * @param image the input image, assumed of TYPE_BYTE_GRAY
     * @return the filtered image
     */
    public BufferedImage filter (final BufferedImage image)
    {
        if (!(image instanceof RenderedImage)) {
            throw new IllegalArgumentException(
                    "Input image is not a RenderedImage");
        }

        if (image.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            throw new IllegalArgumentException(
                    "Input image is not of type TYPE_BYTE_GRAY");
        }

        final int width = image.getWidth();
        final int height = image.getHeight();
        final BufferedImage filtered = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_BYTE_GRAY);
        final Raster in = image.getRaster();
        final WritableRaster out = filtered.getRaster();
        final int[] inPixel = new int[1];
        final int[] outPixel = new int[1];
        final int[] histogram = new int[256];
        Arrays.fill(histogram, 0);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // To address specific behavior at image boundaries, 
                // reduce radius to not use pixels outside the image.
                int rad = radius;

                if ((x - rad) < 0) {
                    rad = x;
                }

                if ((y - rad) < 0) {
                    rad = y;
                }

                if ((x + rad) >= width) {
                    rad = width - 1 - x;
                }

                if ((y + rad) >= height) {
                    rad = height - 1 - y;
                }

                Arrays.fill(histogram, 0); // Brute force!

                for (int i = x - rad; i <= (x + rad); i++) {
                    for (int j = y - rad; j <= (y + rad); j++) {
                        in.getPixel(i, j, inPixel);

                        int val = inPixel[0];
                        histogram[val]++;
                    }
                }

                // Pick up the median value
                final int side = (2 * rad) + 1;
                final int medianCount = ((side * side) + 1) / 2;
                int median = 255;
                int sum = 0;

                while (sum < medianCount) {
                    sum += histogram[median];
                    median--;
                }

                //
                //                // Reset histogram
                //                for (int i = x - rad; i <= (x + rad); i++) {
                //                    for (int j = y - rad; j <= (y + rad); j++) {
                //                        in.getPixel(i, j, inPixel);
                //                        histogram[inPixel[0]] = 0;
                //                    }
                //                }
                outPixel[0] = median + 1;
                out.setPixel(x, y, outPixel);
            }
        }

        return filtered;
    }

//    //-----------//
//    // getMedian //
//    //-----------//
//    /**
//     * Compute the median value for pixel centered at provided (x,y)
//     * location, using the values of all pixels covered by the filter.
//     *
//     * @param x  pixel abscissa, counted from 0
//     * @param y  pixel ordinate, counted from 0
//     * @param in image raster to pick pixels values from
//     */
//    private int getMedian (final int x,
//                           final int y,
//                           final Raster in)
//    {
//        // To address specific behavior at image boundaries, reduce radius
//        // in order to not use pixels outside the image.
//        int rad = radius;
//
//        if ((x - rad) < 0) {
//            rad = x;
//        }
//
//        if ((y - rad) < 0) {
//            rad = y;
//        }
//
//        if ((x + rad) >= in.getWidth()) {
//            rad = in.getWidth() - 1 - x;
//        }
//
//        if ((y + rad) >= in.getHeight()) {
//            rad = in.getHeight() - 1 - y;
//        }
//
//        /** Half of filter pixels count. */
//        int side = (2 * rad) + 1;
//        final int medianCount = ((side * side) + 1) / 2;
//
//        int[] iArray = new int[1];
//        int[] histogram = new int[256];
//        Arrays.fill(histogram, 0);
//
//        for (int i = x - rad; i <= (x + rad); i++) {
//            for (int j = y - rad; j <= (y + rad); j++) {
//                in.getPixel(i, j, iArray);
//
//                int val = iArray[0];
//                histogram[val]++;
//            }
//        }
//
//        // Pick up the median value
//        int index = 255;
//        int sum = 0;
//
//        while (sum < medianCount) {
//            sum += histogram[index];
//            index--;
//        }
//
//        int median = index + 1;
//
//        // Reset histogram
//        for (int i = x - rad; i <= (x + rad); i++) {
//            for (int j = y - rad; j <= (y + rad); j++) {
//                in.getPixel(i, j, iArray);
//
//                int val = iArray[0];
//                histogram[val] = 0;
//            }
//        }
//
//        return median;
//    }
}
