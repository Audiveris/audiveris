//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          A r c V i e w                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code ArcView} presents the arc items in a desired orientation.
 *
 * @author Hervé Bitteur
 */
public class ArcView
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** True arc underneath. */
    private final Arc arc;

    private final boolean reversed;

    private List<Point> points;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ArcView object.
     *
     * @param arc      the arc to view
     * @param reversed true for a reversed view, false for a direct view
     */
    public ArcView (Arc arc,
                    boolean reversed)
    {
        this.arc = arc;
        this.reversed = reversed;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public Arc getArc ()
    {
        return arc;
    }

    public Point getEnd (boolean reverse)
    {
        return arc.getEnd(reverse ^ reversed);
    }

    public Point getJunction (boolean reverse)
    {
        return arc.getJunction(reverse ^ reversed);
    }

    public List<Point> getPoints ()
    {
        if (points == null) {
            if (reversed) {
                points = new ArrayList<Point>(arc.getPoints());
                Collections.reverse(points);
            } else {
                points = arc.getPoints();
            }
        }

        return points;
    }

    /**
     * Report the sequence of 'count' points on desired side of arc.
     *
     * @param count   the maximum number of points to retrieve, or null to take all
     * @param reverse desired arc side
     * @return the sequence of desired points, perhaps limited by the arc length itself.
     */
    public List<Point> getSidePoints (Integer count,
                                      boolean reverse)
    {
        List<Point> pts = getPoints();

        if ((count == null) || (count >= pts.size())) {
            return pts;
        } else {
            if (reverse) {
                return pts.subList(0, count);
            } else {
                return pts.subList(pts.size() - count, pts.size());
            }
        }
    }
}
