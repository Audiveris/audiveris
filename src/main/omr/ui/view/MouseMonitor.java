//----------------------------------------------------------------------------//
//                                                                            //
//                          M o u s e M o n i t o r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.view;

import omr.selection.MouseMovement;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Interface {@code MouseMonitor} defines the entries of an entity to
 * be notified of mouse actions. This is ordinarily used in conjunction
 * with a {@link omr.ui.view.Rubber}.
 *
 * @author Hervé Bitteur
 */
public interface MouseMonitor
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Contextual action (by right button click + control) of an
     * additional point
     *
     * @param pt       the current point in model pixel coordinates
     * @param movement the mouse movement
     */
    void contextAdded (Point pt,
                       MouseMovement movement);

    /**
     * Contextual action (by right button click)
     *
     * @param pt       the current point in model pixel coordinates
     * @param movement the mouse movement
     */
    void contextSelected (Point pt,
                          MouseMovement movement);

    /**
     * Selection (by left button click + control) of an additional point
     *
     * @param pt       the added point in model pixel coordinates
     * @param movement the mouse movement
     */
    void pointAdded (Point pt,
                     MouseMovement movement);

    /**
     * Selection (by left button click)
     *
     * @param pt       the selected point in model pixel coordinates
     * @param movement the mouse movement
     */
    void pointSelected (Point pt,
                        MouseMovement movement);

    /**
     * Selection (by left button drag) of a rectangle when mouse is
     * released
     *
     * @param rect     the selected rectangle in model pixel coordinates
     * @param movement the mouse movement
     */
    void rectangleSelected (Rectangle rect,
                            MouseMovement movement);

    /**
     * Rectangle zoom (by shift + ctrl) at end of rectangle selection
     *
     * @param rect     the rectangle in model pixel coordinates, which defines
     *                 the focus and the zoom ratio
     * @param movement the mouse movement
     */
    void rectangleZoomed (Rectangle rect,
                          MouseMovement movement);
}
