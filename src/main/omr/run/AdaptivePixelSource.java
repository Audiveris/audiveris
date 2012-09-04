//----------------------------------------------------------------------------//
//                                                                            //
//                   A d a p t i v e P i x e l S o u r c e                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2012. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.log.Logger;

import net.jcip.annotations.ThreadSafe;

import java.awt.Rectangle;

/**
 * Class {@code AdaptivePixelSource} implements Interface
 * {@code PixelSource} to provide foreground information based on
 * mean value and standard deviation in pixel neighborhood.
 * <p>
 * This implementation uses a table of integrals, so that it can calculate local
 * threshold in constant time at random location.
 * <p>
 * This class is thread-safe and can be efficiently used by several threads in
 * parallel. The main drawback is the memory used by the two tables, which 
 * amounts to 16 bytes per pixel (2 long's per pixel). 
 * For a image of size 2000 * 3000 (thus typically 6 MBytes for the image), the
 * tables represent 96 MBytes, which is a lot.
 *
 * @author ryo/twitter &#64;xiaot_Tag
 * @author Hervé Bitteur
 */
@ThreadSafe
public class AdaptivePixelSource
        implements PixelSource
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(AdaptivePixelSource.class);

    /** Default value for (half of) window size. */
    public static final int HALF_WINDOW_SIZE = 18;

    /** Coefficient of mean value. */
    public static final double MEAN_COEFF = 0.67;

    /** Coefficient of standard deviation. */
    public static final double STD_DEV_COEFF = 1;

    //~ Instance fields --------------------------------------------------------
    //
    /** Underlying raw pixel source. */
    private final RawPixelSource source;

    /** hold integral image sum pixs for calculating mean. */
    private long[][] sumPixs;

    /** hold integral image square sum pixs for calculating mean. */
    private long[][] sqrSumPixs;

    // 0---------------------------------------------+---------------+
    // |                                             |               |
    // |                                             |               |
    // |                                             |               |
    // |                                            a|              b|
    // +---------------------------------------------+---------------+
    // |                                             |               |
    // |                                             |               |
    // |                                             |               |
    // |                                             |               |
    // |                                             |               |
    // |                                            c|              d|
    // +---------------------------------------------+---------------+
    /*
     * Assumption: The integral of any rectangle with origin at (0,0) is stored
     * in the bottom right cell of the rectangle.
     *
     * Consequence: The integral of any rectangle, whatever its origin,
     * can be simply computed as: a + d - b - c
     * 
     * In particular: If lower right rectangle is reduced to a single cell,
     * d = pixel value + top + left - topLeft
     * This property is used to populate the table.
     */
    //~ Constructors -----------------------------------------------------------
    //
    //---------------------//
    // AdaptivePixelSource //
    //---------------------//
    /**
     * Create an adaptive wrapper on a raw pixel source.
     *
     * @param source the underlying source of raw pixels
     */
    public AdaptivePixelSource (RawPixelSource source)
    {
        this.source = source;

        // Compute integrals
        sumPixs = getSumPixs(/* squared => */false);
        sqrSumPixs = getSumPixs(/* squared => */true);
    }

    //~ Methods ----------------------------------------------------------------
    //
    // -------//
    // isFore //
    // -------//
    @Override
    public boolean isFore (int x,
                           int y)
    {
        double mean = getMean(x, y, sumPixs);
        double sqrMean = getMean(x, y, sqrSumPixs);
        double var = Math.abs(sqrMean - mean * mean);
        double stdDev = Math.sqrt(var);
        double threshold = MEAN_COEFF * mean + STD_DEV_COEFF * stdDev;

        int originPixValue = source.getPixel(x, y);
        boolean isFore = originPixValue <= threshold;

        return isFore;
    }

    //-----------//
    // getHeight //
    //-----------//
    @Override
    public final int getHeight ()
    {
        return source.getHeight();
    }

    //----------//
    // getPixel //
    //----------//
    @Override
    public final int getPixel (int x,
                               int y)
    {
        return source.getPixel(x, y);
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public final int getWidth ()
    {
        return source.getWidth();
    }

    //------------//
    // getSumPixs //
    //------------//
    /**
     * Compute integral image sum pixels
     *
     * @param squared true for summing square values, false for summing values
     */
    private long[][] getSumPixs (boolean squared)
    {
        final int width = getWidth();
        final int height = getHeight();
        final long[][] sums = new long[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Retrieve picture cell value
                long value = getPixel(x, y);
                if (squared) {
                    value *= value;
                }

                // Add rectangle above, if any
                if (y > 0) {
                    value += sums[x][y - 1];
                }

                // Add rectangle on left, if any
                if (x > 0) {
                    value += sums[x - 1][y];

                    // Substract rectangle on upper left, if any
                    if (y > 0) {
                        value -= sums[x - 1][y - 1];
                    }
                }

                // Store rectangle integral into cell
                sums[x][y] = value;
            }
        }

        return sums;
    }

    //---------//
    // getMean //
    //---------//
    /**
     * Compute the mean value (or squared value) in a standard window
     * around the provided location.
     *
     * @param x    provided abscissa
     * @param y    provided ordinate
     * @param sums the proper table to use (sumPixs or sqrSumPixs)
     * @return the average [squared] value over the standard window
     */
    private double getMean (int x,
                            int y,
                            long[][] sums)
    {
        final int width = getWidth();
        final int height = getHeight();

        int x1 = Math.max(-1, x - HALF_WINDOW_SIZE - 1);
        int x2 = Math.min(width - 1, x + HALF_WINDOW_SIZE);

        int y1 = Math.max(-1, y - HALF_WINDOW_SIZE - 1);
        int y2 = Math.min(height - 1, y + HALF_WINDOW_SIZE);

        // Upper left
        long a = (x1 >= 0 && y1 >= 0) ? sums[x1][y1] : 0;

        // Above
        long b = (y1 >= 0) ? sums[x2][y1] : 0;

        // Left
        long c = (x1 >= 0) ? sums[x1][y2] : 0;

        // Lower right
        long d = sums[x2][y2];

        // Integral for window rectangle
        double sum = a + d - b - c;

        // Area = number of values
        int area = (y2 - y1) * (x2 - x1);

        // Return mean value
        return sum / area;
    }
}
