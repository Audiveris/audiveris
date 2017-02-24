//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T e m p l a t e S y m b o l                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
import org.audiveris.omr.image.Template;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code TemplateSymbol} defines a symbol meant only for template matching.
 * <p>
 * <b>BEWARE:</b>Don't use it for simple display, use a ShapeSymbol of proper shape instead.
 *
 * @author Hervé Bitteur
 */
public class TemplateSymbol
        extends BasicSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Affine Transform for small symbol shapes. */
    private static final AffineTransform smallAt = AffineTransform.getScaleInstance(
            Template.smallRatio,
            Template.smallRatio);

    //~ Instance fields ----------------------------------------------------------------------------
    protected final Shape shape;

    protected final boolean isSmall;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TemplateSymbol object.
     *
     * @param shape shape for the template
     * @param codes the codes for MusicFont characters
     */
    public TemplateSymbol (Shape shape,
                           int... codes)
    {
        super(false, codes);
        this.shape = shape;
        isSmall = shape.isSmall();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // getSymbolBounds //
    //-----------------//
    public Rectangle getSymbolBounds (MusicFont font)
    {
        return getParams(font).symbolRect;
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        final MyParams p = new MyParams();
        final int interline = font.getFontInterline();

        final TextLayout fullLayout = layout(font);
        final Rectangle2D fullRect2d = fullLayout.getBounds();
        final Rectangle fullRect = fullRect2d.getBounds();

        if (isSmall) {
            p.layout = font.layout(getString(), smallAt);
        } else {
            p.layout = fullLayout;
        }

        final Rectangle2D r2d = p.layout.getBounds();
        final Rectangle r = r2d.getBounds();
        final int symWidth = r.width;
        final int symHeight = r.height;

        // Choose carefully the template rectangle, with origin at (0,0), around the symbol
        // For full size symbol, add a 1-pixel margin on each side of the symbol
        p.rect = new Rectangle(
                isSmall ? fullRect.width : (symWidth + 2),
                isSmall ? interline : (symHeight + 2));

        // Bounds of symbol within template rectangle
        p.symbolRect = new Rectangle(
                (int) Math.rint((fullRect2d.getWidth() + 2 - r2d.getWidth()) / 2),
                (int) Math.rint((fullRect2d.getHeight() + 2 - r2d.getHeight()) / 2),
                symWidth,
                symHeight);

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
        final MyParams p = (MyParams) params;

        // Background
        g.setColor(Color.RED);
        g.fill(p.rect);

        // Naked symbol
        g.setColor(Color.BLACK);

        Point loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ------------------------------------------------------------------------

        Rectangle symbolRect; // Bounds for symbol inside template image
    }
}
