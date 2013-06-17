//----------------------------------------------------------------------------//
//                                                                            //
//                     D e c r e s c e n d o S y m b o l                      //
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
import java.awt.Stroke;

/**
 * Class {@code DecrescendoSymbol} displays a decrescendo symbol: ">".
 */
public class DecrescendoSymbol
        extends CrescendoSymbol
{
    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // DecrescendoSymbol //
    //-------------------//
    /**
     * Creates a new DecrescendoSymbol object.
     *
     * @param isIcon true for an icon
     * @param shape  the related shape
     */
    public DecrescendoSymbol (boolean isIcon,
                              Shape shape)
    {
        super(isIcon, shape);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new DecrescendoSymbol(true, shape);
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
        Point loc = alignment.translatedPoint(
                MIDDLE_RIGHT,
                p.rect,
                location);

        Stroke oldStroke = g.getStroke();
        g.setStroke(p.stroke);
        g.drawLine(
                loc.x,
                loc.y,
                loc.x - p.rect.width,
                loc.y - (p.rect.height / 2));
        g.drawLine(
                loc.x,
                loc.y,
                loc.x - p.rect.width,
                loc.y + (p.rect.height / 2));
        g.setStroke(oldStroke);
    }
}
