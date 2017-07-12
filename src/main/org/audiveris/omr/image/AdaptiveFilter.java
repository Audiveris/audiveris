//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  A d a p t i v e F i l t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.Population;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Class {@code AdaptiveFilter} is an abstract implementation of {@code PixelFilter}
 * which provides foreground information based on mean value and standard deviation in
 * pixel neighborhood.
 * <p>
 * See work of Sauvola et al.<a
 * href="http://www.mediateam.oulu.fi/publications/pdf/24.p">
 * here</a>.
 * <p>
 * The mean value and the standard deviation value are provided thanks to underlying integrals
 * {@link Tile} instances.
 * The precise tile size and behavior is the responsibility of subclasses of this class.
 * <p>
 * See work of Shafait et al. <a
 * href="http://www.dfki.uni-kl.de/~shafait/papers/Shafait-efficient-binarization-SPIE08.pdf">
 * here</a>.
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
 * <li>As a consequence the integral of any rectangle, whatever its origin, can be simply computed
 * as: <code>a + d - b - c</code> </li>
 * <li>In particular if lower right rectangle is reduced to a single cell, then
 * <code>d = pixel_value + top + left - topLeft</code>
 * <br/>
 * This property is used to incrementally populate the table.</li>
 * </ul>
 *
 * @author ryo/twitter &#64;xiaot_Tag
 * @author Hervé Bitteur
 */
