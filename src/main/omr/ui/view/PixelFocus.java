//----------------------------------------------------------------------------//
//                                                                            //
//                            P i x e l F o c u s                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.view;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Interface {@code PixelFocus} define the features related to setting
 * a focus determined by pixels coordinates. Pixel information is used to
 * focus the user display on the given point or rectangle, and to notify
 * this information to registered observers.
 *
 * @author Hervé Bitteur
 */
public interface PixelFocus
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Focus on a rectangle
     *
     * @param rect the designated rectangle, using pixel coordinates
     */
    void setFocusLocation (Rectangle rect);

    /**
     * Focus on a point
     *
     * @param pt the designated point, using pixel coordinates
     */
    void setFocusPoint (Point pt);
}
