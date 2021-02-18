//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P a t h H i s t o r y                                     //
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
package org.audiveris.omr.util;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.ui.view.HistoryMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

/**
 * Class {@code PathHistory} handles a history of paths, as used for latest input or
 * book files.
 *
 * @author Hervé Bitteur
 */
public class PathHistory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PathHistory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying list of names. */
    private final NameSet nameSet;

    /** Name of last folder used, if any. */
    private final Constant.String folderConstant;

    /** Related UI menu, if any. Null when no UI is used */
    private HistoryMenu menu;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PathHistory} object.
     *
     * @param name           a name for this history instance
     * @param constant       backing constant on disk
     * @param folderConstant backing constant for last folder, or null
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
    /**
     * Add a path to history
     *
     * @param path the path to include
     */
    public void add (Path path)
    {
        nameSet.add(path.toAbsolutePath().toString());

        Path parent = path.toAbsolutePath().getParent();

        if (folderConstant != null) {
            folderConstant.setStringValue(parent.toAbsolutePath().toString());
        }

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
    /**
     * Populate a menu with path history.
     *
     * @param menu         menu to populate, if null it is allocated
     * @param itemListener listener for each menu item
     * @return the populated menu
     */
    public JMenu feedMenu (JMenu menu,
                           final ActionListener itemListener)
    {
        return nameSet.feedMenu(menu, itemListener);
    }

    //----------//
    // getFirst //
    //----------//
    /**
     * Report the first path in history.
     *
     * @return first path, null if none
     */
    public Path getFirst ()
    {
        final String first = nameSet.first();

        return (first != null) ? Paths.get(first) : null;
    }

    //---------//
    // isEmpty //
    //---------//
    /**
     * Tell whether history is empty.
     *
     * @return true if so
     */
    public boolean isEmpty ()
    {
        return nameSet.isEmpty();
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove a path from history
     *
     * @param path to be removed
     * @return true if actually removed
     */
    public boolean remove (Path path)
    {
        return nameSet.remove(path.toAbsolutePath().toString());
    }

    //---------//
    // setMenu //
    //---------//
    /**
     * Set the related UI menu
     *
     * @param menu the related menu
     */
    public void setMenu (HistoryMenu menu)
    {
        this.menu = menu;
    }
}
