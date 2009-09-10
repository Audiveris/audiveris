//----------------------------------------------------------------------------//
//                                                                            //
//                      S e l e c t i o n S e r v i c e                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.log.Logger;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.EventSubscriber;
import org.bushe.swing.event.ThreadSafeEventService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class <code>SelectionService</code> is an OMR customized version of an
 * EventService as provided by the EventBus framework.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SelectionService
    extends ThreadSafeEventService
{
    //~ Static fields/initializers ---------------------------------------------

    /** Catalog of really all Eventclasses, meant only for debugging */
    private static final Collection<Class<?extends UserEvent>> allEventClasses;

    static {
        allEventClasses = new ArrayList<Class<?extends UserEvent>>();
        allEventClasses.add(GlyphEvent.class);
        allEventClasses.add(GlyphIdEvent.class);
        allEventClasses.add(GlyphSetEvent.class);
        allEventClasses.add(LocationEvent.class);
        allEventClasses.add(PixelLevelEvent.class);
        allEventClasses.add(RunEvent.class);
        allEventClasses.add(ScoreLocationEvent.class);
        allEventClasses.add(SectionEvent.class);
        allEventClasses.add(SectionIdEvent.class);
        allEventClasses.add(SectionSetEvent.class);
        allEventClasses.add(SheetEvent.class);
        allEventClasses.add(SheetLocationEvent.class);
        allEventClasses.add(UserEvent.class);
    }

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SelectionService.class);

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // SelectionService //
    //------------------//
    /**
     * Creates a new SelectionService object.
     */
    public SelectionService ()
    {
        // This cache is needed to be able to retrieve the last instance of
        // any event class
        setDefaultCacheSizePerClassOrTopic(1);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // dumpSubscribers //
    //-----------------//
    @SuppressWarnings("unchecked")
    public static void dumpSubscribers (String       title,
                                        EventService service)
    {
        logger.info(title);

        for (Class<?extends UserEvent> eventClass : allEventClasses) {
            List subscribers = service.getSubscribers(eventClass);

            if (!subscribers.isEmpty()) {
                UserEvent last = (UserEvent) service.getLastEvent(eventClass);
                logger.info(
                    "-- " + eventClass.getSimpleName() + ": " +
                    subscribers.size() + ((last != null) ? (" " + last) : ""));

                for (Object obj : subscribers) {
                    logger.info("      " + obj);
                }
            }
        }
    }

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

    //-----------//
    // subscribe //
    //-----------//
    /**
     * Subscribe to an event service
     * @param subscriber The subscriber to accept the events when published.
     */
    public void subscribe (EventSubscriber subscriber)
    {
        // We force a strong subscription
        subscribeStrongly(SheetEvent.class, subscriber);
    }

    //------------------//
    // subscribersCount //
    //------------------//
    /**
     * Convenient method to retrieve the number of subscribers on the selection
     * service for a specific class
     * @param classe the specific class
     */
    public int subscribersCount (Class<?extends UserEvent> classe)
    {
        return getSubscribers(classe)
                   .size();
    }
}
