//----------------------------------------------------------------------------//
//                                                                            //
//                       L e f t O v e r P a t t e r n                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphEvaluator;
import omr.glyph.GlyphNetwork;
import omr.glyph.Grades;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Implement;

/**
 * Class {@code LeftOverPattern} processes the significant glyphs which have
 * been left over. It addresses glyphs of non-assigned shape with significant
 * weight, and assigns them the top 1 shape.
 *
 * @author Hervé Bitteur
 */
public class LeftOverPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        LeftOverPattern.class);

    //~ Constructors -----------------------------------------------------------

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

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        int                  successNb = 0;
        final double         minWeight = constants.minWeight.getValue();
        final GlyphEvaluator evaluator = GlyphNetwork.getInstance();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isKnown() ||
                glyph.isManualShape() ||
                (glyph.getNormalizedWeight() < minWeight)) {
                continue;
            }

            Evaluation vote = evaluator.vote(
                glyph,
                Grades.leftOverMinGrade,
                system);

            if (vote != null) {
                if (logger.isFineEnabled()) {
                    logger.info(
                        "LeftOver glyph#" + glyph.getId() + " Vote: " + vote);
                }

                glyph.setShape(vote.shape, vote.grade);
                successNb++;
            }
        }

        return successNb;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.AreaFraction minWeight = new Scale.AreaFraction(
            0.5,
            "Minimum normalized weight to be a left over glyph");
    }
}
