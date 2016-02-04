//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          I d E v e n t                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.selection;

/**
 * Class {@code IdEvent} is an event that conveys an entity ID.
 *
 * @author Hervé Bitteur
 */
public class IdEvent
        extends UserEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The selected entity id, which may be null. */
    private final Integer id;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new IdEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin (or null)
     * @param movement the precise mouse movement
     * @param id       the entity id
     */
    public IdEvent (Object source,
                    SelectionHint hint,
                    MouseMovement movement,
                    int id)
    {
        super(source, hint, null);
        this.id = id;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public Integer getData ()
    {
        return id;
    }
}
