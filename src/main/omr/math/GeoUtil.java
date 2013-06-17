//----------------------------------------------------------------------------//
//                                                                            //
//                                G e o U t i l                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code GeoUtil} gathers simple utilities related to geometry.
 *
 * @author Hervé Bitteur
 */
public class GeoUtil
{
    //~ Methods ----------------------------------------------------------------

    //----------//
    // vectorOf //
    //----------//
    /**
     * Report the vector that goes from 'from' point to 'to' point.
     *
     * @param from the origin point
     * @param to   the target point
     * @return the vector from origin to target
     */
    public static Point vectorOf (Point from,
                                  Point to)
    {
        return new Point(to.x - from.x, to.y - from.y);
    }

    //----------//
    // centerOf //
    //----------//
    /**
     * Report the center of the provided rectangle
     *
     * @param rect the provided rectangle
     * @return the geometric rectangle center
     */
    public static Point centerOf (Rectangle rect)
    {
        return new Point(rect.x + (rect.width / 2), rect.y + (rect.height / 2));
    }
}
