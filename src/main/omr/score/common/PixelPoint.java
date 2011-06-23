//----------------------------------------------------------------------------//
//                                                                            //
//                            P i x e l P o i n t                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;


/**
 * Class <code>PixelPoint</code> is a simple Point that is meant to represent a
 * point in a sheet, with its absolute coordinates specified in pixels and the
 * origin being the top left corner of the sheet.
 *
 * <p> This specialization is used to take benefit of compiler checks, to
 * prevent the use of points with incorrect meaning or orientation. </p>
 *
 * @author Hervé Bitteur
 */
public class PixelPoint
    extends SimplePoint
{
    //~ Constructors -----------------------------------------------------------

    //------------//
    // PixelPoint //
    //------------//
    /**
     * Creates a new PixelPoint object.
     */
    public PixelPoint ()
    {
    }

    //------------//
    // PixelPoint //
    //------------//
    /**
     * Creates a new PixelPoint object, by cloning a PixelPoint
     *
     * @param point the PixelPoint to clone
     */
    public PixelPoint (PixelPoint point)
    {
        super(point.x, point.y);
    }

    //------------//
    // PixelPoint //
    //------------//
    /**
     * Creates a new PixelPoint object, by cloning an untyped point
     *
     * @param x abscissa
     * @param y ordinate
     */
    public PixelPoint (int x,
                       int y)
    {
        super(x, y);
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // lineIntersection //
    //------------------//
    /**
     * Return the intersection point between infinite line A defined by points
     * p1 & p2 and infinite line B defined by points p3 & p4.
     * @param p1 first point of line A
     * @param p2 second point of line A
     * @param p3 first point of line B
     * @param p4 second point of line B
     * @return the intersection point
     */
    public static PixelPoint lineIntersection (PixelPoint p1,
                                               PixelPoint p2,
                                               PixelPoint p3,
                                               PixelPoint p4)
    {
        double x1 = p1.x;
        double y1 = p1.y;
        double x2 = p2.x;
        double y2 = p2.y;
        double x3 = p3.x;
        double y3 = p3.y;
        double x4 = p4.x;
        double y4 = p4.y;

        double den = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));

        double v12 = (x1 * y2) - (y1 * x2);
        double v34 = (x3 * y4) - (y3 * x4);

        double x = ((v12 * (x3 - x4)) - ((x1 - x2) * v34)) / den;
        double y = ((v12 * (y3 - y4)) - ((y1 - y2) * v34)) / den;

        return new PixelPoint((int) Math.rint(x), (int) Math.rint(y));
    }

    //----//
    // to //
    //----//
    /**
     * Report the translation vector from this point to the other
     * @param other the target point
     * @return the vector from this point to the other
     */
    public PixelPoint to (PixelPoint other)
    {
        return new PixelPoint(other.x - x, other.y - y);
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Translate the current location by geometrically adding the provided
     * vector
     * @param vector the translation vector
     */
    public void translate (PixelPoint vector)
    {
        translate(vector.x, vector.y);
    }
}
