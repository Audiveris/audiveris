//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A r p e g g i a t o s S y m b o l                               //
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
import static org.audiveris.omr.ui.symbol.Alignment.TOP_LEFT;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code ArpeggiatosSymbol} displays a column of several arpeggiato.
 * Class is not used.
 *
 * @author Hervé Bitteur
 */
public class ArpeggiatosSymbol
        extends ShapeSymbol
{

    private final int count;

    /**
     * Creates a new ArpeggiatosSymbol object.
     *
     * @param count  the number of arpeggiato
     * @param isIcon true for an icon
     */
    public ArpeggiatosSymbol (int count,
                              boolean isIcon)
    {
        super(isIcon, Shape.ARPEGGIATO, false, 103);
        this.count = count;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new ArpeggiatosSymbol(count, true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();
        p.layout = layout(font);

        Rectangle2D r = p.layout.getBounds();
        p.rect = new Rectangle(
                (int) Math.ceil(r.getWidth()),
                count * (int) Math.rint(r.getHeight()));

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
        Params p = params;
        Point loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
        int dy = (int) Math.rint(p.layout.getBounds().getHeight());

        for (int i = 0; i < count; i++) {
            MusicFont.paint(g, p.layout, loc, TOP_LEFT);
            loc.y += dy;
        }
    }
}
