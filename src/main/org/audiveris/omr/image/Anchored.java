//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         A n c h o r e d                                        //
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
package org.audiveris.omr.image;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Interface {@code Anchored} handles anchor positions relative to a rectangular area.
 *
 * @author Hervé Bitteur
 */
public interface Anchored
{
    //~ Enumerations -------------------------------------------------------------------------------

    /** Specifies a reference relative location. */
    public enum Anchor
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /**
         * Area Center.
         */
        CENTER,
        /**
         * X at symbol left abscissa, Y at middle.
         */
        MIDDLE_LEFT,
        /**
         * X at symbol left stem, Y at high stem ordinate.
         */
        TOP_LEFT_STEM,
        /**
         * X at symbol left stem, Y at middle.
         */
        LEFT_STEM,
        /**
         * X at symbol left stem, Y at low stem ordinate.
         */
        BOTTOM_LEFT_STEM,
        /**
         * X at symbol right stem, Y at high stem ordinate.
         */
        TOP_RIGHT_STEM,
        /**
         * X at symbol right stem, Y at middle.
         */
        RIGHT_STEM,
        /**
         * X at symbol right stem, Y at low stem ordinate.
         */
        BOTTOM_RIGHT_STEM;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Assign a relative offset for an anchor type.
     *
     * @param anchor the anchor type
     * @param xRatio the abscissa offset from upper left corner, specified as
     *               ratio of rectangle width
     * @param yRatio the ordinate offset from upper left corner, specified as
     *               ratio of rectangle height
     */
    void addAnchor (Anchor anchor,
                    double xRatio,
                    double yRatio);

    /**
     * Report the rectangular bounds when positioning anchor at location (x,y).
     *
     * @param x      abscissa for anchor
     * @param y      ordinate for anchor
     * @param anchor chosen anchor
     * @return the corresponding bounds
     */
    Rectangle getBoundsAt (int x,
                           int y,
                           Anchor anchor);

    /**
     * Report the rectangle height
     *
     * @return the rectangle height
     */
    int getHeight ();

    /**
     * Report the offset from rectangle upper left corner to the provided anchor.
     *
     * @param anchor the desired anchor
     * @return the corresponding offset (vector from upper-left to anchor)
     */
    Point getOffset (Anchor anchor);

    /**
     * Report the template width
     *
     * @return the rectangle width
     */
    int getWidth ();
}
