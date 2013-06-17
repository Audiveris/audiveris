//----------------------------------------------------------------------------//
//                                                                            //
//                          A l t e r P a t t e r n                           //
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

import omr.glyph.CompoundBuilder;
import omr.glyph.Evaluation;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import omr.util.Vip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private static final Logger logger = LoggerFactory.getLogger(AlterPattern.class);

    //~ Instance fields --------------------------------------------------------
    //
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

    /** Collection of (short) stems, sorted by abscissa. */
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
                continue; // Already consumed
            }

            // Retrieve interesting stem pairs in the neighborhood
            List<StemPair> pairs = getNeighboringPairs(glyph);

            // Inspect pairs by increasing distance
            for (StemPair pair : pairs) {
                if (!pair.left.isStem() || !pair.right.isStem()) {
                    continue; // This pair is no longer relevant
                }

                if (pair.isVip()) {
                    logger.info("{} Alter pair: {}", glyph.idString(), pair);
                }

                // "hide" the stems to not perturb evaluation
                pair.left.setShape(null);
                pair.right.setShape(null);

                PairAdapter adapter;

                if (pair.overlap <= maxNaturalOverlap) {
                    logger.debug("NATURAL sign?");
                    adapter = naturalAdapter;
                } else {
                    logger.debug("SHARP sign?");
                    adapter = sharpAdapter;
                }

                // Prepare the adapter with proper stem boxes
                adapter.setStemBoxes(pair.left.getBounds(),
                        pair.right.getBounds());

                Glyph compound = system.buildCompound(
                        pair.left,
                        true,
                        system.getGlyphs(),
                        adapter);

                if (compound != null) {
                    nb++;
                    logger.debug("{}Compound #{} rebuilt as {}",
                            system.getLogPrefix(),
                            compound.getId(), compound.getShape());
                } else {
                    // Restore stem shapes
                    pair.left.setShape(Shape.STEM);
                    pair.right.setShape(Shape.STEM);
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

        if (logger.isDebugEnabled()) {
            logger.debug(
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

            // If stem already has notehead or flag/beam, skip it
            Set<Glyph> goods = new HashSet<>();
            Set<Glyph> bads = new HashSet<>();
            glyph.getSymbolsBefore(StemPattern.reliableStemSymbols, goods, bads);
            glyph.getSymbolsAfter(StemPattern.reliableStemSymbols, goods, bads);
            if (!goods.isEmpty()) {
                logger.debug("Skipping good stem {}", glyph);
                continue;
            }

            // "hide" the stems temporarily to not perturb evaluation
            glyph.setShape(null);

            Glyph compound = system.buildCompound(
                    glyph,
                    true,
                    system.getGlyphs(),
                    flatAdapter);

            if (compound != null) {
                nb++;

                logger.debug("{}Compound #{} rebuilt as {}",
                        system.getLogPrefix(),
                        compound.getId(), compound.getShape());
            } else {
                // Restore stem shape
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
        final SortedSet<Glyph> shortStems = Glyphs.sortedSet();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isStem() && glyph.isActive()) {
                // Check stem length
                if (glyph.getBounds().height <= maxAlterStemLength) {
                    shortStems.add(glyph);
                }
            }
        }

        return shortStems;
    }

    //---------------------//
    // getNeighboringPairs //
    //---------------------//
    /**
     * Retrieve all pairs of stems, transitively close to the provided
     * seed, to pickup the most promising pair for natural/sharp alter.
     *
     * @param seed the stem seed
     * @return the collection of stems pairs
     */
    private List<StemPair> getNeighboringPairs (Glyph seed)
    {
        List<StemPair> pairs = new ArrayList<>();

        // First, come up with candidate stems
        SortedSet<Glyph> neighbors = Glyphs.sortedSet(Arrays.asList(seed));
        Rectangle box = seed.getBounds();

        for (Glyph glyph : stems) {
            if (glyph != seed && glyph.isStem()) {
                Rectangle glyphBox = glyph.getBounds();
                glyphBox.grow(maxCloseStemDx, 0);

                if (box.intersects(glyphBox)) {
                    neighbors.add(glyph);
                    box.add(glyph.getBounds());
                } else if (glyphBox.x > box.x + box.width) {
                    break;
                }
            }
        }

        // Second, evaluate pairs and keep only the possible ones
        for (Glyph left : neighbors) {
            final Rectangle leftBox = left.getBounds();
            final int leftX = leftBox.x + (leftBox.width / 2);

            for (Glyph other : neighbors.tailSet(left)) {
                if ((other == left)) {
                    continue;
                }

                // Check horizontal distance
                final Rectangle rightBox = other.getBounds();
                final int rightX = rightBox.x + (rightBox.width / 2);
                if (rightX - leftX > maxCloseStemDx) {
                    continue;
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

                // Evaluate compatibility
                double deltaLength = Math.abs(leftBox.height - rightBox.height);
                double deltaRatio = deltaLength
                                    / Math.max(leftBox.height, rightBox.height);
                pairs.add(new StemPair(left, other, overlap, deltaRatio));
            }
        }

        Collections.sort(pairs);
        return pairs;
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

        Evaluation.Grade flatMinGrade = new Evaluation.Grade(
                20d,
                "Minimum grade for flat sign verification");

        Scale.Fraction maxCloseStemDx = new Scale.Fraction(
                0.7d,
                "Maximum horizontal distance for close stems");

        Scale.Fraction maxAlterStemLength = new Scale.Fraction(
                3.5,
                "Maximum length for pseudo-stem(s) in alteration sign");

        Scale.Fraction maxNaturalOverlap = new Scale.Fraction(
                2.0d,
                "Maximum vertical overlap for natural stems");

        Scale.Fraction minCloseStemOverlap = new Scale.Fraction(
                0.5d,
                "Minimum vertical overlap for close stems");

        Scale.Fraction flatHeadHeight = new Scale.Fraction(
                1d,
                "Typical height of flat head");

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
        public Rectangle computeReferenceBox ()
        {
            final Rectangle stemBox = seed.getBounds();

            return new Rectangle(
                    stemBox.x,
                    (stemBox.y + stemBox.height) - flatHeadHeight,
                    flatHeadWidth,
                    flatHeadHeight);
        }

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {

            if (glyph.isManualShape()) {
                return false;
            }

            Shape shape = glyph.getShape();

            return !ShapeSet.StemSymbols.contains(shape)
                   || shape == Shape.BEAM_HOOK;
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
        public Rectangle computeReferenceBox ()
        {
            Rectangle newBox = getStemsBox();
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

        protected Rectangle leftBox;

        protected Rectangle rightBox;

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

        public void setStemBoxes (Rectangle leftBox,
                                  Rectangle rightBox)
        {
            this.leftBox = leftBox;
            this.rightBox = rightBox;
        }

        protected Rectangle getStemsBox ()
        {
            if ((leftBox == null) || (rightBox == null)) {
                throw new NullPointerException("Stem boxes have not been set");
            }

            Rectangle box = new Rectangle(leftBox);
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
        public Rectangle computeReferenceBox ()
        {
            Rectangle newBox = getStemsBox();
            newBox.grow(maxCloseStemDx / 2, minCloseStemOverlap / 2);

            return newBox;
        }
    }

    //----------//
    // StemPair //
    //----------//
    /**
     * Data about a possible pair of stems for a sharp/natural alter.
     */
    private class StemPair
            implements Comparable<StemPair>, Vip
    {

        /** Stem on left side. */
        final Glyph left;

        /** Stem on right side. */
        final Glyph right;

        /** Vertical overlap. */
        final int overlap;

        /** Info about pair "distance" (to pick the best pair). */
        final double distance;

        boolean vip = false;

        public StemPair (Glyph left,
                         Glyph right,
                         int overlap,
                         double distance)
        {
            this.left = left;
            this.right = right;
            this.overlap = overlap;
            this.distance = distance;

            if (left.isVip() || right.isVip()) {
                setVip();
            }
        }

        /** To sort pairs. */
        @Override
        public int compareTo (StemPair that)
        {
            return Double.compare(this.distance, that.distance);
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{Stems");
            sb.append(" #").append(left.getId());
            sb.append(" #").append(right.getId());
            sb.append(" over:").append(overlap);
            sb.append(" dist:").append((float) distance);
            sb.append("}");
            return sb.toString();
        }

        @Override
        public final boolean isVip ()
        {
            return vip;
        }

        @Override
        public final void setVip ()
        {
            this.vip = true;
        }
    }
}
