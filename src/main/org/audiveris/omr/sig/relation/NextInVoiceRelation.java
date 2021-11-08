//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              N e x t I n V o i c e R e l a t i o n                             //
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
package org.audiveris.omr.sig.relation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>NextInVoiceRelation</code> indicates that the source chord (on left)
 * and the target chord (on right) are in direct sequence
 * (that is with no other chord in between) within the same voice.
 * <p>
 * This relation is preferred to the less powerful {@link SameVoiceRelation}.
 *
 * @see SameVoiceRelation
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "next-in-voice")
public class NextInVoiceRelation
        extends Rhythm
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>NextInVoiceRelation</code> object.
     */
    public NextInVoiceRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        return true;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        return true;
    }
}
