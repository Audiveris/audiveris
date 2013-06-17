//----------------------------------------------------------------------------//
//                                                                            //
//                         S y m b o l P i c t u r e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.run.PixelFilter;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

/**
 * Class {@code SymbolPicture} is an adapter which wraps a ShapeSymbol
 * in order to use it as a source of pixels.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class SymbolPicture
        implements PixelFilter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SymbolPicture.class);

    //~ Instance fields --------------------------------------------------------
    /** Image data buffer */
    private final DataBuffer dataBuffer;

    /** Cached scaled width */
    private final int width;

    /** Cached scaled height */
    private final int height;

    /**
     * Current max foreground pixel value.
     * Anything from 0 (black) up to 192 included (light gray) is considered as
     * black
     * Anything brighter than gray is considered as white (background)
     */
    private final int maxForeground = 216; // Was Color.LIGHT_GRAY.getRed();

    //~ Constructors -----------------------------------------------------------
    /** Creates a new instance of SymbolPicture
     *
     * @param image the underlying image
     */
    public SymbolPicture (BufferedImage image)
    {
        dataBuffer = image.getData()
                .getDataBuffer();

        width = image.getWidth();
        height = image.getHeight();
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // getContext //
    //------------//
    @Override
    public Context getContext (int x,
                               int y)
    {
        return new Context(maxForeground);
    }

    //-----------//
    // getHeight //
    //-----------//
    @Override
    public final int getHeight ()
    {
        return height;
    }

    //----------//
    // getPixel //
    //----------//
    @Override
    public final int getPixel (int x,
                               int y)
    {
        int index = x + (y * width);
        int elem = dataBuffer.getElem(index);

        // ShapeSymbol instances use alpha channel as the pixel level
        // With 0 as totally transparent so background (255)
        // And with 255 as totally opaque so foreground (0)
        return 255 - (elem >>> 24);
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public final int getWidth ()
    {
        return width;
    }

    //
    // -------//
    // isFore //
    // -------//
    @Override
    public boolean isFore (int x,
                           int y)
    {
        return getPixel(x, y) <= maxForeground;
    }
}
