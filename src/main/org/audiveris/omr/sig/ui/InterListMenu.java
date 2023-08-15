//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    I n t e r L i s t M e n u                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.util.AbstractMouseListener;
import org.audiveris.omr.ui.view.LocationDependentMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JMenuItem;

/**
 * Class <code>InterListMenu</code> displays a collection of interpretations.
 *
 * @author Hervé Bitteur
 */
public class InterListMenu
        extends LocationDependentMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InterListMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------

    private final Sheet sheet;

    private final InterListener interListener = new InterListener();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>InterListMenu</code> object.
     *
     * @param sheet the related sheet
     */
    public InterListMenu (Sheet sheet)
    {
        super("Inters");
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------------//
    // insertDeletionItem //
    //--------------------//
    private void insertDeletionItem (final SystemInfo system,
                                     final List<Inter> sysInters)
    {
        JMenuItem item = new JMenuItem(
                "Delete " + sysInters.size() + " inters for System #" + system.getId() + ":");

        // To delete all listed inters when item is clicked upon
        item.addActionListener( (ActionEvent e) ->
        {
            if (OMR.gui.displayConfirmation(
                    "Do you confirm the removal of " + sysInters.size() + " inter(s)?")) {
                sheet.getInterController().removeInters(sysInters);
            }
        });

        // To (re)focus on all the listed inters when moving the mouse on the item
        item.addMouseListener(new AbstractMouseListener()
        {
            @Override
            public void mouseEntered (MouseEvent e)
            {
                system.getSheet().getInterIndex().getEntityService().publish(
                        new EntityListEvent<>(
                                this,
                                SelectionHint.ENTITY_INIT,
                                MouseMovement.PRESSING,
                                sysInters));
            }
        });
        this.add(item);
        this.addSeparator();
    }

    //------------//
    // updateMenu //
    //------------//
    private void updateMenu (Collection<Inter> inters)
    {
        // Sort the inters, first by containing system, then by decreasing contextual grade
        Map<SystemInfo, List<Inter>> interMap = new TreeMap<>();

        if (inters != null) {
            for (Inter inter : inters) {
                SIGraph sig = inter.getSig();

                if (sig != null) {
                    SystemInfo system = sig.getSystem();

                    if (system != null) {
                        List<Inter> list = interMap.get(system);

                        if (list == null) {
                            interMap.put(system, list = new ArrayList<>());
                        }

                        list.add(inter);
                    }
                }
            }
        }

        for (List<Inter> list : interMap.values()) {
            Collections.sort(list, Inters.byReverseBestGrade);
        }

        try {
            // We rebuild the menu items on each update, since the set of inters is brand new.
            removeAll();

            if ((inters != null) && !inters.isEmpty()) {
                for (Entry<SystemInfo, List<Inter>> entry : interMap.entrySet()) {
                    SystemInfo system = entry.getKey();

                    if (getMenuComponentCount() > 0) {
                        addSeparator();
                    }

                    List<Inter> sysInters = entry.getValue();
                    insertDeletionItem(system, sysInters);

                    for (Inter inter : sysInters) {
                        if (!inter.isRemoved()) {
                            // A menu dedicated to this inter instance
                            final Set<Relation> relations = inter.getSig().edgesOf(inter);
                            final JMenuItem item = new InterMenu(inter, relations).getMenu();
                            item.addMouseListener(interListener);
                            add(item);
                        }
                    }
                }

                setVisible(true);

                return;
            }

            setVisible(false);
        } catch (Exception ex) {
            logger.warn("Error updating menu " + ex, ex);
        }
    }

    //--------------------//
    // updateUserLocation //
    //--------------------//
    @Override
    public void updateUserLocation (Rectangle rect)
    {
        updateMenu(sheet.getInterIndex().getEntityService().getSelectedEntityList());

        super.updateUserLocation(rect);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------------//
    // InterListener //
    //---------------//
    /**
     * Publish related inter when entered by mouse.
     */
    private static class InterListener
            extends AbstractMouseListener
    {

        @Override
        public void mouseEntered (MouseEvent e)
        {
            JMenuItem item = (JMenuItem) e.getSource();
            InterAction action = (InterAction) item.getAction();
            action.publish();
        }
    }
}
