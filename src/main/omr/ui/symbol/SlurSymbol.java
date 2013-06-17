//----------------------------------------------------------------------------//
//                                                                            //
//                            S l u r S y m b o l                             //
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
import java.awt.geom.CubicCurve2D;

/**
 * Class {@code SlurSymbol} implements a decorated slur symbol
 *
 * @author Hervé Bitteur
 */
public class SlurSymbol
        extends ShapeSymbol
{
    //~ Constructors -----------------------------------------------------------

    //------------//
    // SlurSymbol //
    //------------//
    /**
     * Create a SlurSymbol
     */
    public SlurSymbol ()
    {
        this(false);
    }

    //------------//
    // SlurSymbol //
    //------------//
    /**
     * Create a SlurSymbol
     *
     * @param isIcon true for an icon
     */
    protected SlurSymbol (boolean isIcon)
    {
        super(isIcon, Shape.SLUR, true); // Decorated
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new SlurSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();
        int il = font.getFontInterline();
        p.rect = new Rectangle(2 * il, (4 * il) / 3);

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
        Point loc = alignment.translatedPoint(
                TOP_LEFT,
                p.rect,
                location);

        CubicCurve2D curve = new CubicCurve2D.Double(
                loc.x,
                loc.y + p.rect.height,
                loc.x + ((3 * p.rect.width) / 10),
                loc.y + (p.rect.height / 5),
                loc.x + (p.rect.width / 2),
                loc.y,
                loc.x + p.rect.width,
                loc.y);

        // Slur
        g.draw(curve);
    }
}
