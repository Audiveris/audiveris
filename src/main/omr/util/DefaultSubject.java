//-----------------------------------------------------------------------//
//                                                                       //
//                      D e f a u l t S u b j e c t                      //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.util;

import omr.util.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Class <code>DefaultSubject</code> is a basic implementation of the
 * {@link Subject} interface.
 *
 * @param <S> The type of this subject
 * @param <O> The type of observer
 * @param <E> The type of entity to be passed around to observers
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class DefaultSubject<S extends Subject  <S, O, E>,
                            O extends Observer <E>,
                            E>
    implements Subject<S, O, E>
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(DefaultSubject.class);

    //~ Instance variables ------------------------------------------------

    /** Set of observers to be notified */
    protected Set<O> observers = new HashSet<O>();

    //~ Methods -----------------------------------------------------------

    //------------//
    // addObserver //
    //------------//
    /**
     * Register a new observer for the subject
     *
     * @param observer the obserbing entity
     */
    public void addObserver (O observer)
    {
        if (!observers.add(observer)) {
            logger.error("Adding an Observer already registered");
        }
    }

    //---------------//
    // removeObserver //
    //---------------//
    /**
     * Unregister an observer on the subject
     *
     * @param observer the observing entity to unregister
     */
    public void removeObserver (O observer)
    {
        if (!observers.remove(observer)) {
            logger.error("Trying to remove a non-registered Observer");
        }
    }

    //----------------//
    // notifyObservers //
    //----------------//
    /**
     * Push the subject entity to all registered observers
     *
     * @param entity the subject entity
     */
    public void notifyObservers (E entity)
    {
        for (O observer : observers) {
            observer.update(entity);
        }
    }

    //----------------//
    // countObservers //
    //----------------//
    /**
     * Report the current number of registered ibservers
     *
     * @return the number of observers
     */
    public int countObservers()
    {
        return observers.size();
    }
}
