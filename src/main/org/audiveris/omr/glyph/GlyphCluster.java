//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G l y p h C l u s t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.glyph;

import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code GlyphCluster} handles a cluster of connected glyphs, to retrieve all
 * acceptable compounds built on subsets of these glyphs.
 * <p>
 * The processing of any given subset consists in the following:
 * <ol>
 * <li>Build the compound of chosen vertices, and record acceptable evaluations.</li>
 * <li>Build the set of new reachable vertices.</li>
 * <li>For each reachable vertex, recursively process the new set composed of current set + the
 * reachable vertex.</li>
 * </ol>
 * TODO: implement a non-recursive version for better efficiency?
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

    /** Group, if any, to be assigned to created glyphs. */
    private final GlyphGroup group;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Cluster object, with an adapter to the environment.
     *
     * @param adapter the environment adapter
     * @param group   group to be assigned, if any
     */
    public GlyphCluster (Adapter adapter,
                         GlyphGroup group)
    {
        this.adapter = adapter;
        this.group = group;
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
        final Set<Glyph> considered = new LinkedHashSet<>(); // Parts considered so far

        //TODO: we could truncate this list by discarding the smallest items
        // since a too large list would result in explosion of combinations
        final List<Glyph> seeds = adapter.getParts();
        Collections.sort(seeds, Glyphs.byReverseWeight);

        ///logger.debug("Decomposing {}", Glyphs.ids("cluster", seeds));
        for (Glyph seed : seeds) {
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
        Set<Glyph> outliers = new LinkedHashSet<>();

        for (Glyph part : set) {
            outliers.addAll(adapter.getNeighbors(part));
        }

        outliers.removeAll(set);

        return outliers;
    }

    /**
     * Process the provided set of parts.
     *
     * @param parts (read only) the set of current parts
     * @param seen  (read only) all parts considered so far (current parts plus discarded ones)
     */
    private void process (Set<Glyph> parts,
                          Set<Glyph> seen)
    {
        ///logger.debug(" {} {} {}", set.size(), Glyphs.ids("set", set), Glyphs.ids("seen", seen));

        // Check what we have got
        final int weight = Glyphs.weightOf(parts);

        if (adapter.isTooHeavy(weight)) {
            logger.debug("Too high weight {} for {}", weight, parts);

            return;
        }

        Rectangle box = Glyphs.getBounds(parts);

        if (adapter.isTooLarge(box)) {
            logger.debug("Too large  {} for {}", box, parts);

            return;
        }

        if (!adapter.isTooLight(weight)) {
            // Build compound and get acceptable evaluations for the compound
            Glyph compound = (parts.size() > 1) ? GlyphFactory.buildGlyph(parts)
                    : parts.iterator().next();
            compound.addGroup(group);

            // Create all acceptable inters, if any, for the compound
            adapter.evaluateGlyph(compound, parts);
        } else {
            logger.debug("Too low weight {} for {}", weight, parts);
        }

        // Then, identify all outliers immediately reachable from the compound
        Set<Glyph> outliers = getOutliers(parts);
        outliers.removeAll(seen);

        if (outliers.isEmpty()) {
            return; // No further growth is possible
        }

        ///logger.debug("      {}", Glyphs.ids("outliers", outliers));
        Rectangle setBox = Glyphs.getBounds(parts);
        Set<Glyph> newConsidered = new LinkedHashSet<>(seen);

        for (Glyph outlier : outliers) {
            newConsidered.add(outlier);

            // Check appending this atom does not make the resulting symbol too wide or too high
            Rectangle symBox = outlier.getBounds().union(setBox);

            if (!adapter.isTooLarge(symBox)) {
                Set<Glyph> largerSet = new LinkedHashSet<>(parts);
                largerSet.add(outlier);
                process(largerSet, newConsidered);
            }
        }
    }

    //-------------//
    // getSubGraph //
    //-------------//
    /**
     * Extract a subgraph limited to the provided set of glyphs.
     *
     * @param set        the provided set of glyphs
     * @param graph      the global graph to extract from
     * @param checkEdges true if glyph edges may point outside the provided set.
     * @return the graph limited to glyph set and related edges
     */
    public static SimpleGraph<Glyph, GlyphLink> getSubGraph (Set<Glyph> set,
                                                             SimpleGraph<Glyph, GlyphLink> graph,
                                                             boolean checkEdges)
    {
        // Which edges should be extracted for this set?
        Set<GlyphLink> setEdges = new LinkedHashSet<>();

        for (Glyph glyph : set) {
            Set<GlyphLink> glyphEdges = graph.edgesOf(glyph);

            if (!checkEdges) {
                setEdges.addAll(glyphEdges); // Take all edges
            } else {
                // Keep only the edges that link within the set
                for (GlyphLink link : glyphEdges) {
                    Glyph opposite = Graphs.getOppositeVertex(graph, link, glyph);

                    if (set.contains(opposite)) {
                        setEdges.add(link);
                    }
                }
            }
        }

        SimpleGraph<Glyph, GlyphLink> subGraph = new SimpleGraph<>(GlyphLink.class);
        Graphs.addAllVertices(subGraph, set);
        Graphs.addAllEdges(subGraph, graph, setEdges);

        return subGraph;
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Interface to be implemented by a user of GlyphCluster.
     */
    public static interface Adapter
    {

        /**
         * Evaluate a provided glyph and create all acceptable inter instances.
         *
         * @param glyph the glyph to evaluate
         * @param parts the parts that compose this glyph
         */
        void evaluateGlyph (Glyph glyph,
                            Set<Glyph> parts);

        /**
         * Report the neighboring parts of the provided one.
         *
         * @param part the provided part
         * @return the neighbors
         */
        List<Glyph> getNeighbors (Glyph part);

        /**
         * Report the parts to play with.
         *
         * @return the parts to assemble
         */
        List<Glyph> getParts ();

        /**
         * Check whether glyph weight value is too high
         *
         * @param weight glyph weight
         * @return true if too high
         */
        boolean isTooHeavy (int weight);

        /**
         * Check whether glyph box is too large (too high values for height or width)
         *
         * @param bounds glyph bounds
         * @return true if too large
         */
        boolean isTooLarge (Rectangle bounds);

        /**
         * Check whether glyph weight value is too low
         *
         * @param weight glyph weight
         * @return true if too low
         */
        boolean isTooLight (int weight);

        /**
         * Check whether glyph box is too small (too low values for height or width)
         *
         * @param bounds glyph bounds
         * @return true if too small
         */
        boolean isTooSmall (Rectangle bounds);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    public abstract static class AbstractAdapter
            implements Adapter
    {

        /** For debug only */
        public int trials = 0;

        /** Graph of the connected glyphs, with their distance edges if any. */
        protected final SimpleGraph<Glyph, GlyphLink> graph;

        /**
         * Build an adapter from a set of parts and the maximum gap between parts.
         * The connectivity graph is built internally.
         *
         * @param parts      the parts to pick from
         * @param maxPartGap maximum distance between two parts to be directly reachable
         */
        public AbstractAdapter (Collection<Glyph> parts,
                                double maxPartGap)
        {
            this(Glyphs.buildLinks(parts, maxPartGap));
        }

        /**
         * Build an adapter for which the connectivity graph is already known (usually
         * thanks to a ConnectivityInspector to separate sub-sets up front).
         *
         * @param graph the ready-to-use connectivity (sub-)graph
         */
        public AbstractAdapter (SimpleGraph<Glyph, GlyphLink> graph)
        {
            this.graph = graph;
        }

        @Override
        public List<Glyph> getNeighbors (Glyph part)
        {
            return Graphs.neighborListOf(graph, part);
        }

        @Override
        public List<Glyph> getParts ()
        {
            return new ArrayList<>(graph.vertexSet());
        }

        @Override
        public boolean isTooHeavy (int weight)
        {
            return false;
        }

        @Override
        public boolean isTooLarge (Rectangle bounds)
        {
            return false;
        }

        @Override
        public boolean isTooLight (int weight)
        {
            return false;
        }

        @Override
        public boolean isTooSmall (Rectangle bounds)
        {
            return false;
        }
    }
}
