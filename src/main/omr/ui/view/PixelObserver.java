//-----------------------------------------------------------------------//
//                                                                       //
//                     S e c t i o n O b s e r v e r                     //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui.view;

import omr.ui.*;
import omr.util.Observer;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Interface <code>PixelObserver</code> defines an {@link Observer} for
 * pixel information (Point or Rectangle).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface PixelObserver
    extends Observer<Point>
{
    //--------//
    // update //
    //--------//
    /**
     * Added entry for Point + pixel level entity
     *
     * @param ul upper left point
     * @param level pixel level. A -1 value means no value is provided
     */
    void update (Point ul,
                 int   level);

    //--------//
    // update //
    //--------//
    /**
     * Added entry for Rectangle entity
     *
     * @param rect the Rectangle entity to be used
     */
    void update (Rectangle rect);
}
