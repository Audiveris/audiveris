//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S t e m S y m b o l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code StemSymbol} implements a decorated stem symbol
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
     * Create a StemSymbol
     */
    public StemSymbol ()
    {
        this(false);
    }

    /**
     * Create a StemSymbol
     *
     * @param isIcon true for an icon
     */
    protected StemSymbol (boolean isIcon)
    {
        super(isIcon, Shape.STEM, true); // Decorated
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new StemSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        // Quarter layout
        p.layout = font.layout(quarter.getString());

        // Stem layout
        p.stemLayout = font.layout(stem.getString());

        Rectangle2D qRect = p.layout.getBounds();
        p.rect = new Rectangle(
                (int) Math.ceil(qRect.getWidth()),
                (int) Math.ceil(qRect.getHeight()));

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

        Point loc = alignment.translatedPoint(TOP_RIGHT, p.rect, location);

        // Decorations (using composite)
        Composite oldComposite = g.getComposite();
        g.setComposite(decoComposite);
        MusicFont.paint(g, p.layout, loc, TOP_RIGHT);
        g.setComposite(oldComposite);

        // Stem
        MusicFont.paint(g, p.stemLayout, loc, TOP_RIGHT);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Params //
    //--------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ------------------------------------------------------------------------

        // layout for quarter layout
        // rect for global image
        // layout for stem
        TextLayout stemLayout;
    }
}
