//----------------------------------------------------------------------------//
//                                                                            //
//                          P i x e l s B u f f e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.run.GlobalDescriptor;

import net.jcip.annotations.ThreadSafe;

import java.awt.Dimension;
import java.util.Arrays;

/**
 * Class {@code PixelsBuffer} handles a plain rectangular buffer of
 * chars.
 * It is an efficient {@link PixelFilter} both for writing and for reading.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class PixelsBuffer
        implements PixelFilter
{
    //~ Instance fields --------------------------------------------------------

    /** Width of the table */
    private final int width;

    /** Height of the table */
    private final int height;

    /** Underlying buffer */
    private char[] buffer;

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

        buffer = new char[width * height];

        // Initialize the whole buffer with background color value
        Arrays.fill(buffer, (char) BACKGROUND);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // getContext //
    //------------//
    @Override
    public Context getContext (int x,
                               int y)
    {
        return new Context(BACKGROUND / 2);
    }

    //-----------//
    // getHeight //
    //-----------//
    @Override
    public int getHeight ()
    {
        return height;
    }

    //----------//
    // getPixel //
    //----------//
    @Override
    public int getPixel (int x,
                         int y)
    {
        return buffer[(y * width) + x];
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public int getWidth ()
    {
        return width;
    }

    //--------//
    // isFore //
    //--------//
    @Override
    public boolean isFore (int x,
                           int y)
    {
        return getPixel(x, y) != BACKGROUND;
    }

    //----------//
    // setPixel //
    //----------//
    public void setPixel (int x,
                          int y,
                          char val)
    {
        buffer[(y * width) + x] = val;
    }
}
