//----------------------------------------------------------------------------//
//                                                                            //
//                      S e l e c t i o n S e r v i c e                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.log.Logger;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.ThreadSafeEventService;

import java.util.ArrayList;
import java.util.Collection;
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
        allEventClasses.add(SectionEvent.class);
        allEventClasses.add(SectionIdEvent.class);
        allEventClasses.add(SectionSetEvent.class);
        allEventClasses.add(SheetEvent.class);
        allEventClasses.add(UserEvent.class);
    }

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SelectionService.class);

    //~ Instance fields --------------------------------------------------------

    /** (Debug) name of this service */
    private final String name;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // SelectionService //
    //------------------//
    /**
     * Creates a new SelectionService object.
     * @param name a name for this service (meant for debug)
     */
    public SelectionService (String name)
    {
        this.name = name;

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

    //---------//
    // publish //
    //---------//
    /**
     * This method is overridden just to be able to trace every publication
     * @param obj the published event
     */
    @Override
    public void publish (Object obj)
    {
        if (logger.isFineEnabled()) {
            logger.fine(this + " published: " + obj);
        }
        super.publish(obj);
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
}
