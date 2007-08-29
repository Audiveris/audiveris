//----------------------------------------------------------------------------//
//                                                                            //
//                         S e p a r a b l e M e n u                          //
//                                                                            //
//  Copyright (C) Brenton Partridge 2007. All rights reserved.                //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
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

    public SeparableMenu ()
    {
        super();
    }

    public SeparableMenu (Action action)
    {
        super(action);
    }

    public SeparableMenu (String  s,
                          boolean flag)
    {
        super(s, flag);
    }

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
            !(super.getMenuComponent(count - 1) instanceof JSeparator)) {
            super.addSeparator();
        }
    }
}
