//----------------------------------------------------------------------------//
//                                                                            //
//                         F l a g s U p S y m b o l                          //
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
import omr.ui.symbol.FlagsDownSymbol.MyParams;

/**
 * Class {@code FlagsUpSymbol} displays a pack of several flags up
 *
 * @author Hervé Bitteur
 */
public class FlagsUpSymbol
        extends FlagsDownSymbol
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FlagsUpSymbol object.
     *
     * @param flagCount the number of flags
     * @param isIcon    true for an icon
     * @param shape     the related shape
     */
    public FlagsUpSymbol (int flagCount,
                          boolean isIcon,
                          Shape shape)
    {
        super(flagCount, isIcon, shape);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new FlagsUpSymbol(fn, true, shape);
    }

    //------------//
    // initParams //
    //------------//
    @Override
    protected MyParams initParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.flag1 = Symbols.SYMBOL_FLAG_1_UP.layout(font);
        p.rect1 = p.flag1.getBounds();
        p.flag2 = Symbols.SYMBOL_FLAG_2_UP.layout(font);
        p.rect2 = p.flag2.getBounds();
        p.dy = -(int) Math.rint(p.rect2.getHeight() * 0.5);
        p.align = BOTTOM_LEFT;

        return p;
    }
}
