//-----------------------------------------------------------------------//
//                                                                       //
//                        M o u s e M o n i t o r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 * Interface <code>MouseMonitor</code> defines the entries of an entity to be
 * notified of mouse actions.
 */
public interface MouseMonitor
{
    /**
     * Selection (by right button clic)
     * @param e the mouse event
     * @param pt the selected point in model pixel coordinates
     */
    void pointSelected (MouseEvent e,
                        Point pt);

    /**
     * Point designation (by left button clic)
     * @param e the mouse event
     * @param pt the current point in model pixel coordinates
     */
    void pointUpdated (MouseEvent e,
                       Point pt);

    /**
     * Selection (by shift + right button drag) of a rectangle when mouse is
     * released
     * @param e the mouse event
     * @param rect the selected rectangle in model pixel coordinates
     */
    void rectangleSelected (MouseEvent e,
                            Rectangle  rect);

    /**
     * Rectangle designation (by shift + left button drag)
     * @param e the mouse event
     * @param rect the current rectangle in model pixel coordinates
     */
    void rectangleUpdated (MouseEvent e,
                           Rectangle rect);

    /**
     * Rectangle zoom (by shift + ctrl + left button drag)
     * @param e    the mouse event
     * @param rect the rectangle in model pixel coordinates, which defines the
     *             focus and the zoom ratio
     */
    void rectangleZoomed (MouseEvent e,
                          Rectangle rect);
}
