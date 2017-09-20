//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I n t e r M e n u                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.util.AbstractMouseListener;
import org.audiveris.omr.ui.util.SeparableMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code InterMenu} builds a menu around a given inter.
 *
 * @author Hervé Bitteur
 */
public class InterMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InterMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final SeparableMenu menu;

    private final Inter inter;

    private final JMenuItem focusItem;

    private final Listener listener = new Listener();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code InterMenu} object.
     *
     * @param inter originating inter
     */
    public InterMenu (Inter inter)
    {
        this.inter = inter;
        menu = new SeparableMenu(new InterAction(inter, null));

        // Focus
        final Sheet sheet = inter.getSig().getSystem().getSheet();
        final InterController interController = sheet.getInterController();
        final boolean focused = inter == interController.getInterFocus();
        focusItem = new JCheckBoxMenuItem(focused ? "Unset focus" : "Set focus");
        focusItem.addMouseListener(listener);
        focusItem.setSelected(focused);
        menu.add(focusItem);
        menu.addSeparator();

        // Actions on inter
        // Existing relations
        for (Relation relation : inter.getSig().edgesOf(inter)) {
            JMenuItem item = new JMenuItem(new RelationAction(inter, relation));
            item.addMouseListener(listener);
            menu.add(item);
        }

        menu.trimSeparator();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getMenu //
    //---------//
    /**
     * @return the menu
     */
    public JMenu getMenu ()
    {
        return menu;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // Listener //
    //----------//
    private class Listener
            extends AbstractMouseListener
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void mouseEntered (MouseEvent e)
        {
            JMenuItem item = (JMenuItem) e.getSource();
            Action action = item.getAction();

            if (action instanceof RelationAction) {
                ((RelationAction) action).publish();
            } else if (action instanceof InterAction) {
                ((InterAction) action).publish();
            }
        }

        @Override
        public void mouseReleased (MouseEvent e)
        {
            if (e.getSource() == focusItem) {
                final Sheet sheet = inter.getSig().getSystem().getSheet();
                final InterController interController = sheet.getInterController();
                interController.setInterFocus(focusItem.isSelected() ? inter : null);
                ((InterAction) menu.getAction()).publish();
            }
        }
    }
}
