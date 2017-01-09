//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     R e l a t i o n M e n u                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.util.AbstractMouseListener;

import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;

/**
 * Class {@code RelationMenu} builds a menu with all relations around a given inter.
 *
 * @author Hervé Bitteur
 */
public class RelationMenu
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final JMenu menu;

    private final Listener listener = new Listener();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new RelationMenu object.
     *
     * @param inter originating inter
     * @param rels  the relations around inter
     */
    public RelationMenu (Inter inter,
                         Collection<? extends Relation> rels)
    {
        menu = new JMenu(new InterAction(inter, null));

        // Show the originating inter
        JMenuItem interItem = new JMenuItem(new InterAction(inter, "Relations of " + inter + ":"));
        interItem.setHorizontalAlignment(SwingConstants.CENTER);
        interItem.addMouseListener(listener);
        menu.add(interItem);
        menu.addSeparator();

        // Look for a BeamStem relation around a stem
        ///BeamStemRelation beamStemRel = null;
        //            if (inter instanceof StemInter) {
        //                for (Relation relation : rels) {
        //                    if (relation instanceof BeamStemRelation) {
        //                        beamStemRel = (BeamStemRelation) relation;
        //
        //                        break;
        //                    }
        //                }
        //            }
        // Show each relation
        for (Relation relation : rels) {
            JMenuItem item = new JMenuItem(new RelationAction(inter, relation));
            item.addMouseListener(listener);
            menu.add(item);
        }
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
    private static class Listener
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
    }
}
