//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    E n t i t y A c t i o n                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.ui;

import org.audiveris.omr.ui.util.UIUtil;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

/**
 * Class {@code EntityAction} is a template for any entity-dependent action.
 * It builds the action, registers it in the list of entity-dependent actions if such list if
 * provided, inserts the action in the proper menu, and inserts a button in the toolbar if provided.
 *
 * @author Brenton Partridge
 * @author Hervé Bitteur
 */
public class EntityAction
        extends AbstractAction
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Delegation to an existing action, if any */
    private Action delegate = null;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates an action, and registers the action in the provided menu as
     * well as in the toolbar (if so desired)
     *
     * @param entityActions collection of actions that depend on existence of a
     *                      current entity, or null
     * @param menu          the menu where the related item is to be inserted
     * @param toolBar       the toolBar for icon insertion (if so desired), or
     *                      null
     * @param label         label for the menu item
     * @param tip           tooltip text
     * @param key           accelerator key, or null
     * @param icon          icon for menu and toolbar, or null
     */
    protected EntityAction (Collection<Action> entityActions,
                            JMenu menu,
                            JToolBar toolBar,
                            String label,
                            String tip,
                            String key,
                            Icon icon)
    {
        super(label, icon);

        // Entity-dependent action ?
        if (entityActions != null) {
            entityActions.add(this);
        }

        // Always add the related Menu item
        JMenuItem item = menu.add(this);

        // Tooltip
        putValue(SHORT_DESCRIPTION, tip);

        // Accelerator key?
        if (key != null) {
            item.setAccelerator(
                    KeyStroke.getKeyStroke(
                            key.charAt(0),
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        // Add an icon in the Tool bar?
        if ((toolBar != null) && (icon != null)) {
            final JButton button = toolBar.add(this);
            button.setBorder(UIUtil.getToolBorder());
        }
    }

    //-----------//
    // OmrAction //
    //-----------//
    /**
     * Wraps an existing action, used as a delegate.
     *
     * @param entityActions collection of actions
     * @param menu          the menu where the related item is to be inserted
     * @param toolBar       the toolBar for icon insertion
     * @param delegate      the existing action
     */
    protected EntityAction (Collection<Action> entityActions,
                            JMenu menu,
                            JToolBar toolBar,
                            Action delegate)
    {
        this(
                entityActions,
                menu,
                toolBar,
                (String) delegate.getValue(Action.NAME),
                (String) delegate.getValue(Action.SHORT_DESCRIPTION),
                (String) delegate.getValue(Action.ACCELERATOR_KEY),
                (Icon) delegate.getValue(Action.SMALL_ICON));
        this.delegate = delegate;
    }

    //-----------//
    // OmrAction //
    //-----------//
    /**
     * Convenient constructor with no delegate and no accelerator.
     *
     * @param entityActions collection of actions
     * @param menu          the menu where the related item is to be inserted
     * @param toolBar       the toolBar for icon insertion or null
     * @param label         label for the menu item
     * @param tip           tooltip text
     * @param icon          icon for menu and toolbar, or null
     */
    protected EntityAction (Collection<Action> entityActions,
                            JMenu menu,
                            JToolBar toolBar,
                            String label,
                            String tip,
                            Icon icon)
    {
        this(entityActions, menu, toolBar, label, tip, null, icon);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // actionPerformed //
    //-----------------//
    @Override
    public void actionPerformed (ActionEvent e)
    {
        if (delegate != null) {
            delegate.actionPerformed(e);
        }
    }
}
