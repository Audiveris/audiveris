//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A r t i c u l a t i o n S y m b o l                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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
 * Class {@code ArticulationSymbol} implements a decorated articulation symbol.
 *
 * @author Hervé Bitteur
 */
public class ArticulationSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    // The head part
    private static final BasicSymbol head = Symbols.getSymbol(Shape.NOTEHEAD_BLACK);

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

        // Articulation layout
        p.layout = font.layout(getString());

        if (decorated) {
            // Head layout
            p.headLayout = font.layout(head.getString());

            // Use a rectangle twice as high as note head
            Rectangle2D hRect = p.headLayout.getBounds();
            p.rect = new Rectangle(
                    (int) Math.ceil(hRect.getWidth()),
                    (int) Math.ceil(2.5 * hRect.getHeight()));
        } else {
            p.rect = p.layout.getBounds().getBounds();
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
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);

            Point loc = alignment.translatedPoint(BOTTOM_CENTER, p.rect, location);
            MusicFont.paint(g, p.headLayout, loc, BOTTOM_CENTER);
            g.setComposite(oldComposite);

            // Articulation above
            loc.y -= ((3 * p.rect.height) / 4);
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
        } else {
            // Articulation alone
            MusicFont.paint(g, p.layout, location, TOP_LEFT);
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

        // layout for articulation
        // rect for global image
        //
        // Layout for head
        TextLayout headLayout;
    }
}
