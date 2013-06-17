//----------------------------------------------------------------------------//
//                                                                            //
//                       F l a g s D o w n S y m b o l                        //
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
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code FlagsDownSymbol} displays a pack of several flags down
 *
 * @author Hervé Bitteur
 */
public class FlagsDownSymbol
        extends ShapeSymbol
{
    //~ Instance fields --------------------------------------------------------

    /** The number of flags */
    protected final int fn;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new FlagsDownSymbol object.
     *
     * @param flagCount the number of flags
     * @param isIcon    true for an icon
     * @param shape     the related shape
     */
    public FlagsDownSymbol (int flagCount,
                            boolean isIcon,
                            Shape shape)
    {
        super(isIcon, shape, false);
        this.fn = flagCount;
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new FlagsDownSymbol(fn, true, shape);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = initParams(font);

        p.rect = new Rectangle(
                0,
                0,
                (int) Math.ceil(p.rect2.getWidth()),
                (((fn / 2) + ((fn + 1) % 2)) * Math.abs(p.dy))
                + ((fn % 2) * (int) Math.ceil(p.rect1.getHeight())));

        return p;
    }

    //------------//
    // initParams //
    //------------//
    protected MyParams initParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.flag1 = Symbols.SYMBOL_FLAG_1.layout(font);
        p.rect1 = p.flag1.getBounds();
        p.flag2 = Symbols.SYMBOL_FLAG_2.layout(font);
        p.rect2 = p.flag2.getBounds();
        p.dy = (int) Math.rint(p.rect2.getHeight() * 0.5);
        p.align = TOP_LEFT;

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
        Point loc = alignment.translatedPoint(p.align, p.rect, location);

        // We draw from tail to head, double(s) then single if needed
        for (int i = 0; i < (fn / 2); i++) {
            MusicFont.paint(g, p.flag2, loc, p.align);
            loc.y += p.dy;
        }

        if ((fn % 2) != 0) {
            MusicFont.paint(g, p.flag1, loc, p.align);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends BasicSymbol.Params
    {
        //~ Instance fields ----------------------------------------------------

        TextLayout flag1;

        Rectangle2D rect1;

        TextLayout flag2;

        Rectangle2D rect2;

        int dy;

        Alignment align;

    }
}
