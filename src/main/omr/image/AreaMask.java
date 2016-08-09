//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         A r e a M a s k                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.image;

import omr.util.Wrapper;

import ij.process.ByteProcessor;

import java.awt.Rectangle;
import java.awt.geom.Area;

/**
 * Class {@code AreaMask} drives processing of locations using an absolute mask.
 *
 * @author Hervé Bitteur
 */
public class AreaMask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Mask area. */
    private final Area area;

    private final Rectangle rect;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AreaMask object.
     *
     * @param area the defining absolute area
     */
    public AreaMask (Area area)
    {
        this.area = area;
        rect = area.getBounds();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // apply //
    //-------//
    /**
     * Apply the mask and call the provided Adapter for each relevant
     * point of the mask.
     *
     * @param adapter call-back adapter for each relevant point of the mask
     * @return the number of points within mask area
     */
    public int apply (Adapter adapter)
    {
        int count = 0;

        for (int y = rect.y, yBreak = rect.y + rect.height; y < yBreak; y++) {
            for (int x = rect.x, xBreak = rect.x + rect.width; x < xBreak; x++) {
                if (area.contains(x, y)) {
                    adapter.process(x, y);
                    count++;
                }
            }
        }

        return count;
    }

    //------//
    // fore //
    //------//
    /**
     * Count the number of foreground pixels in the mask area.
     *
     * @param fore   (output) to receive the number of foreground pixels
     * @param filter the pixel filter which provides pixel status
     * @return the total number of points in the mask area
     */
    public int fore (final Wrapper<Integer> fore,
                     final ByteProcessor filter)
    {
        fore.value = 0;

        return apply(new ForeCounter(filter, fore));
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    public static interface Adapter
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Method called on each mask relevant point.
         *
         * @param x absolute point abscissa
         * @param y absolute point ordinate
         */
        public void process (int x,
                             int y);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // ForeCounter //
    //-------------//
    private static class ForeCounter
            implements Adapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final ByteProcessor filter;

        private final Wrapper<Integer> fore;

        //~ Constructors ---------------------------------------------------------------------------
        public ForeCounter (ByteProcessor filter,
                            Wrapper<Integer> fore)
        {
            this.filter = filter;
            this.fore = fore;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void process (int x,
                             int y)
        {
            if (filter.get(x, y) == 0) {
                fore.value++;
            }
        }
    }
}
