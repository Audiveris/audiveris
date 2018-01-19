//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E n t i t y I n d e x                                     //
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
package org.audiveris.omr.util;

import org.audiveris.omr.ui.selection.EntityService;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
     * Look up the index for all entities contained in the provided rectangle.
     *
     * @param rectangle provided containing rectangle
     * @return the list of contained entities found, perhaps empty but not null
     */
    List<E> getContainedEntities (Rectangle rectangle);

    /**
     * Look up the index for all entities that contain the provided point.
     *
     * @param point the provided points
     * @return the list of all contained entities found, perhaps empty but not null
     */
    List<E> getContainingEntities (Point point);

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
