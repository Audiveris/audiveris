//-----------------------------------------------------------------------//
//                                                                       //
//                           P r e d i c a t e                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.util;

/**
 * Interface <code>Predicate</code> is used to specify a filter on a
 * provided entity
 */
public interface Predicate <E>
{
    /**
     * Run a check on the provided entity, and return the result
     *
     * @param entity the entity to check
     * @return true if predicate is true, false otherwise
     */
    boolean check (E entity);
}
