//----------------------------------------------------------------------------//
//                                                                            //
//                          N u m D e n S y m b o l                           //
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
 * Class {@code NumDenSymbol} displays a time sig, with numerator & denominator
 */
public class NumDenSymbol
        extends ShapeSymbol
{
    //~ Instance fields --------------------------------------------------------

    private final int[] numCodes;

    private final int[] denCodes;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // NumDenSymbol //
    //--------------//
    /**
     * Creates a new NumDenSymbol object.
     *
     * @param shape       the related shape
     * @param numerator   the numerator value (not code)
     * @param denominator the denominator value (not code)
     */
    public NumDenSymbol (Shape shape,
                         int numerator,
                         int denominator)
    {
        this(
                shape,
                ShapeSymbol.numberCodes(numerator),
                ShapeSymbol.numberCodes(denominator));
    }

    //--------------//
    // NumDenSymbol //
    //--------------//
    /**
     * Creates a new NumDenSymbol object.
     *
     * @param shape    the related shape
     * @param numCodes the numerator codes
     * @param denCodes the denominator codes
     */
    public NumDenSymbol (Shape shape,
                         int[] numCodes,
                         int[] denCodes)
    {
        this(false, shape, false, numCodes, denCodes);
    }

    //--------------//
    // NumDenSymbol //
    //--------------//
    /**
     * Creates a new NumDenSymbol object.
     *
     * @param isIcon    true for an icon
     * @param shape     the related shape
     * @param decorated true for decoration
     * @param numCodes  the numerator codes
     * @param denCodes  the denominator codes
     */
    public NumDenSymbol (boolean isIcon,
                         Shape shape,
                         boolean decorated,
                         int[] numCodes,
                         int[] denCodes)
    {
        super(isIcon, shape, decorated);
        this.numCodes = numCodes;
        this.denCodes = denCodes;
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new NumDenSymbol(true, shape, decorated, numCodes, denCodes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams(font);

        Rectangle2D numRect = p.numLayout.getBounds();
        Rectangle2D denRect = p.denLayout.getBounds();
        p.rect = new Rectangle(
                (int) Math.rint(Math.max(numRect.getWidth(), denRect.getWidth())),
                p.dy
                + (int) Math.rint(Math.max(numRect.getHeight(), denRect.getHeight())));

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
        Point center = alignment.translatedPoint(
                AREA_CENTER,
                p.rect,
                location);

        Point top = new Point(center.x, center.y - (p.dy / 2));
        OmrFont.paint(g, p.numLayout, top, AREA_CENTER);

        Point bot = new Point(center.x, center.y + (p.dy / 2));
        OmrFont.paint(g, p.denLayout, bot, AREA_CENTER);
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ----------------------------------------------------

        final int dy;

        final TextLayout numLayout;

        final TextLayout denLayout;

        //~ Constructors -------------------------------------------------------
        public MyParams (MusicFont font)
        {
            dy = (int) Math.rint(2 * font.getFontInterline());
            numLayout = font.layout(numCodes);
            denLayout = font.layout(denCodes);
        }
    }
}
