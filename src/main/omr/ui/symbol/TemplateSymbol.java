//----------------------------------------------------------------------------//
//                                                                            //
//                         T e m p l a t e S y m b o l                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.image.Template.Key;
import static omr.ui.symbol.Alignment.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;

/**
 * Class {@code TemplateSymbol} defines a symbol meant only for
 * template matching.
 * <p>
 * TODO: Symbol must depend on interline of course, PLUS line thickness and
 * perhaps stem thickness as well.
 * This implies that a set of templates will likely be defined per page 
 * (interline, line, stem).
 * Beware, ledgers are often a bit thicker than staff lines.
 *
 * @author Hervé Bitteur
 */
public class TemplateSymbol
        extends BasicSymbol
{
    //~ Static fields/initializers ---------------------------------------------

    /** Affine Transform for small shapes. */
    private static final AffineTransform smallAt = AffineTransform.getScaleInstance(
            0.67,
            0.67);

    //~ Instance fields --------------------------------------------------------
    protected final Key key;

    protected final boolean isSmall;

    //~ Constructors -----------------------------------------------------------
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

    //~ Methods ----------------------------------------------------------------
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

        Rectangle2D r = p.layout.getBounds();
        p.rect = new Rectangle(
                (int) Math.rint(r.getWidth()),
                isSmall ? interline : (int) Math.rint(r.getHeight()));

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
        final float shortHeight = p.rect.height * 0.4f;
        final float longHeight = p.rect.height * 0.6f;

        // Background for small shapes
        if (isSmall) {
            final Color oldColor = g.getColor();
            g.setColor(Color.GREEN);
            g.fill(p.rect);
            g.setColor(oldColor);
        }

        // Naked symbol
        Point loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);

        // Line(s)/ledger(s)
        Rectangle2D rect;

        switch (key.lines) {
        case LINE_TOP:
            loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
            g.fill(
                    new Float(loc.x, loc.y - (p.line / 2), p.rect.width, p.line));

            break;

        case LINE_MIDDLE:
            loc = alignment.translatedPoint(MIDDLE_LEFT, p.rect, location);
            g.fill(
                    new Float(loc.x, loc.y - (p.line / 2), p.rect.width, p.line));

            break;

        case LINE_BOTTOM:
            loc = alignment.translatedPoint(BOTTOM_LEFT, p.rect, location);
            g.fill(
                    new Float(loc.x, loc.y - (p.line / 2), p.rect.width, p.line));

            break;

        case LINE_DOUBLE:
            loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
            g.fill(
                    new Float(loc.x, loc.y - (p.line / 2), p.rect.width, p.line));
            loc = alignment.translatedPoint(BOTTOM_LEFT, p.rect, location);
            g.fill(
                    new Float(loc.x, loc.y - (p.line / 2), p.rect.width, p.line));

        default:
        case LINE_NONE:
        }

        // Stem(s) portion(s)
        if (!isSmall) {
            return; // No stem variants with standard size shape
        }

        switch (key.stems) {
        case STEM_LEFT_BOTTOM:
            loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
            g.fill(new Float(loc.x, loc.y + longHeight, p.stem, shortHeight));

            break;

        case STEM_LEFT:
            loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
            g.fill(new Float(loc.x, loc.y, p.stem, p.rect.height));

            break;

        case STEM_RIGHT_TOP:
            loc = alignment.translatedPoint(TOP_RIGHT, p.rect, location);
            g.fill(new Float(loc.x - p.stem, loc.y, p.stem, shortHeight));

            break;

        case STEM_RIGHT:
            loc = alignment.translatedPoint(TOP_RIGHT, p.rect, location);
            g.fill(new Float(loc.x - p.stem, loc.y, p.stem, p.rect.height));

            break;

        case STEM_DOUBLE:
            loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
            g.fill(new Float(loc.x, loc.y + longHeight, p.stem, shortHeight));
            loc = alignment.translatedPoint(TOP_RIGHT, p.rect, location);
            g.fill(new Float(loc.x - p.stem, loc.y, p.stem, shortHeight));

            break;

        default:
        case STEM_NONE:
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {
        //~ Instance fields ----------------------------------------------------

        float line; // Thickness of a ledger or staff line

        float stem; // Thickness of a stem
    }
}
