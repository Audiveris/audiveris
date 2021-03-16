//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C u b i c U t i l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class {@code CubicUtil} gathers utility functions related to cubic bezier curves
 * ({@link CubicCurve2D})
 *
 * @author Hervé Bitteur
 */
public abstract class CubicUtil
{

    private static final Logger logger = LoggerFactory.getLogger(CubicUtil.class);

    /**
     * Not meant to be instantiated.
     */
    private CubicUtil ()
    {
    }

    /**
     * Report the point on the curve, located at t = 1-t = 0.5.
     * It splits the curve length equally.
     * P: middle of segment P1..P2
     * C: middle of segment CP1..CP2
     * M: middle of curve
     * PM = 3/4 * PC
     *
     * @param c the provided curve
     * @return the mid point on curve
     */
    public static Point2D getMidPoint (CubicCurve2D c)
    {
        return new Point2D.Double(
                (c.getX1() + (3 * c.getCtrlX1()) + (3 * c.getCtrlX2()) + c.getX2()) / 8,
                (c.getY1() + (3 * c.getCtrlY1()) + (3 * c.getCtrlY2()) + c.getY2()) / 8);
    }

    //-------//
    // above //
    //-------//
    /**
     * Report how the curve is globally /--\ (above) or \--/ (below) or ---- (flat).
     * We use the relative position with respect to the line of the middle of control points
     *
     * @param c the provided curve
     * @return 1 for above, -1 for below, 0 for flat.
     */
    public static int above (CubicCurve2D c)
    {
        Line2D line = new Line2D.Double(c.getP1(), c.getP2());
        Point2D midC = PointUtil.middle(c.getCtrlP1(), c.getCtrlP2());

        return line.relativeCCW(midC);
    }

    //------//
    // yAtX //
    //------//
    /**
     * Report curve ordinate <b>APPROXIMATE</b> value for the provided abscissa value.
     * <p>
     * NOTA: We assume the curve represents a true function of x (in other words curve is rather
     * horizontal).
     * This restriction is acceptable for "civilized" slurs.
     *
     * @param c cubic curve
     * @param x provided abscissa value
     * @return approximate ordinate value
     */
    public static double yAtX (CubicCurve2D c,
                               double x)
    {
        final double x1 = c.getX1();
        final double x2 = c.getX2();

        // First guess
        final double t1 = (x - x1) / (x2 - x1);
        final Point2D p1 = pointAtT(c, t1);
        final double dx1 = p1.getX() - x;

        // Second guess
        final double t2 = (x - dx1 - x1) / (x2 - x1);
        final Point2D p2 = pointAtT(c, t2);

        // Interpolated t parameter
        final double t = t1 + (t2 - t1) * ((x - p1.getX()) / (p2.getX() - p1.getX()));
        final Point2D p = pointAtT(c, t);

        return p.getY();
    }

    //----------//
    // pointAtT //
    //----------//
    /**
     * Report the curve point at provided t value.
     *
     * @param c cubic curve
     * @param t t parameter value (between 0 and 1)
     * @return the corresponding point on curve
     */
    public static Point2D pointAtT (CubicCurve2D c,
                                    double t)
    {
        final double u = 1 - t;

        double x = (c.getX1() * u * u * u)
                           + (3 * c.getCtrlX1() * t * u * u)
                           + (3 * c.getCtrlX2() * t * t * u)
                           + (c.getX2() * t * t * t);

        double y = (c.getY1() * u * u * u)
                           + (3 * c.getCtrlY1() * t * u * u)
                           + (3 * c.getCtrlY2() * t * t * u)
                           + (c.getY2() * t * t * t);

        return new Point2D.Double(x, y);
    }

    //-------------//
    // createCurve //
    //-------------//
    /**
     * Create the cubic curve that goes through the provided sequence of 4 points.
     * <p>
     * NOTA: Points p0 and p3 are curve ending points, and points p1 and p2 are NOT control points
     * but intermediate points located on the curve.
     *
     * @param p0 first ending point
     * @param p1 first intermediate point
     * @param p2 second intermediate point
     * @param p3 last ending point
     * @return the created cubic curve
     */
    public static CubicCurve2D createCurve (Point2D p0,
                                            Point2D p1,
                                            Point2D p2,
                                            Point2D p3)
    {
        return new CubicCurve2D.Double(
                p0.getX(),
                p0.getY(),
                ((-(5 * p0.getX()) + (18 * p1.getX())) - (9 * p2.getX()) + (2 * p3.getX())) / 6.0,
                ((-(5 * p0.getY()) + (18 * p1.getY())) - (9 * p2.getY()) + (2 * p3.getY())) / 6.0,
                (((2 * p0.getX()) - (9 * p1.getX()) + (18 * p2.getX())) - (5 * p3.getX())) / 6.0,
                (((2 * p0.getY()) - (9 * p1.getY()) + (18 * p2.getY())) - (5 * p3.getY())) / 6.0,
                p3.getX(),
                p3.getY());
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Shift the whole curve according to the provided (dx,dy) vector.
     *
     * @param c  the curve to shift
     * @param dx shift on abscissa
     * @param dy shift on ordinate
     */
    public static void translate (CubicCurve2D c,
                                  double dx,
                                  double dy)
    {
        c.setCurve(c.getX1() + dx, c.getY1() + dy,
                   c.getCtrlX1() + dx, c.getCtrlY1() + dy,
                   c.getCtrlX2() + dx, c.getCtrlY2() + dy,
                   c.getX2() + dx, c.getY2() + dy);
    }

    /**
     * Report the unit tangent vector at point 1
     *
     * @param c the curve to process
     * @return unit vector, pointing from Control point to End point
     */
    public static Point2D getEndVector1 (CubicCurve2D c)
    {
        final double dx = c.getX1() - c.getCtrlX1();
        final double dy = c.getY1() - c.getCtrlY1();
        final double length = Math.hypot(dx, dy);

        return new Point2D.Double(dx / length, dy / length);
    }

    /**
     * Report the unit tangent vector at point 2
     *
     * @param c the curve to process
     * @return unit vector, pointing from Control point to End point
     */
    public static Point2D getEndVector2 (CubicCurve2D c)
    {
        final double dx = c.getX2() - c.getCtrlX2();
        final double dy = c.getY2() - c.getCtrlY2();
        final double length = Math.hypot(dx, dy);

        return new Point2D.Double(dx / length, dy / length);
    }
}
