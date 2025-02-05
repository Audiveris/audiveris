//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L e d g e r S y m b o l                                     //
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
import org.audiveris.omr.sig.inter.LedgerInter;
import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>LedgerSymbol</code> implements a ledger symbol, perhaps decorated.
 *
 * @author Hervé Bitteur
 */
public class LedgerSymbol
        extends DecorableSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a LedgerSymbol (with decoration?) standard size
     *
     * @param family the musicFont family
     */
    public LedgerSymbol (MusicFamily family)
    {
        super(Shape.LEDGER, family);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // getModel //
    //----------//
    @Override
    public LedgerInter.Model getModel (MusicFont font,
                                       Point location)
    {
        final MyParams p = getParams(font);
        final double width = p.rect.getWidth();

        return new LedgerInter.Model(
                location.x - width / 2,
                location.y,
                location.x + width / 2,
                location.y);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        final MyParams p = new MyParams();
        p.layout = font.layoutShapeByCode(shape);
        p.rect = p.layout.getBounds();

        if (isDecorated) {
            // Add head layout
            p.headLayout = font.layoutShapeByCode(Shape.NOTEHEAD_BLACK);
            final Rectangle2D hr = p.headLayout.getBounds();
            Rectangle2D.union(p.rect, hr, p.rect);
        }

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
        final MyParams p = (MyParams) params;
        final Point2D loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);

        if (isDecorated) {
            // Draw a note head, using composite
            final Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, p.headLayout, loc, AREA_CENTER);
            g.setComposite(oldComposite);
        }

        // Ledger itself
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends ShapeSymbol.Params
    {
        // offset: not used
        // layout: ledger
        // rect:   global image (ledger + head?)
        //
        // Layout for head decoration
        TextLayout headLayout;
    }
}
