//----------------------------------------------------------------------------//
//                                                                            //
//                   H e a d F l a g s D o w n S y m b o l                    //
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
import omr.score.entity.Chord;
import static omr.ui.symbol.Alignment.*;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Class {@code HeadFlagsDownSymbol} displays one head combined with several
 * flags down.
 *
 * <p>TODO: With an image opening operation, we should be able to separate the
 * head part from the flag(s) part. This would save us this awkward symbol, and
 * would allow to support a pack of several heads combined with a pack of flags.
 *
 * @author Hervé Bitteur
 */
public class HeadFlagsDownSymbol
    extends ShapeSymbol
{
    //~ Static fields/initializers ---------------------------------------------

    // The head part
    protected static final ShapeSymbol head = Symbols.getSymbol(
        Shape.NOTEHEAD_BLACK);

    //~ Instance fields --------------------------------------------------------

    /** The number of flags */
    protected int fn;

    /** The flags part */
    protected ShapeSymbol flags;

    //~ Constructors -----------------------------------------------------------

    //---------------------//
    // HeadFlagsDownSymbol //
    //---------------------//
    /**
     * Create a HeadFlagsDownSymbol
     *
     * @param flagCount the number of flags
     * @param isIcon true for an icon
     * @param shape the related shape
     */
    public HeadFlagsDownSymbol (int     flagCount,
                                boolean isIcon,
                                Shape   shape)
    {
        super(isIcon, shape, false);
        fn = flagCount;

        Shape flagShape = Chord.getFlagShape(flagCount, false); // not up
        flags = Symbols.getSymbol(flagShape);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.font = font;

        Dimension headDim = head.getDimension(font);
        Dimension flagsDim = flags.getDimension(font);

        // Shorten a bit the flags height
        int flagsHeight = (int) Math.rint(
            flagsDim.height - (headDim.height * 0.15));

        p.rect = new Rectangle(
            Math.max(headDim.width, flagsDim.width),
            flagsHeight + headDim.height);
        p.dy = flagsHeight;

        return p;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new HeadFlagsDownSymbol(fn, true, shape);
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
        PixelPoint loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);

        // Flags, top down
        flags.paintSymbol(g, p.font, loc, TOP_LEFT);

        // Head (Head is always painted TOP_LEFT)
        loc.y += p.dy;
        head.paintSymbol(g, p.font, loc, TOP_LEFT);
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------//
    // MyParams //
    //----------//
    protected class MyParams
        extends Params
    {
        //~ Instance fields ----------------------------------------------------

        MusicFont font;
        int       dy;
    }
}
