//----------------------------------------------------------------------------//
//                                                                            //
//                         S e p a r a b l e M e n u                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Brenton Partridge 2007. All rights reserved.                //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JSeparator;

/**
 * Class {@code SeparableMenu} is a menu which is able to collapse
 * unneeded separators.
 *
 * @author Brenton Partridge
 */
public class SeparableMenu
        extends JMenu
{
    //~ Constructors -----------------------------------------------------------

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

    //~ Methods ----------------------------------------------------------------
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

        if ((count > 0)
            && !(getMenuComponent(count - 1) instanceof JSeparator)) {
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

        if ((count > 0)
            && menu.getMenuComponent(count - 1) instanceof JSeparator) {
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
