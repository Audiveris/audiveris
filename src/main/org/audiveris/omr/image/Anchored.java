//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         A n c h o r e d                                        //
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
package org.audiveris.omr.image;

import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.*;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Interface <code>Anchored</code> handles anchor positions relative to a rectangular area.
 *
 * @author Hervé Bitteur
 */
public interface Anchored
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the rectangular bounds when positioning anchor at location (x,y).
     *
     * @param x      abscissa for anchor
     * @param y      ordinate for anchor
     * @param anchor chosen anchor
     * @return the corresponding bounds
     */
    Rectangle2D getBoundsAt (double x,
                             double y,
                             Anchor anchor);

    /**
     * Report the rectangle height
     *
     * @return the rectangle height
     */
    int getHeight ();

    /**
     * Report the Point offset from rectangle upper left corner to the provided anchor.
     *
     * @param anchor the desired anchor
     * @return the corresponding offset (vector from upper-left to anchor)
     */
    Point2D getOffset (Anchor anchor);

    /**
     * Report the template width
     *
     * @return the rectangle width
     */
    int getWidth ();

    /**
     * Assign a relative offset for an anchor.
     *
     * @param anchor the specified anchor
     * @param dx     the abscissa offset from upper left corner in pixels
     * @param dy     the ordinate offset from upper left corner in pixels
     */
    void putOffset (Anchor anchor,
                    double dx,
                    double dy);

    //~ Inner Classes ------------------------------------------------------------------------------

    /** Specifies a reference relative location. */
    public enum Anchor
    {
        /**
         * Area Center.
         */
        CENTER("C"),
        /**
         * X at symbol left abscissa, Y at middle.
         */
        MIDDLE_LEFT("ML"),
        /**
         * X at symbol right abscissa, Y at middle.
         */
        MIDDLE_RIGHT("MR"),
        /**
         * X at symbol left stem, Y at middle.
         */
        LEFT_STEM("LS"),
        /**
         * X at symbol left stem, Y at high stem ordinate.
         */
        TOP_LEFT_STEM("TLS"),
        /**
         * X at symbol left stem, Y at low stem ordinate.
         */
        BOTTOM_LEFT_STEM("BLS"),
        /**
         * X at symbol right stem, Y at middle.
         */
        RIGHT_STEM("RS"),
        /**
         * X at symbol right stem, Y at high stem ordinate.
         */
        TOP_RIGHT_STEM("TRS"),
        /**
         * X at symbol right stem, Y at low stem ordinate.
         */
        BOTTOM_RIGHT_STEM("BRS");

        final String abbreviation;

        Anchor (String a)
        {
            abbreviation = a;
        }

        /**
         * Report the abbreviation based on anchor
         *
         * @return anchor abbreviation
         */
        public String abbreviation ()
        {
            return abbreviation;
        }

        /**
         * Report the horizontal side of this anchor
         *
         * @return anchor horizontal side, null for center abscissa
         */
        public HorizontalSide hSide ()
        {
            return switch (this) {
            case MIDDLE_LEFT, LEFT_STEM, TOP_LEFT_STEM, BOTTOM_LEFT_STEM -> LEFT;
            case CENTER -> null;
            case TOP_RIGHT_STEM, MIDDLE_RIGHT, RIGHT_STEM, BOTTOM_RIGHT_STEM -> RIGHT;
            };
        }

        /**
         * Report the vertical side of this anchor
         *
         * @return anchor vertical side, null for anchors on center ordinate
         */
        public VerticalSide vSide ()
        {
            return switch (this) {
            case TOP_LEFT_STEM, TOP_RIGHT_STEM -> TOP;
            case MIDDLE_LEFT, LEFT_STEM, CENTER, RIGHT_STEM, MIDDLE_RIGHT -> null;
            case BOTTOM_LEFT_STEM, BOTTOM_RIGHT_STEM -> BOTTOM;
            };
        }
    }
}
