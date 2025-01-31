//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C h o r d s M a p p e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.math.InjectionSolver;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.util.Arrangements;
import org.audiveris.omr.util.WrappedInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.joining;

/**
 * Class <code>ChordsMapper</code> tries to voice-map incoming chords to active chords.
 *
 * @author Hervé Bitteur
 */
public class ChordsMapper
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ChordsMapper.class);

    //~ Instance fields ----------------------------------------------------------------------------

    private final List<AbstractChordInter> rookies = new ArrayList<>();

    private final List<AbstractChordInter> actives = new ArrayList<>();

    private final VoiceDistance vd;

    private final Set<ChordPair> blackList;

    private final Set<ChordPair> nextList;

    private final Set<ChordPair> mappedNext;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a <code>ChordsMapper</code> object.
     *
     * @param initialRookies incoming chords to map
     * @param initialActives available previous chords
     * @param vd             the VoiceDistance to use
     * @param blackList      known incompatibilities
     * @param nextList       explicit connections via NextInVoice
     */
    public ChordsMapper (List<AbstractChordInter> initialRookies,
                         List<AbstractChordInter> initialActives,
                         VoiceDistance vd,
                         Set<ChordPair> blackList,
                         Set<ChordPair> nextList)
    {
        this.vd = vd;
        this.blackList = blackList;
        this.nextList = nextList;

        mappedNext = processNext(initialRookies, initialActives, rookies, actives);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // distance //
    //----------//
    private int distance (AbstractChordInter rookie,
                          AbstractChordInter active,
                          StringBuilder details)
    {
        // No link to an active chord
        if (active == null) {
            if (details != null) {
                details.append("NO_LINK=").append(VoiceDistance.NO_LINK);
            }

            return VoiceDistance.NO_LINK;
        }

        // Next list
        for (ChordPair pair : nextList) {
            if (pair.active == active) {
                if (pair.rookie == rookie) {
                    if (details != null) {
                        details.append("NEXT");
                    }

                    return 0;
                }
            }
        }

        // Black list
        for (ChordPair pair : blackList) {
            if ((pair.rookie == rookie) && (pair.active == active)) {
                if (details != null) {
                    details.append("BLACK");
                }

                return VoiceDistance.INCOMPATIBLE;
            }
        }

        // Fall back using VoiceDistance
        return vd.getDistance(active, rookie, details);
    }

    //---------//
    // process //
    //---------//
    /**
     * Perform the mapping.
     *
     * @return the list of mapped pairs
     */
    public Mapping process ()
    {
        final Mapping output = new Mapping();

        // Process the next in voice immediately
        output.pairs.addAll(mappedNext);

        if (!rookies.isEmpty()) {
            final InjectionSolver solver = new InjectionSolver(
                    rookies.size(),
                    actives.size() + rookies.size(),
                    new MyDistance());
            WrappedInteger wrappedCost = new WrappedInteger(null);
            final int[] links = solver.solve(wrappedCost);

            for (int i = 0; i < links.length; i++) {
                final AbstractChordInter ch = rookies.get(i);
                final int index = links[i];

                if (index < actives.size()) {
                    AbstractChordInter act = actives.get(index);
                    output.pairs.add(new ChordPair(ch, act, null));
                }
            }

            output.cost = wrappedCost.value;
        }

        return output;
    }

    //------------//
    // processAll //
    //------------//
    /**
     * Retrieve all possible mappings.
     *
     * @return the list of all possible mappings, ordered by increasing cost
     */
    public List<Mapping> processAll ()
    {
        final int rNb = rookies.size();
        final int aNb = actives.size();
        final List<AbstractChordInter> extended = new ArrayList<>(actives);
        rookies.forEach(r -> extended.add(null));

        final List<List<AbstractChordInter>> buckets = Arrangements.generate(extended, rNb);
        logger.info("Raw     buckets: {}", buckets.size());

        Arrangements.reduce(buckets);
        logger.info("Reduced buckets: {}", buckets.size());

        final List<Mapping> all = new ArrayList<>();

        for (List<AbstractChordInter> bucket : buckets) {
            final Mapping mapping = new Mapping();

            for (int ir = 0; ir < rNb; ir++) {
                final AbstractChordInter rookie = rookies.get(ir);
                final AbstractChordInter active = bucket.get(ir);
                final int dist = distance(rookie, active, null);
                mapping.pairs.add(new ChordPair(rookie, active, dist));
                mapping.cost += dist;
            }

            all.add(mapping);
        }

        // Insert the mappedNext into each mapping (at cost 0)
        all.forEach(m -> m.pairs.addAll(mappedNext));

        Collections.sort(all, Mapping.byCost);

        for (Mapping mapping : all) {
            logger.info("   {}", mapping);
        }

        return all;
    }

    //-------------//
    // processNext //
    //-------------//
    /**
     * Process the potential NextInVoice cases.
     *
     * @param rookies          (input) original rookies
     * @param actives          (input) original actives
     * @param remainingRookies (output) rookies still to link
     * @param remainingActives (output) actives still to link
     * @return the pre-mapped pairs
     */
    private Set<ChordPair> processNext (List<AbstractChordInter> rookies,
                                        List<AbstractChordInter> actives,
                                        List<AbstractChordInter> remainingRookies,
                                        List<AbstractChordInter> remainingActives)
    {
        final Set<ChordPair> mapped = new LinkedHashSet<>();
        remainingRookies.addAll(rookies);
        remainingActives.addAll(actives);

        for (ChordPair pair : nextList) {
            if (rookies.contains(pair.rookie) && actives.contains(pair.active)) {
                mapped.add(pair);
                remainingRookies.remove(pair.rookie);
                remainingActives.remove(pair.active);
            }
        }

        return mapped;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------//
    // Mapping //
    //---------//
    /**
     * Formalizes a mapping between incoming chords and still active chords.
     */
    public static class Mapping
    {
        /** For comparing by increasing cost. */
        public static final Comparator<Mapping> byCost = (m1,
                                                          m2) -> Integer.compare(m1.cost, m2.cost);

        /** Evaluated global cost. */
        public int cost;

        /** Proposed mapping. */
        public Set<ChordPair> pairs = new LinkedHashSet<>();

        /**
         * Report the mapped pairs relevant for the provided collection of chords.
         *
         * @param collection the provided collection of chords
         * @return the pairs relevant for the collection
         */
        public List<ChordPair> pairsOf (Collection<AbstractChordInter> collection)
        {
            final List<ChordPair> found = new ArrayList<>();

            for (ChordPair pair : pairs) {
                final AbstractChordInter ch = pair.rookie;

                if (collection.contains(ch)) {
                    found.add(pair);
                }
            }

            return found;
        }

        /**
         * Report the active chord which is mapped to the provided incoming.
         *
         * @param ch provided incoming
         * @return the related active chord if any, null otherwise
         */
        public AbstractChordInter ref (AbstractChordInter ch)
        {
            for (ChordPair pair : pairs) {
                if (pair.rookie == ch) {
                    return pair.active;
                }
            }

            return null;
        }

        @Override
        public String toString ()
        {
            return new StringBuilder("Mapping {") //
                    .append(cost) //
                    .append(pairs.stream().map(p -> p.toString()).collect(joining(",", " [", "]")))
                    .append('}').toString();
        }
    }

    //------------//
    // MyDistance //
    //------------//
    private class MyDistance
            implements InjectionSolver.Distance
    {
        @Override
        public int getDistance (int in,
                                int ip,
                                StringBuilder details)
        {
            final AbstractChordInter rookie = rookies.get(in);
            final AbstractChordInter active = (ip < actives.size()) ? actives.get(ip) : null;

            return distance(rookie, active, details);
        }
    }
}
