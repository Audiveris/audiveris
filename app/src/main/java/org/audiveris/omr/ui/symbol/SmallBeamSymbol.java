//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S m a l l B e a m S y m b o l                                 //
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
import org.audiveris.omr.sheet.beam.BeamsBuilder;

/**
 * Class <code>SmallBeamSymbol</code> implements a decorated small beam symbol.
 *
 * @author Hervé Bitteur
 */
public class SmallBeamSymbol
        extends BeamSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a SmallBeamSymbol.
     *
     * @param family the musicFont family
     */
    public SmallBeamSymbol (MusicFamily family)
    {
        super(Shape.BEAM_SMALL, family);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        // Apply size ratio for small beam
        final float smallFontSize = (float) BeamsBuilder.getCueBeamRatio() * font.getSize2D();
        final MusicFont smallFont = font.deriveFont(smallFontSize);

        return super.getParams(smallFont);
    }
}
