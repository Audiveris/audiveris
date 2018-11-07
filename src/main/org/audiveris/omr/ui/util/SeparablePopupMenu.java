//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              S e p a r a b l e P o p u p M e n u                               //
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

import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

/**
 * Class {@code SeparablePopupMenu} is a popup menu which is able to collapse unneeded
 * separators.
 * This is derived from {link SeparableMenu}.
 *
 * @author Hervé Bitteur
 */
public class SeparablePopupMenu
        extends JPopupMenu
{

    /**
     * Creates a new SeparablePopupMenu object.
     */
    public SeparablePopupMenu ()
    {
        super();
    }

    /**
     * Creates a new SeparablePopupMenu object.
     *
     * @param s DOCUMENT ME!
     */
    public SeparablePopupMenu (String s)
    {
        super(s);
    }

    //--------------//
    // addSeparator //
    //--------------//
    /**
     * The separator will be inserted only if it is really necessary
     */
    @Override
    public void addSeparator ()
    {
        int count = getComponentCount();

        if ((count > 0) && !(getComponent(count - 1) instanceof JSeparator)) {
            super.addSeparator();
        }
    }

    //---------------//
    // trimSeparator //
    //---------------//
    /**
     * Remove any potential orphan separator at the end of the menu
     */
    public void trimSeparator ()
    {
        int count = getComponentCount();

        if ((count > 0) && getComponent(count - 1) instanceof JSeparator) {
            remove(count - 1);
        }
    }
}
