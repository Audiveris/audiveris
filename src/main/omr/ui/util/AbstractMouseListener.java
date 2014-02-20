//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            A b s t r a c t M o u s e L i s t e n e r                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Class {@code AbstractMouseListener} is a void implementation of {@link MouseListener}
 * except for the mouseEntered() method which remains to be provided by subclass.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractMouseListener
        implements MouseListener
{
    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public void mouseClicked (MouseEvent e)
    {
    }

    @Override
    public void mouseExited (MouseEvent e)
    {
    }

    @Override
    public void mousePressed (MouseEvent e)
    {
    }

    @Override
    public void mouseReleased (MouseEvent e)
    {
    }
}
