//----------------------------------------------------------------------------//
//                                                                            //
//                           H e a d s S y m b o l                            //
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
import static omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code HeadsSymbol} displays a column of several identical heads
 * (black, void or whole)
 */
public class HeadsSymbol
        extends ShapeSymbol
{
    //~ Instance fields --------------------------------------------------------

    private final int count;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // HeadsSymbol //
    //-------------//
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

    //~ Methods ----------------------------------------------------------------
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
        p.dy = (int) Math.rint(font.getFontInterline());
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

    //~ Inner Classes ----------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ----------------------------------------------------

        int dy;

    }
}
