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
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.Relations;
import org.audiveris.omr.ui.util.AbstractMouseListener;
import org.audiveris.omr.ui.util.SeparableMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.MouseEvent;
import java.util.Set;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.audiveris.omr.sig.relation.RepeatDotPairRelation;
import org.audiveris.omr.sig.relation.TimeTopBottomRelation;

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

    private final RelationListener relationListener = new RelationListener();

    private final RelationClassListener relationClassListener = new RelationClassListener();

    private final InterController interController;

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
        interController = sheet.getInterController();

        final Inter focus = interController.getInterFocus();
        final boolean focused = inter == focus;
        focusItem = new JCheckBoxMenuItem(focused ? "Unset focus" : "Set focus");
        focusItem.addMouseListener(new FocusListener());
        focusItem.setSelected(focused);
        menu.add(focusItem);
        menu.addSeparator();

        // Existing relations (available for unlinking)
        for (Relation relation : inter.getSig().edgesOf(inter)) {
            JMenuItem item = new JMenuItem(new RelationAction(inter, relation));
            item.addMouseListener(relationListener);
            menu.add(item);
        }

        // Suggested relations from focus to inter and from inter to focus
        if ((focus != null) && !focused) {
            addSuggestedLinks(menu, focus, inter, focus);
            addSuggestedLinks(menu, inter, focus, focus);
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

    //-------------------//
    // addSuggestedLinks //
    //-------------------//
    /**
     * Insert menu items for suggestions of relations
     *
     * @param menu   menu to be populated
     * @param source relation source
     * @param target relation target
     * @param focus  current inter with focus
     */
    private void addSuggestedLinks (SeparableMenu menu,
                                    Inter source,
                                    Inter target,
                                    Inter focus)
    {
        Set<Class<? extends Relation>> set = Relations.suggestedRelationsBetween(source, target);

        if (!set.isEmpty()) {
            menu.addSeparator();

            for (Class<? extends Relation> classe : set) {
                // Specific cases
                if (DoubleDotRelation.class.isAssignableFrom(classe)) {
                    // Imposed: Source right, Target left
                    if (source.getCenter().x <= target.getCenter().x) {
                        continue;
                    }
                } else if (RepeatDotPairRelation.class.isAssignableFrom(classe)) {
                    // Imposed: Source above, Target below
                    if (source.getCenter().y >= target.getCenter().y) {
                        continue;
                    }
                } else if (TimeTopBottomRelation.class.isAssignableFrom(classe)) {
                    // Imposed: Source above, Target below
                    if (source.getCenter().y >= target.getCenter().y) {
                        continue;
                    }
                }

                JMenuItem item = new JMenuItem(
                        new RelationClassAction(source, target, classe, focus));
                item.addMouseListener(relationClassListener);
                menu.add(item);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------//
    // FocusListener //
    //---------------//
    private class FocusListener
            extends AbstractMouseListener
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void mouseEntered (MouseEvent e)
        {
            inter.getSig().publish(inter);
        }

        @Override
        public void mouseReleased (MouseEvent e)
        {
            interController.setInterFocus(focusItem.isSelected() ? inter : null);
        }
    }

    //-----------------------//
    // RelationClassListener //
    //-----------------------//
    private class RelationClassListener
            extends AbstractMouseListener
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void mouseEntered (MouseEvent e)
        {
            JMenuItem item = (JMenuItem) e.getSource();
            RelationClassAction relationClassAction = (RelationClassAction) item.getAction();
            relationClassAction.publish();
        }

        @Override
        public void mouseExited (MouseEvent e)
        {
            JMenuItem item = (JMenuItem) e.getSource();
            RelationClassAction relationClassAction = (RelationClassAction) item.getAction();
            relationClassAction.unpublish();
        }

        @Override
        public void mousePressed (MouseEvent e)
        {
            JMenuItem item = (JMenuItem) e.getSource();
            RelationClassAction relationClassAction = (RelationClassAction) item.getAction();
            relationClassAction.unpublish();
        }
    }

    //------------------//
    // RelationListener //
    //------------------//
    private class RelationListener
            extends AbstractMouseListener
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void mouseEntered (MouseEvent e)
        {
            JMenuItem item = (JMenuItem) e.getSource();
            RelationAction relationAction = (RelationAction) item.getAction();
            relationAction.publish();
        }

        @Override
        public void mouseReleased (MouseEvent e)
        {
            JMenuItem item = (JMenuItem) e.getSource();
            RelationAction ra = (RelationAction) item.getAction();
            SIGraph sig = ra.getInter().getSig();
            interController.unlink(sig, ra.getRelation());
        }
    }
}
