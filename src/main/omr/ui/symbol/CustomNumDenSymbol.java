//----------------------------------------------------------------------------//
//                                                                            //
//                    C u s t o m N u m D e n S y m b o l                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.glyph.Shape;

import omr.score.common.PixelPoint;
import static omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextLayout;

/**
 *  Class {@code CustomNumDenSymbol} displays a custom time signature, with
 * just the N and D letters
 */
public class CustomNumDenSymbol
    extends ShapeSymbol
{
    //~ Constructors -----------------------------------------------------------

    //--------------------//
    // CustomNumDenSymbol //
    //--------------------//
    /**
     * Creates a new CustomNumDenSymbol object, standard size
     */
    public CustomNumDenSymbol ()
    {
        this(false);
    }

    //--------------------//
    // CustomNumDenSymbol //
    //--------------------//
    /**
     * Creates a new CustomNumDenSymbol object.
     *
     * @param isIcon true for icon
     */
    protected CustomNumDenSymbol (boolean isIcon)
    {
        super(isIcon, Shape.CUSTOM_TIME_SIGNATURE, true);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        TextFont textFont = new TextFont(
            (int) Math.rint(font.getSize2D() * 0.62));
        p.nLayout = textFont.layout("N");
        p.dLayout = textFont.layout("D");
        p.rect = new Rectangle(
            (int) Math.ceil(p.nLayout.getBounds().getWidth()),
            (int) Math.ceil(p.nLayout.getBounds().getHeight() * 2.2));

        return p;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new CustomNumDenSymbol(true);
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params     params,
                          PixelPoint location,
                          Alignment  alignment)
    {
        MyParams   p = (MyParams) params;

        PixelPoint loc = alignment.translatedPoint(
            TOP_CENTER,
            p.rect,
            location);
        OmrFont.paint(g, p.nLayout, loc, TOP_CENTER);

        loc.y += p.rect.height;
        OmrFont.paint(g, p.dLayout, loc, BOTTOM_CENTER);
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

        // Layout for N
        TextLayout nLayout;

        // Layout for D
        TextLayout dLayout;
    }
}
