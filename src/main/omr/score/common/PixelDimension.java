//----------------------------------------------------------------------------//
//                                                                            //
//                        P i x e l D i m e n s i o n                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;


/**
 * Class {@code PixelDimension} is a simple Dimension that is meant to
 * represent an absolute  dimension in a sheet, with its components (width and
 * height) specified in pixels, so the name.
 *
 * <p> This specialization is used to take benefit of compiler checks, to
 * prevent the use of dimensions with incorrect meaning. </p>
 *
 * @author Hervé Bitteur
 */
public class PixelDimension
    extends SimpleDimension
{
    //~ Constructors -----------------------------------------------------------

    //----------------//
    // PixelDimension //
    //----------------//
    /**
     * Creates an instance of {@code PixelDimension} with a width of zero
     * and a height of zero.
     */
    public PixelDimension ()
    {
    }

    //----------------//
    // PixelDimension //
    //----------------//
    /**
     * Constructs a {@code PixelDimension} and initializes it to the
     * specified width and specified height.
     *
     * @param width the specified width
     * @param height the specified height
     */
    public PixelDimension (int width,
                           int height)
    {
        super(width, height);
    }
}
