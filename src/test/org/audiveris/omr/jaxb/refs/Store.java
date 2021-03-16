//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            S t o r e                                           //
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
package org.audiveris.omr.jaxb.refs;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Store}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement
public class Store
{

    // Containment to define all IDs
    @XmlElementWrapper(name = "fruits")
    @XmlElementRef
    //    @XmlElements({
    //        ///@XmlElement(name = "apple", type = Apple.class),
    //        @XmlElement(type = Apple.class),
    //        ///@XmlElement(name = "orange", type = Orange.class)
    //        @XmlElement(type = Orange.class)
    //    })
    ArrayList<Fruit> fruits = new ArrayList<Fruit>();

    /**
     * Creates a new {@code Store} object.
     */
    public Store ()
    {
    }

    public void add (Apple apple)
    {
        fruits.add(apple);
    }

    public void add (Orange orange)
    {
        fruits.add(orange);
    }
}
