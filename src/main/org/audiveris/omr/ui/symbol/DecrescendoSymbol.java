//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               D e c r e s c e n d o S y m b o l                                //
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

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;

/**
 * Class {@code DecrescendoSymbol} displays a decrescendo symbol: {@literal ">"}.
 */
public class DecrescendoSymbol
        extends CrescendoSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new DecrescendoSymbol object.
     *
     * @param isIcon true for an icon
     * @param shape  the related shape
     */
    public DecrescendoSymbol (boolean isIcon,
                              Shape shape)
    {
        super(isIcon, shape);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new DecrescendoSymbol(true, shape);
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params params,
                          Point location,
                          Alignment alignment)
    {
        MyParams p = (MyParams) params;
        Point loc = alignment.translatedPoint(MIDDLE_RIGHT, p.rect, location);

        Stroke oldStroke = g.getStroke();
        g.setStroke(p.stroke);
        g.drawLine(loc.x, loc.y, loc.x - p.rect.width, loc.y - (p.rect.height / 2));
        g.drawLine(loc.x, loc.y, loc.x - p.rect.width, loc.y + (p.rect.height / 2));
        g.setStroke(oldStroke);
    }
}
