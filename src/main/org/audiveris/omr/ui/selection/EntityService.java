//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    E n t i t y S e r v i c e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.ui.selection;

import org.audiveris.omr.util.Entity;
import org.audiveris.omr.util.EntityIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bushe.swing.event.EventSubscriber;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Class <code>EntityService</code> handles user selection on top of a given
 * {@link EntityIndex}.
 * <p>
 * The set of events allowed on the service is defined via the constructor.
 * The service may interact with a location service, injected via the constructor.
 * <p>
 * Actual subscriptions and un-subscriptions are dynamically performed via the {@link #connect} and
 * {@link #disconnect} methods.
 *
 * @param <E> precise entity type
 * @author Hervé Bitteur
 */
@SuppressWarnings("unchecked")
public class EntityService<E extends Entity>
        extends SelectionService
        implements EventSubscriber<UserEvent>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(EntityService.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The underlying entity index, if any. */
    protected final EntityIndex<E> index;

    /** The related location service, if any. */
    protected final SelectionService locationService;

    /** Basket of entities selected via location (rectangle/point). */
    protected final List<E> basket = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>EntityService</code> object.
     *
     * @param index           underlying index
     * @param locationService related location service, or null if none
     * @param eventsAllowed   events allowed on the service
     */
    public EntityService (EntityIndex<E> index,
                          SelectionService locationService,
                          Class[] eventsAllowed)
    {
        super(index.getName() + "Service", eventsAllowed);

        this.index = index;
        this.locationService = locationService;
    }

    /**
     * Creates a new <code>EntityService</code> object with no underlying index.
     *
     * @param name            service name
     * @param locationService related location service, or null if none
     * @param eventsAllowed   events allowed on the service
     */
    public EntityService (String name,
                          SelectionService locationService,
                          Class[] eventsAllowed)
    {
        super(name, eventsAllowed);

        index = null;
        this.locationService = locationService;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // connect //
    //---------//
    /**
     * Actually subscribe to relevant events.
     */
    public void connect ()
    {
        // Location
        if (locationService != null) {
            locationService.subscribeStrongly(LocationEvent.class, this);
        }

        // Entities & others
        for (Class<?> eventClass : getEventsAllowed()) {
            subscribeStrongly(eventClass, this);
        }
    }

    //------------//
    // disconnect //
    //------------//
    /**
     * Un-subscribe from relevant events.
     */
    public void disconnect ()
    {
        // Location
        if (locationService != null) {
            locationService.unsubscribe(LocationEvent.class, this);
        }

        // Entities & others
        for (Class<?> eventClass : getEventsAllowed()) {
            unsubscribe(eventClass, this);
        }
    }

    //----------//
    // getIndex //
    //----------//
    /**
     * Report the underlying index of entities
     *
     * @return the index
     */
    public EntityIndex<E> getIndex ()
    {
        return index;
    }

    //-----------------//
    // getMostRelevant //
    //-----------------//
    /**
     * Among the list of selected entities, report the most "relevant" one.
     *
     * @param list the sequence of selected entities
     * @return the chosen entity
     */
    protected E getMostRelevant (List<E> list)
    {
        if (!list.isEmpty()) {
            return list.get(0); // Use first
        } else {
            return null;
        }
    }

    //-------------------//
    // getSelectedEntity //
    //-------------------//
    /**
     * Report the currently selected entity if any, <b>but mind the case where several
     * entities have been selected</b>.
     *
     * @return the current entity or null
     * @see EntityListEvent#getEntity()
     */
    public E getSelectedEntity ()
    {
        return getMostRelevant(getSelectedEntityList());
    }

    //-----------------------//
    // getSelectedEntityList //
    //-----------------------//
    /**
     * Report the currently selected list of entities if any
     *
     * @return the current list or null
     */
    public List<E> getSelectedEntityList ()
    {
        final List<E> list = (List<E>) getSelection(EntityListEvent.class);

        if (list != null) {
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    //-----------------------//
    // handleEntityListEvent //
    //-----------------------//
    /**
     * Interest in EntityList &rArr; location contour of "selected" entity
     *
     * @param listEvent the list event
     */
    protected void handleEntityListEvent (EntityListEvent<E> listEvent)
    {
        final SelectionHint hint = listEvent.hint;

        final E entity = listEvent.getEntity();

        if (entity != null) {
            if (hint == SelectionHint.ENTITY_INIT) {
                if (locationService != null) {
                    locationService.publish(
                            new LocationEvent(
                                    this,
                                    SelectionHint.ENTITY_TRANSIENT,
                                    listEvent.movement,
                                    entity.getBounds()));
                }

                // Use this entity to start a basket
                basket.clear();
                basket.add(entity);
            }
        }
    }

    //---------------//
    // handleIdEvent //
    //---------------//
    /**
     * Interest in Id &rArr; entity
     *
     * @param idEvent the id event
     */
    protected void handleIdEvent (IdEvent idEvent)
    {
        final E entity = index.getEntity(idEvent.getData());
        publish(new EntityListEvent<>(this, idEvent.hint, idEvent.movement, entity));
    }

    //---------------------//
    // handleLocationEvent //
    //---------------------//
    /**
     * Interest in location &rArr; entity basket
     *
     * @param locationEvent the location event
     */
    protected void handleLocationEvent (LocationEvent locationEvent)
    {
        SelectionHint hint = locationEvent.hint;
        MouseMovement movement = locationEvent.movement;
        Rectangle rect = locationEvent.getData();

        if (rect == null) {
            return;
        }

        // Keep only relevant entities in basket
        purgeBasket();

        final List<E> selection = getSelectedEntityList();

        if (hint.isLocation() || (hint.isContext() && selection.isEmpty())) {
            if ((rect.width > 0) && (rect.height > 0)) {
                // Non-degenerated rectangle: look for contained entities
                basket.clear();
                basket.addAll(index.getContainedEntities(rect));
                publish(
                        new EntityListEvent<>(
                                this,
                                SelectionHint.ENTITY_TRANSIENT,
                                movement,
                                basket));
            } else {
                // Just a point: look for most relevant entity
                final Point loc = rect.getLocation();
                final E entity = getMostRelevant(index.getContainingEntities(loc));

                // Update basket
                switch (hint) {
                case LOCATION_INIT:

                    if (entity != null) {
                        basket.clear();
                        basket.add(entity);
                    } else {
                        basket.clear();
                    }

                    break;

                case LOCATION_ADD:

                    if (entity != null) {
                        if (basket.contains(entity)) {
                            basket.remove(entity);
                        } else {
                            basket.add(entity);
                        }
                    }

                    break;

                case CONTEXT_INIT:

                    if (entity != null) {
                        if (!basket.contains(entity)) {
                            basket.clear();
                            basket.add(entity);
                        }
                    } else {
                        basket.clear();
                    }

                    break;

                case CONTEXT_ADD:
                default:
                }

                // Publish basket
                publish(
                        new EntityListEvent<>(
                                this,
                                SelectionHint.ENTITY_TRANSIENT,
                                movement,
                                basket));
            }
        }
    }

    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            if (event instanceof LocationEvent) {
                handleLocationEvent((LocationEvent) event); // Location => enclosed/enclosing entities(s)
            } else if (event instanceof IdEvent) {
                handleIdEvent((IdEvent) event); // Id => indexed entity
            } else if (event instanceof EntityListEvent) {
                handleEntityListEvent((EntityListEvent<E>) event); // List => display contour of (one) entity
            }
        } catch (ConcurrentModificationException cme) {
            // This can happen because of processing being done on EntityIndex...
            // So, just abort the current UI stuff
            throw cme;
        } catch (Throwable ex) {
            logger.warn(getClass().getSimpleName() + " onEvent error " + ex, ex);
        }
    }

    //-------------//
    // purgeBasket //
    //-------------//
    /**
     * Purge basket of no longer relevant entities.
     */
    protected void purgeBasket ()
    {
        // Void by default
    }
}
