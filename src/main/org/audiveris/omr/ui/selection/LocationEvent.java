//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   L o c a t i o n E v e n t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

/**
 * Class {@code LocationEvent} is UI Event that represents a new location (a rectangle,
 * perhaps degenerated to a point) within the Sheet coordinates space.
 *
 * @author Hervé Bitteur
 */
public class LocationEvent
        extends UserEvent
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(LocationEvent.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * The location rectangle, which can be degenerated to a point when both
     * width and height values equal zero
     */
    private final Rectangle rectangle;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new LocationEvent object.
     *
     * @param source    the actual entity that created this event
     * @param hint      how the event originated
     * @param movement  the precise mouse movement
     * @param rectangle the location within the sheet space
     */
    public LocationEvent (Object source,
                          SelectionHint hint,
                          MouseMovement movement,
                          Rectangle rectangle)
    {
        super(source, hint, movement);
        this.rectangle = rectangle;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public Rectangle getData ()
    {
        return rectangle;
    }
}
