//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 C h o r d V o i c e I d T a s k                                //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sig.inter.AbstractChordInter;

/**
 * Class <code>ChordVoiceIdTask</code> handles the preferred voice ID for a chord.
 *
 * @author Hervé Bitteur
 */
public class ChordVoiceIdTask
        extends InterTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Old voice ID. */
    private final Integer oldId;

    /** New voice ID. */
    private final Integer newId;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>ChordVoiceIdTask</code> object.
     *
     * @param chord the chord to modify
     * @param newId the new preferred voice ID
     */
    public ChordVoiceIdTask (AbstractChordInter chord,
                             Integer newId)
    {
        super(chord.getSig(), chord, chord.getBounds(), null, "voice");
        this.newId = newId;

        oldId = chord.getPreferredVoiceId();
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public AbstractChordInter getInter ()
    {
        return (AbstractChordInter) inter;
    }

    @Override
    public void performDo ()
    {
        getInter().setPreferredVoiceId(newId);
    }

    @Override
    public void performUndo ()
    {
        getInter().setPreferredVoiceId(oldId);
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(actionName);
        sb.append(" ").append(inter);
        sb.append(" from \"").append(oldId).append("\"");
        sb.append(" to \"").append(newId).append("\"");

        return sb.toString();
    }
}
