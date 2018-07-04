//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            L i n e                                             //
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
package org.audiveris.omr.math;

import java.awt.Rectangle;

/**
 * Interface {@code Line} handles the equation of a line (or more generally some curved
 * line for which Y can be computed from X), whatever its orientation.
 *
 * @author Hervé Bitteur
 */
public interface Line
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Compute the orthogonal distance between the line and the provided point.
     * Note that the distance may be negative.
     *
     * @param x the point abscissa
     * @param y the point ordinate
     * @return the algebraic orthogonal distance
     */
    double distanceOf (double x,
                       double y);

    /**
     * Report the bounding rectangle.
     *
     * @return the line bounds
     */
    Rectangle getBounds ();

    /**
     * Return -b/a, from a*x + b*y +c
     *
     * @return the x/y coefficient
     */
    double getInvertedSlope ();

    /**
     * Return the mean quadratic distance of the defining population of points to the
     * resulting line.
     * This can be used to measure how well the line fits the points.
     *
     * @return the absolute value of the mean distance
     */
    double getMeanDistance ();

    /**
     * Return the cardinality of the population of defining points.
     *
     * @return the number of defining points so far
     */
    int getNumberOfPoints ();

    /**
     * Return -a/b, from a*x + b*y +c.
     *
     * @return the y/x coefficient
     */
    double getSlope ();

    /**
     * Add the whole population of another line, which results in merging this other
     * line with the line at hand.
     *
     * @param other the other line
     * @return this augmented line, which permits to chain the additions.
     */
    Line includeLine (Line other);

    /**
     * Add the coordinates of a point in the population of points.
     *
     * @param x abscissa of the new point
     * @param y ordinate of the new point
     */
    void includePoint (double x,
                       double y);

    /**
     * Remove the whole population of points.
     * The line is not immediately usable, it needs now to include defining points.
     */
    void reset ();

    /**
     * Return a new line whose coordinates are swapped with respect to this one.
     *
     * @return a new X/Y swapped line
     */
    Line swappedCoordinates ();

    /**
     * Report the line path.
     *
     * @return the path of the underlying line.
     */
    GeoPath toPath ();

    /**
     * Retrieve the abscissa where the line crosses the given ordinate y.
     * Beware of horizontal lines !!!
     *
     * @param y the imposed ordinate
     * @return the corresponding x value
     */
    double xAtY (double y);

    /**
     * Retrieve the abscissa where the line crosses the given ordinate y,
     * rounded to the nearest integer value.
     * Beware of horizontal lines !!!
     *
     * @param y the imposed ordinate
     * @return the corresponding x value
     */
    int xAtY (int y);

    /**
     * Similar functionality as xAtY, but also accepts ordinates outside the line
     * ordinate range by extrapolating the line based on start and stop points.
     *
     * @param y the provided ordinate
     * @return the abscissa value at this ordinate
     */
    double xAtYExt (double y);

    /**
     * Retrieve the ordinate where the line crosses the given abscissa x.
     * Beware of vertical lines !!!
     *
     * @param x the imposed abscissa
     * @return the corresponding y value
     */
    double yAtX (double x);

    /**
     * Retrieve the ordinate where the line crosses the given abscissa x,
     * rounded to the nearest integer value.
     * Beware of vertical lines !!!
     *
     * @param x the imposed abscissa
     * @return the corresponding y value
     */
    int yAtX (int x);

    /**
     * Similar functionality as yAtX, but also accepts abscissae outside the line
     * abscissa range by extrapolating the line based on start and stop points.
     *
     * @param x the provided abscissa
     * @return the ordinate value at this abscissa
     */
    double yAtXExt (double x);

    //~ Inner Classes ------------------------------------------------------------------------------
    /**
     * Specific exception raised when trying to invert a non-invertible line.
     */
    static class NonInvertibleLineException
            extends RuntimeException
    {
        //~ Constructors ---------------------------------------------------------------------------

        NonInvertibleLineException (String message)
        {
            super(message);
        }
    }

    /**
     * Specific exception raised when trying to use a line with undefined parameters.
     */
    static class UndefinedLineException
            extends RuntimeException
    {
        //~ Constructors ---------------------------------------------------------------------------

        UndefinedLineException (String message)
        {
            super(message);
        }
    }
}
