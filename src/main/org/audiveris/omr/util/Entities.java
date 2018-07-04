//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         E n t i t i e s                                        //
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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code Entities}
 *
 * @author Hervé Bitteur
 */
public class Entities
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** To compare Entity instances according to their id. */
    public static final Comparator<Entity> byId = new Comparator<Entity>()
    {
        @Override
        public int compare (Entity o1,
                            Entity o2)
        {
            return o1.getId() - o2.getId();
        }
    };

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // containedEntities //
    //-------------------//
    /**
     * Look up in an iteration on Entity instances for <b>all</b> instances contained
     * in a provided rectangle.
     *
     * @param <E>      precise type of entity handled
     * @param iterator the iterator on the collection of entities to be browsed
     * @param rect     the coordinates rectangle
     * @return the entities found, which may be an empty list
     */
    public static <E extends Entity> List<E> containedEntities (Iterator<? extends E> iterator,
                                                                Rectangle rect)
    {
        List<E> list = null;

        while (iterator.hasNext()) {
            E entity = iterator.next();
            final Rectangle bounds = entity.getBounds();

            if ((bounds != null) && rect.contains(bounds)) {
                if (list == null) {
                    list = new ArrayList<E>();
                }

                list.add(entity);
            }
        }

        if (list != null) {
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    //--------------------//
    // containingEntities //
    //--------------------//
    /**
     * Look up in an iteration on a collection of Entity instances for <b>all</b>
     * instances that contain the provided point.
     *
     * @param <E>      precise type of entity handled
     * @param iterator the iterator on the collection of entities to be browsed
     * @param point    the provided point
     * @return the entities found, which may be an empty list but not null
     */
    public static <E extends Entity> List<E> containingEntities (Iterator<? extends E> iterator,
                                                                 Point point)
    {
        List<E> list = null;

        while (iterator.hasNext()) {
            E entity = iterator.next();

            if (entity.contains(point)) {
                if (list == null) {
                    list = new ArrayList<E>();
                }

                list.add(entity);
            }
        }

        if (list != null) {
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the bounding box of the provided collection of entities.
     *
     * @param entities the provided entities
     * @return the bounding box, or null if no entity was provided
     */
    public static Rectangle getBounds (Collection<? extends Entity> entities)
    {
        Rectangle bounds = null;

        if ((entities != null) && !entities.isEmpty()) {
            for (Entity entity : entities) {
                if (entity != null) {
                    if (bounds == null) {
                        bounds = entity.getBounds();
                    } else {
                        bounds.add(entity.getBounds());
                    }
                }
            }
        }

        return bounds;
    }

    //-----//
    // ids //
    //-----//
    /**
     * Build a string with just the ids of the entity collection.
     *
     * @param entities the collection of Entity instances
     * @return the string built
     */
    public static String ids (Collection<? extends Entity> entities)
    {
        if (entities == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (Entity entity : entities) {
            sb.append("#").append(entity.getId());
        }

        sb.append("]");

        return sb.toString();
    }
}
