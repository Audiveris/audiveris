//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         C u r v e G a p                                        //
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

import ij.process.ByteProcessor;

import org.audiveris.omr.math.GeoPath;

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

    private static final Logger logger = LoggerFactory.getLogger(CurveGap.class);

    /** Pixels added on both sides of connection line to get some thickness. */
    protected static final int MARGIN = 1;

    /** End point of curve. */
    protected final Point p1;

    /** End point of arc. */
    protected final Point p2;

    /** Underlying area for the gap, which excludes both end points. */
    protected Area area;

    /** Gap vector to check empty locations. */
    protected int[] vector;

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
     * Report the lookup area.
     *
     * @return lookup area
     */
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

    /**
     * For rather horizontal gaps.
     */
    public static class Horizontal
            extends CurveGap
    {

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
