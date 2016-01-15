//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    R u n T a b l e V i e w                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.ui.selection.LocationEvent;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.RunEvent;
import omr.ui.selection.SelectionHint;
import omr.ui.selection.SelectionService;
import omr.ui.selection.UserEvent;

import omr.ui.view.RubberPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code RunTableView} displays a view on an underlying runs table.
 *
 * @author Hervé Bitteur
 */
public class RunTableView
        extends RubberPanel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RunTableView.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying table of runs */
    private final RunTable table;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RunTableView} object.
     *
     * @param name            name for the view
     * @param table           the underlying table of runs
     * @param locationService the service where locations are retrieved from
     */
    public RunTableView (String name,
                         RunTable table,
                         SelectionService locationService)
    {
        this.table = table;
        setName(name);

        // Location service
        setLocationService(locationService);

        // Set background color
        setBackground(Color.white);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    /**
     * Notification about selection objects.
     * We catch:
     * SheetLocation (-&gt; Run)
     *
     * @param event the notified event
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            // Default behavior: making point visible & drawing the markers
            super.onEvent(event);

            if (event instanceof LocationEvent) { // Location => Run
                handleEvent((LocationEvent) event);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------//
    // render //
    //--------//
    @Override
    public void render (Graphics2D g)
    {
        // Render all table runs
        table.render(g, new Point(0, 0));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Location => Run
     *
     * @param locationEvent
     */
    private void handleEvent (LocationEvent locationEvent)
    {
        ///logger.info("RunTableView location: {}", locationEvent);

        // Lookup for Run pointed by this pixel location
        // Search and forward run & section info
        Rectangle rect = locationEvent.getData();

        if (rect == null) {
            return;
        }

        SelectionHint hint = locationEvent.hint;
        MouseMovement movement = locationEvent.movement;

        if (!hint.isLocation()) {
            return;
        }

        Point pt = rect.getLocation();
        Run run = table.getRunAt(pt.x, pt.y);

        // Publish Run information
        table.getRunService().publish(new RunEvent(this, hint, movement, run));
    }
}
