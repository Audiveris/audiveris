//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    V i r t u a l G l y p h                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.glyph;

import java.awt.Point;

/**
 * Class {@code VirtualGlyph} is an artificial glyph specifically build from a
 * MusicFont-based symbol, to carry a shape and features just like a standard glyph
 * would.
 *
 * @author Hervé Bitteur
 */
public class VirtualGlyph
        extends SymbolSample
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
        super(shape, interline, null);
        throw new RuntimeException("HB. Not implemented yet");

        //        // Build a glyph of proper size
        //        super(shape, Symbols.getSymbol(shape), interline, Group.DROP, null);
        //
        //        // Translation from generic center to actual center
        //        translate(GeoUtil.vectorOf(getCenter(), center));
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // isVirtual //
    //-----------//
    @Override
    public boolean isVirtual ()
    {
        return true;
    }
}
