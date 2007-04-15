//-----------------------------------------------------------------------//
//                                                                       //
//                          P i x e l F o c u s                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui.view;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Interface <code>PixelFocus</code> define the features related to setting
 * a focus determined by pixels coordinates. Pixel information is used to
 * focus the user display on the given point or rectangle, and to notify
 * this information to registered observers.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
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
