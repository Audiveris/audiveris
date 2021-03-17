//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S e l e c t i o n S e r v i c e                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;

import org.bushe.swing.event.EventSubscriber;
import org.bushe.swing.event.ThreadSafeEventService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Class {@code SelectionService} is an OMR customized version of an EventService as
 * provided by the EventBus framework, to handle one or several UserEvent classes.
 * <p>
 * Because the same service can be used to convey different sub-classes of UserEvent, we cannot
 * parameterize the SelectionService class.
 *
 * @author Hervé Bitteur
 */
@SuppressWarnings("unchecked")
public class SelectionService
        extends ThreadSafeEventService
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SelectionService.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Name of this service. */
    private final String name;

    /** Allowed events. */
    private final Class[] eventsAllowed;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SelectionService object.
     *
     * @param name          a name for this service (meant for debug)
     * @param eventsAllowed classes of events that can be published here
     */
    public SelectionService (String name,
                             Class[] eventsAllowed)
    {
        this.name = name;
        this.eventsAllowed = eventsAllowed;

        // Cache needed to be able at any time to retrieve the last publication of any event class
        setDefaultCacheSizePerClassOrTopic(1);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // dumpSubscribers //
    //-----------------//
    /**
     * Dump all current subscribers to this service.
     */
    public void dumpSubscribers ()
    {
        logger.info("{} subscribers:", this);

        for (Class<?> eventClass : eventsAllowed) {
            List<?> subscribers = getSubscribers(eventClass);

            if (!subscribers.isEmpty()) {
                UserEvent last = (UserEvent) getLastEvent(eventClass);
                logger.info(
                        "   {}: {}{}",
                        eventClass.getSimpleName(),
                        subscribers.size(),
                        (last != null) ? (" " + last) : "");

                for (Object obj : subscribers) {
                    String size = "";

                    if (obj instanceof EntityService) {
                        EntityService service = (EntityService) obj;
                        size = "size:" + service.getIndex().getEntities().size();
                    }

                    logger.info(String.format("      @%8h %s %s", obj, obj, size));
                }
            }
        }
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report service name.
     *
     * @return service name
     */
    public String getName ()
    {
        return name;
    }

    //--------------//
    // getSelection //
    //--------------//
    /**
     * Report the current selection regarding the specified event class
     *
     * @param classe the event class we are interested in
     * @return the carried data, if any
     */
    public Object getSelection (Class<?> classe)
    {
        UserEvent event = (UserEvent) getLastEvent(classe);

        if (event == null) {
            return null;
        } else {
            return event.getData();
        }
    }

    //---------//
    // publish //
    //---------//
    /**
     * This method is overridden just to be able to potentially check
     * and trace every publication.
     *
     * @param event the published event
     */
    @Override
    public void publish (Object event)
    {
        logger.debug("{}: published {}", this, event);

        // Check whether the event may be published on this service
        if (!constants.checkPublishedEvents.isSet() || isAllowed(event.getClass())) {
            super.publish(event);
        } else {
            logger.error("Unexpected event {} published on {}", event, name);
        }
    }

    //-------------------//
    // subscribeStrongly //
    //-------------------//
    /**
     * {@inheritDoc}.
     * <p>
     * Overridden to check that the subscription corresponds to an allowed class.
     *
     * @param classe the observed class
     * @param es     the subscriber
     * @return true if the subscriber was subscribed successfully, false otherwise
     */
    @Override
    public boolean subscribeStrongly (Class classe,
                                      EventSubscriber es)
    {
        if (isAllowed(classe)) {
            logger.debug(
                    "{}: subscription on {} by {}@{}",
                    this,
                    classe.getSimpleName(),
                    es,
                    Integer.toHexString(es.hashCode()));

            return super.subscribeStrongly(classe, es);
        } else {
            logger.error("Event {} not available on {} service", classe, name);

            return false;
        }
    }

    //------------------//
    // subscribersCount //
    //------------------//
    /**
     * Convenient method to retrieve the number of subscribers on the
     * selection service for a specific class.
     *
     * @param classe the specific class
     * @return the number of subscribers found
     */
    public int subscribersCount (Class<? extends UserEvent> classe)
    {
        return getSubscribers(classe).size();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return name;
    }

    //-------------//
    // unsubscribe //
    //-------------//
    @Override
    public boolean unsubscribe (Class classe,
                                EventSubscriber eh)
    {
        boolean res = super.unsubscribe(classe, eh);
        logger.debug(
                "{}: unsubscription on {} by {}@{} res:{}",
                this,
                classe.getSimpleName(),
                eh,
                Integer.toHexString(eh.hashCode()),
                res);

        return res;
    }

    //------------------//
    // getEventsAllowed //
    //------------------//
    /**
     * Report the event classes that can ne published on this service.
     *
     * @return the allowed classes of event
     */
    protected Class[] getEventsAllowed ()
    {
        return eventsAllowed;
    }

    //-----------//
    // isAllowed //
    //-----------//
    private boolean isAllowed (Class<?> classe)
    {
        for (Class<?> cl : eventsAllowed) {
            if (cl.isAssignableFrom(classe)) {
                return true;
            }
        }

        return false;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean checkPublishedEvents = new Constant.Boolean(
                true,
                "(debug) Should we check published events?");
    }
}
