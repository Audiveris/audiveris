//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                O t t a v a C l e f S y m b o l                                 //
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

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code OttavaClefSymbol} displays a clef (bass or treble) with the addition of
 * an ottava (alta or bassa).
 *
 * @author Hervé Bitteur
 */
public class OttavaClefSymbol
        extends ShapeSymbol
{

    // True for alta, false for bassa
    private final boolean isAlta;

    /**
     * Creates a new OttavaClefSymbol object.
     *
     * @param isAlta true for alta, false for bassa
     * @param isIcon true for an icon
     * @param shape  the related shape
     * @param codes  the codes for MusicFont characters
     */
    public OttavaClefSymbol (boolean isAlta,
                             boolean isIcon,
                             Shape shape,
                             int... codes)
    {
        super(isIcon, shape, false, codes);
        this.isAlta = isAlta;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new OttavaClefSymbol(isAlta, true, shape, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        return new MyParams(font);
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
        Point loc = alignment.translatedPoint(TOP_CENTER, p.rect, location);

        if (isAlta) {
            MusicFont.paint(g, p.ottavaLayout, loc, TOP_CENTER);
            loc.y += p.ottavaRect.getHeight();
            MusicFont.paint(g, p.layout, loc, TOP_CENTER);
        } else {
            MusicFont.paint(g, p.layout, loc, TOP_CENTER);
            loc.y += p.clefRect.getHeight();
            MusicFont.paint(g, p.ottavaLayout, loc, TOP_CENTER);
        }
    }

    //----------//
    // MyParams //
    //----------//
    private class MyParams
            extends Params
    {

        final TextLayout ottavaLayout;

        final Rectangle2D ottavaRect;

        final Rectangle2D clefRect;

        MyParams (MusicFont font)
        {
            ottavaLayout = Symbols.SYMBOL_OTTAVA.layout(font);
            ottavaRect = ottavaLayout.getBounds();

            layout = font.layout(codes);
            clefRect = layout.getBounds();

            rect = new Rectangle(
                    (int) Math.ceil(clefRect.getWidth()),
                    (int) Math.ceil(ottavaRect.getHeight() + clefRect.getHeight()));
        }
    }
}
