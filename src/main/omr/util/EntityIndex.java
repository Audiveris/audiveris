//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E n t i t y I n d e x                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright ? Herv? Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.ui.selection.EntityService;

import java.util.Collection;
import java.util.Iterator;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code EntityIndex} describes a collection manager, with the ability to
 * retrieve a entity within the collection based on its (strictly positive integer) ID.
 *
 * @param <E> specific type for entity
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(BasicIndex.InterfaceAdapter.class)
public interface EntityIndex<E extends Entity>
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Export the whole unmodifiable collection of entities in the index, sorted on ID.
     *
     * @return the collection of entities
     */
    Collection<E> getEntities ();

    /**
     * Retrieve a entity via its Id.
     *
     * @param id the entity id to search for
     * @return the entity found, or null otherwise
     */
    E getEntity (int id);

    /**
     * Report the entity selection service, if any.
     *
     * @return the entity service, perhaps null
     */
    EntityService<E> getEntityService ();

    /**
     * Report the used id, if any, right after the provided one
     *
     * @param id the provided id
     * @return the next used id, or 0
     */
    int getIdAfter (int id);

    /**
     * Report the used id, if any, right before the provided one
     *
     * @param id the provided id
     * @return the previous used id, or 0
     */
    int getIdBefore (int id);

    /**
     * Report the last ID assigned so far.
     *
     * @return the last ID used so far, 0 if none
     */
    int getLastId ();

    /**
     * Report the distinguished name of this index (for debug mainly).
     *
     * @return name for this index
     */
    String getName ();

    /**
     * Check whether the provided ID has been declared as VIP.
     *
     * @param id the ID to check
     * @return true if id was declared as VIP
     */
    boolean isVipId (int id);

    /**
     * Return an iterator on entities in this index.
     *
     * @return an entity iterator
     */
    Iterator<E> iterator ();

    /**
     * Assign a unique id (within this index) to the provided entity.
     *
     * @param entity the provided entity
     * @return the assigned unique id
     */
    int register (E entity);

    /**
     * Remove the provided entity
     *
     * @param entity the entity to remove
     */
    void remove (E entity);

    /**
     * Reset index internals (entities and last ID value).
     */
    void reset ();

    /**
     * Assign and connect an entity selection service.
     *
     * @param entityService the entity service, not null
     */
    void setEntityService (EntityService<E> entityService);

    /**
     * Reset last ID to the provided value
     *
     * @param lastId ID assigned
     */
    void setLastId (int lastId);
}
