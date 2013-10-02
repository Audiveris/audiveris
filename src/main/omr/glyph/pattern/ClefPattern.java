//----------------------------------------------------------------------------//
//                                                                            //
//                           C l e f P a t t e r n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphNetwork;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.Nest;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code ClefPattern} verifies all the initial clefs of a
 * system, using an intersection inner rectangle and a containing
 * outer rectangle to retrieve the clef glyphs and only those ones.
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
    private static final Logger logger = LoggerFactory.getLogger(ClefPattern.class);

    /** Specific predicate to filter clef shapes */
    private static final Predicate<Shape> clefShapePredicate = new Predicate<Shape>()
    {
        @Override
        public boolean check (Shape shape)
        {
            return ShapeSet.Clefs.contains(shape);
        }
    };

    /** Specific predicate to filter clef glyphs */
    private static final Predicate<Glyph> clefGlyphPredicate = new Predicate<Glyph>()
    {
        @Override
        public boolean check (Glyph glyph)
        {
            return glyph.isClef();
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** Glyphs nest */
    private final Nest nest;

    // Scale-dependent parameters
    private final int clefWidth;

    private final int xOffset;

    private final int yOffset;

    private final int xMargin;

    private final int yMargin;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new ClefPattern object.
     *
     * @param system the containing system
     */
    public ClefPattern (SystemInfo system)
    {
        super("Clef", system);

        nest = system.getSheet().getNest();

        clefWidth = scale.toPixels(constants.clefWidth);
        xOffset = scale.toPixels(constants.xOffset);
        yOffset = scale.toPixels(constants.yOffset);
        xMargin = scale.toPixels(constants.xMargin);
        yMargin = scale.toPixels(constants.yMargin);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    /**
     * Check that each staff begins with a clef.
     *
     * @return the number of clefs rebuilt
     */
    @Override
    public int runPattern ()
    {
        int successNb = 0;
        int staffId = 0;

        for (StaffInfo staff : system.getStaves()) {
            staffId++;

            // Define the inner box to intersect clef glyph(s)
            int left = (int) Math.rint(
                    staff.getAbscissa(HorizontalSide.LEFT));
            Rectangle inner = new Rectangle(
                    left + (2 * xOffset) + (clefWidth / 2),
                    staff.getFirstLine().yAt(left) + (staff.getHeight() / 2),
                    0,
                    0);
            inner.grow(
                    (clefWidth / 2) - xOffset,
                    (staff.getHeight() / 2) - yOffset);

            // Remember the box, for visual debug
            staff.addAttachment("  ci", inner);

            // We must find a clef out of these glyphs
            Collection<Glyph> glyphs = system.lookupIntersectedGlyphs(inner);
            logger.debug("{}{}", staffId, Glyphs.toString(" int", glyphs));

            // We assume than there can't be any alien among them, so we should 
            // rebuild the larger glyph which the alien had wrongly segmented
            Set<Glyph> impacted = new HashSet<>();

            for (Glyph glyph : glyphs) {
                if (glyph.getShape() == Shape.STEM) {
                    logger.debug("Clef: Removed stem#{}", glyph.getId());

                    impacted.addAll(glyph.getConnectedNeighbors());
                    impacted.add(glyph);
                }
            }

            if (!impacted.isEmpty()) {
                // Rebuild the larger glyph
                Glyph larger = system.buildCompound(impacted);
                if (larger != null) {
                    logger.debug("Rebuilt stem-segmented {}", larger.idString());
                }

                // Recompute the set of intersected glyphs
                glyphs = system.lookupIntersectedGlyphs(inner);
            }

            if (checkClef(glyphs, staff)) {
                successNb++;
            }
        }

        return successNb;
    }

    //-----------//
    // checkClef //
    //-----------//
    /**
     * Try to recognize a clef in the compound of the provided glyphs.
     *
     * @param glyphs the parts of a clef candidate
     * @param staff  the containing staff
     * @return true if successful
     */
    private boolean checkClef (Collection<Glyph> glyphs,
                               StaffInfo staff)
    {
        if (glyphs.isEmpty()) {
            return false;
        }

        // Check if we already have a clef among the intersected glyphs
        Set<Glyph> clefs = Glyphs.lookupGlyphs(glyphs, clefGlyphPredicate);
        Glyph orgClef = null;

        if (!clefs.isEmpty()) {
            if (Glyphs.containsManual(clefs)) {
                return false; // Respect user decision
            } else {
                // Remember grade of the best existing clef
                for (Glyph glyph : clefs) {
                    if ((orgClef == null)
                        || (glyph.getGrade() > orgClef.getGrade())) {
                        orgClef = glyph;
                    }
                }
            }
        }

        // Remove potential aliens
        Glyphs.purgeManuals(glyphs);

        Glyph compound = system.buildTransientCompound(glyphs);

        // Check if a clef appears in the top evaluations
        Evaluation vote = GlyphNetwork.getInstance().vote(
                compound,
                system,
                Grades.clefMinGrade,
                clefShapePredicate);

        if ((vote != null)
            && ((orgClef == null) || (vote.grade > orgClef.getGrade()))) {
            // We now have a clef!
            // Look around for an even better result...
            logger.debug("{} built from {}",
                    vote.shape, Glyphs.toString(glyphs));

            // Look for larger stuff
            Rectangle outer = compound.getBounds();
            outer.grow(xMargin, yMargin);

            // Remember the box, for visual debug
            staff.addAttachment("co", outer);

            List<Glyph> outerGlyphs = system.lookupIntersectedGlyphs(outer);
            outerGlyphs.removeAll(glyphs);
            Collections.sort(outerGlyphs, Glyph.byReverseWeight);

            final double minWeight = constants.minWeight.getValue();

            for (Glyph g : outerGlyphs) {
                // Consider only glyphs with a minimum weight
                if (g.getNormalizedWeight() < minWeight) {
                    break;
                }

                logger.debug("Considering {}", g);

                Glyph newCompound = system.buildTransientCompound(
                        Arrays.asList(compound, g));
                final Evaluation newVote = GlyphNetwork.getInstance().vote(
                        newCompound,
                        system,
                        Grades.clefMinGrade,
                        clefShapePredicate);

                if ((newVote != null) && (newVote.grade > vote.grade)) {
                    logger.debug("{} better built with {}", vote, g.idString());

                    compound = newCompound;
                    vote = newVote;
                }
            }

            // Register the last definition of the clef
            compound = system.addGlyph(compound);
            compound.setShape(vote.shape, Evaluation.ALGORITHM);

            logger.debug("{} rebuilt as {}", vote.shape, compound.idString());

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

        Scale.Fraction clefWidth = new Scale.Fraction(
                3d,
                "Width of a clef");

        Scale.Fraction xOffset = new Scale.Fraction(
                0.2d,
                "Clef horizontal offset since left bar");

        Scale.Fraction yOffset = new Scale.Fraction(
                0d,
                "Clef vertical offset since staff line");

        Scale.Fraction xMargin = new Scale.Fraction(
                0d,
                "Clef horizontal outer margin");

        Scale.Fraction yMargin = new Scale.Fraction(
                0.5d,
                "Clef vertical outer margin");

        Scale.AreaFraction minWeight = new Scale.AreaFraction(
                0.1,
                "Minimum normalized weight to be added to a clef");

    }
}
