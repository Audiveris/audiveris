//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    R u n T a b l e V i e w                                     //
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
package org.audiveris.omr.run;

import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.RunEvent;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.view.RubberPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code RunTableView} displays a view on an run table.
 *
 * @author Hervé Bitteur
 */
public class RunTableView
        extends RubberPanel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RunTableView.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying table of runs. */
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
    //--------//
    // render //
    //--------//
    @Override
    public void render (Graphics2D g)
    {
        // Render all table runs
        table.render(g, new Point(0, 0));
    }

    //---------------------//
    // handleLocationEvent //
    //---------------------//
    /**
     * Interest in Location => Run
     *
     * @param locationEvent
     */
    @Override
    protected void handleLocationEvent (LocationEvent locationEvent)
    {
        super.handleLocationEvent(locationEvent);

        // Lookup for Run pointed by this pixel location
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
