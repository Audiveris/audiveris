//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     D y n a m i c M e n u                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * Class {@code DynamicMenu} simplifies the definition of a menu, whose content needs
 * to be updated on-the-fly when the menu is being selected.
 *
 * @author Hervé Bitteur
 */
public abstract class DynamicMenu
{

    private static final Logger logger = LoggerFactory.getLogger(DynamicMenu.class);

    /** The concrete UI menu. */
    private JMenu menu;

    /** Specific menu listener. */
    private MenuListener menuListener = new AbstractMenuListener()
    {
        @Override
        public void menuSelected (MenuEvent e)
        {
            // Clean up the whole menu
            menu.removeAll();

            // Rebuild the whole list of menu items on the fly
            buildItems();
        }
    };

    /**
     * Create the dynamic menu.
     *
     * @param menuLabel the label to be used for the menu
     * @param menuClass the precise class for menu
     */
    public DynamicMenu (String menuLabel,
                        Class<? extends JMenu> menuClass)
    {
        try {
            menu = menuClass.newInstance();
            menu.setText(menuLabel);

            // Listener to menu selection, to modify content on-the-fly
            menu.addMenuListener(menuListener);
        } catch (Exception ex) {
            logger.error("Could not instantiate " + menuClass, ex);
            menu = null;
        }
    }

    /**
     * Creates a new DynamicMenu object.
     *
     * @param action    related action
     * @param menuClass the precise class for menu
     */
    public DynamicMenu (Action action,
                        Class<? extends JMenu> menuClass)
    {
        try {
            menu = menuClass.newInstance();
            menu.setAction(action);

            // Listener to menu selection, to modify content on-the-fly
            menu.addMenuListener(menuListener);
        } catch (Exception ex) {
            logger.error("Could not instantiate " + menuClass, ex);
            menu = null;
        }
    }

    //---------//
    // getMenu //
    //---------//
    /**
     * Report the concrete menu.
     *
     * @return the usable menu
     */
    public JMenu getMenu ()
    {
        return menu;
    }

    //------------//
    // buildItems //
    //------------//
    /**
     * This is the method that is called whenever the menu is selected.
     * To be implemented in a subclass.
     */
    protected abstract void buildItems ();
}
