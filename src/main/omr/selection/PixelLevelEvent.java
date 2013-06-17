//----------------------------------------------------------------------------//
//                                                                            //
//                       P i x e l L e v e l E v e n t                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

/**
 * Class {@code PixelLevelEvent} represent a Pixel Level selection.
 *
 * @author Hervé Bitteur
 */
public class PixelLevelEvent
        extends UserEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The current pixel level, which may be null */
    private final Integer pixelLevel;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // PixelLevelEvent //
    //-----------------//
    /**
     * Creates a new PixelLevelEvent object.
     *
     * @param source     the entity that created this event
     * @param hint       hint about event origin
     * @param movement   the mouse movement
     * @param pixelLevel the selected pixelLevel (or null)
     */
    public PixelLevelEvent (Object source,
                            SelectionHint hint,
                            MouseMovement movement,
                            Integer pixelLevel)
    {
        super(source, hint, movement);
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
