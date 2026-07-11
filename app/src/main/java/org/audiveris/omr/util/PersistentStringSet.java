//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              P e r s i s t e n t S t r i n g S e t                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.ui.util.AbstractMenuListener;

import net.jcip.annotations.ThreadSafe;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * Class <code>PersistentStringSet</code> handles a persistent list of strings,
 * a typical use is a history of file names.
 * <p>
 * Actually, rather than a set, it is a list where no equivalent strings are left.
 * <p>
 * The PersistentStringSet can additionally be used to dynamically generate and handle a menu
 * of the contained strings.
 * </p>
 * Persistency is implemented by the optional use of an application constant.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class PersistentStringSet
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Global name for this set. */
    private final String setName;

    /** Backing constant. */
    private final Constant.String constant;

    /** List of strings in this set. */
    private final List<String> strings = new ArrayList<>();

    /** The separator between two strings. */
    private final String separator;

    /** If not null, maximum number of elements in this set. */
    private final Integer maxCount;

    /** To check strings equivalence. */
    private final PairPredicate<String> predicate;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new set of names, with some customizing parameters.
     *
     * @param setName   global name for this set
     * @param separator the separator between two string entities
     * @param constant  the backing constant string, or null
     * @param maxCount  maximum number of elements in this name set, or null
     * @param predicate predicate to test elements equivalence
     */
    public PersistentStringSet (String setName,
                                String separator,
                                Constant.String constant,
                                Integer maxCount,
                                PairPredicate<String> predicate)
    {
        this.setName = setName;
        this.separator = separator;
        this.constant = constant;
        this.maxCount = maxCount;
        this.predicate = predicate;

        // Retrieve the list of strings already in the set
        reload();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----//
    // add //
    //-----//
    /**
     * Insert a (perhaps new) string at some location index in the list.
     * If the string was already in the list, it is first removed.
     *
     * @param index targeted index in the list
     * @param str   string to be inserted in the list
     */
    public synchronized void add (int index,
                                  String str)
    {
        if ((str == null) || (str.isEmpty())) {
            return;
        }

        // Remove duplicate or equivalent if any
        remove(str);

        // Insert the name at the targeted index
        strings.add(index, str);

        // Check for maximum length?
        if (maxCount != null) {
            while (strings.size() > maxCount) {
                strings.remove(strings.size() - 1);
            }
        }

        // Update the constant accordingly
        updateConstant();
    }

    //-------//
    // clear //
    //-------//
    /**
     * Empty the set.
     */
    public synchronized void clear ()
    {
        strings.clear();

        // Update the constant accordingly
        updateConstant();
    }

    //----------//
    // feedMenu //
    //----------//
    /**
     * Feed a menu with the dynamic content of this PersistentStringSet.
     *
     * @param menu         the menu to be fed, if null it is allocated by this method
     * @param itemListener the listener to be called on item selection
     * @return the menu properly populated
     */
    public JMenu feedMenu (JMenu menu,
                           final ActionListener itemListener)
    {
        final JMenu finalMenu = (menu != null) ? menu : new JMenu(setName);

        final MenuListener menuListener = new AbstractMenuListener()
        {
            @Override
            public void menuSelected (MenuEvent e)
            {
                // Clean up the whole menu
                finalMenu.removeAll();

                // Rebuild the whole list of menu items on the fly
                synchronized (PersistentStringSet.this) {
                    for (String f : strings) {
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

    //-------//
    // first //
    //-------//
    /**
     * Report the first name if any.
     *
     * @return first name or null if empty
     */
    public synchronized String first ()
    {
        if (isEmpty()) {
            return null;
        }

        return strings.get(0);
    }

    //-------------//
    // getElements //
    //-------------//
    /**
     * Report a view on the contained elements.
     *
     * @return non modifiable list of the elements
     */
    public synchronized List<String> getElements ()
    {
        return Collections.unmodifiableList(strings);
    }

    //---------//
    // isEmpty //
    //---------//
    /**
     * Tell whether the set is empty
     *
     * @return true if so
     */
    public synchronized boolean isEmpty ()
    {
        return strings.isEmpty();
    }

    //--------//
    // reload //
    //--------//
    /** Reload the strings from the underlying constant, if any. */
    public synchronized void reload ()
    {
        if (constant != null) {
            strings.clear();
            final String[] vals = constant.getValue().split(separator);

            if (!vals[0].isEmpty()) {
                strings.addAll(Arrays.asList(vals));
            }
        }
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove a given string from the list
     *
     * @param str the string to remove
     * @return true if actually found and removed
     */
    public synchronized boolean remove (String str)
    {
        // If the ref (or an equivalent ref) exists in the list, it is removed
        for (Iterator<String> it = strings.iterator(); it.hasNext();) {
            final String s = it.next();

            if (predicate.test(str, s)) {
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
    /**
     * Update the underlying application constant.
     */
    private void updateConstant ()
    {
        if (constant != null) {
            final String str = strings.stream().collect(Collectors.joining(separator));
            constant.setStringValue(str);
        }
    }
}
