//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S t u b E v e n t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.sheet.SheetStub;

/**
 * Class {@code StubEvent} represent a SheetStub selection event, used to call attention
 * about a selected stub.
 *
 * @author Hervé Bitteur
 */
public class StubEvent
        extends UserEvent
{

    /** The selected sheet stub, which may be null. */
    private final SheetStub stub;

    /**
     * Creates a new SheetEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin
     * @param movement the mouse movement
     * @param stub     the selected sheet stub (or null)
     */
    public StubEvent (Object source,
                      SelectionHint hint,
                      MouseMovement movement,
                      SheetStub stub)
    {
        super(source, null, null);
        this.stub = stub;
    }

    //---------//
    // getData //
    //---------//
    @Override
    public SheetStub getData ()
    {
        return stub;
    }
}
