//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    C h o r d S p l i t t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import static omr.run.Orientation.VERTICAL;
import omr.run.RunTableFactory;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.AbstractNoteInter;
import omr.sig.inter.HeadChordInter;
import omr.sig.inter.Inter;
import omr.sig.inter.SlurInter;
import omr.sig.inter.StemInter;
import omr.sig.relation.BeamStemRelation;
import omr.sig.relation.HeadStemRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.SlurHeadRelation;
import omr.sig.relation.StemAlignmentRelation;

import omr.util.HorizontalSide;

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
 * This is the case for the right-most chord below:<br>
 * <img src="doc-files/longDoubleStem.png">
 * <p>
 * Otherwise, the chord stem is kept as it is, but belongs to several sub-chords, and consequently
 * any stem-related beam (or flag detected later) will apply to several sub-chords.
 * This is the case for the right-most chord below:<br>
 * <img src="doc-files/shortDoubleStem.png">
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
    private final AbstractChordInter chord;

    /** The location of this chord WRT conflicting ties. */
    private final HorizontalSide side;

    /** The originating chords for the conflicting ties. */
    private final Map<AbstractChordInter, List<SlurInter>> origins;

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
    public ChordSplitter (AbstractChordInter chord,
                          HorizontalSide side,
                          Map<AbstractChordInter, List<SlurInter>> origins)
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
    //---------//
    // process //
    //---------//
    public void process ()
    {
        // Detect all partitions of consistent heads in this chord
        allPartitions = getAllPartitions();
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

                rootStem.delete();
            } else {
                processStem(rootStem, allPartitions); // Shared mode
            }
        } else {
            // No stem involved, hence dispatch the whole heads
            // TODO: to be implemented
        }

        chord.delete();
    }

    //------------------//
    // getAllPartitions //
    //------------------//
    private List<Partition> getAllPartitions ()
    {
        // Collection of tied heads
        final List<AbstractNoteInter> tiedHeads = new ArrayList<AbstractNoteInter>();

        // Detect the partitions of consistent heads in this chord
        List<Partition> partitions = new ArrayList<Partition>();

        for (List<SlurInter> slurList : origins.values()) {
            Partition partition = new Partition();

            for (SlurInter slur : slurList) {
                for (Relation rel : sig.getRelations(slur, SlurHeadRelation.class)) {
                    SlurHeadRelation shRel = (SlurHeadRelation) rel;

                    if (shRel.getSide() == side) {
                        Inter inter = sig.getOppositeInter(slur, rel);
                        AbstractHeadInter head = (AbstractHeadInter) inter;
                        partition.add(head);
                        tiedHeads.add(head);

                        break;
                    }
                }
            }

            partitions.add(partition);
        }

        Collections.sort(partitions); // Sort list of partitions

        injectStandardHeads(tiedHeads); // Inject each head left over

        return partitions;
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
        final RunTableFactory factory = new RunTableFactory(VERTICAL);
        final Glyph rootGlyph = rootStem.getGlyph();
        int iFirst = 0; // Index of first partition pending
        int iLastAddressed = -1; // Index of last addressed partition
        int yStart = (stemDir > 0) ? rootGlyph.getTop()
                : ((rootGlyph.getTop()
                    + rootGlyph.getHeight()) - 1);

        for (int iLast = 0, iMax = allPartitions.size() - 1; iLast <= iMax; iLast++) {
            final int yStop = (iLast != iMax)
                    ? (allPartitions.get(iLast + 1).first().getCenter().y - stemDir)
                    : chord.getTailLocation().y;
            final int height = stemDir * (yStop - yStart + 1);

            // Extract a sub-stem only if height is significant and smaller than root stem height
            if ((minSubStemLength <= height) && (height < rootGlyph.getHeight())) {
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
    private void injectStandardHeads (Collection<AbstractNoteInter> tiedHeads)
    {
        List<AbstractHeadInter> stdHeads = new ArrayList<AbstractHeadInter>();

        for (Inter inter : chord.getNotes()) {
            AbstractHeadInter head = (AbstractHeadInter) inter;

            if (!tiedHeads.contains(head)) {
                stdHeads.add(head);
            }
        }

        Collections.sort(stdHeads, AbstractChordInter.noteHeadComparator);

        HeadLoop:
        for (AbstractHeadInter head : stdHeads) {
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
     * @param stem       the (sub?) stem to process
     * @param partitions the partitions related to the provided stem
     */
    private void processStem (StemInter stem,
                              List<Partition> partitions)
    {
        for (Partition partition : partitions) {
            // One sub-chord per partition
            HeadChordInter ch = new HeadChordInter(chord.getGrade());
            ch.setStem(stem);

            for (AbstractHeadInter head : partition) {
                // Switch partition heads to sub-chord
                ch.addMember(head);

                if (stem != rootStem) {
                    // Link partition heads to (sub) stem
                    HeadStemRelation relation = (HeadStemRelation) sig.getRelation(
                            head,
                            rootStem,
                            HeadStemRelation.class);
                    sig.removeEdge(relation);
                    sig.addEdge(head, stem, relation);
                }
            }

            sheet.getInterIndex().register(ch);
            sig.addVertex(ch);
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
            extends TreeSet<AbstractHeadInter>
            implements Comparable<Partition>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public Partition ()
        {
            super(AbstractChordInter.noteHeadComparator);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int compareTo (Partition that)
        {
            return AbstractChordInter.noteHeadComparator.compare(this.first(), that.first());
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
