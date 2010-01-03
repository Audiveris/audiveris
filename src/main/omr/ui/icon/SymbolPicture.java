//----------------------------------------------------------------------------//
//                                                                            //
//                         S y m b o l P i c t u r e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.icon;

import omr.lag.PixelSource;

import omr.log.Logger;

import omr.util.Implement;

import java.awt.image.DataBuffer;

/**
 * Class <code>SymbolPicture</code> is an adapter which wraps a SymbolIcon in
 * order to use it as a source of pixels.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SymbolPicture
    implements PixelSource
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolPicture.class);

    //~ Instance fields --------------------------------------------------------

    /** Image data buffer */
    private final DataBuffer dataBuffer;

    /** Scaling factor to apply to the icon before extracting pixels */
    private final int factor;

    /** Original image width */
    private final int originalWidth;

    /** Cached scaled width */
    private final int width;

    /** Cached scaled height */
    private final int height;

    /**
     * Current max foreground pixel value.
     * Exactly 255 is the background (white) value, so anything else up to
     * 254 included is foreground (black)
     */
    private int maxForeground = 254;

    //~ Constructors -----------------------------------------------------------

    /** Creates a new instance of SymbolPicture
     * @param icon the underlying icon
     * @param factor the sizing factor
     */
    public SymbolPicture (SymbolIcon icon,
                          int        factor)
    {
        this.factor = factor;

        dataBuffer = icon.getImage()
                         .getData()
                         .getDataBuffer();
        originalWidth = icon.getImage()
                            .getWidth();
        width = (int) factor * originalWidth;
        height = (int) factor * icon.getImage()
                                    .getHeight();
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getHeight //
    //-----------//
    @Implement(PixelSource.class)
    public final int getHeight ()
    {
        return height;
    }

    //------------------//
    // getMaxForeground //
    //------------------//
    @Implement(PixelSource.class)
    public final int getMaxForeground ()
    {
        return maxForeground;
    }

    //----------//
    // getPixel //
    //----------//
    @Implement(PixelSource.class)
    public final int getPixel (int x,
                               int y)
    {
        int index = (x / factor) + ((y / factor) * originalWidth);
        int elem = dataBuffer.getElem(index);

        // SymbolIcon instances use alpha channel as the pixel level
        // With 0 as totally transparent so background (255)
        // And with 255 as totally opaque so foreground (0)
        return 255 - (elem >>> 24);
    }

    //----------//
    // getWidth //
    //----------//
    @Implement(PixelSource.class)
    public final int getWidth ()
    {
        return width;
    }

    //------------------//
    // setMaxForeground //
    //------------------//
    @Implement(PixelSource.class)
    public void setMaxForeground (int level)
    {
        this.maxForeground = level;
    }
}
