//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           B a s k e t                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Basket}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "basket")
public class Basket
{
    //~ Instance fields ----------------------------------------------------------------------------

    //    @XmlElementWrapper(name = "apples")
    //    @XmlElement(name = "apple")
    @XmlList
    @XmlIDREF
    ArrayList<Apple> apples = new ArrayList<Apple>();

    //    @XmlElementWrapper(name = "oranges")
    //    @XmlElement(name = "orange")
    @XmlList
    @XmlIDREF
    ArrayList<Orange> oranges = new ArrayList<Orange>();

    //~ Methods ------------------------------------------------------------------------------------
    public void add (Apple apple)
    {
        apples.add(apple);
    }

    public void add (Orange orange)
    {
        oranges.add(orange);
    }
}
