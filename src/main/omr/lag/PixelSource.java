//----------------------------------------------------------------------------//
//                                                                            //
//                           P i x e l S o u r c e                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;


/**
 * Interface <code>PixelSource</code> defines the operations expected from a
 * rectangular pixel source, limited by its width and height, in order to
 * populate a lag for example.
 *
 * @author Hervé Bitteur
 */
public interface PixelSource
{
    //~ Static fields/initializers ---------------------------------------------

    /** Default value for background pixel */
    public static final int BACKGROUND = 255;

    //~ Methods ----------------------------------------------------------------

    /**
     * Report the height of the rectangular source
     *
     * @return the source height
     */
    int getHeight ();

    /**
     * Assign a new maximum value for foreground (black) pixels
     *
     * @param level the new maximum foreground pixel value
     */
    void setMaxForeground (int level);

    /**
     * Since foreground is made of rather black pixels (level close to 0), this
     * method reports the maximum value for a pixel to be considered as
     * foreground.
     *
     * @return the maximum foreground pixel value
     */
    int getMaxForeground ();

    /**
     * Report the pixel element, as read at location (x, y) in the source.
     *
     * @param x abscissa value
     * @param y ordinate value
     *
     * @return the pixel value using range 0..255 (0/black for foreground,
     * 255/white for background)
     */
    int getPixel (int x,
                  int y);

    /**
     * Report the width of the rectangular source
     *
     * @return the source width
     */
    int getWidth ();
}
