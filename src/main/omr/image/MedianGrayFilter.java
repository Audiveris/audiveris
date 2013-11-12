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

import java.util.Arrays;

/**
 * Class {@code MedianGrayFilter} allows to run a median filter on an
 * input image, assumed to contain only gray values [0..255].
 *
 * @author Hervé Bitteur
 */
public class MedianGrayFilter
        extends AbstractGrayFilter
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
    @Override
    public void filter (final PixelSource input,
                        final PixelSink output)
    {
        final int width = input.getWidth();
        final int height = input.getHeight();
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
                        int val = input.getPixel(i, j);
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

                output.setPixel(x, y, median + 1);
            }
        }
    }
}
