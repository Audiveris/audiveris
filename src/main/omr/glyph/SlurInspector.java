//----------------------------------------------------------------------------//
//                                                                            //
//                         S l u r I n s p e c t o r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.ConstantSet;

import omr.glyph.facets.BasicStick;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.log.Logger;

import omr.math.Circle;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Implement;

import java.util.*;

/**
 * Class <code>SlurInspector</code> encapsulates physical processing dedicated
 * to inspection of glyphs with SLUR shape at a dedicated system level.
 *
 * @author Hervé Bitteur
 */
public class SlurInspector
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SlurInspector.class);

    //~ Instance fields --------------------------------------------------------

    /** Dedicated system */
    private final SystemInfo system;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SlurInspector //
    //---------------//
    /**
     * Creates a new SlurInspector object.
     *
     * @param system The dedicated system
     */
    public SlurInspector (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ----------------------------------------------------------------

    //----------------------//
    // getMaxCircleDistance //
    //----------------------//
    /**
     * Report the maximum distance to approximating circle that is acceptable
     * for a slur
     * @return the maximum circle distance, expressed as interline fraction
     */
    public static double getMaxCircleDistance ()
    {
        return constants.maxCircleDistance.getValue();
    }

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

    //--------------//
    // fixLargeSlur //
    //--------------//
    /**
     * For large glyphs, we suspect a slur with a stuck object. So the strategy
     * is to rebuild the true Slur portions from the underlying sections. These
     * "good" sections are put into the "kept" collection. Sections left over
     * are put into the "left" collection in order to be used to rebuild the
     * stuck object(s).
     *
     * <p>The method by itself does not build the new oldSlur glyph, this task must
     * be done by the caller.
     *
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
         */
        if (logger.isFineEnabled()) {
            logger.fine("fixing Large Slur for glyph #" + oldSlur.getId());
        }

        final int          interline = system.getSheet()
                                             .getInterline();
        final int          minChunkWeight = system.getScoreSystem()
                                                  .getScale()
                                                  .toPixels(
            constants.minChunkWeight);

        // Get a COPY of the member list */
        List<GlyphSection> members = new ArrayList<GlyphSection>(
            oldSlur.getMembers());

        // Sort by decreasing weight
        Collections.sort(members, GlyphSection.reverseWeightComparator);

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
            logger.fine("Seed section is " + seedSection);
        }

        // If no significant section has been found, just give up
        if (seedSection == null) {
            return null;
        }

        List<GlyphSection> kept = new ArrayList<GlyphSection>();
        List<GlyphSection> left = new ArrayList<GlyphSection>();

        List<GlyphSection> tried = new ArrayList<GlyphSection>();
        double             distThreshold = constants.maxCircleDistance.getValue();
        double             bestDistance = distThreshold;

        // We impose the seed
        kept.add(seedSection);

        for (GlyphSection section : members) {
            if (section == seedSection) {
                continue;
            }

            // Make the policy more strict when dealing with small sections
            if (section.getWeight() < minChunkWeight) {
                distThreshold = bestDistance;
            }

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

        if (!kept.isEmpty()) {
            // Make sure we do have a suitable slur
            try {
                Circle circle = computeCircle(kept);
                double distance = circle.getDistance();

                if (logger.isFineEnabled()) {
                    logger.fine("Final dist=" + distance);
                }

                if (distance <= distThreshold) {
                    // Build new slur glyph with sections kept
                    Glyph newGlyph = new BasicStick(interline);

                    for (GlyphSection section : kept) {
                        newGlyph.addSection(section, /* link => */
                                            true);
                    }

                    newGlyph.setShape(Shape.SLUR);

                    // Beware, the newGlyph may now belong to a different system
                    SystemInfo newSystem = system.getSheet()
                                                 .getSystemOf(newGlyph);
                    newGlyph = newSystem.addGlyph(newGlyph);

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Built slur #" + newGlyph.getId() + " distance=" +
                            (float) distance + " sections=" +
                            Section.toString(kept));

                        logger.fine(
                            "Fixed large slur #" + oldSlur.getId() +
                            " as smaller #" + newGlyph.getId());
                    }

                    return newGlyph;
                } else {
                    if (logger.isFineEnabled()) {
                        logger.fine("Giving up slur w/ " + kept);
                    }

                    left.addAll(kept);

                    return null;
                }
            } catch (Exception ex) {
                left.addAll(kept);

                return null;
            } finally {
                // Remove former oldSlur glyph
                oldSlur.setShape(null);
                system.removeGlyph(oldSlur);

                // Free the sections left over
                for (GlyphSection section : left) {
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
     * Try to correct the oldSlur glyphs (which have a too high circle distance) by
     * either adding a neigboring glyph (for small slurs) or removing stuck
     * glyph sections (for large slurs)
     *
     * @param glyph the spurious glyph at hand
     * @return the fixed slur glyph, if any, otherwise null
     */
    public Glyph fixSpuriousSlur (Glyph glyph)
    {
        if (glyph.getNormalizedWeight() <= constants.spuriousWeightThreshold.getValue()) {
            return fixSmallSlur(glyph);
        } else {
            return fixLargeSlur(glyph);
        }
    }

    //----------------//
    // runSlurPattern //
    //----------------//
    /**
     * Process all the oldSlur glyphs in the given system, and try to correct the
     * spurious ones if any
     * @return the number of slurs fixed
     */
    public int runSlurPattern ()
    {
        int         modifs = 0;

        // First, make a list of all slur glyphs to be checked in this system
        // (So as to free the system glyph list for on-the-fly modifications)
        List<Glyph> slurs = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            if ((glyph.getShape() == Shape.SLUR) && !glyph.isManualShape()) {
                slurs.add(glyph);
            }
        }

        // Then verify each slur seed in turn
        for (Glyph seed : slurs) {
            // Check this slur has not just been 'merged' with another one
            if (seed.isActive()) {
                if (logger.isFineEnabled()) {
                    logger.fine("Verifying slur glyph#" + seed.getId());
                }

                if (!computeCircle(seed)
                         .isValid(getMaxCircleDistance())) {
                    try {
                        Glyph newSlur = fixSpuriousSlur(seed);

                        if (logger.isFineEnabled()) {
                            logger.fine("Fixed " + seed + " into " + newSlur);
                        }

                        modifs++;
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
     * @return the fixed slur glyph, if any
     */
    private Glyph fixSmallSlur (Glyph oldSlur)
    {
        if (logger.isFineEnabled()) {
            logger.finest("fixing Small Slur for glyph #" + oldSlur.getId());
        }

        SlurCompoundAdapter adapter = new SlurCompoundAdapter(
            system.getScoreSystem().getScale());

        // Collect glyphs suitable for participating in compound building
        List<Glyph>         suitables = new ArrayList<Glyph>(
            system.getGlyphs().size());

        for (Glyph g : system.getGlyphs()) {
            if ((g != oldSlur) && adapter.isSuitable(g)) {
                suitables.add(g);
            }
        }

        // Sort suitable glyphs by decreasing weight
        Collections.sort(suitables, Glyphs.reverseWeightComparator);

        // Process that slur, looking at neighbors
        Glyph compound = system.tryCompound(oldSlur, suitables, adapter);

        if (compound != null) {
            // Beware, the compound may now belong to a different system
            SystemInfo newSystem = system.getSheet()
                                         .getSystemOf(compound);
            Glyph      newSlur = newSystem.addGlyph(compound);
            newSlur.setShape(Shape.SLUR);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Fixed small slur #" + oldSlur.getId() + " as compound #" +
                    newSlur.getId());
            }

            return newSlur;
        } else {
            oldSlur.setShape(null); // Since this slur has not been fixed

            return null;
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

        /** Maximum distance to approximating circle for a slur */
        Scale.Fraction maxCircleDistance = new Scale.Fraction(
            0.16,
            "Maximum distance to approximating circle" + " for a slur");

        /** Normalized weight threshold between small and large spurious slurs */
        Scale.AreaFraction spuriousWeightThreshold = new Scale.AreaFraction(
            1.5,
            "Normalized weight threshold between small and large spurious" +
            " slurs");

        /** Minimum weight of a chunk to be part of oldSlur computation */
        Scale.AreaFraction minChunkWeight = new Scale.AreaFraction(
            0.5,
            "Minimum weight of a chunk to be part of slur" + " computation");

        /** Extension abscissa when looking for oldSlur compound */
        Scale.Fraction slurBoxDx = new Scale.Fraction(
            0.3,
            "Extension abscissa when looking for slur compound");

        /** Extension ordinate when looking for oldSlur compound */
        Scale.Fraction slurBoxDy = new Scale.Fraction(
            0.2,
            "Extension ordinate when looking for slur compound");
    }

    //---------------------//
    // SlurCompoundAdapter //
    //---------------------//
    /**
     * Class <code>SlurCompoundAdapter</code> is a CompoundAdapter meant to process a
     * small oldSlur.
     */
    private static class SlurCompoundAdapter
        implements GlyphInspector.CompoundAdapter
    {
        //~ Instance fields ----------------------------------------------------

        /** The scale around the oldSlur */
        private final Scale scale;

        //~ Constructors -------------------------------------------------------

        public SlurCompoundAdapter (Scale scale)
        {
            this.scale = scale;
        }

        //~ Methods ------------------------------------------------------------

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
            if (!glyph.isActive()) { // Safer

                return false;
            }

            if (!glyph.isKnown()) {
                return true;
            }

            if (glyph.getShape() == Shape.SLUR) {
                try {
                    Circle circle = SlurInspector.computeCircle(glyph);

                    return !circle.isValid(getMaxCircleDistance());
                } catch (Exception ex) {
                    logger.warning(
                        "Cannot compute circle for slur #" + glyph.getId(),
                        ex);

                    return true; // Yeah, it is a candidate !!!
                }
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

            return circle.isValid(getMaxCircleDistance());
        }
    }
}
