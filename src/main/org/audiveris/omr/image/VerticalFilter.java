//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  V e r t i c a l F i l t e r                                   //
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
package org.audiveris.omr.image;

import ij.process.ByteProcessor;

import net.jcip.annotations.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code VerticalFilter} is a specialization of
 * {@link AdaptiveFilter} which computes mean and standard
 * deviation values based on vertical tiles of integrals.
 * <p>
 * This implementation is meant to be functionally equivalent to {@link RandomFilter} with similar
 * performances but much lower memory requirements.
 * <p>
 * It uses a vertical window which performs the computation in constant time, provided that the
 * vertical window always moves to the right. Instead of a whole table of integrals, this class uses
 * a vertical tile whose width equals the window size, and the height equals the picture height.
 * <br>
 * <pre>
 *                                              +----------------+
 *                                              |   TILE_WIDTH   |
 * 0---------------------------------------------+---------------+
 * |                                             |               |
 * |                                             |               |
 * |                                             |               |
 * |                                            a|              b|
 * +---------------------------------------------+---------------+
 * |                                             |               |
 * |                                             |    WINDOW     |
 * |                                             |               |
 * |                                             |               |
 * |                                             |       +       |
 * |                                             |               |
 * |                                             |               |
 * |                                             |               |
 * |                                            c|              d|
 * +---------------------------------------------+---------------+
 * </pre>
 * <p>
 * Since only the (1 + WINDOW_SIZE) last columns are relevant, a tile uses a circular buffer to
 * handle only those columns.
 * <p>
 * Drawback: the implementation of the tile as a circular buffer makes an instance of this class
 * usable by only one thread at a time.
 *
 * @author ryo/twitter &#64;xiaot_Tag
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class VerticalFilter
        extends AdaptiveFilter
{

    private static final Logger logger = LoggerFactory.getLogger(VerticalFilter.class);

    /**
     * Create an adaptive wrapper on a raw pixel source.
     *
     * @param source      the underlying source of raw pixels
     * @param meanCoeff   the coefficient for mean value
     * @param stdDevCoeff the coefficient for standard deviation value
     */
    public VerticalFilter (ByteProcessor source,
                           double meanCoeff,
                           double stdDevCoeff)
    {
        super(source, meanCoeff, stdDevCoeff);

        // Prepare tiles
        tile = new MyTile( /* squared => */
                false);
        sqrTile = new MyTile( /* squared => */
                true);
    }

    //--------//
    // MyTile //
    //--------//
    /**
     * A tile as a circular buffer limited by window width.
     */
    private class MyTile
            extends Tile
    {

        MyTile (boolean squared)
        {
            super(2 + (2 * HALF_WINDOW_SIZE), source.getHeight(), squared);
        }

        @Override
        protected void shiftTile (int x2)
        {
            // Make sure we don't violate the tile principle
            if (x2 < xRight) {
                logger.error("SlidingPixelSource can only move forward");
                throw new IllegalStateException();
            }

            // Shift tile as needed to the right
            while (xRight < x2) {
                xRight++;
                populateColumn(xRight);
            }
        }
    }
}
