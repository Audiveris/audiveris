//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S l o t V o i c e                                       //
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SlotVoice} defines which chord, if any, represents a voice in a given
 * slot.
 * <p>
 * If a voice is still active in a given slot, there is an instance of {@code SlotVoice} for it.
 * Its status is:
 * <ul>
 * <li><b>BEGIN</b> if the voice begins at the slot.
 * <li><b>CONTINUE</b> if the voice started before the slot but is still active.
 * </ul>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(value = XmlAccessType.NONE)
@XmlRootElement(name = "slot-voice")
public class SlotVoice
{
    //~ Enumerations -------------------------------------------------------------------------------

    //--------//
    // Status //
    //--------//
    /**
     * Voice status of a chord with respect to a slot.
     */
    public static enum Status
    {
        /** The chord begins at this slot (it's one of slot incoming chords). */
        BEGIN,
        /** The chord is still active at this slot. */
        CONTINUE;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related chord. */
    @XmlIDREF
    @XmlAttribute
    public final AbstractChordInter chord;

    /** Current status. */
    @XmlAttribute
    public final Status status;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a SlotVoice object.
     *
     * @param chord  implementing chord at this slot
     * @param status tell if the chord is starting or continuing at this slot
     */
    public SlotVoice (AbstractChordInter chord,
                      Status status)
    {
        this.chord = chord;
        this.status = status;
    }

    // For JAXB.
    private SlotVoice ()
    {
        this.chord = null;
        this.status = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("{SlotVoice");

        if (chord != null) {
            sb.append(" Ch#").append(chord.getId());
        }

        if (status != null) {
            sb.append(' ').append(status);
        }

        sb.append('}');

        return sb.toString();
    }
}
