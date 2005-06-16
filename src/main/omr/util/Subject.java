//-----------------------------------------------------------------------//
//                                                                       //
//                             S u b j e c t                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.util;

/**
 * Interface <code>Subject</code> specifies that the class is able to
 * register / unregister and notify instances of {@link Observer} about new
 * subject values.
 *
 * @param <S> precise type of this subject
 * @param <O> precise type for the entity observer
 * @param <E> precise type for the entity to be handed out
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface Subject<S extends Subject  <S, O, E>,
                         O extends Observer <E>,
                         E>
{
    //-------------//
    // addObserver //
    //-------------//
    /**
     * Register an Observer
     *
     * @param observer the concrete observer to register
     */
    void addObserver (O observer);

    //----------------//
    // removeObserver //
    //----------------//
    /**
     * Unregister a (registered) observer
     *
     * @param observer the observer to unregister
     */
    void removeObserver (O observer);

    //-----------------//
    // notifyObservers //
    //-----------------//
    /**
     * Tell all the registered observers about the (new value of) the
     * entity
     *
     * @param entity the entity to be passed around
     */
    void notifyObservers (E entity);

    //----------------//
    // countObservers //
    //----------------//
    /**
     * Report the current number of registered observers
     *
     * @return the number of observers connected
     */
    int countObservers();
}
