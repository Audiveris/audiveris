//----------------------------------------------------------------------------//
//                                                                            //
//                         S l a n t e d S y m b o l                          //
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
 * Class {@code SlantedSymbol} draws symbols made of several slanted parts
 * (such as in DYNAMICS_FP, where both the F and the P are slanted and appear
 * too far for each other)
 */
public class SlantedSymbol
        extends ShapeSymbol
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SlantedSymbol //
    //---------------//
    /**
     * Creates a new SlantedSymbol object, standard size
     *
     * @param shape the related shape
     * @param codes the codes for MusicFont characters
     */
    public SlantedSymbol (Shape shape,
                          int... codes)
    {
        this(false, shape, codes);
    }

    //---------------//
    // SlantedSymbol //
    //---------------//
    /**
     * Creates a new SlantedSymbol object.
     *
     * @param isIcon true for icon
     * @param shape  the related shape
     * @param codes  the codes for MusicFont characters
     */
    protected SlantedSymbol (boolean isIcon,
                             Shape shape,
                             int... codes)
    {
        super(isIcon, shape, false, codes);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new SlantedSymbol(true, shape, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.layouts = new SmartLayout[codes.length];

        // Union of all rectangles
        Rectangle2D rect = null;

        // Current abscissa
        float x = 0;

        for (int i = 0; i < codes.length; i++) {
            int code = codes[i];
            TextLayout layout = font.layout(code);
            Rectangle2D r = layout.getBounds();

            // Abscissa reduction because of slanted characters
            // It's value depends on whether we have a 'f' or not
            float dx;
            int c = code - MusicFont.CODE_OFFSET;

            if ((c == 102) || // F
                    (c == 196) || // FF
                    (c == 236) || // FFF
                    (c == 83) || // SF
                    (c == 90)) { // FZ
                dx = (float) r.getHeight() * 0.215f; // Measured
            } else {
                dx = (float) r.getHeight() * 0.075f; // Measured
            }

            p.layouts[i] = new SmartLayout(layout, dx);

            if (i > 0) {
                x -= dx;
            }

            if (i == 0) {
                rect = layout.getPixelBounds(null, x, 0);
            } else {
                Rectangle2D.union(
                        rect,
                        layout.getPixelBounds(null, x, 0),
                        rect);
            }

            x += (r.getWidth() - dx);
        }

        p.rect = new Rectangle(
                (int) Math.floor(rect.getX()),
                (int) Math.floor(rect.getY()),
                (int) Math.ceil(rect.getWidth()),
                (int) Math.ceil(rect.getHeight()));

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

        Point loc = alignment.translatedPoint(
                BASELINE_LEFT,
                p.rect,
                location);

        // Precise abscissa
        float x = loc.x;

        for (int i = 0; i < p.layouts.length; i++) {
            SmartLayout smart = p.layouts[i];

            if (i > 0) {
                x -= smart.dx; // Before symbol
            }

            OmrFont.paint(
                    g,
                    smart.layout,
                    new Point((int) Math.rint(x), loc.y),
                    BASELINE_LEFT);

            x += smart.layout.getBounds()
                    .getWidth();
            x -= smart.dx; // After symbol
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

        // layout not used
        // rect for global image
        // Sequence of layouts
        SmartLayout[] layouts;

    }

    //-------------//
    // SmartLayout //
    //-------------//
    protected static class SmartLayout
    {
        //~ Instance fields ----------------------------------------------------

        // The standard character glyph
        final TextLayout layout;

        // Translation before & after
        final float dx;

        //~ Constructors -------------------------------------------------------
        public SmartLayout (TextLayout layout,
                            float dx)
        {
            this.layout = layout;
            this.dx = dx;
        }
    }
}
