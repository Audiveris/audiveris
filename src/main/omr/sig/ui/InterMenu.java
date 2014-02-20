//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I n t e r M e n u                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.glyph.facets.Glyph;

import omr.sig.Inter;
import omr.sig.Relation;
import omr.sig.SIGraph;

import omr.ui.util.AbstractMouseListener;
import omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code InterMenu} displays a collection of interpretations.
 *
 * @author Hervé Bitteur
 */
public class InterMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InterMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final JMenu menu;

    private InterListener interListener = new InterListener();

    //~ Constructors -------------------------------------------------------------------------------
    //-----------//
    // InterMenu //
    //-----------//
    /**
     * Creates a new InterMenu object.
     */
    public InterMenu ()
    {
        menu = new JMenu("Inters ...");
    }

    //-----------//
    // InterMenu //
    //-----------//
    /**
     * Creates a new InterMenu object.
     */
    public InterMenu (Glyph glyph,
                      Collection<Inter> inters)
    {
        menu = new JMenu(new GlyphAction(glyph, null));
        updateMenu(inters);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // updateMenu //
    //------------//
    public final int updateMenu (Collection<Inter> inters)
    {
        // We rebuild the menu items on each update, since the set of inters
        // is brand new.
        menu.removeAll();

        if ((inters != null) && !inters.isEmpty()) {
            UIUtil.insertTitle(menu, "Interpretations:");

            for (Inter inter : inters) {
                final SIGraph sig = inter.getSig();
                final Set<Relation> rels = sig.edgesOf(inter);

                if ((rels == null) || rels.isEmpty()) {
                    // Just a interpretation item
                    JMenuItem item = new JMenuItem(new InterAction(inter));
                    item.addMouseListener(interListener);
                    menu.add(item);
                } else {
                    // A whole menu of relations for this interpretation
                    JMenu relMenu = new RelationMenu(inter, rels).getMenu();
                    relMenu.addMouseListener(interListener);
                    menu.add(relMenu);
                }
            }

            return inters.size();
        }

        return 0;
    }

    //---------//
    // getMenu //
    //---------//
    public JMenu getMenu ()
    {
        return menu;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------//
    // InterListener //
    //---------------//
    /**
     * Publish related inter when entered by mouse.
     */
    private class InterListener
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
