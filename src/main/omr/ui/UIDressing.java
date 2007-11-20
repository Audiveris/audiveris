//----------------------------------------------------------------------------//
//                                                                            //
//                            U I D r e s s i n g                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.ui;

import omr.ui.icon.IconManager;

import omr.util.Logger;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>UIDressing</code> takes care of dressing actions or directly menu
 * items, according to language-specific definitions found in application
 * bundles
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class UIDressing
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(UIDressing.class);

    /** List of the predefined Action property keywords */
    private static final String[] actionKeys = {
                                                   Action.NAME,
                                                   Action.SHORT_DESCRIPTION,
                                                   Action.LONG_DESCRIPTION,
                                                   Action.SMALL_ICON,
                                                   Action.ACTION_COMMAND_KEY,
                                                   Action.ACCELERATOR_KEY,
                                                   Action.MNEMONIC_KEY,
                                               };

    //~ Methods ----------------------------------------------------------------

    //---------//
    // dressUp //
    //---------//
    /**
     * Dress up a given action, and thus any UI entity such as a menu item of a
     * button built upon this action, with its NAME, SHORT_DESCRIPTION,
     * SMALL_ICON, etc as defined in the Action class
     *
     * @param action the action to be dressed up
     * @param qualifiedName a fully qualified name to designate the action
     * instance in the resource file
     *
     * @see javax.swing.Action
     */
    public static void dressUp (Action action,
                                String qualifiedName)
    {
        final OmrUIDefaults defaults = OmrUIDefaults.getInstance();

        if (logger.isFineEnabled()) {
            logger.fine("qualifiedName=" + qualifiedName);
        }

        for (String k : actionKeys) {
            String mk = qualifiedName + "." + k;

            if (k == Action.MNEMONIC_KEY) {
                action.putValue(k, defaults.getKeyCode(mk));
            } else if (k == Action.ACCELERATOR_KEY) {
                action.putValue(k, defaults.getKeyStroke(mk));
            } else if (k == Action.SMALL_ICON) {
                String iconName = (String) defaults.get(mk);

                if (iconName != null) {
                    action.putValue(
                        k,
                        IconManager.getInstance().loadImageIcon(iconName));
                }
            } else {
                action.putValue(k, defaults.get(mk));
            }
        }
    }

    //---------//
    // dressUp //
    //---------//
    /**
     * Dress up a given menu (or menu item) with its NAME (text),
     * SHORT_DESCRIPTION (tool tip text) and ICON.
     *
     * @param item the menu to dress up
     * @param qualifiedName a fully qualified name to designate the menu
     * instance in the resource file
     */
    public static void dressUp (JMenuItem item,
                                String    qualifiedName)
    {
        final OmrUIDefaults defaults = OmrUIDefaults.getInstance();

        if (logger.isFineEnabled()) {
            logger.fine("qualifiedName=" + qualifiedName);
        }

        for (String k : actionKeys) {
            String mk = qualifiedName + "." + k;

            if (k == Action.NAME) {
                item.setText((String) defaults.get(mk));
            } else if (k == Action.SHORT_DESCRIPTION) {
                item.setToolTipText((String) defaults.get(mk));
            } else if (k == Action.SMALL_ICON) {
                String iconName = (String) defaults.get(mk);

                if (iconName != null) {
                    item.setIcon(
                        IconManager.getInstance().loadImageIcon(iconName));
                }
            }
        }
    }
}
