//-----------------------------------------------------------------------//
//                                                                       //
//                        M o u s e M o n i t o r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui.view;

import omr.selection.MouseMovement;

import omr.ui.*;

import java.awt.*;
import java.awt.event.*;

/**
 * Interface <code>MouseMonitor</code> defines the entries of an entity to
 * be notified of mouse actions. This is ordinarily used in conjunction
 * with a {@link omr.ui.view.Rubber}.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface MouseMonitor
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Contextual action (by right button click)
     * @param pt the current point in model pixel coordinates
     * @param movement the mouse movement
     */
    void contextSelected (Point         pt,
                          MouseMovement movement);

    /**
     * Selection (by left button click + control) of an additional point
     * @param pt the added point in model pixel coordinates
     * @param movement the mouse movement
     */
    void pointAdded (Point         pt,
                     MouseMovement movement);

    /**
     * Selection (by left button click)
     * @param pt the selected point in model pixel coordinates
     * @param movement the mouse movement
     */
    void pointSelected (Point         pt,
                        MouseMovement movement);

    /**
     * Selection (by left button drag) of a rectangle when mouse is
     * released
     * @param rect the selected rectangle in model pixel coordinates
     * @param movement the mouse movement
     */
    void rectangleSelected (Rectangle     rect,
                            MouseMovement movement);

    /**
     * Rectangle zoom (by shift + ctrl) at end of rectangle selection
     * @param rect the rectangle in model pixel coordinates, which defines
     *             the focus and the zoom ratio
     * @param movement the mouse movement
     */
    void rectangleZoomed (Rectangle     rect,
                          MouseMovement movement);
}
