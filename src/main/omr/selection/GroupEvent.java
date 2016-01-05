//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       G r o u p E v e n t                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.glyph.Symbol.Group;

/**
 * Class {@code GroupEvent} represents the selection of a specific glyph group.
 *
 * @author Hervé Bitteur
 */
public class GroupEvent
        extends UserEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The selected group. (Can be null) */
    private final Group group;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new GroupEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin (or null)
     * @param movement the mouse movement
     * @param group    the specific group chosen (perhaps null)
     */
    public GroupEvent (Object source,
                       SelectionHint hint,
                       MouseMovement movement,
                       Group group)
    {
        super(source, hint, movement);
        this.group = group;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public Group getData ()
    {
        return group;
    }
}
