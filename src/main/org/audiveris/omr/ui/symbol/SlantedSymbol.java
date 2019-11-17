//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S l a n t e d S y m b o l                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code SlantedSymbol} draws symbols made of several slanted parts.
 * (such as in DYNAMICS_FP, where both the F and the P are slanted and appear too far from each
 * other)
 *
 * @author Hervé Bitteur
 */
public class SlantedSymbol
        extends ShapeSymbol
{

    /**
     * Creates a new SlantedSymbol object, standard size
     *
     * @param shape the related shape
     * @param codes the codes for MusicFont characters
     */
    public SlantedSymbol (Shape shape,
                          int... codes)
    {
        this(false, shape, codes);
    }

    /**
     * Creates a new SlantedSymbol object.
     *
     * @param isIcon true for icon
     * @param shape  the related shape
     * @param codes  the codes for MusicFont characters
     */
    protected SlantedSymbol (boolean isIcon,
                             Shape shape,
                             int... codes)
    {
        super(isIcon, shape, false, codes);
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new SlantedSymbol(true, shape, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.layouts = new SmartLayout[codes.length];

        // Union of all rectangles
        Rectangle2D rect = null;

        // Current abscissa
        float x = 0;

        for (int i = 0; i < codes.length; i++) {
            int code = codes[i];
            TextLayout layout = font.layout(code);
            Rectangle2D r = layout.getBounds();

            // Abscissa reduction because of slanted characters
            // Its value depends on whether we have a 'f' or not
            float dx;
            int c = code - MusicFont.CODE_OFFSET;

            if ((c == 102) || // F
                    (c == 196) || // FF
                    (c == 236) || // FFF
                    (c == 83) || // SF
                    (c == 90)) { // FZ
                dx = (float) r.getHeight() * 0.215f; // Measured
            } else {
                dx = (float) r.getHeight() * 0.075f; // Measured
            }

            p.layouts[i] = new SmartLayout(layout, dx);

            if (i > 0) {
                x -= dx;
            }

            if (i == 0) {
                rect = layout.getPixelBounds(null, x, 0);
            } else {
                Rectangle2D.union(rect, layout.getPixelBounds(null, x, 0), rect);
            }

            x += (r.getWidth() - dx);
        }

        p.rect = rect;

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

        Point2D loc = alignment.translatedPoint(BASELINE_LEFT, p.rect, location);

        // Precise abscissa
        double x = loc.getX();

        for (int i = 0; i < p.layouts.length; i++) {
            SmartLayout smart = p.layouts[i];

            if (i > 0) {
                x -= smart.dx; // Before symbol
            }

            OmrFont.paint(g, smart.layout, new Point2D.Double(x, loc.getY()), BASELINE_LEFT);

            x += smart.layout.getBounds().getWidth();
            x -= smart.dx; // After symbol
        }
    }

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends Params
    {

        // layout not used
        // rect for global image
        //
        // Sequence of layouts:
        SmartLayout[] layouts;
    }

    //-------------//
    // SmartLayout //
    //-------------//
    /**
     * A trick to remove useless dx margin before and after the symbol is drawn.
     */
    protected static class SmartLayout
    {

        // The standard character glyph
        final TextLayout layout;

        // Translation before and after
        final float dx;

        SmartLayout (TextLayout layout,
                     float dx)
        {
            this.layout = layout;
            this.dx = dx;
        }
    }
}
