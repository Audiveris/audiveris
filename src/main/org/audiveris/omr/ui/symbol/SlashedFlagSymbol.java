//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S la s h e d F l a g S y m b o l                                //
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

import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;

import org.audiveris.omr.glyph.Shape;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class <code>SlashedFlagSymbol</code> displays a SMALL_FLAG_SLASH or SMALL_FLAG_SLASH_DOWN.
 *
 * @author Hervé Bitteur
 */
public class SlashedFlagSymbol
        extends ShapeSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SmallFlagSymbol</code> object.
     *
     * @param shape  the precise shape (SMALL_FLAG_SLASH or SMALL_FLAG_SLASH_DOWN)
     * @param family the MusicFont family
     */
    public SlashedFlagSymbol (Shape shape,
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

        final Shape flagShape = shape == Shape.SMALL_FLAG_SLASH ? Shape.FLAG_1 : Shape.FLAG_1_DOWN;
        p.layout = font.layoutShapeByCode(flagShape, OmrFont.TRANSFORM_SMALL);

        p.rect = p.layout.getBounds();
        p.stroke = new BasicStroke(Math.max(1f, (float) p.rect.getWidth() / 10f));

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
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);

        final boolean isUp = shape == Shape.SMALL_FLAG_SLASH;
        Stroke oldStroke = g.getStroke();
        g.setStroke(p.stroke);
        g.draw(
                isUp ? new Line2D.Double(
                        loc.getX() - (p.rect.getWidth() / 2),
                        loc.getY() + (p.rect.getHeight() / 5),
                        loc.getX() + (p.rect.getWidth() / 2),
                        loc.getY() - (p.rect.getHeight() / 5))
                        : new Line2D.Double(
                                loc.getX() - (p.rect.getWidth() / 2),
                                loc.getY() - (p.rect.getHeight() / 5),
                                loc.getX() + (p.rect.getWidth() / 2),
                                loc.getY() + (p.rect.getHeight() / 5)));
        g.setStroke(oldStroke);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends Params
    {

        Stroke stroke;
    }
}
