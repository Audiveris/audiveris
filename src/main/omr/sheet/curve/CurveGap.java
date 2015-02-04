//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         C u r v e G a p                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import omr.math.GeoPath;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;

/**
 * Class {@code CurveGap} handles a gap between a curve and an arc.
 *
 * @author Hervé Bitteur
 */
public abstract class CurveGap
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CurveGap.class);

    /** Pixels added on both sides of connection line to get some thickness. */
    protected static final int MARGIN = 1;

    //~ Instance fields ----------------------------------------------------------------------------
    /** End point of curve. */
    protected final Point p1;

    /** End point of arc. */
    protected final Point p2;

    /** Underlying area for the gap, which excludes both end points. */
    protected Area area;

    /** Gap vector to check empty locations. */
    protected int[] vector;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new CurveGap object.
     *
     * @param p1 curve end point
     * @param p2 arc end point
     */
    private CurveGap (Point p1,
                      Point p2)
    {
        this.p1 = p1;
        this.p2 = p2;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Compute the gap vector based on foreground pixels found in gap area.
     *
     * @param buf the binary buffer
     * @return the populated gap vector (in which a 1 value indicates a black pixel)
     */
    public int[] computeVector (ByteProcessor buf)
    {
        final Rectangle box = area.getBounds();

        for (int x = box.x, xBreak = box.x + box.width; x < xBreak; x++) {
            for (int y = box.y, yBreak = box.y + box.height; y < yBreak; y++) {
                if (area.contains(x, y)) {
                    if (0 == buf.get(x, y)) {
                        populateVector(x - box.x, y - box.y);
                    }
                }
            }
        }

        return vector;
    }

    /**
     * Factory method to create the CurveGap instance with proper orientation.
     *
     * @param p1 curve end point
     * @param p2 arc end point
     * @return either a Horizontal or a Vertical gap instance according to relative position of
     *         p1 and p2.
     */
    public static CurveGap create (Point p1,
                                   Point p2)
    {
        // Determine if line is rather horizontal or vertical
        final int dx = Math.abs(p2.x - p1.x);
        final int dy = Math.abs(p2.y - p1.y);

        if (dx >= dy) {
            return new Horizontal(p1, p2);
        } else {
            return new Vertical(p1, p2);
        }
    }

    public Area getArea ()
    {
        return area;
    }

    /**
     * Retrieve the largest hole (sequence of white pixels) found in the gap.
     *
     * @return the length of the largest hole found
     */
    public int getLargestGap ()
    {
        int maxHoleLength = 0;
        int holeStart = -1;

        for (int i = 0; i < vector.length; i++) {
            int val = vector[i];

            if (val == 0) {
                // No foreground pixel
                if (holeStart == -1) {
                    // Start of hole
                    holeStart = i;
                }
            } else {
                // Foreground pixel
                if (holeStart != -1) {
                    // Hole completed
                    maxHoleLength = Math.max(maxHoleLength, i - holeStart);
                }

                holeStart = -1;
            }
        }

        if (holeStart != -1) {
            maxHoleLength = Math.max(maxHoleLength, vector.length - holeStart);
        }

        return maxHoleLength;
    }

    /**
     * Populate vector with foreground pixel found
     *
     * @param x abscissa of pixel (WRT area bounds)
     * @param y ordinate of pixel (WRT area bounds)
     */
    protected abstract void populateVector (int x,
                                            int y);

    //~ Inner Classes ------------------------------------------------------------------------------
    /**
     * For rather horizontal gaps.
     */
    public static class Horizontal
            extends CurveGap
    {
        //~ Constructors ---------------------------------------------------------------------------

        /**
         * Creates a new CurveGap.Horizontal object.
         *
         * @param p1 curve end point
         * @param p2 arc end point
         */
        public Horizontal (Point p1,
                           Point p2)
        {
            super(p1, p2);
            vector = new int[Math.abs(p2.x - p1.x) - 1];
            area = computeArea();
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected final void populateVector (int x,
                                             int y)
        {
            vector[x] = 1;
        }

        private Area computeArea ()
        {
            final GeoPath path;

            if (p2.x > p1.x) {
                path = new GeoPath(new Line2D.Double(p1.x + 1, p1.y - MARGIN, p2.x, p2.y - MARGIN));
                path.append(new Line2D.Double(p2.x, p2.y + MARGIN, p1.x + 1, p1.y + MARGIN), true);
            } else {
                path = new GeoPath(new Line2D.Double(p1.x, p1.y - MARGIN, p2.x + 1, p2.y - MARGIN));
                path.append(new Line2D.Double(p2.x + 1, p2.y + MARGIN, p1.x, p1.y + MARGIN), true);
            }

            path.closePath();

            return new Area(path);
        }
    }

    /**
     * For rather vertical gaps.
     */
    public static class Vertical
            extends CurveGap
    {
        //~ Constructors ---------------------------------------------------------------------------

        /**
         * Creates a new CurveGap.Vertical object.
         *
         * @param p1 curve end point
         * @param p2 arc end point
         */
        public Vertical (Point p1,
                         Point p2)
        {
            super(p1, p2);
            vector = new int[Math.abs(p2.y - p1.y) - 1];
            area = computeArea();
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected final void populateVector (int x,
                                             int y)
        {
            vector[y] = 1;
        }

        private Area computeArea ()
        {
            final GeoPath path;

            if (p2.y > p1.y) {
                path = new GeoPath(new Line2D.Double(p1.x - MARGIN, p1.y + 1, p2.x - MARGIN, p2.y));
                path.append(new Line2D.Double(p2.x + MARGIN, p2.y, p1.x + MARGIN, p1.y + 1), true);
            } else {
                path = new GeoPath(new Line2D.Double(p1.x - MARGIN, p1.y, p2.x - MARGIN, p2.y + 1));
                path.append(new Line2D.Double(p2.x + MARGIN, p2.y + 1, p1.x + MARGIN, p1.y), true);
            }

            path.closePath();

            return new Area(path);
        }
    }
}
