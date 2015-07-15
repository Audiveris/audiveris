//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       R u n S e r v i c e                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code RunService} provides a Run service on top of a RunTable.
 *
 * @author Hervé Bitteur
 */
public class RunService
        extends SelectionService
        implements EventSubscriber<LocationEvent>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RunService.class);

    /** Events that can be published on the run service. */
    public static final Class<?>[] eventsWritten = new Class<?>[]{RunEvent.class};

    /** Events observed on location service. */
    public static final Class<?>[] eventsRead = new Class<?>[]{LocationEvent.class};

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying run table. */
    private final RunTable table;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RunService} object.
     *
     * @param name  name for the service
     * @param table the underlying run table
     */
    public RunService (String name,
                       RunTable table)
    {
        super(name, eventsWritten);
        this.table = table;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // cutLocationService //
    //--------------------//
    /**
     * Disconnect this run service from the location service.
     *
     * @param locationService the service that provide location
     */
    public void cutLocationService (SelectionService locationService)
    {
        for (Class<?> eventClass : eventsRead) {
            locationService.unsubscribe(eventClass, this);
        }
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Interest on Location =&gt; Run
     *
     * @param locationEvent the interesting event
     */
    @Override
    public void onEvent (LocationEvent locationEvent)
    {
        try {
            // Ignore RELEASING
            if (locationEvent.movement == MouseMovement.RELEASING) {
                return;
            }

            logger.debug("RunsTable {}: {}", getName(), locationEvent);

            if (locationEvent instanceof LocationEvent) {
                // Location => Run
                handleEvent(locationEvent);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------------------//
    // setLocationService //
    //--------------------//
    /**
     * Connect this run service to the location service.
     *
     * @param locationService the service that provide location information
     */
    public void setLocationService (SelectionService locationService)
    {
        for (Class<?> eventClass : eventsRead) {
            locationService.subscribeStrongly(eventClass, this);
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in location => Run
     *
     * @param location
     */
    private void handleEvent (LocationEvent locationEvent)
    {
        ///logger.info("RunTable location: {}", locationEvent);
        Rectangle rect = locationEvent.getData();

        if (rect == null) {
            return;
        }

        SelectionHint hint = locationEvent.hint;
        MouseMovement movement = locationEvent.movement;

        if (!hint.isLocation() && !hint.isContext()) {
            return;
        }

        if ((rect.width == 0) && (rect.height == 0)) {
            Point pt = rect.getLocation();

            // Publish Run information
            Run run = table.getRunAt(pt.x, pt.y);
            publish(new RunEvent(this, hint, movement, run));
        }
    }
}
