//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S e c t i o n V i e w                                      //
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
package org.audiveris.omr.lag.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Class {@code SectionView} defines one view meant for display of a given section.
 *
 * @author Hervé Bitteur
 */
public interface SectionView
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Return the display rectangle used by the rendering of the section
     *
     * @return the bounding rectangle in the display space
     */
    Rectangle getBounds ();

    /**
     * Render the section
     *
     * @param g             the graphics context
     * @param drawBorders   should section borders be drawn
     * @param specificColor specific color
     * @return true if actually rendered, i.e. is displayed
     */
    boolean render (Graphics g,
                    boolean drawBorders,
                    Color specificColor);

    /**
     * Render the section using the provided graphics object, while
     * showing that the section has been selected.
     *
     * @param g the graphics environment (which may be applying transformation such as scale)
     * @return true if the section is concerned by the clipping rectangle, which means if (part of)
     *         the section has been drawn
     */
    boolean renderSelected (Graphics g);
}
