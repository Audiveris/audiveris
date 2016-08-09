//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  G h o s t D r o p E v e n t                                   //
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
package omr.ui.dnd;

/**
 * Class {@code GhostDropEvent} is the type of event that is handed to any {@link
 * GhostDropListener} instance.
 *
 * @param <A> the precise type of the action carried by the drop
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostDropEvent<A>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The drop location with respect to screen */
    private final ScreenPoint screenPoint;

    /** The action carried by the drop event */
    private final A action;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new GhostDropEvent object.
     *
     * @param action      the action carried by the drop
     * @param screenPoint the screen-based location of the drop
     */
    public GhostDropEvent (A action,
                           ScreenPoint screenPoint)
    {
        this.action = action;
        this.screenPoint = screenPoint;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // getAction //
    //-----------//
    /**
     * Report the action carried by the drop.
     *
     * @return the carried action
     */
    public A getAction ()
    {
        return action;
    }

    //-----------------//
    // getDropLocation //
    //-----------------//
    /**
     * Report the drop location.
     *
     * @return the screen-based location of the drop
     */
    public ScreenPoint getDropLocation ()
    {
        return screenPoint;
    }
}
