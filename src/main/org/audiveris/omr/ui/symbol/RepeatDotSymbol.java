//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  R e p e a t D o t S y m b o l                                 //
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
import static org.audiveris.omr.ui.symbol.Alignment.TOP_CENTER;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>RepeatDotSymbol</code> implements a repeat dot symbol, perhaps decorated.
 *
 * @author Hervé Bitteur
 */
public class RepeatDotSymbol
        extends DecorableSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Global decorated height WRT dot height. */
    private static final double yRatio = 3.5;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a <code>RepeatDotSymbol</code> standard size with no decoration.
     *
     * @param family the musicFont family
     */
    public RepeatDotSymbol (MusicFamily family)
    {
        super(Shape.REPEAT_DOT, family);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        // offset: if decorated, offset of symbol center vs decorated image center
        // layout: dot layout
        // rect:   global image (= other dot + dot if decorated, dot if not)
        Params p = new Params();

        // Dot layout
        p.layout = font.layoutShapeByCode(shape);

        Rectangle2D rs = p.layout.getBounds(); // Symbol bounds

        if (isDecorated) {
            // Use a rectangle yTimes times as high as dot
            p.rect = new Rectangle2D.Double(0, 0, rs.getWidth(), yRatio * rs.getHeight());

            // Define specific offset
            p.offset = new Point2D.Double(0, rs.getHeight() * ((yRatio - 1) / 2));
        } else {
            p.rect = rs;
        }

        return p;
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params p,
                          Point2D location,
                          Alignment alignment)
    {
        if (isDecorated) {
            // Draw a dot (using composite) on the top
            Point2D loc = alignment.translatedPoint(TOP_CENTER, p.rect, location);
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, p.layout, loc, TOP_CENTER);
            g.setComposite(oldComposite);

            // Dot below
            loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
            PointUtil.add(loc, 0, p.offset.getY());
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
        } else {
            // Dot alone
            Point2D loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
        }
    }
}
