//----------------------------------------------------------------------------//
//                                                                            //
//                               A r e a U t i l                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * Class {@code AreaUtil} gathers static utility methods for Area
 * instances.
 *
 * @author Hervé Bitteur
 */
public class AreaUtil
{
    //~ Methods ----------------------------------------------------------------

    //-------------------------//
    // horizontalParallelogram //
    //-------------------------//
    /**
     * Create a parallelogram mostly horizontal, where top and bottom
     * sides are short and horizontal.
     * This is most useful for beams.
     * <p>Nota: the defining points are meant to be the extrema points 
     * <b>inside</b> the parallelogram.
     *
     * @param left   left point of median line
     * @param right  right point of median line
     * @param height total height
     * @return the created area
     */
    public static Area horizontalParallelogram (Point2D left,
                                                Point2D right,
                                                double height)
    {
        final double dy = height / 2; // Half height
        final Path2D path = new Path2D.Double();
        path.moveTo(left.getX(), left.getY() - dy); // Upper left
        path.lineTo(right.getX() + 1, right.getY() - dy); // Upper right
        path.lineTo(right.getX() + 1, right.getY() + dy + 1); // Lower right
        path.lineTo(left.getX(), left.getY() + dy + 1); // Lower left
        path.closePath();

        return new Area(path);
    }

    //-----------------------//
    // verticalParallelogram //
    //-----------------------//
    /**
     * Create a parallelogram mostly horizontal, where left and right
     * sides are short and vertical.
     * This is most useful for stems.
     * <p>Nota: the defining points are meant to be the extrema points 
     * <b>inside</b> the parallelogram.
     *
     * @param top    top point of median line
     * @param bottom bottom point of median line
     * @param width  total width
     * @return the created area
     */
    public static Area verticalParallelogram (Point2D top,
                                              Point2D bottom,
                                              double width)
    {
        final double dx = width / 2; // Half width
        final Path2D path = new Path2D.Double();
        path.moveTo(top.getX() - dx, top.getY()); // Upper left
        path.lineTo(top.getX() + dx + 1, top.getY()); // Upper right
        path.lineTo(bottom.getX() + dx + 1, bottom.getY() + 1); // Lower right
        path.lineTo(bottom.getX() - dx, bottom.getY() + 1); // Lower left
        path.closePath();

        return new Area(path);
    }
}
