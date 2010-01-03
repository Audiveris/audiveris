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

import javax.swing.*;

/**
 * Class <code>SeparableMenu</code> is a menu which is able to collapse unneeded
 * separators
 *
 * @author Brenton Partridge
 * @version $Id$
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
     * @param action DOCUMENT ME!
     */
    public SeparableMenu (Action action)
    {
        super(action);
    }

    /**
     * Creates a new SeparableMenu object.
     *
     * @param s DOCUMENT ME!
     * @param flag DOCUMENT ME!
     */
    public SeparableMenu (String  s,
                          boolean flag)
    {
        super(s, flag);
    }

    /**
     * Creates a new SeparableMenu object.
     *
     * @param s DOCUMENT ME!
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
     * The separator will be inserted only if it is really necessary
     */
    @Override
    public void addSeparator ()
    {
        int count = super.getMenuComponentCount();

        if ((count > 0) &&
            !(getMenuComponent(count - 1) instanceof JSeparator)) {
            super.addSeparator();
        }
    }

    //----------------//
    // purgeSeparator //
    //----------------//
    /**
     * Remove any potential orphan separator at the end of the menu
     */
    public static void purgeSeparator (JMenu menu)
    {
        int count = menu.getMenuComponentCount();

        if (menu.getMenuComponent(count - 1) instanceof JSeparator) {
            menu.remove(count - 1);
        }
    }
}
