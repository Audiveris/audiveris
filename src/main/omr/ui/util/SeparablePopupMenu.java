//----------------------------------------------------------------------------//
//                                                                            //
//                    S e p a r a b l e P o p u p M e n u                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import javax.swing.*;

/**
 * Class <code>SeparablePopupMenu</code> is a popup menu which is able to
 * collapse unneeded separators. This is derived from {link SeparableMenu}.
 *
 * @author Hervé Bitteur
 */
public class SeparablePopupMenu
    extends JPopupMenu
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SeparablePopupMenu object.
     */
    public SeparablePopupMenu ()
    {
        super();
    }

    /**
     * Creates a new SeparablePopupMenu object.
     *
     * @param s DOCUMENT ME!
     */
    public SeparablePopupMenu (String s)
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
        int count = super.getComponentCount();

        if ((count > 0) && !(getComponent(count - 1) instanceof JSeparator)) {
            super.addSeparator();
        }
    }

    //----------------//
    // purgeSeparator //
    //----------------//
    /**
     * Remove any potential orphan separator at the end of the menu
     */
    public void purgeSeparator ()
    {
        int count = getComponentCount();

        if (getComponent(count - 1) instanceof JSeparator) {
            remove(count - 1);
        }
    }
}
