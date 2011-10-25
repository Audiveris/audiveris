//----------------------------------------------------------------------------//
//                                                                            //
//                      S e l e c t i o n S e r v i c e                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import org.bushe.swing.event.EventSubscriber;
import org.bushe.swing.event.ThreadSafeEventService;

import java.util.List;

/**
 * Class <code>SelectionService</code> is an OMR customized version of an
 * EventService as provided by the EventBus framework.
 *
 * @author Hervé Bitteur
 */
public class SelectionService
    extends ThreadSafeEventService
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SelectionService.class);

    //~ Instance fields --------------------------------------------------------

    /** (Debug) name of this service */
    private final String name;

    /** Allowed events */
    private final Class[] allowedEvents;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // SelectionService //
    //------------------//
    /**
     * Creates a new SelectionService object.
     * @param name a name for this service (meant for debug)
     * @param allowedEvents classes of events that can be published here
     */
    public SelectionService (String  name,
                             Class[] allowedEvents)
    {
        this.name = name;
        this.allowedEvents = allowedEvents;

        // This cache is needed to be able to retrieve the last publication of
        // any event class
        setDefaultCacheSizePerClassOrTopic(1);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getSelection //
    //--------------//
    /**
     * Report the current selection regarding the specified event class
     * @param classe the event class we are interested in
     * @return the carried data, if any
     */
    public Object getSelection (Class<?extends UserEvent> classe)
    {
        UserEvent event = (UserEvent) getLastEvent(classe);

        if (event == null) {
            return null;
        } else {
            return event.getData();
        }
    }

    //-----------------//
    // dumpSubscribers //
    //-----------------//
    public void dumpSubscribers ()
    {
        logger.info(toString() + " subscriber:");

        for (Class eventClass : allowedEvents) {
            List subscribers = getSubscribers(eventClass);

            if (!subscribers.isEmpty()) {
                UserEvent last = (UserEvent) getLastEvent(eventClass);
                logger.info(
                    "-- " + eventClass.getSimpleName() + ": " +
                    subscribers.size() + ((last != null) ? (" " + last) : ""));

                for (Object obj : subscribers) {
                    logger.info("      " + obj);
                }
            }
        }
    }

    //---------//
    // publish //
    //---------//
    /**
     * This method is overridden just to be able to potentially check and trace
     * every publication
     * @param event the published event
     */
    @Override
    public void publish (Object event)
    {
        if (logger.isFineEnabled()) {
            logger.fine(this + " published: " + event);
            ///dumpSubscribers();
        }

        // Check whether the event may be published on this service
        if (!constants.checkPublishedEvents.isSet() ||
            contains(allowedEvents, event.getClass())) {
            super.publish(event);
        } else {
            logger.severe(
                "Unexpected event " + event + " published on " + name);
        }
    }

    //-------------------//
    // subscribeStrongly //
    //-------------------//
    /**
     * Overridden to check that the subscription corresponds to a declared class
     * @param type the observed class
     * @param es the subscriber
     * @return I don't know
     */
    @Override
    public boolean subscribeStrongly (Class           type,
                                      EventSubscriber es)
    {
        if (contains(allowedEvents, type)) {
            return super.subscribeStrongly(type, es);
        } else {
            logger.severe("event class " + type + " not available on " + name);

            return false;
        }
    }

    //------------------//
    // subscribersCount //
    //------------------//
    /**
     * Convenient method to retrieve the number of subscribers on the selection
     * service for a specific class
     * @param classe the specific class
     * @return the number of subscribers found
     */
    public int subscribersCount (Class<?extends UserEvent> classe)
    {
        return getSubscribers(classe)
                   .size();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        sb.append(" ")
          .append(name);
        sb.append("}");

        return sb.toString();
    }

    //----------//
    // contains //
    //----------//
    private boolean contains (Class[] classes,
                              Class   classe)
    {
        for (Class cl : classes) {
            if (cl == classe) {
                return true;
            }
        }

        return false;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean checkPublishedEvents = new Constant.Boolean(
            true,
            "(debug) Should we check published events?");
    }
}
