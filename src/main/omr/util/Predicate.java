//----------------------------------------------------------------------------//
//                                                                            //
//                             P r e d i c a t e                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

/**
 * Interface {@code Predicate} is used to specify a filter on a
 * provided entity.
 *
 * @param <E> Specific type of the entity on which the predicate is checked
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
