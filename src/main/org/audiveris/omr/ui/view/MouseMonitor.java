//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M o u s e M o n i t o r                                     //
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
package org.audiveris.omr.ui.view;

import org.audiveris.omr.ui.selection.MouseMovement;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Interface {@code MouseMonitor} defines the entries of an entity to be notified of
 * mouse actions.
 * This is ordinarily used in conjunction with a {@link org.audiveris.omr.ui.view.Rubber}.
 *
 * @author Hervé Bitteur
 */
public interface MouseMonitor
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Contextual action (by right button click + control) of an
     * additional point
     *
     * @param pt       the current point in model pixel coordinates
     * @param movement the mouse movement
     */
    void contextAdded (Point pt,
                       MouseMovement movement);

    /**
     * Contextual action (by right button click)
     *
     * @param pt       the current point in model pixel coordinates
     * @param movement the mouse movement
     */
    void contextSelected (Point pt,
                          MouseMovement movement);

    /**
     * Selection (by left button click + control) of an additional point
     *
     * @param pt       the added point in model pixel coordinates
     * @param movement the mouse movement
     */
    void pointAdded (Point pt,
                     MouseMovement movement);

    /**
     * Selection (by left button click)
     *
     * @param pt       the selected point in model pixel coordinates
     * @param movement the mouse movement
     */
    void pointSelected (Point pt,
                        MouseMovement movement);

    /**
     * Selection (by left or right button drag + shift) of a rectangle
     *
     * @param rect     the selected rectangle in model pixel coordinates
     * @param movement the mouse movement
     */
    void rectangleSelected (Rectangle rect,
                            MouseMovement movement);

    /**
     * Rectangle zoom (by shift + ctrl) at end of rectangle selection
     *
     * @param rect     the rectangle in model pixel coordinates, which defines
     *                 the focus and the zoom ratio
     * @param movement the mouse movement
     */
    void rectangleZoomed (Rectangle rect,
                          MouseMovement movement);
}
