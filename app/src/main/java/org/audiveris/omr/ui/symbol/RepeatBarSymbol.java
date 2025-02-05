//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  R e p e a t B a r S y m b o l                                 //
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

import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;

import org.audiveris.omr.glyph.Shape;

import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>RepeatBarSymbol</code> defines symbol for shapes REPEAT_TWO_BARS and
 * REPEAT_FOUR_BARS that are always located over a barline.
 * <p>
 * The glyph classifier thus needs to take this barline into account because it appears as part of
 * the glyph.
 * <p>
 * The REPEAT_ONE_BAR shape is not concerned because it lies in the middle of a measure.
 *
 * @author Hervé Bitteur
 */
public class RepeatBarSymbol
        extends ShapeSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a <code>RepeatBarSymbol</code> object.
     *
     * @param shape  REPEAT_TWO_BARS, REPEAT_FOUR_BARS
     * @param family the selected MusicFont family
     */
    public RepeatBarSymbol (Shape shape,
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
        p.layout = font.layoutShapeByCode(shape);
        p.barlineLayout = font.layoutShapeByCode(Shape.THIN_BARLINE);

        Rectangle2D repeatRect = p.layout.getBounds();
        Rectangle2D barlineRect = p.barlineLayout.getBounds();
        p.rect = new Rectangle2D.Double(
                0,
                0,
                Math.max(repeatRect.getWidth(), barlineRect.getWidth()),
                Math.max(repeatRect.getHeight(), barlineRect.getHeight()));

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
        final Point2D center = alignment.translatedPoint(AREA_CENTER, p.rect, location);

        OmrFont.paint(g, p.layout, center, AREA_CENTER);
        OmrFont.paint(g, p.barlineLayout, center, AREA_CENTER);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // MyParams //
    //----------//
    protected static class MyParams
            extends Params
    {
        // offset not used
        // layout for repeat symbol
        // rect for (repeat + barline) bounds
        //
        TextLayout barlineLayout; // For barline
    }
}
