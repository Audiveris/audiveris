//----------------------------------------------------------------------------//
//                                                                            //
//                          P i x e l s B u f f e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.lag.PixelSource;

import omr.util.Implement;

import java.awt.Dimension;
import java.util.Arrays;

/**
 * Class {@code PixelsBuffer} handles a plain rectangular buffer of chars.
 * It is an efficient {@link PixelSource} both for writing and for reading.
 *
 * @author Hervé Bitteur
 */
public class PixelsBuffer
    implements PixelSource
{
    //~ Instance fields --------------------------------------------------------

    /** Width of the table */
    private final int width;

    /** Hheight of the table */
    private final int height;

    /** Buffer */
    final char[] buffer;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // PixelsBuffer //
    //--------------//
    /**
     * Creates a new PixelsBuffer object.
     *
     * @param dimension the buffer dimension
     */
    public PixelsBuffer (Dimension dimension)
    {
        width = dimension.width;
        height = dimension.height;
        buffer = new char[dimension.width * dimension.height];
        Arrays.fill(buffer, (char) BACKGROUND);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getHeight //
    //-----------//
    @Implement(PixelSource.class)
    public int getHeight ()
    {
        return height;
    }

    public void setMaxForeground (int level)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getMaxForeground ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //----------//
    // setPixel //
    //----------//
    public void setPixel (int  x,
                          int  y,
                          char val)
    {
        buffer[(y * width) + x] = val;
    }

    //----------//
    // getPixel //
    //----------//
    @Implement(PixelSource.class)
    public int getPixel (int x,
                         int y)
    {
        return buffer[(y * width) + x];
    }

    //----------//
    // getWidth //
    //----------//
    @Implement(PixelSource.class)
    public int getWidth ()
    {
        return width;
    }
}
