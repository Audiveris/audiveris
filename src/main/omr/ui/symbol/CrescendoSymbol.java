//----------------------------------------------------------------------------//
//                                                                            //
//                       C r e s c e n d o S y m b o l                        //
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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;

/**
 * Class {@code CrescendoSymbol} displays a crescendo symbol: "<" .
 */
public class CrescendoSymbol
        extends ShapeSymbol
{
    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // CrescendoSymbol //
    //-----------------//
    /**
     * Creates a new CrescendoSymbol object.
     *
     * @param isIcon true for an icon
     * @param shape  the related shape
     */
    public CrescendoSymbol (boolean isIcon,
                            Shape shape)
    {
        super(isIcon, shape, false);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new CrescendoSymbol(true, shape);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        double interline = font.getFontInterline();
        p.rect = new Rectangle(
                (int) Math.ceil(5 * interline),
                (int) Math.ceil(1.5 * interline));
        p.stroke = new BasicStroke(
                Math.max(1f, (float) interline / 7f),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);

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

        Stroke oldStroke = g.getStroke();
        g.setStroke(p.stroke);
        g.drawLine(
                loc.x,
                loc.y,
                loc.x + p.rect.width,
                loc.y - (p.rect.height / 2));
        g.drawLine(
                loc.x,
                loc.y,
                loc.x + p.rect.width,
                loc.y + (p.rect.height / 2));
        g.setStroke(oldStroke);
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------//
    // Params //
    //--------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ----------------------------------------------------

        Stroke stroke;

    }
}
