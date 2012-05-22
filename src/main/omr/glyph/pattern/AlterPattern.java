//----------------------------------------------------------------------------//
//                                                                            //
//                          A l t e r P a t t e r n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
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
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;

import java.util.EnumSet;
import java.util.SortedSet;

/**
 * Class {@code AlterPattern} implements a pattern for alteration
 * glyphs which have been "oversegmented" into stem(s) + other stuff.
 * <p>This applies for sharp, natural and flat signs.
 * We use the fact that the stem(s) are rather short and, for the case of sharp
 * and natural, very close to each other.
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

    //~ Instance fields --------------------------------------------------------
    // Scale-dependent constants for alter verification
    private final int maxCloseStemDx;

    private final int minCloseStemOverlap;

    private final int maxAlterStemLength;

    private final int maxNaturalOverlap;

    private final int flatHeadWidth;

    private final int flatHeadHeight;

    // Adapters
    private final PairAdapter sharpAdapter;

    private final PairAdapter naturalAdapter;

    /** Collection of (short) stems, sorted on abscissa */
    private SortedSet<Glyph> stems;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new AlterPattern object.
     */
    public AlterPattern (SystemInfo system)
    {
        super("Alter", system);

        maxCloseStemDx = scale.toPixels(constants.maxCloseStemDx);
        minCloseStemOverlap = scale.toPixels(constants.minCloseStemOverlap);
        maxAlterStemLength = scale.toPixels(constants.maxAlterStemLength);
        maxNaturalOverlap = scale.toPixels(constants.maxNaturalOverlap);

        flatHeadWidth = scale.toPixels(constants.flatHeadWidth);
        flatHeadHeight = scale.toPixels(constants.flatHeadHeight);

        sharpAdapter = new SharpAdapter(system);
        naturalAdapter = new NaturalAdapter(system);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    /**
     * Check the neighborhood of all short stems.
     *
     * @return the number of cases fixed
     */
    @Override
    public int runPattern ()
    {
        int successNb = 0; // Success counter

        // Sorted short stems
        stems = retrieveShortStems();

        // Look for close stems (sharps & naturals)
        successNb += checkCloseStems();

        // Look for isolated stems (flats)
        successNb += checkSingleStems();

        // Impacted neighbors
        checkFormerStems();

        return successNb;
    }

    //-----------------//
    // checkCloseStems //
    //-----------------//
    /**
     * Verify the case of stems very close to each other since they
     * may result from oversegmentation of sharp or natural signs.
     *
     * @return the number of cases fixed
     */
    private int checkCloseStems ()
    {
        int nb = 0;

        for (Glyph glyph : stems) {
            if (!glyph.isStem()) {
                continue;
            }

            final PixelRectangle leftBox = glyph.getBounds();
            final int leftX = leftBox.x + (leftBox.width / 2);

            //logger.info("Checking stems close to glyph #" + glyph.getId());
            for (Glyph other : stems.tailSet(glyph)) {
                if ((other == glyph) || !other.isStem()) {
                    continue;
                }

                // Check horizontal distance
                final PixelRectangle rightBox = other.getBounds();
                final int rightX = rightBox.x
                        + (rightBox.width / 2);
                final int dx = rightX - leftX;

                if (dx > maxCloseStemDx) {
                    break; // Since the set is sorted, no candidate is left
                }

                // Check vertical overlap
                final int commonTop = Math.max(leftBox.y, rightBox.y);
                final int commonBot = Math.min(
                        leftBox.y + leftBox.height,
                        rightBox.y + rightBox.height);
                final int overlap = commonBot - commonTop;

                if (overlap < minCloseStemOverlap) {
                    continue;
                }

                if (logger.isFineEnabled()) {
                    logger.fine("close stems: {0}",
                                Glyphs.toString(glyph, other));
                }

                // "hide" the stems to not perturb evaluation
                glyph.setShape(null);
                other.setShape(null);

                PairAdapter adapter = null;

                if (overlap <= maxNaturalOverlap) {
                    logger.fine("NATURAL sign?");

                    adapter = naturalAdapter;
                } else {
                    logger.fine("SHARP sign?");

                    adapter = sharpAdapter;
                }

                // Prepare the adapter with proper stem boxes
                adapter.setStemBoxes(leftBox, rightBox);

                Glyph compound = system.buildCompound(
                        glyph,
                        true,
                        system.getGlyphs(),
                        adapter);

                if (compound != null) {
                    nb++;

                    logger.fine("{0}Compound #{1} rebuilt as {2}",
                                new Object[]{system.getLogPrefix(), compound.
                                getId(), compound.getShape()});
                } else {
                    // Restore stem shapes
                    glyph.setShape(Shape.STEM);
                    other.setShape(Shape.STEM);
                }
            }
        }

        return nb;
    }

    //------------------//
    // checkFormerStems //
    //------------------//
    /**
     * Look for glyphs whose shape was dependent on former stems,
     * and call their shape into question again.
     */
    private void checkFormerStems ()
    {
        SortedSet<Glyph> impacted = Glyphs.sortedSet();

        for (Glyph glyph : system.getGlyphs()) {
            if (!glyph.isActive()) {
                continue;
            }

            for (HorizontalSide side : HorizontalSide.values()) {
                // Retrieve "deassigned" stem if any
                Glyph stem = glyph.getStem(side);

                if ((stem != null) && (stem.getShape() != Shape.STEM)) {
                    impacted.add(glyph);
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                    Glyphs.toString("Impacted alteration neighbors", impacted));
        }

        for (Glyph glyph : impacted) {
            Shape shape = glyph.getShape();

            if (ShapeSet.StemSymbols.contains(shape)) {
                // Trigger a reevaluation (w/o forbidding the current shape)
                glyph.setShape(null);
                glyph.allowShape(shape);
            }

            // Re-compute glyph features
            system.computeGlyphFeatures(glyph);
        }
    }

    //------------------//
    // checkSingleStems //
    //------------------//
    /**
     * Verify the case of isolated short stems since they may result
     * from oversegmentation of flat signs.
     *
     * @return the number of cases fixed
     */
    private int checkSingleStems ()
    {
        int nb = 0;
        FlatAdapter flatAdapter = new FlatAdapter(system);

        for (Glyph glyph : stems) {
            if (!glyph.isStem()) {
                continue;
            }

            // "hide" the stems to not perturb evaluation
            glyph.setShape(null);

            Glyph compound = system.buildCompound(
                    glyph,
                    true,
                    system.getGlyphs(),
                    flatAdapter);

            if (compound != null) {
                nb++;

                logger.fine("{0}Compound #{1} rebuilt as {2}",
                            new Object[]{system.getLogPrefix(), compound.getId(),
                                         compound.getShape()});
            } else {
                // Restore stem shapes
                glyph.setShape(Shape.STEM);
            }
        }

        return nb;
    }

    //--------------------//
    // retrieveShortStems //
    //--------------------//
    /**
     * Retrieve the collection of all stems in the system,
     * ordered naturally by their abscissa.
     *
     * @return the set of short stems
     */
    private SortedSet<Glyph> retrieveShortStems ()
    {
        final SortedSet<Glyph> stems = Glyphs.sortedSet();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isStem() && glyph.isActive()) {
                // Check stem length
                if (glyph.getBounds().height <= maxAlterStemLength) {
                    stems.add(glyph);
                }
            }
        }

        return stems;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Evaluation.Grade alterMinGrade = new Evaluation.Grade(
                0.3,
                "Minimum grade for sharp/natural sign verification");

        //
        Evaluation.Grade flatMinGrade = new Evaluation.Grade(
                20d,
                "Minimum grade for flat sign verification");

        //
        Scale.Fraction maxCloseStemDx = new Scale.Fraction(
                0.7d,
                "Maximum horizontal distance for close stems");

        //
        Scale.Fraction maxAlterStemLength = new Scale.Fraction(
                3d,
                "Maximum length for pseudo-stem(s) in alteration sign");

        //
        Scale.Fraction maxNaturalOverlap = new Scale.Fraction(
                2.0d,
                "Maximum vertical overlap for natural stems");

        //
        Scale.Fraction minCloseStemOverlap = new Scale.Fraction(
                0.5d,
                "Minimum vertical overlap for close stems");

        //
        Scale.Fraction flatHeadHeight = new Scale.Fraction(
                1d,
                "Typical height of flat head");

        //
        Scale.Fraction flatHeadWidth = new Scale.Fraction(
                0.5d,
                "Typical width of flat head");
    }

    //-------------//
    // FlatAdapter //
    //-------------//
    /**
     * Compound adapter meant to build flats.
     */
    private class FlatAdapter
            extends CompoundBuilder.TopShapeAdapter
    {
        //~ Constructors -------------------------------------------------------

        public FlatAdapter (SystemInfo system)
        {
            super(
                    system,
                    constants.flatMinGrade.getValue(),
                    EnumSet.of(Shape.FLAT));
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public PixelRectangle computeReferenceBox ()
        {
            final PixelRectangle stemBox = seed.getBounds();

            return new PixelRectangle(
                    stemBox.x,
                    (stemBox.y + stemBox.height) - flatHeadHeight,
                    flatHeadWidth,
                    flatHeadHeight);
        }

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {
            return !glyph.isManualShape();
        }
    }

    //----------------//
    // NaturalAdapter //
    //----------------//
    /**
     * Compound adapter meant to build naturals.
     */
    private class NaturalAdapter
            extends PairAdapter
    {
        //~ Constructors -------------------------------------------------------

        public NaturalAdapter (SystemInfo system)
        {
            super(system, EnumSet.of(Shape.NATURAL));
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public PixelRectangle computeReferenceBox ()
        {
            PixelRectangle newBox = getStemsBox();
            newBox.grow(maxCloseStemDx / 4, minCloseStemOverlap / 2);

            return newBox;
        }
    }

    //-------------//
    // PairAdapter //
    //-------------//
    /**
     * Abstract compound adapter meant to build sharps or naturals
     * from a pair of close stems.
     */
    private abstract class PairAdapter
            extends CompoundBuilder.TopShapeAdapter
    {
        //~ Instance fields ----------------------------------------------------

        protected PixelRectangle leftBox;

        protected PixelRectangle rightBox;

        //~ Constructors -------------------------------------------------------
        public PairAdapter (SystemInfo system,
                            EnumSet<Shape> shapes)
        {
            super(system, constants.alterMinGrade.getValue(), shapes);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public boolean isCandidateClose (Glyph glyph)
        {
            // We use containment instead of intersection
            return box.contains(glyph.getBounds());
        }

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {
            return !glyph.isManualShape();
        }

        public void setStemBoxes (PixelRectangle leftBox,
                                  PixelRectangle rightBox)
        {
            this.leftBox = leftBox;
            this.rightBox = rightBox;
        }

        protected PixelRectangle getStemsBox ()
        {
            if ((leftBox == null) || (rightBox == null)) {
                throw new NullPointerException("Stem boxes have not been set");
            }

            PixelRectangle box = new PixelRectangle(leftBox);
            box.add(rightBox);

            return box;
        }
    }

    //--------------//
    // SharpAdapter //
    //--------------//
    /**
     * Compound adapter meant to build sharps.
     */
    private class SharpAdapter
            extends PairAdapter
    {
        //~ Constructors -------------------------------------------------------

        public SharpAdapter (SystemInfo system)
        {
            super(system, EnumSet.of(Shape.SHARP));
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public PixelRectangle computeReferenceBox ()
        {
            PixelRectangle newBox = getStemsBox();
            newBox.grow(maxCloseStemDx / 2, minCloseStemOverlap / 2);

            return newBox;
        }
    }
}
