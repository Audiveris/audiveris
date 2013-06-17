//----------------------------------------------------------------------------//
//                                                                            //
//                              L a g E v e n t                               //
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
 * Class {@code LagEvent} is an abstract class to represent any event
 * related to a lag (section, section id, section set).
 *
 * @author Hervé Bitteur
 */
public abstract class LagEvent
        extends UserEvent
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LagEvent object.
     *
     * @param source   the actual entity that created this event
     * @param hint     how the event originated
     * @param movement the mouse movement
     */
    public LagEvent (Object source,
                     SelectionHint hint,
                     MouseMovement movement)
    {
        super(source, hint, movement);
    }
}
