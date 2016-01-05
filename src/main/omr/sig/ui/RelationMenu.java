//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     R e l a t i o n M e n u                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.sig.inter.Inter;
import omr.sig.relation.Relation;

import omr.ui.util.AbstractMouseListener;

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
