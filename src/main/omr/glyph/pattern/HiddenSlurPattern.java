//----------------------------------------------------------------------------//
//                                                                            //
//                     H i d d e n S l u r P a t t e r n                      //
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
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.log.Logger;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Implement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code HiddenSlurPattern} processes the significant glyphs which have
 * not been assigned a shape, looking for a slur in one of their section
 *
 * @author Hervé Bitteur
 */
public class HiddenSlurPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        HiddenSlurPattern.class);

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // HiddenSlurPattern //
    //-------------------//
    /**
     * Creates a new HiddenSlurPattern object.
     *
     * @param system the containing system
     */
    public HiddenSlurPattern (SystemInfo system)
    {
        super("HiddenSlur", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        int                  successNb = 0;
        final double         minGlyphWeight = constants.minGlyphWeight.getValue();
        final int            minSectionWeight = scale.toPixels(
            constants.minSectionWeight);
        final GlyphEvaluator evaluator = GlyphNetwork.getInstance();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isKnown() ||
                glyph.isManualShape() ||
                (glyph.getNormalizedWeight() < minGlyphWeight)) {
                continue;
            }

            List<Section> sections = new ArrayList<Section>(glyph.getMembers());
            Collections.sort(sections, Section.reverseWeightComparator);

            for (Section section : sections) {
                if (section.getWeight() < minSectionWeight) {
                    break;
                }

                Glyph      compound = system.buildTransientGlyph(
                    Collections.singleton(section));
                Evaluation vote = evaluator.vote(
                    compound,
                    Grades.slurMinGrade,
                    system);

                if ((vote != null) && (vote.shape == Shape.SLUR)) {
                    compound.setEvaluation(vote);
                    compound = system.addGlyph(compound);

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "SLUR extracted as glyph#" + compound.getId());
                    }

                    successNb++;

                    break;
                }
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

        Scale.AreaFraction minGlyphWeight = new Scale.AreaFraction(
            0.5,
            "Minimum normalized glyph weight to lookup a slur section");
        Scale.AreaFraction minSectionWeight = new Scale.AreaFraction(
            0.4,
            "Minimum normalized section weight to check for a slur shape");
    }
}
