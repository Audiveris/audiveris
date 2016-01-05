//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     F l o o d F i l l e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import java.awt.image.BufferedImage;

/**
 * Class {@code FloodFiller}
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
     * Flood fill the area from provided (x,y) location by converting
     * from oldColor to newColor.
     * Simplistic implementation, but sufficient for the time being.
     *
     * @param x
     * @param y
     * @param oldColor
     * @param newColor
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
