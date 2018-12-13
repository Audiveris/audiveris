//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     F l o o d F i l l e r                                      //
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

/**
 * Class {@code FloodFiller}
 *
 * @author Hervé Bitteur
 */
public class FloodFiller
{

    private final BufferedImage image;

    private final int width;

    private final int height;

    /**
     * Creates a new FloodFiller object.
     *
     * @param image the image to fill
     */
    public FloodFiller (BufferedImage image)
    {
        this.image = image;
        width = image.getWidth();
        height = image.getHeight();
    }

    /**
     * Flood fill the area from provided (x,y) location by recursively converting
     * pixels from oldColor to newColor.
     * Simplistic implementation, but sufficient for the time being.
     *
     * @param x        the abscissa of the pixel to process
     * @param y        the ordinate of the pixel to process
     * @param oldColor the color to be replaced
     * @param newColor the color to replace with
     */
    public void fill (int x,
                      int y,
                      int oldColor,
                      int newColor)
    {
        if ((x >= 0) && (x < width) && (y >= 0) && (y < height)) {
            int pix = image.getRGB(x, y);

            if ((pix == oldColor) && (pix != newColor)) {
                image.setRGB(x, y, newColor);
                fill(x - 1, y, oldColor, newColor);
                fill(x + 1, y, oldColor, newColor);
                fill(x, y - 1, oldColor, newColor);
                fill(x, y + 1, oldColor, newColor);
            }
        }
    }
}
