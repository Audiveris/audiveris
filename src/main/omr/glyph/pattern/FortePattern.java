//----------------------------------------------------------------------------//
//                                                                            //
//                          F o r t e P a t t e r n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.CompoundBuilder;
import omr.glyph.Evaluation;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
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

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            FortePattern.class);

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
     *
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
                        Grades.forteMinGrade,
                        forteNeighbors));

                if (compound != null) {
                    nb++;
                }
            }
        }

        return nb;
    }

    //~ Inner Classes ----------------------------------------------------------
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

        public ForteAdapter (SystemInfo system,
                             double minGrade,
                             EnumSet<Shape> desiredShapes)
        {
            super(system, minGrade, desiredShapes);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public Rectangle computeReferenceBox ()
        {
            Rectangle rect = seed.getBounds();
            Rectangle leftBox = new Rectangle(
                    rect.x,
                    rect.y + (rect.height / 3),
                    rect.width / 3,
                    rect.height / 3);
            seed.addAttachment("fl", leftBox);

            return rect;
        }

        @Override
        public Evaluation getChosenEvaluation ()
        {
            return new Evaluation(chosenEvaluation.shape, Evaluation.ALGORITHM);
        }

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {
            return true;
        }
    }
}
