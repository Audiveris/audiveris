//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P i x e l F o c u s                                       //
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
package org.audiveris.omr.ui.view;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Interface {@code PixelFocus} define the features related to setting a focus
 * determined by pixels coordinates.
 * <p>
 * Pixel information is used to focus the user display on the given point or rectangle, and to
 * notify this information to registered observers.
 *
 * @author Hervé Bitteur
 */
public interface PixelFocus
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Focus on a rectangle
     *
     * @param rect the designated rectangle, using pixel coordinates
     */
    void setFocusLocation (Rectangle rect);

    /**
     * Focus on a point
     *
     * @param pt the designated point, using pixel coordinates
     */
    void setFocusPoint (Point pt);
}
