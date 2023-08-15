//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                G e o m e t r i c M o m e n t s                                 //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

/**
 * Class <code>GeometricMoments</code> encapsulates the set of all geometric moments that
 * characterize an image.
 * <p>
 * We use only central moments (invariant Hu moments are disabled by default).
 *
 * @author Hervé Bitteur
 */
public class GeometricMoments
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GeometricMoments.class);

    /**
     * Hu coefficients are optional.
     */
    public static final boolean useHuCoefficients = false;

    /** Number of features handled: {@value} */
    public static final int size = 12 + (useHuCoefficients ? 7 : 0);

    /** Labels for better display */
    private static final String[] labels =
    {
            /**
             * Unit-normalized stuff
             */
            "weight", // 0
            "width", //  1
            "height", // 2

            /**
             * Mass-normalized central moments
             */
            "n20", // 3
            "n11", // 4
            "n02", // 5
            "n30", // 6
            "n21", // 7
            "n12", // 8
            "n03", // 9

            /**
             * Mass center
             */
            "xBar", // 10
            "yBar", // 11

            /**
             * Hu coefficients, if any
             */
            "h1", // 12
            "h2", // 13
            "h3", // 14
            "h4", // 15
            "h5", // 16
            "h6", // 17
            "h7", // 18
    };

    //~ Instance fields ----------------------------------------------------------------------------

    /** The various moments, implemented as an array of double's. */
    private final double[] k = new double[size];

    //~ Constructors -------------------------------------------------------------------------------

    //------------------//
    // GeometricMoments //
    //------------------//
    /**
     * No-arg constructor, needed for XML binder.
     */
    public GeometricMoments ()
    {
    }

    /**
     * Creates a new GeometricMoments object.
     *
     * @param that the other GeometricMoments to clone
     */
    public GeometricMoments (GeometricMoments that)
    {
        System.arraycopy(that.k, 0, this.k, 0, size);
    }

    /**
     * Compute the moments for a set of points whose x and y coordinates are provided,
     * all values being normalized by the provided unit value.
     *
     * @param xx   the array of abscissa values
     * @param yy   the array of ordinate values
     * @param dim  the number of points
     * @param unit the length (number of pixels) of normalizing unit
     */
    public GeometricMoments (int[] xx,
                             int[] yy,
                             int dim,
                             int unit)
    {
        // Safety check
        if (unit == 0) {
            throw new IllegalArgumentException("Zero-valued unit");
        }

        int xMin = Integer.MAX_VALUE;
        int xMax = Integer.MIN_VALUE;
        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;

        // Normalized GeometricMoments
        double n00 = dim / (double) (unit * unit);
        double n01 = 0d;
        double n02 = 0d;
        double n03 = 0d;
        double n10 = 0d;
        double n11 = 0d;
        double n12 = 0d;
        double n20 = 0d;
        double n21 = 0d;
        double n30 = 0d;

        // Total weight
        double w = dim; // For p+q == 0
        double w2 = w * w; // For p+q == 2
        double w3 = Math.sqrt(w * w * w * w * w); // For p+q == 3

        // Mean x & y, width & height
        for (int i = dim - 1; i >= 0; i--) {
            int x = xx[i];
            n10 += x;

            if (x < xMin) {
                xMin = x;
            }

            if (x > xMax) {
                xMax = x;
            }

            int y = yy[i];
            n01 += y;

            if (y < yMin) {
                yMin = y;
            }

            if (y > yMax) {
                yMax = y;
            }
        }

        n10 /= dim;
        n01 /= dim;

        for (int i = dim - 1; i >= 0; i--) {
            // Coordinates centered around center of mass
            double x = xx[i] - n10;
            double y = yy[i] - n01;
            n11 += (x * y);
            n12 += (x * y * y);
            n21 += (x * x * y);
            n20 += (x * x);
            n02 += (y * y);
            n30 += (x * x * x);
            n03 += (y * y * y);
        }

        // Normalize
        //
        // p + q = 2
        n11 /= w2;
        n20 /= w2;
        n02 /= w2;
        //
        // p + q = 3
        n12 /= w3;
        n21 /= w3;
        n30 /= w3;
        n03 /= w3;

        // Unit-based weight, width and height
        k[0] = n00; // Unit-based Weight
        k[1] = (double) (xMax - xMin + 1) / unit; // Unit-based Width
        k[2] = (double) (yMax - yMin + 1) / unit; // Unit-based Height

        // Non-orthogonal central moments
        // (invariant to translation & scaling)
        k[3] = n20; // X absolute eccentricity
        k[4] = n11; // XY covariance
        k[5] = n02; // Y absolute eccentricity
        k[6] = n30; // X signed eccentricity
        k[7] = n21; // V vs. ^
        k[8] = n12; // > vs. <
        k[9] = n03; // Y signed eccentricity

        // Mass center
        k[10] = n10; // xBar
        k[11] = n01; // yBar

        if (useHuCoefficients) {
            // Orthogonals moments (Hu set)
            // (Invariant to translation / scaling / rotation)
            int i = 12;
            k[i++] = n20 + n02;
            //
            k[i++] = ((n20 - n02) * (n20 - n02)) + (4 * n11 * n11);
            //
            k[i++] = ((n30 - (3 * n12)) * (n30 - (3 * n12))) + ((n03 - (3 * n21)) * (n03 - (3
                    * n21)));
            //
            k[i++] = ((n30 + n12) * (n30 + n12)) + ((n03 + n21) * (n03 + n21));
            //
            k[i++] = ((n30 - (3 * n12)) * (n30 + n12) * (((n30 + n12) * (n30 + n12)) - (3 * (n21
                    + n03) * (n21 + n03)))) + ((n03 - (3 * n21)) * (n03 + n21) * (((n03 + n21)
                            * (n03 + n21)) - (3 * (n12 + n30) * (n12 + n30))));
            //
            k[i++] = ((n20 - n02) * (((n30 + n12) * (n30 + n12)) - ((n03 + n21) * (n03 + n21))))
                    + (4 * n11 * (n30 + n12) * (n03 + n21));
            //
            k[i++] = (((3 * n21) - n03) * (n30 + n12) * (((n30 + n12) * (n30 + n12)) - (3 * (n21
                    + n03) * (n21 + n03)))) - (((3 * n12) - n30) * (n03 + n21) * (((n03 + n21)
                            * (n03 + n21)) - (3 * (n12 + n30) * (n12 + n30))));
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------//
    // getCentroid //
    //-------------//
    /**
     * Report the mass center of the glyph.
     *
     * @return the centroid
     */
    public Point getCentroid ()
    {
        return new Point((int) Math.rint(k[10]), (int) Math.rint(k[11]));
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the height of the glyph, normalized by unit.
     *
     * @return the normalized height
     */
    public double getHeight ()
    {
        return k[2];
    }

    //--------//
    // getN12 //
    //--------//
    /**
     * Report the n11 moment (which relates to xy covariance).
     *
     * @return the n11 moment
     */
    public double getN11 ()
    {
        return k[4];
    }

    //--------//
    // getN12 //
    //--------//
    /**
     * Report the n12 moment (which relates to xy2: &gt; vs &lt;).
     *
     * @return the n12 moment
     */
    public double getN12 ()
    {
        return k[8];
    }

    //--------//
    // getN21 //
    //--------//
    /**
     * Report the n21 moment (which relates to x2y: V vs ^).
     *
     * @return the n21 moment
     */
    public double getN21 ()
    {
        return k[7];
    }

    //-----------//
    // getValues //
    //-----------//
    /**
     * Report the array of moment values.
     *
     * @return the moment values
     */
    public double[] getValues ()
    {
        return k;
    }

    //-----------//
    // getWeight //
    //-----------//
    /**
     * Report the total weight of the glyph, normalized by unit**2.
     *
     * @return the normalized weight
     */
    public double getWeight ()
    {
        return k[0];
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of the glyph, normalized by unit.
     *
     * @return the normalized width
     */
    public double getWidth ()
    {
        return k[1];
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        for (int i = 0; i < k.length; i++) {
            sb.append(" ").append(labels[i]).append("=").append(String.format("%g", k[i]));
        }

        sb.append("}");

        return sb.toString();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //----------//
    // getLabel //
    //----------//
    /**
     * Report the label related to moment at specified index.
     *
     * @param index the moment index
     * @return the related index
     */
    public static String getLabel (int index)
    {
        return labels[index];
    }
}
