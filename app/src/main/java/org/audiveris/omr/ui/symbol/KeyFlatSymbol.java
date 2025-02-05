//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   K e y F l a t S y m b o l                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

/**
 * Class <code>KeyFlatSymbol</code> displays a Key Signature symbol.
 *
 * @author Hervé Bitteur
 */
public class KeyFlatSymbol
        extends KeySymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new KeyFlatSymbol object.
     *
     * @param key    the key value: -7..-1 for flats
     * @param shape  the related shape
     * @param family the selected MusicFont family
     */
    public KeyFlatSymbol (int key,
                          Shape shape,
                          MusicFamily family)
    {
        super(key, shape, family, Shape.FLAT);
    }
}
