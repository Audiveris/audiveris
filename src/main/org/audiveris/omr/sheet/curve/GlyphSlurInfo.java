//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    G l y p h S l u r I n f o                                   //
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.math.CubicUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;

import java.awt.Point;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code GlyphSlurInfo} is a degenerated {@link SlurInfo}, meant for a manual
 * slur defined by its underlying glyph.
 *
 * @author Hervé Bitteur
 */
public class GlyphSlurInfo
        extends SlurInfo
{
    //~ Constructors -------------------------------------------------------------------------------

    private GlyphSlurInfo (Glyph glyph,
                           List<Point> keyPoints)
    {
        super(0, null, null, keyPoints, null, Collections.EMPTY_LIST, 0);
        this.glyph = glyph;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Create proper GlyphSlurInfo for a slur defined by its glyph.
     * The bezier curve is directly computed from the glyph.
     *
     * @param glyph the defining glyph
     * @return the corresponding slur info
     */
    public static GlyphSlurInfo create (Glyph glyph)
    {
        final List<Point> points = new KeyPointsBuilder(glyph).retrieveKeyPoints();
        final GlyphSlurInfo info = new GlyphSlurInfo(glyph, points);

        // Curve (nota: s1 and s2 are not control points, but intermediate points located on curve)
        final Point s0 = points.get(0);
        final Point s1 = points.get(1);
        final Point s2 = points.get(2);
        final Point s3 = points.get(3);

        info.curve = new CubicCurve2D.Double(
                s0.x,
                s0.y,
                ((-(5 * s0.x) + (18 * s1.x)) - (9 * s2.x) + (2 * s3.x)) / 6,
                ((-(5 * s0.y) + (18 * s1.y)) - (9 * s2.y) + (2 * s3.y)) / 6,
                (((2 * s0.x) - (9 * s1.x) + (18 * s2.x)) - (5 * s3.x)) / 6,
                (((2 * s0.y) - (9 * s1.y) + (18 * s2.y)) - (5 * s3.y)) / 6,
                s3.x,
                s3.y);

        // Above or below?
        final int ccw = new Line2D.Double(s0, s1).relativeCCW(s3);
        info.above = -ccw;
        info.bisUnit = info.computeBisector(info.above > 0);

        return info;
    }

    //----------//
    // getCurve //
    //----------//
    @Override
    public CubicCurve2D getCurve ()
    {
        return curve;
    }

    //--------------//
    // getEndVector //
    //--------------//
    @Override
    public Point2D getEndVector (boolean reverse)
    {
        // We use the control points to retrieve tangent vector
        Point2D vector = reverse ? PointUtil.subtraction(curve.getP1(), curve.getCtrlP1())
                : PointUtil.subtraction(curve.getP2(), curve.getCtrlP2());

        // Normalize
        vector = PointUtil.times(vector, 1.0 / PointUtil.length(vector));

        return vector;
    }

    //-------------//
    // getMidPoint //
    //-------------//
    @Override
    public Point2D getMidPoint ()
    {
        return CubicUtil.getMidPoint(curve);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------------//
    // KeyPointsBuilder //
    //------------------//
    /**
     * Class {@code KeyPointsBuilder} builds a slur from a provided glyph.
     * <p>
     * This class is meant for manual slur built on a selected glyph, by using exactly 4 key points
     * evenly spaced along the glyph abscissa axis.
     */
    private static class KeyPointsBuilder
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Glyph glyph;

        private final RunTable rt;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new {@code KeyPointsBuilder} object.
         *
         * @param glyph the underlying glyph
         */
        public KeyPointsBuilder (Glyph glyph)
        {
            this.glyph = glyph;
            rt = glyph.getRunTable();
        }

        //~ Methods --------------------------------------------------------------------------------
        public List<Point> retrieveKeyPoints ()
        {
            if (rt.getOrientation() != Orientation.VERTICAL) {
                throw new IllegalArgumentException("Slur glyph runs must be vertical");
            }

            // Retrieve the 4 points WRT glyph bounds
            final int width = glyph.getWidth();
            final List<Point> points = new ArrayList<Point>(4);
            points.add(vectorAtX(0));
            points.add(vectorAtX((int) Math.rint(width / 3.0)));
            points.add(vectorAtX((int) Math.rint((2 * width) / 3.0)));
            points.add(vectorAtX(width - 1));

            // Use sheet coordinates rather than glyph-based coordinates
            final int dx = glyph.getLeft();
            final int dy = glyph.getTop();

            for (Point point : points) {
                point.translate(dx, dy);
            }

            return points;
        }

        private Point lookLeft (int x0)
        {
            for (int x = x0 - 1; x >= 0; x--) {
                int y = yAtX(x);

                if (y != -1) {
                    return new Point(x, y);
                }
            }

            return null;
        }

        private Point lookRight (int x0)
        {
            int size = rt.getSize();

            for (int x = x0 + 1; x < size; x++) {
                int y = yAtX(x);

                if (y != -1) {
                    return new Point(x, y);
                }
            }

            return null;
        }

        /**
         * Report the best offset for a given x offset.
         * <p>
         * If no run is found at x offset, columns on left and on right are searched, and y offset
         * is then interpolated.
         *
         * @param x abscissa offset since glyph left side
         * @return vector offset since glyph top left corner
         */
        private Point vectorAtX (int x)
        {
            int y = yAtX(x);

            if (y != -1) {
                return new Point(x, y);
            }

            Point left = lookLeft(x);
            Point right = lookRight(x);

            return PointUtil.rounded(LineUtil.intersectionAtX(new Line2D.Double(left, right), x));
        }

        /**
         * Read the y offset for a given x offset.
         *
         * @param x given x offset
         * @return -1 if there is no run at x, otherwise the middle of run
         */
        private int yAtX (int x)
        {
            if (rt.isSequenceEmpty(x)) {
                return -1;
            }

            int yMin = Integer.MAX_VALUE;
            int yMax = Integer.MIN_VALUE;

            for (Iterator<Run> it = rt.iterator(x); it.hasNext();) {
                Run run = it.next();
                yMin = Math.min(yMin, run.getStart());
                yMax = Math.max(yMax, run.getStop());
            }

            return yMin + ((yMax - yMin) / 2);
        }
    }
}
