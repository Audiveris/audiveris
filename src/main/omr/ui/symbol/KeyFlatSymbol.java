//----------------------------------------------------------------------------//
//                                                                            //
//                         K e y F l a t S y m b o l                          //
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
 * Class {@code KeyFlatSymbol} displays a Key Signature symbol.
 *
 * <p><img src="doc-files/KeySignatures.png" />
 *
 */
public class KeyFlatSymbol
        extends KeySymbol
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // KeyFlatSymbol //
    //---------------//
    /**
     * Creates a new KeyFlatSymbol object.
     *
     * @param key    the key value: -7..-1 for flats
     * @param isIcon true for an icon
     * @param shape  the related shape
     */
    public KeyFlatSymbol (int key,
                          boolean isIcon,
                          Shape shape)
    {
        super(key, isIcon, shape, 98);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new KeyFlatSymbol(key, true, shape);
    }
}
