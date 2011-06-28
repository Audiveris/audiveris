//----------------------------------------------------------------------------//
//                                                                            //
//                           T i m e P a t t e r n                            //
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
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;
import omr.score.entity.ScoreSystem;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Implement;
import omr.util.Predicate;

import java.util.Collection;

/**
 * Class {@code TimePattern} verifies the time signature glyphs
 *
 * @author Hervé Bitteur
 */
public class TimePattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TimePattern.class);

    /** Specific predicate to filter time shapes */
    private static final Predicate<Shape> timePredicate = new Predicate<Shape>() {
        public boolean check (Shape shape)
        {
            return ShapeRange.Times.contains(shape);
        }
    };


    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new TimePattern object.
     * @param system the containing system
     */
    public TimePattern (SystemInfo system)
    {
        super("Time", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    /**
     * Check that each staff begins with a time
     * @return the number of times rebuilt
     */
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        int successNb = 0;

        for (Glyph glyph : system.getGlyphs()) {
            if (!ShapeRange.Times.contains(glyph.getShape())) {
                continue;
            }

            // Retrieve environment (staff)
            final ScoreSystem scoreSystem = system.getScoreSystem();
            final Scale       scale = scoreSystem.getScale();
            final int         xOffset = scale.toPixels(constants.xOffset);
            final int         yOffset = scale.toPixels(constants.yOffset);

            // Define the core box to intersect time glyph(s)
            PixelRectangle pixCore = glyph.getContourBox();
            pixCore.grow(-xOffset, -yOffset);

            // Draw the time core box, for visual debug
            glyph.addAttachment("time", pixCore);

            // We must find a time out of these glyphs
            Collection<Glyph> glyphs = system.lookupIntersectedGlyphs(pixCore);

            if (checkTime(glyphs)) {
                successNb++;
            }
        }

        return successNb;
    }

    //-----------//
    // checkTime //
    //-----------//
    /**
     * Try to recognize a time glyph in the compound of the provided glyphs
     * @param glyphs the parts of a time candidate
     * @return true if successful
     */
    private boolean checkTime (Collection<Glyph> glyphs)
    {
        Glyphs.purgeManuals(glyphs);

        if (glyphs.isEmpty()) {
            return false;
        }

        Glyph compound = system.buildTransientCompound(glyphs);
        system.computeGlyphFeatures(compound);

        // Check if a time appears in the top evaluations
        final Evaluation vote = GlyphNetwork.getInstance()
                                            .topVote(
            compound,
            constants.timeMaxDoubt.getValue(),
            system,
            timePredicate);

        if (vote != null) {
            compound = system.addGlyph(compound);
            compound.setShape(vote.shape, Evaluation.ALGORITHM);

            if (logger.isFineEnabled()) {
                logger.fine(
                    vote.shape + " rebuilt as glyph#" + compound.getId());
            }

            return true;
        } else {
            return false;
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

        Scale.Fraction   xOffset = new Scale.Fraction(
            0.25d,
            "Core Time horizontal offset");
        Scale.Fraction   yOffset = new Scale.Fraction(
            0.25d,
            "Core Time vertical offset");
        Evaluation.Doubt timeMaxDoubt = new Evaluation.Doubt(
            300d,
            "Maximum doubt for time verification");
    }
}
