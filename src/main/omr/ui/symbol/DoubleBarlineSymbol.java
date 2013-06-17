//----------------------------------------------------------------------------//
//                                                                            //
//                   D o u b l e B a r l i n e S y m b o l                    //
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
import java.awt.geom.Rectangle2D;

/**
 * Class {@code DoubleBarlineSymbol} displays two thin barlines
 *
 * @author Hervé Bitteur
 */
public class DoubleBarlineSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers ---------------------------------------------

    // Total width, computed from width of thin barline
    private static final double WIDTH_RATIO = 4.5;

    //~ Instance fields --------------------------------------------------------
    // The thin barline symbol
    private final ShapeSymbol thinSymbol = Symbols.getSymbol(
            Shape.THIN_BARLINE);

    //~ Constructors -----------------------------------------------------------
    //---------------------//
    // DoubleBarlineSymbol //
    //---------------------//
    /**
     * Create a DoubleBarlineSymbol
     *
     * @param isIcon true for an icon
     */
    public DoubleBarlineSymbol (boolean isIcon)
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
        return new DoubleBarlineSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();

        p.layout = font.layout(thinSymbol);

        Rectangle2D thinRect = p.layout.getBounds();
        p.rect = new Rectangle(
                (int) Math.ceil(thinRect.getWidth() * WIDTH_RATIO),
                (int) Math.ceil(thinRect.getHeight()));

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
        MusicFont.paint(g, p.layout, loc, TOP_LEFT);
        loc.x += p.rect.width;
        MusicFont.paint(g, p.layout, loc, TOP_RIGHT);
    }
}
