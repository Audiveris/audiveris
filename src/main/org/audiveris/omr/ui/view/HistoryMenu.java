//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      H i s t o r y M e n u                                     //
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
package org.audiveris.omr.ui.view;

import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.util.PathHistory;
import org.audiveris.omr.util.PathTask;

import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Paths;

import javax.swing.JMenu;

/**
 * Class {@code HistoryMenu} defines a menu filled with a history of paths.
 *
 * @author Hervé Bitteur
 */
public class HistoryMenu
{

    private static final Logger logger = LoggerFactory.getLogger(HistoryMenu.class);

    /** Underlying path history. */
    protected final PathHistory history;

    /** Task class launched on selected path. */
    protected final Class<? extends PathTask> pathTaskClass;

    /** The concrete menu. */
    protected JMenu menu;

    /**
     * Creates a new {@code HistoryMenu} object.
     *
     * @param history       the underlying path history
     * @param pathTaskClass the task launched to process the selected path
     */
    public HistoryMenu (PathHistory history,
                        Class<? extends PathTask> pathTaskClass)
    {
        this.history = history;
        this.pathTaskClass = pathTaskClass;
        history.setMenu(this);
    }

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
        history.feedMenu(menu, new ActionListener()
                 {
                     @Override
                     public void actionPerformed (ActionEvent e)
                     {
                         try {
                             final String name = e.getActionCommand().trim();

                             if (!name.isEmpty()) {
                                 PathTask pathTask = pathTaskClass.newInstance();
                                 pathTask.setPath(Paths.get(name));
                                 pathTask.execute();
                             }
                         } catch (IllegalAccessException |
                                  InstantiationException ex) {
                             logger.warn("Error in HistoryMenu " + ex, ex);
                         }
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
