//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B u f f e r e d S o u r c e                                  //
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
import java.awt.image.ColorModel;
import java.awt.image.Raster;

/**
 * Class {@code BufferedSource} wraps a BufferedImage as a PixelSource.
 *
 * @author Hervé Bitteur
 */
public class BufferedSource
        implements PixelSource
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The wrapped BufferedImage instance. */
    private final BufferedImage image;

    /** Image raster. */
    private final Raster raster;

    /** Is there an alpha channel. */
    private final boolean hasAlpha;

    /** Buffer to read pixel value. */
    private final int[] pixelArray;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BufferedSource object around a given BufferedImage
     * instance.
     *
     * @param image the BufferedImage to interface
     */
    public BufferedSource (BufferedImage image)
    {
        this.image = image;
        raster = image.getRaster();

        ColorModel colorModel = image.getColorModel();
        hasAlpha = colorModel.hasAlpha();
        pixelArray = new int[4];
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public int get (int x,
                    int y)
    {
        raster.getPixel(x, y, pixelArray);

        if (hasAlpha) {
            int gray = pixelArray[0];
            int alpha = pixelArray[3];
            double a = alpha / 255d;
            double p = ((1 - a) * 255) + (a * gray);

            return clamp((int) (p + 0.5));
        } else {
            return pixelArray[0];
        }
    }

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

    private int clamp (int val)
    {
        if (val < 0) {
            return 0;
        }

        if (val > 255) {
            return 255;
        }

        return val;
    }
}
