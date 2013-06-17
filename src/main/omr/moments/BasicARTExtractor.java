//----------------------------------------------------------------------------//
//                                                                            //
//                     B a s i c A R T E x t r a c t o r                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.moments;

import static omr.moments.ARTMoments.*;

import java.awt.image.WritableRaster;

/**
 * Class {@code BasicARTExtractor} implements extraction
 * of ART Moments.
 *
 * See MPEG-7 Experimentation Model for the original C++ code
 *
 * @author Hervé Bitteur
 */
public class BasicARTExtractor
        extends AbstractExtractor<ARTMoments>
{
    //~ Static fields/initializers ---------------------------------------------

    // Zernike basis function radius
    private static final int LUT_RADIUS = 50;

    /** Real values of ARTMoments basis function */
    private static final LUT[][] realLuts = new LUT[ANGULAR][RADIAL];

    /** Imaginary values of ARTMoments basis function */
    private static final LUT[][] imagLuts = new LUT[ANGULAR][RADIAL];

    static {
        initLUT();
    }

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BasicARTExtractor object and process
     * the provided foreground points.
     */
    public BasicARTExtractor ()
    {
    }

    //~ Methods ----------------------------------------------------------------
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
        final LUT anyLut = realLuts[0][0]; // Just for template
        final int lutRadius = anyLut.getRadius();
        final double centerX = center.getX();
        final double centerY = center.getY();

        // Coefficients, real part & imaginary part
        final double[][] coeffReal = new double[ANGULAR][RADIAL];
        final double[][] coeffImag = new double[ANGULAR][RADIAL];

        for (int i = 0; i < mass; i++) {
            // Map image coordinate to LUT coordinates
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
                    // We are within circle
                    double angle = Math.atan2(ty, tx);

                    for (int p = 0; p < ANGULAR; p++) {
                        for (int r = 0; r < RADIAL; r++) {
                            double temp = Math.cos(rad * Math.PI * r);
                            realLuts[p][r].assign(
                                    x,
                                    y,
                                    temp * Math.cos(angle * p));
                            imagLuts[p][r].assign(
                                    x,
                                    y,
                                    temp * Math.sin(angle * p));
                        }
                    }
                } else {
                    // We are on or outside circle
                    for (int p = 0; p < ANGULAR; p++) {
                        for (int r = 0; r < RADIAL; r++) {
                            realLuts[p][r].assign(x, y, 0);
                            imagLuts[p][r].assign(x, y, 0);
                        }
                    }
                }
            }
        }
    }
}
