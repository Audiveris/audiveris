//----------------------------------------------------------------------------//
//                                                                            //
//                          E n d i n g S y m b o l                           //
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

/**
 * Class {@code EndingSymbol} implements a decorated ending symbol
 *
 * @author Hervé Bitteur
 */
public class EndingSymbol
        extends ShapeSymbol
{
    //~ Constructors -----------------------------------------------------------

    //--------------//
    // EndingSymbol //
    //--------------//
    /**
     * Create an EndingSymbol
     */
    public EndingSymbol ()
    {
        this(false);
    }

    //--------------//
    // EndingSymbol //
    //--------------//
    /**
     * Create an EndingSymbol
     *
     * @param isIcon true for an icon
     */
    protected EndingSymbol (boolean isIcon)
    {
        super(isIcon, Shape.ENDING, true); // Decorated
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new EndingSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();
        int il = font.getFontInterline();
        p.rect = new Rectangle(4 * il, il);

        return p;
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params p,
                          Point location,
                          Alignment alignment)
    {
        Point loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);

        g.drawLine(loc.x, loc.y, (loc.x + p.rect.width) - 1, loc.y);
        g.drawLine(
                (loc.x + p.rect.width) - 1,
                loc.y,
                (loc.x + p.rect.width) - 1,
                loc.y + p.rect.height);
    }
}
