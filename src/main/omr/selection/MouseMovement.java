//----------------------------------------------------------------------------//
//                                                                            //
//                         M o u s e M o v e m e n t                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.selection;


/**
 * Class <code>MouseMovement</code> defines which phase of user action generated
 * the event. For example, we may decide to update minimal information while the
 * user is moving the mouse, and launch expensive processing only when the user
 * is releasing the mouse
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum MouseMovement {
    /**
     * User presses the mouse button down, or manually enters the location
     * data in a dedicated board
     */
    PRESSING,
    /**
     * User moves the mouse while keeping the button down
     */
    DRAGGING, 
    /**
     * User releases the mouse button
     */
    RELEASING;
}
