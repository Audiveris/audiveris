//-----------------------------------------------------------------------//
//                                                                       //
//                          P i x e l F o c u s                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Interface <code>PixelFocus</code> define the features related to setting
 * a focus determined by pixels coordinates, it is thus an input entity.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface PixelFocus
{
    /**
     * Focus on a point
     *
     * @param pt the designated point, using pixel coordinates
     */
    void setFocusPoint (Point pt);

    /**
     * Focus on a rectangle
     *
     * @param rect the designated rectangle, using pixel coordinates
     */
    void setFocusRectangle (Rectangle rect);
}
