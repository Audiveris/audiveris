//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C h o r d P a i r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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
 * Class <code>ChordPair</code> defines an oriented pair of chords, used to formalize a mapping
 * from chord 'one' (the rookie) back to a previous chord 'two' (the active),
 * likely to belong to the same voice.
 *
 * @author Hervé Bitteur
 */
public class ChordPair
{
    //~ Instance fields ----------------------------------------------------------------------------

    public final AbstractChordInter rookie;

    public final AbstractChordInter active;

    public Integer cost;

    //~ Constructors -------------------------------------------------------------------------------

    public ChordPair (AbstractChordInter rookie,
                      AbstractChordInter active,
                      Integer cost)
    {
        this.rookie = rookie;
        this.active = active;
        this.cost = cost;
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public boolean equals (Object obj)
    {
        if (!(obj instanceof ChordPair)) {
            return false;
        }

        final ChordPair that = (ChordPair) obj;

        return (rookie == that.rookie) && (active == that.active);
    }

    @Override
    public int hashCode ()
    {
        int hash = 3;
        hash = (79 * hash) + Objects.hashCode(this.rookie);
        hash = (79 * hash) + Objects.hashCode(this.active);

        return hash;
    }

    @Override
    public String toString ()
    {
        return ((cost != null) ? cost : "") + (active != null ? "#" + active.getId() : " null")
                + "--#" + rookie.getId();
    }
}
