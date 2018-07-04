//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t E x t r a c t o r                                //
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
package org.audiveris.omr.moments;

import java.awt.geom.Point2D;

/**
 * Class {@code AbstractExtractor} provides the basis for moments extraction.
 *
 * @param <D> actual descriptor type
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractExtractor<D extends OrthogonalMoments<D>>
        implements MomentsExtractor<D>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Input abscissae. */
    protected int[] xx;

    /** Input ordinates. */
    protected int[] yy;

    /** Image mass (number of foreground points). */
    protected int mass;

    /** Center of mass. */
    protected Point2D center;

    /** Image max radius around its mass center. */
    protected double radius;

    /** The target descriptor. */
    protected D descriptor;

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // extract //
    //---------//
    @Override
    public void extract (int[] xx,
                         int[] yy,
                         int mass)
    {
        // Check arguments
        if ((xx == null) || (yy == null)) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + " cannot process a null array");
        }

        if ((mass <= 0) || (mass > xx.length) || (mass > yy.length)) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + " on inconsistent input");
        }

        if (descriptor == null) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + " has no target descriptor");
        }

        this.xx = xx;
        this.yy = yy;
        this.mass = mass;

        center = findCenterOfMass();
        radius = findRadius();

        extractMoments();
    }

    //---------------//
    // setDescriptor //
    //---------------//
    @Override
    public void setDescriptor (D descriptor)
    {
        this.descriptor = descriptor;
    }

    //----------------//
    // extractMoments //
    //----------------//
    /**
     * Actual extraction core, to be provided by subclasses.
     */
    protected abstract void extractMoments ();

    //------------------//
    // findCenterOfMass //
    //------------------//
    /**
     * Computer the image mass center coordinates.
     */
    private Point2D findCenterOfMass ()
    {
        int m10 = 0;
        int m01 = 0;

        for (int i = 0; i < mass; i++) {
            m10 += xx[i];
            m01 += yy[i];
        }

        return new Point2D.Double((double) m10 / (double) mass, (double) m01 / (double) mass);
    }

    //------------//
    // findRadius //
    //------------//
    /**
     * Compute the image contour, centered around its mass center.
     *
     * @return the min radius that contains the whole image
     */
    private double findRadius ()
    {
        double dxMax = Double.MIN_VALUE;
        double dyMax = Double.MIN_VALUE;

        for (int i = 0; i < mass; i++) {
            dxMax = Math.max(dxMax, Math.abs(xx[i] - center.getX()));
            dyMax = Math.max(dyMax, Math.abs(yy[i] - center.getY()));
        }

        return Math.hypot(dxMax, dyMax);
    }
}
