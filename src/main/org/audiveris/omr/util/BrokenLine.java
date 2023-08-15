//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B r o k e n L i n e                                      //
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
package org.audiveris.omr.util;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.GeoPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.NotThreadSafe;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>BrokenLine</code> handles the broken line defined by a sequence of points
 * which can be modified at any time.
 * <p>
 * This class make use of several distance parameters, presented here from smaller to larger:
 * <dl>
 * <dt><b>colinear</b></dt>
 * <dd>A point sufficiently close to a segment can be considered as colinear and thus removed. See
 * {@link #isColinear} method.</dd>
 * <dt><b>sticky</b></dt>
 * <dd>A point sufficiently close to a reference point or to a segment allows to select this
 * reference point or segment. See {@link #findPoint} and {@link #findSegment} methods.</dd>
 * <dt><b>dragging</b></dt>
 * <dd>A point sufficiently close to the last location of a point being dragged is considered as the
 * new location for this point. This UI feature is actually beyond the scope of BrokenLine, so only
 * the default dragging value is handled here for convenience. See {@link #getDraggingDistance}.
 * </dd>
 * </dl>
 * <p>
 * <b>Nota:</b> Internal reference points data can still be modified at any time, since the
 * BrokenLine, just like a List, merely handles points pointers.
 * For example, to move a point, just call point.setLocation() method.
 * </p>
 * <p>
 * This ability of dynamic modification is the main reason why this class is not simply implemented
 * as a Path2D. If a Path2D instance is needed, use the {@link #toGeoPath()} conversion method.
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "broken-line")
public class BrokenLine
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BrokenLine.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The ordered sequence of points. */
    @XmlElement(name = "point")
    @XmlJavaTypeAdapter(Jaxb.PointAdapter.class)
    private final List<Point> points = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BrokenLine object with an initially empty sequence of points.
     */
    public BrokenLine ()
    {
    }

    /**
     * Creates a new BrokenLine object with a few initial points.
     *
     * @param points collection of initial points
     */
    public BrokenLine (Collection<Point> points)
    {
        resetPoints(points);
    }

    /**
     * Creates a new BrokenLine object with a few initial points.
     *
     * @param points array of initial points
     */
    public BrokenLine (Point... points)
    {
        resetPoints(Arrays.asList(points));
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // addPoint //
    //----------//
    /**
     * Append a point at the end of the current sequence.
     *
     * @param point the new point to append
     */
    public void addPoint (Point point)
    {
        points.add(point);
    }

    //-----------//
    // findPoint //
    //-----------//
    /**
     * Find the first point of the current sequence which is close to the provided point
     * (less than sticky distance).
     *
     * @param point the provided point
     * @return the point found, or null if not found
     */
    public Point findPoint (Point point)
    {
        Rectangle window = new Rectangle(point);
        window.grow(getStickyDistance(), getStickyDistance());

        for (Point pt : points) {
            if (window.contains(pt)) {
                return pt;
            }
        }

        return null;
    }

    //-------------//
    // findSegment //
    //-------------//
    /**
     * Find the closest segment (if any) which lies at a maximum of sticky distance from
     * the provided point.
     *
     * @param point the provided point
     * @return the sequence point that starts the segment found (or null if not found)
     */
    public Point findSegment (Point point)
    {
        final int sqrStickyDistance = getStickyDistance() * getStickyDistance();
        Point bestPoint = null;
        double bestDistSq = java.lang.Double.MAX_VALUE;

        if (points.size() < 2) {
            return null;
        }

        Point prevPt = points.get(0);

        for (Point pt : points) {
            // Skip first point
            if (pt == prevPt) {
                continue;
            }

            Line2D.Double line = new Line2D.Double(prevPt, pt);
            double distSq = line.ptSegDistSq(point);

            if (distSq < bestDistSq) {
                bestPoint = prevPt;
                bestDistSq = distSq;
            }

            prevPt = pt;
        }

        if (bestDistSq <= sqrStickyDistance) {
            return bestPoint;
        } else {
            return null;
        }
    }

    //----------//
    // getPoint //
    //----------//
    /**
     * Report the point at 'index' position in current sequence.
     *
     * @param index the desired index
     * @return the desired point
     */
    public Point getPoint (int index)
    {
        return points.get(index);
    }

    //-----------//
    // getPoints //
    //-----------//
    /**
     * Report current sequence (meant for debugging).
     *
     * @return an unmodifiable view (perhaps empty) of list of current points
     */
    public List<Point> getPoints ()
    {
        return Collections.unmodifiableList(points);
    }

    //-------------------//
    // getSequenceString //
    //-------------------//
    /**
     * Report a string which summarizes the current sequence of points.
     *
     * @return a string of the sequence points
     */
    public String getSequenceString ()
    {
        StringBuilder sb = new StringBuilder("[");
        boolean started = false;

        for (Point p : getPoints()) {
            if (started) {
                sb.append(' ');
            }

            sb.append('(').append(p.x).append(',').append(p.y).append(')');
            started = true;
        }

        sb.append("]");

        return sb.toString();
    }

    //---------//
    // indexOf //
    //---------//
    /**
     * Retrieve the index of provided point.
     *
     * @param point the point to look for
     * @return the index of the point, or -1
     */
    public int indexOf (Point point)
    {
        return points.indexOf(point);
    }

    //-------------//
    // insertPoint //
    //-------------//
    /**
     * Insert a point at the specified index value.
     *
     * @param index the insertion position in the sequence
     * @param point the new point to insert
     */
    public void insertPoint (int index,
                             Point point)
    {
        points.add(index, point);
    }

    //------------------//
    // insertPointAfter //
    //------------------//
    /**
     * Insert a point right after the specified point.
     *
     * @param point the new point to insert
     * @param after the point after which insertion must be done
     */
    public void insertPointAfter (Point point,
                                  Point after)
    {
        int ptIndex = points.indexOf(after);

        if (ptIndex != -1) {
            points.add(ptIndex + 1, point);
        } else {
            throw new IllegalArgumentException("Insertion point not found");
        }
    }

    //------------//
    // isColinear //
    //------------//
    /**
     * Check whether the specified point is colinear (within colinearDistance) with the
     * previous and the following points in the sequence.
     *
     * @param point the point to check
     * @return true if the 3 points are colinear or nearly so
     */
    public boolean isColinear (Point point)
    {
        int index = points.indexOf(point);

        if ((index > 0) && (index < (points.size() - 1))) {
            Line2D.Double line = new Line2D.Double(getPoint(index - 1), getPoint(index + 1));
            double dist = line.ptLineDist(point);

            return dist <= constants.colinearDistance.getValue();
        } else {
            return false;
        }
    }

    //-------------//
    // removePoint //
    //-------------//
    /**
     * Remove the specified point from the current sequence.
     *
     * @param point the point to remove
     */
    public void removePoint (Point point)
    {
        points.remove(point);
    }

    //-------------//
    // resetPoints //
    //-------------//
    /**
     * Replace the current line points with the provided ones.
     *
     * @param points the new collection of points
     */
    public final void resetPoints (Collection<Point> points)
    {
        if (this.points != points) {
            this.points.clear();

            if (points != null) {
                this.points.addAll(points);
            }
        }
    }

    //------//
    // size //
    //------//
    /**
     * Report the number of points in the current sequence.
     *
     * @return the current size of the points sequence
     */
    public int size ()
    {
        return points.size();
    }

    //-----------//
    // toGeoPath //
    //-----------//
    /**
     * Build a GeoPath instance from this BrokenLine instance
     *
     * @return the corresponding standard GeoPath instance
     */
    public GeoPath toGeoPath ()
    {
        GeoPath path = new GeoPath();
        boolean started = false;

        for (Point point : points) {
            if (!started) {
                path.moveTo(point.x, point.y);
                started = true;
            } else {
                path.lineTo(point.x, point.y);
            }
        }

        return path;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{BrokenLine " + getSequenceString() + "}";
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //---------------------//
    // getDraggingDistance //
    //---------------------//
    /**
     * Report the dragging distance.
     *
     * @return the dragging distance, specified in pixels
     */
    public static int getDraggingDistance ()
    {
        return constants.draggingDistance.getValue();
    }

    //-------------------//
    // getStickyDistance //
    //-------------------//
    /**
     * Report the maximum distance (from a point, from a segment).
     *
     * @return the maximum distance, specified in pixels
     */
    public static int getStickyDistance ()
    {
        return constants.stickyDistance.getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer colinearDistance = new Constant.Integer(
                "Pixels",
                2,
                "Maximum distance from a point to a segment to be colinear");

        private final Constant.Integer stickyDistance = new Constant.Integer(
                "Pixels",
                5,
                "Maximum distance from a point or segment to get stuck to it");

        private final Constant.Integer draggingDistance = new Constant.Integer(
                "Pixels",
                25,
                "Maximum distance from a point to drag it");
    }
}
