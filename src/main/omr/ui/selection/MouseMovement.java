//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   M o u s e M o v e m e n t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.ui.selection;

/**
 * Class {@code MouseMovement} defines which phase of user action
 * generated the event.
 * <p>
 * For example, we may decide to update minimal information while the user is moving the mouse, and
 * launch expensive processing only when the user is releasing the mouse.
 *
 * @author Hervé Bitteur
 */
public enum MouseMovement
{

    /**
     * User presses the mouse button down or, by extension, manually
     * enters the location data in a dedicated board.
     */
    PRESSING,
    /**
     * User moves the mouse while keeping the button down.
     */
    DRAGGING,
    /**
     * User releases the mouse button.
     */
    RELEASING;
}
