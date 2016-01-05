//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         A n c h o r e d                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

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
         * X at symbol right stem, Y at low stem ordinate.
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
