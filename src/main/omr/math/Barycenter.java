//----------------------------------------------------------------------------//
//                                                                            //
//                            B a r y c e n t e r                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.geom.Point2D;

/**
 * Class {@code Barycenter} is meant to cumulate data when computing
 * barycenter.
 */
public class Barycenter
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The total weight (such as the number of pixels).
     * At any time, the barycenter coordinates are respectively xx/weight
     * and yy/weight
     */
    private double weight;

    /** The weighted abscissa */
    private double xx;

    /** The weighted ordinate */
    private double yy;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // Barycenter //
    //------------//
    /**
     * Creates a new Barycenter object.
     */
    public Barycenter ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getWeight //
    //-----------//
    public final double getWeight ()
    {
        return weight;
    }

    //------//
    // getX //
    //------//
    /**
     * Report the current barycenter abscissa.
     *
     * @return current abscissa
     */
    public final double getX ()
    {
        return xx / weight;
    }

    //------//
    // getY //
    //------//
    /**
     * Report the current barycenter ordinate.
     *
     * @return current ordinate
     */
    public final double getY ()
    {
        return yy / weight;
    }

    //---------//
    // include //
    //---------//
    /**
     * Include another barycenter.
     *
     * @param weight total weight of this other barycenter
     * @param x      abscissa
     * @param y      ordinate
     */
    public final void include (double weight,
                               double x,
                               double y)
    {
        this.weight += weight;
        this.xx += (x * weight);
        this.yy += (y * weight);
    }

    //---------//
    // include //
    //---------//
    /**
     * Include another barycenter.
     *
     * @param that the other barycenter to include
     */
    public final void include (Barycenter that)
    {
        this.weight += that.weight;
        this.xx += that.xx;
        this.yy += that.yy;
    }

    //---------//
    // include //
    //---------//
    /**
     * Include one point (with default weight assigned to 1).
     *
     * @param x point abscissa
     * @param y point ordinate
     */
    public final void include (double x,
                               double y)
    {
        include(1, x, y);
    }

    //---------//
    // include //
    //---------//
    /**
     * Include one point (with default weight assigned to 1).
     *
     * @param point point to include
     */
    public final void include (Point2D point)
    {
        include(1, point.getX(), point.getY());
    }

    //---------//
    // include //
    //---------//
    /**
     * Include one point.
     *
     * @param weight weight assigned to the point
     * @param point  point to include
     */
    public final void include (double weight,
                               Point2D point)
    {
        include(weight, point.getX(), point.getY());
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName())
                .append(" weight:")
                .append(weight);

        if (weight > 0) {
            sb.append(" x:")
                    .append((float) getX())
                    .append(" y:")
                    .append((float) getY());
        }

        return sb.toString();
    }
}
