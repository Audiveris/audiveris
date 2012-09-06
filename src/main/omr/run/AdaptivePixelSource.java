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

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import java.util.Arrays;

/**
 * Class {@code AdaptivePixelSource} is an abstract implementation of
 * {@code PixelSource} which provides foreground information based on
 * mean value and standard deviation in pixel neighborhood.
 *
 * <p>See <a href="http://www.mediateam.oulu.fi/publications/pdf/24.p">
 * work of Sauvola et al.</a>
 *
 * <p>The mean value and the standard deviation value are provided thanks to
 * underlying integrals {@link Tile} instances.
 * The precise tile size and behavior is the responsibility of subclasses of
 * this abstract class.
 *
 * <p> See <a
 * href="http://www.dfki.uni-kl.de/~shafait/papers/Shafait-efficient-binarization-SPIE08.pdf">
 * work of Shafait et al.</a>
 *
 * <pre>
 * 0---------------------------------------------+---------------+
 * |                                             |               |
 * |                                             |               |
 * |                                             |               |
 * |                                            a|              b|
 * +---------------------------------------------+---------------+
 * |                                             |               |
 * |                                             |               |
 * |                                             |               |
 * |                                             |               |
 * |                                             |               |
 * |                                            c|              d|
 * +---------------------------------------------+---------------+
 * </pre>
 * Key table features:
 * <ul>
 * <li>Assumption: The integral of any rectangle with origin at (0,0) is stored
 * in the bottom right cell of the rectangle.</li>
 *
 * <li>As a consequence the integral of any rectangle, whatever its origin,
 * can be simply computed as:
 * <code>a + d - b - c</code>
 * </li>
 *
 * <li>In particular if lower right rectangle is reduced to a single cell, then
 * <code>d = pixel_value + top + left - topLeft</code><br/>
 * This property is used to incrementally populate the table.</li>
 * </ul>
 *
 * @author ryo/twitter &#64;xiaot_Tag
 * @author Hervé Bitteur
 */
public abstract class AdaptivePixelSource
        extends RawPixelSourceWrapper
        implements PixelSource
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(AdaptivePixelSource.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Default value for (half of) window size. */
    protected final int HALF_WINDOW_SIZE = constants.halfWindowSize.getValue();

    /** Coefficient of mean value. */
    private final double MEAN_COEFF = constants.meanCoeff.getValue();

    /** Coefficient of standard deviation. */
    private final double STD_DEV_COEFF = constants.stdDevCoeff.getValue();

    /** Vertical tile for plain value. */
    protected Tile tile;

    /** Vertical tile for squared value. */
    protected Tile sqrTile;

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
        super(source);
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
        double mean = tile.getMean(x, y);
        double sqrMean = sqrTile.getMean(x, y);
        double var = Math.abs(sqrMean - mean * mean);
        double stdDev = Math.sqrt(var);

        // This is the key formula
        double threshold = MEAN_COEFF * mean + STD_DEV_COEFF * stdDev;

        int pixValue = source.getPixel(x, y);
        boolean isFore = pixValue <= threshold;

        return isFore;
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //------//
    // Tile //
    //------//
    /**
     * Handles a vertical tile of integrals.
     */
    protected class Tile
    {

        /** Width of the tile circular buffer. */
        protected final int TILE_WIDTH;

        /** Remember if we handle squared values or plain values. */
        protected final boolean squared;

        /** Height of the tile = height of the image. */
        protected final int height;

        /** Abscissa corresponding to the right side of the tile. */
        protected int xRight = -1;

        /** Circular buffer for integrals. */
        protected final long[][] sums;

        /**
         * Create a tile instance.
         *
         * @param tileWitdh tile width
         * @param height    tile height = image height
         * @param squared   true for squared values, false for plain values
         */
        public Tile (int tileWidth,
                     int height,
                     boolean squared)
        {
            this.TILE_WIDTH = tileWidth;
            this.height = height;
            this.squared = squared;

            // Allocate buffer of integrals
            sums = new long[TILE_WIDTH][height];

            // Initialize the "previous" column
            Arrays.fill(sums[TILE_WIDTH - 1], 0);
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

            // Make sure the tile is positioned correctly
            shiftTile(x2);

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
         * Make sure the column at abscissa 'x2' lies within the tile.
         *
         * @param x2 the abscissa to check
         */
        protected void shiftTile (int x2)
        {
            // Void by default
        }

        /**
         * Populate the provided column with proper integrals, building
         * on the content of previous column.
         *
         * @param x the column to populate
         */
        protected void populateColumn (int x)
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
                "Threshold formula coefficient for mean pixel value");

        Constant.Ratio stdDevCoeff = new Constant.Ratio(
                1.0,
                "Threshold formula coefficient for pixel standard deviation");

    }
}
