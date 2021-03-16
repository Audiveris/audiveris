//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L e d g e r S y m b o l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sig.inter.LedgerInter;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code LedgerSymbol} implements a ledger symbol.
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

    //----------//
    // getModel //
    //----------//
    @Override
    public LedgerInter.Model getModel (MusicFont font,
                                       Point location,
                                       Alignment alignment)
    {
        MyParams p = getParams(font);
        Point2D loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
        p.model.translate(loc.getX(), loc.getY());

        return p.model;
    }

    //-----------------------//
    // createDecoratedSymbol //
    //-----------------------//
    @Override
    protected ShapeSymbol createDecoratedSymbol ()
    {
        return new LedgerSymbol(isIcon, true);
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new LedgerSymbol(true, decorated);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        // Typical ledger length
        final int width = (int) Math.ceil(
                font.getStaffInterline() * LedgerInter.getDefaultLength().getValue());
        p.thickness = (int) Math.ceil(LedgerInter.DEFAULT_THICKNESS);

        if (decorated) {
            // Head layout
            p.layout = font.layout(head.getString());

            p.rect = new Rectangle2D.Double(0, 0, width, p.layout.getBounds().getHeight());
        } else {
            p.rect = new Rectangle2D.Double(0, 0, width, p.thickness);
        }

        final double y = p.rect.getHeight() / 2.0;
        p.model = new LedgerInter.Model(0, y, width, y);

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

        Point2D loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);

        if (decorated) {
            // Draw a note head (using composite)
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
            g.setComposite(oldComposite);
        }

        // Ledger itself
        PointUtil.add(loc, -p.rect.getWidth() / 2, 0);
        p.model.translate(loc.getX(), 0);
        g.draw(new Line2D.Double(p.model.p1, p.model.p2));
    }

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends BasicSymbol.Params
    {

        // offset: not used
        // layout: head decoration
        // rect:   global image
        //
        // ledger thickness
        int thickness;

        // model
        LedgerInter.Model model;
    }
}
