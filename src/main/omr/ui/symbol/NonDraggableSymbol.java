//----------------------------------------------------------------------------//
//                                                                            //
//                    N o n D r a g g a b l e S y m b o l                     //
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code NonDraggableSymbol} implements a double-sized non-draggable
 * symbol: X
 *
 * @author Hervé Bitteur
 */
public class NonDraggableSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers ---------------------------------------------

    private static final AffineTransform at = AffineTransform.getScaleInstance(
            2,
            2);

    //~ Constructors -----------------------------------------------------------
    //--------------------//
    // NonDraggableSymbol //
    //--------------------//
    /**
     * Create an NonDraggableSymbol
     */
    public NonDraggableSymbol (int... codes)
    {
        this(false, codes);
    }

    //--------------------//
    // NonDraggableSymbol //
    //--------------------//
    /**
     * Create an NonDraggableSymbol
     *
     * @param isIcon true for an icon
     */
    protected NonDraggableSymbol (boolean isIcon,
                                  int... codes)
    {
        super(isIcon, Shape.NON_DRAGGABLE, true, codes); // Decorated
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new NonDraggableSymbol(true, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();

        p.layout = font.layout(getString(), at);

        Rectangle2D r = p.layout.getBounds();

        p.rect = new Rectangle(
                (int) Math.ceil(r.getWidth()),
                (int) Math.ceil(r.getHeight()));

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
        Color oldColor = g.getColor();
        g.setColor(Color.RED);
        super.paint(g, p, location, alignment);
        g.setColor(oldColor);
    }
}
