//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A b s t r a c t M e n u L i s t e n e r                            //
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

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * Class {@code AbstractMenuListener} is a void implementation of {@link MenuListener}
 * interface.
 *
 * @author Hervé Bitteur
 */
public class AbstractMenuListener
        implements MenuListener
{
    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public void menuCanceled (MenuEvent e)
    {
    }

    @Override
    public void menuDeselected (MenuEvent e)
    {
    }

    @Override
    public void menuSelected (MenuEvent e)
    {
    }
}
