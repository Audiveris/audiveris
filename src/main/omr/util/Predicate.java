//----------------------------------------------------------------------------//
//                                                                            //
//                             P r e d i c a t e                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.util;


/**
 * Interface <code>Predicate</code> is used to specify a filter on a provided
 * entity
 * @param E Specific type of the entity on which the predicate is checked
 */
public interface Predicate<E>
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Run a check on the provided entity, and return the result
     *
     * @param entity the entity to check
     * @return true if predicate is true, false otherwise
     */
    boolean check (E entity);
}
