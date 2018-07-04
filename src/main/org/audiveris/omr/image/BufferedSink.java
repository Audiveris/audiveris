//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B u f f e r e d S i n k                                    //
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

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * Class {@code BufferedSink} wraps a BufferedImage as a PixelSink.
 *
 * @author Hervé Bitteur
 */
public class BufferedSink
        implements PixelSink
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The wrapped BufferedImage instance. */
    private final BufferedImage image;

    /** Image raster. */
    private final WritableRaster raster;

    /** Buffer to write pixel value. */
    private final int[] pixelArray;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BufferedSink object around a given BufferedImage
     * instance.
     *
     * @param image the BufferedImage to interface
     */
    public BufferedSink (BufferedImage image)
    {
        this.image = image;
        raster = image.getRaster();
        pixelArray = new int[4];
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public int getHeight ()
    {
        return image.getHeight();
    }

    @Override
    public int getWidth ()
    {
        return image.getWidth();
    }

    @Override
    public void setValue (int x,
                          int y,
                          int val)
    {
        pixelArray[0] = val;
        raster.setPixel(x, y, pixelArray);
    }
}
