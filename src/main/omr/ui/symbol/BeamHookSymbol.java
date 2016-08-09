//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  B e a m H o o k S y m b o l                                   //
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
 * Class {@code BeamHookSymbol} implements a decorated beam hook symbol
 *
 * @author Hervé Bitteur
 */
public class BeamHookSymbol
        extends BeamSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a BeamHookSymbol
     */
    public BeamHookSymbol ()
    {
        this(false);
    }

    /**
     * Create a BeamHookSymbol
     *
     * @param isIcon true for an icon
     */
    protected BeamHookSymbol (boolean isIcon)
    {
        super(1, isIcon, Shape.BEAM_HOOK);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new BeamHookSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = super.getParams(font);

        p.rect.width = (int) Math.rint(p.rect.width * 0.33);
        p.rect.height = (int) Math.ceil(p.layout.getBounds().getHeight());

        return p;
    }
}
