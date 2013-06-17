//----------------------------------------------------------------------------//
//                                                                            //
//                        R a w P i x e l S o u r c e                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

/**
 * Interface {@code PixelSource} defines the operations expected
 * from a rectangular pixel source, limited by its width and height.
 * <p>It is a <b>raw</b> pixel source, because just the pixel gray value
 * is returned, with no interpretation as foreground or background.
 * This additional interpretation is reported by a {@link PixelSource}.
 *
 * @author Hervé Bitteur
 */
public interface PixelSource
{
    //~ Static fields/initializers ---------------------------------------------

    /** Default value for background pixel. */
    public static final int BACKGROUND = 255;

    //~ Methods ----------------------------------------------------------------
    /**
     * Report the height of the rectangular source
     *
     * @return the source height
     */
    int getHeight ();

    /**
     * Report the pixel element, as read at location (x, y) in the
     * source.
     *
     * @param x abscissa value
     * @param y ordinate value
     *
     * @return the pixel value using range 0..255 (0/black for foreground,
     *         255/white for background)
     */
    int getPixel (int x,
                  int y);

    /**
     * Report the width of the rectangular source.
     *
     * @return the source width
     */
    int getWidth ();
}
