//----------------------------------------------------------------------------//
//                                                                            //
//                         L i n e U t i l i t i e s                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.geom.Point2D;

/**
 * Class {@code LineUtilities} is a collection of utilities related to lines
 *
 * @author Hervé Bitteur
 */
public class LineUtilities
{
    //~ Methods ----------------------------------------------------------------

    //--------------//
    // intersection //
    //--------------//
    /**
     * Return the intersection point between infinite line A defined by points
     * p1 & p2 and infinite line B defined by points p3 & p4.
     * @param p1 first point of line A
     * @param p2 second point of line A
     * @param p3 first point of line B
     * @param p4 second point of line B
     * @return the intersection point
     */
    public static Point2D intersection (Point2D p1,
                                        Point2D p2,
                                        Point2D p3,
                                        Point2D p4)
    {
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();
        double x3 = p3.getX();
        double y3 = p3.getY();
        double x4 = p4.getX();
        double y4 = p4.getY();

        double den = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));

        double v12 = (x1 * y2) - (y1 * x2);
        double v34 = (x3 * y4) - (y3 * x4);

        double x = ((v12 * (x3 - x4)) - ((x1 - x2) * v34)) / den;
        double y = ((v12 * (y3 - y4)) - ((y1 - y2) * v34)) / den;

        return new Point2D.Double(x, y);
    }
}
