//----------------------------------------------------------------------------//
//                                                                            //
//                        K e y S h a r p S y m b o l                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.glyph.Shape;

/**
 * Class {@code KeySharpSymbol} displays a Key Signature symbol.
 *
 * <p><img src="doc-files/KeySignatures.png" />
 *
 */
public class KeySharpSymbol
        extends KeySymbol
{
    //~ Constructors -----------------------------------------------------------

    //----------------//
    // KeySharpSymbol //
    //----------------//
    /**
     * Creates a new KeySharpSymbol object.
     *
     * @param key    the key value: 1..7 for sharps
     * @param isIcon true for an icon
     * @param shape  the related shape
     */
    public KeySharpSymbol (int key,
                           boolean isIcon,
                           Shape shape)
    {
        super(key, isIcon, shape, 35);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new KeySharpSymbol(key, true, shape);
    }
}
