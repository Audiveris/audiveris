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

import omr.glyph.Evaluation;
import omr.glyph.GlyphNetwork;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.sheet.SystemInfo;

import omr.util.Predicate;

import java.util.List;

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

    /** Specific predicate to filter pre-forte shapes */
    private static final Predicate<Shape> fortePredicate = new Predicate<Shape>() {
        public boolean check (Shape shape)
        {
            return (shape == Shape.DYNAMICS_CHAR_M) ||
                   (shape == Shape.DYNAMICS_CHAR_R) ||
                   (shape == Shape.DYNAMICS_CHAR_S);
        }
    };


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
            if (forte.getShape() != Shape.DYNAMICS_F) {
                continue;
            }

            PixelRectangle box = forte.getContourBox();
            PixelRectangle leftBox = new PixelRectangle(
                box.x,
                box.y + (box.height / 3),
                box.width / 3,
                box.height / 3);
            forte.addAttachment("fl", leftBox);

            // Look for intersected glyph
            List<Glyph> glyphs = system.lookupIntersectedGlyphs(leftBox, forte);

            if (logger.isFineEnabled()) {
                logger.fine(
                    system.getLogPrefix() + " Forte#" + forte.getId() + " " +
                    Glyphs.toString(glyphs));
            }

            if (!glyphs.isEmpty()) {
                Glyph compound = system.buildTransientCompound(glyphs);
                system.computeGlyphFeatures(compound);

                // Check if a clef appears in the top evaluations
                Evaluation vote = GlyphNetwork.getInstance()
                                              .topVote(
                    compound,
                    constants.maxDoubt.getValue(),
                    system,
                    fortePredicate);

                if (vote != null) {
                    compound = system.addGlyph(compound);
                    compound.setShape(vote.shape, Evaluation.ALGORITHM);

                    if (logger.isFineEnabled()) {
                        logger.info("PreForte " + compound);
                    }
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
}
