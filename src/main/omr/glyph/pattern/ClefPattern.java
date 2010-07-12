/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphNetwork;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import omr.util.Predicate;

import java.util.Collection;

/**
 * Class {@code ClefPattern} verifies the initial clefs of a system
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


    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ClefPattern object.
     */
    public ClefPattern (SystemInfo system)
    {
        super("Clef", system);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public int run ()
    {
        int         successNb = 0;

        final Scale scale = system.getSheet()
                                  .getScale();
        final int   clefHalfWidth = scale.toPixels(constants.clefHalfWidth);

        for (Glyph glyph : system.getGlyphs()) {
            if (!glyph.isClef()) {
                continue;
            }

            if (logger.isFineEnabled()) {
                logger.fine("Glyph#" + glyph.getId() + " " + glyph.getShape());
            }

            PixelPoint center = glyph.getAreaCenter();
            StaffInfo  staff = system.getStaffAtY(center.y);

            // Look in the other staves
            for (StaffInfo oStaff : system.getStaves()) {
                if (oStaff == staff) {
                    continue;
                }

                // Is there a clef in this staff, with similar abscissa?
                PixelRectangle oBox = new PixelRectangle(
                    center.x - clefHalfWidth,
                    oStaff.getFirstLine().yAt(center.x),
                    2 * clefHalfWidth,
                    oStaff.getHeight());

                if (logger.isFineEnabled()) {
                    logger.fine("oBox: " + oBox);
                }

                Collection<Glyph> glyphs = system.lookupIntersectedGlyphs(oBox);

                if (logger.isFineEnabled()) {
                    logger.fine(Glyphs.toString(glyphs));
                }

                if (!foundClef(glyphs)) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "No clef found at x:" + center.x + " in staff " +
                            oStaff);
                    }

                    if (checkClef(glyphs)) {
                        successNb++;
                    }
                }
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
        Glyphs.purgeManualShapes(glyphs);

        if (glyphs.isEmpty()) {
            return false;
        }

        Glyph compound = system.buildTransientCompound(glyphs);
        system.computeGlyphFeatures(compound);

        // Check if a clef appears in the top evaluations
        final Evaluation vote = GlyphNetwork.getInstance()
                                            .topVote(
            compound,
            constants.bassMaxDoubt.getValue(),
            clefPredicate);

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

    //-----------//
    // foundClef //
    //-----------//
    /**
     * Check whether the provided collection of glyphs contains a clef
     * @param glyphs the provided glyphs
     * @return trur if a clef shape if found
     */
    private boolean foundClef (Collection<Glyph> glyphs)
    {
        for (Glyph gl : glyphs) {
            if (gl.isClef()) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Found glyph#" + gl.getId() + " as " + gl.getShape());
                }

                return true;
            }
        }

        return false;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction   clefHalfWidth = new Scale.Fraction(
            2d,
            "Half width of a clef");
        Evaluation.Doubt bassMaxDoubt = new Evaluation.Doubt(
            3d,
            "Maximum doubt for bass clef verification");
    }
}
