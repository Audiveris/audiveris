//----------------------------------------------------------------------------//
//                                                                            //
//                         S y m b o l P i c t u r e                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.ui.icon;

import omr.lag.PixelSource;

import omr.sheet.Picture;

import omr.util.Implement;
import omr.util.Logger;

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

    /** Underlying SymbolIcon instance, used as the source of pixels */
    private final SymbolIcon icon;

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

    //~ Constructors -----------------------------------------------------------

    /** Creates a new instance of SymbolPicture */
    public SymbolPicture (SymbolIcon icon,
                          int        factor)
    {
        this.icon = icon;
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
        // Exactly 255 is the background (white) value, so anything else up to 
        // 254 included is foreground (black)
        return 254;
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
        // And with 255 as totally opaque so forground (0)
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
}
