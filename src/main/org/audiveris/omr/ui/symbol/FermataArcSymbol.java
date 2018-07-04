//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 F e r m a t a A r c S y m b o l                                //
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
import static org.audiveris.omr.glyph.Shape.DOT_set;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.font.TextLayout;

/**
 * Class {@code FermataArcSymbol} implements Fermata Arc symbols with the related dot
 * as decoration.
 *
 * @author Hervé Bitteur
 */
public class FermataArcSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    // The DOT_set symbol
    private final ShapeSymbol dotSymbol = Symbols.getSymbol(DOT_set);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a FermataArcSymbol (with decoration?) standard size.
     *
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for rest part
     */
    public FermataArcSymbol (Shape shape,
                             boolean decorated,
                             int... codes)
    {
        this(false, shape, decorated, codes);
    }

    /**
     * Create a FermataArcSymbol (with decoration?).
     *
     * @param isIcon    true for an icon
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for rest part
     */
    protected FermataArcSymbol (boolean isIcon,
                                Shape shape,
                                boolean decorated,
                                int... codes)
    {
        super(isIcon, shape, decorated, codes);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new FermataArcSymbol(true, shape, decorated, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        // Full symbol (arc + dot)
        p.layout = font.layout(codes);
        p.rect = p.layout.getBounds().getBounds();

        // Dot layout
        p.dotLayout = font.layout(dotSymbol);

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
        // We paint the full fermata symbol first
        // Then we paint a dot using white or decoComposite
        MyParams p = (MyParams) params;
        Alignment align = (shape == Shape.FERMATA_ARC) ? BOTTOM_CENTER : TOP_CENTER;
        Point loc = alignment.translatedPoint(align, p.rect, location);

        MusicFont.paint(g, p.layout, loc, align); // Arc + Dot

        if (decorated) {
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
    protected class MyParams
            extends Params
    {
        //~ Instance fields ------------------------------------------------------------------------

        // layout for full fermata
        // rect for full fermata
        TextLayout dotLayout; // For the dot
    }
}
