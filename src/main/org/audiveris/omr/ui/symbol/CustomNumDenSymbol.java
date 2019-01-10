//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              C u s t o m N u m D e n S y m b o l                               //
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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;

/**
 * Class {@code CustomNumDenSymbol} displays a custom time signature, with just the N
 * and D letters.
 *
 * @author Hervé Bitteur
 */
public class CustomNumDenSymbol
        extends ShapeSymbol
{

    /**
     * Creates a new CustomNumDenSymbol object, standard size
     */
    public CustomNumDenSymbol ()
    {
        this(false);
    }

    /**
     * Creates a new CustomNumDenSymbol object.
     *
     * @param isIcon true for icon
     */
    protected CustomNumDenSymbol (boolean isIcon)
    {
        super(isIcon, Shape.CUSTOM_TIME, true);
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new CustomNumDenSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        TextFont textFont = new TextFont((int) Math.rint(font.getSize2D() * 0.62));
        p.nLayout = textFont.layout("N");
        p.dLayout = textFont.layout("D");
        p.rect = new Rectangle(
                (int) Math.ceil(p.nLayout.getBounds().getWidth()),
                (int) Math.ceil(p.nLayout.getBounds().getHeight() * 2.2));

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

        Point loc = alignment.translatedPoint(TOP_CENTER, p.rect, location);
        OmrFont.paint(g, p.nLayout, loc, TOP_CENTER);

        loc.y += p.rect.height;
        OmrFont.paint(g, p.dLayout, loc, BOTTOM_CENTER);
    }

    //--------//
    // Params //
    //--------//
    protected class MyParams
            extends Params
    {

        // layout not used
        // rect for global image
        // Layout for N
        TextLayout nLayout;

        // Layout for D
        TextLayout dLayout;
    }
}
