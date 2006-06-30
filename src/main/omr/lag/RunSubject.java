//-----------------------------------------------------------------------//
//                                                                       //
//                          R u n S u b j e c t                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.lag;

import omr.util.Subject;

/**
 * Interface <code>RunSubject</code> is a specific {@link Subject}
 * meant for {@link RunObserver} observers
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface RunSubject
    extends Subject<RunSubject, RunObserver, Run>
{
}
