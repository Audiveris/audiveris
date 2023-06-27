//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 F e r m a t a A r c S y m b o l                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
import static org.audiveris.omr.ui.symbol.Alignment.BOTTOM_CENTER;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_CENTER;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;

/**
 * Class <code>FermataArcSymbol</code> implements Fermata Arc symbols with the related dot
 * as decoration.
 * <p>
 * Purpose of this symbol is to allow the glyph classifier to recognize fermata arc and fermata dot
 * as two separate entities (they lie too far apart for the glyph classifier to be considered as
 * a single symbol candidate).
 *
 * @author Hervé Bitteur
 */
public class FermataArcSymbol
        extends DecorableSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a FermataArcSymbol standard size with no decoration.
     *
     * @param shape  the precise shape
     * @param family the musicFont family
     */
    public FermataArcSymbol (Shape shape,
                             MusicFamily family)
    {
        super(shape, family);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        // Full symbol (arc + dot)
        p.layout = font.layoutShapeByCode(shape);
        p.rect = p.layout.getBounds();

        // Dot layout
        p.dotLayout = font.layoutShapeByCode(Shape.DOT_set);

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
        // We paint the full fermata symbol first
        // Then we paint a dot using white or decoComposite
        MyParams p = (MyParams) params;
        Alignment align = (shape == Shape.FERMATA_ARC) ? BOTTOM_CENTER : TOP_CENTER;
        Point2D loc = alignment.translatedPoint(align, p.rect, location);

        MusicFont.paint(g, p.layout, loc, align); // Arc + Dot

        if (isDecorated) {
            // Paint dot in gray
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, p.dotLayout, loc, align);
            g.setComposite(oldComposite);
        } else {
            // Erase dot using white color (?)
            Color oldColor = g.getColor();
            g.setColor(Color.WHITE);
            MusicFont.paint(g, p.dotLayout, loc, align);
            g.setColor(oldColor);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends Params
    {

        // layout for full fermata
        // rect for full fermata
        TextLayout dotLayout; // For the dot
    }
}
