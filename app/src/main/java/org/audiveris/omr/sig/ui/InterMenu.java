//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I n t e r M e n u                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class <code>InterMenu</code> builds a menu around a given inter.
 * <p>
 * TODO: I18N of this class
 *
 * @author Hervé Bitteur
 */
public class InterMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InterMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------

    private final SeparableMenu menu;

    private final RelationListener relationListener = new RelationListener();

    private final InterController interController;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>InterMenu</code> object.
     *
     * @param inter     originating inter
     * @param relations set of inter relations
     */
    public InterMenu (final Inter inter,
                      final Set<Relation> relations)
    {
        final Sheet sheet = inter.getSig().getSystem().getSheet();
        interController = sheet.getInterController();

        menu = new SeparableMenu(new InterAction(inter, null));

        // To ensemble
        Inter ensemble = inter.getEnsemble();

        if (ensemble != null) {
            menu.add(new JMenuItem(new MenuToEnsembleAction(ensemble)));
        }

        // Edit mode
        if (inter.isEditable()) {
            menu.add(new JMenuItem(new EditAction(inter)));
        }

        // Deassign
        if (!inter.isRemoved()) {
            menu.add(new JMenuItem(new DeassignAction(inter)));
        }

        // Shape-based selection
        if (inter.getShape() != null) {
            menu.add(new JMenuItem(new ShapeSelectionAction(inter)));
        }

        // Relations
        menu.addSeparator();
        menu.add(buildRelationsTitle(sheet, inter));

        for (Relation relation : relations) {
            JMenuItem item = new RelationMenu(inter, relation);
            item.addMouseListener(relationListener);
            menu.add(item);
        }

        menu.trimSeparator();
    }

    //~ Methods ------------------------------------------------------------------------------------

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

    //----------------//
    // DeassignAction //
    //----------------//
    private static class DeassignAction
            extends AbstractAction
    {
        /** Originating inter. */
        private final Inter inter;

        public DeassignAction (Inter inter)
        {
            this.inter = inter;

            putValue(NAME, "Deassign");
            putValue(SHORT_DESCRIPTION, "Deassign inter");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Delete the inter
            final Sheet sheet = inter.getSig().getSystem().getSheet();
            sheet.getInterController().removeInters(Arrays.asList(inter));
        }
    }

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
            sheet.getSheetEditor().openEditMode(inter);
            inter.getSig().publish(inter, SelectionHint.ENTITY_TRANSIENT);
        }
    }

    //----------------------//
    // MenuToEnsembleAction //
    //----------------------//
    /**
     * Ability to select the ensemble of underlying inter.
     */
    private static class MenuToEnsembleAction
            extends AbstractAction
    {
        /** Target ensemble. */
        private final Inter ensemble;

        public MenuToEnsembleAction (Inter ensemble)
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
    }

    //--------------//
    // RelationMenu //
    //--------------//
    private class RelationMenu
            extends JMenu
            implements ActionListener
    {
        private final Inter inter;

        private final Relation relation;

        private final JMenuItem dumpItem;

        private final JMenuItem deleteItem;

        public RelationMenu (Inter inter,
                             Relation relation)
        {
            super(new RelationAction(inter, relation));

            this.inter = inter;
            this.relation = relation;

            // Dump
            add(dumpItem = new JMenuItem("Dump"));
            dumpItem.addActionListener(this);

            add(deleteItem = new JMenuItem("Delete"));
            deleteItem.addActionListener(this);
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final Object source = e.getSource();
            final SIGraph sig = inter.getSig();

            if (source == dumpItem) {
                logger.info("{}", relation.dumpOf(sig));
            } else if (source == deleteItem) {
                if (OMR.gui.displayConfirmation("Remove " + relation.getName() + " relation?")) {
                    interController.unlink(sig, relation);
                }
            }
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
}
