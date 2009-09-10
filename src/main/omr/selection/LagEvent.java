//----------------------------------------------------------------------------//
//                                                                            //
//                              L a g E v e n t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
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
 * @author Herv&eacute Bitteur
 * @version $Id$
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
