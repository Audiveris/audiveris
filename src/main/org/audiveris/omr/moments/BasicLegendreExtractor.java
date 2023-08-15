//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                          B a s i c L e g e n d r e E x t r a c t o r                           //
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
package org.audiveris.omr.moments;

import org.audiveris.omr.math.Polynomial;
import static org.audiveris.omr.moments.LegendreMoments.ORDER;

import java.awt.image.WritableRaster;

/**
 * Class <code>BasicLegendreExtractor</code> implements extraction of Legendre moments.
 *
 * @author Hervé Bitteur
 */
public class BasicLegendreExtractor
        extends AbstractExtractor<LegendreMoments>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Legendre polynomials. */
    private static final Polynomial[] P = generatePolynomials();

    /** LUT's for Legendre basis function values. */
    private static final LUT[][] luts;

    static {
        System.out.println("order:" + ORDER);

        /** Basis function LUT radius, if zero no LUT is used. */
        final int LUT_RADIUS = 50;

        if (LUT_RADIUS > 0) {
            System.out.println("LUT_RADIUS:" + LUT_RADIUS);
            luts = new LUT[ORDER + 1][ORDER + 1];

            for (int m = 0; m <= ORDER; m++) {
                for (int n = 0; n <= (ORDER - m); n++) {
                    luts[m][n] = new BasicLUT(LUT_RADIUS);
                }
            }

            initLUT();
        } else {
            luts = null;
        }

        ///checkOrthogonal();
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BasicLegendreExtractor object.
     */
    public BasicLegendreExtractor ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------//
    // extractMoments //
    //----------------//
    @Override
    protected void extractMoments ()
    {
        if (luts == null) {
            // No use of LUTs
            extractMomentsDirectly();

            return;
        }

        final double area = 1.0 / (radius * radius);
        final double centerX = center.getX();
        final double centerY = center.getY();
        final LUT anyLut = luts[0][0]; // Just for template
        final int lutRadius = anyLut.getRadius();

        // Coefficients
        final double[][] coeffs = new double[ORDER + 1][ORDER + 1];

        for (int i = 0; i < mass; i++) {
            // Map image coordinates to LUT coordinates
            double x = xx[i] - centerX;
            double y = yy[i] - centerY;
            double lx = ((x * lutRadius) / radius) + lutRadius;
            double ly = ((y * lutRadius) / radius) + lutRadius;

            // Summation of basis function
            if (anyLut.contains(lx, ly)) {
                for (int m = 0; m <= ORDER; m++) {
                    for (int n = 0; n <= (ORDER - m); n++) {
                        coeffs[m][n] += luts[m][n].interpolate(lx, ly);
                    }
                }
            }
        }

        // Save to descriptor
        for (int m = 0; m <= ORDER; m++) {
            double mNorm = Math.sqrt(((2 * m) + 1) / 2.0);

            for (int n = 0; n <= (ORDER - m); n++) {
                double nNorm = Math.sqrt(((2 * n) + 1) / 2.0);
                descriptor.setMoment(m, n, coeffs[m][n] * area * mNorm * nNorm);
            }
        }
    }

    //------------------------//
    // extractMomentsDirectly // Not using LUT (so rather slow...)
    //------------------------//
    private void extractMomentsDirectly ()
    {
        final double area = 1.0 / (radius * radius);
        final double centerX = center.getX();
        final double centerY = center.getY();

        for (int m = 0; m <= ORDER; m++) {
            double mNorm = Math.sqrt(((2 * m) + 1) / 2.0);

            for (int n = 0; n <= (ORDER - m); n++) {
                double nNorm = Math.sqrt(((2 * n) + 1) / 2.0);
                double val = 0;

                for (int i = 0; i < mass; i++) {
                    // Map image coordinate to basis function coordinate
                    double x = xx[i] - centerX;
                    double y = yy[i] - centerY;

                    double ix = x / radius; // [-1 .. +1]
                    ix = Math.min(1, Math.max(ix, -1));

                    double iy = y / radius; // [-1 .. +1]
                    iy = Math.min(1, Math.max(iy, -1));

                    // Summation of basis function
                    double inc = P[m].evaluate(ix) * P[n].evaluate(iy);
                    inc *= (mNorm * nNorm);
                    val += inc;
                }

                // Save to descriptor
                descriptor.setMoment(m, n, val * area);
            }
        }
    }

    //-------------//
    // reconstruct //
    //-------------//
    @Override
    public void reconstruct (WritableRaster raster)
    {
        int size = raster.getHeight();

        ///double[][] buf = new double[size][size];
        final int rad = size / 2;
        final int[] ia = new int[1];
        double minVal = Double.MAX_VALUE;
        double maxVal = Double.MIN_VALUE;

        for (int x = 0; x < size; x++) {
            double tx = (x - rad) / (double) rad;

            for (int y = 0; y < size; y++) {
                double ty = (y - rad) / (double) rad;
                double val = 0;

                for (int m = 0; m <= ORDER; m++) {
                    for (int n = 0; n <= (ORDER - m); n++) {
                        double tau = Math.sqrt((((2 * m) + 1) * ((2 * n) + 1)) / 4d);
                        double moment = descriptor.getMoment(m, n);
                        double pm = P[m].evaluate(tx);
                        double pn = P[n].evaluate(ty);
                        double inc = tau * moment * pm * pn;
                        val += inc;

                        //                        System.out.println(
                        //                            String.format(
                        //                                Locale.UK,
                        //                                "m:%02d n:%02d tau:%7.3f mom:%6.3f pmn:%6.3f pn:%6.3f inc:%6.3f val:%6.3f",
                        //                                m,
                        //                                n,
                        //                                tau,
                        //                                moment,
                        //                                pmn,
                        //                                pn,
                        //                                inc,
                        //                                val));
                    }
                }

                //                buf[x][y] = val;
                minVal = Math.min(minVal, val);
                maxVal = Math.max(maxVal, val);

                int gray = Math.min(255, Math.max(0, (int) Math.rint(val * 256)));
                ia[0] = 255 - gray;
                raster.setPixel(x, y, ia);
            }
        }

        System.out.println("minVal:" + minVal + " maxVal:" + maxVal);

        //        // Normalize the gray levels (berk!)
        //        for (int x = 0; x < size; x++) {
        //            for (int y = 0; y < size; y++) {
        //                double val = buf[x][y];
        //                int    gray = (int) Math.rint((val / maxVal) * 255);
        //                gray = Math.min(255, Math.max(0, gray));
        //                ia[0] = 255 - gray;
        //                raster.setPixel(x, y, ia);
        //            }
        //        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //    //-----------------//
    //    // checkOrthogonal //
    //    //-----------------//
    //    private static void checkOrthogonal ()
    //    {
    //        for (int m = 0; m <= ORDER; m++) {
    //            for (int n = 0; n <= (ORDER - m); n++) {
    //                double val = 0;
    //
    //                for (int x = 0; x < LUT_SIZE; x++) {
    //                    double tx = (x - lutRadius) / (double) lutRadius;
    //
    //                    val += ((P[m].evaluate(tx) * P[n].evaluate(tx)) / lutRadius);
    //                }
    //
    //                double exp = (m == n) ? (2.0 / ((2 * m) + 1)) : 0;
    //
    //                System.out.println(
    //                    "m:" + m + " n:" + n + " exp:" +
    //                    String.format("%5.2f", exp) + " val:" +
    //                    String.format("%5.2f", Math.abs(val)));
    //            }
    //        }
    //    }
    //---------------------//
    // generatePolynomials //
    //---------------------//
    /**
     * Generate all Legendre polynomials, iteratively up to ORDER.
     *
     * @return the array of polynomials
     */
    private static Polynomial[] generatePolynomials ()
    {
        Polynomial[] Q = new Polynomial[ORDER + 1];

        Q[0] = new Polynomial(1, 0);
        Q[1] = new Polynomial(1, 1);

        for (int n = 2; n <= ORDER; n++) {
            Q[n] = Q[1].times(Q[n - 1]).times((2 * n) - 1).minus(Q[n - 2].times(n - 1)).times(
                    1d / n);
        }

        if (false) {
            for (int n = 0; n <= ORDER; n++) {
                System.out.println("P[" + n + "] = " + Q[n]);
            }
        }

        return Q;
    }

    //---------//
    // initLUT //
    //---------//
    /**
     * Compute, once for all, the lookup table values.
     */
    private static void initLUT ()
    {
        final LUT anyLut = luts[0][0]; // Just for template
        final int lutSize = anyLut.getSize();
        final int lutRadius = anyLut.getRadius();

        for (int x = 0; x < lutSize; x++) {
            double tx = (x - lutRadius) / (double) lutRadius;

            for (int y = 0; y < lutSize; y++) {
                double ty = (y - lutRadius) / (double) lutRadius;

                for (int m = 0; m <= ORDER; m++) {
                    double pmx = P[m].evaluate(tx);

                    for (int n = 0; n <= (ORDER - m); n++) {
                        luts[m][n].assign(x, y, pmx * P[n].evaluate(ty));
                    }
                }
            }
        }
    }
}
