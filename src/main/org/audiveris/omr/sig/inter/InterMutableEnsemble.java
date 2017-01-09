//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             I n t e r M u t a b l e E n s e m b l e                            //
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
package org.audiveris.omr.sig.inter;

/**
 * Interface {@code InterMutableEnsemble} is a mutable {@link InterEnsemble}, with the
 * ability to add or remove members.
 *
 * @author Hervé Bitteur
 */
public interface InterMutableEnsemble
        extends InterEnsemble
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Add a member to the mutable ensemble.
     *
     * @param member the member to add
     */
    void addMember (Inter member);

    /**
     * Remove a member from the mutable ensemble.
     *
     * @param member the member to remove
     */
    void removeMember (Inter member);
}
