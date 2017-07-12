//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t A c t i o n M e n u                              //
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
package org.audiveris.omr.glyph.ui;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.util.SeparableMenu;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code AbstractActionMenu}
 * <p>
 * In a menu, actions are physically grouped by semantic tag and
 * separators are inserted between such groups.</p>
 * <p>
 * Actions are also organized according to their target menu level,
 * to allow actions to be dispatched into a hierarchy of menus.
 * Although currently all levels are set to 0.</p>
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractActionMenu
        extends AbstractGlyphMenu
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Map action -> tag to update according to context */
    private final Map<DynAction, Integer> dynActions = new LinkedHashMap<DynAction, Integer>();

    /** Map action -> menu level */
    private final Map<DynAction, Integer> levels = new LinkedHashMap<DynAction, Integer>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractActionMenu object.
     *
     * @param sheet the related sheet
     * @param text  the menu text
     */
    public AbstractActionMenu (Sheet sheet,
                               String text)
    {
        super(sheet, text);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // updateMenu //
    //------------//
    @Override
    public int updateMenu (Collection<Glyph> glyphs)
    {
        super.updateMenu(glyphs);

        // Update all dynamic actions accordingly
        for (DynAction action : dynActions.keySet()) {
            action.update();
        }

        return glyphNb;
    }

    //----------//
    // initMenu //
    //----------//
    @Override
    protected void initMenu ()
    {
        super.initMenu();

        // Sort actions on their tag
        SortedSet<Integer> tags = new TreeSet<Integer>(dynActions.values());

        // Retrieve the highest menu level
        int maxLevel = 0;

        for (Integer level : levels.values()) {
            maxLevel = Math.max(maxLevel, level);
        }

        // Generate the hierarchy of menus
        JMenu prevMenu = getMenu();

        for (int level = 0; level <= maxLevel; level++) {
            JMenu currentMenu = (level == 0) ? getMenu() : new SeparableMenu("Continued ...");

            for (Integer tag : tags) {
                for (Map.Entry<DynAction, Integer> entry : dynActions.entrySet()) {
                    if (entry.getValue().equals(tag)) {
                        DynAction action = entry.getKey();

                        if (levels.get(action) == level) {
                            currentMenu.add(action.getMenuItem());
                        }
                    }
                }

                currentMenu.addSeparator();
            }

            if (currentMenu instanceof SeparableMenu) {
                ((SeparableMenu) currentMenu).trimSeparator();
            }

            if ((level > 0) && (currentMenu.getMenuComponentCount() > 0)) {
                // Insert this menu as a submenu of the previous one
                prevMenu.addSeparator();
                prevMenu.add(currentMenu);
                prevMenu = currentMenu;
            }
        }
    }

    //----------//
    // register //
    //----------//
    /**
     * Register this action instance in the set of dynamic actions
     *
     * @param menuLevel which menu should host the action item
     * @param action    the action to register
     */
    protected void register (int menuLevel,
                             DynAction action)
    {
        levels.put(action, menuLevel);
        dynActions.put(action, action.tag);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // DynAction //
    //-----------//
    /**
     * Base implementation, to register the dynamic actions that need
     * to be updated according to the current glyph selection context.
     */
    protected abstract class DynAction
            extends AbstractAction
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Semantic tag */
        protected final int tag;

        //~ Constructors ---------------------------------------------------------------------------
        public DynAction (int tag)
        {
            this.tag = tag;
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Report which item class should be used to the related menu item
         *
         * @return the precise menu item class
         */
        public JMenuItem getMenuItem ()
        {
            return new JMenuItem(this);
        }

        /**
         * Method to update the action according to the current context
         */
        public abstract void update ();
    }
}
