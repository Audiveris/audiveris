//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  D e c o r a b l e S y m b o l                                 //
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
 * Class <code>DecorableSymbol</code> is a {@link ShapeSymbol} which can provide a decorated
 * version (the symbol within decoration).
 *
 * @author Hervé Bitteur
 */
public class DecorableSymbol
        extends ShapeSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a DecorableSymbol with the provided shape and codes.
     *
     * @param shape  the related shape
     * @param family the MusicFont family
     */
    public DecorableSymbol (Shape shape,
                            MusicFamily family)
    {
        super(shape, family);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------------//
    // supportsDecoration //
    //--------------------//
    @Override
    protected boolean supportsDecoration ()
    {
        return true;
    }
}
