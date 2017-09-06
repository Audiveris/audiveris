//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I n t e r M e n u                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.util.AbstractMouseListener;
import org.audiveris.omr.ui.view.LocationDependentMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code InterMenu} displays a collection of interpretations.
 *
 * @author Hervé Bitteur
 */
public class InterMenu
        extends LocationDependentMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InterMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final Sheet sheet;

    private final InterListener interListener = new InterListener();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new InterMenu object.
     *
     * @param sheet the related sheet
     */
    public InterMenu (Sheet sheet)
    {
        super("Inters ...");
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // updateUserLocation //
    //--------------------//
    @Override
    public void updateUserLocation (Rectangle rect)
    {
        updateMenu(sheet.getInterIndex().getEntityService().getSelectedEntityList());

        super.updateUserLocation(rect);
    }

    private void insertDeletion (SystemInfo system,
                                 final List<Inter> sysInters)
    {
        JMenuItem item = new JMenuItem(
                "Delete " + sysInters.size() + " inters for System #" + system.getId() + ":");
        item.addActionListener(
                new ActionListener()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                final InterController interController = sheet.getInterController();

                interController.removeInters(sysInters, null);
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
        Map<SystemInfo, List<Inter>> interMap = new TreeMap<SystemInfo, List<Inter>>();

        if (inters != null) {
            for (Inter inter : inters) {
                SIGraph sig = inter.getSig();

                if (sig != null) {
                    SystemInfo system = sig.getSystem();

                    if (system != null) {
                        List<Inter> list = interMap.get(system);

                        if (list == null) {
                            interMap.put(system, list = new ArrayList<Inter>());
                        }

                        list.add(inter);
                    }
                }
            }
        }

        for (List<Inter> list : interMap.values()) {
            Collections.sort(list, Inter.byReverseBestGrade);
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

                    //                    UIUtil.insertTitle(
                    //                            this,
                    //                            sysInters.size() + " inters for System #" + system.getId() + ":");
                    List<Inter> sysInters = entry.getValue();
                    insertDeletion(system, sysInters);

                    for (Inter inter : sysInters) {
                        final SIGraph sig = inter.getSig();
                        final Set<Relation> rels = sig.edgesOf(inter);

                        if ((rels == null) || rels.isEmpty()) {
                            // Just a interpretation item
                            JMenuItem item = new JMenuItem(new InterAction(inter));
                            item.addMouseListener(interListener);
                            add(item);
                        } else {
                            // A whole menu of relations for this interpretation
                            JMenu relMenu = new RelationMenu(inter, rels).getMenu();
                            relMenu.addMouseListener(interListener);
                            add(relMenu);
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
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void mouseEntered (MouseEvent e)
        {
            JMenuItem item = (JMenuItem) e.getSource();
            InterAction action = (InterAction) item.getAction();
            action.publish();
        }
    }
}
