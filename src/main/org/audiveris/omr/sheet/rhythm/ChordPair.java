//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C h o r d P a i r                                       //
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.sig.inter.AbstractChordInter;

import java.util.Objects;

/**
 * Class <code>ChordPair</code> defines a pair of chords.
 *
 * @author Hervé Bitteur
 */
public class ChordPair
{

    //~ Instance fields ----------------------------------------------------------------------------
    public final AbstractChordInter one;

    public final AbstractChordInter two;

    //~ Constructors -------------------------------------------------------------------------------
    public ChordPair (AbstractChordInter one,
                      AbstractChordInter two)
    {
        this.one = one;
        this.two = two;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public boolean equals (Object obj)
    {
        if (!(obj instanceof ChordPair)) {
            return false;
        }

        final ChordPair that = (ChordPair) obj;

        return (one == that.one) && (two == that.two);
    }

    @Override
    public int hashCode ()
    {
        int hash = 3;
        hash = (79 * hash) + Objects.hashCode(this.one);
        hash = (79 * hash) + Objects.hashCode(this.two);

        return hash;
    }

    @Override
    public String toString ()
    {
        return "ChordPair{ch#" + one.getId() + ",ch#" + two.getId() + "}";
    }
}
