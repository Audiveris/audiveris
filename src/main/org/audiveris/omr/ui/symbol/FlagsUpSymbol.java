//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   F l a g s U p S y m b o l                                    //
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
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.ui.symbol.Alignment.*;
import org.audiveris.omr.ui.symbol.FlagsDownSymbol.MyParams;

/**
 * Class {@code FlagsUpSymbol} displays a pack of several flags up
 *
 * @author Hervé Bitteur
 */
public class FlagsUpSymbol
        extends FlagsDownSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new FlagsUpSymbol object.
     *
     * @param flagCount the number of flags
     * @param isIcon    true for an icon
     * @param shape     the related shape
     */
    public FlagsUpSymbol (int flagCount,
                          boolean isIcon,
                          Shape shape)
    {
        super(flagCount, isIcon, shape);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new FlagsUpSymbol(fn, true, shape);
    }

    //------------//
    // initParams //
    //------------//
    @Override
    protected MyParams initParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.flag1 = Symbols.SYMBOL_FLAG_1_UP.layout(font);
        p.rect1 = p.flag1.getBounds();
        p.flag2 = Symbols.SYMBOL_FLAG_2_UP.layout(font);
        p.rect2 = p.flag2.getBounds();
        p.dy = -(int) Math.rint(p.rect2.getHeight() * 0.5);
        p.align = BOTTOM_LEFT;

        return p;
    }
}
