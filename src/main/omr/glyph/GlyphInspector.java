//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h I n s p e c t o r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Class {@code GlyphInspector} is at a system level, dedicated to the
 * inspection of retrieved glyphs, their recognition being usually
 * based on features used by a shape evaluator.
 *
 * @author Hervé Bitteur
 */
public class GlyphInspector
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(GlyphInspector.class);

    /** Shapes acceptable for a part candidate */
    private static final EnumSet<Shape> partShapes = EnumSet.of(
            Shape.DOT_set,
            Shape.NOISE,
            Shape.CLUTTER,
            Shape.STACCATISSIMO,
            Shape.NOTEHEAD_VOID,
            Shape.FLAG_1,
            Shape.FLAG_1_UP);

    //~ Instance fields --------------------------------------------------------
    /** Dedicated system */
    private final SystemInfo system;

    //~ Constructors -----------------------------------------------------------
    //----------------//
    // GlyphInspector //
    //----------------//
    /**
     * Create an GlyphInspector instance.
     *
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
     * All unassigned symbol glyphs of a given system, for which we can
     * get a positive vote from the evaluator, are assigned the voted
     * shape.
     *
     * @param minGrade the lower limit on grade to accept an evaluation
     */
    public void evaluateGlyphs (double minGrade)
    {
        ShapeEvaluator evaluator = GlyphNetwork.getInstance();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == null) {
                // Get vote
                Evaluation vote = evaluator.vote(glyph, system, minGrade);

                if (vote != null) {
                    glyph.setEvaluation(vote);
                }
            }
        }
    }

    //---------------//
    // inspectGlyphs //
    //---------------//
    /**
     * Process the given system, by retrieving unassigned glyphs,
     * evaluating and assigning them if OK, or trying compounds
     * otherwise.
     *
     * @param minGrade the minimum acceptable grade for this processing
     * @param wide     flag for extra wide box
     */
    public void inspectGlyphs (double minGrade,
                               boolean wide)
    {
        logger.debug("S#{} inspectGlyphs start", system.getId());

        // For Symbols & Leaves
        system.retrieveGlyphs();
        system.removeInactiveGlyphs();
        evaluateGlyphs(minGrade);
        system.removeInactiveGlyphs();

        // For Compounds
        retrieveCompounds(minGrade, wide);
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
     *
     * @param minGrade minimum acceptable grade
     * @param wide     flag for extra wide box
     */
    private void retrieveCompounds (double minGrade,
                                    boolean wide)
    {
        // Use a copy to avoid concurrent modifications
        List<Glyph> glyphs = new ArrayList<>(system.getGlyphs());

        for (Glyph seed : glyphs) {
            // Now process this seed, by looking at neighbors
            BasicAdapter adapter = new BasicAdapter(system, minGrade, seed, wide);

            if (adapter.isCandidateSuitable(seed)) {
                system.buildCompound(seed, true, glyphs, adapter);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction boxMargin = new Scale.Fraction(
                0.25,
                "Box margin to check intersection with compound");

        Scale.Fraction boxWiden = new Scale.Fraction(
                0.5,
                "Box special abscissa margin to check intersection with compound");

    }

    //--------------//
    // BasicAdapter //
    //--------------//
    /**
     * Class {@code BasicAdapter} is a CompoundAdapter meant to
     * retrieve all compounds (in a system).
     */
    private static class BasicAdapter
            extends CompoundBuilder.AbstractAdapter
    {
        //~ Instance fields ----------------------------------------------------

        private Glyph stem = null;

        private int stemX;

        private int stemToSeed;

        private final boolean wide;

        //~ Constructors -------------------------------------------------------
        /**
         * Construct a BasicAdapter around a given seed
         *
         * @param system   the containing system
         * @param minGrade minimum acceptable grade
         */
        public BasicAdapter (SystemInfo system,
                             double minGrade,
                             Glyph seed,
                             boolean wide)
        {
            super(system, minGrade);
            this.wide = wide;

            stem = seed.getFirstStem();

            if (stem != null) {
                // Remember this stem as a border
                stemX = stem.getCentroid().x;
                stemToSeed = seed.getCentroid().x - stemX;
            }
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public Rectangle computeReferenceBox ()
        {
            if (seed == null) {
                throw new NullPointerException(
                        "Compound seed has not been set");
            }

            Rectangle newBox = seed.getBounds();

            Scale scale = system.getScoreSystem().getScale();
            int boxMargin = scale.toPixels(GlyphInspector.constants.boxMargin);
            int boxWiden = scale.toPixels(GlyphInspector.constants.boxWiden);

            if (wide) {
                newBox.grow(boxWiden, boxMargin);
            } else {
                newBox.grow(boxMargin, boxMargin);
            }

            return newBox;
        }

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            if (!glyph.isActive() || (shape == Shape.LEDGER)) {
                return false;
            }

            if (glyph.isKnown()
                && (glyph.isManualShape()
                    || (!partShapes.contains(shape)
                        && (glyph.getGrade() > Grades.compoundPartMaxGrade)))) {
                return false;
            }

            // Stay on same side of the stem if any
            if ((stem != null)) {
                return ((glyph.getCentroid().x - stemX) * stemToSeed) > 0;
            } else {
                return true;
            }
        }

        @Override
        public boolean isCompoundValid (Glyph compound)
        {
            Evaluation eval = GlyphNetwork.getInstance().vote(compound, system,
                    minGrade);

            if ((eval != null)
                && eval.shape.isWellKnown()
                && (eval.shape != Shape.CLUTTER)
                && (!seed.isKnown() || (eval.grade > seed.getGrade()))) {
                chosenEvaluation = eval;

                return true;
            } else {
                return false;
            }
        }
    }
}
