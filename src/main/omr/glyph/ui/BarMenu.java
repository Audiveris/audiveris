//----------------------------------------------------------------------------//
//                                                                            //
//                               B a r M e n u                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphsModel;
import omr.glyph.Shape;
import static omr.script.ScriptRecording.*;

import omr.selection.GlyphEvent;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;

import omr.util.Implement;
import static omr.util.Synchronicity.*;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import static javax.swing.Action.*;

/**
 * Class <code>BarMenu</code> defines the popup menu to interact with bar lines
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class BarMenu
{
    //~ Instance fields --------------------------------------------------------

    // Links to partnering entities
    private final Sheet          sheet;
    private final GlyphsModel     glyphModel;

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
     * @param glyphModel the top companion
     * @param glyphLag the related glyph lag
     */
    public BarMenu (final Sheet      sheet,
                    final GlyphsModel glyphModel,
                    final GlyphLag   glyphLag)
    {
        this.sheet = sheet;
        this.glyphModel = glyphModel;
        this.glyphLag = glyphLag;

        popup = new JPopupMenu(); //------------------------------------------

        // Direct link to latest shape assigned
        popup.add(new JMenuItem(new IdemAction()));

        popup.addSeparator(); //----------------------------------------------

        // Deassign selected glyph(s)
        popup.add(new JMenuItem(new DeassignAction()));

        // Manually assign a shape
        JMenu assignMenu = new JMenu(new AssignAction());
        Shape.addRangeShapeItems(Shape.Barlines,
            assignMenu,
            new ActionListener() {
                    @Implement(ActionListener.class)
                    public void actionPerformed (final ActionEvent e)
                    {
                        JMenuItem source = (JMenuItem) e.getSource();
                        glyphModel.assignSetShape(
                            ASYNC,
                            glyphLag.getCurrentGlyphSet(),
                            Shape.valueOf(source.getText()),
                            false,
                            RECORDING);
                    }
                });
        popup.add(assignMenu);

        popup.addSeparator(); //----------------------------------------------

        // Build a compound, with menu for shape selection
        JMenu compoundMenu = new JMenu(new CompoundAction());
        Shape.addShapeItems(
            compoundMenu,
            new ActionListener() {
                    @Implement(ActionListener.class)
                    public void actionPerformed (ActionEvent e)
                    {
                        JMenuItem source = (JMenuItem) e.getSource();
                        glyphModel.assignSetShape(
                            ASYNC,
                            glyphLag.getCurrentGlyphSet(),
                            Shape.valueOf(source.getText()),
                            true,
                            RECORDING);
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
        Set<Glyph> glyphs = glyphLag.getCurrentGlyphSet();
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

            // Initially updateMenu the action items
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
            Glyph      glyph = glyphLag.getCurrentGlyph();

            // Actually deassign the whole set
            Set<Glyph> glyphs = glyphLag.getCurrentGlyphSet();
            glyphModel.deassignSetShape(ASYNC, glyphs, RECORDING);

            // Update focus on current glyph, if reused in a compound
            if (glyph != null) {
                Glyph newGlyph = glyph.getFirstSection()
                                      .getGlyph();

                if (glyph != newGlyph) {
                    glyphLag.publish(
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
            for (Glyph glyph : glyphLag.getCurrentGlyphSet()) {
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

    //------------//
    // IdemAction //
    //------------//
    /**
     * Assign the same latest shape to the glyph(s) at end
     */
    private class IdemAction
        extends DynAction
    {
        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            JMenuItem source = (JMenuItem) e.getSource();
            Shape     shape = Shape.valueOf(source.getText());
            Glyph     glyph = glyphLag.getCurrentGlyph();

            if (glyph != null) {
                glyphModel.assignGlyphShape(ASYNC, glyph, shape, RECORDING);
            }
        }

        @Override
        public void update ()
        {
            Shape latest = glyphModel.getLatestShapeAssigned();

            if ((glyphNb > 0) && (latest != null)) {
                setEnabled(true);
                putValue(NAME, latest.toString());
                putValue(SHORT_DESCRIPTION, "Assign latest shape");
            } else {
                setEnabled(false);
                putValue(NAME, "Idem");
                putValue(SHORT_DESCRIPTION, "No shape to assign again");
            }
        }
    }
}
