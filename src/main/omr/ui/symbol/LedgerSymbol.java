//----------------------------------------------------------------------------//
//                                                                            //
//                          L e d g e r S y m b o l                           //
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

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code LedgerSymbol} implements a decorated ledger symbol.
 *
 * @author Hervé Bitteur
 */
public class LedgerSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers ---------------------------------------------

    // The head part
    private static final BasicSymbol head = Symbols.getSymbol(
            Shape.NOTEHEAD_BLACK);

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // LedgerSymbol //
    //--------------//
    /**
     * Create a LedgerSymbol (with decoration?) standard size
     *
     * @param decorated true for a decorated image
     */
    public LedgerSymbol (boolean decorated)
    {
        this(false, decorated);
    }

    //--------------//
    // LedgerSymbol //
    //--------------//
    /**
     * Create a LedgerSymbol (with decoration?)
     *
     * @param isIcon    true for an icon
     * @param decorated true for a decorated image
     */
    protected LedgerSymbol (boolean isIcon,
                            boolean decorated)
    {
        super(isIcon, Shape.LEDGER, decorated);
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // getRefPoint //
    //-------------//
    /**
     * Report the symbol reference point at ledger ordinate.
     */
    @Override
    public Point getRefPoint (Rectangle box)
    {
        return new Point(
                box.x + (box.width / 2),
                box.y + (int) Math.rint(box.height * 0.67));
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new LedgerSymbol(true, true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();

        // Head layout
        p.layout = font.layout(head.getString());

        // Use a ledger length twice as large as note head
        Rectangle2D hRect = p.layout.getBounds();
        p.rect = new Rectangle(
                (int) Math.ceil(2 * hRect.getWidth()),
                (int) Math.ceil(hRect.getHeight()));

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
        Point loc = alignment.translatedPoint(
                AREA_CENTER,
                params.rect,
                location);

        if (decorated) {
            // Draw a note head (using composite)
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, params.layout, loc, AREA_CENTER);
            g.setComposite(oldComposite);
        }

        // Ledger
        g.drawLine(
                loc.x - (params.rect.width / 2),
                loc.y,
                loc.x + (params.rect.width / 2),
                loc.y);
    }
}
