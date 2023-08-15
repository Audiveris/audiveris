//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        O r i e n t e d                                         //
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
package org.audiveris.omr.run;

/**
 * Interface <code>Oriented</code> flags an entity as having some orientation.
 *
 * @author Hervé Bitteur
 */
public interface Oriented
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the orientation constant
     *
     * @return HORIZONTAL or VERTICAL
     */
    Orientation getOrientation ();
}
