//----------------------------------------------------------------------------//
//                                                                            //
//                         R u n s T a b l e V i e w                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.ui.view.RubberPanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code RunsTableView} displays a view on an underlying runs
 * table.
 *
 * @author Hervé Bitteur
 */
public class RunsTableView
        extends RubberPanel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(RunsTableView.class);

    //~ Instance fields --------------------------------------------------------
    /** The underlying table of runs */
    private final RunsTable table;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // RunsTableView //
    //---------------//
    /**
     * Creates a new RunsTableView object.
     *
     * @param table the underlying table of runs
     */
    public RunsTableView (RunsTable table,
                          SelectionService locationService)
    {
        this.table = table;
        setName(table.getName());

        // Location service
        setLocationService(locationService);

        // Set background color
        setBackground(Color.white);
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    /**
     * Notification about selection objects.
     * We catch:
     * SheetLocation (-> Run)
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

            if (event instanceof LocationEvent) { // Location => Section(s) & Run
                handleEvent((LocationEvent) event);
            }
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the table in the provided Graphics context, which may be
     * already scaled.
     *
     * @param g the graphics context
     */
    @Override
    public void render (Graphics2D g)
    {
        // Render all sections, using the colors they have been assigned
        renderRuns(g);

        // Paint additional items, such as recognized items, etc...
        renderItems(g);
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Room for rendering additional items, if any.
     *
     * @param g the graphic context
     */
    protected void renderItems (Graphics2D g)
    {
        // Void
    }

    //------------//
    // renderRuns //
    //------------//
    protected void renderRuns (Graphics2D g)
    {
        Rectangle clip = g.getClipBounds();

        switch (table.getOrientation()) {
        case HORIZONTAL: {
            int minRow = Math.max(clip.y, 0);
            int maxRow = Math.min((clip.y + clip.height), table.getHeight())
                    - 1;

            for (int row = minRow; row <= maxRow; row++) {
                List<Run> seq = table.getSequence(row);

                for (Run run : seq) {
                    g.setColor(runColor(run));
                    g.fillRect(run.getStart(), row, run.getLength(), 1);
                }
            }
        }

        break;

        case VERTICAL: {
            int minRow = Math.max(clip.x, 0);
            int maxRow = Math.min((clip.x + clip.width), table.getWidth()) - 1;

            for (int row = minRow; row <= maxRow; row++) {
                List<Run> seq = table.getSequence(row);

                for (Run run : seq) {
                    g.setColor(runColor(run));
                    g.fillRect(row, run.getStart(), 1, run.getLength());
                }
            }
        }

        break;
        }
    }

    //----------//
    // runColor //
    //----------//
    protected Color runColor (Run run)
    {
        int level = run.getLevel();

        return new Color(level, level, level);
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
        logger.fine("sheetLocation: {0}", locationEvent);

        // Lookup for Run pointed by this pixel location
        // Search and forward run & section info
        PixelRectangle rect = locationEvent.getData();

        if (rect == null) {
            return;
        }

        SelectionHint hint = locationEvent.hint;
        MouseMovement movement = locationEvent.movement;

        if ((hint != SelectionHint.LOCATION_ADD)
                && (hint != SelectionHint.LOCATION_INIT)) {
            return;
        }

        PixelPoint pt = rect.getLocation();
        Run run = table.lookupRun(pt);

        // Publish Run information
        table.getRunService().publish(new RunEvent(this, hint, movement, run));
    }
}
