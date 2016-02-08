//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      H i s t o r y M e n u                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.view;

import omr.OMR;

import omr.util.PathHistory;
import omr.util.PathTask;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HistoryMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    // Underlying path history
    protected final PathHistory history;

    // Task class launched on selected path
    protected final Class<? extends PathTask> pathTaskClass;

    // The concrete menu
    protected JMenu menu;

    //~ Constructors -------------------------------------------------------------------------------
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
        history.feedMenu(menu,
                         new ActionListener()
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
                         } catch (Exception ex) {
                             logger.warn("Error in HistoryMenu " + ex, ex);
                         }
                     }
                 });

        // Resource injection
        ResourceMap resource = OMR.gui.getApplication().getContext().getResourceMap(resourceClass);
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
