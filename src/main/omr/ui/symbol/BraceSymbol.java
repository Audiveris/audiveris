//----------------------------------------------------------------------------//
//                                                                            //
//                           B r a c e S y m b o l                            //
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code BraceSymbol} displays a BRACE symbol: {
 *
 * @author Hervé Bitteur
 */
public class BraceSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers ---------------------------------------------

    // The upper part symbol
    private static final BasicSymbol upperSymbol = Symbols.SYMBOL_BRACE_UPPER_HALF;

    // The lower part symbol
    private static final BasicSymbol lowerSymbol = Symbols.SYMBOL_BRACE_LOWER_HALF;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // BraceSymbol //
    //-------------//
    /**
     * Create a BraceSymbol (which is mad of upper and lower parts)
     *
     * @param isIcon true for an icon
     */
    public BraceSymbol (boolean isIcon)
    {
        super(isIcon, Shape.BRACE, false);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new BraceSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        AffineTransform at = isIcon ? tiny : null;
        p.upperLayout = font.layout(upperSymbol.getString(), at);
        p.lowerLayout = font.layout(lowerSymbol.getString(), at);

        Rectangle2D r = p.upperLayout.getBounds();
        p.rect = new Rectangle(
                (int) Math.ceil(r.getWidth()),
                (int) Math.ceil(2 * r.getHeight()));

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
        Point loc = alignment.translatedPoint(MIDDLE_LEFT, p.rect, location);
        MusicFont.paint(g, p.upperLayout, loc, BOTTOM_LEFT);
        MusicFont.paint(g, p.lowerLayout, loc, TOP_LEFT);
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------//
    // Params //
    //--------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ----------------------------------------------------

        TextLayout upperLayout;

        TextLayout lowerLayout;

    }
}
