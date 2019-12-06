//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    E v e n S t e p S y m b o l                                 //
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
import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;
import static org.audiveris.omr.ui.symbol.Alignment.MIDDLE_LEFT;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code EvenStepSymbol} is the basis for symbols located on an even step position.
 * There is a ledger in the middle of the symbol.
 *
 * @author Hervé Bitteur
 */
public class EvenStepSymbol
        extends ShapeSymbol
{

    /**
     * Creates a new EvenStepSymbol object.
     *
     * @param shape the underlying shape
     * @param codes the codes for MusicFont characters
     */
    public EvenStepSymbol (Shape shape,
                           int... codes)
    {
        super(shape, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();
        p.layout = layout(font);
        p.rect = p.layout.getBounds();

        int interline = font.getStaffInterline();
        p.line = Math.max(1, interline * 0.17);

        return p;
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params params,
                          Point2D location,
                          Alignment alignment)
    {
        MyParams p = (MyParams) params;

        // Paint naked symbol
        Point2D loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);

        // Paint ledger at middle position
        loc = alignment.translatedPoint(MIDDLE_LEFT, p.rect, location);
        ///g.fillRect(loc.x, loc.y - (p.line / 2), p.rect.width - 1, p.line);
        g.fill(new Rectangle2D.Double(loc.getX(),
                                      loc.getY() - (p.line / 2),
                                      p.rect.getWidth() - 1,
                                      p.line));
    }

    //----------//
    // MyParams //
    //----------//
    protected static class MyParams
            extends Params
    {

        double line; // Thickness of a ledger or staff line
    }
}
