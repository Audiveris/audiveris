//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P i x e l E v e n t                                       //
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
 * Class <code>PixelEvent</code> represent a Pixel Level selection.
 *
 * @author Hervé Bitteur
 */
public class PixelEvent
        extends UserEvent<Integer>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The current pixel level, which may be null. */
    private final Integer level;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>PixelEvent</code> object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin
     * @param movement the mouse movement
     * @param level    the selected pixel level (or null)
     */
    public PixelEvent (Object source,
                       SelectionHint hint,
                       MouseMovement movement,
                       Integer level)
    {
        super(source, hint, movement);
        this.level = level;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // getData //
    //---------//
    @Override
    public Integer getData ()
    {
        return level;
    }
}
