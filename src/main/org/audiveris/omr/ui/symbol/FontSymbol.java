//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       F o n t S y m b o l                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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

/**
 * Class <code>FontSymbol</code> handles a couple made of a font and a symbol,
 * because often a symbol geometry depends on the underlying font.
 *
 * @author Hervé Bitteur
 */
public class FontSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    public final MusicFont font;

    public final ShapeSymbol symbol;

    //~ Constructors -------------------------------------------------------------------------------
    public FontSymbol (MusicFont font,
                       ShapeSymbol symbol)
    {
        this.font = font;
        this.symbol = symbol;
    }
}
