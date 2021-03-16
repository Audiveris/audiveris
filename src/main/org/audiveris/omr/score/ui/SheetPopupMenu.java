//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S h e e t P o p u p M e n u                                   //
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
package org.audiveris.omr.score.ui;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.view.LocationDependent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

/**
 * Class {@code SheetPopupMenu} is a general sheet pop-up menu meant to host sub-menus.
 *
 * @author Hervé Bitteur
 */
public class SheetPopupMenu
{

    private static final Logger logger = LoggerFactory.getLogger(SheetPopupMenu.class);

    /** The related sheet. */
    protected final Sheet sheet;

    /** Concrete pop-up menu. */
    protected final JPopupMenu popup = new JPopupMenu();

    /**
     * Creates a new {@code SheetPopupMenu} object.
     *
     * @param sheet the related sheet
     */
    public SheetPopupMenu (Sheet sheet)
    {
        this.sheet = sheet;

        popup.setName("SheetPopupMenu");
    }

    //---------//
    // addMenu //
    //---------//
    /**
     * Add a menu to the sheet popup
     *
     * @param menu the menu to add
     */
    public void addMenu (JMenu menu)
    {
        popup.add(menu);
    }

    //----------//
    // getPopup //
    //----------//
    /**
     * Report the concrete pop-up menu.
     *
     * @return the pop-up menu
     */
    public JPopupMenu getPopup ()
    {
        return popup;
    }

    //------------//
    // updateMenu //
    //------------//
    /**
     * Update the pop-up menu according to the currently selected items.
     *
     * @param rect the selected rectangle, perhaps degenerated to a point
     * @return true if pop-up is not empty, and thus is worth being shown
     */
    public boolean updateMenu (Rectangle rect)
    {
        // Update interested components
        for (Component component : popup.getComponents()) {
            if (component instanceof LocationDependent) {
                ((LocationDependent) component).updateUserLocation(rect);
            }
        }

        // Check if popup is worth being displayed
        for (Component component : popup.getComponents()) {
            if (component.isVisible()) {
                return true;
            }
        }

        return false;
    }
}
