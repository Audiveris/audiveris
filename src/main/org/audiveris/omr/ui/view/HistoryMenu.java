//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      H i s t o r y M e n u                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.sheet.ui.BookActions.LoadBookTask;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.util.AbstractHistory;
import org.audiveris.omr.util.PathTask;
import org.audiveris.omr.util.SheetPath;

import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.nio.file.Paths;

import javax.swing.JMenu;

/**
 * Class <code>HistoryMenu</code> defines a menu filled with a history of paths.
 *
 * @author Hervé Bitteur
 */
public class HistoryMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HistoryMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Underlying path history. */
    protected final AbstractHistory history;

    /** Task class launched on selected path. */
    protected final Class<? extends PathTask> pathTaskClass;

    /** The concrete menu. */
    protected JMenu menu;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>HistoryMenu</code> object.
     *
     * @param history       the underlying path history
     * @param pathTaskClass the task launched to process the selected path
     */
    public HistoryMenu (AbstractHistory history,
                        Class<? extends PathTask> pathTaskClass)
    {
        this.history = history;
        this.pathTaskClass = pathTaskClass;
        history.setMenu(this);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // populate //
    //----------//
    /**
     * Fill the provided menu with one item per each history path.
     *
     * @param menu          the menu to populate
     * @param resourceClass the class to get menu resources from
     */
    public void populate (JMenu menu,
                          Class<?> resourceClass)
    {
        history.feedMenu(menu, (ActionEvent e) ->
        {
            try {
                final String str = e.getActionCommand().trim();

                if (!str.isEmpty()) {
                    final PathTask task = pathTaskClass.newInstance();

                    if (pathTaskClass.isAssignableFrom(LoadBookTask.class)) {
                        ((LoadBookTask) task).setPath(SheetPath.decode(str));
                    } else {
                        task.setPath(Paths.get(str));
                    }

                    task.execute();
                }
            } catch (IllegalAccessException | InstantiationException ex) {
                logger.warn("Error in HistoryMenu " + ex, ex);
            }
        });

        // Resource injection
        ResourceMap resource = OmrGui.getApplication().getContext().getResourceMap(resourceClass);
        resource.injectComponents(menu);
        this.menu = menu;

        // Initial menu status
        menu.setEnabled(!history.isEmpty());
    }

    //------------//
    // setEnabled //
    //------------//
    /**
     * Enable or disable the menu.
     *
     * @param bool true for enable
     */
    public void setEnabled (boolean bool)
    {
        menu.setEnabled(bool);
    }
}
