//----------------------------------------------------------------------------//
//                                                                            //
//                    S l i d i n g P i x e l S o u r c e                     //
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

import net.jcip.annotations.NotThreadSafe;

import java.util.Arrays;
import omr.constant.Constant;
import omr.constant.ConstantSet;

/**
 * Class {@code SlidingPixelSource} is an adaptive
 * {@code PixelSource} which provides foreground information based on
 * mean value and standard deviation in pixel neighborhood.
 * <p>
 * This implementation is meant to be functionally equivalent to
 * {@link AdaptivePixelSource} with similar performances but much lower
 * memory requirements.
 * It uses a sliding window which performs the computation in
 * constant time, provided that the window always moves forward.
 * Instead of a whole table of integrals, this class uses a vertical tile whose
 * width equals the window size, and the height equals the picture height.
 * For a 2000 * 3000 image, and a window size of 40, the memory cost is
 * 2 * 40 * 3000 * 8, which is a bit less than 2 MBytes.
 * <p>
 * Drawback: the implementation of the tile as a circular buffer makes
 * an instance of this class usable by only one thread at a time.
 *
 * @author ryo/twitter &#64;xiaot_Tag
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class SlidingPixelSource
        implements PixelSource
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SlidingPixelSource.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Underlying raw pixel source. */
    private final RawPixelSource source;

    /** Vertical tile for plain value. */
    private final Tile tile;

    /** Vertical tile for squared value. */
    private final Tile sqrTile;

    /** Default value for (half of) window size. */
    private final int HALF_WINDOW_SIZE = constants.halfWindowSize.getValue();

    /** Total window size. */
    private final int WINDOW_SIZE = 1 + 2 * HALF_WINDOW_SIZE;

    /** Coefficient of mean value. */
    private final double MEAN_COEFF = constants.meanCoeff.getValue();

    /** Coefficient of standard deviation. */
    private final double STD_DEV_COEFF = constants.stdDevCoeff.getValue();

    //~ Constructors -----------------------------------------------------------
    //
    //--------------------//
    // SlidingPixelSource //
    //--------------------//
    /**
     * Create an adaptive wrapper on a raw pixel source.
     *
     * @param source the underlying source of raw pixels
     */
    public SlidingPixelSource (RawPixelSource source)
    {
        this.source = source;

        // Prepare tiles
        tile = new Tile(source.getHeight(), /* squared => */ false);
        sqrTile = new Tile(source.getHeight(), /* squared => */ true);
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
        // Mean value
        double mean = tile.getMean(x, y);

        // Standard deviation
        double sqrMean = sqrTile.getMean(x, y);
        double stdDev = Math.sqrt(Math.abs(sqrMean - mean * mean));

        // Inferred threshold
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

    //------//
    // Tile //
    //------//
    //                                              +----------------+
    //                                              |   TILE_WIDTH   |
    // 0---------------------------------------------+---------------+
    // |                                             |               |
    // |                                             |               |
    // |                                             |               |
    // |                                            a|              b|
    // +---------------------------------------------+---------------+
    // |                                             |               |
    // |                                             |    WINDOW     |
    // |                                             |               |
    // |                                             |               |
    // |                                             |       +       |
    // |                                             |               |
    // |                                             |               |
    // |                                             |               |
    // |                                            c|              d|
    // +---------------------------------------------+---------------+
    /**
     * Handles a vertical tile of integrals.
     *
     * <p>Assumption: The integral of any rectangle with origin at (0,0) is
     * stored in the bottom right cell of the rectangle.
     * <p>Consequence: The integral of any rectangle, whatever its origin,
     * can be simply computed as: a + d - b - c
     * <p>In particular: If lower right rectangle is reduced to a single cell,
     * d = pixel value + top + left - topLeft.
     * This property is used to populate the table.
     *
     * <p>Since only the (1 + WINDOW_SIZE) last columns are relevant, this tile
     * uses a circular buffer to handle only those columns.
     */
    private class Tile
    {

        /** Width of the tile circular buffer. */
        private final int TILE_WIDTH = WINDOW_SIZE + 1;

        /** Remember if we handle squared values or plain values. */
        private final boolean squared;

        /** Height of the tile = height of the image. */
        private final int height;

        /** Abscissa corresponding to the right side of the tile. */
        private int xRight = -1;

        /** Circular buffer for integrals. */
        private final long[][] sums;

        /**
         * Create a tile instance.
         *
         * @param height  image height
         * @param squared true for squared values, false for plain values
         */
        public Tile (int height,
                     boolean squared)
        {
            this.height = height;
            this.squared = squared;

            // Allocate buffer of integrals
            sums = new long[TILE_WIDTH][height];
            for (int x = 0; x < TILE_WIDTH; x++) {
                Arrays.fill(sums[x], 0);
            }
        }

        /**
         * Make sure that the sliding window is positioned around the
         * provided location, and return mean data.
         *
         * @param x provided abscissa
         * @param y provided ordinate
         * @return the average value around the provided location
         */
        public double getMean (int x,
                               int y)
        {

            // Compute actual borders of the window
            final int imageWidth = getWidth();

            int x1 = Math.max(-1, x - HALF_WINDOW_SIZE - 1);
            int x2 = Math.min(imageWidth - 1, x + HALF_WINDOW_SIZE);

            int y1 = Math.max(-1, y - HALF_WINDOW_SIZE - 1);
            int y2 = Math.min(height - 1, y + HALF_WINDOW_SIZE);

            // Make sure we don't violate the tile principle
            if (x2 < xRight) {
                logger.severe("SlidingPixelSource can only move forward");
                throw new IllegalStateException();
            }

            // Shift tile as needed to the right
            while (xRight != x2) {
                xRight++;
                populateColumn(xRight);
            }

            // Upper left
            long a = (x1 >= 0 && y1 >= 0) ? sums[x1 % TILE_WIDTH][y1] : 0;

            // Above
            long b = (y1 >= 0) ? sums[x2 % TILE_WIDTH][y1] : 0;

            // Left
            long c = (x1 >= 0) ? sums[x1 % TILE_WIDTH][y2] : 0;

            // Lower right
            long d = sums[x2 % TILE_WIDTH][y2];

            // Integral for window rectangle
            double sum = a + d - b - c;

            // Area = number of values
            int area = (y2 - y1) * (x2 - x1);

            // Return mean value
            return sum / area;

        }

        /**
         * Populate incrementally the provided column with proper
         * integrals.
         *
         * @param x the column to populate (absolute value)
         */
        private void populateColumn (int x)
        {
            // Translate the absolute column to circular buffer column
            final int tx = x % TILE_WIDTH;
            final long[] column = sums[tx];

            // The column to the left (modulo tile width)
            final int prevTx = (x + TILE_WIDTH - 1) % TILE_WIDTH;
            final long[] prevColumn = sums[prevTx];

            long top = 0;
            long topLeft = 0;

            for (int y = 0; y < height; y++) {
                long left = prevColumn[y];

                long pix = getPixel(x, y);
                if (squared) {
                    pix *= pix;
                }

                long val = pix + left + top - topLeft;
                column[y] = val;

                // For next iteration
                top = val;
                topLeft = left;
            }
        }
    }
    //-----------//
    // Constants //
    //-----------//

    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer halfWindowSize = new Constant.Integer(
                "Pixels",
                18,
                "Half size of window around a given pixel");

        Constant.Ratio meanCoeff = new Constant.Ratio(
                0.67,
                "Coefficient for mean pixel value");

        Constant.Ratio stdDevCoeff = new Constant.Ratio(
                1.0,
                "Coefficient for pixel standard deviation");

    }
}
