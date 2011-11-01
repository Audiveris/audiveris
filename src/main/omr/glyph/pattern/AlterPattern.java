//----------------------------------------------------------------------------//
//                                                                            //
//                          A l t e r P a t t e r n                           //
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
 * Class {@code AlterPattern} implements a pattern for alteration glyphs which
 * have been over-segmented into stem(s) + other stuff.
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
    final int             maxCloseStemDx;
    final int             minCloseStemOverlap;
    final int             maxAlterStemLength;
    final int             maxNaturalOverlap;

    // Adapters
    final CompoundBuilder compoundBuilder;
    final AlterAdapter    sharpAdapter;
    final AlterAdapter    naturalAdapter;

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

        compoundBuilder = system.getCompoundBuilder();
        sharpAdapter = new SharpAdapter(system);
        naturalAdapter = new NaturalAdapter(system);
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
        int              successNb = 0; // Success counter
        SortedSet<Glyph> stems = retrieveShortStems(); // Ordered short stems

        // Look for close stems
        for (Glyph glyph : stems) {
            if (!glyph.isStem()) {
                continue;
            }

            final PixelRectangle leftBox = glyph.getContourBox();
            final int            leftX = leftBox.x + (leftBox.width / 2);

            //logger.info("Checking stems close to glyph #" + glyph.getId());
            for (Glyph other : stems.tailSet(glyph)) {
                if ((other == glyph) || !other.isStem()) {
                    continue;
                }

                // Check horizontal distance
                final PixelRectangle rightBox = other.getContourBox();
                final int            rightX = rightBox.x +
                                              (rightBox.width / 2);
                final int            dx = rightX - leftX;

                if (dx > maxCloseStemDx) {
                    break; // Since the set is ordered, no candidate is left
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
                adapter.setStemBoxes(leftBox, rightBox);

                Glyph compound = compoundBuilder.buildCompound(
                    glyph,
                    true,
                    system.getGlyphs(),
                    adapter);

                if (compound != null) {
                    successNb++;

                    if (logger.isFineEnabled()) {
                        logger.info(
                            system.getLogPrefix() + "Compound #" +
                            compound.getId() + " rebuilt as " +
                            compound.getShape());
                    }
                } else {
                    // Restore stem shapes
                    glyph.setShape(Shape.COMBINING_STEM);
                    other.setShape(Shape.COMBINING_STEM);
                }
            }
        }

        return successNb;
    }

    //--------------------//
    // retrieveShortStems //
    //--------------------//
    /**
     * Retrieve the collection of all stems in the system,
     * ordered naturally by their abscissa.
     * @return the set of short stems
     */
    private SortedSet<Glyph> retrieveShortStems ()
    {
        final SortedSet<Glyph> stems = Glyphs.sortedSet();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isStem() && glyph.isActive()) {
                PixelRectangle box = glyph.getContourBox();

                // Check stem length
                if (box.height <= maxAlterStemLength) {
                    stems.add(glyph);
                }
            }
        }

        return stems;
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

        protected PixelRectangle leftBox;
        protected PixelRectangle rightBox;

        //~ Constructors -------------------------------------------------------

        public AlterAdapter (SystemInfo     system,
                             EnumSet<Shape> shapes)
        {
            super(system, constants.alterMaxDoubt.getValue(), shapes);
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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Evaluation.Doubt alterMaxDoubt = new Evaluation.Doubt(
            Double.MAX_VALUE,
            "Maximum doubt for alteration sign verification");
        Scale.Fraction   maxCloseStemDx = new Scale.Fraction(
            0.7d,
            "Maximum horizontal distance for close stems");
        Scale.Fraction   maxAlterStemLength = new Scale.Fraction(
            3d,
            "Maximum length for pseudo-stem(s) in alteration sign");
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
        public PixelRectangle getReferenceBox ()
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
        public PixelRectangle getReferenceBox ()
        {
            PixelRectangle box = getStemsBox();
            box.grow(maxCloseStemDx / 2, minCloseStemOverlap / 2);

            return box;
        }
    }
}
