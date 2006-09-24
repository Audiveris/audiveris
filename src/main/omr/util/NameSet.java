//----------------------------------------------------------------------------//
//                                                                            //
//                               N a m e S e t                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.util;

import omr.constant.Constant;

import omr.ui.util.DynamicMenu;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;

/**
 * Class <code>NameSet</code> encapsulates the handling of a list of names, a
 * typical use is a history of file names.
 *
 * <p> Actually, rather than a set, it is a list where the most recently used
 * are kept at the head of the list. There is no duplicate in the set (tests are
 * case-insensitive). </p>
 *
 * <p> The NameSet can additionally be used to dynamically generate and handle a
 * menu. </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class NameSet
{
    //~ Static fields/initializers ---------------------------------------------

    /** Separator */
    private static final String   SEPARATOR = ";";

    //~ Instance fields --------------------------------------------------------

    /** Backing constant */
    private final Constant.String constant;

    /** List of names in this set */
    private final List<String> names = new ArrayList<String>();

    /** Name of this set */
    private final String setName;

    /** Max number of names in this set */
    private final int maxNameNb;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // NameSet //
    //---------//
    /**
     * Creates a new set of names, with some customizing parameters.
     *
     * @param setName   Global name for this set
     * @param maxNameNb Maximum number of elements in this name set
     */
    public NameSet (String setName,
                    int    maxNameNb)
    {
        this.setName = setName;
        this.maxNameNb = maxNameNb;

        // Retrieve the list of names in history
        constant = new Constant.String(
            setName,
            "names",
            "",
            "List of names in this name set");

        StringTokenizer st = new StringTokenizer(
            constant.getValue(),
            SEPARATOR);

        while (st.hasMoreTokens()) {
            names.add(st.nextToken());
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-----//
    // add //
    //-----//
    /**
     * Insert a (perhaps new) name at the head of the list. If the name was
     * already in the list, it is moved to the head.
     *
     * @param name Name to be inserted in the set
     */
    public void add (String name)
    {
        if ((name == null) || (name.equals(""))) {
            return;
        }

        // Remove duplicate if any
        remove(name);

        // Insert the name at the beginning of the list
        names.add(0, name);

        // Check for maximum length
        while (names.size() > maxNameNb) {
            names.remove(names.size() - 1);
        }

        // Update the constant accordingly
        StringBuffer buf = new StringBuffer(1024);

        for (String n : names) {
            if (buf.length() > 0) {
                buf.append(SEPARATOR);
            }

            buf.append(n);
        }

        constant.setValue(buf.toString());
    }

    //------//
    // menu //
    //------//
    /**
     * Return an up-to-date menu that can be used to trigger actions related to
     * the designated name.
     *
     * @param menu the existing menu to update
     * @param listener The ActionListener to be triggered. (the selected name
     *                 can be retrieved by the listener in the ActionEvent, by
     *                 using the ActionEvent.getActionCommand method)
     *
     * @return the JMenu, ready to be inserted in a menu hierarchy.
     */
    public JMenu menu (JMenu          menu,
                       ActionListener listener)
    {
        // Don't fully destroy the menu, just clean all its items, so that the
        // modifications are made available to the menu hierarchy.
        if (menu == null) {
            menu = new JMenu(setName);
        } else {
            menu.removeAll();
        }

        // Regenerate proper menu items
        for (String f : names) {
            JMenuItem menuItem = new JMenuItem(f);
            menuItem.addActionListener(listener);
            menu.add(menuItem);
        }

        return menu;
    }

    //------//
    // menu //
    //------//
    /**
     * Create a brand new menu, with the name of the set as default text for
     * menu text.
     *
     * @param listener the listener to be trigerred on item selection
     * @return the menu ready to be inserted
     */
    public JMenu menu (ActionListener listener)
    {
        return menu(setName, listener);
    }

    //------//
    // menu //
    //------//
    /**
     * Create a brand new menu for name selection, with label provided for the
     * menu and action listener
     *
     * @param label the text in the menu item
     * @param listener the listener to be trigerred on item selection
     * @return the menu ready to be inserted
     */
    public JMenu menu (String         label,
                       ActionListener listener)
    {
        return new MyMenu(label, listener).getMenu();
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
    public boolean remove (String name)
    {
        // If the ref exists in the list, it is removed
        for (Iterator<String> it = names.iterator(); it.hasNext();) {
            String f = it.next();

            if (f.equalsIgnoreCase(name)) {
                it.remove();

                return true;
            }
        }

        return false;
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // MyMenu //
    //--------//
    private class MyMenu
        extends DynamicMenu
    {
        private ActionListener listener;

        public MyMenu (String         label,
                       ActionListener listener)
        {
            super(label);
            this.listener = listener;
        }

        protected void buildItems ()
        {
            // Regenerate proper menu items
            for (String f : names) {
                JMenuItem menuItem = new JMenuItem(f);
                menuItem.addActionListener(listener);
                getMenu()
                    .add(menuItem);
            }
        }
    }
}
