//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         A r e a M a s k                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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

    //-------//
    // fore //
    //-------//
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

        return apply(
                new AreaMask.Adapter()
                {
                    @Override
                    public void process (int x,
                                         int y)
                    {
                        if (filter.get(x, y) == 0) {
                            fore.value++;
                        }
                    }
                });
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
}
