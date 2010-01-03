//----------------------------------------------------------------------------//
//                                                                            //
//                       P i x e l L e v e l E v e n t                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;


/**
 * Class <code>PixelLevelEvent</code> represent a Pixel Level selection
 *
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>Picture
 * <dt><b>Subscribers:</b><dd>PixelBoard
 * <dt><b>Readers:</b><dd>
 * </dl>
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class PixelLevelEvent
    extends UserEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The current pixel level, which may be null */
    public final Integer pixelLevel;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // PixelLevelEvent //
    //-----------------//
    /**
     * Creates a new PixelLevelEvent object.
     *
     * @param source the entity that created this event
     * @param pixelLevel the selected pixelLevel (or null)
     */
    public PixelLevelEvent (Object  source,
                            Integer pixelLevel)
    {
        super(source, null, null);
        this.pixelLevel = pixelLevel;
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getData //
    //---------//
    @Override
    public Integer getData ()
    {
        return pixelLevel;
    }
}
