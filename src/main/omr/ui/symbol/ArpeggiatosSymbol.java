//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A r p e g g i a t o s S y m b o l                               //
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
import static omr.ui.symbol.Alignment.TOP_LEFT;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code ArpeggiatosSymbol} displays a column of several arpeggiato.
 * Class is not used.
 *
 * @author Hervé Bitteur
 */
public class ArpeggiatosSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final int count;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ArpeggiatosSymbol object.
     *
     * @param count  the number of arpeggiato
     * @param isIcon true for an icon
     */
    public ArpeggiatosSymbol (int count,
                              boolean isIcon)
    {
        super(isIcon, Shape.ARPEGGIATO, false, 103);
        this.count = count;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new ArpeggiatosSymbol(count, true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();
        p.layout = layout(font);

        Rectangle2D r = p.layout.getBounds();
        p.rect = new Rectangle(
                (int) Math.ceil(r.getWidth()),
                count * (int) Math.rint(r.getHeight()));

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
        Params p = params;
        Point loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
        int dy = (int) Math.rint(p.layout.getBounds().getHeight());

        for (int i = 0; i < count; i++) {
            MusicFont.paint(g, p.layout, loc, TOP_LEFT);
            loc.y += dy;
        }
    }
}
