//----------------------------------------------------------------------------//
//                                                                            //
//                            F l a t S y m b o l                             //
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

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code FlatSymbol} handles a flat or double-flat symbol with a refPoint
 */
public class FlatSymbol
        extends ShapeSymbol
{
    //~ Constructors -----------------------------------------------------------

    //------------//
    // FlatSymbol //
    //------------//
    /**
     * Creates a new FlatSymbol object.
     *
     * @param shape the related shape
     * @param codes the codes for MusicFont characters
     */
    public FlatSymbol (Shape shape,
                       int... codes)
    {
        this(false, shape, codes);
    }

    //------------//
    // FlatSymbol //
    //------------//
    /**
     * Creates a new FlatSymbol object.
     *
     * @param isIcon true for an icon
     * @param shape  the related shape
     * @param codes  the codes for MusicFont characters
     */
    protected FlatSymbol (boolean isIcon,
                          Shape shape,
                          int... codes)
    {
        super(isIcon, shape, false, codes);
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // getRefPoint //
    //-------------//
    /**
     * Report the symbol reference point which is lower than center for flats
     */
    @Override
    public Point getRefPoint (Rectangle box)
    {
        return new Point(
                box.x + (box.width / 2),
                box.y + (int) Math.rint(box.height * 0.67));
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new FlatSymbol(true, shape, codes);
    }
}
