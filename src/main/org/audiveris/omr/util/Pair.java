//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             P a i r                                            //
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

import java.util.Objects;

/**
 * Class {@code Pair} is a pair of compatible items.
 *
 * @author Hervé Bitteur
 *
 * @param <E> type of pair item
 */
public class Pair<E>
{
    //~ Instance fields ----------------------------------------------------------------------------

    public final E one;

    public final E two;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Pair} object.
     *
     * @param one DOCUMENT ME!
     * @param two DOCUMENT ME!
     */
    public Pair (E one,
                 E two)
    {
        this.one = one;
        this.two = two;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public boolean equals (Object obj)
    {
        if (!(obj instanceof Pair)) {
            return false;
        }

        final Pair that = (Pair) obj;

        return one.equals(that.one) && two.equals(that.two);
    }

    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (97 * hash) + Objects.hashCode(this.one);
        hash = (97 * hash) + Objects.hashCode(this.two);

        return hash;
    }
}
