//----------------------------------------------------------------------------//
//                                                                            //
//                           C l e f P a t t e r n                            //
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
import omr.score.common.SystemRectangle;
import omr.score.entity.Barline;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;

import omr.sheet.Scale;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import omr.util.Implement;
import omr.util.Predicate;
import omr.util.TreeNode;

import java.util.Collection;

/**
 * Class {@code ClefPattern} verifies all the initial clefs of a system
 *
 * @author Hervé Bitteur
 */
public class ClefPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ClefPattern.class);

    /** Specific predicate to filter clef shapes */
    private static final Predicate<Shape> clefShapePredicate = new Predicate<Shape>() {
        public boolean check (Shape shape)
        {
            return ShapeRange.Clefs.contains(shape);
        }
    };

    /** Specific predicate to filter clef glyphs */
    private static final Predicate<Glyph> clefGlyphPredicate = new Predicate<Glyph>() {
        public boolean check (Glyph glyph)
        {
            return glyph.isClef();
        }
    };


    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ClefPattern object.
     * @param system the containing system
     */
    public ClefPattern (SystemInfo system)
    {
        super("Clef", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    /**
     * Check that each staff begins with a clef
     * @return the number of clefs rebuilt
     */
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        int               successNb = 0;

        final ScoreSystem scoreSystem = system.getScoreSystem();
        final Scale       scale = scoreSystem.getScale();
        final int         clefWidth = scale.toPixels(constants.clefWidth);
        final int         xOffset = scale.toPixels(constants.xOffset);
        final int         yOffset = scale.toPixels(constants.yOffset);

        int               staffId = 0;

        for (StaffInfo staff : system.getStaves()) {
            staffId++;

            if (staffId == 3) {
                logger.warning("BINGO");
            }

            // Define the core box to intersect clef glyph(s)
            int            left = staff.getLeft();
            int            top = staff.getFirstLine()
                                      .yAt(left);
            PixelRectangle pixCore = new PixelRectangle(
                left,
                top,
                clefWidth,
                staff.getHeight());
            pixCore.grow(-xOffset, -yOffset);

            // Draw the clef core box, for visual debug
            SystemRectangle sysCore = scoreSystem.toSystemRectangle(pixCore);
            SystemPart      part = scoreSystem.getPartAt(sysCore.getCenter());
            Barline         barline = part.getStartingBarline();

            if (barline != null) {
                Glyph line = Glyphs.firstOf(
                    barline.getGlyphs(),
                    Barline.linePredicate);
                line.addAttachment("clef#" + staffId, pixCore);
            }

            // We must find a clef out of these glyphs
            Collection glyphs = system.lookupIntersectedGlyphs(pixCore);

            if (checkClef(glyphs)) {
                successNb++;
            }
        }

        return successNb;
    }

    //-----------//
    // checkClef //
    //-----------//
    /**
     * Try to recognize a clef in the compound of the provided glyphs
     * @param glyphs the parts of a clef candidate
     * @return true if successful
     */
    private boolean checkClef (Collection<Glyph> glyphs)
    {
        Glyphs.purgeManuals(glyphs);

        if (glyphs.isEmpty()) {
            return false;
        }

        Glyph compound = system.buildTransientCompound(glyphs);
        system.computeGlyphFeatures(compound);

        // Check if a clef appears in the top evaluations
        final Evaluation vote = GlyphNetwork.getInstance()
                                            .topRawVote(
            compound,
            constants.clefMaxDoubt.getValue(),
            clefShapePredicate);

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

        Scale.Fraction   clefWidth = new Scale.Fraction(4d, "Width of a clef");
        Scale.Fraction   xOffset = new Scale.Fraction(
            0.5d,
            "Clef horizontal offset since left bar");
        Scale.Fraction   yOffset = new Scale.Fraction(
            0.5d,
            "Clef vertical offset since left bar");
        Evaluation.Doubt clefMaxDoubt = new Evaluation.Doubt(
            300000d,
            "Maximum doubt for clef verification");
    }
}
