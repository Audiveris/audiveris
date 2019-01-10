//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               C l o s a b l e T a b b e d P a n e                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import java.awt.Component;
import java.awt.Container;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTabbedPane;

/**
 * Class {@code ClosableTabbedPane} is a JTabbedPane where each tab can be closed by
 * the user.
 * <p>
 * Each tab in this tabbed pane is assigned a {@link ButtonTabComponent}.
 * Right before closing a tab, the method {@link #tabAboutToClose(int)} is called-back to let user
 * make any specific processing before accepting or canceling the close action.
 *
 * @author Hervé Bitteur
 */
public class ClosableTabbedPane
        extends JTabbedPane
{

    //-----------//
    // insertTab //
    //-----------//
    /**
     * {@inheritDoc}
     * <p>
     * It overrides the standard method so that any inserted tab uses a ButtonTabComponent
     */
    @Override
    public void insertTab (String title,
                           Icon icon,
                           Component component,
                           String tip,
                           int index)
    {
        super.insertTab(title, icon, component, tip, index);

        // Use a ButtonTabComponent
        final int i = indexOfComponent(component);
        setTabComponentAt(i, new ButtonTabComponent(this));
    }

    //---------------------//
    // removeClosingButton //
    //---------------------//
    /**
     * Remove the closing button for the provided tab index.
     *
     * @param tabIndex index of tab in tabbed pane
     */
    public void removeClosingButton (int tabIndex)
    {
        Component tab = getTabComponentAt(tabIndex);

        if (tab instanceof ButtonTabComponent) {
            for (Component c : ((Container) tab).getComponents()) {
                if (c instanceof JButton) {
                    ((Container) tab).remove(c);
                    tab.invalidate();
                    tab.repaint();

                    return;
                }
            }
        }
    }

    //-----------------//
    // tabAboutToClose //
    //-----------------//
    /**
     * Signal that the tab at provided index is about to close.
     * This method can be overridden to add any specific processing at this point.
     *
     * @param tabIndex index of tab in tabbed pane
     * @return true to continue closing, false to cancel
     */
    public boolean tabAboutToClose (int tabIndex)
    {
        return true; // By default, complete the closing
    }
}
