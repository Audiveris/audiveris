//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       G r o u p E v e n t                                      //
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

import org.audiveris.omr.glyph.Symbol.Group;

/**
 * Class {@code GroupEvent} represents the selection of a specific glyph group.
 *
 * @author Hervé Bitteur
 */
public class GroupEvent
        extends UserEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The selected group. (Can be null) */
    private final Group group;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new GroupEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin (or null)
     * @param movement the mouse movement
     * @param group    the specific group chosen (perhaps null)
     */
    public GroupEvent (Object source,
                       SelectionHint hint,
                       MouseMovement movement,
                       Group group)
    {
        super(source, hint, movement);
        this.group = group;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public Group getData ()
    {
        return group;
    }
}
