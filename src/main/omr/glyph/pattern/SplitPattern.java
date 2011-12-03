//----------------------------------------------------------------------------//
//                                                                            //
//                          S p l i t P a t t e r n                           //
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

import omr.glyph.Evaluation;
import omr.glyph.GlyphNetwork;
import omr.glyph.GlyphSignature;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphComposition.Linking;

import omr.lag.Section;

import omr.log.Logger;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class {@code SplitPattern} tries to split large unknown glyphs into two
 * valid chunks.
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
    private static final Logger logger = Logger.getLogger(SplitPattern.class);

    /** Set of shapes not accepted for glyph chunks. TODO: expand the set */
    private static final EnumSet<Shape> invalidShapes = EnumSet.of(
        Shape.CLUTTER,
        Shape.NOISE,
        Shape.STRUCTURE);

    //~ Instance fields --------------------------------------------------------

    private final double       minGlyphWeight;
    private final GlyphNetwork evaluator = GlyphNetwork.getInstance();

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // SplitPattern //
    //--------------//
    /**
     * Creates a new SplitPattern object.
     * @param system the dedicated system
     */
    public SplitPattern (SystemInfo system)
    {
        super("Split", system);

        minGlyphWeight = scale.toPixels(constants.minGlyphWeight);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    /**
     * In the related system, look for heavy unknown glyphs which might be
     * composed of several glyphs, each with a valid shape.
     * @return the number of glyphs actually split
     */
    @Override
    public int runPattern ()
    {
        int nb = 0;

        for (Glyph glyph : system.getGlyphs()) {
            if (!glyph.isActive() ||
                glyph.isKnown() ||
                (glyph.getWeight() < minGlyphWeight)) {
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
     * @param glyph to glyph to expand
     * @param section the section to cehck for expandion pf the glyph
     * @param master the master glyph which contains the section candidates
     */
    private void expand (Glyph   glyph,
                         Section section,
                         Glyph   master)
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
     * Try to split the provided master glyph into two valid glyph chunks.
     * @param master the master glyph to split
     * @return true if successful
     */
    private boolean splitGlyph (Glyph master)
    {
        if (master.isVip()) {
            logger.info("Trying to split G#" + master.getId());
        }

        List<Split> splits = new ArrayList<Split>();

        // Retrieve all binary splits of this glyph
        for (Section seed : master.getMembers()) {
            for (Section s : master.getMembers()) {
                s.setProcessed(false);
            }

            seed.setProcessed(true); // To not use this one

            Split         split = new Split(master, seed);
            List<Section> others = new ArrayList<Section>();
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

            if (split.sigs.size() == 2) {
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

        if (master.isVip() || logger.isFineEnabled()) {
            logger.info("Checking " + bestSplit);
        }

        // Check whether each of the chunks can be assigned a valid shape
        for (Glyph chunk : bestSplit.sigs.values()) {
            system.computeGlyphFeatures(chunk);

            Evaluation vote = evaluator.vote(
                chunk,
                Grades.partMinGrade,
                system);

            if ((vote == null) || invalidShapes.contains(vote.shape)) {
                if (master.isVip() || logger.isFineEnabled()) {
                    logger.info("No valid shape for chunk " + chunk);
                }

                return false;
            } else {
                if (logger.isFineEnabled()) {
                    logger.fine(vote + " for chunk " + chunk);
                }

                chunk.setEvaluation(vote);
            }
        }

        // Now actually perform the split!
        if (master.isVip() || logger.isFineEnabled()) {
            logger.info(system.getLogPrefix() + "Performing " + bestSplit);
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
            1.3,
            "Minimum normalized glyph weight to look for split");
    }

    //-------//
    // Split //
    //-------//
    /**
     * Records information about a potential split of a master glyph
     */
    private static class Split
        implements Comparable<Split>
    {
        //~ Instance fields ----------------------------------------------------

        // Master glyph
        private final Glyph                      master;

        // The section used for the split
        private final Section                    seed;

        // The resulting glyph chunks
        private final Map<GlyphSignature, Glyph> sigs = new HashMap<GlyphSignature, Glyph>();

        //~ Constructors -------------------------------------------------------

        /**
         * Create a split information.
         * @param master the master glyph to be split
         * @param seed the section where split would be performed
         */
        public Split (Glyph   master,
                      Section seed)
        {
            this.master = master;
            this.seed = seed;
        }

        //~ Methods ------------------------------------------------------------

        public int compareTo (Split that)
        {
            // Bigger first!
            return Integer.signum(
                that.getLowerWeight() - this.getLowerWeight());
        }

        public void register (SystemInfo system)
        {
            // Include section seed into the smaller chunk
            Entry<GlyphSignature, Glyph> lowEntry = getLowerEntry();
            Glyph                        smallerGlyph = lowEntry.getValue();
            sigs.remove(lowEntry.getKey());
            smallerGlyph.addSection(seed, Linking.NO_LINK_BACK);
            sigs.put(smallerGlyph.getSignature(), smallerGlyph);

            // Register the chunks (copy needed to avoid concurrent modifs)
            Set<Entry<GlyphSignature, Glyph>> entries = new HashSet<Entry<GlyphSignature, Glyph>>(
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
            sb.append(" @S")
              .append(seed.isVertical() ? "V" : "H")
              .append(seed.getId());

            for (Entry<GlyphSignature, Glyph> entry : sigs.entrySet()) {
                Glyph glyph = entry.getValue();
                sb.append(" #")
                  .append(glyph.getId());

                Evaluation eval = glyph.getEvaluation();

                if (eval != null) {
                    sb.append(":")
                      .append(eval);
                }
            }

            sb.append("}");

            return sb.toString();
        }

        private Entry<GlyphSignature, Glyph> getLowerEntry ()
        {
            Entry<GlyphSignature, Glyph> best = null;

            for (Entry<GlyphSignature, Glyph> entry : sigs.entrySet()) {
                if ((best == null) ||
                    (best.getKey()
                         .getWeight() > entry.getKey()
                                             .getWeight())) {
                    best = entry;
                }
            }

            return best;
        }

        private int getLowerWeight ()
        {
            return getLowerEntry()
                       .getKey()
                       .getWeight();
        }
    }
}
