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
import omr.score.common.PixelRectangle;
import omr.score.entity.Barline;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;

import omr.sheet.Scale;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import omr.util.Implement;
import omr.util.Predicate;

import java.util.*;

/**
 * Class {@code ClefPattern} verifies all the initial clefs of a system, using
 * an intersection inner rectangle and a containing outer rectangle to retrieve
 * the clef glyphs and only those.
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
    private static final Predicate<Shape> clefPredicate = new Predicate<Shape>() {
        public boolean check (Shape shape)
        {
            return ShapeRange.Clefs.contains(shape);
        }
    };


    //~ Instance fields --------------------------------------------------------

    private int clefWidth;
    private int xOffset;
    private int yOffset;
    private int xMargin;
    private int yMargin;

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
        int         successNb = 0;

        ScoreSystem scoreSystem = system.getScoreSystem();
        Scale       scale = scoreSystem.getScale();
        clefWidth = scale.toPixels(constants.clefWidth);
        xOffset = scale.toPixels(constants.xOffset);
        yOffset = scale.toPixels(constants.yOffset);
        xMargin = scale.toPixels(constants.xMargin);
        yMargin = scale.toPixels(constants.yMargin);

        int staffId = 0;

        for (StaffInfo staff : system.getStaves()) {
            staffId++;

            int            left = staff.getLeft();
            PixelRectangle pix = new PixelRectangle(
                left + (2 * xOffset) + (clefWidth / 2),
                staff.getFirstLine().yAt(left) + (staff.getHeight() / 2),
                0,
                0);

            // Define the inner box to intersect clef glyph(s)
            PixelRectangle pixInner = new PixelRectangle(pix);
            pixInner.grow(
                (clefWidth / 2) - xOffset,
                (staff.getHeight() / 2) - yOffset);

            // Draw the box, for visual debug
            SystemPart      part = scoreSystem.getPartAt(pixInner.getCenter());
            Barline         barline = part.getStartingBarline();
            Glyph           line = null;

            if (barline != null) {
                line = Glyphs.firstOf(
                    barline.getGlyphs(),
                    Barline.linePredicate);

                if (line != null) {
                    line.addAttachment("clefInner#" + staffId, pixInner);
                }
            }

            // We must find a clef out of these glyphs
            Collection<Glyph> glyphs = system.lookupIntersectedGlyphs(pixInner);

            if (logger.isFineEnabled()) {
                logger.fine(staffId + Glyphs.toString(" int", glyphs));
            }

            if (checkClef(glyphs, line, staffId)) {
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
    private boolean checkClef (Collection<Glyph> glyphs,
                               Glyph             line,
                               int               staffId)
    {
        Glyphs.purgeManuals(glyphs);

        if (glyphs.isEmpty()) {
            return false;
        }

        Glyph compound = system.buildTransientCompound(glyphs);
        system.computeGlyphFeatures(compound);

        // Check if a clef appears in the top evaluations
        Evaluation vote = GlyphNetwork.getInstance()
                                      .topRawVote(
            compound,
            constants.clefMaxDoubt.getValue(),
            clefPredicate);

        if (vote != null) {
            // We now have a clef!
            // Look around for an even better result...
            if (logger.isFineEnabled()) {
                logger.fine(
                    vote.shape + " built from " + Glyphs.toString(glyphs));
            }

            PixelRectangle outer = compound.getContourBox();
            outer.grow(xMargin, yMargin);

            if (line != null) {
                line.addAttachment("clefOuter#" + staffId, outer);
            }

            List<Glyph> outerGlyphs = system.lookupIntersectedGlyphs(outer);
            outerGlyphs.removeAll(glyphs);
            Collections.sort(outerGlyphs, Glyph.reverseWeightComparator);

            final double minWeight = constants.minWeight.getValue();

            for (Glyph g : outerGlyphs) {
                // Consider only glyphs with a minimum weight
                if (g.getNormalizedWeight() < minWeight) {
                    break;
                }

                if (logger.isFineEnabled()) {
                    logger.fine("Considering " + g);
                }

                Glyph newCompound = system.buildTransientCompound(
                    Arrays.asList(compound, g));
                system.computeGlyphFeatures(newCompound);

                final Evaluation newVote = GlyphNetwork.getInstance()
                                                       .topRawVote(
                    newCompound,
                    constants.clefMaxDoubt.getValue(),
                    clefPredicate);

                if ((newVote != null) && (newVote.doubt < vote.doubt)) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            vote + " better built with glyph#" + g.getId());
                    }

                    compound = newCompound;
                    vote = newVote;
                }
            }

            // Register the last definition of the clef
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

        Scale.Fraction     clefWidth = new Scale.Fraction(
            3d,
            "Width of a clef");
        Scale.Fraction     xOffset = new Scale.Fraction(
            0.2d,
            "Clef horizontal offset since left bar");
        Scale.Fraction     yOffset = new Scale.Fraction(
            0d,
            "Clef vertical offset since staff line");
        Scale.Fraction     xMargin = new Scale.Fraction(
            0.5d,
            "Clef horizontal outer margin");
        Scale.Fraction     yMargin = new Scale.Fraction(
            0.5d,
            "Clef vertical outer margin");
        Scale.AreaFraction minWeight = new Scale.AreaFraction(
            0.1,
            "Minimum normalized weight to be added to a clef");
        Evaluation.Doubt   clefMaxDoubt = new Evaluation.Doubt(
            300d,
            "Maximum doubt for clef verification");
    }
}
