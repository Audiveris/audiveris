//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C u b i c U t i l                                       //
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

import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;

/**
 * Class {@code CubicUtil} gathers utility functions related to cubic bezier curves
 * ({@link CubicCurve2D})
 *
 * @author Hervé Bitteur
 */
public abstract class CubicUtil
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Not meant to be instantiated.
     */
    private CubicUtil ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
}
