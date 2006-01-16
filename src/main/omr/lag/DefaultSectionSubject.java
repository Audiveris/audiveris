//-----------------------------------------------------------------------//
//                                                                       //
//               D e f a u l t S e c t i o n S u b j e c t               //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.lag;

import omr.util.DefaultSubject;
import omr.util.Subject;

/**
 * Class <code>DefaultSectionSubject</code> is an implementation of the
 * specific {@link Subject} meant for {@link SectionObserver} observers
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class DefaultSectionSubject
    extends DefaultSubject<SectionSubject, SectionObserver, Section>
{
    //----------------//
    // notifyObservers //
    //----------------//
    /**
     * Push the Run information to registered observers
     *
     * @param run the Run info to be pushed
     */
    public void notifyObservers (Run run)
    {
        for (SectionObserver observer : observers) {
            observer.update(run);
        }
    }
}
