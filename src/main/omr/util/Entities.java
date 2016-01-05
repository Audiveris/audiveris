//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         E n t i t i e s                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

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
            ///return o1.getId() - o2.getId(); // When integers
            return IdUtil.compare(o1.getId(), o2.getId()); // When prefix + integers
        }
    };

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // containedEntities //
    //-------------------//
    /**
     * Look up in a collection of Entity instances for <b>all</b> instances contained
     * in a provided rectangle.
     *
     * @param <E>        precise type of entity handled
     * @param collection the collection of entities to be browsed
     * @param rect       the coordinates rectangle
     * @return the entities found, which may be an empty list
     */
    public static <E extends Entity> Set<E> containedEntities (Collection<? extends E> collection,
                                                               Rectangle rect)
    {
        Set<E> set = null;

        for (E entity : collection) {
            final Rectangle bounds = entity.getBounds();

            if ((bounds != null) && rect.contains(bounds)) {
                if (set == null) {
                    set = new LinkedHashSet<E>();
                }

                set.add(entity);
            }
        }

        if (set != null) {
            return set;
        } else {
            return Collections.emptySet();
        }
    }

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
    public static <E extends Entity> Set<E> containedEntities (Iterator<? extends E> iterator,
                                                               Rectangle rect)
    {
        Set<E> set = null;

        while (iterator.hasNext()) {
            E entity = iterator.next();
            final Rectangle bounds = entity.getBounds();

            if ((bounds != null) && rect.contains(bounds)) {
                if (set == null) {
                    set = new LinkedHashSet<E>();
                }

                set.add(entity);
            }
        }

        if (set != null) {
            return set;
        } else {
            return Collections.emptySet();
        }
    }

    //--------------------//
    // containingEntities //
    //--------------------//
    /**
     * Look up in a collection of Entity instances for <b>all</b> instances that
     * contain the provided point.
     *
     * @param <E>        precise type of entity handled
     * @param collection the collection of entities to be browsed
     * @param point      the provided point
     * @return the entities found, which may be an empty list but not null
     */
    public static <E extends Entity> Set<E> containingEntities (Collection<? extends E> collection,
                                                                Point point)
    {
        Set<E> set = null;

        for (E entity : collection) {
            if (entity.contains(point)) {
                if (set == null) {
                    set = new LinkedHashSet<E>();
                }

                set.add(entity);
            }
        }

        if (set != null) {
            return set;
        } else {
            return Collections.emptySet();
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
    public static <E extends Entity> Set<E> containingEntities (Iterator<? extends E> iterator,
                                                                Point point)
    {
        Set<E> set = null;

        while (iterator.hasNext()) {
            E entity = iterator.next();

            if (entity.contains(point)) {
                if (set == null) {
                    set = new LinkedHashSet<E>();
                }

                set.add(entity);
            }
        }

        if (set != null) {
            return set;
        } else {
            return Collections.emptySet();
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
                if (bounds == null) {
                    bounds = entity.getBounds();
                } else {
                    bounds.add(entity.getBounds());
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
