//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               G h o s t D r o p L i s t e n e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.ui.dnd;

/**
 * Interface {@code GhostDropListener} defines a listener interested in
 * {@link GhostDropEvent} instances
 *
 * @param <A> The type of action carried by the drop event
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public interface GhostDropListener<A>
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Call-back function to receive the drop event
     *
     * @param e the handed event
     */
    public void dropped (GhostDropEvent<A> e);
}
