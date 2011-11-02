//----------------------------------------------------------------------------//
//                                                                            //
//                          F o r t e P a t t e r n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.CompoundBuilder;
import omr.glyph.Evaluation;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.sheet.SystemInfo;

import java.util.EnumSet;

/**
 * Class {@code FortePattern} uses easily recognized Forte signs ("f") to
 * check the glyph next to them of the left, which is harder to recognize.
 * It can only be "m" (mezzo), "r" (rinforzando) or "s" (sforzando).
 *
 * @author Hervé Bitteur
 */
public class FortePattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(FortePattern.class);

    /** Pre-forte shapes */
    public static final EnumSet<Shape> forteNeighbors = EnumSet.of(
        Shape.DYNAMICS_CHAR_M,
        Shape.DYNAMICS_CHAR_R,
        Shape.DYNAMICS_CHAR_S);

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // FortePattern //
    //--------------//
    /**
     * Creates a new FortePattern object.
     * @param system the system to process
     */
    public FortePattern (SystemInfo system)
    {
        super("Forte", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int nb = 0;

        for (Glyph forte : system.getGlyphs()) {
            // Focus on forte shaped glyphs
            if (forte.getShape() == Shape.DYNAMICS_F) {
                Glyph compound = system.buildCompound(
                    forte,
                    false,
                    system.getGlyphs(),
                    new ForteAdapter(
                        system,
                        constants.maxDoubt.getValue(),
                        forteNeighbors));

                if (compound != null) {
                    nb++;
                }
            }
        }

        return nb;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Evaluation.Doubt maxDoubt = new Evaluation.Doubt(
            300d,
            "Maximum doubt for glyph on left of forte");
    }

    //---------------//
    // ForteAdapter //
    //---------------//
    /**
     * Adapter to actively search a Forte-compatible entity near the Forte glyph
     */
    private final class ForteAdapter
        extends CompoundBuilder.TopShapeAdapter
    {
        //~ Constructors -------------------------------------------------------

        public ForteAdapter (SystemInfo     system,
                             double         maxDoubt,
                             EnumSet<Shape> desiredShapes)
        {
            super(system, maxDoubt, desiredShapes);
        }

        //~ Methods ------------------------------------------------------------

        public boolean isCandidateSuitable (Glyph glyph)
        {
            return true;
        }

        @Override
        public Evaluation getChosenEvaluation ()
        {
            return new Evaluation(chosenEvaluation.shape, Evaluation.ALGORITHM);
        }

        public PixelRectangle getReferenceBox ()
        {
            PixelRectangle box = seed.getContourBox();
            PixelRectangle leftBox = new PixelRectangle(
                box.x,
                box.y + (box.height / 3),
                box.width / 3,
                box.height / 3);
            seed.addAttachment("fl", leftBox);

            return box;
        }
    }
}
