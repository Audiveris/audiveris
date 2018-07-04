//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          M y G l y p h                                         //
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
package org.audiveris.omr.jaxb.itf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code MyGlyph}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class MyGlyph
        extends MyAbstractEntity
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    private final String name;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MyGlyph} object.
     */
    public MyGlyph ()
    {
        this.name = null;
    }

    /**
     * Creates a new {@code MyGlyph} object.
     *
     * @param name DOCUMENT ME!
     */
    public MyGlyph (String name)
    {
        this.name = name;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("{");
        sb.append(name);
        sb.append(" id:").append(getId());
        sb.append("}");

        return sb.toString();
    }
}
