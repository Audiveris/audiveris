//----------------------------------------------------------------------------//
//                                                                            //
//                         S l u r I n s p e c t o r                          //
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
import omr.glyph.GlyphInspector;
import omr.glyph.Shape;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;

import omr.lag.Section;
import omr.lag.Sections;

import omr.log.Logger;

import omr.math.Circle;
import omr.math.PointsCollector;

import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Implement;
import omr.util.Wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code SlurInspector} encapsulates physical processing dedicated
 * to inspection at system level of glyphs with SLUR shape.
 *
 * @author Hervé Bitteur
 */
public class SlurInspector
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SlurInspector.class);

    //~ Instance fields --------------------------------------------------------

    /** Related compound builder */
    private final CompoundBuilder compoundBuilder;

    // Cached system-dependent constants
    final int    interline;
    final int    minChunkWeight;
    final int    slurBoxDx;
    final int    slurBoxDy;
    final double maxCircleDistance;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SlurInspector //
    //---------------//
    /**
     * Creates a new SlurInspector object.
     * @param system The dedicated system
     */
    public SlurInspector (SystemInfo system)
    {
        super("Slur", system);
        compoundBuilder = new CompoundBuilder(system);

        // Compute parameters
        interline = scale.interline();
        minChunkWeight = scale.toPixels(constants.minChunkWeight);
        slurBoxDx = scale.toPixels(constants.slurBoxDx);
        slurBoxDy = scale.toPixels(constants.slurBoxDy);
        maxCircleDistance = constants.maxCircleDistance.getValue();
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // computeCircle //
    //---------------//
    /**
     * Compute the Circle which best approximates the pixels of a given glyph.
     * @param glyph The glyph to fit the circle on
     * @return The best circle possible
     */
    public static Circle computeCircle (Glyph glyph)
    {
        return computeCircle(glyph.getMembers());
    }

    //---------------//
    // computeCircle //
    //---------------//
    /**
     * Compute the Circle which best approximates the pixels of a given
     * collection of sections.
     * @param sections The collection of sections to fit the circle on
     * @return The best circle possible
     */
    public static Circle computeCircle (Collection<?extends Section> sections)
    {
        // First cumulate point from sections
        int weight = 0;

        for (Section section : sections) {
            weight += section.getWeight();
        }

        double[]        xx = new double[weight];
        double[]        yy = new double[weight];

        // Append recursively all points
        PointsCollector collector = new PointsCollector(null, weight);

        for (Section section : sections) {
            section.cumulate(collector);
        }

        // Convert arrays of int's to arrays of double's
        int[] intXX = collector.getXValues();
        int[] intYY = collector.getYValues();

        for (int i = 0; i < weight; i++) {
            xx[i] = intXX[i];
            yy[i] = intYY[i];
        }

        // Then compute the circle 
        return new Circle(xx, yy);
    }

    //--------------//
    // fixLargeSlur //
    //--------------//
    /**
     * For large glyphs, we suspect a slur with a stuck object, so the strategy
     * is to rebuild the true Slur portions from the underlying sections.
     * @param oldSlur the spurious slur
     * @return the extracted oldSlur glyph, if any
     */
    public Glyph fixLargeSlur (Glyph oldSlur)
    {
        /**
         * Sections are first ordered by decreasing weight and continuously
         * tested via the distance to the best approximating circle.  Sections
         * whose weight is under a given threshold are appended to the slur only
         * if the resulting circle distance gets lower.
         *
         * The "good" sections are put into the "kept" collection.
         * Sections left over are put into the "left" collection in order to be
         * used to rebuild the stuck object(s).
         */
        if (logger.isFineEnabled()) {
            logger.fine("fixing Large Slur for glyph #" + oldSlur.getId());
        }

        // Get a COPY of the member list, sorted by decreasing weight */
        List<Section> members = new ArrayList<Section>(oldSlur.getMembers());
        Collections.sort(members, Section.reverseWeightComparator);

        // Find the suitable seed
        Wrapper<Double> seedDist = new Wrapper<Double>();
        Section         seedSection = findSeedSection(members, seedDist);

        // If no significant section has been found, just give up
        if (seedSection == null) {
            oldSlur.setShape(null);

            return null;
        }

        List<Section> kept = new ArrayList<Section>();
        List<Section> left = new ArrayList<Section>();

        findSectionsOnCircle(members, seedSection, seedDist.value, kept, left);
        removeIsolatedSections(seedSection, kept, left);

        if (!kept.isEmpty()) {
            try {
                // Make sure we do have a suitable slur
                return buildFinalSlur(oldSlur, kept, left);
            } catch (Exception ex) {
                left.addAll(kept);

                return null;
            } finally {
                // Remove former oldSlur glyph
                oldSlur.setShape(null);
                system.removeGlyph(oldSlur);

                // Free the sections left over
                for (Section section : left) {
                    section.setGlyph(null);
                }
            }
        } else {
            logger.warning(
                system.getScoreSystem().getContextString() +
                " No section left from large slur #" + oldSlur.getId());

            return null;
        }
    }

    //-----------------//
    // fixSpuriousSlur //
    //-----------------//
    /**
     * Try to correct the slur glyphs (which have a too high circle distance) by
     * either adding a neigboring glyph (for small slurs) or removing stuck
     * glyph sections (for large slurs).
     * @param glyph the spurious glyph at hand
     * @return the fixed slur glyph, if any, otherwise null
     */
    public Glyph fixSpuriousSlur (Glyph glyph)
    {
        if (glyph.getNormalizedWeight() <= constants.spuriousWeightThreshold.getValue()) {
            return extendSlur(glyph);
        } else {
            return fixLargeSlur(glyph);
        }
    }

    //------------//
    // runPattern //
    //------------//
    /**
     * Process all the slur glyphs in the given system, and try to correct the
     * spurious ones if any
     * @return the number of slurs fixed
     */
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        int         modifs = 0;

        // Make a list of all slur glyphs to be checked in this system
        // (So as to free the system glyph list for on-the-fly modifications)
        List<Glyph> slurs = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            if ((glyph.getShape() == Shape.SLUR) && !glyph.isManualShape()) {
                slurs.add(glyph);
            }
        }

        // Try to extend existing slurs (by merging with other slurs/glyphs)
        List<Glyph> toAdd = new ArrayList<Glyph>();

        for (Iterator<Glyph> it = slurs.iterator(); it.hasNext();) {
            Glyph seed = it.next();

            // Check this slur has not just been 'merged' with another one
            if (seed.isActive()) {
                try {
                    Glyph largerSlur = extendSlur(seed);

                    if (largerSlur != null) {
                        toAdd.add(largerSlur);
                        it.remove();
                        modifs++;
                    }
                } catch (Exception ex) {
                    logger.warning("Error in extending slur", ex);
                }
            }
        }

        slurs.addAll(toAdd);

        // Then verify each slur seed in turn
        for (Glyph seed : slurs) {
            // Check this slur has not just been 'merged' with another one
            if (seed.isActive()) {
                if (!computeCircle(seed)
                         .isValid(maxCircleDistance)) {
                    try {
                        Glyph newSlur = fixSpuriousSlur(seed);

                        if (newSlur == null) {
                            seed.setShape(null);
                        } else {
                            modifs++;
                        }
                    } catch (Exception ex) {
                        logger.warning("Error in fixing slur", ex);
                    }
                } else if (logger.isFineEnabled()) {
                    logger.finest("Valid slur " + seed.getId());
                }
            }
        }

        return modifs;
    }

    //----------------//
    // buildFinalSlur //
    //----------------//
    private Glyph buildFinalSlur (Glyph         oldSlur,
                                  List<Section> kept,
                                  List<Section> left)
    {
        Circle circle = computeCircle(kept);
        double distance = circle.getDistance();

        if (logger.isFineEnabled()) {
            logger.fine(
                "oldSlur#" + oldSlur.getId() + " final dist=" + distance);
        }

        if (distance <= maxCircleDistance) {
            // Build new slur glyph with sections kept
            Glyph newGlyph = new BasicGlyph(interline);

            for (Section section : kept) {
                newGlyph.addSection(section, Glyph.Linking.LINK_BACK);
            }

            // Beware, the newGlyph may now belong to a different system
            SystemInfo newSystem = system.getSheet()
                                         .getSystemOf(newGlyph);
            newGlyph = newSystem.addGlyph(newGlyph);
            newGlyph.setShape(Shape.SLUR);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Built slur #" + newGlyph.getId() + " distance=" +
                    (float) distance + Sections.toString(" sections", kept));

                logger.fine(
                    "Fixed large slur #" + oldSlur.getId() + " as smaller #" +
                    newGlyph.getId());
            }

            return newGlyph;
        } else {
            if (logger.isFineEnabled()) {
                logger.fine("Giving up slur w/ " + kept);
            }

            left.addAll(kept);

            return null;
        }
    }

    //------------//
    // extendSlur //
    //------------//
    /**
     * For small glyphs, we suspect a slur segmented by a barline for
     * example. The strategy is then to try to build a compound glyph with
     * neighboring glyphs (either another slur, or a clutter), and test the
     * distance to the resulting best approximating circle.
     * @param oldSlur the spurious slur glyph
     * @return the fixed slur glyph, if any
     */
    private Glyph extendSlur (Glyph oldSlur)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Trying to extend slur glyph #" + oldSlur.getId());
        }

        SlurCompoundAdapter adapter = new SlurCompoundAdapter(system);
        adapter.setSeed(oldSlur);

        // Collect glyphs suitable for participating in compound building
        List<Glyph> suitables = new ArrayList<Glyph>(system.getGlyphs().size());

        for (Glyph g : system.getGlyphs()) {
            if ((g != oldSlur) && adapter.isCandidateSuitable(g)) {
                suitables.add(g);
            }
        }

        // Sort suitable glyphs by decreasing weight
        Collections.sort(suitables, Glyph.reverseWeightComparator);

        // Process that slur, looking at neighbors
        Glyph compound = compoundBuilder.buildCompound(
            oldSlur,
            suitables,
            adapter);

        if (compound != null) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Extended slur #" + oldSlur.getId() + " as compound #" +
                    compound.getId());
            }

            return compound;
        } else {
            return null;
        }
    }

    //----------------------//
    // findSectionsOnCircle //
    //----------------------//
    /**
     * From the provided members, find all sections which are well located
     * on the slur circle
     * @param members
     * @param seedSection
     * @param seedDist
     * @param kept
     * @param left
     */
    private void findSectionsOnCircle (List<Section> members,
                                       Section       seedSection,
                                       double        seedDist,
                                       List<Section> kept,
                                       List<Section> left)
    {
        // We impose the seed
        kept.add(seedSection);

        List<Section> tried = new ArrayList<Section>();
        double        distThreshold = seedDist;
        double        bestDistance = distThreshold;

        for (Section section : members) {
            if (section == seedSection) {
                continue;
            }

            distThreshold = bestDistance;

            if (logger.isFineEnabled()) {
                logger.fine("Trying " + section);
            }

            // Try a circle
            tried.clear();
            tried.addAll(kept);
            tried.add(section);

            try {
                Circle circle = computeCircle(tried);
                double distance = circle.getDistance();

                if (logger.isFineEnabled()) {
                    logger.fine("dist=" + distance);
                }

                if (distance <= distThreshold) {
                    kept.add(section);
                    bestDistance = distance;

                    if (logger.isFineEnabled()) {
                        logger.fine("Keep " + section);
                    }
                } else {
                    left.add(section);

                    if (logger.isFineEnabled()) {
                        logger.fine("Discard " + section);
                    }
                }
            } catch (Exception ex) {
                left.add(section);

                if (logger.isFineEnabled()) {
                    logger.fine(ex.getMessage() + " w/ " + section);
                }
            }
        }
    }

    //-----------------//
    // findSeedSection //
    //-----------------//
    /**
     *  Find the suitable seed, which is chosen as the section with best circle
     * distance among the sections whose weight is significant
     * @param sortedMembers the candidate sections, by decreasing weight
     * @return the suitable seed, perhaps null
     */
    private Section findSeedSection (List<Section>   sortedMembers,
                                     Wrapper<Double> seedDist)
    {
        Section seedSection = null;
        seedDist.value = Double.MAX_VALUE;

        for (Section seed : sortedMembers) {
            if (seed.getWeight() >= minChunkWeight) {
                Circle circle = computeCircle(Arrays.asList(seed));
                double dist = circle.getDistance();

                if ((dist <= maxCircleDistance) && (dist < seedDist.value)) {
                    seedDist.value = dist;
                    seedSection = seed;
                }
            }
        }

        if (logger.isFineEnabled()) {
            if (seedSection == null) {
                logger.fine("No suitable seed section found");
            } else {
                logger.fine(
                    "Seed section is " + seedSection + " dist:" +
                    seedDist.value);
            }
        }

        return seedSection;
    }

    //------------------------//
    // removeIsolatedSections //
    //------------------------//
    /**
     * Remove any section which is too far from the other ones
     * @param seedSection
     * @param kept
     * @param left
     */
    private void removeIsolatedSections (Section       seedSection,
                                         List<Section> kept,
                                         List<Section> left)
    {
        PixelRectangle slurBox = seedSection.getContourBox();
        List<Section>  toCheck = new ArrayList<Section>(kept);
        toCheck.remove(seedSection);
        kept.clear();
        kept.add(seedSection);

        boolean makingProgress;

        do {
            makingProgress = false;

            for (Iterator<Section> it = toCheck.iterator(); it.hasNext();) {
                Section        section = it.next();
                PixelRectangle sectBox = section.getContourBox();
                sectBox.grow(slurBoxDx, slurBoxDy);

                if (sectBox.intersects(slurBox)) {
                    slurBox.add(sectBox);
                    it.remove();
                    kept.add(section);
                    makingProgress = true;
                }
            }
        } while (makingProgress);

        left.addAll(toCheck);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        //
        Scale.Fraction     maxCircleDistance = new Scale.Fraction(
            0.15,
            "Maximum distance to approximating circle" + " for a slur");

        //
        Scale.AreaFraction spuriousWeightThreshold = new Scale.AreaFraction(
            1.5,
            "Normalized weight threshold between small and large spurious" +
            " slurs");

        //
        Scale.AreaFraction minChunkWeight = new Scale.AreaFraction(
            0.5,
            "Minimum weight of a chunk to be part of slur" + " computation");

        //
        Scale.Fraction slurBoxDx = new Scale.Fraction(
            1.2, //0.7,
            "Extension abscissa when looking for slur compound");

        //
        Scale.Fraction slurBoxDy = new Scale.Fraction(
            0.5, //0.4,
            "Extension ordinate when looking for slur compound");
    }

    //---------------------//
    // SlurCompoundAdapter //
    //---------------------//
    /**
     * Class {@code SlurCompoundAdapter} is a CompoundAdapter meant to process
     * a small slur.
     */
    private class SlurCompoundAdapter
        extends CompoundBuilder.AbstractAdapter
    {
        //~ Constructors -------------------------------------------------------

        public SlurCompoundAdapter (SystemInfo system)
        {
            super(system, 0d); // Value is irrelevant
        }

        //~ Methods ------------------------------------------------------------

        @Implement(CompoundBuilder.CompoundAdapter.class)
        public boolean isCandidateSuitable (Glyph glyph)
        {
            if (!glyph.isActive()) {
                return false; // Safer
            }

            Shape shape = glyph.getShape();

            if (!glyph.isKnown()) {
                return true;
            }

            if ((shape == Shape.SLUR) && !glyph.isManualShape()) {
                return true;
            }

            return (!glyph.isManualShape() &&
                   ((shape == Shape.CLUTTER) || (shape == Shape.STRUCTURE))) ||
                   (glyph.getDoubt() >= GlyphInspector.getMinCompoundPartDoubt());
        }

        @Implement(CompoundBuilder.CompoundAdapter.class)
        public boolean isCompoundValid (Glyph compound)
        {
            // Look for a circle
            Circle circle = computeCircle(compound);

            if (circle.isValid(maxCircleDistance)) {
                chosenEvaluation = new Evaluation(
                    Shape.SLUR,
                    Evaluation.ALGORITHM);

                return true;
            }

            return false;
        }

        @Implement(CompoundBuilder.CompoundAdapter.class)
        public PixelRectangle getIntersectionBox ()
        {
            PixelRectangle box = new PixelRectangle(seed.getContourBox());
            box.grow(slurBoxDx, slurBoxDy);

            return box;
        }
    }
}
