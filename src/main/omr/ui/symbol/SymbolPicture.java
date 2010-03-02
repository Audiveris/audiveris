//----------------------------------------------------------------------------//
//                                                                            //
//                         S y m b o l P i c t u r e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.lag.PixelSource;

import omr.log.Logger;

import omr.score.ui.ScoreConstants;

import omr.util.Implement;

import java.awt.image.DataBuffer;

/**
 * Class <code>SymbolPicture</code> is an adapter which wraps a ShapeSymbol in
 * order to use it as a source of pixels.
 *
 * @author Hervé Bitteur
 */
public class SymbolPicture
    implements PixelSource
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolPicture.class);

    //~ Instance fields --------------------------------------------------------

    /** The related symbol */
    private final ShapeSymbol symbol;

    /** Image data buffer */
    private final DataBuffer dataBuffer;

    /** Scaling factor to apply to the symbol before extracting pixels */
    private final double factor;

    /** Actual image width */
    private final int actualWidth;

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
     * @param symbol the underlying symbol
     */
    public SymbolPicture (ShapeSymbol symbol)
    {
        this.symbol = symbol;

        factor = (double) ScoreConstants.INTER_LINE / symbol.getInterline();
        dataBuffer = symbol.getImage()
                           .getData()
                           .getDataBuffer();
        actualWidth = symbol.getImage()
                            .getWidth();
        width = (int) Math.rint(factor * actualWidth);
        height = (int) Math.rint(factor * symbol.getImage().getHeight());
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
        int index = (int) Math.rint((x + (y * actualWidth)) / factor);
        int elem = dataBuffer.getElem(index);

        // ShapeSymbol instances use alpha channel as the pixel level
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
