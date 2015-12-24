//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         B y t e U t i l                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import ij.process.ByteProcessor;

import java.util.Arrays;

/**
 * Class {@code ByteUtil} gathers convenient methods dealing with bytes.
 *
 * @author Hervé Bitteur
 */
public abstract class ByteUtil
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Fill the ByteProcessor with provided value.
     *
     * @param bp  buffer
     * @param val value to fill buffer with
     */
    public static void fill (ByteProcessor bp,
                             int val)
    {
        final byte[] pixels = (byte[]) bp.getPixels();
        Arrays.fill(pixels, (byte) val);
    }

    /**
     * Fill the provided ByteProcessor with background value (255).
     *
     * @param bp the ByteProcessor to set to background
     */
    public static void raz (ByteProcessor bp)
    {
        fill(bp, 255);
    }
}
