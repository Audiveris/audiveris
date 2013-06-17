//----------------------------------------------------------------------------//
//                                                                            //
//                               L i n e U t i l                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class {@code LineUtil} is a collection of utilities related to
 * lines.
 *
 * @author Hervé Bitteur
 */
public class LineUtil
{
    //~ Constructors -----------------------------------------------------------

    private LineUtil ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // bisector //
    //----------//
    /**
     * Return the bisector (french: médiatrice) of the provided segment
     *
     * @param segment the provided segment
     * @return (a segment on) the bisector
     */
    public static Line2D bisector (Line2D segment)
    {
        double x1 = segment.getX1();
        double y1 = segment.getY1();

        double hdx = (segment.getX2() - x1) / 2;
        double hdy = (segment.getY2() - y1) / 2;

        // Use middle as reference point
        double mx = x1 + hdx;
        double my = y1 + hdy;

        double x3 = mx + hdy;
        double y3 = my - hdx;
        double x4 = mx - hdy;
        double y4 = my + hdx;

        return new Line2D.Double(x3, y3, x4, y4);
    }

    //--------------//
    // intersection //
    //--------------//
    /**
     * Return the intersection point between infinite line A defined by
     * points p1 & p2 and infinite line B defined by points p3 & p4.
     *
     * @param p1 first point of line A
     * @param p2 second point of line A
     * @param p3 first point of line B
     * @param p4 second point of line B
     * @return the intersection point
     */
    public static Point2D.Double intersection (Point2D p1,
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
