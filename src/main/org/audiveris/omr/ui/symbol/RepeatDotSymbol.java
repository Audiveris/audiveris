//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  R e p e a t D o t S y m b o l                                 //
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
import static org.audiveris.omr.ui.symbol.ShapeSymbol.decoComposite;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code RepeatDotSymbol} implements a decorated repeat dot symbol.
 *
 * @author Hervé Bitteur
 */
public class RepeatDotSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Global decorated height WRT dot height. */
    private static final double yRatio = 3.5;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a {@code RepeatDotSymbol} (with decoration?) standard size
     *
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for articulation part
     */
    public RepeatDotSymbol (Shape shape,
                            boolean decorated,
                            int... codes)
    {
        this(false, shape, decorated, codes);
    }

    /**
     * Create a {@code RepeatDotSymbol} (with decoration?)
     *
     * @param isIcon    true for an icon
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for articulation part
     */
    protected RepeatDotSymbol (boolean isIcon,
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
        return new RepeatDotSymbol(true, shape, decorated, codes);
    }

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
        p.layout = font.layout(getString());

        Rectangle2D rs = p.layout.getBounds(); // Symbol bounds

        if (decorated) {
            // Use a rectangle yTimes times as high as dot
            p.rect = new Rectangle(
                    (int) Math.ceil(rs.getWidth()),
                    (int) Math.ceil(yRatio * rs.getHeight()));

            // Define specific offset
            p.offset = new Point(0, (int) Math.rint(rs.getHeight() * ((yRatio - 1) / 2)));
        } else {
            p.rect = rs.getBounds();
        }

        return p;
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params p,
                          Point location,
                          Alignment alignment)
    {
        if (decorated) {
            // Draw a dot (using composite) on the top
            Point loc = alignment.translatedPoint(TOP_CENTER, p.rect, location);
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, p.layout, loc, TOP_CENTER);
            g.setComposite(oldComposite);

            // Dot below
            loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
            loc.y += p.offset.y;
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
        } else {
            // Dot alone
            Point loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
        }
    }
}
