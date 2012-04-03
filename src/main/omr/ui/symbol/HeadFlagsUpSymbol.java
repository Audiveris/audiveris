//----------------------------------------------------------------------------//
//                                                                            //
//                     H e a d F l a g s U p S y m b o l                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.glyph.Shape;

import omr.score.common.PixelPoint;
import omr.score.entity.Chord;
import static omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;

/**
 * Class {@code HeadFlagsUpSymbol} displays one head combined with several
 * flags up
 *
 * @author Hervé Bitteur
 */
public class HeadFlagsUpSymbol
    extends HeadFlagsDownSymbol
{
    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // HeadFlagsUpSymbol //
    //-------------------//
    /**
     * Create a HeadFlagsUpSymbol
     *
     * @param flagCount the number of flags
     * @param isIcon true for an icon
     * @param shape the related shape
     */
    public HeadFlagsUpSymbol (int     flagCount,
                              boolean isIcon,
                              Shape   shape)
    {
        super(flagCount, isIcon, shape);

        Shape flagShape = Chord.getFlagShape(flagCount, true); // up
        flags = Symbols.getSymbol(flagShape);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new HeadFlagsUpSymbol(fn, true, shape);
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
            BOTTOM_LEFT,
            p.rect,
            location);

        // Flags, bottom up
        flags.paintSymbol(g, p.font, loc, BOTTOM_LEFT);

        // Head (Head is always painted TOP_LEFT)
        loc.y -= p.rect.height;
        head.paintSymbol(g, p.font, loc, TOP_LEFT);
    }
}
