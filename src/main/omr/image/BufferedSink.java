//----------------------------------------------------------------------------//
//                                                                            //
//                           B u f f e r e d S i n k                          //
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
import java.awt.image.WritableRaster;

/**
 * Class {@code BufferedSink} wraps a BufferedImage as a PixelSink.
 *
 * @author Hervé Bitteur
 */
public class BufferedSink
        implements PixelSink
{
    //~ Instance fields --------------------------------------------------------

    /** The wrapped BufferedImage instance. */
    private final BufferedImage image;

    /** Image raster. */
    private final WritableRaster raster;

    /** Buffer to write pixel value. */
    private final int[] pixelArray;

    //~ Constructors -----------------------------------------------------------
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

    //~ Methods ----------------------------------------------------------------
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
    public void setPixel (int x,
                          int y,
                          int val)
    {
        pixelArray[0] = val;
        raster.setPixel(x, y, pixelArray);
    }
}
