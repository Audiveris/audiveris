//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     F l o o d F i l l e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
 * Class <code>FloodFiller</code>
 *
 * @author Hervé Bitteur
 */
public class FloodFiller
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final BufferedImage image;

    private final int width;

    private final int height;

    //~ Constructors -------------------------------------------------------------------------------

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

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Flood fill the area from provided (x,y) location by recursively converting
     * pixels from oldColor to newColor.
     * <p>
     * This implementation flood fills only vertically and horizontally, not in diagonal.
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
                fill(x - 1, y, oldColor, newColor); // Left
                fill(x, y - 1, oldColor, newColor); // Top
                fill(x, y + 1, oldColor, newColor); // Bottom
                fill(x + 1, y, oldColor, newColor); // Right
            }
        }
    }

    /**
     * Check if the pixel at provided (x,y) location is an isolated pixel, and if so,
     * replace it by newColor.
     *
     * @param x        the abscissa of the pixel to process
     * @param y        the ordinate of the pixel to process
     * @param newColor the color to replace with
     */
    public void adjust (int x,
                        int y,
                        int newColor)
    {
        // Check if pixel is isolated (no vertical/horizontal neighbor w/ same color)
        final int pix = image.getRGB(x, y);

        if (image.getRGB(x - 1, y) == pix) {
            return;
        }

        if (image.getRGB(x, y - 1) == pix) {
            return;
        }

        if (image.getRGB(x, y + 1) == pix) {
            return;
        }

        if (image.getRGB(x + 1, y) == pix) {
            return;
        }

        image.setRGB(x, y, newColor);
    }
}
