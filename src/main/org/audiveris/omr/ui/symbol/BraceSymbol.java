//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B r a c e S y m b o l                                      //
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

import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code BraceSymbol} displays a BRACE symbol: {
 *
 * @author Hervé Bitteur
 */
public class BraceSymbol
        extends ShapeSymbol
{

    // The upper part symbol
    private static final BasicSymbol upperSymbol = Symbols.SYMBOL_BRACE_UPPER_HALF;

    // The lower part symbol
    private static final BasicSymbol lowerSymbol = Symbols.SYMBOL_BRACE_LOWER_HALF;

    /**
     * Create a BraceSymbol (which is made of upper and lower parts).
     */
    public BraceSymbol ()
    {
        this(false);
    }

    /**
     * Create a BraceSymbol (which is made of upper and lower parts)
     *
     * @param isIcon true for an icon
     */
    protected BraceSymbol (boolean isIcon)
    {
        super(isIcon, Shape.BRACE, false);
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new BraceSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        AffineTransform at = isIcon ? tiny : null;
        p.upperLayout = font.layout(upperSymbol.getString(), at);
        p.lowerLayout = font.layout(lowerSymbol.getString(), at);

        Rectangle2D r = p.upperLayout.getBounds();
        p.rect = new Rectangle2D.Double(0, 0, r.getWidth(), 2 * r.getHeight());

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
        Point2D loc = alignment.translatedPoint(MIDDLE_LEFT, p.rect, location);
        MusicFont.paint(g, p.upperLayout, loc, BOTTOM_LEFT);
        MusicFont.paint(g, p.lowerLayout, loc, TOP_LEFT);
    }

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends Params
    {

        TextLayout upperLayout;

        TextLayout lowerLayout;
    }
}
