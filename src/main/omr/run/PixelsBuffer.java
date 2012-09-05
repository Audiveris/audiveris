//----------------------------------------------------------------------------//
//                                                                            //
//                          P i x e l s B u f f e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import net.jcip.annotations.ThreadSafe;

import java.awt.Dimension;
import java.util.Arrays;

/**
 * Class {@code PixelsBuffer} handles a plain rectangular buffer of 
 * chars.
 * It is an efficient {@link PixelSource} both for writing and for reading.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class PixelsBuffer
    implements PixelSource 
{
    //~ Instance fields --------------------------------------------------------

    /** Width of the table */
    private final int width;

    /** Height of the table */
    private final int height;

    /** Underlying buffer */
    private final char[] buffer;
    

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

        // Initialize the whole buffer with background color value
        Arrays.fill(buffer, (char) BACKGROUND);        
    }

    //~ Methods ----------------------------------------------------------------

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

    //----------//
    // setPixel //
    //----------//
    public void setPixel (int  x,
                          int  y,
                          char val)
    {
        buffer[(y * width) + x] = val;
    } 

    //--------//
    // isFore //
    //--------//
    @Override
    public boolean isFore (int x,
                           int y)
    {
        return getPixel(x,y) != BACKGROUND;
    }    
}
