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

import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.inter.KeyInter;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code KeySymbol} displays a Key Signature symbol.
 *
 * @author Hervé Bitteur
 */
public abstract class KeySymbol
        extends ShapeSymbol
{

    /**
     * The fifths value to represent, -7..-1 for flats, 1..7 for sharps.
     * For KEY_CANCEL, fifths value is the one of the canceled key.
     */
    public final int fifths;

    /**
     * Creates a new KeySymbol object.
     *
     * @param fifths fifths value within [-7 .. +7]
     * @param isIcon true for an icon
     * @param shape  the related shape
     * @param codes  the code for item shape
     */
    public KeySymbol (int fifths,
                      boolean isIcon,
                      Shape shape,
                      int... codes)
    {
        super(isIcon, shape, false, codes);
        this.fifths = fifths;
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

        int sign = Integer.signum(fifths);
        p.itemRect = new Rectangle((int) Math.ceil(r2.getWidth()), (int) Math.ceil(r2.getHeight()));

        // Compute global p.rect for KeySharps and KeyFlats
        for (int k = 1; k <= (fifths * sign); k++) {
            int pitch = KeyInter.getItemPitch(k * sign, null);
            Rectangle2D r = new Rectangle2D.Double(
                    (k - 1) * p.itemDx,
                    pitch * p.stepDy,
                    p.itemRect.width,
                    p.itemRect.height);

            if (p.rect == null) {
                p.rect = r;
            } else {
                p.rect = p.rect.createUnion(r);
            }
        }

        if (p.rect == null) {
            // Case of basic KeyCancel for Shape palette icon (just one item)
            p.rect = new Rectangle2D.Double(0, 0, p.itemRect.width, p.itemRect.height);
        }

        // Define global rect as:
        // (x,y): area center wrt staff middle-left
        // (w,h): area dim
        p.rect.setRect(p.rect.getWidth() / 2,
                       p.rect.getY() - KeyInter.getStandardPosition(fifths) * p.stepDy,
                       p.rect.getWidth(),
                       p.rect.getHeight());

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
        Point2D loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);

        // Set loc to (x=left side, y=staff mid line)
        PointUtil.add(loc, -p.rect.getWidth() / 2, -KeyInter.getStandardPosition(fifths) * p.stepDy);

        if (fifths == 0) {
            int pitch = KeyInter.getItemPitch(1, null);
            MusicFont.paint(
                    g,
                    p.layout,
                    new Point2D.Double(loc.getX(),
                                       loc.getY() + pitch * p.stepDy),
                    MIDDLE_LEFT);
        } else {
            int sign = Integer.signum(fifths);

            for (int k = 1; k <= (fifths * sign); k++) {
                int pitch = KeyInter.getItemPitch(k * sign, null);
                MusicFont.paint(
                        g,
                        p.layout,
                        new Point2D.Double(loc.getX() + (k - 1) * p.itemDx,
                                           loc.getY() + pitch * p.stepDy),
                        MIDDLE_LEFT);
            }
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(" fifths:").append(fifths);

        return sb.toString();
    }

    //----------//
    // MyParams //
    //----------//
    protected static class MyParams
            extends Params
    {

        double stepDy; // Dy from one step to the other

        double itemDx; // Dx from one item to the other

        Rectangle itemRect; // One item rectangle
    }
}
