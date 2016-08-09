//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         B y t e U t i l                                        //
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
