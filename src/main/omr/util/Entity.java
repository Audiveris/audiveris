//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           E n t i t y                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

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
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Tell whether the entity contains the provided point
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
     * Report the ID of this entity
     *
     * @return the entity ID
     */
    String getId ();

    /**
     * Assign an ID to this entity
     *
     * @param id the ID to be assigned to the entity
     */
    void setId (String id);
}
