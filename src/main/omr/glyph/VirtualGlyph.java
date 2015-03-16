//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    V i r t u a l G l y p h                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.math.GeoUtil;

import omr.ui.symbol.Symbols;

import java.awt.Point;

/**
 * Class {@code VirtualGlyph} is an artificial glyph specifically build from a
 * MusicFont-based symbol, to carry a shape and features just like a standard glyph
 * would.
 *
 * @author Hervé Bitteur
 */
public class VirtualGlyph
        extends SymbolGlyph
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new VirtualGlyph object
     *
     * @param shape     the assigned shape
     * @param interline the related interline scaling value
     * @param center    where the glyph area center will be located
     */
    public VirtualGlyph (Shape shape,
                         int interline,
                         Point center)
    {
        // Build a glyph of proper size
        super(shape, Symbols.getSymbol(shape), interline, GlyphLayer.DROP, null);

        // Translation from generic center to actual center
        translate(GeoUtil.vectorOf(getAreaCenter(), center));
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // isActive //
    //----------//
    /**
     * By definition a virtual glyph is always active
     *
     * @return true
     */
    @Override
    public boolean isActive ()
    {
        return true;
    }

    //-----------//
    // isVirtual //
    //-----------//
    @Override
    public boolean isVirtual ()
    {
        return true;
    }
}
