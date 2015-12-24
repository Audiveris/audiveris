//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A b s t r a c t M e n u L i s t e n e r                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * Class {@code AbstractMenuListener} is a void implementation of {@link MenuListener}
 * interface.
 *
 * @author Hervé Bitteur
 */
public class AbstractMenuListener
        implements MenuListener
{
    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public void menuCanceled (MenuEvent e)
    {
    }

    @Override
    public void menuDeselected (MenuEvent e)
    {
    }

    @Override
    public void menuSelected (MenuEvent e)
    {
    }
}
