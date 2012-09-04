//----------------------------------------------------------------------------//
//                                                                            //
//                           P i x e l S o u r c e                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

/**
 * Interface {@code PixelSource} reports the foreground pixels of a
 * {@link RawPixelSource}.
 *
 * @author Hervé Bitteur
 */
public interface PixelSource
        extends RawPixelSource
{
    //~ Static fields/initializers ---------------------------------------------

    /** Default value for background pixel. */
    public static final int BACKGROUND = 255;

    //~ Methods ----------------------------------------------------------------

    /**
     * Report whether the pixel at location (x,y) is a foreground pixel
     * or not.
     *
     * @param x abscissa value
     * @param y ordinate value
     * @return true for a foreground pixel, false for a background pixel
     */
    boolean isFore (int x,
                    int y);
}
