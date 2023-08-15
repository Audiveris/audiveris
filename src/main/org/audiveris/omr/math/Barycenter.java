//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B a r y c e n t e r                                       //
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

import java.awt.geom.Point2D;

/**
 * Class <code>Barycenter</code> is meant to cumulate data when computing barycenter.
 *
 * @author Hervé Bitteur
 */
public class Barycenter
{
    //~ Instance fields ----------------------------------------------------------------------------

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

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new Barycenter object.
     */
    public Barycenter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getWeight //
    //-----------//
    /**
     * Report the total weight.
     *
     * @return total weight
     */
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{").append(" weight:").append(weight);

        if (weight > 0) {
            sb.append(" x:").append((float) getX()).append(" y:").append((float) getY());
        }

        return sb.toString();
    }
}
