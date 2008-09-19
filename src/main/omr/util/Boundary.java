//----------------------------------------------------------------------------//
//                                                                            //
//                              B o u n d a r y                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.util;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import net.jcip.annotations.NotThreadSafe;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.util.Arrays;

/**
 * Class <code>Boundary</code> handles the boundary of a 2D area, defined by a
 * sequence of points which can be modified at any time. Several features use a
 * "stickyDistance" constant which defines proximity margins.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@NotThreadSafe
public class Boundary
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Boundary.class);

    //~ Instance fields --------------------------------------------------------

    /** Handling of the boundary is delegated to a Polygon */
    private final Polygon polygon = new Polygon();

    /** Default sticky distance */
    private int stickyDistance = getDefaultStickyDistance();

    //~ Constructors -----------------------------------------------------------

    //----------//
    // Boundary //
    //----------//
    /**
     * Creates a new Boundary object with an initially empty sequence of points.
     */
    public Boundary ()
    {
    }

    //----------//
    // Boundary //
    //----------//
    /**
     * Creates a new Boundary object with a few initial points
     * @param points array of initial points
     */
    public Boundary (Point... points)
    {
        for (Point point : points) {
            polygon.addPoint(point.x, point.y);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the rectangular bounds that enclose this (perhaps complex) boundary
     * @return the rectangular bounds
     */
    public Rectangle getBounds ()
    {
        return polygon.getBounds();
    }

    //--------------------------//
    // getDefaultStickyDistance //
    //--------------------------//
    /**
     * Report the default sticky distance.
     * This value can be overridden for the current Boundary instance, through
     * method {@link #setStickyDistance}.
     * @return the (default) maximum distance
     */
    public static int getDefaultStickyDistance ()
    {
        return constants.stickyDistance.getValue();
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
        checkIndex(index);

        return new Point(polygon.xpoints[index], polygon.ypoints[index]);
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

        for (Point p : getSequence()) {
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
     * @param stickyDistance the new value
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
     * @return the maximum distance
     */
    public int getStickyDistance ()
    {
        return stickyDistance;
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
        polygon.addPoint(point.x, point.y);
    }

    //-------------//
    // areColinear //
    //-------------//
    /**
     * Check whether the 3 specified points are colinear (within stickyDistance)
     * @param index1 index of first point
     * @param index2 index of second point
     * @param index3 index of third point
     * @return true if the 3 point are colinear or nearly so
     */
    public boolean areColinear (int index1,
                                int index2,
                                int index3)
    {
        checkIndex(index1);
        checkIndex(index2);
        checkIndex(index3);

        Line2D.Double line = new Line2D.Double(
            polygon.xpoints[index1],
            polygon.ypoints[index1],
            polygon.xpoints[index3],
            polygon.ypoints[index3]);
        double        dist = line.ptLineDist(
            polygon.xpoints[index2],
            polygon.ypoints[index2]);

        return dist <= stickyDistance;
    }

    //----------//
    // contains //
    //----------//
    /**
     * Check whether the provided point lies within the boundary
     * @param point the provided point
     * @return true if the provided point lies within the boundary
     */
    public boolean contains (Point point)
    {
        return polygon.contains(point);
    }

    //-----------//
    // findPoint //
    //-----------//
    /**
     * Find the first point of the current sequence which is close to the
     * provided point (less than sticky distance)
     * @param point the provided point
     * @return the index of the point found, or -1 if not found
     */
    public int findPoint (Point point)
    {
        Rectangle window = new Rectangle(
            point.x - stickyDistance,
            point.y - stickyDistance,
            2 * stickyDistance,
            2 * stickyDistance);

        for (int i = 0; i < polygon.npoints; i++) {
            if (window.contains(polygon.xpoints[i], polygon.ypoints[i])) {
                return i;
            }
        }

        return -1;
    }

    //-------------//
    // findSegment //
    //-------------//
    /**
     * Find the closest segment (if any) which lies at a maximum of sticky
     * distance from the provided point.
     * @param point the provided point
     * @return the index of the sequence point that starts the segment found
     * (or -1 if not found)
     */
    public int findSegment (Point point)
    {
        int    bestIndex = -1;
        double bestDistSq = Double.MAX_VALUE;

        for (int i = 0; i < polygon.npoints; i++) {
            // Beware of the end of the sequence
            int           next = (i == (polygon.npoints - 1)) ? 0 : (i + 1);
            Line2D.Double line = new Line2D.Double(
                polygon.xpoints[i],
                polygon.ypoints[i],
                polygon.xpoints[next],
                polygon.ypoints[next]);
            double        distSq = line.ptSegDistSq(point.x, point.y);

            if (distSq < bestDistSq) {
                bestIndex = i;
                bestDistSq = distSq;
            }
        }

        if (bestDistSq <= (stickyDistance * stickyDistance)) {
            return bestIndex;
        } else {
            return -1;
        }
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
        if (index >= polygon.npoints) {
            addPoint(point);
        } else {
            checkIndex(index);

            // Check we have enough room
            if ((polygon.npoints >= polygon.xpoints.length) ||
                (polygon.npoints >= polygon.ypoints.length)) {
                int newLength = polygon.npoints * 2;
                polygon.xpoints = Arrays.copyOf(polygon.xpoints, newLength);
                polygon.ypoints = Arrays.copyOf(polygon.ypoints, newLength);
            }

            System.arraycopy(
                polygon.xpoints,
                index,
                polygon.xpoints,
                index + 1,
                polygon.npoints - index);
            System.arraycopy(
                polygon.ypoints,
                index,
                polygon.ypoints,
                index + 1,
                polygon.npoints - index);
            polygon.xpoints[index] = point.x;
            polygon.ypoints[index] = point.y;
            polygon.npoints++;
            polygon.invalidate();
        }
    }

    //-----------//
    // movePoint //
    //-----------//
    /**
     * Move the specified point to the provided location
     * @param index the index of the point to move
     * @param location the new location for this point
     */
    public void movePoint (int   index,
                           Point location)
    {
        checkIndex(index);
        polygon.xpoints[index] = location.x;
        polygon.ypoints[index] = location.y;
        polygon.invalidate();
    }

    //-------------//
    // removePoint //
    //-------------//
    /**
     * Remove the specified point from the current sequence
     * @param index index of the point to remove
     */
    public void removePoint (int index)
    {
        checkIndex(index);
        System.arraycopy(
            polygon.xpoints,
            index + 1,
            polygon.xpoints,
            index,
            polygon.npoints - index - 1);
        System.arraycopy(
            polygon.ypoints,
            index + 1,
            polygon.ypoints,
            index,
            polygon.npoints - index - 1);
        polygon.npoints--;
        polygon.invalidate();
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint the boundary in the provided graphic context.
     *
     * @param g     the Graphics context
     * @param ratio the display zoom ratio
     */
    public void render (Graphics g,
                        double   ratio)
    {
        Graphics2D      g2 = (Graphics2D) g;
        int             radius = getStickyDistance();

        // Draw the polygon
        AffineTransform saveAT = g2.getTransform();
        g2.transform(AffineTransform.getScaleInstance(ratio, ratio));
        g2.drawPolygon(polygon);

        // Mark the points
        for (int i = 0; i < polygon.npoints; i++) {
            g2.drawRect(
                polygon.xpoints[i] - radius,
                polygon.ypoints[i] - radius,
                2 * radius,
                2 * radius);
        }

        g2.setTransform(saveAT); // Restore
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
        return polygon.npoints;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Boundary " + getSequenceString() + "}";
    }

    //-------------//
    // getSequence //
    //-------------//
    /**
     * Report current sequence (meant for debugging)
     * @return the array (perhaps empty) of current points
     */
    Point[] getSequence ()
    {
        Point[] sequence = new Point[polygon.npoints];

        for (int i = 0; i < polygon.npoints; i++) {
            sequence[i] = new Point(polygon.xpoints[i], polygon.ypoints[i]);
        }

        return sequence;
    }

    //------------//
    // checkIndex //
    //------------//
    private void checkIndex (int index)
    {
        if ((index < 0) || (index >= polygon.npoints)) {
            throw new IllegalArgumentException(
                "index out of bound 0.." + polygon.npoints);
        }
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
