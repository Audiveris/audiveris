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

import org.audiveris.omr.OMR;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.util.AbstractMouseListener;
import org.audiveris.omr.ui.util.SeparableMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.MouseEvent;

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

    private final RelationListener relationListener = new RelationListener();

    private final InterController interController;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code InterMenu} object.
     *
     * @param inter originating inter
     */
    public InterMenu (final Inter inter)
    {
        this.inter = inter;

        final Sheet sheet = inter.getSig().getSystem().getSheet();
        menu = new SeparableMenu(new InterAction(inter, null));

        // Title
        menu.add(buildTitle(sheet, inter));
        menu.addSeparator();

        interController = sheet.getInterController();

        // Existing relations (available for unlinking)
        for (Relation relation : inter.getSig().edgesOf(inter)) {
            JMenuItem item = new JMenuItem(new RelationAction(inter, relation));
            item.addMouseListener(relationListener);
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

    //------------//
    // buildTitle //
    //------------//
    private JMenuItem buildTitle (final Sheet sheet,
                                  final Inter inter)
    {
        JMenuItem title = new JMenuItem("Relations:");
        title.setEnabled(false);
        title.addMouseListener(
                new AbstractMouseListener()
        {
            @Override
            public void mouseEntered (MouseEvent e)
            {
                sheet.getInterIndex().getEntityService().publish(
                        new EntityListEvent<Inter>(
                                this,
                                SelectionHint.ENTITY_INIT,
                                MouseMovement.PRESSING,
                                inter));
            }
        });

        return title;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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
            Relation relation = ra.getRelation();
            String relStr = relation.getName();

            if (OMR.gui.displayConfirmation("Remove " + relStr + " relation?")) {
                interController.unlink(sig, relation);
            }
        }
    }
}
