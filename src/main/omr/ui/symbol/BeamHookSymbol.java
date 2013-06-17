//----------------------------------------------------------------------------//
//                                                                            //
//                        B e a m H o o k S y m b o l                         //
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

/**
 * Class {@code BeamHookSymbol} implements a decorated beam hook symbol
 *
 * @author Hervé Bitteur
 */
public class BeamHookSymbol
        extends BeamSymbol
{
    //~ Constructors -----------------------------------------------------------

    //----------------//
    // BeamHookSymbol //
    //----------------//
    /**
     * Create a BeamHookSymbol
     */
    public BeamHookSymbol ()
    {
        this(false);
    }

    //----------------//
    // BeamHookSymbol //
    //----------------//
    /**
     * Create a BeamHookSymbol
     *
     * @param isIcon true for an icon
     */
    protected BeamHookSymbol (boolean isIcon)
    {
        super(1, isIcon, Shape.BEAM_HOOK);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new BeamHookSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = super.getParams(font);

        p.rect.width = (int) Math.rint(p.rect.width * 0.33);
        p.rect.height = (int) Math.ceil(p.layout.getBounds().getHeight());

        return p;
    }
}
