//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P a r a b o l a                                        //
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
import java.util.List;

/**
 * Class <code>Parabola</code> handles a parabola curve whose parameters (a,b,c) are such
 * that: y = a * x ^ 2 + b * x + c.
 * <p>
 * See http://mathforum.org/library/drmath/view/72047.html
 *
 * @author Hervé Bitteur
 */
public class Parabola
{
    //~ Instance fields ----------------------------------------------------------------------------

    private double a;

    private double b;

    private double c;

    private final double dist;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new Parabola object.
     *
     * @param points the defining points
     */
    public Parabola (List<? extends Point2D> points)
    {
        final int n = points.size();
        final double[] xx = new double[n];
        final double[] yy = new double[n];

        int i = -1;

        for (Point2D p : points) {
            i++;
            xx[i] = p.getX();
            yy[i] = p.getY();
        }

        fit(xx, yy);
        dist = computeDistance(xx, yy);
    }

    //~ Methods ------------------------------------------------------------------------------------

    private double computeDistance (double[] xx,
                                    double[] yy)
    {
        // Compute mean distance
        final int n = xx.length;
        double sum = 0;

        for (int i = 0; i < n; i++) {
            double x = xx[i];
            double val = ((a * x * x) + (b * x) + c) - yy[i];
            sum += (val * val);
        }

        sum /= n;

        return Math.sqrt(sum);
    }

    private void fit (double[] xx,
                      double[] yy)
    {
        final int n = xx.length;
        double s00 = n;
        double s10 = 0;
        double s20 = 0;
        double s30 = 0;
        double s40 = 0;
        double s01 = 0;
        double s11 = 0;
        double s21 = 0;

        for (int i = 0; i < n; i++) {
            double x = xx[i];
            double x2 = x * x;
            double x3 = x * x2;
            double x4 = x2 * x2;
            double y = yy[i];

            s10 += x;
            s20 += x2;
            s30 += x3;
            s40 += x4;
            s01 += y;
            s11 += (x * y);
            s21 += (x2 * y);
        }

        double den = ((s00 * s20 * s40) - (s10 * s10 * s40) - (s00 * s30 * s30) + (2 * s10 * s20
                * s30)) - (s20 * s20 * s20);
        a = (((s01 * s10 * s30) - (s11 * s00 * s30) - (s01 * s20 * s20) + (s11 * s10 * s20) + (s21
                * s00 * s20)) - (s21 * s10 * s10)) / den;
        b = (((s11 * s00 * s40) - (s01 * s10 * s40) + (s01 * s20 * s30)) - (s21 * s00 * s30) - (s11
                * s20 * s20) + (s21 * s10 * s20)) / den;
        c = (((s01 * s20 * s40) - (s11 * s10 * s40) - (s01 * s30 * s30) + (s11 * s20 * s30) + (s21
                * s10 * s30)) - (s21 * s20 * s20)) / den;
    }

    /**
     * Report the mean distance from fitting points to parabola.
     *
     * @return mean distance to parabola
     */
    public double getMeanDistance ()
    {
        return dist;
    }
}
