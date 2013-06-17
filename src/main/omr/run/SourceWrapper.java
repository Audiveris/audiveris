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
package omr.run;

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
    protected final PixelSource source;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new SourceWrapper object.
     *
     * @param source DOCUMENT ME!
     */
    public SourceWrapper (PixelSource source)
    {
        this.source = source;
    }

    //~ Methods ----------------------------------------------------------------
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
