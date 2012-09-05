//----------------------------------------------------------------------------//
//                                                                            //
//                     B i n a r y P i x e l S o u r c e                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2012. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import net.jcip.annotations.ThreadSafe;

/**
 * Class {@code BinaryPixelSource} implements Interface
 * {@code PixelSource} by using a global threshold on pixel value.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class BinaryPixelSource
        implements PixelSource
{
    //~ Instance fields --------------------------------------------------------
    //
    /** Underlying raw pixel source. */
    private final RawPixelSource source;

    /** Global threshold. */
    private final int maxForeground;

    //~ Constructors -----------------------------------------------------------
    //
    //-------------------//
    // BinaryPixelSource //
    //-------------------//
    /**
     * Create a binary wrapper on a raw pixel source.
     *
     * @param source the underlying source of raw pixels
     * @param maxForeground maximum gray level of foreground pixel
     */
    public BinaryPixelSource (RawPixelSource source,
                              int maxForeground)
    {
        this.source = source;
        this.maxForeground = maxForeground;
    }

    //~ Methods ----------------------------------------------------------------
    //
    // -------//
    // isFore //
    // -------//
    @Override
    public boolean isFore (int x,
                           int y)
    {
        return source.getPixel(x, y) <= maxForeground;
    }

    //-----------//
    // getHeight //
    //-----------//
    @Override
    public int getHeight ()
    {
        return source.getHeight();
    }

    //----------//
    // getPixel //
    //----------//
    @Override
    public int getPixel (int x,
                         int y)
    {
        return source.getPixel(x, y);
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public int getWidth ()
    {
        return source.getWidth();
    }
}
