//----------------------------------------------------------------------------//
//                                                                            //
//                            P i x e l P o i n t                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;

import java.awt.geom.Point2D;

/**
 * Class {@code PixelPoint} is a simple Point that is meant to represent a
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
