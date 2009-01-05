//----------------------------------------------------------------------------//
//                                                                            //
//                            B r o k e n L i n e                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.util;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import net.jcip.annotations.NotThreadSafe;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.*;

/**
 * Class <code>BrokenLine</code> handles the broken line defined by a sequence
 * of points which can be modified at any time. Several features use a
 * "stickyDistance" constant which defines proximity margins.
 *
 * @author Herv&eacute Bitteur
 * @version $Id $
 */
@NotThreadSafe
public class BrokenLine
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BrokenLine.class);

    //~ Instance fields --------------------------------------------------------

    /** The ordered sequence of points */
    private List<Point> points = new ArrayList<Point>();

    /** Default sticky distance */
    private int stickyDistance = getDefaultStickyDistance();

    /** Set of insterested listeners */
    private Set<Listener> listeners = new LinkedHashSet<Listener>();

    //~ Constructors -----------------------------------------------------------

    //------------//
    // BrokenLine //
    //------------//
    /**
     * Creates a new BrokenLine object with an initially empty sequence of
     * points.
     */
    public BrokenLine ()
    {
    }

    //------------//
    // BrokenLine //
    //------------//
    /**
     * Creates a new BrokenLine object with a few initial points
     * @param points array of initial points
     */
    public BrokenLine (Point... points)
    {
        this.points.addAll(Arrays.asList(points));
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------------//
    // getDefaultStickyDistance //
    //--------------------------//
    /**
     * Report the default sticky distance.
     * This value can be overridden for the current BrokenLine instance, through
     * method {@link #setStickyDistance}.
     * @return the (default) maximum distance, specified in pixels
     */
    public static int getDefaultStickyDistance ()
    {
        return constants.stickyDistance.getValue();
    }

    //------------//
    // isColinear //
    //------------//
    /**
     * Check whether the specified point is colinear (within stickyDistance)
     * with the previous and the following points in the sequence
     * @param point the point to check
     * @return true if the 3 points are colinear or nearly so
     */
    public boolean isColinear (Point point)
    {
        int index = points.indexOf(point);

        if ((index > 0) && (index < (points.size() - 1))) {
            Line2D.Double line = new Line2D.Double(
                getPoint(index - 1),
                getPoint(index + 1));
            double        dist = line.ptLineDist(point);

            return dist <= stickyDistance;
        } else {
            return false;
        }
    }

    //----------//
    // getPoint //
    //----------//
    /**
     * Report the point at 'index' position in current sequence
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
     * Report current sequence (meant for debugging)
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
     * Report a string which summarizes the current sequence of points
     * @return a string of the sequence points
     */
    public String getSequenceString ()
    {
        StringBuilder sb = new StringBuilder("[");
        boolean       started = false;

        for (Point p : getPoints()) {
            if (started) {
                sb.append(' ');
            }

            sb.append('(')
              .append(p.x)
              .append(',')
              .append(p.y)
              .append(')');
            started = true;
        }

        sb.append("]");

        return sb.toString();
    }

    //-------------------//
    // setStickyDistance //
    //-------------------//
    /**
     * Set the sticky distance for all methods that need a margin value
     * @param stickyDistance the new value, specified in pixels
     */
    public void setStickyDistance (int stickyDistance)
    {
        this.stickyDistance = stickyDistance;
    }

    //-------------------//
    // getStickyDistance //
    //-------------------//
    /**
     * Report the maximum distance (from a point, from a segment, for
     * colinearity)
     * @return the maximum distance, specified in pixels
     */
    public int getStickyDistance ()
    {
        return stickyDistance;
    }

    //-------------//
    // addListener //
    //-------------//
    /**
     * Add a listener to be notified on any modification in this BrokenLine
     * @param listener the entity to notify
     */
    public void addListener (Listener listener)
    {
        if (listener == null) {
            throw new IllegalArgumentException(
                "BrokenLine cannot add null listeners");
        }

        listeners.add(listener);
    }

    //----------//
    // addPoint //
    //----------//
    /**
     * Append a point at the end of the current sequence
     * @param point the new point to append
     */
    public void addPoint (Point point)
    {
        points.add(point);
        fireListeners();
    }

    //-----------//
    // findPoint //
    //-----------//
    /**
     * Find the first point of the current sequence which is close to the
     * provided point (less than sticky distance)
     * @param point the provided point
     * @return the point found, or null if not found
     */
    public Point findPoint (Point point)
    {
        Rectangle window = new Rectangle(
            point.x - stickyDistance,
            point.y - stickyDistance,
            2 * stickyDistance,
            2 * stickyDistance);

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
     * Find the closest segment (if any) which lies at a maximum of sticky
     * distance from the provided point.
     * @param point the provided point
     * @return the sequence point that starts the segment found
     * (or null if not found)
     */
    public Point findSegment (Point point)
    {
        Point  bestPoint = null;
        double bestDistSq = Double.MAX_VALUE;

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
            double        distSq = line.ptSegDistSq(point);

            if (distSq < bestDistSq) {
                bestPoint = prevPt;
                bestDistSq = distSq;
            }

            prevPt = pt;
        }

        if (bestDistSq <= (stickyDistance * stickyDistance)) {
            return bestPoint;
        } else {
            return null;
        }
    }

    //---------//
    // indexOf //
    //---------//
    /**
     * Retrieve the index of provided point
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
     * Insert a point at the specified index value
     * @param index the insertion position in the sequence
     * @param point the new point to insert
     */
    public void insertPoint (int   index,
                             Point point)
    {
        points.add(index, point);
        fireListeners();
    }

    //------------------//
    // insertPointAfter //
    //------------------//
    /**
     * Insert a point right after the specified point
     * @param point the new point to insert
     * @param after the point after which insertion must be done
     */
    public void insertPointAfter (Point point,
                                  Point after)
    {
        int ptIndex = points.indexOf(after);

        if (ptIndex != -1) {
            points.add(ptIndex + 1, point);
            fireListeners();
        } else {
            throw new IllegalArgumentException("Insertion point not found");
        }
    }

    //-----------//
    // movePoint //
    //-----------//
    /**
     * Move the specified point to the provided location
     * @param point the point to move
     * @param location the new location for this point
     */
    public void movePoint (Point point,
                           Point location)
    {
        if (point == null) {
            throw new IllegalArgumentException("Cannot move a null point");
        }

        point.setLocation(location);
        fireListeners();
    }

    //----------------//
    // removeListener //
    //----------------//
    /**
     * Remove a listener
     * @param listener the listener to remove
     */
    public void removeListener (Listener listener)
    {
        listeners.remove(listener);
    }

    //-------------//
    // removePoint //
    //-------------//
    /**
     * Remove the specified point from the current sequence
     * @param point the point to remove
     */
    public void removePoint (Point point)
    {
        points.remove(point);
        fireListeners();
    }

    //------//
    // size //
    //------//
    /**
     * Report the number of points in the current sequence
     * @return the current size of the points sequence
     */
    public int size ()
    {
        return points.size();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{BrokenLine " + getSequenceString() + "}";
    }

    //---------------//
    // fireListeners //
    //---------------//
    /**
     * Fire (a copy of) the set of registered listeners
     */
    protected void fireListeners ()
    {
        for (Listener listener : new LinkedHashSet<Listener>(listeners)) {
            try {
                listener.update(this);
            } catch (Exception ex) {
                logger.warning("Error notifyinh BrokenLine listener", ex);
            }
        }
    }

    //~ Inner Interfaces -------------------------------------------------------

    /**
     * Interface for a potential listener on this broken line, to be notified
     * of any modification in the line
     */
    public static interface Listener
    {
        //~ Methods ------------------------------------------------------------

        void update (BrokenLine brokenLine);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Max distance from a point or segment to get stuck to it */
        Constant.Integer stickyDistance = new Constant.Integer(
            "pixels",
            5,
            "Maximum distance from a point or segment to get stuck to it");
    }
}
