//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               G a u s s i a n G r a y F i l t e r                              //
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
package org.audiveris.omr.image;

import ij.process.ByteProcessor;

import java.awt.image.Kernel;

/**
 * Class <code>GaussianGrayFilter</code> allows to run a Gaussian filter on an input image,
 * assumed to contain only gray values [0..255].
 * <p>
 * This implementation is derived from Jerry Huxtable more general filter but limited to
 * BufferedImage class.
 *
 * @author Hervé Bitteur
 */
public class GaussianGrayFilter
        extends AbstractGrayFilter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Radius of the kernel. */
    private final float radius;

    /** The kernel to apply. */
    private final Kernel kernel;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new GaussianGrayFilter object with a default radius value.
     */
    public GaussianGrayFilter ()
    {
        this(2);
    }

    /**
     * Creates a new GaussianGrayFilter object with a specified radius.
     *
     * @param radius kernel radius in pixels
     */
    public GaussianGrayFilter (float radius)
    {
        this.radius = radius;
        kernel = makeKernel(radius);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------------//
    // convolveAndTranspose //
    //----------------------//
    private void convolveAndTranspose (byte[] inPixels,
                                       byte[] outPixels,
                                       int width,
                                       int height)
    {
        float[] matrix = kernel.getKernelData(null);
        int cols = kernel.getWidth();
        int cols2 = cols / 2;

        for (int y = 0; y < height; y++) {
            int index = y;
            int ioffset = y * width;

            for (int x = 0; x < width; x++) {
                float p = 0;
                int moffset = cols2;

                for (int col = -cols2; col <= cols2; col++) {
                    float f = matrix[moffset + col];

                    if (f != 0) {
                        int ix = x + col;

                        if (ix < 0) {
                            ix = 0;
                        } else if (ix >= width) {
                            ix = width - 1;
                        }

                        int pix = inPixels[ioffset + ix] & 0xff;
                        p += (f * pix);
                    }
                }

                int ip = clamp((int) (p + 0.5));
                outPixels[index] = (byte) ip;
                index += height;
            }
        }
    }

    //--------//
    // filter //
    //--------//
    @Override
    public void filter (ByteProcessor input,
                        ByteProcessor output)
    {
        final int width = input.getWidth();
        final int height = input.getHeight();
        final byte[] inPixels = new byte[width * height];
        final byte[] outPixels = new byte[width * height];

        // Read pixels
        for (int y = 0; y < height; y++) {
            final int offset = y * width;

            for (int x = 0; x < width; x++) {
                inPixels[offset + x] = (byte) input.get(x, y);
            }
        }

        convolveAndTranspose(inPixels, outPixels, width, height);
        convolveAndTranspose(outPixels, inPixels, height, width);

        // Write pixels
        for (int y = 0; y < height; y++) {
            final int offset = y * width;

            for (int x = 0; x < width; x++) {
                output.set(x, y, inPixels[offset + x] & 0xff);
            }
        }
    }

    //-----------//
    // getRadius //
    //-----------//
    /**
     * Get the radius of the kernel.
     *
     * @return the kernel radius
     */
    public float getRadius ()
    {
        return radius;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------//
    // clamp //
    //-------//
    /**
     * Clamp a value to the range 0..255.
     *
     * @param val the input value
     * @return the value constrained in [0..255]
     */
    private static int clamp (int val)
    {
        if (val < 0) {
            return 0;
        }

        if (val > 255) {
            return 255;
        }

        return val;
    }

    //------------//
    // makeKernel //
    //------------//
    /**
     * Make a Gaussian blur kernel.
     *
     * @param radius desired kernel radius specified in pixels around center
     * @return the Gaussian kernel of desired radius
     */
    public static Kernel makeKernel (float radius)
    {
        final int r = (int) Math.ceil(radius);
        final int rows = (r * 2) + 1;
        final float[] matrix = new float[rows];
        final float sigma = 1f; //HB: was radius / 3;
        final float sigmaSq2 = 2 * sigma * sigma;
        final float radiusSq = radius * radius;

        float total = 0;
        int index = 0;

        for (int row = -r; row <= r; row++) {
            float distanceSq = row * row;

            if (distanceSq > radiusSq) {
                matrix[index] = 0;
            } else {
                matrix[index] = (float) Math.exp(-distanceSq / sigmaSq2);
            }

            total += matrix[index];
            index++;
        }

        // Normalize all matrix items
        for (int i = 0; i < rows; i++) {
            matrix[i] /= total;
        }

        return new Kernel(rows, 1, matrix);
    }
}
