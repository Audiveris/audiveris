//----------------------------------------------------------------------------//
//                                                                            //
//                               G e o U t i l                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.awt.Rectangle;

/**
 * This class gathers static utilities related to geometry.
 *
 * @author Hervé Bitteur
 */
public class GeoUtil
{
    //~ Methods ----------------------------------------------------------------

    //------//
    // xGap //
    //------//
    /**
     * Report the abscissa gap between the provided rectangles
     *
     * @param one first provided rectangle
     * @param two the other provided rectangle
     * @return a negative value if the rectangles overlap horizontally or a
     *         positive value if there is a true horizontal gap
     */
    public static int xGap (Rectangle one,
                            Rectangle two)
    {
        return -xOverlap(one, two);
    }

    //----------//
    // xOverlap //
    //----------//
    /**
     * Report the abscissa overlap between the provided rectangles
     *
     * @param one first provided rectangle
     * @param two the other provided rectangle
     * @return a positive value if the rectangles overlap horizontally or a
     *         negative value if there is a true horizontal gap
     */
    public static int xOverlap (Rectangle one,
                                Rectangle two)
    {
        final int commonLeft = Math.max(one.x, two.x);
        final int commonRight = Math.min(one.x + one.width, two.y + two.width);

        return commonRight - commonLeft;
    }

    //------//
    // yGap //
    //------//
    /**
     * Report the ordinate gap between the provided rectangles
     *
     * @param one first provided rectangle
     * @param two the other provided rectangle
     * @return a negative value if the rectangles overlap vertically or a
     *         positive value if there is a true vertical gap
     */
    public static int yGap (Rectangle one,
                            Rectangle two)
    {
        return -yOverlap(one, two);
    }

    //----------//
    // yOverlap //
    //----------//
    /**
     * Report the ordinate overlap between the provided rectangles
     *
     * @param one first provided rectangle
     * @param two the other provided rectangle
     * @return a positive value if the rectangles overlap vertically or a
     *         negative value if there is a true vertical gap
     */
    public static int yOverlap (Rectangle one,
                                Rectangle two)
    {
        final int commonTop = Math.max(one.y, two.y);
        final int commonBot = Math.min(one.y + one.height, two.y + two.height);

        return commonBot - commonTop;
    }
}
