//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    O b j e c t U I M o d e l                                   //
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
package org.audiveris.omr.sheet.ui;

import java.awt.geom.Point2D;

/**
 * Class <code>ObjectUIModel</code> gathers the data to be modified or transferred when UI
 * works on an Object (an Inter, a Staff or a StaffLine).
 *
 * @author Hervé Bitteur
 */
public interface ObjectUIModel
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Translate the model along the provided vector components.
     *
     * @param dx translation in abscissa
     * @param dy translation in ordinate
     */
    default void translate (double dx,
                            double dy)
    {
    }

    /**
     * Translate the model along the provided vector.
     *
     * @param vector translation in abscissa and ordinate
     */
    default void translate (Point2D vector)
    {
        translate(vector.getX(), vector.getY());
    }
}
