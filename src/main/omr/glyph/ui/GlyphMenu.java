//----------------------------------------------------------------------------//
//                                                                            //
//                             G l y p h M e n u                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Nest;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.selection.GlyphEvent;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;

import omr.ui.util.SeparableMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Abstract class {@code GlyphMenu} is the base for glyph-based
 * menus such as {@link SymbolMenu}.
 * It also provides implementation for basic actions: copy, paste, assign,
 * compound, deassign and dump.
 *
 * <p>In a menu, actions are physically grouped by semantic tag and separators
 * are inserted between such groups.</p>
 *
 * <p>Actions are also organized according to their target menu level, to
 * allow actions to be dispatched into a hierarchy of menus.
 * Although currently all levels are set to 0.</p>
 *
 * @author Hervé Bitteur
 */
public abstract class GlyphMenu
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(GlyphMenu.class);

    //~ Instance fields --------------------------------------------------------
    /** Map action -> tag to update according to context */
    private final Map<DynAction, Integer> dynActions = new LinkedHashMap<>();

    /** Map action -> menu level */
    private final Map<DynAction, Integer> levels = new LinkedHashMap<>();

    /** Concrete menu */
    private final SeparableMenu menu = new SeparableMenu();

    /** The controller in charge of user gesture */
    protected final GlyphsController controller;

    /** Related sheet */
    protected final Sheet sheet;

    /** Related nest */
    protected final Nest nest;

    /** Current number of selected glyphs */
    protected int glyphNb;

    /** Current number of known glyphs */
    protected int knownNb;

    /** Current number of stems */
    protected int stemNb;

    /** Current number of virtual glyphs */
    protected int virtualNb;

    /** Sure we have no virtual glyphs? */
    protected boolean noVirtuals;

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // GlyphMenu //
    //-----------//
    /**
     * Creates a new GlyphMenu object.
     *
     * @param controller the related glyphs controller
     */
    public GlyphMenu (GlyphsController controller)
    {
        this.controller = controller;

        sheet = controller.sheet;
        nest = controller.getNest();

        buildMenu();
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // getMenu //
    //---------//
    /**
     * Report the concrete menu.
     *
     * @return the menu
     */
    public JMenu getMenu ()
    {
        return menu;
    }

    //------------//
    // updateMenu //
    //------------//
    /**
     * Update the menu according to the currently selected glyphs.
     *
     * @return the number of selected glyphs
     */
    public int updateMenu ()
    {
        // Analyze the context
        glyphNb = 0;
        knownNb = 0;
        stemNb = 0;
        virtualNb = 0;

        Set<Glyph> glyphs = nest.getSelectedGlyphSet();

        if (glyphs != null) {
            glyphNb = glyphs.size();

            for (Glyph glyph : glyphs) {
                if (glyph.isKnown()) {
                    knownNb++;

                    if (glyph.getShape() == Shape.STEM) {
                        stemNb++;
                    }
                }

                if (glyph.isVirtual()) {
                    virtualNb++;
                }
            }
        }

        noVirtuals = (virtualNb == 0);

        // Update all dynamic actions accordingly
        for (DynAction action : dynActions.keySet()) {
            action.update();
        }

        // Update the menu root item
        menu.setEnabled(glyphNb > 0);

        if (glyphNb > 0) {
            menu.setText("Glyphs ...");
        } else {
            menu.setText("no glyph");
        }

        return glyphNb;
    }

    //-----------------//
    // registerActions //
    //-----------------//
    /**
     * Register all actions to be used in the menu
     */
    protected abstract void registerActions ();

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

    //-----------//
    // buildMenu //
    //-----------//
    /**
     * Build the menu instance, grouping the actions with the same tag
     * and separating them from other tags, and organize actions into
     * their target menu level.
     */
    private void buildMenu ()
    {
        // Register actions
        registerActions();

        // Sort actions on their tag
        SortedSet<Integer> tags = new TreeSet<>(dynActions.values());

        // Retrieve the highest menu level
        int maxLevel = 0;

        for (Integer level : levels.values()) {
            maxLevel = Math.max(maxLevel, level);
        }

        // Initially update all the action items
        for (DynAction action : dynActions.keySet()) {
            action.update();
        }

        // Generate the hierarchy of menus
        SeparableMenu prevMenu = menu;

        for (int level = 0; level <= maxLevel; level++) {
            SeparableMenu currentMenu = (level == 0) ? menu
                    : new SeparableMenu("Continued ...");

            for (Integer tag : tags) {
                for (Entry<DynAction, Integer> entry : dynActions.entrySet()) {
                    if (entry.getValue()
                            .equals(tag)) {
                        DynAction action = entry.getKey();

                        if (levels.get(action) == level) {
                            currentMenu.add(action.getMenuItem());
                        }
                    }
                }

                currentMenu.addSeparator();
            }

            currentMenu.trimSeparator();

            if ((level > 0) && (currentMenu.getMenuComponentCount() > 0)) {
                // Insert this menu as a submenu of the previous one
                prevMenu.addSeparator();
                prevMenu.add(currentMenu);
                prevMenu = currentMenu;
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------------//
    // AssignListener //
    //----------------//
    /**
     * A standard listener used in all shape assignment menus.
     */
    protected class AssignListener
            implements ActionListener
    {
        //~ Instance fields ----------------------------------------------------

        private final boolean compound;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates the AssignListener, with the compound flag.
         *
         * @param compound true if we assign a compound, false otherwise
         */
        public AssignListener (boolean compound)
        {
            this.compound = compound;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            JMenuItem source = (JMenuItem) e.getSource();
            controller.asyncAssignGlyphs(
                    nest.getSelectedGlyphSet(),
                    Shape.valueOf(source.getText()),
                    compound);
        }
    }

    //----------------//
    // CompoundAction //
    //----------------//
    /**
     * Build a compound and assign the shape selected in the menu.
     */
    protected class CompoundAction
            extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public CompoundAction ()
        {
            super(30);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
        }

        @Override
        public JMenuItem getMenuItem ()
        {
            JMenu menu = new JMenu(this);
            ShapeSet.addAllShapes(menu, new AssignListener(true));

            return menu;
        }

        @Override
        public void update ()
        {
            if ((glyphNb > 1) && noVirtuals) {
                setEnabled(true);
                putValue(NAME, "Build compound as ...");
                putValue(SHORT_DESCRIPTION, "Manually build a compound");
            } else {
                setEnabled(false);
                putValue(NAME, "No compound");
                putValue(SHORT_DESCRIPTION, "No glyphs for a compound");
            }
        }
    }

    //------------//
    // CopyAction //
    //------------//
    /**
     * Copy the shape of the selected glyph shape (in order to replicate
     * the assignment to another glyph later).
     */
    protected class CopyAction
            extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public CopyAction ()
        {
            super(10);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            Glyph glyph = nest.getSelectedGlyph();

            if (glyph != null) {
                Shape shape = glyph.getShape();

                if (shape != null) {
                    controller.setLatestShapeAssigned(shape);
                }
            }
        }

        @Override
        public void update ()
        {
            Glyph glyph = nest.getSelectedGlyph();

            if (glyph != null) {
                Shape shape = glyph.getShape();

                if (shape != null) {
                    setEnabled(true);
                    putValue(NAME, "Copy " + shape);
                    putValue(SHORT_DESCRIPTION, "Copy this shape");

                    return;
                }
            }

            setEnabled(false);
            putValue(NAME, "Copy");
            putValue(SHORT_DESCRIPTION, "No shape to copy");
        }
    }

    //------------//
    // DumpAction //
    //------------//
    /**
     * Dump each glyph in the selected collection of glyphs.
     */
    protected class DumpAction
            extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public DumpAction ()
        {
            super(40);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            for (Glyph glyph : nest.getSelectedGlyphSet()) {
                logger.info(glyph.dumpOf());
            }
        }

        @Override
        public void update ()
        {
            if (glyphNb > 0) {
                setEnabled(true);

                StringBuilder sb = new StringBuilder();
                sb.append("Dump ")
                        .append(glyphNb)
                        .append(" glyph");

                if (glyphNb > 1) {
                    sb.append("s");
                }

                putValue(NAME, sb.toString());
                putValue(SHORT_DESCRIPTION, "Dump selected glyphs");
            } else {
                setEnabled(false);
                putValue(NAME, "Dump");
                putValue(SHORT_DESCRIPTION, "No glyph to dump");
            }
        }
    }

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
        //~ Instance fields ----------------------------------------------------

        /** Semantic tag */
        protected final int tag;

        //~ Constructors -------------------------------------------------------
        public DynAction (int tag)
        {
            this.tag = tag;
        }

        //~ Methods ------------------------------------------------------------
        /**
         * Method to update the action according to the current context
         */
        public abstract void update ();

        /**
         * Report which item class should be used to the related menu item
         *
         * @return the precise menu item class
         */
        public JMenuItem getMenuItem ()
        {
            return new JMenuItem(this);
        }
    }

    //--------------//
    // AssignAction //
    //--------------//
    /**
     * Assign to each glyph the shape selected in the menu.
     */
    protected class AssignAction
            extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public AssignAction ()
        {
            super(20);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
        }

        @Override
        public JMenuItem getMenuItem ()
        {
            JMenu menu = new JMenu(this);
            ShapeSet.addAllShapes(menu, new AssignListener(false));

            return menu;
        }

        @Override
        public void update ()
        {
            if ((glyphNb > 0) && noVirtuals) {
                setEnabled(true);

                if (glyphNb == 1) {
                    putValue(NAME, "Assign glyph as ...");
                } else {
                    putValue(NAME, "Assign each glyph as ...");
                }

                putValue(SHORT_DESCRIPTION, "Manually force an assignment");
            } else {
                setEnabled(false);
                putValue(NAME, "Assign glyph as ...");
                putValue(SHORT_DESCRIPTION, "No glyph to assign a shape to");
            }
        }
    }

    //----------------//
    // DeassignAction //
    //----------------//
    /**
     * Deassign each glyph in the selected collection of glyphs.
     */
    protected class DeassignAction
            extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public DeassignAction ()
        {
            super(20);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Remember which is the current selected glyph
            Glyph glyph = nest.getSelectedGlyph();

            // Actually deassign the whole set
            Set<Glyph> glyphs = nest.getSelectedGlyphSet();

            if (noVirtuals) {
                controller.asyncDeassignGlyphs(glyphs);

                // Update focus on current glyph, if reused in a compound
                if (glyph != null) {
                    Glyph newGlyph = glyph.getFirstSection()
                            .getGlyph();

                    if (glyph != newGlyph) {
                        nest.getGlyphService()
                                .publish(
                                new GlyphEvent(
                                this,
                                SelectionHint.GLYPH_INIT,
                                null,
                                newGlyph));
                    }
                }
            } else {
                controller.asyncDeleteVirtualGlyphs(glyphs);
            }
        }

        @Override
        public void update ()
        {
            if ((knownNb > 0) && (noVirtuals || (virtualNb == knownNb))) {
                setEnabled(true);

                StringBuilder sb = new StringBuilder();

                if (noVirtuals) {
                    sb.append("Deassign ");
                    sb.append(knownNb)
                            .append(" glyph");

                    if (knownNb > 1) {
                        sb.append("s");
                    }
                } else {
                    sb.append("Delete ");

                    if (virtualNb > 0) {
                        sb.append(virtualNb)
                                .append(" virtual glyph");

                        if (virtualNb > 1) {
                            sb.append("s");
                        }
                    }
                }

                if (stemNb > 0) {
                    sb.append(" w/ ")
                            .append(stemNb)
                            .append(" stem");

                    if (stemNb > 1) {
                        sb.append("s");
                    }
                }

                putValue(NAME, sb.toString());
                putValue(SHORT_DESCRIPTION, "Deassign selected glyphs");
            } else {
                setEnabled(false);
                putValue(NAME, "Deassign");
                putValue(SHORT_DESCRIPTION, "No glyph to deassign");
            }
        }
    }

    //-------------//
    // PasteAction //
    //-------------//
    /**
     * Paste the latest shape to the glyph(s) at end.
     */
    protected class PasteAction
            extends DynAction
    {
        //~ Static fields/initializers -----------------------------------------

        private static final String PREFIX = "Paste ";

        //~ Constructors -------------------------------------------------------
        public PasteAction ()
        {
            super(10);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            JMenuItem source = (JMenuItem) e.getSource();
            Shape shape = Shape.valueOf(
                    source.getText().substring(PREFIX.length()));
            Glyph glyph = nest.getSelectedGlyph();

            if (glyph != null) {
                controller.asyncAssignGlyphs(
                        Collections.singleton(glyph),
                        shape,
                        false);
            }
        }

        @Override
        public void update ()
        {
            Shape latest = controller.getLatestShapeAssigned();

            if ((glyphNb > 0) && (latest != null) && noVirtuals) {
                setEnabled(true);
                putValue(NAME, PREFIX + latest.toString());
                putValue(SHORT_DESCRIPTION, "Assign latest shape");
            } else {
                setEnabled(false);
                putValue(NAME, PREFIX);
                putValue(SHORT_DESCRIPTION, "No shape to assign again");
            }
        }
    }
}
