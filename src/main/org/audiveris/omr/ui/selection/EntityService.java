//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    E n t i t y S e r v i c e                                   //
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
package org.audiveris.omr.ui.selection;

import static org.audiveris.omr.ui.selection.SelectionHint.ENTITY_INIT;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.Entity;
import org.audiveris.omr.util.EntityIndex;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;

/**
 * Class {@code EntityService} handles user selection on top of a given
 * {@link EntityIndex}.
 * <p>
 * The set of events allowed on the service is defined via the constructor.
 * The service may interact with a location service, injected via the constructor.
 * <p>
 * Actual subscriptions and un-subscriptions are dynamically performed via the {@link #connect} and
 * {@link #disconnect} methods.
 *
 * @param <E> precise entity type
 *
 * @author Hervé Bitteur
 */
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

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code EntityService} object with no underlying index.
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

    /**
     * Creates a new {@code EntityService} object.
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
    @SuppressWarnings("unchecked")
    public E getSelectedEntity ()
    {
        List<E> list = getSelectedEntityList();

        if ((list == null) || list.isEmpty()) {
            return null;
        }

        return list.get(0); // Use first
        ///return list.get(list.size() - 1); // Use last
    }

    //-----------------------//
    // getSelectedEntityList //
    //-----------------------//
    /**
     * Report the currently selected list of entities if any
     *
     * @return the current list or null
     */
    @SuppressWarnings("unchecked")
    public List<E> getSelectedEntityList ()
    {
        return (List<E>) getSelection(EntityListEvent.class);
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

            ///logger.info("{} {}", this.getClass().getSimpleName(), event);
            if (event instanceof LocationEvent) {
                handleEvent((LocationEvent) event); // Location => enclosed/enclosing entities(s)
            } else if (event instanceof IdEvent) {
                handleEvent((IdEvent) event); // Id => indexed entity
            } else if (event instanceof EntityListEvent) {
                handleEvent((EntityListEvent) event); // List => display contour of (first) entity
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
    // handleEvent //
    //-------------//
    /**
     * Interest in location => entity list
     *
     * @param locationEvent
     */
    private void handleEvent (LocationEvent locationEvent)
    {
        SelectionHint hint = locationEvent.hint;
        MouseMovement movement = locationEvent.movement;
        Rectangle rect = locationEvent.getData();

        ///if (!hint.isLocation() && !hint.isContext()) {
        if (!hint.isLocation()) {
            return;
        }

        if (rect == null) {
            return;
        }

        final Set<E> found;

        if ((rect.width > 0) && (rect.height > 0)) {
            // Non-degenerated rectangle: look for contained entities
            found = Entities.containedEntities(index.iterator(), rect);
        } else {
            // Just a point: look for containing entities
            found = Entities.containingEntities(index.iterator(), rect.getLocation());
        }

        // Publish EntityList
        publish(new EntityListEvent<E>(this, hint, movement, new ArrayList<E>(found)));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Id => entity
     *
     * @param idEvent
     */
    private void handleEvent (IdEvent idEvent)
    {
        final E entity = index.getEntity(idEvent.getData());
        publish(
                new EntityListEvent<E>(
                        this,
                        idEvent.hint,
                        idEvent.movement,
                        (entity != null) ? Arrays.asList(entity) : null));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in EntityList => location contour of "selected" entity
     *
     * @param EntityListEvent
     */
    private void handleEvent (EntityListEvent<E> listEvent)
    {
        final SelectionHint hint = listEvent.hint;

        if ((hint == ENTITY_INIT) && (locationService != null)) {
            final E entity = listEvent.getEntity();

            if (entity != null) {
                locationService.publish(
                        new LocationEvent(this, hint, listEvent.movement, entity.getBounds()));
            }
        }
    }
}
