//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      N a m e d D o u b l e                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
 * Interface {@code NamedDouble}
 *
 * @author Hervé Bitteur
 */
public interface NamedDouble
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get the description sentence recorded with the NamedDouble
     *
     * @return the description sentence as a string
     */
    String getDescription ();

    /**
     * Report the name of the NamedDouble
     *
     * @return the NamedDouble name
     */
    String getName ();

    /**
     * Report the unit, if any, used as base of quantity measure
     *
     * @return the quantity unit, if any
     */
    String getQuantityUnit ();

    /**
     * Report a short name.
     *
     * @return short name
     */
    String getShortTypeName ();

    /**
     * Report the current value
     *
     * @return current value
     */
    double getValue ();

    /**
     * Assign a new value
     *
     * @param value value to be assigned
     */
    void setValue (double value);
}
