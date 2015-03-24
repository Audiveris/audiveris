//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    E v e n S t e p S y m b o l                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.glyph.Shape;
import static omr.ui.symbol.Alignment.AREA_CENTER;
import static omr.ui.symbol.Alignment.MIDDLE_LEFT;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code EvenStepSymbol} is the basis for symbols located on an even step position.
 * There is a ledger in the middle of the symbol.
 *
 * @author Hervé Bitteur
 */
public class EvenStepSymbol
        extends ShapeSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new EvenStepSymbol object.
     *
     * @param shape the underlying shape
     * @param codes the codes for MusicFont characters
     */
    public EvenStepSymbol (Shape shape,
                           int... codes)
    {
        super(shape, codes);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        MyParams p = new MyParams();
        p.layout = layout(font);

        Rectangle2D rect = p.layout.getBounds();

        p.rect = new Rectangle((int) Math.ceil(rect.getWidth()), (int) Math.ceil(rect.getHeight()));

        int interline = font.getFontInterline();
        p.line = Math.max(1, (int) Math.rint(interline * 0.17));

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

        // Paint naked symbol
        Point loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);

        // Paint ledger at middle position
        loc = alignment.translatedPoint(MIDDLE_LEFT, p.rect, location);
        g.fillRect(loc.x, loc.y - (p.line / 2), p.rect.width - 1, p.line);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ------------------------------------------------------------------------

        int line; // Thickness of a ledger or staff line
    }
}
