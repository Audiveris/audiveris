//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             V i p                                              //
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
package org.audiveris.omr.util;

/**
 * Interface {@code Vip} allows to flag an object as a VIP and generally triggers
 * specific debugging printouts related to this object.
 *
 * @author Hervé Bitteur
 */
public interface Vip
{

    /**
     * (Debug) Report whether this object is flagged as VIP
     *
     * @return true if VIP
     */
    boolean isVip ();

    /**
     * (Debug) Assign the VIP flag
     *
     * @param vip true if VIP, false if not
     */
    void setVip (boolean vip);
}
