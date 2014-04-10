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

import omr.sheet.Sheet;

import omr.sig.Inter;
import omr.sig.Relation;
import omr.sig.SIGraph;

import omr.ui.util.AbstractMouseListener;
import omr.ui.util.UIUtil;
import omr.ui.view.LocationDependentMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    //-----------//
    // InterMenu //
    //-----------//
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

    //-----------//
    // InterMenu //
    //-----------//
    /**
     * Creates a new InterMenu object.
     *
     * @param sheet  the related sheet
     * @param glyph  the glyph at hand
     * @param inters the glyph interpretations
     */
    public InterMenu (Sheet sheet,
                      Glyph glyph,
                      Collection<Inter> inters)
    {
        super(new GlyphAction(glyph, null));
        this.sheet = sheet;
        updateMenu(inters);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void updateUserLocation (Rectangle rect)
    {
        List<Inter> inters = sheet.getSelectedInterList();
        Collections.sort(inters, Inter.byReverseContextualGrade);
        updateMenu(inters);

        super.updateUserLocation(rect);
    }

    //------------//
    // updateMenu //
    //------------//
    private void updateMenu (Collection<Inter> inters)
    {
        // We rebuild the menu items on each update, since the set of inters is brand new.
        removeAll();

        if ((inters != null) && !inters.isEmpty()) {
            UIUtil.insertTitle(this, "Interpretations:");

            for (Inter inter : inters) {
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

            setVisible(true);

            return;
        }

        setVisible(false);
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
