//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L e d g e r S y m b o l                                     //
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

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code LedgerSymbol} implements a decorated ledger symbol.
 *
 * @author Hervé Bitteur
 */
public class LedgerSymbol
        extends ShapeSymbol
{

    // The head part
    private static final BasicSymbol head = Symbols.getSymbol(Shape.NOTEHEAD_BLACK);

    /**
     * Create a LedgerSymbol (with decoration?) standard size
     *
     * @param decorated true for a decorated image
     */
    public LedgerSymbol (boolean decorated)
    {
        this(false, decorated);
    }

    /**
     * Create a LedgerSymbol (with decoration?)
     *
     * @param isIcon    true for an icon
     * @param decorated true for a decorated image
     */
    protected LedgerSymbol (boolean isIcon,
                            boolean decorated)
    {
        super(isIcon, Shape.LEDGER, decorated);
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new LedgerSymbol(true, true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();

        // Head layout
        p.layout = font.layout(head.getString());

        // Use a ledger length twice as large as note head
        Rectangle2D hRect = p.layout.getBounds();
        p.rect = new Rectangle(
                (int) Math.ceil(2 * hRect.getWidth()),
                (int) Math.ceil(hRect.getHeight()));

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
        Point loc = alignment.translatedPoint(AREA_CENTER, params.rect, location);

        if (decorated) {
            // Draw a note head (using composite)
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, params.layout, loc, AREA_CENTER);
            g.setComposite(oldComposite);
        }

        // Ledger
        g.drawLine(loc.x - (params.rect.width / 2), loc.y, loc.x + (params.rect.width / 2), loc.y);
    }
}
