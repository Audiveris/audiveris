//----------------------------------------------------------------------------//
//                                                                            //
//                      B a c k T o B a c k S y m b o l                       //
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
import static omr.glyph.Shape.*;
import static omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code BackToBackSymbol} displays a BACK_TO_BACK_REPEAT_SIGN.
 *
 * @author Hervé Bitteur
 */
public class BackToBackSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers ---------------------------------------------

    // Total width, computed from width of RIGHT_REPEAT_SIGN symbol
    private static final double WIDTH_RATIO = 1.6;

    // Abscissa of thin barline, computed from width of RIGHT_REPEAT_SIGN symbol
    private static final double DX_RATIO = 1.15;

    //~ Instance fields --------------------------------------------------------
    // The RIGHT_REPEAT_SIGN symbol
    private final ShapeSymbol rightSymbol = Symbols.getSymbol(
            RIGHT_REPEAT_SIGN);

    // The THIN_BARLINE symbol
    private final ShapeSymbol thinSymbol = Symbols.getSymbol(THIN_BARLINE);

    // The REPEAT_DOT_PAIR symbol
    private final ShapeSymbol dotsSymbol = Symbols.getSymbol(REPEAT_DOT_PAIR);

    //~ Constructors -----------------------------------------------------------
    //------------------//
    // BackToBackSymbol //
    //------------------//
    /**
     * Create a BackToBackSymbol
     *
     * @param isIcon true for an icon
     */
    public BackToBackSymbol (boolean isIcon)
    {
        super(isIcon, Shape.DOUBLE_BARLINE, false);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new BackToBackSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.layout = font.layout(rightSymbol);
        p.thinLayout = font.layout(thinSymbol);
        p.dotsLayout = font.layout(dotsSymbol);

        Rectangle2D rightRect = p.layout.getBounds();
        p.dx = (int) Math.ceil(rightRect.getWidth() * DX_RATIO);
        p.rect = new Rectangle(
                (int) Math.ceil(rightRect.getWidth() * WIDTH_RATIO),
                (int) Math.ceil(rightRect.getHeight()));

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

        loc.x += p.dx;
        MusicFont.paint(g, p.thinLayout, loc, AREA_CENTER);

        loc.x = location.x + p.rect.width;
        MusicFont.paint(g, p.dotsLayout, loc, MIDDLE_RIGHT);
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ----------------------------------------------------

        TextLayout thinLayout;

        TextLayout dotsLayout;

        int dx;

    }
}
