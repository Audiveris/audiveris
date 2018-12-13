//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          A r c V i e w                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.curve;

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

    /** True arc underneath. */
    private final Arc arc;

    private final boolean reversed;

    private List<Point> points;

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

    /**
     * Report the underlying arc.
     *
     * @return the arc
     */
    public Arc getArc ()
    {
        return arc;
    }

    /**
     * Report the ending point at specified end.
     *
     * @param reverse desired direction
     * @return ending point
     */
    public Point getEnd (boolean reverse)
    {
        return arc.getEnd(reverse ^ reversed);
    }

    /**
     * Report junction, if any, at specified end.
     *
     * @param reverse desired direction
     * @return junction or null
     */
    public Point getJunction (boolean reverse)
    {
        return arc.getJunction(reverse ^ reversed);
    }

    /**
     * Report the sequence of points.
     *
     * @return the current sequence of defining points
     */
    public List<Point> getPoints ()
    {
        if (points == null) {
            if (reversed) {
                points = new ArrayList<>(arc.getPoints());
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
        } else if (reverse) {
            return pts.subList(0, count);
        } else {
            return pts.subList(pts.size() - count, pts.size());
        }
    }
}
