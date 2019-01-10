//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           O r a n g e                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.jaxb.refs;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Orange}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "orange")
public class Orange
        extends Fruit
{

    @XmlAttribute
    final String name;

    /**
     * Creates a new {@code Orange} object.
     *
     * @param id   DOCUMENT ME!
     * @param name DOCUMENT ME!
     */
    public Orange (String id,
                   String name)
    {
        super(id);
        this.name = name;
    }

    /**
     * Creates a new {@code Orange} object.
     */
    public Orange ()
    {
        this.name = null;
    }
}
