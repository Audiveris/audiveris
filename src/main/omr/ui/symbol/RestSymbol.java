//----------------------------------------------------------------------------//
//                                                                            //
//                            R e s t S y m b o l                             //
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
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code RestSymbol} implements rest symbols whose decoration
 * uses staff lines as background
 *
 * @author Hervé Bitteur
 */
public class RestSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers ---------------------------------------------

    // The lines
    protected static final BasicSymbol linesSymbol = Symbols.SYMBOL_STAFF_LINES;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // RestSymbol //
    //------------//
    /**
     * Create a RestSymbol (with decoration?) standard size.
     *
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for rest part
     */
    public RestSymbol (Shape shape,
                       boolean decorated,
                       int... codes)
    {
        this(false, shape, decorated, codes);
    }

    //------------//
    // RestSymbol //
    //------------//
    /**
     * Create a RestSymbol (with decoration?).
     *
     * @param isIcon    true for an icon
     * @param shape     the precise shape
     * @param decorated true for a decorated image
     * @param codes     precise code for rest part
     */
    protected RestSymbol (boolean isIcon,
                          Shape shape,
                          boolean decorated,
                          int... codes)
    {
        super(isIcon, shape, decorated, codes);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new RestSymbol(true, shape, decorated, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        // Rest layout
        p.layout = getRestLayout(font);

        Rectangle2D rs = p.layout.getBounds();
        Rectangle2D r;

        if (decorated) {
            // Lines layout
            p.linesLayout = font.layout(linesSymbol.getString());
            r = p.linesLayout.getBounds();

            // Define specific offset
            p.offset = new Point(
                    0,
                    (int) Math.rint(rs.getY() + (rs.getHeight() / 2)));
        } else {
            r = rs;
        }

        p.rect = new Rectangle(
                (int) Math.ceil(r.getWidth()),
                (int) Math.ceil(r.getHeight()));

        return p;
    }

    //---------------//
    // getRestLayout //
    //---------------//
    /**
     * Retrieve the layout of just the rest symbol part, w/o the lines.
     */
    protected TextLayout getRestLayout (MusicFont font)
    {
        return font.layout(getString());
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

        if (decorated) {
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            MusicFont.paint(g, p.linesLayout, loc, AREA_CENTER);
            g.setComposite(oldComposite);

            MusicFont.paint(g, p.layout, loc, BASELINE_CENTER);
        } else {
            MusicFont.paint(g, p.layout, loc, AREA_CENTER);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------//
    // Params //
    //--------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ----------------------------------------------------

        // layout for just rest layout
        // rect for global image (=lines if decorated, rest if not)
        TextLayout linesLayout; // For lines

    }
}
