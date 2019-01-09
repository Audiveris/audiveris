//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               B a s i c A R T E x t r a c t o r                                //
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
package org.audiveris.omr.moments;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import static org.audiveris.omr.moments.ARTMoments.*;
import org.audiveris.omr.util.StopWatch;

import java.awt.image.WritableRaster;

/**
 * Class {@code BasicARTExtractor} implements extraction of ART Moments.
 * <p>
 * See MPEG-7 Experimentation Model for the original C++ code
 *
 * @author Hervé Bitteur
 */
public class BasicARTExtractor
        extends AbstractExtractor<ARTMoments>
{

    private static final Constants constants = new Constants();

    // Zernike basis function radius
    private static final int LUT_RADIUS = 50;

    /** Real values of ARTMoments basis function */
    private static final LUT[][] realLuts = new LUT[ANGULAR][RADIAL];

    /** Imaginary values of ARTMoments basis function */
    private static final LUT[][] imagLuts = new LUT[ANGULAR][RADIAL];

    static {
        initLUT();
    }

    /**
     * Creates a new BasicARTExtractor object and process the provided foreground points.
     */
    public BasicARTExtractor ()
    {
    }

    @Override
    public void reconstruct (WritableRaster raster)
    {
        ///throw new UnsupportedOperationException("Not supported yet.");
    }

    //----------------//
    // extractMoments //
    //----------------//
    @Override
    protected void extractMoments ()
    {
        final LUT anyLut = realLuts[0][0]; // Just for LUT dimensions
        final int lutRadius = anyLut.getRadius();
        final double centerX = center.getX();
        final double centerY = center.getY();

        // Coefficients, real part & imaginary part
        final double[][] coeffReal = new double[ANGULAR][RADIAL];
        final double[][] coeffImag = new double[ANGULAR][RADIAL];

        for (int i = 0; i < mass; i++) {
            // Map image coordinates to LUT coordinates
            double x = xx[i] - centerX;
            double y = yy[i] - centerY;
            double lx = ((x * lutRadius) / radius) + lutRadius;
            double ly = ((y * lutRadius) / radius) + lutRadius;

            // Summation of basis function
            if (anyLut.contains(lx, ly)) {
                for (int p = 0; p < ANGULAR; p++) {
                    for (int r = 0; r < RADIAL; r++) {
                        coeffReal[p][r] += realLuts[p][r].interpolate(lx, ly);
                        coeffImag[p][r] -= imagLuts[p][r].interpolate(lx, ly);
                    }
                }
            }
        }

        // Save to descriptor
        for (int p = 0; p < ANGULAR; p++) {
            for (int r = 0; r < RADIAL; r++) {
                double real = coeffReal[p][r] / mass;
                double imag = coeffImag[p][r] / mass;
                descriptor.setMoment(p, r, Math.hypot(imag, real));

                //                descriptor.setArgument(p, r, Math.atan2(imag, real));
                //                descriptor.setReal(p, r, real);
                //                descriptor.setImag(p, r, imag);
            }
        }
    }

    //---------//
    // initLUT //
    //---------//
    /**
     * Compute, once for all, the lookup table values.
     */
    private static void initLUT ()
    {
        StopWatch watch = new StopWatch("LUT");
        watch.start("initLUT");

        // Allocate LUT's
        for (int p = 0; p < ANGULAR; p++) {
            for (int r = 0; r < RADIAL; r++) {
                realLuts[p][r] = new BasicLUT(LUT_RADIUS);
                imagLuts[p][r] = new BasicLUT(LUT_RADIUS);
            }
        }

        final LUT anyLut = realLuts[0][0]; // Just for template
        final int lutSize = anyLut.getSize();
        final int lutRadius = anyLut.getRadius();

        for (int x = 0; x < lutSize; x++) {
            double tx = (x - lutRadius) / (double) lutRadius; // [-1..+1]

            for (int y = 0; y < lutSize; y++) {
                double ty = (y - lutRadius) / (double) lutRadius; // [-1..+1]
                double rad = Math.hypot(tx, ty); // [0..sqrt(2)]

                if (rad < 1) {
                    // We are within unit circle
                    double angle = Math.atan2(ty, tx);

                    for (int p = 0; p < ANGULAR; p++) {
                        for (int r = 0; r < RADIAL; r++) {
                            double temp = Math.cos(rad * Math.PI * r);
                            realLuts[p][r].assign(x, y, temp * Math.cos(angle * p));
                            imagLuts[p][r].assign(x, y, temp * Math.sin(angle * p));
                        }
                    }
                } else {
                    // We are on or outside unit circle, so let's set value to 0
                    for (int p = 0; p < ANGULAR; p++) {
                        for (int r = 0; r < RADIAL; r++) {
                            realLuts[p][r].assign(x, y, 0);
                            imagLuts[p][r].assign(x, y, 0);
                        }
                    }
                }
            }
        }

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");
    }
}
