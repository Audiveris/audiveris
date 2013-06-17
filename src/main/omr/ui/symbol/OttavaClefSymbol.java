//----------------------------------------------------------------------------//
//                                                                            //
//                      O t t a v a C l e f S y m b o l                       //
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
import java.awt.geom.Rectangle2D;

/**
 * Class {@code OttavaClefSymbol} displays a clef (bass or treble) with the
 * addition of an ottava (alta or bassa)
 */
public class OttavaClefSymbol
        extends ShapeSymbol
{
    //~ Instance fields --------------------------------------------------------

    // True for alta, false for bassa
    private final boolean isAlta;

    //~ Constructors -----------------------------------------------------------
    //------------------//
    // OttavaClefSymbol //
    //------------------//
    /**
     * Creates a new OttavaClefSymbol object.
     *
     * @param isAlta true for alta, false for bassa
     * @param isIcon true for an icon
     * @param shape  the related shape
     * @param codes  the codes for MusicFont characters
     */
    public OttavaClefSymbol (boolean isAlta,
                             boolean isIcon,
                             Shape shape,
                             int... codes)
    {
        super(isIcon, shape, false, codes);
        this.isAlta = isAlta;
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new OttavaClefSymbol(isAlta, true, shape, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        return new MyParams(font);
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
        Point loc = alignment.translatedPoint(TOP_CENTER, p.rect, location);

        if (isAlta) {
            MusicFont.paint(g, p.ottavaLayout, loc, TOP_CENTER);
            loc.y += p.ottavaRect.getHeight();
            MusicFont.paint(g, p.layout, loc, TOP_CENTER);
        } else {
            MusicFont.paint(g, p.layout, loc, TOP_CENTER);
            loc.y += p.clefRect.getHeight();
            MusicFont.paint(g, p.ottavaLayout, loc, TOP_CENTER);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    private class MyParams
            extends Params
    {
        //~ Instance fields ----------------------------------------------------

        final TextLayout ottavaLayout;

        final Rectangle2D ottavaRect;

        final Rectangle2D clefRect;

        //~ Constructors -------------------------------------------------------
        public MyParams (MusicFont font)
        {
            ottavaLayout = Symbols.SYMBOL_OTTAVA.layout(font);
            ottavaRect = ottavaLayout.getBounds();

            layout = font.layout(codes);
            clefRect = layout.getBounds();

            rect = new Rectangle(
                    (int) Math.ceil(clefRect.getWidth()),
                    (int) Math.ceil(ottavaRect.getHeight() + clefRect.getHeight()));
        }
    }
}
