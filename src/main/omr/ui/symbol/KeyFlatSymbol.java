//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   K e y F l a t S y m b o l                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.ui.symbol;

import omr.glyph.Shape;

/**
 * Class {@code KeyFlatSymbol} displays a Key Signature symbol.
 *
 * <p>
 * <img src="doc-files/KeySignatures.png">
 *
 */
public class KeyFlatSymbol
        extends KeySymbol
{
    //~ Constructors -------------------------------------------------------------------------------

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

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new KeyFlatSymbol(key, true, shape);
    }
}
