//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       R u n S e r v i c e                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.run;

import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.RunEvent;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.SelectionService;

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

    /** Events allowed to be published on this run service. */
    private static final Class<?>[] eventsWritten = new Class<?>[]{RunEvent.class};

    /** Events observed on location service. */
    private static final Class<?>[] locEventsRead = new Class<?>[]{LocationEvent.class};

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
        super(name + "Service", eventsWritten);
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
        for (Class<?> eventClass : locEventsRead) {
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
            handleLocationEvent(locationEvent); // Location => Run
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
        for (Class<?> eventClass : locEventsRead) {
            locationService.subscribeStrongly(eventClass, this);
        }
    }

    //---------------------//
    // handleLocationEvent //
    //---------------------//
    /**
     * Interest in location &rArr; Run
     *
     * @param locationEvent the location event
     */
    protected void handleLocationEvent (LocationEvent locationEvent)
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
