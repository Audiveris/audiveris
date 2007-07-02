//----------------------------------------------------------------------------//
//                                                                            //
//                             G l y p h M e n u                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Evaluation;
import omr.glyph.Evaluator;
import omr.glyph.Glyph;
import omr.glyph.GlyphInspector;
import omr.glyph.Shape;

import omr.selection.Selection;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import static javax.swing.Action.*;

/**
 * Class <code>GlyphMenu</code> defines the popup menu which is linked to the
 * current selection of either one or several glyphs
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphMenu
{
    //~ Instance fields --------------------------------------------------------

    // Links to partnering entities
    private final Sheet           sheet;
    private final ShapeFocusBoard shapeFocus;
    private final SymbolsEditor   symbolsEditor;
    private final Evaluator       evaluator;

    /** Set of actions to update menu according to selected glyphs */
    private final Set<DynAction> dynActions = new HashSet<DynAction>();

    /** Concrete popup menu */
    private final JPopupMenu popup;

    /** Current selection of glyphs */
    private final Selection glyphSelection;

    /** Current selection of glyphs */
    private final Selection glyphSetSelection;

    // To customize UI items based on selection context
    private int   glyphNb;
    private int   knownNb;
    private int   stemNb;

    // To handle proposed compound shape
    private Glyph proposedGlyph;
    private Shape proposedShape;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // GlyphMenu //
    //-----------//
    /**
     * Create the popup menu
     *
     * @param sheet the related sheet
     * @param symbolsEditor the top companion
     * @param evaluator the glyph evaluator
     * @param shapeFocus the current shape focus
     * @param glyphSelection the currently selected glyph
     * @param glyphSetSelection the currently selected glyphs
     */
    public GlyphMenu (final Sheet         sheet,
                      final SymbolsEditor symbolsEditor,
                      Evaluator           evaluator,
                      ShapeFocusBoard     shapeFocus,
                      Selection           glyphSelection,
                      Selection           glyphSetSelection)
    {
        this.sheet = sheet;
        this.symbolsEditor = symbolsEditor;
        this.evaluator = evaluator;
        this.shapeFocus = shapeFocus;
        this.glyphSelection = glyphSelection;
        this.glyphSetSelection = glyphSetSelection;

        popup = new JPopupMenu(); //------------------------------------------

        // Direct link to latest shape assigned
        popup.add(new JMenuItem(new IdemAction()));

        popup.addSeparator(); //----------------------------------------------

        // Deassign selected glyph(s)
        popup.add(new JMenuItem(new DeassignAction()));

        // Manually assign a shape
        JMenu assignMenu = new JMenu(new AssignAction());
        Shape.addShapeItems(
            assignMenu,
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        JMenuItem         source = (JMenuItem) e.getSource();
                        Shape             shape = Shape.valueOf(
                            source.getText());
                        List<Glyph>       glyphs = getCurrentGlyphs();
                        Collection<Shape> shapes = Glyph.shapesOf(glyphs);
                        symbolsEditor.assignSetShape(
                            glyphs,
                            shape, /* compound => */
                            false);
                        sheet.updateLastSteps(glyphs, shapes);
                    }
                });
        popup.add(assignMenu);

        popup.addSeparator(); //----------------------------------------------

        // Segment the glyph into stems & leaves
        popup.add(new JMenuItem(new StemSegmentAction()));

        // Segment the glyph into short stems & leaves
        popup.add(new JMenuItem(new ShortStemSegmentAction()));

        popup.addSeparator(); //----------------------------------------------

        // Build a compound, with proposed shape
        popup.add(new JMenuItem(new ProposedAction()));

        // Build a compound, with menu for shape selection
        JMenu compoundMenu = new JMenu(new CompoundAction());
        Shape.addShapeItems(
            compoundMenu,
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        JMenuItem         source = (JMenuItem) e.getSource();
                        Shape             shape = Shape.valueOf(
                            source.getText());
                        List<Glyph>       glyphs = getCurrentGlyphs();
                        Collection<Shape> shapes = Glyph.shapesOf(glyphs);
                        shapes.add(shape);
                        symbolsEditor.assignSetShape(
                            glyphs,
                            shape, /* compound => */
                            true);
                        sheet.updateLastSteps(glyphs, shapes);
                    }
                });
        popup.add(compoundMenu);

        popup.addSeparator(); //----------------------------------------------

        // Dump current glyph
        popup.add(new JMenuItem(new DumpAction()));

        // Display score counterpart
        popup.add(new JMenuItem(new TranslationAction()));

        popup.addSeparator(); //----------------------------------------------

        // Display all glyphs of the same shape
        popup.add(new JMenuItem(new SimilarAction()));
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
        List<Glyph> glyphs = getCurrentGlyphs();
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

    //-----------------//
    // getCurrentGlyph //
    //-----------------//
    private Glyph getCurrentGlyph ()
    {
        return (Glyph) glyphSelection.getEntity(); // Compiler warning
    }

    //------------------//
    // getCurrentGlyphs //
    //------------------//
    private List<Glyph> getCurrentGlyphs ()
    {
        return new ArrayList<Glyph>(
            (List<Glyph>) glyphSetSelection.getEntity()); // Compiler warning
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
        public DynAction ()
        {
            // Record the instance
            dynActions.add(this);

            // Initially updateMenu the action items
            update();
        }

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
        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
        }

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
        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
        }

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
        public void actionPerformed (ActionEvent e)
        {
            // Remember which is the current selected glyph
            Glyph             glyph = getCurrentGlyph();

            // Actually deassign the whole set
            List<Glyph>       glyphs = getCurrentGlyphs();
            Collection<Shape> shapes = Glyph.shapesOf(glyphs);
            symbolsEditor.deassignSetShape(glyphs);
            sheet.updateLastSteps(glyphs, shapes);

            // Update focus on current glyph, if reused in a compound
            Glyph newGlyph = glyph.getFirstSection()
                                  .getGlyph();

            if (glyph != newGlyph) {
                glyphSelection.setEntity(newGlyph, SelectionHint.GLYPH_INIT);
            }
        }

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
        public void actionPerformed (ActionEvent e)
        {
            for (Glyph glyph : (List<Glyph>) glyphSetSelection.getEntity()) { // Compiler warning
                glyph.dump();
            }
        }

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
        public void actionPerformed (ActionEvent e)
        {
            JMenuItem         source = (JMenuItem) e.getSource();
            Shape             shape = Shape.valueOf(source.getText());
            Glyph             glyph = getCurrentGlyph();
            List<Glyph>       glyphs = getCurrentGlyphs();
            Collection<Shape> shapes = Glyph.shapesOf(glyphs);

            if (glyph != null) {
                symbolsEditor.assignGlyphShape(glyph, shape);
                sheet.updateLastSteps(glyphs, shapes);
            }
        }

        public void update ()
        {
            Shape latest = symbolsEditor.getLatestShapeAssigned();

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

    //----------------//
    // ProposedAction //
    //----------------//
    /**
     * Accept the proposed compound with its evaluated shape
     */
    private class ProposedAction
        extends DynAction
    {
        public void actionPerformed (ActionEvent e)
        {
            Glyph             glyph = getCurrentGlyph();
            List<Glyph>       glyphs = getCurrentGlyphs();
            Collection<Shape> shapes = Glyph.shapesOf(glyphs);

            if ((glyph != null) && (glyph == proposedGlyph)) {
                symbolsEditor.assignGlyphShape(glyph, proposedShape);
                sheet.updateLastSteps(glyphs, shapes);
            }
        }

        public void update ()
        {
            // Proposed compound?
            Glyph glyph = getCurrentGlyph();

            if ((glyphNb > 0) && (glyph.getId() == 0)) {
                Evaluation vote = evaluator.vote(
                    glyph,
                    GlyphInspector.getSymbolMaxDoubt());

                if (vote != null) {
                    proposedGlyph = glyph;
                    proposedShape = vote.shape;
                    setEnabled(true);
                    putValue(NAME, "Build compound as " + proposedShape);
                    putValue(SHORT_DESCRIPTION, "Accept the proposed compound");

                    return;
                }
            }

            // Nothing to propose
            proposedGlyph = null;
            proposedShape = null;
            setEnabled(false);
            putValue(NAME, "Build compound");
            putValue(SHORT_DESCRIPTION, "No proposed compound");
        }
    }

    //------------------------//
    // ShortStemSegmentAction //
    //------------------------//
    /**
     * Perform a segmentation on the selected glyphs, into short stems and leaves
     */
    private class ShortStemSegmentAction
        extends DynAction
    {
        public void actionPerformed (ActionEvent e)
        {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning
            symbolsEditor.stemSegment(glyphs, false); // normal
        }

        public void update ()
        {
            putValue(NAME, "Look for short verticals");

            if (glyphNb > 0) {
                setEnabled(true);
                putValue(SHORT_DESCRIPTION, "Extract short stems and leaves");
            } else {
                setEnabled(false);
                putValue(SHORT_DESCRIPTION, "No glyph to segment");
            }
        }
    }

    //---------------//
    // SimilarAction //
    //---------------//
    /**
     * Set the focus on all glyphs with the same shape
     */
    private class SimilarAction
        extends DynAction
    {
        public void actionPerformed (ActionEvent e)
        {
            List<Glyph> glyphs = getCurrentGlyphs();

            if ((glyphs != null) && (glyphs.size() == 1)) {
                Glyph glyph = glyphs.get(0);

                if (glyph.getShape() != null) {
                    shapeFocus.setCurrentShape(glyph.getShape());
                }
            }
        }

        public void update ()
        {
            Glyph glyph = getCurrentGlyph();

            if ((glyph != null) && (glyph.getShape() != null)) {
                setEnabled(true);
                putValue(NAME, "Show similar " + glyph.getShape() + "'s");
                putValue(SHORT_DESCRIPTION, "Display all similar glyphs");
            } else {
                setEnabled(false);
                putValue(NAME, "Show similar");
                putValue(SHORT_DESCRIPTION, "No shape defined");
            }
        }
    }

    //-------------------//
    // StemSegmentAction //
    //-------------------//
    /**
     * Perform a segmentation on the selected glyphs, into stems and leaves
     */
    private class StemSegmentAction
        extends DynAction
    {
        public void actionPerformed (ActionEvent e)
        {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning
            symbolsEditor.stemSegment(glyphs, true); // normal
        }

        public void update ()
        {
            putValue(NAME, "Look for verticals");

            if (glyphNb > 0) {
                setEnabled(true);
                putValue(SHORT_DESCRIPTION, "Extract stems and leaves");
            } else {
                setEnabled(false);
                putValue(SHORT_DESCRIPTION, "No glyph to segment");
            }
        }
    }

    //-------------------//
    // TranslationAction //
    //-------------------//
    /**
     * Display the score entity that translates this glyph
     */
    private class TranslationAction
        extends DynAction
    {
        public void actionPerformed (ActionEvent e)
        {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning
            symbolsEditor.showTranslations(glyphs);
        }

        public void update ()
        {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning

            if (glyphNb > 0) {
                for (Glyph glyph : glyphs) {
                    if (glyph.isTranslated()) {
                        setEnabled(true);

                        StringBuilder sb = new StringBuilder();
                        sb.append("Show translations");
                        putValue(NAME, sb.toString());
                        putValue(
                            SHORT_DESCRIPTION,
                            "Show translations related to the glyph(s)");

                        return;
                    }
                }
            }

            // No translation to show
            setEnabled(false);
            putValue(NAME, "Translations");
            putValue(SHORT_DESCRIPTION, "No translation");
        }
    }
}
