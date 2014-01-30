//----------------------------------------------------------------------------//
//                                                                            //
//                          S o u r c e W r a p p e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import ij.process.ByteProcessor;

/**
 * Class {@code SourceWrapper} wraps a PixelSource.
 *
 * @author Hervé Bitteur
 */
public class SourceWrapper
        implements PixelSource
{
    //~ Instance fields --------------------------------------------------------

    /** Underlying pixel source. */
    protected final ByteProcessor source;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new SourceWrapper object.
     *
     * @param source the pixel source
     */
    public SourceWrapper (ByteProcessor source)
    {
        this.source = source;
    }

    //~ Methods ----------------------------------------------------------------
    //-----//
    // get //
    //-----//
    @Override
    public int get (int x,
                    int y)
    {
        return source.get(x, y);
    }

    //
    //-----------//
    // getHeight //
    //-----------//
    @Override
    public int getHeight ()
    {
        return source.getHeight();
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
