//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  R e p e a t D o t S y m b o l                                 //
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
import java.awt.geom.Rectangle2D;

/**
 * Class {@code RepeatDotSymbol} implements a decorated repeat dot symbol.
 *
 * @author Hervé Bitteur
 */
public class RepeatDotSymbol
        extends ShapeSymbol
{
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
        Params p = new Params();

        // Dot layout
        p.layout = font.layout(getString());

        if (decorated) {
            // Use a rectangle 3 times as high as dot
            Rectangle2D dRect = p.layout.getBounds();
            p.rect = new Rectangle(
                    (int) Math.ceil(dRect.getWidth()),
                    (int) Math.ceil(3 * dRect.getHeight()));
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
        if (decorated) {
            // Draw a dot(using composite) on the top
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);

            Point loc = alignment.translatedPoint(TOP_CENTER, params.rect, location);
            MusicFont.paint(g, params.layout, loc, TOP_CENTER);
            g.setComposite(oldComposite);

            // Dot below
            loc.y += ((5 * params.rect.height) / 6);
            MusicFont.paint(g, params.layout, loc, AREA_CENTER);
        } else {
            // Dot alone
            MusicFont.paint(g, params.layout, location, TOP_LEFT);
        }
    }
}
