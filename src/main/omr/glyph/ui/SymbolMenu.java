//----------------------------------------------------------------------------//
//                                                                            //
//                            S y m b o l M e n u                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Evaluation;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.ShapeEvaluator;
import omr.glyph.facets.Glyph;

import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Set;

/**
 * Class {@code SymbolMenu} defines the menu which is linked to
 * the current selection of one or several glyphs.
 *
 * @author Hervé Bitteur
 */
public class SymbolMenu
        extends GlyphMenu
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SymbolMenu.class);

    //~ Instance fields --------------------------------------------------------
    // Links to partnering entities
    private final ShapeFocusBoard shapeFocus;

    private final ShapeEvaluator evaluator;

    // To handle proposed compound shape
    private Glyph proposedGlyph;

    private Shape proposedShape;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // SymbolMenu //
    //------------//
    /**
     * Create the Symbol menu.
     *
     * @param symbolsController the top companion
     * @param evaluator         the glyph evaluator
     * @param shapeFocus        the current shape focus
     */
    public SymbolMenu (final SymbolsController symbolsController,
                       ShapeEvaluator evaluator,
                       ShapeFocusBoard shapeFocus)
    {
        super(symbolsController);
        this.evaluator = evaluator;
        this.shapeFocus = shapeFocus;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // registerActions //
    //-----------------//
    @Override
    protected void registerActions ()
    {
        // Copy & Paste actions
        register(0, new PasteAction());
        register(0, new CopyAction());

        // Deassign selected glyph(s)
        register(0, new DeassignAction());

        // Manually assign a shape
        register(0, new AssignAction());

        // Build a compound, with menu for shape selection
        register(0, new CompoundAction());

        // Segment the glyph into stems & leaves
        register(0, new StemSegmentAction());

        // Segment the glyph into short stems & leaves
        register(0, new ShortStemSegmentAction());

        // Build a compound, with proposed shape
        register(0, new ProposedAction());

        // Trim large slur glyphs
        register(0, new TrimSlurAction());

        // Dump current glyph
        register(0, new DumpAction());

        // Dump current glyph text info
        register(0, new DumpTextAction());

        // Display score counterpart
        register(0, new TranslationAction());

        // Display all glyphs of the same shape
        register(0, new ShapeAction());

        // Display all glyphs similar to the current glyph
        register(0, new SimilarAction());
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------------//
    // DumpTextAction //
    //----------------//
    /**
     * Dump the text information of each glyph in the selected
     * collection of glyphs.
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

    //----------------//
    // ProposedAction //
    //----------------//
    /**
     * Accept the proposed compound with its evaluated shape.
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
        @Override
        public void actionPerformed (ActionEvent e)
        {
            Glyph glyph = nest.getSelectedGlyph();

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
            Glyph glyph = nest.getSelectedGlyph();

            if ((glyphNb > 0) && (glyph != null) && (glyph.getId() == 0)) {
                SystemInfo system = sheet.getSystemOf(glyph);
                Evaluation vote = evaluator.vote(
                        glyph,
                        system,
                        Grades.symbolMinGrade);

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
     * Set the focus on all glyphs with the same shape.
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
        @Override
        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = nest.getSelectedGlyphSet();

            if ((glyphs != null) && (glyphs.size() == 1)) {
                Glyph glyph = glyphs.iterator()
                        .next();

                if (glyph.getShape() != null) {
                    shapeFocus.setCurrentShape(glyph.getShape());
                }
            }
        }

        @Override
        public void update ()
        {
            Glyph glyph = nest.getSelectedGlyph();

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
     * Perform a segmentation on the selected glyphs, into short stems
     * and leaves.
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
        @Override
        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = nest.getSelectedGlyphSet();
            ((SymbolsController) controller).asyncSegment(glyphs, true); // isShort
        }

        @Override
        public void update ()
        {
            putValue(NAME, "Look for short verticals");

            if (sheet.hasSystemBoundaries() && (glyphNb > 0) && noVirtuals) {
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
     * Set the focus on all glyphs similar to the selected glyph.
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
        @Override
        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = nest.getSelectedGlyphSet();

            if ((glyphs != null) && (glyphs.size() == 1)) {
                Glyph glyph = glyphs.iterator()
                        .next();

                if (glyph != null) {
                    shapeFocus.setSimilarGlyph(glyph);
                }
            }
        }

        @Override
        public void update ()
        {
            Glyph glyph = nest.getSelectedGlyph();

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
     * Perform a segmentation on the selected glyphs, into stems and
     * leaves.
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
        @Override
        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = nest.getSelectedGlyphSet();
            ((SymbolsController) controller).asyncSegment(glyphs, false); // isShort
        }

        @Override
        public void update ()
        {
            putValue(NAME, "Look for verticals");

            if (sheet.hasSystemBoundaries() && (glyphNb > 0) && noVirtuals) {
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
     * Display the score entity that translates this glyph.
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
        @Override
        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = nest.getSelectedGlyphSet();
            ((SymbolsController) controller).showTranslations(glyphs);
        }

        @Override
        public void update ()
        {
            if (glyphNb > 0) {
                for (Glyph glyph : nest.getSelectedGlyphSet()) {
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

    //----------------//
    // TrimSlurAction //
    //----------------//
    /**
     * Cleanup a glyph with focus on its slur shape.
     */
    private class TrimSlurAction
            extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public TrimSlurAction ()
        {
            super(60);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            Set<Glyph> glyphs = nest.getSelectedGlyphSet();
            ((SymbolsController) controller).asyncTrimSlurs(glyphs);
        }

        @Override
        public void update ()
        {
            putValue(NAME, "Trim slur");

            if (sheet.hasSystemBoundaries() && (glyphNb > 0) && noVirtuals) {
                setEnabled(true);
                putValue(SHORT_DESCRIPTION, "Extract slur from large glyph");
            } else {
                setEnabled(false);
                putValue(SHORT_DESCRIPTION, "No slur to fix");
            }
        }
    }
}
