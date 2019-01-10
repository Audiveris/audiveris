//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 C r e s c e n d o S y m b o l                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;

/**
 * Class {@code CrescendoSymbol} displays a crescendo symbol: {@literal "<"}.
 *
 * @author Hervé Bitteur
 */
public class CrescendoSymbol
        extends ShapeSymbol
{

    /**
     * Creates a new CrescendoSymbol object.
     *
     * @param isIcon true for an icon
     * @param shape  the related shape
     */
    public CrescendoSymbol (boolean isIcon,
                            Shape shape)
    {
        super(isIcon, shape, false);
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new CrescendoSymbol(true, shape);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        double interline = font.getStaffInterline();
        p.rect = new Rectangle((int) Math.ceil(5 * interline), (int) Math.ceil(1.5 * interline));
        p.stroke = new BasicStroke(
                Math.max(1f, (float) interline / 7f),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);

        return p;
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
        Point loc = alignment.translatedPoint(MIDDLE_LEFT, p.rect, location);

        Stroke oldStroke = g.getStroke();
        g.setStroke(p.stroke);
        g.drawLine(loc.x, loc.y, loc.x + p.rect.width, loc.y - (p.rect.height / 2));
        g.drawLine(loc.x, loc.y, loc.x + p.rect.width, loc.y + (p.rect.height / 2));
        g.setStroke(oldStroke);
    }

    //--------//
    // Params //
    //--------//
    protected class MyParams
            extends Params
    {

        Stroke stroke;
    }
}
