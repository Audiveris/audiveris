//----------------------------------------------------------------------------//
//                                                                            //
//                         B u f f e r e d S o u r c e                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

/**
 * Class {@code BufferedSource} wraps a BufferedImage as a PixelSource.
 *
 * @author Hervé Bitteur
 */
public class BufferedSource
        implements PixelSource
{
    //~ Instance fields --------------------------------------------------------

    /** The wrapped BufferedImage instance. */
    private final BufferedImage image;

    /** Image raster. */
    private final Raster raster;

    /** Buffer to read pixel value. */
    private final int[] pixelArray;

    //~ Constructors -----------------------------------------------------------
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
        pixelArray = new int[4];
    }

    //~ Methods ----------------------------------------------------------------
    @Override
    public int getHeight ()
    {
        return image.getHeight();
    }

    @Override
    public int getPixel (int x,
                         int y)
    {
        raster.getPixel(x, y, pixelArray);

        return pixelArray[0];
    }

    @Override
    public int getWidth ()
    {
        return image.getWidth();
    }
}
