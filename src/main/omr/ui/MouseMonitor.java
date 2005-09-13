//-----------------------------------------------------------------------//
//                                                                       //
//                        M o u s e M o n i t o r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 * Interface <code>MouseMonitor</code> defines the entries of an entity to
 * be notified of mouse actions.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface MouseMonitor
{
    /**
     * Selection (by left button clic)
     * @param e the mouse event
     * @param pt the selected point in model pixel coordinates
     */
    void pointSelected (MouseEvent e,
                        Point pt);

    /**
     * Selection (by left button clic + control) of an additional point
     * @param e the mouse event
     * @param pt the added point in model pixel coordinates
     */
    void pointAdded (MouseEvent e,
                     Point pt);

    /**
     * Contextual action (by right button clic)
     * @param e the mouse event
     * @param pt the current point in model pixel coordinates
     */
    void contextSelected (MouseEvent e,
                          Point pt);

    /**
     * Selection (by left button drag) of a rectangle when mouse is
     * released
     * @param e the mouse event
     * @param rect the selected rectangle in model pixel coordinates
     */
    void rectangleSelected (MouseEvent e,
                            Rectangle  rect);

    /**
     * Rectangle zoom (by shift + ctrl) at end of rectangle selection
     * @param e    the mouse event
     * @param rect the rectangle in model pixel coordinates, which defines
     *             the focus and the zoom ratio
     */
    void rectangleZoomed (MouseEvent e,
                          Rectangle rect);
}
