//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I n t e r M e n u                                       //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.OMR;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
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

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
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
     * @param inter     originating inter
     * @param relations set of inter relations
     */
    public InterMenu (final Inter inter,
                      final Set<Relation> relations)
    {
        this.inter = inter;

        final Sheet sheet = inter.getSig().getSystem().getSheet();
        interController = sheet.getInterController();

        menu = new SeparableMenu(new InterAction(inter, null));

        // To ensemble
        Inter ensemble = inter.getEnsemble();

        if (ensemble != null) {
            menu.add(new JMenuItem(new ToEnsembleAction(ensemble)));
        }

        // Edit mode
        if (inter.isEditable()) {
            menu.add(new JMenuItem(new EditAction(inter)));
        }

        // Shape-based selection
        if (inter.getShape() != null) {
            menu.add(new JMenuItem(new ShapeSelectionAction(inter)));
        }

        // Relations
        menu.addSeparator();
        menu.add(buildRelationsTitle(sheet, inter));

        for (Relation relation : relations) {
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

    //---------------------//
    // buildRelationsTitle //
    //---------------------//
    private JMenuItem buildRelationsTitle (final Sheet sheet,
                                           final Inter inter)
    {
        JMenuItem title = new JMenuItem("Relations:");
        title.setEnabled(false);
        title.addMouseListener(new AbstractMouseListener()
        {
            @Override
            public void mouseEntered (MouseEvent e)
            {
                sheet.getInterIndex().getEntityService().publish(
                        new EntityListEvent<>(
                                this,
                                SelectionHint.ENTITY_INIT,
                                MouseMovement.PRESSING,
                                inter));
            }
        });

        return title;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // EditAction //
    //------------//
    /**
     * Ability to set the underlying inter into edit mode.
     */
    private static class EditAction
            extends AbstractAction
    {

        /** Originating inter. */
        private final Inter inter;

        public EditAction (Inter inter)
        {
            this.inter = inter;

            putValue(NAME, "Edit");
            putValue(SHORT_DESCRIPTION, "Set inter into edit mode");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final Sheet sheet = inter.getSig().getSystem().getSheet();
            sheet.getSymbolsEditor().openEditMode(inter);
            inter.getSig().publish(inter, SelectionHint.ENTITY_TRANSIENT);
        }
    }

    //----------------------//
    // ShapeSelectionAction //
    //----------------------//
    /**
     * Ability to select all inters in sheet with the same shape as this inter.
     */
    private static class ShapeSelectionAction
            extends AbstractAction
    {

        /** Originating inter. */
        private final Inter inter;

        public ShapeSelectionAction (Inter inter)
        {
            this.inter = inter;

            putValue(NAME, "Select all " + inter.getShape() + " in sheet");
            putValue(SHORT_DESCRIPTION, "Select all inters with the same shape as this one");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final Shape shape = inter.getShape();
            final Page page = inter.getSig().getSystem().getPage();
            final List<Inter> inters = new ArrayList<>();

            for (SystemInfo system : page.getSystems()) {
                inters.addAll(system.getSig().inters(shape));
            }

            page.getSheet().getInterIndex().getEntityService().publish(
                    new EntityListEvent<>(
                            this,
                            SelectionHint.ENTITY_INIT,
                            MouseMovement.PRESSING,
                            inters));
        }
    }

    //------------------//
    // ToEnsembleAction //
    //------------------//
    /**
     * Ability to select the ensemble of underlying inter.
     */
    private static class ToEnsembleAction
            extends AbstractAction
    {

        /** Target ensemble. */
        private final Inter ensemble;

        public ToEnsembleAction (Inter ensemble)
        {
            this.ensemble = ensemble;

            putValue(NAME, "To ensemble");
            putValue(SHORT_DESCRIPTION, "Select the containing ensemble");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            ensemble.getSig().publish(ensemble, SelectionHint.ENTITY_INIT);
        }
    }

    //------------------//
    // RelationListener //
    //------------------//
    private class RelationListener
            extends AbstractMouseListener
    {

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
