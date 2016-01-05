//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S la s h e d F l a g S y m b o l                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.glyph.Shape;
import static omr.ui.symbol.Alignment.AREA_CENTER;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code SlashedFlagSymbol} displays a SMALL_FLAG_SLASH.
 *
 * @author Hervé Bitteur
 */
public class SlashedFlagSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The small flag symbol. */
    private final ShapeSymbol flagSymbol = Symbols.getSymbol(Shape.SMALL_FLAG);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SmallFlagSymbol} object.
     *
     * @param isIcon true for an icon
     */
    public SlashedFlagSymbol (boolean isIcon)
    {
        super(isIcon, Shape.SMALL_FLAG_SLASH, false);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new SlashedFlagSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.layout = flagSymbol.layout(font);

        Rectangle2D r = p.layout.getBounds();
        p.rect = new Rectangle((int) Math.ceil(r.getWidth()), (int) Math.ceil(r.getHeight()));
        p.stroke = new BasicStroke(Math.max(1f, p.rect.width / 10f));

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
        Point loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);

        Stroke oldStroke = g.getStroke();
        g.setStroke(p.stroke);
        g.drawLine(
                loc.x - (p.rect.width / 2),
                loc.y + (p.rect.height / 5),
                loc.x + (p.rect.width / 2),
                loc.y - (p.rect.height / 5));
        g.setStroke(oldStroke);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Params //
    //--------//
    private class MyParams
            extends Params
    {
        //~ Instance fields ------------------------------------------------------------------------

        Stroke stroke;
    }
}
