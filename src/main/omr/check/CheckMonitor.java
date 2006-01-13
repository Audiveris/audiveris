//-----------------------------------------------------------------------//
//                                                                       //
//                        C h e c k M o n i t o r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.check;

/**
 * Interface <code>CheckMonitor</code> defines how checking information can
 * be told to a dedicated monitor. This interface is used to pass check
 * results to the board where such info is displayed.
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface CheckMonitor <T extends Checkable>
{
    /**
     * Pass the object to check
     *
     * @param object the checkable object to check
     */
    void tellObject (T object);
}
