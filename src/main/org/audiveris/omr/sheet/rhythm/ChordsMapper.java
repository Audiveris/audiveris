//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C h o r d s M a p p e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import java.util.Objects;
import java.util.Set;

/**
 * Class {@code ChordsMapper} tries to voice-map incoming chords to active chords.
 *
 * @author Hervé Bitteur
 */
public class ChordsMapper
{

    private final List<AbstractChordInter> news;

    private final List<AbstractChordInter> olds;

    private final VoiceDistance vd;

    private final Set<ChordPair> incompatibilities;

    /**
     * Creates a {@code ChordsMapper} object.
     *
     * @param news              incoming chords to map
     * @param olds              available previous chords
     * @param vd                the VoiceDistance to use
     * @param incompatibilities known incompatibilities
     */
    public ChordsMapper (List<AbstractChordInter> news,
                         List<AbstractChordInter> olds,
                         VoiceDistance vd,
                         Set<ChordPair> incompatibilities)
    {
        this.news = news;
        this.olds = olds;
        this.vd = vd;
        this.incompatibilities = incompatibilities;
    }

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

            // Incompatible link
            for (ChordPair pair : incompatibilities) {
                if (pair.newChord == newChord && pair.oldChord == oldChord) {
                    if (details != null) {
                        details.append("INCOMP=").append(VoiceDistance.INCOMPATIBLE);
                    }

                    return VoiceDistance.INCOMPATIBLE;
                }
            }

            // Use of VoiceDistance
            return vd.getDistance(oldChord, newChord, details);
        }
    }

    //-----------//
    // ChordPair //
    //-----------//
    public static class ChordPair
    {

        public final AbstractChordInter newChord;

        public final AbstractChordInter oldChord;

        public ChordPair (AbstractChordInter newChord,
                          AbstractChordInter oldChord)
        {
            this.newChord = newChord;
            this.oldChord = oldChord;
        }

        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof ChordPair)) {
                return false;
            }

            final ChordPair that = (ChordPair) obj;

            return newChord == that.newChord && oldChord == that.oldChord;
        }

        @Override
        public int hashCode ()
        {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.newChord);
            hash = 79 * hash + Objects.hashCode(this.oldChord);

            return hash;
        }
    }

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
         * Report the active chord which is mapped to the provided incoming.
         *
         * @param ch provided incoming
         * @return the related active chord if any, null otherwise
         */
        public AbstractChordInter ref (AbstractChordInter ch)
        {
            for (ChordPair pair : pairs) {
                if (pair.newChord == ch) {
                    return pair.oldChord;
                }
            }

            return null;
        }

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
                final AbstractChordInter ch = pair.newChord;

                if (collection.contains(ch)) {
                    if (found == null) {
                        found = new ArrayList<>();
                    }

                    found.add(pair);
                }
            }

            if (found == null) {
                return Collections.EMPTY_LIST;
            }

            return found;
        }
    }
}
