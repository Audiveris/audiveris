//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               C l o s a b l e T a b b e d P a n e                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import java.awt.Component;

import javax.swing.Icon;
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
    //~ Methods ------------------------------------------------------------------------------------

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
