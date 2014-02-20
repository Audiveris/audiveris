//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I n t e r I d E v e n t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

/**
 * Class {@code InterIdEvent} represents an Inter Id selection.
 *
 * @author Hervé Bitteur
 */
public class InterIdEvent
        extends UserEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The selected glyph id, which may be null */
    private final Integer id;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new InterIdEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin (or null)
     * @param movement the precise mouse movement
     * @param id       the glyph id
     */
    public InterIdEvent (Object source,
                         SelectionHint hint,
                         MouseMovement movement,
                         Integer id)
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
