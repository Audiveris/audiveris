//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                B a c k T o B a c k S y m b o l                                 //
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

import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code BackToBackSymbol} displays a BACK_TO_BACK_REPEAT_SIGN.
 *
 * @author Hervé Bitteur
 */
public class BackToBackSymbol
        extends ShapeSymbol
{

    // Total width, computed from width of RIGHT_REPEAT_SIGN symbol
    private static final double WIDTH_RATIO = 1.6;

    // Abscissa of thin barline, computed from width of RIGHT_REPEAT_SIGN symbol
    private static final double DX_RATIO = 1.15;

    // The RIGHT_REPEAT_SIGN symbol
    private final ShapeSymbol rightSymbol = Symbols.getSymbol(RIGHT_REPEAT_SIGN);

    // The THIN_BARLINE symbol
    private final ShapeSymbol thinSymbol = Symbols.getSymbol(THIN_BARLINE);

    // The REPEAT_DOT_PAIR symbol
    private final ShapeSymbol dotsSymbol = Symbols.getSymbol(REPEAT_DOT_PAIR);

    /**
     * Create a BackToBackSymbol.
     */
    public BackToBackSymbol ()
    {
        this(false);
    }

    /**
     * Create a BackToBackSymbol.
     *
     * @param isIcon true for an icon
     */
    protected BackToBackSymbol (boolean isIcon)
    {
        super(isIcon, Shape.DOUBLE_BARLINE, false);
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new BackToBackSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.layout = font.layout(rightSymbol);
        p.thinLayout = font.layout(thinSymbol);
        p.dotsLayout = font.layout(dotsSymbol);

        Rectangle2D rightRect = p.layout.getBounds();
        p.dx = (int) Math.ceil(rightRect.getWidth() * DX_RATIO);
        p.rect = new Rectangle2D.Double(0,
                                        0,
                                        rightRect.getWidth() * WIDTH_RATIO,
                                        rightRect.getHeight());

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
        Point2D loc = alignment.translatedPoint(MIDDLE_LEFT, p.rect, location);
        MusicFont.paint(g, p.layout, loc, MIDDLE_LEFT);

        PointUtil.add(loc, p.dx, 0);
        MusicFont.paint(g, p.thinLayout, loc, AREA_CENTER);

        PointUtil.add(loc, p.rect.getWidth() - p.dx, 0);
        MusicFont.paint(g, p.dotsLayout, loc, MIDDLE_RIGHT);
    }

    //----------//
    // MyParams //
    //----------//
    protected static class MyParams
            extends Params
    {

        TextLayout thinLayout;

        TextLayout dotsLayout;

        int dx;
    }
}
