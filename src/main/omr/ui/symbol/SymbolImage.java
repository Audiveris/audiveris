//----------------------------------------------------------------------------//
//                                                                            //
//                           S y m b o l I m a g e                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Class {@code SymbolImage} is a BufferedImage with the ability to define
 * a reference point, defined as a translation from the area center.
 *
 * @author Hervé Bitteur
 */
public class SymbolImage
    extends BufferedImage
{
    //~ Instance fields --------------------------------------------------------

    /** The reference point for this image */
    private final Point refPoint;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SymbolImage //
    //-------------//
    /**
     * Creates a new SymbolImage object.
     *
     * @param width image width in pixels
     * @param height image height in pixels
     * @param refPoint the reference point, if any, with coordinated defined
     * from image center
     */
    public SymbolImage (int   width,
                        int   height,
                        Point refPoint)
    {
        super(width, height, BufferedImage.TYPE_INT_ARGB);
        this.refPoint = refPoint;
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getRefPoint //
    //-------------//
    /**
     * Report a copy of image reference point if any
     * @return the refPoint if any, otherwise null
     */
    public Point getRefPoint ()
    {
        if (refPoint != null) {
            return new Point(refPoint);
        } else {
            return null;
        }
    }
}
