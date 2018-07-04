//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    C h o r d S p l i t t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sheet.curve;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.sig.relation.StemAlignmentRelation;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

/**
 * Class {@code ChordSplitter} handles the splitting of a chord due to conflicting ties.
 * <p>
 * The problem is as follows: Several ties may arrive on different heads of a same chord, but they
 * must all come from a same other chord, otherwise the chord is in fact made of two chords (if not
 * more), one for the upper half and one for the lower half.
 * The situation is similar for ties departing from different heads of a same chord.
 * <p>
 * The chord stem can be divided in smaller stems only if the resulting chunks are long enough.
 * This is the case for the right-most chord below,
 * found in Dichterliebe01 example, part 2, page 1, measure 1:<br>
 * <img src="doc-files/longDoubleStem.png" alt="Example of long stem portions">
 * <p>
 * Otherwise, the chord stem is kept as it is, but belongs to several sub-chords, and consequently
 * any stem-related beam (or flag detected later) will apply to several sub-chords.
 * This is the case for the right-most chord below,
 * found in Dichterliebe01 example, part 2, page 2, measure 14:<br>
 * <img src="doc-files/shortDoubleStem.png" alt="Example of shared stem">
 * <p>
 * The general approach works in three phases:
 * <ol>
 * <li>Partition the chord heads into sequences of consistent heads (that are tied to a same other
 * chord). Every partition will correspond to a final separate sub-chord.
 * Any chord head not be related to a tie is included into the y-based closest partition.
 * </li>
 * <li>Physically split the initial chord stem into a sequence of long enough sub-stems.
 * The idea is that these sub-stems should be considered as full-fledge stems that happen to be
 * vertically aligned one under the other, and that have been collapsed into one root stem simply
 * because there were not enough white pixels to clearly separate them.
 * </li>
 * <li>Process each sub-stem separately, to detect one or several sub-chords around it.
 * There will be as many sub-chords as head partitions that share the stem.
 * Since the stem is shared, all these sub-chords will be impacted by the stem-linked beams (or
 * flags to be later detected).
 * </li>
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class ChordSplitter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            ChordSplitter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The large chord to be split. */
    private final HeadChordInter chord;

    /** The location of this chord WRT conflicting ties. */
    private final HorizontalSide side;

    /** The originating chords for the conflicting ties. */
    private final Map<HeadChordInter, List<SlurInter>> origins;

    /** Minimum length for a sub-stem to be extracted from chord stem. */
    private final int minSubStemLength;

    /** Initial chord stem, if any. */
    private final StemInter rootStem;

    /** Stem direction. (positive when going down from head to tail) */
    private final int stemDir;

    /** All partitions of consistent heads. */
    private List<Partition> allPartitions;

    private final SIGraph sig;

    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ChordSplitter} object.
     *
     * @param chord   the chord to split
     * @param side    the ties side where the chord is linked
     * @param origins the originating chords for the ties
     */
    public ChordSplitter (HeadChordInter chord,
                          HorizontalSide side,
                          Map<HeadChordInter, List<SlurInter>> origins)
    {
        this.chord = chord;
        this.side = side;
        this.origins = origins;

        sig = chord.getSig();
        sheet = sig.getSystem().getSheet();
        rootStem = chord.getStem();
        stemDir = chord.getStemDir();
        minSubStemLength = sheet.getScale().toPixels(constants.minSubStemLength);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // split //
    //-------//
    public void split ()
    {
        if (chord.isVip()) {
            logger.info("VIP split {}, {} origins on {}", chord, origins.size(), side.opposite());
        }

        // Detect all partitions of consistent heads in this chord
        getAllPartitions();
        logger.debug("allPartitions: {}", allPartitions);

        if (rootStem != null) {
            // Detect all sub-stems of the (root) chord stem
            Map<StemInter, List<Partition>> subStems = getSubStems();

            if (subStems != null) {
                StemInter lastSubStem = null;

                for (Entry<StemInter, List<Partition>> entry : subStems.entrySet()) {
                    lastSubStem = entry.getKey();
                    processStem(lastSubStem, entry.getValue());
                }

                // Beams attached to last sub-stem?
                for (Relation rel : sig.getRelations(rootStem, BeamStemRelation.class)) {
                    BeamStemRelation oldRel = (BeamStemRelation) rel;
                    BeamStemRelation newRel = new BeamStemRelation();
                    newRel.setGrade(oldRel.getGrade());
                    newRel.setBeamPortion(oldRel.getBeamPortion());
                    newRel.setExtensionPoint(oldRel.getExtensionPoint());
                    sig.addEdge(sig.getEdgeSource(oldRel), lastSubStem, newRel);
                    sig.removeEdge(oldRel);
                }

                rootStem.remove();
            } else {
                processStem(rootStem, allPartitions); // Shared mode
            }
        } else {
            // No stem involved, hence dispatch the whole heads
            // TODO: to be implemented
        }

        chord.remove();
    }

    //------------------//
    // getAllPartitions //
    //------------------//
    private void getAllPartitions ()
    {
        // Collection of tied heads
        final List<HeadInter> tiedHeads = new ArrayList<HeadInter>();

        // Detect the partitions of consistent heads in this chord
        allPartitions = new ArrayList<Partition>();

        for (List<SlurInter> slurList : origins.values()) {
            Partition partition = new Partition();

            for (SlurInter slur : slurList) {
                for (Relation rel : sig.getRelations(slur, SlurHeadRelation.class)) {
                    SlurHeadRelation shRel = (SlurHeadRelation) rel;

                    if (shRel.getSide() == side) {
                        Inter inter = sig.getOppositeInter(slur, rel);
                        HeadInter head = (HeadInter) inter;
                        partition.add(head);
                        tiedHeads.add(head);

                        break;
                    }
                }
            }

            allPartitions.add(partition);
        }

        Collections.sort(allPartitions); // Sort list of partitions

        injectStandardHeads(tiedHeads); // Inject each head left over
    }

    //-------------//
    // getSubStems //
    //-------------//
    /**
     * Generate the map of sub-stems, each with its partition(s).
     * A null value is returned if no sub-stem was extracted, meaning that the root stem must be
     * shared by several partitions.
     *
     * @return the map of detected sub-stems, or null if no sub-stem was extracted.
     */
    private Map<StemInter, List<Partition>> getSubStems ()
    {
        final Map<StemInter, List<Partition>> stemMap = new LinkedHashMap<StemInter, List<Partition>>();
        int iFirst = 0; // Index of first partition pending
        int iLastAddressed = -1; // Index of last addressed partition
        final int rootHeight = rootStem.getBounds().height;
        int yStart = (int) Math.rint(
                ((stemDir > 0) ? rootStem.getTop() : rootStem.getBottom()).getY());

        for (int iLast = 0, iMax = allPartitions.size() - 1; iLast <= iMax; iLast++) {
            final int yStop = (iLast != iMax)
                    ? (allPartitions.get(iLast + 1).first().getCenter().y - stemDir)
                    : chord.getTailLocation().y;
            final int height = stemDir * (yStop - yStart + 1);

            // Extract a sub-stem only if height is significant and smaller than root stem height
            if ((minSubStemLength <= height) && (height < rootHeight)) {
                StemInter subStem = rootStem.extractSubStem(yStart, yStop);
                stemMap.put(subStem, allPartitions.subList(iFirst, iLast + 1));
                iLastAddressed = iLast;

                // Next
                yStart = yStop + stemDir;
                iFirst = iLast + 1;
            }
        }

        if (stemMap.isEmpty()) {
            return null;
        }

        // Sub-stems are strongly aligned with one another
        StemInter previous = null;

        for (StemInter stem : stemMap.keySet()) {
            if (previous != null) {
                sig.addEdge(previous, stem, new StemAlignmentRelation());
            }

            previous = stem;
        }

        // Check every partition is addressed?
        for (int i = iLastAddressed + 1; i < allPartitions.size(); i++) {
            logger.warn("{} unaddressed partition: {}", chord, allPartitions.get(i));
        }

        return stemMap;
    }

    //---------------------//
    // injectStandardHeads //
    //---------------------//
    /**
     * Inject each head left over (not tied) into closest partition.
     */
    private void injectStandardHeads (Collection<HeadInter> tiedHeads)
    {
        List<HeadInter> stdHeads = new ArrayList<HeadInter>();

        for (Inter inter : chord.getNotes()) {
            HeadInter head = (HeadInter) inter;

            if (!tiedHeads.contains(head)) {
                stdHeads.add(head);
            }
        }

        Collections.sort(stdHeads, HeadChordInter.headComparator);

        HeadLoop:
        for (HeadInter head : stdHeads) {
            final int y = head.getCenter().y;
            Integer bestDist = null;
            Partition bestPartition = null;

            for (Partition partition : allPartitions) {
                int dist = partition.distanceTo(y);

                if (dist < 0) {
                    partition.add(head);

                    continue HeadLoop;
                } else if ((bestDist == null) || (bestDist > dist)) {
                    bestDist = dist;
                    bestPartition = partition;
                }
            }

            bestPartition.add(head); // Here, bestPartition cannot be null
        }
    }

    //-------------//
    // processStem //
    //-------------//
    /**
     * Process the provided stem (either the rootStem or a subStem) with its
     * related partitions.
     *
     * @param stem       the (sub?) stem to split
     * @param partitions the partitions related to the provided stem
     */
    private void processStem (StemInter stem,
                              List<Partition> partitions)
    {
        for (Partition partition : partitions) {
            // One sub-chord per partition
            HeadChordInter ch = new HeadChordInter(chord.getGrade());
            sig.addVertex(ch);
            ch.setStem(stem);

            for (HeadInter head : partition) {
                // Switch partition heads to sub-chord
                ch.addMember(head);

                if (stem != rootStem) {
                    // Link partition heads to (sub) stem
                    HeadStemRelation relation = (HeadStemRelation) sig.getRelation(
                            head,
                            rootStem,
                            HeadStemRelation.class);

                    if (relation != null) {
                        sig.removeEdge(relation);
                    } else {
                        relation = new HeadStemRelation();
                    }

                    sig.addEdge(head, stem, relation);
                }
            }

            chord.getMeasure().addInter(ch);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction minSubStemLength = new Scale.Fraction(
                2.5,
                "Minimum sub-stem length for a tie split");
    }

    //-----------//
    // Partition //
    //-----------//
    /**
     * A consistent sequence of note heads, kept sorted from chord head location.
     */
    private static class Partition
            extends TreeSet<HeadInter>
            implements Comparable<Partition>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public Partition ()
        {
            super(HeadChordInter.headComparator);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int compareTo (Partition that)
        {
            return HeadChordInter.headComparator.compare(this.first(), that.first());
        }

        /**
         * Report the vertical (algebraic) distance between partition and the provided y.
         * Distance is negative if ordinate is within partition height and positive if outside.
         */
        public int distanceTo (int y)
        {
            final int first = first().getCenter().y;
            final int last = last().getCenter().y;

            return Math.max(first - y, y - last);
        }
    }
}
