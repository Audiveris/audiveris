//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            L o c a t i o n D e p e n d e n t M e n u                           //
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
package org.audiveris.omr.ui.view;

import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JMenu;

/**
 * Class {@code LocationDependentMenu}
 *
 * @author Hervé Bitteur
 */
public class LocationDependentMenu
        extends JMenu
        implements LocationDependent
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new LocationDependentMenu object.
     */
    public LocationDependentMenu ()
    {
        super();
    }

    /**
     * Creates a new LocationDependentMenu object.
     *
     * @param s menu text
     */
    public LocationDependentMenu (String s)
    {
        super(s);
    }

    /**
     * Creates a new LocationDependentMenu object.
     *
     * @param action an action
     */
    public LocationDependentMenu (Action action)
    {
        this();
        setAction(action);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void updateUserLocation (Rectangle rect)
    {
        for (Component comp : getMenuComponents()) {
            if (comp instanceof AbstractButton) {
                Action action = ((AbstractButton) comp).getAction();

                if (action instanceof LocationDependent) {
                    ((LocationDependent) action).updateUserLocation(rect);
                }
            }
        }
    }
}
