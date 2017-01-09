//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    I n t e r E n s e m b l e                                   //
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

import java.util.List;

/**
 * Interface {@code InterEnsemble} refers to an inter that is composed of other inters.
 * This class is not mutable.
 * <p>
 * Examples are:<ul>
 * <li>Sentence vs words</li>
 * <li>TimePairInter vs num & den</li>
 * <li>KeyInter vs its alterations</li>
 * <li>ChordInter vs its notes and stem</li>
 * </ul>
 *
 * @see InterMutableEnsemble
 *
 * @author Hervé Bitteur
 */
public interface InterEnsemble
        extends Inter
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the list of ensemble members.
     *
     * @return the members
     */
    List<? extends Inter> getMembers ();
}
