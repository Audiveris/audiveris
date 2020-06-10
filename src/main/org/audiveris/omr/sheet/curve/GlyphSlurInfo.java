//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    G l y p h S l u r I n f o                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.run.RunTableFactory;

/**
 * Class {@code GlyphSlurInfo} is a degenerated {@link SlurInfo}, meant for a manual
 * slur defined by its underlying glyph.
 *
 * @author Hervé Bitteur
 */
public class GlyphSlurInfo
        extends SlurInfo
{

    private GlyphSlurInfo (Glyph glyph,
                           List<Point> keyPoints)
    {
        super(0, null, null, keyPoints, null, Collections.emptyList(), 0);
        this.glyph = glyph;
    }

    //----------//
    // getCurve //
    //----------//
    @Override
    public CubicCurve2D getCurve ()
    {
        return curve;
    }

    //-------------//
    // getMidPoint //
    //-------------//
    /**
     * Report the middle point of the slur.
     *
     * @return middle point
     */
    @Override
    public Point2D getMidPoint ()
    {
        return CubicUtil.getMidPoint(curve);
    }

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

        // Curve points
        final Point s0 = points.get(0);
        final Point s1 = points.get(1);
        final Point s2 = points.get(2);
        final Point s3 = points.get(3);

        info.curve = CubicUtil.createCurve(s0, s1, s2, s3);

        // Above or below?
        info.above = CubicUtil.above(info.curve);

        return info;
    }

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

        private final Glyph glyph;

        private final RunTable rt;

        /**
         * Creates a new {@code KeyPointsBuilder} object.
         *
         * @param glyph the underlying glyph
         */
        KeyPointsBuilder (Glyph glyph)
        {
            this.glyph = glyph;

            RunTable table = glyph.getRunTable();

            if (table.getOrientation() != Orientation.VERTICAL) {
                ByteProcessor buf = table.getBuffer();
                table = new RunTableFactory(Orientation.VERTICAL).createTable(buf);
            }

            rt = table;
        }

        public List<Point> retrieveKeyPoints ()
        {
            // Retrieve the 4 points WRT glyph bounds
            final int width = glyph.getWidth();
            final List<Point> points = new ArrayList<>(4);
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
