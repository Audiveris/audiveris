//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P r e d i c a t e                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.util;

/**
 * Interface {@code Predicate} is used to specify a filter on a provided entity.
 *
 * @param <E> Specific type of the entity on which the predicate is checked
 * @author Hervé Bitteur
 */
public interface Predicate<E>
{

    /**
     * Run a check on the provided entity, and return the result
     *
     * @param entity the entity to check
     * @return true if predicate is true, false otherwise
     */
    boolean check (E entity);
}
