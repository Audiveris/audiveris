//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S e p a r a b l e M e n u                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JSeparator;

/**
 * Class {@code SeparableMenu} is a menu which is able to collapse unneeded separators.
 *
 * @author Brenton Partridge
 */
public class SeparableMenu
        extends JMenu
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SeparableMenu object.
     */
    public SeparableMenu ()
    {
        super();
    }

    /**
     * Creates a new SeparableMenu object.
     *
     * @param action properties are grabbed from this action
     */
    public SeparableMenu (Action action)
    {
        super(action);
    }

    /**
     * Creates a new SeparableMenu object.
     *
     * @param s Text for the menu label
     */
    public SeparableMenu (String s)
    {
        super(s);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // addSeparator //
    //--------------//
    /**
     * The separator will be inserted only if it is really necessary.
     */
    @Override
    public void addSeparator ()
    {
        int count = getMenuComponentCount();

        if ((count > 0) && !(getMenuComponent(count - 1) instanceof JSeparator)) {
            super.addSeparator();
        }
    }

    //---------------//
    // trimSeparator //
    //---------------//
    /**
     * Remove any potential orphan separator at the end of the menu.
     *
     * @param menu the menu to purge
     */
    public static void trimSeparator (JMenu menu)
    {
        int count = menu.getMenuComponentCount();

        if ((count > 0) && menu.getMenuComponent(count - 1) instanceof JSeparator) {
            menu.remove(count - 1);
        }
    }

    //---------------//
    // trimSeparator //
    //---------------//
    /**
     * Remove any potential orphan separator at the end of the menu.
     */
    public void trimSeparator ()
    {
        int count = getMenuComponentCount();

        if ((count > 0) && getMenuComponent(count - 1) instanceof JSeparator) {
            remove(count - 1);
        }
    }
}
