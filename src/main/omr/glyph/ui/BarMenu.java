//----------------------------------------------------------------------------//
//                                                                            //
//                               B a r M e n u                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;

import omr.selection.GlyphEvent;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;
import omr.sheet.SystemsBuilder.BarsController;

import omr.util.Implement;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

/**
 * Class <code>BarMenu</code> defines the popup menu to interact with barlines
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class BarMenu
{
    //~ Instance fields --------------------------------------------------------

    // Links to partnering entities
    private final Sheet          sheet;
    private final BarsController barsController;

    /** Set of actions to update menu according to selected glyphs */
    private final Set<DynAction> dynActions = new HashSet<DynAction>();

    /** Concrete popup menu */
    private final JPopupMenu popup;

    /** Related glyph lag */
    private final GlyphLag glyphLag;

    // To customize UI items based on selection context
    private int glyphNb;
    private int knownNb;
    private int stemNb;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // BarMenu //
    //---------//
    /**
     * Create the popup menu
     *
     * @param sheet the related sheet
     * @param barsController the top companion
     * @param glyphLag the related glyph lag
     */
    public BarMenu (final Sheet          sheet,
                    final BarsController barsController,
                    final GlyphLag       glyphLag)
    {
        this.sheet = sheet;
        this.barsController = barsController;
        this.glyphLag = glyphLag;

        popup = new JPopupMenu(); //------------------------------------------

        // Deassign selected glyph(s)
        popup.add(new JMenuItem(new DeassignAction()));

        // Manually assign a shape
        JMenu assignMenu = new JMenu(new AssignAction());
        ShapeRange.addRangeShapeItems(
            ShapeRange.Barlines,
            assignMenu,
            new ActionListener() {
                    @Implement(ActionListener.class)
                    public void actionPerformed (final ActionEvent e)
                    {
                        JMenuItem source = (JMenuItem) e.getSource();
                        barsController.asyncAssignGlyphs(
                            glyphLag.getSelectedGlyphSet(),
                            Shape.valueOf(source.getText()),
                            false);
                    }
                });
        popup.add(assignMenu);

        popup.addSeparator(); //----------------------------------------------

        // Build a compound, with menu for shape selection
        JMenu compoundMenu = new JMenu(new CompoundAction());
        ShapeRange.addRangeShapeItems(
            ShapeRange.Barlines,
            compoundMenu,
            new ActionListener() {
                    @Implement(ActionListener.class)
                    public void actionPerformed (ActionEvent e)
                    {
                        JMenuItem source = (JMenuItem) e.getSource();
                        barsController.asyncAssignGlyphs(
                            glyphLag.getSelectedGlyphSet(),
                            Shape.valueOf(source.getText()),
                            true);
                    }
                });
        popup.add(compoundMenu);

        popup.addSeparator(); //----------------------------------------------

        // Dump current glyph
        popup.add(new JMenuItem(new DumpAction()));
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

        for (Glyph glyph : glyphs) {
            if (glyph.isKnown()) {
                knownNb++;

                if (glyph.getShape() == Shape.COMBINING_STEM) {
                    stemNb++;
                }
            }
        }

        // Update all dynamic actions accordingly
        for (DynAction action : dynActions) {
            action.update();
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // DynAction //
    //-----------//
    /**
     * Base implementation, to register the dynamic actions that need to be
     * updated according to the current glyph selection context.
     */
    private abstract class DynAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public DynAction ()
        {
            // Record the instance
            dynActions.add(this);

            // Initially update the action items
            update();
        }

        //~ Methods ------------------------------------------------------------

        public abstract void update ();
    }

    //--------------//
    // AssignAction //
    //--------------//
    /**
     * Assign to each glyph the shape selected in the menu
     */
    private class AssignAction
        extends DynAction
    {
        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
        }

        @Override
        public void update ()
        {
            if (glyphNb > 0) {
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
    // CompoundAction //
    //----------------//
    /**
     * Build a compound and assign the shape selected in the menu
     */
    private class CompoundAction
        extends DynAction
    {
        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
        }

        @Override
        public void update ()
        {
            if (glyphNb > 1) {
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

    //----------------//
    // DeassignAction //
    //----------------//
    /**
     * Deassign each glyph in the selected collection of glyphs
     */
    private class DeassignAction
        extends DynAction
    {
        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            // Remember which is the current selected glyph
            Glyph      glyph = glyphLag.getSelectedGlyph();

            // Actually deassign the whole set
            Set<Glyph> glyphs = glyphLag.getSelectedGlyphSet();
            barsController.asyncDeassignGlyphs(glyphs);

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
            if (knownNb > 0) {
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
    private class DumpAction
        extends DynAction
    {
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
}
