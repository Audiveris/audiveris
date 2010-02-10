//----------------------------------------------------------------------------//
//                                                                            //
//                             G l y p h M e n u                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.GlyphLag;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.selection.GlyphEvent;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;

import omr.ui.util.SeparablePopupMenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.*;

/**
 * Class <code>GlyphMenu</code> is the base for all glyph-based popup menus
 * such as {@link DashMenu}, {@link BarMenu} and {@link SymbolMenu}.
 * It also provides implementation for basic actions: copy, paste, assign,
 * compound, deassign and dump.
 *
 * @author Hervé Bitteur
 */
public abstract class GlyphMenu
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphMenu.class);

    //~ Instance fields --------------------------------------------------------

    /** The controller in charge of user gesture */
    protected final GlyphsController controller;

    /** Related sheet */
    protected final Sheet sheet;

    /** Related glyph lag */
    protected final GlyphLag glyphLag;

    /** Map of actions/tag to update menu according to selected glyphs */
    protected final Map<DynAction, Integer> dynActions = new LinkedHashMap<DynAction, Integer>();

    /** Concrete popup menu */
    protected final SeparablePopupMenu popup = new SeparablePopupMenu();

    /** Current number of selected glyphs */
    protected int glyphNb;

    /** Current number of known glyphs */
    protected int knownNb;

    /** Current number of stems */
    protected int stemNb;

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
        this.sheet = controller.sheet;

        this.glyphLag = controller.getLag();

        buildMenu();
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getPopup //
    //----------//
    /**
     * Report the concrete popup menu
     *
     * @return the popup menu
     */
    public JPopupMenu getPopup ()
    {
        return popup;
    }

    //------------//
    // updateMenu //
    //------------//
    /**
     * Update the popup menu according to the currently selected glyphs
     */
    public void updateMenu ()
    {
        // Analyze the context
        Set<Glyph> glyphs = glyphLag.getSelectedGlyphSet();
        glyphNb = glyphs.size();
        knownNb = 0;
        stemNb = 0;
        noVirtuals = true;

        for (Glyph glyph : glyphs) {
            if (glyph.isKnown()) {
                knownNb++;

                if (glyph.getShape() == Shape.COMBINING_STEM) {
                    stemNb++;
                }
            }

            if (glyph.isVirtual()) {
                noVirtuals = false;
            }
        }

        // Update all dynamic actions accordingly
        for (DynAction action : dynActions.keySet()) {
            action.update();
        }
    }

    //-----------------//
    // allocateActions //
    //-----------------//
    /**
     * Allocate all actions to be used in the menu
     */
    protected abstract void allocateActions ();

    //-----------//
    // buildMenu //
    //-----------//
    /**
     * Build the popup menu instance, grouping the actions with the same tag
     * and separating them from other tags.
     */
    private void buildMenu ()
    {
        // Allocate and register actions
        allocateActions();

        // Sort actions on their tag
        SortedSet<Integer> tags = new TreeSet<Integer>(dynActions.values());

        for (Integer tag : tags) {
            for (Entry<DynAction, Integer> entry : dynActions.entrySet()) {
                if (entry.getValue()
                         .equals(tag)) {
                    popup.add(entry.getKey().getMenuItem());
                }
            }

            popup.addSeparator();
        }

        popup.purgeSeparator();
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------//
    // AssignListener //
    //----------------//
    /**
     * A standard listener used in all shape assignment menus
     */
    protected class AssignListener
        implements ActionListener
    {
        //~ Instance fields ----------------------------------------------------

        private final boolean compound;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates the AssignListener, with the compound flag
         * @param compound true if we assign a compound, false otherwise
         */
        public AssignListener (boolean compound)
        {
            this.compound = compound;
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            JMenuItem source = (JMenuItem) e.getSource();
            controller.asyncAssignGlyphs(
                glyphLag.getSelectedGlyphSet(),
                Shape.valueOf(source.getText()),
                compound);
        }
    }

    //----------------//
    // CompoundAction //
    //----------------//
    /**
     * Build a compound and assign the shape selected in the menu
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
        public JMenuItem getMenuItem ()
        {
            JMenu menu = new JMenu(this);
            ShapeRange.addShapeItems(menu, new AssignListener(true));

            return menu;
        }

        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
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
     * Copy the shape of the selected glyph shape (in order to replicate the
     * assignment to another glyph later)
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

        public void actionPerformed (ActionEvent e)
        {
            Glyph glyph = glyphLag.getSelectedGlyph();

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
            Glyph glyph = glyphLag.getSelectedGlyph();

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

    //-----------//
    // DynAction //
    //-----------//
    /**
     * Base implementation, to register the dynamic actions that need to be
     * updated according to the current glyph selection context.
     */
    protected abstract class DynAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public DynAction (int tag)
        {
            // Record the instance
            dynActions.put(this, tag);

            // Initially update the action items
            update();
        }

        //~ Methods ------------------------------------------------------------

        public abstract void update ();

        public JMenuItem getMenuItem ()
        {
            return new JMenuItem(this);
        }
    }

    //--------------//
    // AssignAction //
    //--------------//
    /**
     * Assign to each glyph the shape selected in the menu
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
        public JMenuItem getMenuItem ()
        {
            JMenu menu = new JMenu(this);
            ShapeRange.addShapeItems(menu, new AssignListener(false));

            return menu;
        }

        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
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
     * Deassign each glyph in the selected collection of glyphs
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

        public void actionPerformed (ActionEvent e)
        {
            // Remember which is the current selected glyph
            Glyph      glyph = glyphLag.getSelectedGlyph();

            // Actually deassign the whole set
            Set<Glyph> glyphs = glyphLag.getSelectedGlyphSet();
            controller.asyncDeassignGlyphs(glyphs);

            // Update focus on current glyph, if reused in a compound
            if (glyph != null) {
                Glyph newGlyph = glyph.getFirstSection()
                                      .getGlyph();

                if (glyph != newGlyph) {
                    glyphLag.getSelectionService()
                            .publish(
                        new GlyphEvent(
                            this,
                            SelectionHint.GLYPH_INIT,
                            null,
                            newGlyph));
                }
            }
        }

        @Override
        public void update ()
        {
            if ((knownNb > 0) && noVirtuals) {
                setEnabled(true);

                StringBuilder sb = new StringBuilder();
                sb.append("Deassign ")
                  .append(knownNb)
                  .append(" glyph");

                if (knownNb > 1) {
                    sb.append("s");
                }

                if (stemNb > 0) {
                    sb.append(" w/ ")
                      .append(stemNb)
                      .append(" stem");
                }

                if (stemNb > 1) {
                    sb.append("s");
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

    //------------//
    // DumpAction //
    //------------//
    /**
     * Dump each glyph in the selected collection of glyphs
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

        public void actionPerformed (ActionEvent e)
        {
            for (Glyph glyph : glyphLag.getSelectedGlyphSet()) {
                glyph.dump();
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

    //-------------//
    // PasteAction //
    //-------------//
    /**
     * Paste the latest shape to the glyph(s) at end
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

        public void actionPerformed (ActionEvent e)
        {
            JMenuItem source = (JMenuItem) e.getSource();
            Shape     shape = Shape.valueOf(
                source.getText().substring(PREFIX.length()));
            Glyph     glyph = glyphLag.getSelectedGlyph();

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
