//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P a t h H i s t o r y                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.OMR;

import omr.constant.Constant;

import omr.ui.view.HistoryMenu;

import java.awt.event.ActionListener;
import java.nio.file.Path;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

/**
 * Class {@code PathHistory} handles a history of paths, as used for latest input or
 * project files.
 *
 * @author Hervé Bitteur
 */
public class PathHistory
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Underlying list of names. */
    private final NameSet nameSet;

    /** Name of last folder used. */
    private final Constant.String folderConstant;

    /** Related UI menu, if any. Null when no UI is used */
    private HistoryMenu menu;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PathHistory} object.
     *
     * @param name           a name for this history instance
     * @param constant       backing constant on disk
     * @param folderConstant backing constant for last folder
     * @param maxSize        maximum items in history
     */
    public PathHistory (String name,
                        Constant.String constant,
                        Constant.String folderConstant,
                        int maxSize)
    {
        nameSet = new NameSet(name, constant, maxSize);
        this.folderConstant = folderConstant;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----//
    // add //
    //-----//
    public void add (Path path)
    {
        nameSet.add(path.toAbsolutePath().toString());

        Path parent = path.getParent();
        folderConstant.setValue(parent.toAbsolutePath().toString());

        if (OMR.gui != null) {
            // Enable input history menu
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    menu.setEnabled(true);
                }
            });
        }
    }

    //----------//
    // feedMenu //
    //----------//
    public JMenu feedMenu (JMenu menu,
                           final ActionListener itemListener)
    {
        return nameSet.feedMenu(menu, itemListener);
    }

    //---------//
    // isEmpty //
    //---------//
    public boolean isEmpty ()
    {
        return nameSet.isEmpty();
    }

    //--------//
    // remove //
    //--------//
    public boolean remove (Path path)
    {
        return nameSet.remove(path.toAbsolutePath().toString());
    }

    //---------//
    // setMenu //
    //---------//
    public void setMenu (HistoryMenu menu)
    {
        this.menu = menu;
    }
}
