//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S e t O p e r a t i o n                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for operations on Sets.
 *
 * @author Peter Greth
 */
public abstract class SetOperation
{
    /**
     * The union of two sets.
     * Returns all elements that are in any of the two sets.
     */
    public static <T> Set<T> union (Set<T> a, Set<T> b)
    {
        Set<T> unionSet = new HashSet<>();
        unionSet.addAll(a);
        unionSet.addAll(b);
        return unionSet;
    }

    /**
     * The intersection of two sets.
     * Returns only those elements that are in both of the sets.
     */
    @SuppressWarnings("CollectionAddAllCanBeReplacedWithConstructor")
    public static <T> Set<T> intersection (Set<T> a, Set<T> b)
    {
        Set<T> unionSet = new HashSet<>();
        unionSet.addAll(a);
        unionSet.retainAll(b);
        return unionSet;
    }

    /**
     * The diff of two sets.
     * Returns only those elements of set a that are not in set b.
     */
    @SuppressWarnings("CollectionAddAllCanBeReplacedWithConstructor")
    public static <T> Set<T> diff (Set<T> a, Set<T> b)
    {
        Set<T> diffSet = new HashSet<>();
        diffSet.addAll(a);
        diffSet.removeAll(b);
        return diffSet;
    }

}
