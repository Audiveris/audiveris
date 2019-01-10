//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       K e y S y m b o l                                        //
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
import org.audiveris.omr.sig.inter.KeyInter;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code KeySymbol} displays a Key Signature symbol.
 *
 * @author Hervé Bitteur
 */
public abstract class KeySymbol
        extends ShapeSymbol
{

    /** The key to represent, -7..-1 for flats, 1..7 for sharps */
    protected final int key;

    /**
     * Creates a new KeySymbol object.
     *
     * @param key    the key value: 1..7 for sharps
     * @param isIcon true for an icon
     * @param shape  the related shape
     * @param codes  the code for item shape
     */
    public KeySymbol (int key,
                      boolean isIcon,
                      Shape shape,
                      int... codes)
    {
        super(isIcon, shape, false, codes);
        this.key = key;
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.stepDy = font.getStaffInterline() / 2.0;

        // One item
        p.layout = layout(font);

        Rectangle2D r2 = p.layout.getBounds();
        p.itemDx = r2.getWidth() * 1.15;

        int sign = Integer.signum(key);
        p.itemRect = new Rectangle((int) Math.ceil(r2.getWidth()), (int) Math.ceil(r2.getHeight()));

        for (int k = 1; k <= (key * sign); k++) {
            int pitch = KeyInter.getItemPitch(k * sign, null);
            Rectangle r = new Rectangle(
                    (int) Math.rint((k - 1) * p.itemDx),
                    (int) Math.rint(pitch * p.stepDy),
                    p.itemRect.width,
                    p.itemRect.height);

            if (p.rect == null) {
                p.rect = r;
            } else {
                p.rect = p.rect.union(r);
            }
        }

        p.rect.x = (p.rect.width / 2);
        p.rect.y = -(int) Math.rint(KeyInter.getStandardPosition(key) * p.stepDy);

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
        Point loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        loc.x -= (p.rect.width / 2);
        loc.y -= (int) Math.rint(KeyInter.getStandardPosition(key) * p.stepDy);

        int sign = Integer.signum(key);

        for (int k = 1; k <= (key * sign); k++) {
            int pitch = KeyInter.getItemPitch(k * sign, null);
            MusicFont.paint(
                    g,
                    p.layout,
                    new Point(
                            loc.x + (int) Math.rint((k - 1) * p.itemDx),
                            loc.y + (int) Math.rint(pitch * p.stepDy)),
                    MIDDLE_LEFT);
        }
    }

    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {

        double stepDy; // Dy from one step to the other

        double itemDx; // Dx from one item to the other

        Rectangle itemRect; // Item rectangle
    }
}
