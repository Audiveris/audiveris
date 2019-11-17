//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R e s t S y m b o l                                       //
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
import static org.audiveris.omr.glyph.Shape.*;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code RestSymbol} implements rest symbols whose decoration uses staff lines
 * as background.
 *
 * @author Hervé Bitteur
 */
public class RestSymbol
        extends ShapeSymbol
{

    /** Staff lines symbol. */
    protected static final BasicSymbol linesSymbol = Symbols.SYMBOL_STAFF_LINES;

    /**
     * Create a RestSymbol (with decoration?) standard size.
     *
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for rest part
     */
    public RestSymbol (Shape shape,
                       boolean decorated,
                       int... codes)
    {
        this(false, shape, decorated, codes);
    }

    /**
     * Create a RestSymbol (with decoration?).
     *
     * @param isIcon    true for an icon
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for rest part
     */
    protected RestSymbol (boolean isIcon,
                          Shape shape,
                          boolean decorated,
                          int... codes)
    {
        super(isIcon, shape, decorated, codes);
    }

    //-----------------------//
    // createDecoratedSymbol //
    //-----------------------//
    @Override
    protected ShapeSymbol createDecoratedSymbol ()
    {
        if ((shape == BREVE_REST) || (shape == WHOLE_REST) || (shape == HALF_REST)) {
            return new RestSymbol(isIcon, shape, true, codes);
        }

        return this; // No decoration
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new RestSymbol(true, shape, decorated, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        // Rest symbol layout
        p.layout = getRestLayout(font);

        Rectangle2D rs = p.layout.getBounds();

        if (decorated) {
            // Define specific offset
            p.offset = new Point2D.Double(0, rs.getY() + (rs.getHeight() / 2));

            // Lines layout
            p.linesLayout = font.layout(linesSymbol.getString());
            p.rect = p.linesLayout.getBounds();
        } else {
            p.rect = rs;
        }

        return p;
    }

    //---------------//
    // getRestLayout //
    //---------------//
    /**
     * Retrieve the layout of just the rest symbol part, w/o the lines.
     *
     * @param font the font to extract the layout from
     * @return text layout for rest symbols
     */
    protected TextLayout getRestLayout (MusicFont font)
    {
        return font.layout(getString());
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
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, p.linesLayout, loc, AREA_CENTER);
            g.setComposite(oldComposite);

            MusicFont.paint(g, p.layout, loc, BASELINE_CENTER);
        } else {
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
        }
    }

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends Params
    {

        // offset: if decorated, offset of symbol center vs decorated image center
        // layout: rest layout
        // rect:   global image (=lines if decorated, rest if not)
        //
        // Layout for lines
        TextLayout linesLayout;
    }
}
