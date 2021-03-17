//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P a i r P r e d i c a t e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
 * Interface {@code PairPredicate}
 *
 * @author Hervé Bitteur
 *
 * @param <E> type of entity
 */
public interface PairPredicate<E>
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Evaluates this predicate on the given pair of Inter instances.
     *
     * @param one an entity
     * @param two another entity
     * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
     */
    boolean test (E one,
                  E two);
}
