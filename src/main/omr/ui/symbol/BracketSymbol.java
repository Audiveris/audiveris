//----------------------------------------------------------------------------//
//                                                                            //
//                         B r a c k e t S y m b o l                          //
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
 * Class {@code BracketSymbol} displays a BRACKET symbol: [
 *
 * @author Hervé Bitteur
 */
public class BracketSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers ---------------------------------------------

    // The upper serif
    private static final BasicSymbol upperSymbol = Symbols.SYMBOL_BRACKET_UPPER_SERIF;

    // The lower serif
    private static final BasicSymbol lowerSymbol = Symbols.SYMBOL_BRACKET_LOWER_SERIF;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // BracketSymbol //
    //---------------//
    /**
     * Create a BracketSymbol (which is made of upper and lower parts)
     *
     * @param isIcon true for an icon
     */
    public BracketSymbol (boolean isIcon)
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
        return new BracketSymbol(true);
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
        p.layout = font.layout(Shape.THICK_BARLINE, at);
        p.lowerLayout = font.layout(lowerSymbol.getString(), at);

        Rectangle2D upperRect = p.upperLayout.getBounds();
        Rectangle2D trunkRect = p.layout.getBounds();
        Rectangle2D lowerRect = p.lowerLayout.getBounds();
        p.rect = new Rectangle(
                (int) Math.ceil(upperRect.getWidth()),
                (int) Math.floor(
                upperRect.getHeight() + trunkRect.getHeight()
                + lowerRect.getHeight()));

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
        MusicFont.paint(g, p.layout, loc, MIDDLE_LEFT);
        loc.y -= (p.rect.height / 2);
        MusicFont.paint(g, p.upperLayout, loc, TOP_LEFT);
        loc.y += (2 * (p.rect.height / 2));
        MusicFont.paint(g, p.lowerLayout, loc, BOTTOM_LEFT);
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
