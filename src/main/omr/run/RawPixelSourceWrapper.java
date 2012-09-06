//----------------------------------------------------------------------------//
//                                                                            //
//                  R a w P i x e l S o u r c e W r a p p e r                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

/**
 * Class {@code RawPixelSourceWrapper} wraps a RawPixelSource.
 *
 * @author Hervé Bitteur
 */
public class RawPixelSourceWrapper
        implements RawPixelSource
{
    //~ Instance fields --------------------------------------------------------

    /** Underlying raw pixel source. */
    protected final RawPixelSource source;

    //~ Constructors -----------------------------------------------------------
    //
    //-----------------------//
    // RawPixelSourceWrapper //
    //-----------------------//
    public RawPixelSourceWrapper (RawPixelSource source)
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
