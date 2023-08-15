//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         A r e a U t i l                                        //
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
package org.audiveris.omr.math;

import ij.process.ByteProcessor;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * Class <code>AreaUtil</code> gathers static utility methods for Area instances.
 *
 * @author Hervé Bitteur
 */
public abstract class AreaUtil
{
    //~ Constructors -------------------------------------------------------------------------------

    // Not meant to be instantiated.
    private AreaUtil ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------------------//
    // horizontalParallelogram //
    //-------------------------//
    /**
     * Create a parallelogram mostly horizontal, where left and right
     * sides are short and vertical.
     * This is most useful for beams and ledgers.
     * <p>
     * Nota: the defining points are meant to be the extrema points
     * <b>on the borders of</b> the parallelogram.
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
        return new Area(horizontalParallelogramPath(left, right, height));
    }

    //-------------------------//
    // horizontalParallelogram //
    //-------------------------//
    /**
     * Create a parallelogram mostly horizontal, where left and right
     * sides are short and vertical.
     * This is most useful for beams and ledgers.
     * <p>
     * Nota: the defining points are meant to be the extrema points
     * <b>on the borders of</b> the parallelogram.
     *
     * @param left   left point of median line
     * @param right  right point of median line
     * @param height total height
     * @return the created path
     */
    public static Path2D horizontalParallelogramPath (Point2D left,
                                                      Point2D right,
                                                      double height)
    {
        final double dy = height / 2; // Half height
        final Path2D path = new Path2D.Double();
        path.moveTo(left.getX(), left.getY() - dy); // Upper left
        path.lineTo(right.getX(), right.getY() - dy); // Upper right
        path.lineTo(right.getX(), right.getY() + dy); // Lower right
        path.lineTo(left.getX(), left.getY() + dy); // Lower left
        path.closePath();

        return path;
    }

    //--------------//
    // intersection //
    //--------------//
    /**
     * Check whether the two provided areas intersect one another.
     *
     * @param a1 some area
     * @param a2 some other area
     * @return true if there is a non-empty intersection
     */
    public static boolean intersection (Area a1,
                                        Area a2)
    {
        Area copy = new Area(a1);
        copy.intersect(a2);

        return !copy.isEmpty();
    }

    //--------------//
    // verticalCore //
    //--------------//
    /**
     * Perform core analysis on a vertical shape.
     *
     * @param filter     source of foreground / background
     * @param leftLimit  limit on left side
     * @param rightLimit limit on right side
     * @return the CoreData measured
     */
    public static CoreData verticalCore (ByteProcessor filter,
                                         GeoPath leftLimit,
                                         GeoPath rightLimit)
    {
        // Inspect each horizontal row of the stick
        int largestGap = 0; // Largest gap so far
        int lastBlackY = -1; // Ordinate of last black row
        int lastWhiteY = -1; // Ordinate of last white row
        int whiteCount = 0; // Count of rows where item is white (broken)

        final int yMin = (int) Math.ceil(leftLimit.getFirstPoint().getY());
        final int yMax = (int) Math.floor(leftLimit.getLastPoint().getY());

        for (int y = yMin; y <= yMax; y++) {
            final int xMin = (int) Math.floor(leftLimit.xAtY(y));
            final int xMax = (int) Math.ceil(rightLimit.xAtY(y));

            // Make sure the row is not empty
            boolean empty = true;

            for (int x = xMin; x <= xMax; x++) {
                if (filter.get(x, y) == 0) {
                    empty = false;

                    break;
                }
            }

            if (empty) {
                whiteCount++;
                lastWhiteY = y;

                continue;
            }

            // End of gap?
            if ((lastWhiteY != -1) && (lastBlackY != -1)) {
                largestGap = Math.max(largestGap, lastWhiteY - lastBlackY);
                lastWhiteY = -1;
            }

            lastBlackY = y;
        }

        int height = yMax - yMin + 1;
        double whiteRatio = (double) whiteCount / height;

        return new CoreData(height, largestGap, whiteRatio);
    }

    //-----------------------//
    // verticalParallelogram //
    //-----------------------//
    /**
     * Create a parallelogram mostly vertical, where top and bottom sides are short and
     * horizontal.
     * This is most useful for stems.
     * <p>
     * Nota: the defining points are meant to be the extrema points
     * <b>on the borders of</b> the parallelogram.
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
        path.lineTo(top.getX() + dx, top.getY()); // Upper right
        path.lineTo(bottom.getX() + dx, bottom.getY()); // Lower right
        path.lineTo(bottom.getX() - dx, bottom.getY()); // Lower left
        path.closePath();

        return new Area(path);
    }

    //----------------//
    // verticalRibbon //
    //----------------//
    /**
     * Create a ribbon mostly vertical, where top and bottom are short and horizontal
     * and left and right sides are long and rather vertical.
     * This is most useful for barlines.
     * <p>
     * Nota: the defining points are meant to be the precise extrema Point2D values <b>on the
     * borders of</b> the ribbon.
     *
     * @param median the defining vertical line (either a straight BasicLine or a more wavy
     *               NaturalSpline)
     * @param width  ribbon width
     * @return the created area
     */
    public static Area verticalRibbon (Shape median,
                                       double width)
    {
        final double dx = width / 2; // Half width
        final GeoPath path = new GeoPath();

        // Left line
        path.append(median.getPathIterator(AffineTransform.getTranslateInstance(-dx, 0)), false);

        // Right line (reversed)
        path.append(
                ReversePathIterator.getReversePathIterator(
                        median,
                        AffineTransform.getTranslateInstance(dx, 0)),
                true);

        path.closePath();

        return new Area(path);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // CoreData //
    //----------//
    /**
     * Output data from a core analysis.
     */
    public static class CoreData
    {

        /** Total area length. (height for vertical, width for horizontal) */
        public final int length;

        /** Length of longest gap found. */
        public final int gap;

        /** Ratio of white elements on total length. */
        public final double whiteRatio;

        /**
         * Create a CoreData object.
         *
         * @param length     area length to measure
         * @param gap        longest gag found
         * @param whiteRatio ratio of white on total length
         */
        public CoreData (int length,
                         int gap,
                         double whiteRatio)
        {
            this.length = length;
            this.gap = gap;
            this.whiteRatio = whiteRatio;
        }

        @Override
        public String toString ()
        {
            return String.format(
                    "length:%d largestGap:%d white:%.0f%s",
                    length,
                    gap,
                    100 * whiteRatio,
                    "%");
        }
    }
}
