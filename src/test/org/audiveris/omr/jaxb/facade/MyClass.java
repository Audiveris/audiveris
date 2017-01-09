//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          M y C l a s s                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.jaxb.facade;

import org.audiveris.omr.util.Jaxb;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code MyClass}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class MyClass
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    public final String name;

    @XmlElement
    public final Point origin;

    @XmlElement
    public final Rectangle box;

    /** Global id to uniquely identify an entity instance. */
    @XmlAttribute(name = "last-id")
    @XmlJavaTypeAdapter(Jaxb.AtomicIntegerAdapter.class)
    public final AtomicInteger lastId = new AtomicInteger(0);

    /** Collection of all entities registered. */
    @XmlElementWrapper(name = "entities")
    public final Map<Integer, String> allEntities = new ConcurrentHashMap<Integer, String>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MyClass} object.
     */
    public MyClass ()
    {
        this.name = null;
        this.origin = null;
        this.box = null;
    }

    /**
     * Creates a new {@code MyClass} object.
     *
     * @param name   DOCUMENT ME!
     * @param origin DOCUMENT ME!
     * @param box    DOCUMENT ME!
     */
    public MyClass (String name,
                    Point origin,
                    Rectangle box)
    {
        this.name = name;
        this.origin = origin;
        this.box = box;
    }
}
