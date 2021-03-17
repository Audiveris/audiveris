//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     H e a d s S y m b o l                                      //
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

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code HeadsSymbol} displays a column of several identical heads.
 * (black, void or whole)
 * <p>
 * [NOTA: Class no longer used, but kept for potential future interest]
 *
 * @author Hervé Bitteur
 */
public class HeadsSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final int count;

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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

        p.dy = font.getStaffInterline();

        Rectangle2D r = p.layout.getBounds();
        p.rect = new Rectangle2D.Double();
        p.rect.setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight() + ((count - 1) * p.dy));

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
        Point2D loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);

        for (int i = 0; i < count; i++) {
            MusicFont.paint(g, p.layout, loc, TOP_LEFT);
            loc.setLocation(loc.getX(), loc.getY() + p.dy);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected static class MyParams
            extends Params
    {

        double dy;
    }
}
