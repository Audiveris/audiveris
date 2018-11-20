//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     H e a d s S y m b o l                                      //
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
import java.awt.geom.Rectangle2D;

/**
 * Class {@code HeadsSymbol} displays a column of several identical heads.
 * (black, void or whole)
 *
 * @author Hervé Bitteur
 */
public class HeadsSymbol
        extends ShapeSymbol
{

    private final int count;

    /**
     * Creates a new HeadsSymbol object.
     *
     * @param count  the number of heads
     * @param isIcon true for an icon
     * @param shape  the related shape
     * @param codes  the codes for MusicFont characters
     */
    public HeadsSymbol (int count,
                        boolean isIcon,
                        Shape shape,
                        int... codes)
    {
        super(isIcon, shape, false, codes);
        this.count = count;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new HeadsSymbol(count, true, shape, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();
        p.layout = layout(font);

        Rectangle2D r = p.layout.getBounds();
        p.dy = (int) Math.rint(font.getStaffInterline());
        p.rect = new Rectangle(
                (int) Math.ceil(r.getWidth()),
                ((count * p.dy) + (int) Math.rint(r.getHeight())) - p.dy);

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
        Point loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);

        for (int i = 0; i < count; i++) {
            MusicFont.paint(g, p.layout, loc, TOP_LEFT);
            loc.y += p.dy;
        }
    }

    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {

        int dy;
    }
}
