//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C h o r d s M a p p e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Class <code>ChordsMapper</code> tries to voice-map incoming chords to active chords.
 *
 * @author Hervé Bitteur
 */
public class ChordsMapper
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final List<AbstractChordInter> news;

    private final List<AbstractChordInter> olds;

    private final List<AbstractChordInter> extinctExplicits;

    private final VoiceDistance vd;

    private final Set<ChordPair> blackList;

    private final Set<ChordPair> whiteList;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a <code>ChordsMapper</code> object.
     *
     * @param news             incoming chords to map
     * @param olds             available previous chords
     * @param extinctExplicits extinct chords but with explicit SameVoiceRelation
     * @param vd               the VoiceDistance to use
     * @param blackList        known incompatibilities
     * @param whiteList        explicit compatibilities
     */
    public ChordsMapper (List<AbstractChordInter> news,
                         List<AbstractChordInter> olds,
                         List<AbstractChordInter> extinctExplicits,
                         VoiceDistance vd,
                         Set<ChordPair> blackList,
                         Set<ChordPair> whiteList)
    {
        this.news = news;
        this.olds = olds;
        this.extinctExplicits = extinctExplicits;
        this.vd = vd;
        this.blackList = blackList;
        this.whiteList = whiteList;
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Perform the mapping.
     *
     * @return the list of mapped pairs
     */
    public Mapping process ()
    {
        final Mapping output = new Mapping();
        final InjectionSolver solver = new InjectionSolver(
                news.size(),
                olds.size() + news.size(),
                new MyDistance());
        final int[] links = solver.solve();

        for (int i = 0; i < links.length; i++) {
            final AbstractChordInter ch = news.get(i);
            final int index = links[i];

            if (index < olds.size()) {
                AbstractChordInter act = olds.get(index);
                output.pairs.add(new ChordPair(ch, act));
            }
        }

        return output;
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

        /** Proposed mapping. */
        public List<ChordPair> pairs = new ArrayList<>();

        /**
         * Report the mapped pairs relevant for the provided collection of chords.
         *
         * @param collection the provided collection of chords
         * @return the pairs relevant for the collection
         */
        public List<ChordPair> pairsOf (Collection<AbstractChordInter> collection)
        {
            List<ChordPair> found = null;

            for (ChordPair pair : pairs) {
                final AbstractChordInter ch = pair.one;

                if (collection.contains(ch)) {
                    if (found == null) {
                        found = new ArrayList<>();
                    }

                    found.add(pair);
                }
            }

            if (found == null) {
                return Collections.emptyList();
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
                if (pair.one == ch) {
                    return pair.two;
                }
            }

            return null;
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
            // No link to an old chord
            if (ip >= olds.size()) {
                if (details != null) {
                    details.append("NO_LINK=").append(VoiceDistance.NO_LINK);
                }

                return VoiceDistance.NO_LINK;
            }

            AbstractChordInter newChord = news.get(in);
            AbstractChordInter oldChord = olds.get(ip);

            // White list
            for (ChordPair pair : whiteList) {
                if (pair.two == oldChord) {
                    if (pair.one == newChord) {
                        if (details != null) {
                            details.append("WHITE");
                        }

                        return 0;
                    } else if (extinctExplicits.contains(oldChord)) {
                        if (details != null) {
                            details.append("EXTINCT");
                        }

                        return VoiceDistance.INCOMPATIBLE;
                    }
                }
            }

            // Black list
            for (ChordPair pair : blackList) {
                if ((pair.one == newChord) && (pair.two == oldChord)) {
                    if (details != null) {
                        details.append("BLACK");
                    }

                    return VoiceDistance.INCOMPATIBLE;
                }
            }

            // Use of VoiceDistance
            return vd.getDistance(oldChord, newChord, details);
        }
    }
}
