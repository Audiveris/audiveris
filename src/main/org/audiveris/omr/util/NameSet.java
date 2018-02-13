//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         N a m e S e t                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import net.jcip.annotations.ThreadSafe;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.ui.util.AbstractMenuListener;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * Class {@code NameSet} encapsulates the handling of a list of names,
 * a typical use is a history of file names.
 * <p>
 * Actually, rather than a set, it is a list where the most recently used
 * are kept at the head of the list. There is no duplicate in the set (tests are
 * case-insensitive). </p>
 * <p>
 * The NameSet can additionally be used to dynamically generate and handle a menu. </p>
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class NameSet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Separator */
    private static final String SEPARATOR = ";";

    //~ Instance fields ----------------------------------------------------------------------------
    /** Global name for this set. */
    private final String setName;

    /** Backing constant. */
    private final Constant.String constant;

    /** List of names in this set. */
    private final List<String> names = new ArrayList<String>();

    /** Max number of names in this set. */
    private final int maxNameCount;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new set of names, with some customizing parameters.
     *
     * @param setName      global name for this set
     * @param constant     the backing constant string
     * @param maxNameCount maximum number of elements in this name set
     */
    public NameSet (String setName,
                    Constant.String constant,
                    int maxNameCount)
    {
        this.setName = setName;
        this.constant = constant;
        this.maxNameCount = maxNameCount;

        // Retrieve the list of names already in the set
        String[] vals = constant.getValue().split(SEPARATOR);

        if (!vals[0].isEmpty()) {
            names.addAll(Arrays.asList(vals));
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----//
    // add //
    //-----//
    /**
     * Insert a (perhaps new) name at the head of the list.
     * If the name was already in the list, it is moved to the head.
     *
     * @param name Name to be inserted in the set
     */
    public synchronized void add (String name)
    {
        if ((name == null) || (name.isEmpty())) {
            return;
        }

        // Remove duplicate if any
        remove(name);

        // Insert the name at the beginning of the list
        names.add(0, name);

        // Check for maximum length
        while (names.size() > maxNameCount) {
            names.remove(names.size() - 1);
        }

        // Update the constant accordingly
        updateConstant();
    }

    //----------//
    // feedMenu //
    //----------//
    /**
     * Feed a menu with the dynamic content of this NameSet.
     *
     * @param menu         the menu to be fed, if null it is allocated by this method
     * @param itemListener the listener to be called on item selection
     * @return the menu properly dynamized
     */
    public JMenu feedMenu (JMenu menu,
                           final ActionListener itemListener)
    {
        final JMenu finalMenu = (menu != null) ? menu : new JMenu(setName);

        MenuListener menuListener = new AbstractMenuListener()
        {
            @Override
            public void menuSelected (MenuEvent e)
            {
                // Clean up the whole menu
                finalMenu.removeAll();

                // Rebuild the whole list of menu items on the fly
                synchronized (NameSet.this) {
                    for (String f : names) {
                        JMenuItem menuItem = new JMenuItem(f);
                        menuItem.addActionListener(itemListener);
                        finalMenu.add(menuItem);
                    }
                }
            }
        };

        // Listener to menu selection, to modify content on-the-fly
        finalMenu.addMenuListener(menuListener);

        return finalMenu;
    }

    //---------//
    // isEmpty //
    //---------//
    public synchronized boolean isEmpty ()
    {
        return names.isEmpty();
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove a given name from the list
     *
     * @param name the name to remove
     * @return true if actually found and removed
     */
    public synchronized boolean remove (String name)
    {
        // If the ref exists in the list, it is removed
        for (Iterator<String> it = names.iterator(); it.hasNext();) {
            String f = it.next();

            if (f.equalsIgnoreCase(name)) {
                it.remove();
                updateConstant();

                return true;
            }
        }

        return false;
    }

    //----------------//
    // updateConstant //
    //----------------//
    private void updateConstant ()
    {
        StringBuilder buf = new StringBuilder(1024);

        for (String n : names) {
            if (buf.length() > 0) {
                buf.append(SEPARATOR);
            }

            buf.append(n);
        }

        constant.setValue(buf.toString());
    }
}
