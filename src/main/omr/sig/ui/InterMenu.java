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

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph;
import omr.sig.inter.Inter;
import omr.sig.relation.Relation;

import omr.ui.util.AbstractMouseListener;
import omr.ui.util.UIUtil;
import omr.ui.view.LocationDependentMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        updateMenu(sheet.getSelectedInterList());

        super.updateUserLocation(rect);
    }

    //------------//
    // updateMenu //
    //------------//
    private void updateMenu (Collection<Inter> inters)
    {
        // Sort the inters, first by containing system, then by decreasing contextual grade
        Map<SystemInfo, List<Inter>> interMap = new TreeMap<SystemInfo, List<Inter>>();

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

        for (List<Inter> list : interMap.values()) {
            Collections.sort(list, Inter.byReverseBestGrade);
        }

        try {
            // We rebuild the menu items on each update, since the set of inters is brand new.
            removeAll();

            if ((inters != null) && !inters.isEmpty()) {
                for (SystemInfo system : interMap.keySet()) {
                    if (getMenuComponentCount() > 0) {
                        addSeparator();
                    }

                    UIUtil.insertTitle(this, "Inters for System #" + system.getId() + ":");

                    for (Inter inter : interMap.get(system)) {
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
