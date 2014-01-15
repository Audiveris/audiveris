//----------------------------------------------------------------------------//
//                                                                            //
//                              P i x e l S i n k                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

/**
 * Interface {@code PixelSink} defines the operations expected from
 * a rectangular pixel sink.
 *
 * @author Hervé Bitteur
 */
public interface PixelSink
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the height of the rectangular sink
     *
     * @return the sink height
     */
    int getHeight ();

    /**
     * Report the width of the rectangular sink.
     *
     * @return the sink width
     */
    int getWidth ();

    /**
     * Assign the provided value to the pixel at location (x, y).
     *
     * @param x   pixel abscissa
     * @param y   pixel ordinate
     * @param val new pixel value, assumed to be in range 0..255
     */
    void setValue (int x,
                   int y,
                   int val);
}