public abstract class AdaptiveFilter
        extends SourceWrapper
        implements PixelFilter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveFilter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Default value for (half of) window size. */
    protected final int HALF_WINDOW_SIZE = constants.halfWindowSize.getValue();

    /** Coefficient of mean value. */
    protected final double MEAN_COEFF;

    /** Coefficient of standard deviation. */
    protected final double STD_DEV_COEFF;

    /** Table for integrals of plain values. */
    protected Tile tile;

    /** Table for integrals of squared values. */
    protected Tile sqrTile;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an adaptive wrapper on a pixel source.
     *
     * @param source      the underlying source of raw pixels
     * @param meanCoeff   coefficient for mean variable
     * @param stdDevCoeff coefficient for standard deviation value
     */
    public AdaptiveFilter (ByteProcessor source,
                           double meanCoeff,
                           double stdDevCoeff)
    {
        super(source);

        this.MEAN_COEFF = meanCoeff;
        this.STD_DEV_COEFF = stdDevCoeff;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------------//
    // getDefaultMeanCoeff //
    //---------------------//
    public static double getDefaultMeanCoeff ()
    {
        return constants.meanCoeff.getValue();
    }

    //-----------------------//
    // getDefaultStdDevCoeff //
    //-----------------------//
    public static double getDefaultStdDevCoeff ()
    {
        return constants.stdDevCoeff.getValue();
    }

    //---------------------//
    // setDefaultMeanCoeff //
    //---------------------//
    public static void setDefaultMeanCoeff (double meanCoeff)
    {
        constants.meanCoeff.setValue(meanCoeff);
    }

    //-----------------------//
    // setDefaultStdDevCoeff //
    //-----------------------//
    public static void setDefaultStdDevCoeff (double stdDevCoeff)
    {
        constants.stdDevCoeff.setValue(stdDevCoeff);
    }

    //---------------//
    // filteredImage //
    //---------------//
    @Override
    public ByteProcessor filteredImage ()
    {
        ByteProcessor ip = new ByteProcessor(source.getWidth(), source.getHeight());

        for (int x = 0, w = ip.getWidth(); x < w; x++) {
            for (int y = 0, h = ip.getHeight(); y < h; y++) {
                if (isFore(x, y)) {
                    ip.set(x, y, FOREGROUND);
                } else {
                    ip.set(x, y, BACKGROUND);
                }
            }
        }

        return ip;
    }

    //------------//
    // getContext //
    //------------//
    @Override
    public Context getContext (int x,
                               int y)
    {
        final int imageWidth = source.getWidth();
        final int imageHeight = source.getHeight();

        int xMin = Math.max(0, x - HALF_WINDOW_SIZE);
        int xMax = Math.min(imageWidth - 1, x + HALF_WINDOW_SIZE);

        int yMin = Math.max(0, y - HALF_WINDOW_SIZE);
        int yMax = Math.min(imageHeight - 1, y + HALF_WINDOW_SIZE);

        // Brute force retrieval
        Population pop = new Population();

        for (int ix = xMin; ix <= xMax; ix++) {
            for (int iy = yMin; iy <= yMax; iy++) {
                pop.includeValue(source.get(ix, iy));
            }
        }

        if (pop.getCardinality() > 0) {
            double mean = pop.getMeanValue();
            double stdDev = pop.getStandardDeviation();
            double threshold = getThreshold(mean, stdDev);

            return new AdaptiveContext(mean, stdDev, threshold);
        } else {
            return null;
        }
    }

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
        double var = Math.abs(sqrMean - (mean * mean));
        double stdDev = Math.sqrt(var);

        double threshold = getThreshold(mean, stdDev);

        int pixValue = source.get(x, y);
        boolean isFore = pixValue <= threshold;

        return isFore;
    }

    //------------------//
    // getAdaptiveClass //
    //------------------//
    static Class<?> getImplementationClass ()
    {
        String name = constants.className.getValue();

        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            logger.error("Cannot find adaptive filter class " + name);

            return null;
        }
    }

    //--------------//
    // getThreshold //
    //--------------//
    private double getThreshold (double mean,
                                 double stdDev)
    {
        // This is the key formula
        return (MEAN_COEFF * mean) + (STD_DEV_COEFF * stdDev);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------------//
    // AdaptiveContext //
    //-----------------//
    public static class AdaptiveContext
            extends Context
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Mean pixel value in the neighborhood. */
        public final double mean;

        /** Standard deviation of pixel values in the neighborhood. */
        public final double standardDeviation;

        //~ Constructors ---------------------------------------------------------------------------
        public AdaptiveContext (double mean,
                                double standardDeviation,
                                double threshold)
        {
            super(threshold);
            this.mean = mean;
            this.standardDeviation = standardDeviation;
        }
    }

    //
    //------//
    // Tile //
    //------//
    /**
     * Handles a vertical tile of integrals.
     */
    protected class Tile
    {
        //~ Instance fields ------------------------------------------------------------------------

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

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create a tile instance.
         *
         * @param tileWidth tile width
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

        //~ Methods --------------------------------------------------------------------------------
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
            long a = ((x1 >= 0) && (y1 >= 0)) ? sums[x1 % TILE_WIDTH][y1] : 0;

            // Above
            long b = (y1 >= 0) ? sums[x2 % TILE_WIDTH][y1] : 0;

            // Left
            long c = (x1 >= 0) ? sums[x1 % TILE_WIDTH][y2] : 0;

            // Lower right
            long d = sums[x2 % TILE_WIDTH][y2];

            // Integral for window rectangle
            double sum = (a + d) - b - c;

            // Area = number of values
            int area = (y2 - y1) * (x2 - x1);

            // Return mean value
            return sum / area;
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
            final int prevTx = ((x + TILE_WIDTH) - 1) % TILE_WIDTH;
            final long[] prevColumn = sums[prevTx];

            long top = 0;
            long topLeft = 0;

            for (int y = 0; y < height; y++) {
                long left = prevColumn[y];

                long pix = get(x, y);

                if (squared) {
                    pix *= pix;
                }

                long val = (pix + left + top) - topLeft;
                column[y] = val;

                // For next iteration
                top = val;
                topLeft = left;
            }
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
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer halfWindowSize = new Constant.Integer(
                "Pixels",
                18,
                "Half size of window around a given pixel");

        private final Constant.Ratio meanCoeff = new Constant.Ratio(
                0.7,
                "Threshold formula coefficient for mean pixel value");

        private final Constant.Ratio stdDevCoeff = new Constant.Ratio(
                0.9,
                "Threshold formula coefficient for pixel standard deviation");

        private final Constant.String className = new Constant.String(
                "org.audiveris.omr.image.VerticalFilter",
                "org.audiveris.omr.image.VerticalFilter or org.audiveris.omr.image.RandomFilter");
    }
}
