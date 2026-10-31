//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                L i n u x P o p u p M e n u U I                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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

import com.formdev.flatlaf.ui.FlatPopupMenuUI;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.plaf.ComponentUI;

/**
 * Class <code>LinuxPopupMenuUI</code> is a FlatLaf popup menu UI which fits popup menus
 * within the whole screen bounds, ignoring screen insets.
 * <p>
 * On Linux, screen insets are derived from the window manager "_NET_WORKAREA" property.
 * With KDE on Wayland, when XWayland applications apply scaling themselves, this work area is
 * published in logical coordinates while XWayland screen is in physical pixels.
 * For example with a 1.5 scale, a 2558x1238 screen gets a 1705x783 work area, and FlatLaf would
 * shift any popup menu crossing y=783 upwards, over the menu bar.
 * <p>
 * This is the same policy as FlatLaf for Linux multi-screen configurations and as Swing
 * <code>JPopupMenu</code> when the popup is allowed to overlap the task bar.
 * Popup menus higher than the whole screen are still handled by FlatLaf (scrolling).
 */
public class LinuxPopupMenuUI
        extends FlatPopupMenuUI
{
    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public Popup getPopup (JPopupMenu popup,
                           int x,
                           int y)
    {
        final Rectangle screen = getScreenBoundsAt(x, y);
        final Dimension size = popup.getPreferredSize();

        if ((screen == null) || (size.height > screen.height) || (size.width > screen.width)) {
            return super.getPopup(popup, x, y);
        }

        // Shift popup if needed to fit within screen
        x = Math.max(screen.x, Math.min(x, screen.x + screen.width - size.width));
        y = Math.max(screen.y, Math.min(y, screen.y + screen.height - size.height));

        return PopupFactory.getSharedInstance().getPopup(popup.getInvoker(), popup, x, y);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    /**
     * Create the UI for provided component.
     *
     * @param c the popup menu component
     * @return the created UI
     */
    public static ComponentUI createUI (JComponent c)
    {
        return new LinuxPopupMenuUI();
    }

    //-------------------//
    // getScreenBoundsAt //
    //-------------------//
    /**
     * Report the bounds of the screen that contains the provided location.
     *
     * @param x abscissa on screen
     * @param y ordinate on screen
     * @return the screen bounds, or null if not found
     */
    private static Rectangle getScreenBoundsAt (int x,
                                                int y)
    {
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }

        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getScreenDevices()) {
            final GraphicsConfiguration gc = device.getDefaultConfiguration();
            final Rectangle bounds = gc.getBounds();

            if (bounds.contains(x, y)) {
                return bounds;
            }
        }

        return null;
    }
}
