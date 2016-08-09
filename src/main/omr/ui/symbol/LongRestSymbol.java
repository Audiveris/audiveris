//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  L o n g R e s t S y m b o l                                   //
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

import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;

/**
 * Class {@code LongRestSymbol} is a basis for WHOLE_REST and HALF_REST symbols.
 *
 * @author Hervé Bitteur
 */
public class LongRestSymbol
        extends RestSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a LongRestSymbol (with decoration?)
     *
     * @param decorated true for a decorated image
     */
    public LongRestSymbol (boolean decorated)
    {
        this(false, decorated);
    }

    /**
     * Create a LongRestSymbol (with decoration?)
     *
     * @param isIcon    true for an icon
     * @param decorated true for a decorated image
     */
    private LongRestSymbol (boolean isIcon,
                            boolean decorated)
    {
        super(isIcon, Shape.LONG_REST, decorated, 227);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new LongRestSymbol(true, decorated);
    }

    //---------------//
    // getRestLayout //
    //---------------//
    /**
     * Retrieve the layout of just the rest symbol part, w/o the lines
     * For this symbol, we need to integrate the AffineTransform effect
     */
    @Override
    protected TextLayout getRestLayout (MusicFont font)
    {
        TextLayout layout = super.getRestLayout(font);
        double restHeight = layout.getBounds().getHeight() * 1.03;
        AffineTransform at = new AffineTransform(1, 0, 0, 2, 0, restHeight);

        return font.layout(getString(), at);
    }
}
