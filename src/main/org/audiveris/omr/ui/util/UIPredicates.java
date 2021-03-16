//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    U I P r e d i c a t e s                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.util;

import org.audiveris.omr.WellKnowns;
import static java.awt.event.InputEvent.*;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

/**
 * Class {@code UIPredicates} gathers convenient methods to check user gesture.
 *
 * @author Hervé Bitteur
 */
public abstract class UIPredicates
{

    /**
     * Not meant to be instantiated.
     */
    private UIPredicates ()
    {
    }

    //------------------//
    // isAdditionWanted //
    //------------------//
    /**
     * Predicate to check if an additional selection is wanted.
     * Default is the typical selection (left button), while control key is pressed.
     *
     * @param e the mouse context
     * @return the predicate result
     */
    public static boolean isAdditionWanted (MouseEvent e)
    {
        if (WellKnowns.MAC_OS_X) {
            boolean command = e.isMetaDown();
            boolean left = SwingUtilities.isLeftMouseButton(e);

            return left && command && !e.isPopupTrigger();
        } else {
            return (SwingUtilities.isRightMouseButton(e) != SwingUtilities.isLeftMouseButton(e))
                           && e.isControlDown();
        }
    }

    //-----------------//
    // isContextWanted //
    //-----------------//
    /**
     * Predicate to check if a context selection is wanted.
     * Default is the typical pressing with Right button only.
     *
     * @param e the mouse context
     * @return the predicate result
     */
    public static boolean isContextWanted (MouseEvent e)
    {
        return SwingUtilities.isRightMouseButton(e) && !SwingUtilities.isLeftMouseButton(e);
    }

    //--------------//
    // isDragWanted //
    //--------------//
    /**
     * Predicate to check whether the zoomed display must be dragged.
     * This method can simply be overridden to adapt to another policy.
     * Default is to have both left and right buttons, or just middle button, pressed when moving.
     *
     * @param e the mouse event to check
     * @return true if drag is desired
     */
    public static boolean isDragWanted (MouseEvent e)
    {
        if (WellKnowns.MAC_OS_X) {
            return e.isAltDown();
        } else {
            // Mouse buttons 1 & 3 pressed and only those ones
            int onmask = BUTTON1_DOWN_MASK | BUTTON3_DOWN_MASK;
            int offmask = 0;

            if ((e.getModifiersEx() & (onmask | offmask)) == onmask) {
                return true;
            }

            // Mouse button 2 (wheel) pressed and only this one
            int onmask2 = BUTTON2_DOWN_MASK;
            int offmask2 = 0;

            if ((e.getModifiersEx() & (onmask2 | offmask2)) == onmask2) {
                return true;
            }

            return false;
        }
    }

    //----------------//
    // isRezoomWanted //
    //----------------//
    /**
     * Predicate to check if the display should be rezoomed to fit as
     * close as possible to the rubber definition.
     * Default is to have both Shift and Control keys pressed when the mouse is
     * released.
     *
     * @param e the mouse context
     * @return the predicate result
     */
    public static boolean isRezoomWanted (MouseEvent e)
    {
        if (WellKnowns.MAC_OS_X) {
            return e.isMetaDown() && e.isShiftDown();
        } else {
            return e.isControlDown() && e.isShiftDown();
        }
    }

    //----------------//
    // isRubberWanted //
    //----------------//
    /**
     * Predicate to check if the rubber must be extended while the
     * mouse is being moved.
     * Default is the typical pressing of Shift key while moving the mouse.
     *
     * @param e the mouse context
     * @return the predicate result
     */
    public static boolean isRubberWanted (MouseEvent e)
    {
        int onmask = BUTTON1_DOWN_MASK | SHIFT_DOWN_MASK;
        int offmask = BUTTON2_DOWN_MASK | BUTTON3_DOWN_MASK;

        return (e.getModifiersEx() & (onmask | offmask)) == onmask;
    }
}
