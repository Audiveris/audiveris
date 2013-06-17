//----------------------------------------------------------------------------//
//                                                                            //
//                                  L i n e                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

/**
 * Interface {@code Line} handles the equation of a line (or more
 * generally some curved line for which Y can be computed from X),
 * whatever its orientation.
 *
 * @author Hervé Bitteur
 */
public interface Line
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Compute the orthogonal distance between the line and the
     * provided point.
     * Note that the distance may be negative.
     *
     * @param x the point abscissa
     * @param y the point ordinate
     * @return the algebraic orthogonal distance
     */
    double distanceOf (double x,
                       double y);

    /**
     * Return -b/a, from a*x + b*y +c
     *
     * @return the x/y coefficient
     */
    double getInvertedSlope ();

    /**
     * Return the mean quadratic distance of the defining population
     * of points to the resulting line.
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
     * Add the whole population of another line, which results in
     * merging this other line with the line at hand.
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
     * Check if line is horizontal ('a' coeff is null)
     *
     * @return true if horizontal
     */
    boolean isHorizontal ();

    /**
     * Check if line is vertical ('b' coeff is null).
     *
     * @return true if vertical
     */
    boolean isVertical ();

    /**
     * Remove the whole population of points.
     * The line is not immediately usable, it needs now to include defining
     * points.
     */
    void reset ();

    /**
     * Return a new line whose coordinates are swapped with respect
     * to this one.
     *
     * @return a new X/Y swapped line
     */
    Line swappedCoordinates ();

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

    //~ Inner Classes ----------------------------------------------------------
    /**
     * Specific exception raised when trying to invert a
     * non-invertible line.
     */
    static class NonInvertibleLineException
            extends RuntimeException
    {
        //~ Constructors -------------------------------------------------------

        NonInvertibleLineException (String message)
        {
            super(message);
        }
    }

    /**
     * Specific exception raised when trying to use a line with
     * undefined parameters.
     */
    static class UndefinedLineException
            extends RuntimeException
    {
        //~ Constructors -------------------------------------------------------

        UndefinedLineException (String message)
        {
            super(message);
        }
    }
}
