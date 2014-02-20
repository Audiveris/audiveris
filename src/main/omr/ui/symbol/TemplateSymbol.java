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
import omr.image.Template.Key;
import static omr.ui.symbol.Alignment.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code TemplateSymbol} defines a symbol meant only for template matching.
 * <p>
 * TODO: Symbol must depend on interline of course, PLUS line thickness and perhaps stem thickness
 * as well.
 * This implies that a set of templates will likely be defined per page (interline, line, stem).
 * Beware, ledgers are often a bit thicker than staff lines.
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
    protected final Key key;

    protected final boolean isSmall;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TemplateSymbol object.
     *
     * @param key   specification key for the template
     * @param codes the codes for MusicFont characters
     */
    public TemplateSymbol (Key key,
                           int... codes)
    {
        super(false, codes);
        this.key = key;
        isSmall = key.shape.isSmall();
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

        if (isSmall) {
            p.layout = font.layout(getString(), smallAt);
            p.stem = interline * 0.10f;
        } else {
            p.layout = layout(font);
            p.stem = interline * 0.15f;
        }

        p.line = interline * 0.2f;

        final Rectangle2D r = p.layout.getBounds();
        final int symWidth = (int) Math.rint(r.getWidth());
        final int symHeight = (int) Math.rint(r.getHeight());
        p.rect = new Rectangle(symWidth, isSmall ? interline : symHeight);
        p.symbolRect = new Rectangle(0, (interline - symHeight) / 2, symWidth, symHeight);

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

        if ((key.shape == Shape.WHOLE_NOTE_SMALL) || (key.shape == Shape.WHOLE_NOTE)) {
            g.fill(
                    new Rectangle2D.Float(
                            p.rect.x,
                            p.rect.y + (p.line / 2),
                            p.rect.width,
                            p.rect.height - p.line));
        } else {
            g.fill(
                    new Rectangle2D.Float(
                            p.rect.x + (p.stem / 2),
                            p.rect.y + (p.line / 2),
                            p.rect.width - p.stem,
                            p.rect.height - p.line));
        }

        g.setColor(Color.BLACK);

        // Naked symbol
        Point loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);

        // Middle line?
        if (key.hasLine) {
            loc = alignment.translatedPoint(MIDDLE_LEFT, p.rect, location);
            g.fill(new Rectangle2D.Float(loc.x, loc.y - (p.line / 2), p.rect.width, p.line));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ------------------------------------------------------------------------

        Rectangle symbolRect; // Bounds for symbol inside image

        float line; // Thickness of a ledger or staff line

        float stem; // Thickness of a stem
    }
}
