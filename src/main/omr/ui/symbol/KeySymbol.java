//----------------------------------------------------------------------------//
//                                                                            //
//                             K e y S y m b o l                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.glyph.Shape;

import omr.score.entity.KeySignature;
import static omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code KeySymbol} displays a Key Signature symbol.
 *
 * <p><img src="doc-files/KeySignatures.png" />
 *
 */
public abstract class KeySymbol
        extends ShapeSymbol
{
    //~ Instance fields --------------------------------------------------------

    /** The key to represent, -7..-1 for flats, 1..7 for sharps */
    protected final int key;

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // KeySymbol //
    //-----------//
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

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.stepDy = font.getFontInterline() / 2;

        // One item
        p.layout = layout(font);

        Rectangle2D r2 = p.layout.getBounds();
        p.itemDx = r2.getWidth() * 1.15;

        int sign = Integer.signum(key);
        p.itemRect = new Rectangle(
                (int) Math.ceil(r2.getWidth()),
                (int) Math.ceil(r2.getHeight()));

        for (int k = 1; k <= (key * sign); k++) {
            int position = KeySignature.getItemPosition(k * sign, null);
            Rectangle r = new Rectangle(
                    (int) Math.rint((k - 1) * p.itemDx),
                    (int) Math.rint(position * p.stepDy),
                    p.itemRect.width,
                    p.itemRect.height);

            if (p.rect == null) {
                p.rect = r;
            } else {
                p.rect = p.rect.union(r);
            }
        }

        p.rect.x = (p.rect.width / 2);
        p.rect.y = -(int) Math.rint(
                KeySignature.getStandardPosition(key) * p.stepDy);

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
        loc.y -= (int) Math.rint(
                KeySignature.getStandardPosition(key) * p.stepDy);

        int sign = Integer.signum(key);

        for (int k = 1; k <= (key * sign); k++) {
            int position = KeySignature.getItemPosition(k * sign, null);
            MusicFont.paint(
                    g,
                    p.layout,
                    new Point(
                    loc.x + (int) Math.rint((k - 1) * p.itemDx),
                    loc.y + (int) Math.rint(position * p.stepDy)),
                    MIDDLE_LEFT);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ----------------------------------------------------

        double stepDy; // Dy from one step to the other

        double itemDx; // Dx from one item to the other

        Rectangle itemRect; // Item rectangle

    }
}
