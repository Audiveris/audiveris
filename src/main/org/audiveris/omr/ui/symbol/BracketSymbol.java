//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B r a c k e t S y m b o l                                    //
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
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code BracketSymbol} displays a BRACKET symbol: [
 *
 * @author Hervé Bitteur
 */
public class BracketSymbol
        extends ShapeSymbol
{

    // The upper serif
    private static final BasicSymbol upperSymbol = Symbols.SYMBOL_BRACKET_UPPER_SERIF;

    // The lower serif
    private static final BasicSymbol lowerSymbol = Symbols.SYMBOL_BRACKET_LOWER_SERIF;

    /**
     * Create a BracketSymbol (which is made of upper and lower parts)
     *
     * @param isIcon true for an icon
     */
    public BracketSymbol (boolean isIcon)
    {
        super(isIcon, Shape.BRACKET, false);
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new BracketSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        AffineTransform at = isIcon ? tiny : null;
        p.upperLayout = font.layout(upperSymbol.getString(), at);
        p.layout = font.layout(Shape.THICK_BARLINE, at);
        p.lowerLayout = font.layout(lowerSymbol.getString(), at);

        Rectangle2D upperRect = p.upperLayout.getBounds();
        Rectangle2D trunkRect = p.layout.getBounds();
        Rectangle2D lowerRect = p.lowerLayout.getBounds();
        p.rect = new Rectangle(
                (int) Math.ceil(upperRect.getWidth()),
                (int) Math.floor(
                        upperRect.getHeight() + trunkRect.getHeight() + lowerRect.getHeight()));

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
        MusicFont.paint(g, p.layout, loc, MIDDLE_LEFT);
        loc.y -= (p.rect.height / 2);
        MusicFont.paint(g, p.upperLayout, loc, TOP_LEFT);
        loc.y += (2 * (p.rect.height / 2));
        MusicFont.paint(g, p.lowerLayout, loc, BOTTOM_LEFT);
    }

    //--------//
    // Params //
    //--------//
    protected class MyParams
            extends Params
    {

        TextLayout upperLayout;

        TextLayout lowerLayout;
    }
}
