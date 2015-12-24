//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     M y B a s i c I n d e x                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.itf;

import omr.util.Jaxb;

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
    //~ Instance fields ----------------------------------------------------------------------------

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

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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

    //~ Inner Classes ------------------------------------------------------------------------------
    /**
     * Class {@code Snap} is just a flat list of entities, with each item name based on
     * actual item type.
     *
     * @param <E> the specific entity type
     */
    private static class Snap<E extends MyAbstractEntity>
    {
        //~ Instance fields ------------------------------------------------------------------------

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
        //~ Methods --------------------------------------------------------------------------------

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
