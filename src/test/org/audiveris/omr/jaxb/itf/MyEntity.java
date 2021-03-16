//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         M y E n t i t y                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.jaxb.itf;

/**
 * Interface {@code MyEntity}
 *
 * @author Hervé Bitteur
 */
public interface MyEntity
{

    /**
     * Report the ID of this entity
     *
     * @return the entity ID
     */
    int getId ();

    /**
     * Assign an ID to this entity
     *
     * @param id the ID to be assigned to the entity
     */
    void setId (int id);
}
