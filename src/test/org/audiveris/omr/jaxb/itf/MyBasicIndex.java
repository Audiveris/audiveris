//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     M y B a s i c I n d e x                                    //
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
package org.audiveris.omr.jaxb.itf;

import org.audiveris.omr.util.Jaxb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code MyBasicIndex}
 *
 * @author Hervé Bitteur
 * @param <E>
 */
@XmlRootElement
public class MyBasicIndex<E extends MyEntity>
{

    /** Index name. */
    @XmlAttribute
    protected final String name;

    /** Global id to uniquely identify an entity instance. */
    @XmlAttribute(name = "last-id")
    @XmlJavaTypeAdapter(Jaxb.AtomicIntegerAdapter.class)
    private AtomicInteger lastId = new AtomicInteger(0);

    /** Collection of all entities registered. */
    @XmlElement(name = "entities")
    @XmlJavaTypeAdapter(SnapAdapter.class)
    protected final ConcurrentHashMap<Integer, E> allEntities = new ConcurrentHashMap<Integer, E>();

    /**
     * Creates a new {@code MyBasicIndex} object.
     *
     * @param name DOCUMENT ME!
     */
    public MyBasicIndex (String name)
    {
        this.name = name;
    }

    /**
     * Creates a new {@code MyBasicIndex} object.
     */
    public MyBasicIndex ()
    {
        this.name = null;
    }

    public Collection<E> getEntities ()
    {
        return new ArrayList<E>(allEntities.values());
    }

    public String getName ()
    {
        return name;
    }

    public void register (E entity)
    {
        int id = entity.getId();

        if (id == 0) {
            entity.setId(id = lastId.incrementAndGet());
        }

        allEntities.put(id, entity);
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("{");
        sb.append(name);

        for (E entity : allEntities.values()) {
            sb.append(" ").append(entity);
        }

        sb.append("}");

        return sb.toString();
    }

    /**
     * Class {@code Snap} is just a flat list of entities, with each item name based on
     * actual item type.
     *
     * @param <E> the specific entity type
     */
    private static class Snap<E extends MyAbstractEntity>
    {

        @XmlElements({
            @XmlElement(name = "glyph", type = MyGlyph.class),
            @XmlElement(name = "symbol", type = MySymbol.class)
        })
        ArrayList<E> list; // Flat list of entities (each with its embedded id)
    }

    /**
     * This adapter converts an un-mappable ConcurrentHashMap<Integer, E> to/from
     * a JAXB-mappable Snap<E> (flat list).
     *
     * @param <E> the specific entity type
     */
    private static class SnapAdapter<E extends MyAbstractEntity>
            extends XmlAdapter<Snap<E>, ConcurrentHashMap<Integer, E>>
    {

        @Override
        public Snap<E> marshal (ConcurrentHashMap<Integer, E> map)
                throws Exception
        {
            Snap<E> snap = new Snap<E>();
            snap.list = new ArrayList<E>(map.values());

            return snap;
        }

        @Override
        public ConcurrentHashMap<Integer, E> unmarshal (Snap<E> snap)
                throws Exception
        {
            ConcurrentHashMap<Integer, E> map = new ConcurrentHashMap<Integer, E>();

            for (E value : snap.list) {
                map.put(value.getId(), value);
            }

            return map;
        }
    }
}
