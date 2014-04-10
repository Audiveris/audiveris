//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 L e f t O v e r P a t t e r n                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphClassifier;
import omr.glyph.Grades;
import omr.glyph.ShapeEvaluator;
import omr.glyph.facets.Glyph;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code LeftOverPattern} processes the significant glyph instances which have
 * been left over.
 * <p>
 * It addresses glyphs of non-assigned shape with significant weight, and assigns them the top 1
 * shape.
 *
 * @author Hervé Bitteur
 */
public class LeftOverPattern
        extends GlyphPattern
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LeftOverPattern.class);

    //~ Constructors -------------------------------------------------------------------------------
    //-----------------//
    // LeftOverPattern //
    //-----------------//
    /**
     * Creates a new LeftOverPattern object.
     *
     * @param system the containing system
     */
    public LeftOverPattern (SystemInfo system)
    {
        super("LeftOver", system);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int successNb = 0;
        final double minWeight = constants.minWeight.getValue();
        final ShapeEvaluator evaluator = GlyphClassifier.getInstance();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isKnown()
                || glyph.isManualShape()
                || (glyph.getNormalizedWeight() < minWeight)) {
                continue;
            }

            Evaluation vote = evaluator.vote(glyph, system, Grades.leftOverMinGrade);

            if (vote != null) {
                glyph = system.registerGlyph(glyph);
                glyph.setEvaluation(vote);

                if (logger.isDebugEnabled() || glyph.isVip()) {
                    logger.info("LeftOver {} vote: {}", glyph.idString(), vote);
                }

                successNb++;
            }
        }

        return successNb;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Scale.AreaFraction minWeight = new Scale.AreaFraction(
                0.04,
                "Minimum normalized weight to be a left over glyph");
    }
}
