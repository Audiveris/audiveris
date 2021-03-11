//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           E n t i t y                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Interface {@code Entity} describes an entity with an assigned ID, and with
 * minimal geometric features (bounding box and point containment).
 *
 * @author Hervé Bitteur
 */
public interface Entity
        extends Vip
{

    /**
     * Tell whether the entity contains the provided point.
     * <p>
     * This is mostly used to detect when an entity is pointed at.
     *
     * @param point the provided point
     * @return true if point is found as contained by the entity
     */
    boolean contains (Point point);

    /**
     * Report details about this entity.
     *
     * @return a string dump
     */
    String dumpOf ();

    /**
     * Return (a copy of) the absolute bounding box.
     *
     * @return a COPY of the bounding rectangle
     */
    Rectangle getBounds ();

    /**
     * Report the full ID of this entity.
     *
     * @return the entity full ID
     */
    String getFullId ();

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
