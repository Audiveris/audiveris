//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    D i s t a n c e B o a r d                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.image.ChamferDistance;
import org.audiveris.omr.image.DistanceTable;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.selection.LocationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class <code>DistanceBoard</code> is a PixelBoard where the 'level' field is in fact used
 * to display distance value.
 *
 * @author Hervé Bitteur
 */
public class DistanceBoard
        extends PixelBoard
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DistanceBoard.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The distance table to browse. */
    private final DistanceTable table;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a <code>DistanceBoard</code> object.
     *
     * @param sheet related sheet
     * @param table distance table to browse
     */
    public DistanceBoard (Sheet sheet,
                          DistanceTable table)
    {
        super(sheet);
        this.table = table;

        // Customize the 'level' field
        level.getLabel().setText("Dist");
        level.getLabel().setToolTipText("Distance to foreground");
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------------//
    // handleLocationEvent //
    //---------------------//
    /**
     * Display rectangle attributes
     *
     * @param locEvent the location event
     */
    @Override
    protected void handleLocationEvent (LocationEvent locEvent)
    {
        super.handleLocationEvent(locEvent);

        Rectangle rect = locEvent.getData();

        if (rect != null) {
            Point point = rect.getLocation();

            if ((point.x < table.getWidth()) && (point.y < table.getHeight())) {
                // Display distance value
                int raw = table.getValue(point.x, point.y);

                if (raw == ChamferDistance.VALUE_UNKNOWN) {
                    level.setText("none");
                } else {
                    double distance = raw / (double) table.getNormalizer();
                    level.setText(String.format("%.1f", distance));
                }
            } else {
                level.setText("");
            }
        }
    }
}
