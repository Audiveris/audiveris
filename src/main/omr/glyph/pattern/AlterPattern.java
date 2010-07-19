//----------------------------------------------------------------------------//
//                                                                            //
//                          A l t e r P a t t e r n                           //
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

import omr.glyph.CompoundBuilder;
import omr.glyph.Evaluation;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Implement;

import java.util.EnumSet;
import java.util.SortedSet;

/**
 * Class {@code AlterPattern} implements a pattern for alteration glyphs, which
 * have been over-segmented into stems + other stuff. We use the fact that the
 * two stems are very close to each other.
 *
 * @author Hervé Bitteur
 */
public class AlterPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(AlterPattern.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlterPattern object.
     */
    public AlterPattern (SystemInfo system)
    {
        super("Alter", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    /**
     * Verify the case of stems very close to each other since they may result
     * from wrong segmentation of sharp or natural signs
     * @return the number of cases fixed
     */
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        CompoundBuilder        compoundBuilder = system.getCompoundBuilder();
        Scale                  scale = system.getScoreSystem()
                                             .getScale();

        // Constants for alter verification
        final int              maxCloseStemDx = scale.toPixels(
            constants.maxCloseStemDx);
        final int              minCloseStemOverlap = scale.toPixels(
            constants.minCloseStemOverlap);
        final int              maxCloseStemLength = scale.toPixels(
            constants.maxCloseStemLength);
        final int              maxNaturalOverlap = scale.toPixels(
            constants.maxNaturalOverlap);

        final AlterAdapter     sharpAdapter = new SharpAdapter(system);
        final AlterAdapter     naturalAdapter = new NaturalAdapter(system);

        // First retrieve the collection of all stems in the system
        // Ordered naturally by their abscissa
        final SortedSet<Glyph> stems = Glyphs.sortedSet();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isStem() && glyph.isActive()) {
                PixelRectangle box = glyph.getContourBox();

                // Check stem length
                if (box.height <= maxCloseStemLength) {
                    stems.add(glyph);
                }
            }
        }

        int successNb = 0; // Success counter

        // Then, look for close stems
        for (Glyph glyph : stems) {
            if (!glyph.isStem()) {
                continue;
            }

            final PixelRectangle lBox = glyph.getContourBox();
            final int            lX = lBox.x + (lBox.width / 2);

            //logger.info("Checking stems close to glyph #" + glyph.getId());
            for (Glyph other : stems.tailSet(glyph)) {
                if ((other == glyph) || !other.isStem()) {
                    continue;
                }

                // Check horizontal distance
                final PixelRectangle rBox = other.getContourBox();
                final int            rX = rBox.x + (rBox.width / 2);
                final int            dx = rX - lX;

                if (dx > maxCloseStemDx) {
                    break; // Since the set is ordered, no candidate is left
                }

                // Check vertical overlap
                final int commonTop = Math.max(lBox.y, rBox.y);
                final int commonBot = Math.min(
                    lBox.y + lBox.height,
                    rBox.y + rBox.height);
                final int overlap = commonBot - commonTop;

                if (overlap < minCloseStemOverlap) {
                    continue;
                }

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "close stems: " + Glyphs.toString(glyph, other));
                }

                // "hide" the stems to not perturb evaluation
                glyph.setShape(null);
                other.setShape(null);

                AlterAdapter adapter = null;

                if (overlap <= maxNaturalOverlap) {
                    if (logger.isFineEnabled()) {
                        logger.fine("NATURAL sign?");
                    }

                    adapter = naturalAdapter;
                } else {
                    if (logger.isFineEnabled()) {
                        logger.fine("SHARP sign?");
                    }

                    adapter = sharpAdapter;
                }

                // Prepare the adapter with proper stem boxes
                adapter.setStemBoxes(lBox, rBox);

                Glyph compound = compoundBuilder.buildCompound(
                    glyph,
                    system.getGlyphs(),
                    adapter);

                if (compound != null) {
                    successNb++;
                    logger.info(
                        "Compound #" + compound.getId() + " rebuilt as " +
                        compound.getShape());
                } else {
                    // Restore stem shapes
                    glyph.setShape(Shape.COMBINING_STEM);
                    other.setShape(Shape.COMBINING_STEM);
                }
            }
        }

        return successNb;
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // AlterAdapter //
    //--------------//
    /**
     * Abstract compound adapter meant to build sharps or naturals
     */
    private abstract class AlterAdapter
        extends CompoundBuilder.TopShapeAdapter
    {
        //~ Instance fields ----------------------------------------------------

        protected final int      maxCloseStemDx;
        protected final int      minCloseStemOverlap;
        protected PixelRectangle lBox;
        protected PixelRectangle rBox;

        //~ Constructors -------------------------------------------------------

        public AlterAdapter (SystemInfo     system,
                             EnumSet<Shape> shapes)
        {
            super(system, constants.alterMaxDoubt.getValue(), shapes);

            Scale scale = system.getScoreSystem()
                                .getScale();
            maxCloseStemDx = scale.toPixels(constants.maxCloseStemDx);
            minCloseStemOverlap = scale.toPixels(constants.minCloseStemOverlap);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {
            return !glyph.isManualShape();
        }

        @Override
        public boolean isGlyphClose (PixelRectangle box,
                                     Glyph          glyph)
        {
            return box.contains(glyph.getContourBox());
        }

        public void setStemBoxes (PixelRectangle lBox,
                                  PixelRectangle rBox)
        {
            this.lBox = lBox;
            this.rBox = rBox;
        }

        protected PixelRectangle getStemsBox ()
        {
            if ((lBox == null) || (rBox == null)) {
                throw new NullPointerException("Stem boxes have not been set");
            }

            PixelRectangle box = new PixelRectangle(lBox);
            box.add(rBox);

            return box;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Evaluation.Doubt alterMaxDoubt = new Evaluation.Doubt(
            6,
            "Maximum doubt for alteration sign verification");
        Scale.Fraction   maxCloseStemDx = new Scale.Fraction(
            0.7d,
            "Maximum horizontal distance for close stems");
        Scale.Fraction   maxCloseStemLength = new Scale.Fraction(
            3d,
            "Maximum length for close stems");
        Scale.Fraction   maxNaturalOverlap = new Scale.Fraction(
            2.0d,
            "Maximum vertical overlap for natural stems");
        Scale.Fraction   minCloseStemOverlap = new Scale.Fraction(
            0.5d,
            "Minimum vertical overlap for close stems");
    }

    //----------------//
    // NaturalAdapter //
    //----------------//
    /**
     * Compound adapter meant to build naturals
     */
    private class NaturalAdapter
        extends AlterAdapter
    {
        //~ Constructors -------------------------------------------------------

        public NaturalAdapter (SystemInfo system)
        {
            super(system, EnumSet.of(Shape.NATURAL));
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public PixelRectangle getIntersectionBox ()
        {
            PixelRectangle box = getStemsBox();
            box.grow(maxCloseStemDx / 4, minCloseStemOverlap / 2);

            return box;
        }
    }

    //--------------//
    // SharpAdapter //
    //--------------//
    /**
     * Compound adapter meant to build sharps
     */
    private class SharpAdapter
        extends AlterAdapter
    {
        //~ Constructors -------------------------------------------------------

        public SharpAdapter (SystemInfo system)
        {
            super(system, EnumSet.of(Shape.SHARP));
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public PixelRectangle getIntersectionBox ()
        {
            PixelRectangle box = getStemsBox();
            box.grow(maxCloseStemDx / 2, minCloseStemOverlap / 2);

            return box;
        }
    }
}
