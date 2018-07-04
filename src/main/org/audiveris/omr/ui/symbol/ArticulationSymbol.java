//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A r t i c u l a t i o n S y m b o l                              //
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
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code ArticulationSymbol} implements an articulation symbol.
 *
 * @author Hervé Bitteur
 */
public class ArticulationSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The head part. */
    private static final BasicSymbol head = Symbols.getSymbol(Shape.NOTEHEAD_BLACK);

    /** Ratio of head height for the decorated rectangle height. */
    private static final double yRatio = 2.5;

    /** Offset ratio of articulation center WRT decorated rectangle height. */
    private static final double dyRatio = -0.25;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a {@code ArticulationSymbol} (with decoration?) standard size
     *
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for articulation part
     */
    public ArticulationSymbol (Shape shape,
                               boolean decorated,
                               int... codes)
    {
        this(false, shape, decorated, codes);
    }

    /**
     * Create a {@code ArticulationSymbol} (with decoration?)
     *
     * @param isIcon    true for an icon
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for articulation part
     */
    protected ArticulationSymbol (boolean isIcon,
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
        return new ArticulationSymbol(true, shape, decorated, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        // Articulation symbol layout
        p.layout = font.layout(getString());

        Rectangle2D rs = p.layout.getBounds(); // Symbol bounds

        if (decorated) {
            // Head layout
            p.headLayout = font.layout(head.getString());

            // Use a rectangle 'yRatio' times as high as note head
            Rectangle2D rh = p.headLayout.getBounds(); // Head bounds
            p.rect = new Rectangle(
                    (int) Math.ceil(Math.max(rh.getWidth(), rs.getWidth())),
                    (int) Math.ceil(yRatio * rh.getHeight()));

            // Define specific offset
            p.offset = new Point(0, (int) Math.rint(dyRatio * p.rect.height));
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
                          Params params,
                          Point location,
                          Alignment alignment)
    {
        MyParams p = (MyParams) params;

        if (decorated) {
            // Draw a note head (using composite) on the bottom
            Point loc = alignment.translatedPoint(BOTTOM_CENTER, p.rect, location);
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, p.headLayout, loc, BOTTOM_CENTER);
            g.setComposite(oldComposite);

            // Articulation above head
            loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
            loc.translate(p.offset.x, p.offset.y);
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
        } else {
            // Articulation alone
            Point loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
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

        // offset: if decorated, offset of symbol center vs decorated image center
        // layout: articulation layout
        // rect:   global image (= head + artic if decorated, artic if not)
        //
        // Layout for head
        TextLayout headLayout;
    }
}
