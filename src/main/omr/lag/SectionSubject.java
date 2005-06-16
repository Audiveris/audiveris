//-----------------------------------------------------------------------//
//                                                                       //
//                      S e c t i o n S u b j e c t                      //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.lag;

import omr.util.Subject;

/**
 * Interface <code>SectionSubject</code> is a specific {@link Subject}
 * meant for {@link SectionObserver} observers
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface SectionSubject
    extends Subject<SectionSubject, SectionObserver, Section>
{
    /**
     * Special addition to notify observers about a Run entity
     *
     * @param run the Run entity to be passed around
     */
    void notifyObservers (Run run);
}
