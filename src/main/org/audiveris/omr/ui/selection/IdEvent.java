//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          I d E v e n t                                         //
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
package org.audiveris.omr.ui.selection;

/**
 * Class <code>IdEvent</code> is an event that conveys an entity ID.
 *
 * @author Hervé Bitteur
 */
public class IdEvent
        extends UserEvent<Integer>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The selected entity id, which may be null. */
    private final Integer id;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new IdEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin (or null)
     * @param movement the precise mouse movement
     * @param id       the entity id
     */
    public IdEvent (Object source,
                    SelectionHint hint,
                    MouseMovement movement,
                    int id)
    {
        super(source, hint, null);
        this.id = id;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // getData //
    //---------//
    @Override
    public Integer getData ()
    {
        return id;
    }
}
