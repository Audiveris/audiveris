//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     D y n a m i c M e n u                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DynamicMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
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

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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
