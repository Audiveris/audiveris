//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      M e d i a n F i l t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.image;

import ij.process.ByteProcessor;

import java.util.Arrays;

/**
 * Class {@code MedianGrayFilter} allows to run a median filter on an input image,
 * assumed to contain only gray values [0..255].
 *
 * @author Hervé Bitteur
 */
public class MedianGrayFilter
        extends AbstractGrayFilter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Desired radius for the filter. */
    private final int radius;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new MedianGrayFilter object.
     *
     * @param radius desired radius for the filter (1 for 3x3 filter, 2 for 5x5, etc)
     */
    public MedianGrayFilter (int radius)
    {
        this.radius = radius;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // filter //
    //--------//
    @Override
    public void filter (final ByteProcessor input,
                        final ByteProcessor output)
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
                        int val = input.get(i, j);
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

                output.set(x, y, median + 1);
            }
        }
    }
}
