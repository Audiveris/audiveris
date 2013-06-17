//----------------------------------------------------------------------------//
//                                                                            //
//                          S p l i t P a t t e r n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphNetwork;
import omr.glyph.GlyphSignature;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphComposition.Linking;

import omr.lag.Section;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

/**
 * Class {@code SplitPattern} tries to split large unknown glyphs into
 * two valid chunks.
 *
 * @author Hervé Bitteur
 */
public class SplitPattern
        extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(SplitPattern.class);

    /** Set of shapes not accepted for glyph chunks. TODO: expand the set */
    private static final EnumSet<Shape> invalidShapes = EnumSet.of(
            Shape.CLUTTER,
            Shape.NOISE);

    //~ Instance fields --------------------------------------------------------
    // Scale-dependent parameters
    private final double minGlyphWeight;

    private final double minChunkWeight;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // SplitPattern //
    //--------------//
    /**
     * Creates a new SplitPattern object.
     *
     * @param system the dedicated system
     */
    public SplitPattern (SystemInfo system)
    {
        super("Split", system);

        minGlyphWeight = scale.toPixels(constants.minGlyphWeight);
        minChunkWeight = scale.toPixels(constants.minChunkWeight);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    /**
     * In the related system, look for heavy unknown glyphs which might
     * be composed of several glyphs, each with a valid shape.
     *
     * @return the number of glyphs actually split
     */
    @Override
    public int runPattern ()
    {
        int nb = 0;

        for (Glyph glyph : system.getGlyphs()) {
            if (!glyph.isActive()
                || glyph.isKnown()
                || (glyph.getStemNumber() == 0)
                || (glyph.getWeight() < minGlyphWeight)) {
                continue;
            }

            if (splitGlyph(glyph)) {
                nb++;
            }
        }

        return nb;
    }

    //--------//
    // expand //
    //--------//
    /**
     * Try to expand the provided glyph with the provided section.
     *
     * @param glyph   to glyph to expand
     * @param section the section to cehck for expandion pf the glyph
     * @param master  the master glyph which contains the section candidates
     */
    private void expand (Glyph glyph,
                         Section section,
                         Glyph master)
    {
        // Check whether this section is suitable to expand the glyph
        if (section.isProcessed() || (section.getGlyph() != master)) {
            return;
        }

        section.setProcessed(true);

        glyph.addSection(section, Glyph.Linking.NO_LINK_BACK);

        // Check recursively all sections linked to this one...

        // Incoming ones
        for (Section source : section.getSources()) {
            expand(glyph, source, master);
        }

        // Outgoing ones
        for (Section target : section.getTargets()) {
            expand(glyph, target, master);
        }

        // Sections from other orientation
        for (Section other : section.getOppositeSections()) {
            expand(glyph, other, master);
        }
    }

    //------------//
    // splitGlyph //
    //------------//
    /**
     * Try to split the provided master glyph into two valid glyph
     * chunks.
     *
     * @param master the master glyph to split
     * @return true if successful
     */
    private boolean splitGlyph (Glyph master)
    {
        if (master.isVip()) {
            logger.info("Trying to split G#{}", master.getId());
        }

        List<Split> splits = new ArrayList<>();

        // Retrieve all binary splits of this glyph
        for (Section seed : master.getMembers()) {
            for (Section s : master.getMembers()) {
                s.setProcessed(false);
            }

            seed.setProcessed(true); // To not use this one

            Split split = new Split(master, seed);
            List<Section> others = new ArrayList<>();
            others.addAll(seed.getSources());
            others.addAll(seed.getTargets());
            others.addAll(seed.getOppositeSections());

            for (Section s : others) {
                if ((s.getGlyph() == master) && !s.isProcessed()) {
                    Glyph g = new BasicGlyph(scale.getInterline());
                    expand(g, s, master);

                    split.sigs.put(g.getSignature(), g);
                }
            }

            // Check if we have exactly two significant chunks
            // (neglecting very small others)
            int count = split.sigs.size();

            for (GlyphSignature sig : split.sigs.keySet()) {
                if (sig.getWeight() < minChunkWeight) {
                    count--;
                }
            }

            if (count == 2) {
                if (master.isVip()) {
                    logger.info("Split candidate: {}", split);
                }

                splits.add(split);
            }
        }

        if (splits.isEmpty()) {
            return false;
        }

        // Pickup the more weightwise balanced split
        Collections.sort(splits);

        Split bestSplit = splits.get(0);

        bestSplit.register(system);

        if (master.isVip() || logger.isDebugEnabled()) {
            logger.info("Checking {}", bestSplit);
        }

        // Check whether each of the chunks can be assigned a valid shape
        for (Glyph chunk : bestSplit.sigs.values()) {
            if (chunk.getWeight() < minChunkWeight) {
                continue;
            }

            system.computeGlyphFeatures(chunk);

            Evaluation vote = GlyphNetwork.getInstance().vote(
                    chunk,
                    system,
                    Grades.partMinGrade);

            if ((vote == null) || invalidShapes.contains(vote.shape)) {
                if (master.isVip() || logger.isDebugEnabled()) {
                    logger.info("No valid shape for chunk {}", chunk);
                }
            } else {
                if (master.isVip() || logger.isDebugEnabled()) {
                    logger.info("{} for chunk {}", vote, chunk);
                }

                chunk.setEvaluation(vote);
            }
        }

        // Now actually perform the split!
        if (master.isVip() || logger.isDebugEnabled()) {
            logger.info("{}Performing {}", system.getLogPrefix(), bestSplit);
        }

        for (Glyph glyph : bestSplit.sigs.values()) {
            system.addGlyph(glyph);
        }

        return true;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.AreaFraction minGlyphWeight = new Scale.AreaFraction(
                1.0,
                "Minimum normalized glyph weight to look for split");

        //
        Scale.AreaFraction minChunkWeight = new Scale.AreaFraction(
                0.025,
                "Minimum normalized weight of a chunk to be part of a split");

    }

    //-------//
    // Split //
    //-------//
    /**
     * Records information about a potential split of a master glyph.
     */
    private static class Split
            implements Comparable<Split>
    {
        //~ Instance fields ----------------------------------------------------

        // Master glyph
        private final Glyph master;

        // The section used for the split
        private final Section seed;

        // The resulting glyph chunks, kept sorted by (increasing) weight
        private final SortedMap<GlyphSignature, Glyph> sigs = new TreeMap<>();

        //~ Constructors -------------------------------------------------------
        /**
         * Create a split information.
         *
         * @param master the master glyph to be split
         * @param seed   the section where split would be performed
         */
        public Split (Glyph master,
                      Section seed)
        {
            this.master = master;
            this.seed = seed;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int compareTo (Split that)
        {
            // Bigger first!
            return Integer.signum(
                    that.getLowerWeight() - this.getLowerWeight());
        }

        public void register (SystemInfo system)
        {
            // Include section seed into the second largest chunk
            Entry<GlyphSignature, Glyph> lowEntry = getSecondLargest();
            Glyph smallerGlyph = lowEntry.getValue();
            sigs.remove(lowEntry.getKey());
            smallerGlyph.addSection(seed, Linking.NO_LINK_BACK);
            sigs.put(smallerGlyph.getSignature(), smallerGlyph);

            // Register the chunks (copy needed to avoid concurrent modifs)
            Set<Entry<GlyphSignature, Glyph>> entries = new HashSet<>(
                    sigs.entrySet());

            for (Entry<GlyphSignature, Glyph> entry : entries) {
                Glyph value = entry.getValue();
                Glyph glyph = system.registerGlyph(value);

                if (glyph != value) {
                    GlyphSignature key = entry.getKey();
                    sigs.remove(key);
                    sigs.put(key, glyph);
                }
            }
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{SplitOf#");
            sb.append(master.getId());
            sb.append(" @S").append(seed.isVertical() ? "V" : "H").append(seed.
                    getId());

            for (Entry<GlyphSignature, Glyph> entry : sigs.entrySet()) {
                Glyph glyph = entry.getValue();
                sb.append(" #").append(glyph.getId());

                Evaluation eval = glyph.getEvaluation();

                if (eval != null) {
                    sb.append(":").append(eval);
                }
            }

            sb.append("}");

            return sb.toString();
        }

        private int getLowerWeight ()
        {
            return getSecondLargest().getKey().getWeight();
        }

        private Entry<GlyphSignature, Glyph> getSecondLargest ()
        {
            // The map entries are sorted from small to large key
            Entry<GlyphSignature, Glyph> prevEntry = null;
            GlyphSignature lastKey = sigs.lastKey();

            for (Entry<GlyphSignature, Glyph> entry : sigs.entrySet()) {
                if (entry.getKey() == lastKey) {
                    return prevEntry;
                } else {
                    prevEntry = entry;
                }
            }

            return prevEntry;
        }
    }
}
