//----------------------------------------------------------------------------//
//                                                                            //
//                           P i x e l S o u r c e                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.lag;


/**
 * Interface <code>PixelSource</code> defines the operations expected from a
 * rectangular pixel source, limited by its width and height, in order to
 * populate a lag for example.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface PixelSource
{
    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the height of the rectangular source
     *
     * @return the source height
     */
    int getHeight ();

    //------------------//
    // getMaxForeground //
    //------------------//
    /**
     * Since foreground is made of rather black pixels (level close to 0), this
     * method reports the maximum value for a pixel to be considered as
     * foreground.
     *
     * @return the maximum foreground pixel value
     */
    int getMaxForeground ();

    //----------//
    // getPixel //
    //----------//
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

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of the rectangular source
     *
     * @return the source width
     */
    int getWidth ();
}
