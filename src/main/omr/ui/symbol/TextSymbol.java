//----------------------------------------------------------------------------//
//                                                                            //
//                            T e x t S y m b o l                             //
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

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code TextSymbol} implements a decorated text symbol
 *
 * @author Hervé Bitteur
 */
public class TextSymbol
        extends ShapeSymbol
{
    //~ Instance fields --------------------------------------------------------

    /** The text string to use */
    private final String str;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // TextSymbol //
    //------------//
    /**
     * Create an TextSymbol
     *
     * @param shape the precise shape
     * @param str   the text to draw
     */
    public TextSymbol (Shape shape,
                       String str)
    {
        this(false, shape, str);
    }

    //------------//
    // TextSymbol //
    //------------//
    /**
     * Create an TextSymbol
     *
     * @param isIcon true for an icon
     * @param shape  the precise shape
     * @param str    the text to draw
     */
    protected TextSymbol (boolean isIcon,
                          Shape shape,
                          String str)
    {
        super(isIcon, shape, true); // Decorated
        this.str = str;
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new TextSymbol(true, shape, str);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();

        TextFont textFont = new TextFont(
                (int) Math.rint(font.getSize2D() * 0.62));
        p.layout = textFont.layout(str);

        Rectangle2D r = p.layout.getBounds();
        p.rect = new Rectangle(
                (int) Math.ceil(r.getWidth()),
                (int) Math.ceil(r.getHeight()));

        return p;
    }
}
