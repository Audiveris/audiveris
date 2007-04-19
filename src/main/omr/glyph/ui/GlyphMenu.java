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
import java.util.List;

import javax.swing.*;

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

    private final Sheet             sheet;
    private final CompoundAction    compoundAction = new CompoundAction();
    private final DeassignAction    deassignAction = new DeassignAction();
    private final StemSegmentAction stemSegmentAction = new StemSegmentAction();
    private final DumpAction        dumpAction = new DumpAction();
    private final JMenu             assignMenu;
    private final JMenu             compoundMenu;
    private final JMenuItem         compoundItem;
    private final JMenuItem         deassignItem;
    private final JMenuItem         stemSegmentItem;
    private final JMenuItem         dumpItem;
    private final JMenuItem         latestAssign;
    private final JMenuItem         similarItem;

    /** Concrete popup menu */
    private final JPopupMenu popup;

    /** Current selection of glyphs */
    private final Selection glyphSelection;
    private final Selection         glyphSetSelection;
    private final ShapeFocusBoard   shapeFocus;
    private final SimilarAction     similarAction = new SimilarAction();
    private final SymbolsEditor     symbolsEditor;
    private final Evaluator         evaluator;

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
        latestAssign = new JMenuItem("no shape", null);
        latestAssign.setToolTipText("Assign latest shape");
        latestAssign.addActionListener(
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        JMenuItem source = (JMenuItem) e.getSource();
                        Shape     shape = Shape.valueOf(source.getText());
                        symbolsEditor.assignSetShape(
                            getCurrentGlyphs(),
                            shape,
                            /* compound => */ false);
                        sheet.updateSteps();
                    }
                });
        popup.add(latestAssign);

        // Deassign selected glyph(s)
        deassignItem = popup.add(deassignAction);
        deassignItem.setToolTipText("Deassign selected glyph(s)");

        // Manually assign a shape
        assignMenu = new JMenu("Force to");

        Shape.addShapeItems(
            assignMenu,
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        JMenuItem source = (JMenuItem) e.getSource();
                        Shape     shape = Shape.valueOf(source.getText());
                        symbolsEditor.assignSetShape(
                            getCurrentGlyphs(),
                            shape,
                            /* compound => */ false);
                        sheet.updateSteps();
                    }
                });
        assignMenu.setToolTipText("Manually force an assignment");
        popup.add(assignMenu);

        popup.addSeparator(); //----------------------------------------------

        // Segment the glyph into stems & leaves
        stemSegmentItem = popup.add(stemSegmentAction);

        // Build a compound, with proposed shape
        compoundItem = popup.add(compoundAction);
        compoundItem.setToolTipText("Build compound as");

        // Build a compound, with menu for shape selection
        compoundMenu = new JMenu("Build compound");
        Shape.addShapeItems(
            compoundMenu,
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        JMenuItem source = (JMenuItem) e.getSource();
                        Shape     shape = Shape.valueOf(source.getText());
                        symbolsEditor.assignSetShape(
                            getCurrentGlyphs(),
                            shape,
                            /* compound => */ true);
                        sheet.updateSteps();
                    }
                });
        compoundMenu.setToolTipText("Manually build a compound");
        popup.add(compoundMenu);

        popup.addSeparator(); //----------------------------------------------

        // Dump current glyph
        dumpItem = popup.add(dumpAction);
        dumpItem.setToolTipText("Dump this glyph");

        popup.addSeparator(); //----------------------------------------------

        // Display all glyphs of the same shape
        similarItem = popup.add(similarAction);
        similarItem.setToolTipText("Display all similar glyphs");
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

    //----------------//
    // updateForGlyph //
    //----------------//
    /**
     * Update the popup menu when one glyph is selected as current
     *
     * @param glyph the current glyph
     */
    public void updateForGlyph (Glyph glyph)
    {
        // No compound for a single glyph
        compoundItem.setEnabled(false);
        compoundItem.setText("No compound");
        compoundMenu.setEnabled(false);

        assignMenu.setText("Force assignment to");
        updateLatestAssign();

        // Dump
        dumpItem.setText("Dump glyph");

        // Deassign
        deassignAction.setEnabled(glyph.isKnown());

        if (glyph.getShape() == Shape.COMBINING_STEM) {
            deassignItem.setText("Cancel this stem");
            deassignItem.setToolTipText("Remove this selected stem");
        } else {
            deassignItem.setText("Deassign");
            deassignItem.setToolTipText("Deassign selected glyph");
        }

        // Show similar
        if (glyph.getShape() == null) {
            similarAction.setEnabled(false);
            similarItem.setText("Show similar");
        } else {
            similarAction.setEnabled(true);
            similarItem.setText("Show similar " + glyph.getShape() + "'s");
        }
    }

    //-------------------//
    // updateForGlyphSet //
    //-------------------//
    /**
     * Update the popup menu when there are several glyphs selected (more than
     * one)
     *
     * @param glyphs the collection of current glyphs
     */
    public void updateForGlyphSet (List<Glyph> glyphs)
    {
        // Assign
        assignMenu.setText(
            "Assign each glyph as ...");
        updateLatestAssign();

        // Compound
        if (glyphs.size() > 1) {
            Glyph glyph = getCurrentGlyph();

            // Proposed compound?
            if (glyph.getId() == 0) {
                Evaluation vote = evaluator.vote(
                    glyph,
                    GlyphInspector.getSymbolMaxDoubt());

                if (vote != null) {
                    proposedGlyph = glyph;
                    proposedShape = vote.shape;
                    compoundItem.setEnabled(true);
                    compoundItem.setText("Build compound as " + proposedShape);
                } else {
                    proposedGlyph = null;
                    proposedShape = null;
                    compoundItem.setEnabled(false);
                    compoundItem.setText("No compound");
                }
            }

            compoundMenu.setEnabled(true);
            compoundMenu.setText("Build compound as ...");
        } else {
            compoundItem.setEnabled(false);
            compoundItem.setText("No compound");
            compoundMenu.setEnabled(false);
            compoundMenu.setText("No compound");
        }

        // Dump
        dumpItem.setText("Dump " + glyphs.size() + " glyphs");

        // Deassign, check what is to be deassigned
        int knownNb = 0;
        int stemNb = 0;

        for (Glyph glyph : (List<Glyph>) glyphSetSelection.getEntity()) { // Compiler warning

            if (glyph.isKnown()) {
                knownNb++;

                if (glyph.getShape() == Shape.COMBINING_STEM) {
                    stemNb++;
                }
            }
        }

        if (knownNb > 0) {
            deassignAction.setEnabled(true);
            deassignItem.setText(
                "Deassign " + knownNb + " glyphs" +
                ((stemNb > 0) ? (" w/ " + stemNb + " stems") : ""));
            deassignItem.setToolTipText("Deassign selected glyphs");
        } else {
            deassignAction.setEnabled(false);
            deassignItem.setText("Deassign");
            deassignItem.setToolTipText("No glyphs to deassign");
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
        return (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning
    }

    //--------------------//
    // updateLatestAssign //
    //--------------------//
    private void updateLatestAssign ()
    {
        if (symbolsEditor.getLatestShapeAssigned() != null) {
            latestAssign.setEnabled(true);
            latestAssign.setText(
                symbolsEditor.getLatestShapeAssigned().toString());
        } else {
            latestAssign.setEnabled(false);
            latestAssign.setText("no shape");
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------//
    // CompoundAction //
    //----------------//
    private class CompoundAction
        extends AbstractAction
    {
        public CompoundAction ()
        {
            super("Build compound");
        }

        public void actionPerformed (ActionEvent e)
        {
            Glyph glyph = getCurrentGlyph();

            if ((glyph != null) && (glyph == proposedGlyph)) {
                symbolsEditor.assignGlyphShape(glyph, proposedShape);
            }

            sheet.updateSteps();
        }
    }

    //----------------//
    // DeassignAction //
    //----------------//
    private class DeassignAction
        extends AbstractAction
    {
        public DeassignAction ()
        {
            super("Deassign");
        }

        public void actionPerformed (ActionEvent e)
        {
            Glyph glyph = getCurrentGlyph();
            symbolsEditor.deassignSetShape(getCurrentGlyphs());

            sheet.updateSteps();

            // Update focus on current glyph, if reused in a compound
            Glyph newGlyph = glyph.getFirstSection()
                                  .getGlyph();

            if (glyph != newGlyph) {
                glyphSelection.setEntity(newGlyph, SelectionHint.GLYPH_INIT);
            }
        }
    }

    //------------//
    // DumpAction //
    //------------//
    private class DumpAction
        extends AbstractAction
    {
        public DumpAction ()
        {
            super("Dump Glyph");
        }

        public void actionPerformed (ActionEvent e)
        {
            for (Glyph glyph : (List<Glyph>) glyphSetSelection.getEntity()) { // Compiler warning
                glyph.dump();
            }
        }
    }

    //---------------//
    // SimilarAction //
    //---------------//
    private class SimilarAction
        extends AbstractAction
    {
        public SimilarAction ()
        {
            super("Show Similars");
        }

        public void actionPerformed (ActionEvent e)
        {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning

            if ((glyphs != null) && (glyphs.size() == 1)) {
                Glyph glyph = glyphs.get(0);

                if (glyph.getShape() != null) {
                    shapeFocus.setCurrentShape(glyph.getShape());
                }
            }
        }
    }

    //-------------------//
    // StemSegmentAction //
    //-------------------//
    private class StemSegmentAction
        extends AbstractAction
    {
        public StemSegmentAction ()
        {
            super("Segment on Stems");
        }

        public void actionPerformed (ActionEvent e)
        {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning
            symbolsEditor.stemSegment(glyphs);
        }
    }
}
