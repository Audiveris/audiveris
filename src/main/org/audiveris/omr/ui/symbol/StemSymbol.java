//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S t e m S y m b o l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;

/**
 * Class {@code StemSymbol} implements a stem symbol.
 *
 * @author Hervé Bitteur
 */
public class StemSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    // The head+stem part
    private static final BasicSymbol quarter = Symbols.SYMBOL_QUARTER;

    // The stem part
    private static final BasicSymbol stem = Symbols.SYMBOL_STEM;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a {@code StemSymbol} (with decoration?) standard size
     *
     * @param decorated true for a decorated image
     */
    public StemSymbol (boolean decorated)
    {
        this(false, decorated);
    }

    /**
     * Create a {@code StemSymbol}(with decoration?)
     *
     * @param isIcon    true for an icon
     * @param decorated true for a decorated image
     */
    protected StemSymbol (boolean isIcon,
                          boolean decorated)
    {
        super(isIcon, Shape.STEM, decorated);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------------//
    // createDecoratedSymbol //
    //-----------------------//
    @Override
    protected ShapeSymbol createDecoratedSymbol ()
    {
        return new StemSymbol(isIcon, true);
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new StemSymbol(true, decorated);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        // Stem layout
        p.layout = font.layout(stem.getString());

        Rectangle rs = p.layout.getBounds().getBounds(); // Stem bounds

        if (decorated) {
            // Quarter layout
            p.quarterLayout = font.layout(quarter.getString());

            p.rect = p.quarterLayout.getBounds();

            // Define specific offset
            p.offset = new Point2D.Double((p.rect.getWidth() - rs.width) / 2,
                                          -(p.rect.getHeight() - rs.height) / 2);
        } else {
            p.rect = p.layout.getBounds();
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
        MyParams p = (MyParams) params;

        if (decorated) {
            Point2D loc = alignment.translatedPoint(TOP_RIGHT, p.rect, location);

            // Decorations (using composite)
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, p.quarterLayout, loc, TOP_RIGHT);
            g.setComposite(oldComposite);

            // Stem
            MusicFont.paint(g, p.layout, loc, TOP_RIGHT);
        } else {
            // Stem alone
            Point2D loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends Params
    {

        // offset: if decorated, offset of symbol center vs decorated image center
        // layout: stem layout
        // rect:   global image (head + stem if decorated, stem if not)
        //
        // Layout for quarter
        TextLayout quarterLayout;
    }
}
