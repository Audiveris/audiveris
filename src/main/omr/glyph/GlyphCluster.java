//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G l y p h C l u s t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code GlyphCluster} handles a cluster of connected glyphs, to retrieve all
 * acceptable compounds built on subsets of these glyphs.
 * <p>
 * The processing of any given subset consists in the following:<ol>
 * <li>Build the compound of chosen vertices, and record acceptable evaluations.</li>
 * <li>Build the set of new reachable vertices.</li>
 * <li>For each reachable vertex, recursively process the new set composed of current set + the
 * reachable vertex.</li></ol>
 *
 * @author Hervé Bitteur
 */
public class GlyphCluster
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphCluster.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Environment adapter. */
    private final Adapter adapter;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Cluster object, with an adapter to the environment.
     *
     * @param adapter the environment adapter
     */
    public GlyphCluster (Adapter adapter)
    {
        this.adapter = adapter;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // decompose //
    //-----------//
    /**
     * Identify all acceptable compounds within the cluster and evaluate them.
     */
    public void decompose ()
    {
        final Set<Glyph> considered = new HashSet<Glyph>(); // Parts considered so far
        final List<Glyph> seeds = adapter.getParts();
        Collections.sort(seeds, Glyph.byId); // It's easier to debug
        logger.debug("Decomposing {}", Glyphs.toString("cluster", seeds));

        for (Glyph seed : seeds) {
            if (seed.isVip()) {
                logger.info("   Seed #{}", seed.getId());
            }

            considered.add(seed);
            process(Collections.singleton(seed), considered);
        }
    }

    /**
     * Retrieve all parts at acceptable distance from at least one member of the
     * provided set.
     *
     * @param set the provided set
     * @return all the parts reachable from the set
     */
    private Set<Glyph> getOutliers (Set<Glyph> set)
    {
        Set<Glyph> outliers = new HashSet<Glyph>();

        for (Glyph part : set) {
            outliers.addAll(adapter.getNeighbors(part));
        }

        outliers.removeAll(set);

        return outliers;
    }

    /**
     * Process the provided set of parts.
     *
     * @param set        the current parts
     * @param considered all parts considered so far (current parts plus discarded ones)
     */
    private void process (Set<Glyph> set,
                          Set<Glyph> considered)
    {
        // Check what we have got
        int weight = Glyphs.weightOf(set);

        if (adapter.isWeightAcceptable(weight)) {
            // Build compound and get acceptable evaluations for the compound
            Glyph compound = (set.size() == 1) ? set.iterator().next()
                    : adapter.getNest().buildGlyph(set, true, Glyph.Linking.NO_LINK);

            // Create all acceptable inters, if any, for the compound
            adapter.evaluateGlyph(compound);
        } else {
            logger.debug("Too low weight {} for {}", weight, set);
        }

        // Then, identify all outliers immediately reachable from the compound
        Set<Glyph> outliers = getOutliers(set);
        outliers.removeAll(considered);

        if (outliers.isEmpty()) {
            return; // No further growth is possible
        }

        Rectangle setBox = Glyphs.getBounds(set);
        Set<Glyph> newConsidered = new HashSet<Glyph>(considered);

        for (Glyph outlier : outliers) {
            newConsidered.add(outlier);

            // Check appending this atom does not make the resulting symbol too wide or too high
            Rectangle symBox = outlier.getBounds().union(setBox);

            if (adapter.isSizeAcceptable(symBox)) {
                Set<Glyph> largerSet = new HashSet<Glyph>(set);
                largerSet.add(outlier);
                process(largerSet, newConsidered);
            }
        }
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    public static interface Adapter
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Evaluate a provided glyph and create all acceptable inter instances.
         *
         * @param glyph the glyph to evaluate
         */
        void evaluateGlyph (Glyph glyph);

        /**
         * Report the neighboring parts of the provided one.
         *
         * @param part the provided part
         * @return the neighbors
         */
        List<Glyph> getNeighbors (Glyph part);

        /**
         * Report the glyph nest to populate.
         *
         * @return the hosting glyph nest
         */
        GlyphNest getNest ();

        /**
         * Report the parts to play with.
         *
         * @return the parts to assemble
         */
        List<Glyph> getParts ();

        /**
         * Check whether symbol size is acceptable.
         *
         * @param box symbol bounding box
         * @return true if OK
         */
        boolean isSizeAcceptable (Rectangle box);

        /**
         * Check whether symbol weight is acceptable.
         *
         * @param weight symbol weight
         * @return true if OK
         */
        boolean isWeightAcceptable (int weight);
    }
}
