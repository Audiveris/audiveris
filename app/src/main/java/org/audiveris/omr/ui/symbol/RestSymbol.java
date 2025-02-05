//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R e s t S y m b o l                                       //
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

import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.BREVE_REST;
import static org.audiveris.omr.glyph.Shape.HALF_REST;
import static org.audiveris.omr.glyph.Shape.LONG_REST;
import static org.audiveris.omr.glyph.Shape.WHOLE_REST;
import org.audiveris.omr.math.PointUtil;
import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>RestSymbol</code> implements rest symbols whose decoration uses staff lines
 * as background.
 *
 * @author Hervé Bitteur
 */
public class RestSymbol
        extends DecorableSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RestSymbol.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a RestSymbol (with decoration?) standard size.
     *
     * @param shape  the precise shape (LONG_REST, BREVE_REST, WHOLE_REST, HALF_REST, HW_REST_set)
     * @param family the musicFont family
     */
    public RestSymbol (Shape shape,
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

        // Rest symbol layout
        p.layout = font.layoutShapeByCode(shape);

        final Rectangle2D rs = p.layout.getBounds();

        if (isDecorated) {
            // Define specific vertical offset from lines center to rest center
            p.offset = new Point2D.Double(0, getYOffset(rs.getHeight()));

            // Lines layout
            p.linesLayout = font.layoutShapeByCode(Shape.STAFF_LINES);
            p.rect = p.linesLayout.getBounds();
        } else {
            p.rect = rs;
        }

        return p;
    }

    //------------//
    // getYOffset //
    //------------//
    /**
     * Report the vertical offset to use, according to rest shape.
     *
     * @param height symbol height
     * @return the vertical offset
     */
    private double getYOffset (double height)
    {
        return switch (shape) {
            default -> 0;
            case LONG_REST -> 0;
            case BREVE_REST -> -0.5 * height;
            case WHOLE_REST -> -1.25 * height;
            case HALF_REST -> -0.5 * height;
        };
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
        final Point2D loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);

        if (isDecorated) {
            // Paint staff lines and compute location for rest symbol
            final Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, p.linesLayout, loc, AREA_CENTER);
            g.setComposite(oldComposite);

            PointUtil.add(loc, p.offset);
        }

        // Paint rest symbol
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);
    }

    //--------------------//
    // supportsDecoration //
    //--------------------//
    @Override
    protected boolean supportsDecoration ()
    {
        return (shape == BREVE_REST) || (shape == LONG_REST) || (shape == WHOLE_REST)
                || (shape == HALF_REST);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

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
