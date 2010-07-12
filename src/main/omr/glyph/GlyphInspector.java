//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h I n s p e c t o r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
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

import java.util.*;

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

    /** Related scale */
    private final Scale scale;

    /** Related lag */
    private final GlyphLag lag;

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
        scale = system.getSheet()
                      .getScale();
        lag = system.getSheet()
                    .getVerticalLag();
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getHookMaxDoubt //
    //-----------------//
    /**
     * Report the maximum doubt for a beam hook
     *
     *
     * @return maximum acceptable doubt value
     */
    public static double getHookMaxDoubt ()
    {
        return constants.hookMaxDoubt.getValue();
    }

    //-----------------//
    // getLeafMaxDoubt //
    //-----------------//
    /**
     * Report the maximum doubt for a leaf
     *
     *
     * @return maximum acceptable doubt value
     */
    public static double getLeafMaxDoubt ()
    {
        return constants.leafMaxDoubt.getValue();
    }

    //-------------------------//
    // getMinCompoundPartDoubt //
    //-------------------------//
    /**
     * Report the minimum doubt value to be considered as part of a compound
     * @return the doubt threshold for a compound part
     */
    public static double getMinCompoundPartDoubt ()
    {
        return constants.minCompoundPartDoubt.getValue();
    }

    //--------------------//
    // getPatternsMaxDoubt //
    //--------------------//
    /**
     * Report the maximum doubt for a cleanup
     *
     *
     * @return maximum acceptable doubt value
     */
    public static double getPatternsMaxDoubt ()
    {
        return constants.patternsMaxDoubt.getValue();
    }

    //-------------------//
    // getSymbolMaxDoubt //
    //-------------------//
    /**
     * Report the maximum doubt for a symbol
     *
     * @return maximum acceptable doubt value
     */
    public static double getSymbolMaxDoubt ()
    {
        return constants.symbolMaxDoubt.getValue();
    }

    //-----------------//
    // getTextMaxDoubt //
    //-----------------//
    /**
     * Report the maximum doubt for a text symbol
     *
     * @return maximum acceptable doubt value
     */
    public static double getTextMaxDoubt ()
    {
        return constants.textMaxDoubt.getValue();
    }

    //----------------//
    // evaluateGlyphs //
    //----------------//
    /**
     * All unassigned symbol glyphs of a given system, for which we can get
     * a positive vote from the evaluator, are assigned the voted shape.
     * @param maxDoubt the upper limit on doubt to accept an evaluation
     */
    public void evaluateGlyphs (double maxDoubt)
    {
        GlyphEvaluator evaluator = GlyphNetwork.getInstance();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == null) {
                // Get vote
                Evaluation vote = evaluator.vote(glyph, maxDoubt);

                if ((vote != null) && !glyph.isShapeForbidden(vote.shape)) {
                    glyph.setShape(vote.shape, vote.doubt);
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
     *
     * @param maxDoubt the maximum acceptable doubt for this processing
     */
    public void inspectGlyphs (double maxDoubt)
    {
        // For Symbols & Leaves
        system.retrieveGlyphs();
        system.removeInactiveGlyphs();
        evaluateGlyphs(maxDoubt);
        system.removeInactiveGlyphs();

        // For Compounds
        retrieveCompounds(maxDoubt);
        system.removeInactiveGlyphs();
        evaluateGlyphs(maxDoubt);
        system.removeInactiveGlyphs();
    }


    //-------------------//
    // retrieveCompounds //
    //-------------------//
    /**
     * In the specified system, look for glyphs portions that should be
     * considered as parts of compound glyphs
     */
    private void retrieveCompounds (double maxDoubt)
    {
        // Sort suitable glyphs by decreasing weight
        List<Glyph> glyphs = new ArrayList<Glyph>(system.getGlyphs());
        Collections.sort(glyphs, Glyph.reverseWeightComparator);

        // Now process each seed in turn, by looking at smaller ones
        BasicAdapter adapter = new BasicAdapter(system, maxDoubt);

        for (int index = 0; index < glyphs.size(); index++) {
            Glyph seed = glyphs.get(index);

            if (adapter.isCandidateSuitable(seed)) {
                Glyph compound = system.getCompoundBuilder()
                                       .buildCompound(
                    seed,
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
        //~ Constructors -------------------------------------------------------

        /**
         * Construct a BasicAdapter around a given seed
         * @param system the containing system
         * @param maxDoubt maximum acceptable doubt
         */
        public BasicAdapter (SystemInfo system,
                             double     maxDoubt)
        {
            super(system, maxDoubt);
        }

        //~ Methods ------------------------------------------------------------

        @Implement(CompoundAdapter.class)
        public boolean isCandidateSuitable (Glyph glyph)
        {
            return glyph.isActive() &&
                   (!glyph.isKnown() ||
                   (!glyph.isManualShape() &&
                   ((glyph.getShape() == Shape.DOT) ||
                   (glyph.getShape() == Shape.SLUR) ||
                   (glyph.getShape() == Shape.CLUTTER) ||
                   (glyph.getShape() == Shape.VOID_NOTEHEAD) ||
                   (glyph.getShape() == Shape.VOID_NOTEHEAD_2) ||
                   (glyph.getShape() == Shape.VOID_NOTEHEAD_3) ||
                   (glyph.getDoubt() >= GlyphInspector.getMinCompoundPartDoubt()))));
        }

        @Implement(CompoundAdapter.class)
        public boolean isCompoundValid (Glyph compound)
        {
            Evaluation eval = GlyphNetwork.getInstance()
                                          .vote(compound, maxDoubt);

            if ((eval != null) &&
                eval.shape.isWellKnown() &&
                (eval.shape != Shape.CLUTTER) &&
                (!seed.isKnown() || (eval.doubt < seed.getDoubt()))) {
                chosenEvaluation = eval;

                return true;
            } else {
                return false;
            }
        }

        @Implement(CompoundAdapter.class)
        public PixelRectangle getIntersectionBox ()
        {
            if (seed == null) {
                throw new NullPointerException(
                    "Compound seed has not been set");
            }

            PixelRectangle box = new PixelRectangle(seed.getContourBox());
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

        Scale.Fraction   boxWiden = new Scale.Fraction(
            0.3,
            "Box widening to check intersection with compound");
        Evaluation.Doubt patternsMaxDoubt = new Evaluation.Doubt(
            1.5,
            "Maximum doubt for pattern phase");
        Evaluation.Doubt leafMaxDoubt = new Evaluation.Doubt(
            1.3,
            "Maximum acceptance doubt for a leaf");
        Evaluation.Doubt symbolMaxDoubt = new Evaluation.Doubt(
            1.2,
            "Maximum doubt for a symbol");
        Evaluation.Doubt textMaxDoubt = new Evaluation.Doubt(
            10000.0,
            "Maximum doubt for a text symbol");
        Evaluation.Doubt minCompoundPartDoubt = new Evaluation.Doubt(
            1.5,
            "Minimum doubt for a suitable compound part");
        Evaluation.Doubt hookMaxDoubt = new Evaluation.Doubt(
            5d,
            "Maximum doubt for beam hook verification");
    }
}
