//----------------------------------------------------------------------------//
//                                                                            //
//                              L a g E v e n t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;


/**
 * Class <code>LagEvent</code> is an abstract class to represent any event
 * related to a lag (run, section, section id)
 *
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>
 * <dt><b>Subscribers:</b><dd>
 * <dt><b>Readers:</b><dd>
 * </dl>
 * @author Hervé Bitteur
 */
public abstract class LagEvent
    extends UserEvent
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LagEvent object.
     * @param source the actual entity that created this event
     * @param hint how the event originated
     * @param movement the mouse movement
     */
    public LagEvent (Object        source,
                     SelectionHint hint,
                     MouseMovement movement)
    {
        super(source, hint, movement);
    }
}
