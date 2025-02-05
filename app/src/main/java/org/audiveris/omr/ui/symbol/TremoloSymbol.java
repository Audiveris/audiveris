//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T r e m o l o S y m b o l                                   //
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
import static org.audiveris.omr.ui.symbol.Alignment.BOTTOM_CENTER;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>TremoloSymbol</code> implements a multiple-tremolo symbol.
 *
 * @author Hervé Bitteur
 */
public class TremoloSymbol
        extends DecorableSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a <code>TremoloSymbol</code> standard size with no decoration.
     *
     * @param shape  TREMOLO_1, TREMOLO_2, TREMOLO_3
     * @param family the musicFont family
     */
    public TremoloSymbol (Shape shape,
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
        final MyParams p = new MyParams();

        // Tremolo layout
        p.layout = font.layoutShapeByCode(shape);
        final Rectangle2D rTrem = p.layout.getBounds(); // Tremolo bounds

        if (isDecorated) {
            // Stem layout
            p.stemLayout = font.layoutShapeByCode(Shape.STEM);
            final Rectangle2D rStem = p.stemLayout.getBounds(); // Stem bounds (not centered)
            GeoUtil.translate2D(rStem, 0, rStem.getHeight() / 2); // Stem bounds centered

            p.rect = rTrem.createUnion(rStem);
        } else {
            p.rect = rTrem;
        }

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

        Point2D loc;

        if (isDecorated) {
            // Decorating stem
            loc = alignment.translatedPoint(BOTTOM_CENTER, p.rect, location);
            final Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, p.stemLayout, loc, BOTTOM_CENTER);
            g.setComposite(oldComposite);
        }

        // Tremolo
        loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends Params
    {
        // offset: if decorated, offset of symbol center vs decorated image center: null
        // layout: tremolo layout
        // rect:   global image (tremolo + stem if decorated, tremolo alone if not)
        //
        // Layout for decorating stem
        TextLayout stemLayout;
    }
}
