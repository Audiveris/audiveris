//----------------------------------------------------------------------------//
//                                                                            //
//                             S l u r G l y p h                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.constant.ConstantSet;

import omr.lag.Section;

import omr.math.Circle;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.stick.Stick;

import omr.util.Implement;
import omr.util.Logger;

import java.util.*;

/**
 * Class <code>SlurGlyph</code> encapsulates physical processing dedicated to
 * glyphs with SLUR shape, before they are actually used to generate Slurs
 * entities in the score domain.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SlurGlyph
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SlurGlyph.class);

    //~ Constructors -----------------------------------------------------------

    /** Pure functionalities ??? */
    private SlurGlyph ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // computeCircle //
    //---------------//
    /**
     * Compute the Circle which best approximates the pixels of a given glyph
     *
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
     * collection of sections
     *
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

        double[] coord = new double[weight];
        double[] pos = new double[weight];

        // Append recursively all points
        int nb = 0;

        for (Section section : sections) {
            nb = section.cumulatePoints(coord, pos, nb);
        }

        // Then compute the circle (swapping coord & pos, for vertical sections)
        return new Circle(pos, coord);
    }

    //-----------------//
    // fixSpuriousSlur //
    //-----------------//
    /**
     * Try to correct the slur glyphs (which have a too high circle distance) by
     * either adding a neigboring glyph (for small slurs) or removing stuck
     * glyph sections (for large slurs)
     *
     * @param glyph the spurious glyph at hand
     * @param system the containing system
     * @return true if the slur glyph has actually been fixed
     */
    public static boolean fixSpuriousSlur (Glyph      glyph,
                                           SystemInfo system)
    {
        if (glyph.getNormalizedWeight() <= constants.spuriousWeightThreshold.getValue()) {
            return fixSmallSlur(glyph, system);
        } else {
            return fixLargeSlur(glyph, system);
        }
    }

    //----------------------//
    // getMaxCircleDistance //
    //----------------------//
    public static double getMaxCircleDistance ()
    {
        return constants.maxCircleDistance.getValue();
    }

    //--------------//
    // fixLargeSlur //
    //--------------//
    /**
     * For large glyphs, we suspect a slur with a stuck object. So the strategy
     * is to rebuild the true Slur portions from the underlying sections. These
     * "good" seections are put into the "kept" collection. Sections left over
     * are put into the "left" collections in order to be used to rebuild the
     * stuck object(s).
     *
     * <p>The method by itself does not build the new slur glyph, this task must
     * be done by the caller.
     *
     * @param slur the spurious slur slur
     * @return true if we have been able to find a convenient slur
     */
    private static boolean fixLargeSlur (Glyph      slur,
                                         SystemInfo system)
    {
        /**
         * Sections are first ordered by decreasing weight and continuously
         * tested via the distance to the best approximating circle.  Sections
         * whose weight is under a given threshold are appended to the slur only
         * if the resulting circle distance is lower than before appending them.
         */
        if (logger.isFineEnabled()) {
            logger.finest("fixing Large Slur for glyph #" + slur.getId());
        }

        int                minChunkWeight = system.getScoreSystem()
                                                  .getScale()
                                                  .toPixels(
            constants.minChunkWeight);

        // Get a COPY of the member list */
        List<GlyphSection> members = new ArrayList<GlyphSection>(
            slur.getMembers());

        // Sort by decreasing weight
        Collections.sort(
            members,
            new Comparator<GlyphSection>() {
                    public int compare (GlyphSection s1,
                                        GlyphSection s2)
                    {
                        return Integer.signum(s2.getWeight() - s1.getWeight());
                    }
                });

        // Find the suitable seed, which is chosen as the section with best
        // circle distance among the sections whose weight is significant
        GlyphSection seedSection = null;
        double       seedDist = Double.MAX_VALUE;

        for (GlyphSection seed : members) {
            if (seed.getWeight() >= minChunkWeight) {
                Circle circle = computeCircle(Arrays.asList(seed));
                double dist = circle.getDistance();

                if (dist < seedDist) {
                    seedDist = dist;
                    seedSection = seed;
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.finest("Seed section is " + seedSection);
        }

        List<GlyphSection> kept = new ArrayList<GlyphSection>();
        List<GlyphSection> left = new ArrayList<GlyphSection>();

        List<GlyphSection> tried = new ArrayList<GlyphSection>();
        double             distThreshold = constants.maxCircleDistance.getValue();
        double             bestDistance = distThreshold;

        kept.add(seedSection); // We impose the seed

        for (GlyphSection section : members) {
            if (section == seedSection) {
                continue;
            }

            // Make the policy more strict when dealing with small sections
            if (section.getWeight() < minChunkWeight) {
                distThreshold = bestDistance;
            }

            if (logger.isFineEnabled()) {
                logger.finest("Trying " + section);
            }

            // Try a circle
            tried.clear();
            tried.addAll(kept);
            tried.add(section);

            try {
                Circle circle = computeCircle(tried);
                double distance = circle.getDistance();

                if (logger.isFineEnabled()) {
                    logger.finest("dist=" + distance);
                }

                if (distance <= distThreshold) {
                    kept.add(section);
                    bestDistance = distance;

                    if (logger.isFineEnabled()) {
                        logger.finest("Keep " + section);
                    }
                } else {
                    left.add(section);

                    if (logger.isFineEnabled()) {
                        logger.finest("Discard " + section);
                    }
                }
            } catch (Exception ex) {
                left.add(section);

                if (logger.isFineEnabled()) {
                    logger.finest(ex.getMessage() + " w/ " + section);
                }
            }
        }

        if (kept.size() > 0) {
            GlyphsBuilder builder = system.getScoreSystem()
                                          .getScore()
                                          .getSheet()
                                          .getGlyphsBuilder();

            // Build new slur glyph with sections kept
            Glyph newGlyph = new Stick();

            for (GlyphSection section : kept) {
                newGlyph.addSection(section, /* link => */
                                    true);
            }

            newGlyph.setShape(Shape.SLUR);
            newGlyph = builder.insertGlyph(newGlyph, system);

            // Remove former slur glyph
            builder.removeGlyph(slur, system, /*cutSections=>*/
                                false);

            // Free the sections left over
            for (GlyphSection section : left) {
                section.setGlyph(null);
            }

            if (logger.isFineEnabled()) {
                Circle circle = computeCircle(kept);
                double distance = circle.getDistance();
                logger.finest(
                    "Built slur #" + newGlyph.getId() + " distance=" +
                    (float) distance + " sections=" + Section.toString(kept));

                logger.fine(
                    "Fixed large slur #" + slur.getId() + " as smaller #" +
                    newGlyph.getId());
            }

            return true;
        } else {
            logger.warning(
                system.getScoreSystem().getContextString() +
                " No section left from large slur #" + slur.getId());

            return false;
        }
    }

    //--------------//
    // fixSmallSlur //
    //--------------//
    /**
     * For small glyphs, we suspect a slur segmented by a barline for
     * example. The strategy is then to try to build a compound glyph with
     * neighboring glyphs (either another slur, or a clutter), and test the
     * distance to the resulting best approximating circle.
     *
     * @param slur the spurious slur glyph
     * @return true if we have been able to build a convenient slur
     */
    private static boolean fixSmallSlur (Glyph      slur,
                                         SystemInfo system)
    {
        if (logger.isFineEnabled()) {
            logger.finest("fixing Small Slur for glyph #" + slur.getId());
        }

        SlurCompoundAdapter adapter = new SlurCompoundAdapter(
            system.getScoreSystem().getScale());

        // Collect glyphs suitable for participating in compound building
        List<Glyph>         suitables = new ArrayList<Glyph>(
            system.getGlyphs().size());

        for (Glyph g : system.getGlyphs()) {
            if ((g != slur) && adapter.isSuitable(g)) {
                suitables.add(g);
            }
        }

        // Sort suitable glyphs by decreasing weight (BOF)
        Collections.sort(
            suitables,
            new Comparator<Glyph>() {
                    public int compare (Glyph o1,
                                        Glyph o2)
                    {
                        return o2.getWeight() - o1.getWeight();
                    }
                });

        // Process that slur, looking at neighbors
        Glyph compound = system.getScoreSystem()
                               .getScore()
                               .getSheet()
                               .getGlyphInspector()
                               .tryCompound(slur, suitables, adapter);

        if (compound != null) {
            compound.setShape(Shape.SLUR);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Fixed small slur #" + slur.getId() + " as compound #" +
                    compound.getId());
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
        /** Maximum distance to approximating circle for a slur */
        Scale.Fraction maxCircleDistance = new Scale.Fraction(
            0.12,
            "Maximum distance to approximating circle" + " for a slur");

        /** Normalized weight threshold between small and large spurious slurs */
        Scale.AreaFraction spuriousWeightThreshold = new Scale.AreaFraction(
            1.5,
            "Normalized weight threshold between small and large spurious" +
            " slurs");

        /** Minimum weight of a chunk to be part of slur computation */
        Scale.AreaFraction minChunkWeight = new Scale.AreaFraction(
            0.5,
            "Minimum weight of a chunk to be part of slur" + " computation");

        /** Extension abscissa when looking for slur compound */
        Scale.Fraction slurBoxDx = new Scale.Fraction(
            0.3,
            "Extension abscissa when looking for slur compound");

        /** Extension ordinate when looking for slur compound */
        Scale.Fraction slurBoxDy = new Scale.Fraction(
            0.2,
            "Extension ordinate when looking for slur compound");
    }

    //---------------------//
    // SlurCompoundAdapter //
    //---------------------//
    /**
     * Class <code>SlurCompoundAdapter</code> is a CompoundAdapter meant to process a
     * small slur.
     */
    private static class SlurCompoundAdapter
        implements GlyphInspector.CompoundAdapter
    {
        /** The scale around the slur */
        private final Scale scale;

        /** The seed being considered */
        private Glyph seed;

        public SlurCompoundAdapter (Scale scale)
        {
            this.scale = scale;
        }

        @Implement(GlyphInspector.CompoundAdapter.class)
        public int getBoxDx ()
        {
            return scale.toPixels(constants.slurBoxDx);
        }

        @Implement(GlyphInspector.CompoundAdapter.class)
        public int getBoxDy ()
        {
            return scale.toPixels(constants.slurBoxDy);
        }

        @Implement(GlyphInspector.CompoundAdapter.class)
        public boolean isSuitable (Glyph glyph)
        {
            if (!glyph.isKnown()) {
                return true;
            }

            if (glyph.getShape() == Shape.SLUR) {
                Circle circle = SlurGlyph.computeCircle(glyph);

                return !circle.isValid(SlurGlyph.getMaxCircleDistance());
            }

            return (!glyph.isManualShape() &&
                   (glyph.getShape() == Shape.CLUTTER)) ||
                   (glyph.getDoubt() >= GlyphInspector.getMinCompoundPartDoubt());
        }

        @Implement(GlyphInspector.CompoundAdapter.class)
        public boolean isValid (Glyph compound)
        {
            // Look for a circle
            Circle circle = computeCircle(compound);

            return circle.isValid(SlurGlyph.getMaxCircleDistance());
        }

        public void setSeed (Glyph seed)
        {
            this.seed = seed;
        }
    }
}
