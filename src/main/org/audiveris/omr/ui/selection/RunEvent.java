//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R u n E v e n t                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.run.Run;

/**
 * Class {@code RunEvent} represents a Run selection.
 *
 * @author Hervé Bitteur
 */
public class RunEvent
        extends UserEvent<Run>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The selected run, which may be null */
    private final Run run;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new RunEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     how the event originated
     * @param movement the mouse movement
     * @param run      the selected run (or null)
     */
    public RunEvent (Object source,
                     SelectionHint hint,
                     MouseMovement movement,
                     Run run)
    {
        super(source, hint, movement);
        this.run = run;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public Run getData ()
    {
        return run;
    }
}
