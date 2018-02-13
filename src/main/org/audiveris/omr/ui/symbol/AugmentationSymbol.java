//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A u g m e n t a t i o n S y m b o l                              //
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

import static org.audiveris.omr.ui.symbol.Alignment.MIDDLE_LEFT;

/**
 * Class {@code AugmentationSymbol} implements a decorated augmentation symbol.
 *
 * @author Hervé Bitteur
 */
public class AugmentationSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    // The head part
    private static final BasicSymbol head = Symbols.getSymbol(Shape.NOTEHEAD_BLACK);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a {@code AugmentationSymbol} (with decoration?) standard size
     *
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for augmentation part
     */
    public AugmentationSymbol (Shape shape,
                               boolean decorated,
                               int... codes)
    {
        this(false, shape, decorated, codes);
    }

    /**
     * Create a {@code AugmentationSymbol} (with decoration?)
     *
     * @param isIcon    true for an icon
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for augmentation part
     */
    protected AugmentationSymbol (boolean isIcon,
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
        return new AugmentationSymbol(true, shape, decorated, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected BasicSymbol.Params getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        // Augmentation layout
        p.layout = font.layout(getString());

        if (decorated) {
            // Head layout
            p.headLayout = font.layout(head.getString());

            // Use a rectangle twice as wide as note head
            Rectangle2D hRect = p.headLayout.getBounds();
            p.rect = new Rectangle(
                    (int) Math.ceil(2 * hRect.getWidth()),
                    (int) Math.ceil(hRect.getHeight()));
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
                          BasicSymbol.Params params,
                          Point location,
                          Alignment alignment)
    {
        MyParams p = (MyParams) params;

        if (decorated) {
            // Draw a note head (using composite) on the left side
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);

            Point loc = alignment.translatedPoint(MIDDLE_LEFT, p.rect, location);
            MusicFont.paint(g, p.headLayout, loc, MIDDLE_LEFT);
            g.setComposite(oldComposite);

            // Augmentation on right side
            loc.x += ((3 * p.rect.width) / 4);
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
        } else {
            // Augmentation alone
            MusicFont.paint(g, p.layout, location, TOP_LEFT);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Params //
    //--------//
    protected class MyParams
            extends BasicSymbol.Params
    {
        //~ Instance fields ------------------------------------------------------------------------

        // layout for augmentation
        // rect for global image
        //
        // Layout for head
        TextLayout headLayout;
    }
}
