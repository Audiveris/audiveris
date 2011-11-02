//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h I n s p e c t o r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.ConstantSet;

import omr.glyph.CompoundBuilder.CompoundAdapter;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Implement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class <code>GlyphInspector</code> is at a System level, dedicated to the
 * inspection of retrieved glyphs, their recognition being usually based on
 * features used by a neural network evaluator.
 *
 * @author Hervé Bitteur
 */
public class GlyphInspector
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphInspector.class);

    //~ Instance fields --------------------------------------------------------

    /** Dedicated system */
    private final SystemInfo system;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // GlyphInspector //
    //----------------//
    /**
     * Create an GlyphInspector instance.
     * @param system the dedicated system
     */
    public GlyphInspector (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // evaluateGlyphs //
    //----------------//
    /**
     * All unassigned symbol glyphs of a given system, for which we can get
     * a positive vote from the evaluator, are assigned the voted shape.
     * @param minGrade the lower limit on grade to accept an evaluation
     */
    public void evaluateGlyphs (double minGrade)
    {
        GlyphEvaluator evaluator = GlyphNetwork.getInstance();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == null) {
                // Get vote
                Evaluation vote = evaluator.vote(glyph, minGrade, system);

                if ((vote != null) && !glyph.isShapeForbidden(vote.shape)) {
                    glyph.setShape(vote.shape, vote.grade);
                }
            }
        }
    }

    //---------------//
    // inspectGlyphs //
    //---------------//
    /**
     * Process the given system, by retrieving unassigned glyphs, evaluating
     * and assigning them if OK, or trying compounds otherwise.
     * @param minGrade the minimum acceptable grade for this processing
     */
    public void inspectGlyphs (double minGrade)
    {
        if (logger.isFineEnabled()) {
            logger.info("S#" + system.getId() + " inspectGlyphs start");
        }

        // For Symbols & Leaves
        system.retrieveGlyphs();
        system.removeInactiveGlyphs();
        evaluateGlyphs(minGrade);
        system.removeInactiveGlyphs();

        // For Compounds
        retrieveCompounds(minGrade);
        system.removeInactiveGlyphs();
        evaluateGlyphs(minGrade);
        system.removeInactiveGlyphs();
    }

    //-------------------//
    // retrieveCompounds //
    //-------------------//
    /**
     * In the specified system, look for glyphs portions that should be
     * considered as parts of compound glyphs.
     * @param minGrade minimum acceptable grade
     */
    private void retrieveCompounds (double minGrade)
    {
        // Sort suitable glyphs by decreasing weight
        List<Glyph> glyphs = new ArrayList<Glyph>(system.getGlyphs());
        Collections.sort(glyphs, Glyph.reverseWeightComparator);

        for (int index = 0; index < glyphs.size(); index++) {
            Glyph        seed = glyphs.get(index);

            // Now process this seed, by looking at smaller ones
            // Do not cross a stem if any is found
            BasicAdapter adapter = new BasicAdapter(system, minGrade, seed);

            if (adapter.isCandidateSuitable(seed)) {
                system.buildCompound(
                    seed,
                    true,
                    glyphs.subList(index + 1, glyphs.size()),
                    adapter);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // BasicAdapter //
    //--------------//
    /**
     * Class <code>BasicAdapter</code> is a CompoundAdapter meant to retrieve
     * all compounds (in a system).
     */
    private class BasicAdapter
        extends CompoundBuilder.AbstractAdapter
    {
        //~ Instance fields ----------------------------------------------------

        Glyph stem = null;
        int   stemX;
        int   stemToSeed;

        //~ Constructors -------------------------------------------------------

        /**
         * Construct a BasicAdapter around a given seed
         * @param system the containing system
         * @param minGrade minimum acceptable grade
         */
        public BasicAdapter (SystemInfo system,
                             double     minGrade,
                             Glyph      seed)
        {
            super(system, minGrade);

            if (seed.getStemNumber() > 0) {
                // Remember this stem as a border
                if (seed.getLeftStem() != null) {
                    stem = seed.getLeftStem();
                } else {
                    stem = seed.getRightStem();
                }

                stemX = stem.getCentroid().x;
                stemToSeed = seed.getCentroid().x - stemX;
            }
        }

        //~ Methods ------------------------------------------------------------

        @Implement(CompoundAdapter.class)
        public boolean isCandidateSuitable (Glyph glyph)
        {
            Shape   shape = glyph.getShape();
            boolean ok = glyph.isActive() && (shape != Shape.LEDGER) &&
                         (!glyph.isKnown() ||
                         (!glyph.isManualShape() &&
                         ((shape == Shape.DOT) || (shape == Shape.NOISE) ||
                         (shape == Shape.CLUTTER) ||
                         (shape == Shape.STRUCTURE) ||
                         (shape == Shape.VOID_NOTEHEAD) ||
                         (shape == Shape.VOID_NOTEHEAD_2) ||
                         (shape == Shape.VOID_NOTEHEAD_3) ||
                         (shape == Shape.STACCATISSIMO) ||
                         (glyph.getGrade() <= Grades.compoundPartMaxGrade))));

            if (!ok) {
                return false;
            }

            // Stay on same side of the stem if any
            if ((stem != null)) {
                ok = ((glyph.getCentroid().x - stemX) * stemToSeed) > 0;
            }

            return ok;
        }

        @Implement(CompoundAdapter.class)
        public boolean isCompoundValid (Glyph compound)
        {
            Evaluation eval = GlyphNetwork.getInstance()
                                          .vote(compound, minGrade, system);

            if ((eval != null) &&
                eval.shape.isWellKnown() &&
                (eval.shape != Shape.CLUTTER) &&
                (!seed.isKnown() || (eval.grade > seed.getGrade()))) {
                chosenEvaluation = eval;

                return true;
            } else {
                return false;
            }
        }

        @Implement(CompoundAdapter.class)
        public PixelRectangle getReferenceBox ()
        {
            if (seed == null) {
                throw new NullPointerException(
                    "Compound seed has not been set");
            }

            PixelRectangle box = seed.getContourBox();
            Scale          scale = system.getScoreSystem()
                                         .getScale();
            int            boxWiden = scale.toPixels(
                GlyphInspector.constants.boxWiden);
            box.grow(boxWiden, boxWiden);

            return box;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction boxWiden = new Scale.Fraction(
            0.25,
            "Box widening to check intersection with compound");
    }
}
