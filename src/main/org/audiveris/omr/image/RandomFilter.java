//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    R a n d o m F i l t e r                                     //
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

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code RandomFilter} is a specialization of {@link AdaptiveFilter} which
 * computes mean and standard deviation values based on pre-populated tables of integrals.
 * <p>
 * This implementation is ThreadSafe and provides fast random access to any location in constant
 * time. The drawback is that each of the two underlying tables of integrals needs 8 bytes per image
 * pixel.
 *
 * @author ryo/twitter &#64;xiaot_Tag
 * @author Hervé Bitteur
 */
@ThreadSafe
public class RandomFilter
        extends AdaptiveFilter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RandomFilter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an adaptive wrapper on a raw pixel source.
     *
     * @param source      the underlying source of raw pixels
     * @param meanCoeff   the coefficient for mean value
     * @param stdDevCoeff the coefficient for standard deviation value
     */
    public RandomFilter (ByteProcessor source,
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //
    //--------//
    // MyTile //
    //--------//
    /**
     * This is a degenerated tile, since it is as big as the source
     * image and is never shifted.
     */
    private class MyTile
            extends Tile
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyTile (boolean squared)
        {
            // Allocate a tile as big as the source
            super(source.getWidth(), source.getHeight(), squared);

            // Populate the whole tile at once
            for (int x = 0, width = source.getWidth(); x < width; x++) {
                populateColumn(x);
            }
        }
    }
}
