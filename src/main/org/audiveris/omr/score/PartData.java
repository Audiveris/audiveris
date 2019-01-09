//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        P a r t D a t a                                         //
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
package org.audiveris.omr.score;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code PartData} gathers descriptive information at score level for a part.
 *
 * @author Hervé Bitteur
 */
public class PartData
{

    /** Name of the part, such as found in left margin of a part. */
    @XmlAttribute
    public final String name;

    /** Midi Instrument. */
    @XmlAttribute
    public final int program;

    /**
     * Creates a new {@code PartData} object.
     *
     * @param name    part name
     * @param program midi program number
     */
    public PartData (String name,
                     int program)
    {
        this.name = name;
        this.program = program;
    }

    private PartData ()
    {
        name = null;
        program = 0;
    }

    @Override
    public String toString ()
    {
        return "{name:" + name + " program:" + program + "}";
    }
}
