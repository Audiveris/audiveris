//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T e m p l a t e S y m b o l                                  //
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
import org.audiveris.omr.image.TemplateFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>TemplateSymbol</code> defines a symbol meant only for template matching.
 * <p>
 * <b>BEWARE:</b>Don't use it for simple display, use a {@link ShapeSymbol} of proper shape instead.
 *
 * @author Hervé Bitteur
 */
public class TemplateSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TemplateSymbol.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Indicates a smaller symbol. */
    protected final boolean isSmall;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new TemplateSymbol object.
     *
     * @param shape  shape for the template
     * @param family the selected MusicFont family
     */
    public TemplateSymbol (Shape shape,
                           MusicFamily family)
    {
        super(shape, family);
        isSmall = shape.isSmallHead();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // getFatBounds //
    //--------------//
    /**
     * Report the fat bounds of symbol (within a perhaps larger template).
     *
     * @param font provided font
     * @return relative fat symbol bounds within template
     */
    public Rectangle getFatBounds (MusicFont font)
    {
        return getParams(font).rawRect.getBounds();
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        final MyParams p = new MyParams();

        ShapeSymbol symbol = font.getSymbol(shape);

        while (symbol == null && font.getBackup() != null) {
            logger.debug(
                    "no symbol in TemplateSymbol for {} in family {} ...",
                    shape,
                    font.getMusicFamily());
            font = font.getBackup();
            symbol = font.getSymbol(shape);
        }

        if (symbol == null) {
            logger.warn(
                    "TemplateSymbol. No symbol for {} in family {}",
                    shape,
                    font.getMusicFamily());
        }

        final Params symParams = symbol.getParams(font);
        p.layout = symParams.layout;

        // Symbol rectangle
        final Rectangle2D rawSym = p.layout.getBounds();
        final Rectangle fatSym = rawSym.getBounds();

        // Template rectangle with origin at (0,0) and some room around the symbol
        final int room = 2 * (int) Math.ceil(TemplateFactory.maxDistanceFromSymbol());
        p.rect = new Rectangle2D.Double(
                0,
                0,
                room + (isSmall ? fatSym.width : rawSym.getWidth()),
                room + (isSmall ? fatSym.height : rawSym.getHeight()));

        // Symbol bounds relative to template rectangle
        p.rawRect = new Rectangle2D.Double(
                (p.rect.getWidth() - rawSym.getWidth()) / 2,
                (p.rect.getHeight() - rawSym.getHeight()) / 2,
                rawSym.getWidth(),
                rawSym.getHeight());

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

        g.setColor(Color.BLACK);

        Point2D loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);
    }
    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // MyParams //
    //----------//
    protected static class MyParams
            extends Params
    {
        Rectangle2D rawRect; // Precise font-based bounds of symbol inside template image
    }
}
