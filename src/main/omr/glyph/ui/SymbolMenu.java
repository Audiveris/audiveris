//----------------------------------------------------------------------------//
//                                                                            //
//                            S y m b o l M e n u                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Evaluation;
import omr.glyph.Glyph;
import omr.glyph.GlyphEvaluator;
import omr.glyph.GlyphInspector;
import omr.glyph.Shape;

import java.awt.event.*;
import java.util.*;

/**
 * Class <code>SymbolMenu</code> defines the popup menu which is linked to the
 * current selection of either one or several glyphs
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SymbolMenu
    extends GlyphMenu
{
    //~ Instance fields --------------------------------------------------------

    // Links to partnering entities
    private final ShapeFocusBoard shapeFocus;

    /////private final SymbolsController symbolsController;
    private final GlyphEvaluator evaluator;

    // To handle proposed compound shape
    private Glyph proposedGlyph;
    private Shape proposedShape;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SymbolMenu //
    //------------//
    /**
     * Create the Symbol popup menu
     *
     * @param symbolsController the top companion
     * @param evaluator the glyph evaluator
     * @param shapeFocus the current shape focus
     */
    public SymbolMenu (final SymbolsController symbolsController,
                       GlyphEvaluator          evaluator,
                       ShapeFocusBoard         shapeFocus)
    {
        super(symbolsController);
        this.evaluator = evaluator;
        this.shapeFocus = shapeFocus;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // allocateActions //
    //-----------------//
    @Override
    protected void allocateActions ()
    {
        // Copy & Paste actions
        new PasteAction();
        new CopyAction();

        // Deassign selected glyph(s)
        new DeassignAction();

        // Manually assign a shape
        new AssignAction();

        // Build a compound, with menu for shape selection
        new CompoundAction();

        // Dump current glyph
        new DumpAction();

        // Segment the glyph into stems & leaves
        new StemSegmentAction();

        // Segment the glyph into short stems & leaves
        new ShortStemSegmentAction();

        // Build a compound, with proposed shape
        new ProposedAction();

        // Cleanup large slur glyphs
        new LargeSlurAction();

        // Dump current glyph text info
        new DumpTextAction();

        // Display score counterpart
        new TranslationAction();

        // Display all glyphs of the same shape
        new ShapeAction();

        // Display all glyphs similar to the curent glyph
        new SimilarAction();
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------//
    // DumpTextAction //
    //----------------//
    /**
     * Dump the text information of each glyph in the selected collection of
     * glyphs
     */
    private class DumpTextAction
        extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public DumpTextAction ()
        {
            super(40);
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            for (Glyph glyph : glyphLag.getSelectedGlyphSet()) {
                glyph.getTextInfo()
                     .dump();
            }
        }

        @Override
        public void update ()
        {
            if (glyphNb > 0) {
                setEnabled(true);

                StringBuilder sb = new StringBuilder();
                sb.append("Dump text of ")
                  .append(glyphNb)
                  .append(" glyph");

                if (glyphNb > 1) {
                    sb.append("s");
                }

                putValue(NAME, sb.toString());
                putValue(SHORT_DESCRIPTION, "Dump text of selected glyphs");
            } else {
                setEnabled(false);
                putValue(NAME, "Dump text");
                putValue(SHORT_DESCRIPTION, "No glyph to dump text");
            }
        }
    }

    //-----------------//
    // LargeSlurAction //
    //-----------------//
    /**
     * Cleanup a glyph with focus on its slur shape
     */
    private class LargeSlurAction
        extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public LargeSlurAction ()
        {
            super(60);
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = glyphLag.getSelectedGlyphSet();
            ((SymbolsController) controller).asyncFixLargeSlurs(glyphs);
        }

        public void update ()
        {
            putValue(NAME, "Cleanup large Slur");

            if (glyphNb > 0) {
                setEnabled(true);
                putValue(SHORT_DESCRIPTION, "Extract slur from large glyph");
            } else {
                setEnabled(false);
                putValue(SHORT_DESCRIPTION, "No slur to fix");
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
        //~ Constructors -------------------------------------------------------

        public ProposedAction ()
        {
            super(30);
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            Glyph glyph = glyphLag.getSelectedGlyph();

            if ((glyph != null) && (glyph == proposedGlyph)) {
                controller.asyncAssignGlyphs(
                    Collections.singleton(glyph),
                    proposedShape,
                    false);
            }
        }

        @Override
        public void update ()
        {
            // Proposed compound?
            Glyph glyph = glyphLag.getSelectedGlyph();

            if ((glyphNb > 0) && (glyph != null) && (glyph.getId() == 0)) {
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

    //-------------//
    // ShapeAction //
    //-------------//
    /**
     * Set the focus on all glyphs with the same shape
     */
    private class ShapeAction
        extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public ShapeAction ()
        {
            super(70);
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = glyphLag.getSelectedGlyphSet();

            if ((glyphs != null) && (glyphs.size() == 1)) {
                Glyph glyph = glyphs.iterator()
                                    .next();

                if (glyph.getShape() != null) {
                    shapeFocus.setCurrentShape(glyph.getShape());
                }
            }
        }

        public void update ()
        {
            Glyph glyph = glyphLag.getSelectedGlyph();

            if ((glyph != null) && (glyph.getShape() != null)) {
                setEnabled(true);
                putValue(NAME, "Show all " + glyph.getShape() + "'s");
                putValue(SHORT_DESCRIPTION, "Display all glyphs of this shape");
            } else {
                setEnabled(false);
                putValue(NAME, "Show all");
                putValue(SHORT_DESCRIPTION, "No shape defined");
            }
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
        //~ Constructors -------------------------------------------------------

        public ShortStemSegmentAction ()
        {
            super(50);
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = glyphLag.getSelectedGlyphSet();
            ((SymbolsController) controller).asyncSegment(glyphs, true); // isShort
        }

        @Override
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
     * Set the focus on all glyphs similar to the selected glyph
     */
    private class SimilarAction
        extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public SimilarAction ()
        {
            super(70);
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = glyphLag.getSelectedGlyphSet();

            if ((glyphs != null) && (glyphs.size() == 1)) {
                Glyph glyph = glyphs.iterator()
                                    .next();

                if (glyph != null) {
                    shapeFocus.setSimilarGlyph(glyph);
                }
            }
        }

        public void update ()
        {
            Glyph glyph = glyphLag.getSelectedGlyph();

            if (glyph != null) {
                setEnabled(true);
                putValue(NAME, "Show similar glyphs");
                putValue(
                    SHORT_DESCRIPTION,
                    "Display all glyphs similar to this one");
            } else {
                setEnabled(false);
                putValue(NAME, "Show similar");
                putValue(SHORT_DESCRIPTION, "No glyph selected");
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
        //~ Constructors -------------------------------------------------------

        public StemSegmentAction ()
        {
            super(50);
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = glyphLag.getSelectedGlyphSet();
            ((SymbolsController) controller).asyncSegment(glyphs, false); // isShort
        }

        @Override
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
        //~ Constructors -------------------------------------------------------

        public TranslationAction ()
        {
            super(40);
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = glyphLag.getSelectedGlyphSet();
            ((SymbolsController) controller).showTranslations(glyphs);
        }

        @Override
        public void update ()
        {
            if (glyphNb > 0) {
                for (Glyph glyph : glyphLag.getSelectedGlyphSet()) {
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
