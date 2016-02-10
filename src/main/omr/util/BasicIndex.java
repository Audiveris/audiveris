//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B a s i c I n d e x                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright � Herv� Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.glyph.BasicGlyph;

import omr.ui.selection.EntityService;
import omr.ui.symbol.BasicSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code BasicIndex}
 *
 * @param <E> precise type for indexed entities
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
@XmlType(propOrder = {
    "lastIdValue", "entities"}
)
public class BasicIndex<E extends Entity>
        implements EntityIndex<E>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            BasicIndex.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Global id used to uniquely identify an entity instance. */
    @XmlAttribute(name = "last-id")
    @XmlJavaTypeAdapter(Jaxb.AtomicIntegerAdapter.class)
    protected final AtomicInteger lastId = new AtomicInteger(0);

    /** Collection of all entities registered in this index, sorted on ID. */
    @XmlElement(name = "entities")
    @XmlJavaTypeAdapter(Adapter.class)
    protected final ConcurrentSkipListMap<Integer, E> entities = new ConcurrentSkipListMap<Integer, E>();

    // Transient data
    //---------------
    //
    /** Selection service, if any. */
    protected EntityService<E> entityService;

    /** List of IDs for declared VIP entities. */
    private List<Integer> vipIds;

    /** (debug) for easy inspection via browser. */
    private Collection<E> values;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BasicIndex} object.
     */
    public BasicIndex ()
    {
        values = entities.values();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getEntities //
    //-------------//
    @Override
    public Collection<E> getEntities ()
    {
        return Collections.unmodifiableCollection(entities.values());
    }

    //-----------//
    // getEntity //
    //-----------//
    @Override
    public E getEntity (int id)
    {
        return entities.get(id);
    }

    //------------------//
    // getEntityService //
    //------------------//
    @Override
    public EntityService<E> getEntityService ()
    {
        return entityService;
    }

    //------------//
    // getIdAfter //
    //------------//
    @Override
    public int getIdAfter (int id)
    {
        if (id == 0) {
            if (!entities.isEmpty()) {
                for (int i = 0, iMax = lastId.get(); i <= iMax; i++) {
                    final E entity = entities.get(i);

                    if (isValid(entity)) {
                        return i;
                    }
                }
            } else {
                return 0;
            }
        }

        for (int i = id + 1, iMax = lastId.get(); i <= iMax; i++) {
            final E entity = entities.get(i);

            if (isValid(entity)) {
                return i;
            }
        }

        return 0;
    }

    //-------------//
    // getIdBefore //
    //-------------//
    @Override
    public int getIdBefore (int id)
    {
        if (id == 0) {
            return 0;
        }

        for (int i = id - 1; i >= 0; i--) {
            final E entity = entities.get(i);

            if (isValid(entity)) {
                return i;
            }
        }

        return 0;
    }

    //-----------//
    // getLastId //
    //-----------//
    @Override
    public int getLastId ()
    {
        return lastId.get();
    }

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "";
    }

    //--------//
    // insert //
    //--------//
    /**
     * Insert an entity with its ID already defined.
     * This method is meant for re-loading an index.
     *
     * @param entity the entity to insert
     */
    public void insert (E entity)
    {
        int id = entity.getId();

        if (id == 0) {
            throw new IllegalArgumentException("Entity has no ID");
        }

        entities.put(id, entity);

        if (isVipId(id)) {
            entity.setVip(true);
            logger.info("VIP insert {}", entity);
        }
    }

    //---------//
    // isVipId //
    //---------//
    @Override
    public boolean isVipId (int id)
    {
        return (vipIds != null) && vipIds.contains(id);
    }

    //----------//
    // Iterator //
    //----------//
    @Override
    public Iterator<E> iterator ()
    {
        return entities.values().iterator();
    }

    //----------//
    // register //
    //----------//
    @Override
    public int register (E entity)
    {
        // Already registered?
        if (entity.getId() != 0) {
            return entity.getId();
        }

        int id = generateId();
        entity.setId(id);

        entities.put(id, entity);

        if (isVipId(id)) {
            entity.setVip(true);
            logger.info("VIP entity {} registered", entity);
        }

        return id;
    }

    //--------//
    // remove //
    //--------//
    @Override
    public void remove (E entity)
    {
        entities.remove(entity.getId());
    }

    //-------//
    // reset //
    //-------//
    @Override
    public void reset ()
    {
        lastId.set(0);
        entities.clear();
    }

    //------------------//
    // setEntityService //
    //------------------//
    @Override
    public void setEntityService (EntityService<E> entityService)
    {
        this.entityService = entityService;
        entityService.connect();
    }

    //-----------//
    // setLastId //
    //-----------//
    @Override
    public void setLastId (int lastId)
    {
        this.lastId.set(lastId);
    }

    //-----------//
    // setVipIds //
    //-----------//
    public void setVipIds (List<Integer> vipIds)
    {
        this.vipIds = vipIds;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append(internals());
        sb.append("}");

        return sb.toString();
    }

    //------------//
    // generateId //
    //------------//
    protected int generateId ()
    {
        return lastId.incrementAndGet();
    }

    //-----------//
    // internals //
    //-----------//
    protected String internals ()
    {
        return getName();
    }

    //---------//
    // isValid //
    //---------//
    /**
     * Report whether the provided entity is valid
     *
     * @param entity the entity to check
     * @return true if entity is valid
     */
    protected boolean isValid (E entity)
    {
        return entity != null;
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this object,
     * but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        values = entities.values();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------------//
    // InterfaceAdapter //
    //------------------//
    public static class InterfaceAdapter<E extends AbstractEntity>
            extends XmlAdapter<BasicIndex<E>, EntityIndex<E>>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public BasicIndex<E> marshal (EntityIndex<E> itf)
                throws Exception
        {
            return (BasicIndex<E>) itf;
        }

        @Override
        public EntityIndex<E> unmarshal (BasicIndex<E> basic)
                throws Exception
        {
            return basic;
        }
    }

    //---------//
    // Adapter //
    //---------//
    /**
     * This adapter converts an un-mappable ConcurrentHashMap<String, E> to/from
     * a JAXB-mappable IndexValue<E> (a flat list).
     *
     * @param <E> the specific entity type
     */
    private static class Adapter<E extends AbstractEntity>
            extends XmlAdapter<IndexValue<E>, ConcurrentSkipListMap<Integer, E>>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public IndexValue<E> marshal (ConcurrentSkipListMap<Integer, E> map)
                throws Exception
        {
            IndexValue<E> value = new IndexValue<E>();
            value.list = new ArrayList<E>(map.values());

            return value;
        }

        @Override
        public ConcurrentSkipListMap<Integer, E> unmarshal (IndexValue<E> value)
                throws Exception
        {
            // TODO: is sorting needed?
            Collections.sort(
                    value.list,
                    new Comparator<E>()
            {
                @Override
                public int compare (E e1,
                                    E e2)
                {
                    return Integer.compare(e1.getId(), e2.getId());
                }
            });

            ConcurrentSkipListMap<Integer, E> map = new ConcurrentSkipListMap<Integer, E>();

            for (E entity : value.list) {
                map.put(entity.getId(), entity);
            }

            return map;
        }
    }

    //------------//
    // IndexValue //
    //------------//
    /**
     * Class {@code IndexValue} is just a flat list of entities, with each item name
     * based on actual item type.
     *
     * @param <E> the specific entity type
     */
    private static class IndexValue<E extends AbstractEntity>
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlElementRefs({
            @XmlElementRef(type = BasicGlyph.class),
            @XmlElementRef(type = BasicSymbol.class)
        })
        ArrayList<E> list; // Flat list of entities (each with its embedded id)
    }
}
