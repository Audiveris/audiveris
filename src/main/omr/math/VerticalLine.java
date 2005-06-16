//-----------------------------------------------------------------------//
//                                                                       //
//                        V e r t i c a l L i n e                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.math;

/**
 * Class <code>VerticalLine</code> is a special kind of Line, meant for
 * vertical or near-vertical lines. This is done by computing and storing
 * the equation in the form of : x = ay + b (instead of y = ax +b)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class VerticalLine
        extends Line
{
    //~ Constructors ------------------------------------------------------

    //--------------//
    // VerticalLine //
    //--------------//

    /**
     * Create an empty vertical line
     */
    public VerticalLine ()
    {
        super.reset();
    }

    //--------------//
    // VerticalLine //
    //--------------//

    /**
     * Create a vertical line, when we already know its coeffs
     *
     * @param a slope
     * @param b offset (the x offset, when y equals 0)
     */
    public VerticalLine (double a,
                         double b)
    {
        super(a, b);
    }

    //--------------//
    // VerticalLine //
    //--------------//

    /**
     * Create a vertical line, with Least Square Fitting
     *
     * @param xVals abscissas of the defining points
     * @param yVals ordinates of the defining points
     */
    public VerticalLine (double[] xVals,
                         double[] yVals)
    {
        super(yVals, xVals);
    }

    //~ Methods -----------------------------------------------------------

    //------------//
    // distanceOf //
    //------------//

    /**
     * Compute the algebraic distance of the provided point to the line
     *
     * @param x abscissa of the point
     * @param y ordinate of the point
     *
     * @return the algebraic distance
     */
    public double distanceOf (double x,
                              double y)
    {
        return super.distanceOf(y, x);
    }

    //---------//
    // include //
    //---------//

    /**
     * Add one defining point.
     *
     * @param x abscissa of the point
     * @param y ordinate of the point
     */
    public void include (double x,
                         double y)
    {
        super.include(y, x);
    }

    //---------//
    // include //
    //---------//

    /**
     * Merge a whole other line with this one
     *
     * @param other the line to merge
     *
     * @return the result of the merge
     */
    public VerticalLine include (VerticalLine other)
    {
        n += other.n;
        sx += other.sx;
        sy += other.sy;
        sx2 += other.sx2;
        sy2 += other.sy2;
        sxy += other.sxy;
        dirty = true;

        return this;
    }

    //----------//
    // toString //
    //----------//

    /**
     * A special equation formatting
     *
     * @return the readable equation
     */
    public String toString ()
    {
        if (dirty) {
            compute();
        }

        return "{VerticalLine x = " + (float) a + "*y" + ((b < 0)
                                                          ? " "
                                                          : " +") + (float) b
               + "}";
    }

    //-----//
    // xAt //
    //-----//

    /**
     * Compute x for given y value
     *
     * @param y the imposed ordinate
     *
     * @return the corresponding x
     */
    public double xAt (double y)
    {
        return super.yAt(y);
    }

    //-----//
    // xAt //
    //-----//

    /**
     * Compute x for given y value
     *
     * @param y the imposed ordinate
     *
     * @return the corresponding x
     */
    public int xAt (int y)
    {
        return (int) Math.rint(xAt((double) y));
    }

    //-----//
    // yAt //
    //-----//

    /**
     * Compute y for given x value
     *
     * @param x the imposed abscissa
     *
     * @return the corresponding x
     */
    public double yAt (double x)
    {
        return super.xAt(x);
    }

    //-----//
    // yAt //
    //-----//

    /**
     * Compute y for given x value
     *
     * @param x the imposed abscissa
     *
     * @return the corresponding x
     */
    public int yAt (int x)
    {
        return (int) Math.rint(yAt((double) x));
    }
}
