//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    E n d i n g S y m b o l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code EndingSymbol} implements a decorated ending symbol
 *
 * @author Hervé Bitteur
 */
public class EndingSymbol
        extends ShapeSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create an EndingSymbol
     */
    public EndingSymbol ()
    {
        this(false);
    }

    /**
     * Create an EndingSymbol
     *
     * @param isIcon true for an icon
     */
    protected EndingSymbol (boolean isIcon)
    {
        super(isIcon, Shape.ENDING, true); // Decorated
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new EndingSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();
        int il = font.getStaffInterline();
        p.rect = new Rectangle(4 * il, il);

        return p;
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params p,
                          Point location,
                          Alignment alignment)
    {
        Point loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);

        g.drawLine(loc.x, loc.y, (loc.x + p.rect.width) - 1, loc.y);
        g.drawLine(loc.x, loc.y, loc.x, loc.y + p.rect.height);
    }
}
