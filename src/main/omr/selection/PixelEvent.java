//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P i x e l E v e n t                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

/**
 * Class {@code PixelEvent} represent a Pixel Level selection.
 *
 * @author Hervé Bitteur
 */
public class PixelEvent
        extends UserEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The current pixel level, which may be null. */
    private final Integer level;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PixelEvent} object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin
     * @param movement the mouse movement
     * @param level    the selected pixel level (or null)
     */
    public PixelEvent (Object source,
                       SelectionHint hint,
                       MouseMovement movement,
                       Integer level)
    {
        super(source, hint, movement);
        this.level = level;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public Integer getData ()
    {
        return level;
    }
}
