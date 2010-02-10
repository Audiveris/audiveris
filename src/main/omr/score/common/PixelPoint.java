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
 * point in a deskewed page, with its coordinates specified in pixels, so the
 * name.
 *
 * <p> This specialization is used to take benefit of compiler checks, to
 * prevent the use of points with incorrect meaning or units. </p>
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

    //-----------//
    // translate //
    //-----------//
    /**
     * Shift the current location by geometrically adding the provided vetor
     * @param vector the shift vector
     */
    public void translate (PixelPoint vector)
    {
        translate(vector.x, vector.y);
    }
}
