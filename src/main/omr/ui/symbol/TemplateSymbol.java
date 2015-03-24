//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T e m p l a t e S y m b o l                                  //
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

import omr.image.Template;
import static omr.ui.symbol.Alignment.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code TemplateSymbol} defines a symbol meant only for template matching.
 * <p>
 * <b>BEWARE:</b>Don't use it for simple display, use a ShapeSymbol of proper shape instead.
 *
 * @author Hervé Bitteur
 */
public class TemplateSymbol
        extends BasicSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Affine Transform for small symbol shapes. */
    private static final AffineTransform smallAt = AffineTransform.getScaleInstance(
            Template.smallRatio,
            Template.smallRatio);

    //~ Instance fields ----------------------------------------------------------------------------
    protected final Shape shape;

    protected final boolean isSmall;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TemplateSymbol object.
     *
     * @param shape shape for the template
     * @param codes the codes for MusicFont characters
     */
    public TemplateSymbol (Shape shape,
                           int... codes)
    {
        super(false, codes);
        this.shape = shape;
        isSmall = shape.isSmall();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // getSymbolBounds //
    //-----------------//
    public Rectangle getSymbolBounds (MusicFont font)
    {
        return getParams(font).symbolRect;
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        final MyParams p = new MyParams();
        final int interline = font.getFontInterline();

        final TextLayout stdLayout = layout(font);
        final int stdWidth = (int) Math.ceil(stdLayout.getBounds().getWidth());

        if (isSmall) {
            p.layout = font.layout(getString(), smallAt);
        } else {
            p.layout = stdLayout;
        }

        final Rectangle2D r = p.layout.getBounds();
        final int symWidth = (int) Math.ceil(r.getWidth());
        final int symHeight = (int) Math.ceil(r.getHeight());
        p.rect = new Rectangle(isSmall ? stdWidth : symWidth, isSmall ? interline : symHeight);
        p.symbolRect = new Rectangle(
                (int) Math.rint((stdLayout.getBounds().getWidth() - symWidth) / 2),
                (int) Math.rint((stdLayout.getBounds().getHeight() - symHeight) / 2),
                symWidth,
                symHeight);

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
        final MyParams p = (MyParams) params;

        // Background
        g.setColor(Color.RED);
        g.fill(p.rect);

        // Naked symbol
        g.setColor(Color.BLACK);

        Point loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ------------------------------------------------------------------------

        Rectangle symbolRect; // Bounds for symbol inside template image
    }
}
