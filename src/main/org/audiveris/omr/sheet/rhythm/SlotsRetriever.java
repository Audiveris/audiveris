//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S l o t s R e t r i e v e r                                  //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.Fraction;
import org.audiveris.omr.sheet.rhythm.Slot.MeasureSlot;
import static org.audiveris.omr.sheet.rhythm.SlotsRetriever.Rel.AFTER;
import static org.audiveris.omr.sheet.rhythm.SlotsRetriever.Rel.BEFORE;
import static org.audiveris.omr.sheet.rhythm.SlotsRetriever.Rel.CLOSE;
import static org.audiveris.omr.sheet.rhythm.SlotsRetriever.Rel.EQUAL;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.SmallBeamInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.NextInVoiceRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SameTimeRelation;
import org.audiveris.omr.sig.relation.SameVoiceRelation;
import org.audiveris.omr.sig.relation.SeparateTimeRelation;
import org.audiveris.omr.sig.relation.StemAlignmentRelation;

import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleDirectedGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.NotThreadSafe;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class <code>SlotsRetriever</code> is in charge, within one measure of a system,
 * to organize all chords into vertical slots.
 * <p>
 * Voices and time offsets are not considered yet, they will be handled later.
 * <p>
 * A key point is to determine when two chords should belong or not to the same slot:
 * <ul>
 * <li>Chords that share a common stem belong to the same slot.</li>
 * <li>Chords that originate from mirrored heads belong to the same slot.
 * (for example a note head with one stem on left and one stem on right leads to two overlapping
 * logical chords)</li>
 * <li>Chords within the same beam group, but not on the same stem, cannot belong to the same
 * slot.</li>
 * <li>Similar abscissa is only an indication, it is not always reliable.</li>
 * <li>Certain chords, though slightly shifted abscissa-wise, can be considered as <i>adjacent</i>
 * and thus share the same slot.
 * <br>
 * Adjacency example (first two chords of upper staff):<br>
 * <img src="doc-files/AdjacentChords.png" alt="Adjacency example">
 * <br>
 * More adjacency examples (upper staff):<br>
 * <img src="doc-files/AdjacentChords4.png" alt="More adjacency examples">
 * </li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class SlotsRetriever
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SlotsRetriever.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The dedicated measure. */
    private final Measure measure;

    /** Should we use wide or narrow slots?. */
    private final boolean useWideSlots;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Inter-chord relationships for the current measure. */
    private SimpleDirectedGraph<AbstractChordInter, Edge> graph = new SimpleDirectedGraph<>(
            Edge.class);

    /** Comparator based on inter-chord relationships, then on timeOffset when known. */
    private final Comparator<AbstractChordInter> byRel = (c1,
                                                          c2) ->
    {
        if (c1 == c2) {
            return 0;
        }

        Rel rel = getRel(c1, c2);

        if (rel == null) {
            return 0;
        } else {
            switch (rel) {
            case BEFORE:
                return -1;

            case AFTER:
                return 1;

            default:

                // Use time offset difference when known
                if ((c1.getTimeOffset() != null) && (c2.getTimeOffset() != null)) {
                    return c1.getTimeOffset().compareTo(c2.getTimeOffset());
                } else {
                    return 0;
                }
            }
        }
    };

    /** Candidate measure chords for slots. Measure-long rests and small chords are excluded. */
    private final List<AbstractChordInter> candidateChords;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SlotsBuilder</code> object for a measure.
     *
     * @param measure      the provided measure
     * @param useWideSlots true for wide slots, false for narrow slots
     */
    public SlotsRetriever (Measure measure,
                           boolean useWideSlots)
    {
        this.measure = measure;
        this.useWideSlots = useWideSlots;

        Scale scale = measure.getStack().getSystem().getSheet().getScale();
        params = new Parameters(scale);

        candidateChords = getCandidateChords();

        // Build the graph of chords relationships
        buildChordRelationships();

        Collections.sort(candidateChords, byRel);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------//
    // areAdjacent //
    //-------------//
    /**
     * Check whether the two provided chords can be considered as "adjacent" and thus
     * share the same slot, though they are slightly separated in x and y.
     *
     * @param ch1 one chord
     * @param ch2 another chord
     * @return true if adjacent
     */
    private boolean areAdjacent (AbstractChordInter ch1,
                                 AbstractChordInter ch2)
    {
        if (ch1.isVip() && ch2.isVip()) {
            logger.info("VIP areAdjacent? {} {}", ch1, ch2);
        }

        // Adjacency cannot occur if at least 1 rest-chord is involved.
        if (ch1.isRest() || ch2.isRest()) {
            return false;
        }

        final Rectangle box1 = ch1.getBoundsWithDots();
        final Rectangle box2 = ch2.getBoundsWithDots();

        // Check horizontal void gap
        final int xGap = GeoUtil.xGap(box1, box2);

        if (xGap > params.maxAdjacencyXGap) {
            return false;
        }

        // Two stem-based chords?
        if ((ch1.getStem() != null) && (ch2.getStem() != null)) {
            // If they share the same stem -> true
            if (ch1.getStem() == ch2.getStem()) {
                return true;
            }

            // If stem directions are identical -> false
            if (ch1.getStemDir() == ch2.getStemDir()) {
                return false;
            }

            // Embraced?
            if (xGap < 0) {
                return true;
            }

            // If beam on each side -> false (different groups!)
            if (!ch1.getBeams().isEmpty() && !ch2.getBeams().isEmpty()) {
                return false;
            }

            // Similar pitches?
            for (Inter i1 : ch1.getNotes()) {
                final HeadInter h1 = (HeadInter) i1;
                final int p1 = h1.getIntegerPitch();

                for (Inter i2 : ch2.getNotes()) {
                    final HeadInter h2 = (HeadInter) i2;
                    final int p2 = h2.getIntegerPitch();

                    if (Math.abs(p2 - p1) <= 2) {
                        return true;
                    }
                }
            }
        } else {
            // What if we have two whole heads, 1 step apart vertically and very close on x ???
            // (or similar config between one whole head and one stemmed-head)
            AbstractNoteInter h1 = ch1.getLeadingNote();
            AbstractNoteInter h2 = ch2.getLeadingNote();

            if (Math.abs(h1.getIntegerPitch() - h2.getIntegerPitch()) <= 2) {
                return true;
            }
        }

        // Check abscissa shift between heads
        return Math.abs(ch1.getHeadLocation().x - ch2.getHeadLocation().x) <= params.maxSlotDxLow;
    }

    //-----------------------//
    // areExplicitlySeparate //
    //-----------------------//
    /**
     * Check whether the two provided chords have been declared as using separate slots
     * (or sharing the same voice, which implies separate slots).
     *
     * @param ch1 one chord
     * @param ch2 another chord
     * @return true if explicitly separate
     */
    private boolean areExplicitlySeparate (AbstractChordInter ch1,
                                           AbstractChordInter ch2)
    {
        if (ch1.isVip() && ch2.isVip()) {
            logger.info("VIP areExplicitlySeparate? {} {}", ch1, ch2);
        }

        final SIGraph sig = ch1.getSig();
        final LinkedHashSet<Relation> rels = new LinkedHashSet<>();
        rels.addAll(sig.getAllEdges(ch1, ch2));
        rels.addAll(sig.getAllEdges(ch2, ch1));

        for (Relation rel : rels) {
            if ((rel instanceof SeparateTimeRelation) || (rel instanceof NextInVoiceRelation)
                    || (rel instanceof SameVoiceRelation)) {
                return true;
            }
        }

        return false;
    }

    //-------------------------//
    // buildChordRelationships //
    //-------------------------//
    /**
     * Compute the matrix of inter-chords relationships.
     */
    private void buildChordRelationships ()
    {
        // Sort measure standard chords by abscissa
        List<AbstractChordInter> stdChords = new ArrayList<>(measure.getStandardChords());
        purgeMeasureRestChords(stdChords);
        Collections.sort(stdChords, Inters.byCenterAbscissa);

        // Populate graph with chords
        Graphs.addAllVertices(graph, stdChords);

        // Explicit separate time slots
        inspectSeparateSlots();

        // Explicit same time
        inspectSameSlot(null);

        // BeamGroupInter-based relationships
        inspectBeams();

        // Mirror-based relationships
        inspectMirrors();

        // RootStem-based relationships
        inspectRootStems();

        // Extend explicit same time slot to intermediate chords if any
        inspectSameSlot(stdChords);

        // Detect adjacencies
        inspectAdjacencies(stdChords);

        // Finally, default location-based relationships
        inspectCloseChords(stdChords);
        inspectDistantChords(stdChords);

        if (constants.dumpRelationships.isSet() || logger.isDebugEnabled()) {
            dumpRelationships(stdChords);
        }
    }

    //------------//
    // buildSlots //
    //------------//
    /**
     * Organize the standard chords into a sequence of slot candidates in measure.
     * <p>
     * Slots are built based on the inter-chord relationships, voices and time offset values will be
     * computed later.
     *
     * @return the detected slots
     */
    public List<MeasureSlot> buildSlots ()
    {
        final List<MeasureSlot> slots = new ArrayList<>();
        final int chordCount = candidateChords.size();

        int slotCount = 0;
        int iStart = 0;

        NextSlot:
        while (iStart < chordCount) {
            for (int i = iStart + 1; i < chordCount; i++) {
                AbstractChordInter c2 = candidateChords.get(i);

                // Make sure c2 is compatible with ALL slot chords so far
                for (AbstractChordInter c1 : candidateChords.subList(iStart, i)) {
                    Rel rel = getRel(c1, c2);

                    if ((rel != Rel.EQUAL) && (rel != Rel.CLOSE)) {
                        // New slot starting here, register previous one
                        MeasureSlot slot = new MeasureSlot(
                                ++slotCount,
                                measure,
                                candidateChords.subList(iStart, i));
                        slots.add(slot);
                        iStart = i;

                        continue NextSlot;
                    }
                }
            }

            // Register last slot
            MeasureSlot slot = new MeasureSlot(
                    ++slotCount,
                    measure,
                    candidateChords.subList(iStart, chordCount));
            slots.add(slot);

            iStart = chordCount;
        }

        return slots;
    }

    //-------------------//
    // dumpRelationships //
    //-------------------//
    /**
     * Print out the matrix of chord relationships for the measure.
     *
     * @param stdChords standard chords, sorted by abscissa
     */
    private void dumpRelationships (List<AbstractChordInter> stdChords)
    {
        // List BeamGroups
        if (logger.isDebugEnabled()) {
            for (BeamGroupInter group : measure.getBeamGroups()) {
                logger.info("  {}", group);
            }
        }

        // List chords relationships
        StringBuilder sb = new StringBuilder("    ");
        int length = stdChords.size();

        for (int ix = 0; ix < length; ix++) {
            sb.append(String.format(" %5s", stdChords.get(ix).getId()));
        }

        for (int iy = 0; iy < length; iy++) {
            AbstractChordInter chy = stdChords.get(iy);
            sb.append("\n");
            sb.append(String.format("%5s", chy.getId()));

            for (int ix = 0; ix < length; ix++) {
                sb.append("   ");

                AbstractChordInter chx = stdChords.get(ix);

                Rel rel = getRel(chy, chx);

                if (rel != null) {
                    sb.append(" ").append(rel).append(" ");
                } else {
                    sb.append(" . ");
                }
            }

            // Append chord description
            sb.append("  ").append(chy);
        }

        logger.info("\nwide:{}\n{}", useWideSlots, sb);
    }

    //--------------------//
    // getCandidateChords //
    //--------------------//
    /**
     * Report all chords in measure that are candidate to slot assignment.
     *
     * @return list of standard chords in measure, except the measure-long rest chords
     */
    private List<AbstractChordInter> getCandidateChords ()
    {
        List<AbstractChordInter> candidates = new ArrayList<>();

        for (AbstractChordInter chord : measure.getStandardChords()) {
            if (!chord.isMeasureRest()) {
                candidates.add(chord);
            }
        }

        return candidates;
    }

    //------------------//
    // getEqualPartners //
    //------------------//
    /**
     * Report the chords that are in EQUAL relationship with the provided chord.
     *
     * @param chord the provided chord
     * @return the chords linked by EQUAL relationship
     */
    public Set<AbstractChordInter> getEqualPartners (AbstractChordInter chord)
    {
        Set<AbstractChordInter> found = null;

        for (Edge edge : graph.outgoingEdgesOf(chord)) {
            if (edge.rel == Rel.EQUAL) {
                AbstractChordInter ch = graph.getEdgeTarget(edge);

                if (found == null) {
                    found = new LinkedHashSet<>();
                }

                found.add(ch);
            }
        }

        if (found == null) {
            return Collections.emptySet();
        }

        return found;
    }

    //-----------//
    // getEquals //
    //-----------//
    /**
     * Report the chords that are equal to the provided chord
     *
     * @param chord the provided chord
     * @return the set of chords candidates which are flagged as EQUAL to the provided chord
     */
    private Set<AbstractChordInter> getEquals (AbstractChordInter chord)
    {
        final Set<AbstractChordInter> equals = new LinkedHashSet<>();
        final Set<Edge> outEdges = graph.outgoingEdgesOf(chord);

        for (Edge edge : outEdges) {
            if (edge.rel == EQUAL) {
                final AbstractChordInter ch = graph.getEdgeTarget(edge);
                equals.add(ch);
            }
        }

        return equals;
    }

    //--------//
    // getRel //
    //--------//
    /**
     * Report the inter-chord relationship, if any, that exists within the ordered pair
     * of chords.
     *
     * @param from source chord
     * @param to   target chord
     * @return the relationship from source to target, if any
     */
    public Rel getRel (AbstractChordInter from,
                       AbstractChordInter to)
    {
        Edge edge = graph.getEdge(from, to);

        if (edge != null) {
            return edge.rel;
        }

        return null;
    }

    //----------------//
    // haveCommonHead //
    //----------------//
    /**
     * Check whether the provided two chords have a head in common (either because
     * the two chords are mirrors of one another, or because they share mirror heads).
     * If so, they must share the same slot (and must be in separate voices).
     *
     * @param ch1 first provided chord
     * @param ch2 second provided chord
     * @return true if there is a common note
     */
    private boolean haveCommonHead (AbstractChordInter ch1,
                                    AbstractChordInter ch2)
    {
        for (Inter inter : ch1.getMembers()) {
            final Inter mirror = inter.getMirror();

            if ((mirror != null) && ch2.getMembers().contains(mirror)) {
                return true;
            }
        }

        return false;
    }

    //--------------------//
    // inspectAdjacencies //
    //--------------------//
    /**
     * Inspect couples of chords to detect adjacencies.
     *
     * @param stdChords standard chords, sorted by abscissa
     */
    private void inspectAdjacencies (List<AbstractChordInter> stdChords)
    {
        for (int i = 0; i < stdChords.size(); i++) {
            final AbstractChordInter ch1 = stdChords.get(i);
            final Rectangle box1 = ch1.getBounds();

            for (AbstractChordInter ch2 : stdChords.subList(i + 1, stdChords.size())) {
                if (ch1.isVip() && ch2.isVip()) {
                    logger.info("VIP inspectAdjacencies {} vs {}", ch1, ch2);
                }

                if (getRel(ch1, ch2) != null) {
                    continue;
                }

                // Check boxes vertical overlap + adjacency
                final Rectangle box2 = ch2.getBounds();
                final int yOverlap = GeoUtil.yOverlap(box1, box2);

                if ((yOverlap > params.maxVerticalOverlap) && areAdjacent(ch1, ch2)) {
                    setRel(ch1, ch2, EQUAL);
                    setRel(ch2, ch1, EQUAL);
                }
            }
        }
    }

    //--------------//
    // inspectBeams //
    //--------------//
    /**
     * Derive some inter-chord relationships from BeamGroup instances, excepting the
     * cue beams of course.
     * <p>
     * Within a single BeamGroupInter, there are strict relationships between the chords.
     */
    private void inspectBeams ()
    {
        GroupLoop:
        for (BeamGroupInter group : measure.getBeamGroups()) {
            Set<AbstractChordInter> chordSet = new LinkedHashSet<>();

            for (Inter bInter : group.getMembers()) {
                final AbstractBeamInter beam = (AbstractBeamInter) bInter;

                if (beam instanceof SmallBeamInter) {
                    continue GroupLoop; // Exclude cue beam group
                }

                chordSet.addAll(beam.getChords());
            }

            final List<AbstractChordInter> groupChords = new ArrayList<>(chordSet);
            Collections.sort(groupChords, Inters.byAbscissa);

            // We consider only chords within the measure
            // This is a protection against a beam crossing measure limits
            for (int i = 0; i < groupChords.size(); i++) {
                AbstractChordInter ch1 = groupChords.get(i);
                if (graph.containsVertex(ch1)) {
                    for (AbstractChordInter ch2 : groupChords.subList(i + 1, groupChords.size())) {
                        if (graph.containsVertex(ch2)) {
                            setRel(ch1, ch2, BEFORE);
                            setRel(ch2, ch1, AFTER);
                        }
                    }
                }
            }
        }
    }

    //--------------------//
    // inspectCloseChords //
    //--------------------//
    /**
     * Detect close chords.
     *
     * @param stdChords standard chords, sorted by abscissa
     */
    private void inspectCloseChords (List<AbstractChordInter> stdChords)
    {
        final int maxSlotDx = useWideSlots ? params.maxSlotDxHigh : params.maxSlotDxLow;
        final MeasureStack stack = measure.getStack();

        for (int i = 0; i < stdChords.size(); i++) {
            final AbstractChordInter ch1 = stdChords.get(i);
            final double x1 = stack.getXOffset(ch1.getCenter());

            for (AbstractChordInter ch2 : stdChords.subList(i + 1, stdChords.size())) {
                if (ch1.isVip() && ch2.isVip()) {
                    logger.info("VIP inspectCloseChords {} vs {}", ch1, ch2);
                }

                if (getRel(ch1, ch2) != null) {
                    continue;
                }

                // Check abscissa
                final double x2 = stack.getXOffset(ch2.getCenter());
                final double dx = Math.abs(x1 - x2);

                if (dx <= maxSlotDx) {
                    setRel(ch1, ch2, CLOSE);
                    setRel(ch2, ch1, CLOSE);
                }
            }
        }
    }

    //----------------------//
    // inspectDistantChords //
    //----------------------//
    /**
     * Determine inter-chord relationships for remaining chord couples.
     *
     * @param stdChords standard chords, sorted by abscissa
     */
    private void inspectDistantChords (List<AbstractChordInter> stdChords)
    {
        final int maxSlotDx = useWideSlots ? params.maxSlotDxHigh : params.maxSlotDxLow;
        final MeasureStack stack = measure.getStack();

        for (int i = 0; i < stdChords.size(); i++) {
            final AbstractChordInter ch1 = stdChords.get(i);
            final double x1 = stack.getXOffset(ch1.getCenter());

            OtherLoop:
            for (AbstractChordInter ch2 : stdChords.subList(i + 1, stdChords.size())) {
                if (ch1.isVip() && ch2.isVip()) {
                    logger.info("VIP inspectDistantChords {} vs {}", ch1, ch2);
                }

                if (getRel(ch1, ch2) != null) {
                    continue;
                }

                // Check if compatible with a chord equal to ch2
                final Set<AbstractChordInter> equals = getEquals(ch2);

                for (AbstractChordInter ch : equals) {
                    if (!areExplicitlySeparate(ch1, ch)) {
                        final double x = stack.getXOffset(ch.getCenter());
                        final double dx = Math.abs(x1 - x);

                        if (dx <= maxSlotDx) {
                            setRel(ch1, ch2, CLOSE);
                            setRel(ch2, ch1, CLOSE);

                            continue OtherLoop;
                        }
                    }
                }

                // Not directly close and no compatible equal found
                final double x2 = stack.getXOffset(ch2.getCenter());

                if (x1 < x2) {
                    setRel(ch1, ch2, BEFORE);
                    setRel(ch2, ch1, AFTER);
                } else {
                    setRel(ch2, ch1, BEFORE);
                    setRel(ch1, ch2, AFTER);
                }
            }
        }
    }

    //----------------//
    // inspectMirrors //
    //----------------//
    /**
     * Derive some inter-chords relationships from standard mirror chords (and heads).
     */
    private void inspectMirrors ()
    {
        final List<HeadChordInter> headChords = new ArrayList<>(measure.getStandardHeadChords());

        for (int i = 0; i < headChords.size(); i++) {
            HeadChordInter ch1 = headChords.get(i);

            for (HeadChordInter ch2 : headChords.subList(i + 1, headChords.size())) {
                if (getRel(ch1, ch2) != null) {
                    continue;
                }

                // Check for common notes
                if (haveCommonHead(ch1, ch2)) {
                    // Propagate existing ch1 relationships and ch2 relationships
                    propagateRelationships(ch1, ch2);

                    // Record relationship
                    setRel(ch1, ch2, EQUAL);
                    setRel(ch2, ch1, EQUAL);
                }
            }
        }
    }

    //------------------//
    // inspectRootStems //
    //------------------//
    /**
     * Derive some inter-chords relationships from chords whose stems were portions of
     * the same root stem instance.
     */
    private void inspectRootStems ()
    {
        final SIGraph sig = measure.getStack().getSystem().getSig();
        final List<HeadChordInter> headChords = new ArrayList<>(measure.getStandardHeadChords());

        for (int i = 0; i < headChords.size(); i++) {
            HeadChordInter ch1 = headChords.get(i);
            StemInter stem1 = ch1.getStem();

            if (stem1 == null) {
                continue;
            }

            Set<Relation> aligns = sig.getRelations(stem1, StemAlignmentRelation.class);

            if (aligns.isEmpty()) {
                continue;
            }

            Set<Inter> alignedStems = new LinkedHashSet<>();

            for (Relation rel : aligns) {
                Inter inter = sig.getOppositeInter(stem1, rel);
                alignedStems.add(inter);
            }

            for (HeadChordInter ch2 : headChords.subList(i + 1, headChords.size())) {
                StemInter stem2 = ch2.getStem();

                if (stem2 == null) {
                    continue;
                }

                if (alignedStems.contains(stem2)) {
                    // Propagate existing ch1 relationships and ch2 relationships
                    propagateRelationships(ch1, ch2);

                    // Record relationship
                    setRel(ch1, ch2, EQUAL);
                    setRel(ch2, ch1, EQUAL);
                }
            }
        }
    }

    //-----------------//
    // inspectSameSlot //
    //-----------------//
    /**
     * A pair of chords may be linked by an explicit SameTimeRelation.
     *
     * @param stdChords standard chords, sorted by abscissa
     */
    private void inspectSameSlot (List<AbstractChordInter> stdChords)
    {
        final SIGraph sig = measure.getStack().getSystem().getSig();

        for (Relation same : sig.relations(SameTimeRelation.class)) {
            final AbstractChordInter ch1 = (AbstractChordInter) sig.getEdgeSource(same);
            final AbstractChordInter ch2 = (AbstractChordInter) sig.getEdgeTarget(same);

            if ((ch1.getMeasure() == measure) && (ch2.getMeasure() == measure)) {
                if (stdChords == null) {
                    // First pass: just set equal rel to the chords explicitly chosen
                    setRel(ch1, ch2, EQUAL);
                    setRel(ch2, ch1, EQUAL);
                } else {
                    // Second pass: forward equal rel to all chords included abscissawise
                    // if they still have no rel assigned.
                    final int j1 = stdChords.indexOf(ch1);
                    final int j2 = stdChords.indexOf(ch2);
                    final int iMin = Math.min(j1, j2);
                    final int iMax = Math.max(j1, j2);

                    for (int i1 = iMin; i1 < iMax; i1++) {
                        AbstractChordInter c1 = stdChords.get(i1);

                        for (int i2 = i1 + 1; i2 <= iMax; i2++) {
                            AbstractChordInter c2 = stdChords.get(i2);

                            if (getRel(c1, c2) == null) {
                                setRel(c1, c2, EQUAL);
                            }

                            if (getRel(c2, c1) == null) {
                                setRel(c2, c1, EQUAL);
                            }
                        }
                    }
                }
            }
        }
    }

    //----------------------//
    // inspectSeparateSlots //
    //----------------------//
    /**
     * A pair of chords may be linked by an explicit SeparateTimeRelation
     * (or SameVoiceRelation).
     */
    private void inspectSeparateSlots ()
    {
        final SIGraph sig = measure.getStack().getSystem().getSig();
        final MeasureStack stack = measure.getStack();

        for (Relation same : sig.relations(SeparateTimeRelation.class, SameVoiceRelation.class)) {
            final AbstractChordInter ch1 = (AbstractChordInter) sig.getEdgeSource(same);
            final AbstractChordInter ch2 = (AbstractChordInter) sig.getEdgeTarget(same);

            if ((ch1.getMeasure() == measure) && (ch2.getMeasure() == measure)) {
                final double x1 = stack.getXOffset(ch1.getCenter());
                final double x2 = stack.getXOffset(ch2.getCenter());

                if (x1 < x2) {
                    setRel(ch1, ch2, BEFORE);
                    setRel(ch2, ch1, AFTER);
                } else {
                    setRel(ch2, ch1, BEFORE);
                    setRel(ch1, ch2, AFTER);
                }
            }
        }
    }

    //------------------------//
    // propagateRelationships //
    //------------------------//
    /**
     * Since the chords in the provided pair are mirrored or share the same root stem,
     * they share identical relationships with the other chords in measure.
     *
     * @param pair array of exactly 2 head chords
     */
    private void propagateRelationships (HeadChordInter... pair)
    {
        Set<AbstractChordInter> stdChords = measure.getStandardChords();
        stdChords.removeAll(Arrays.asList(pair));

        for (AbstractChordInter ch : stdChords) {
            for (int i = 0; i < 2; i++) {
                HeadChordInter one = pair[i];
                HeadChordInter two = pair[(i + 1) % 2];

                Rel rel;
                rel = getRel(ch, one);

                if ((rel != null) && (getRel(ch, two) == null)) {
                    setRel(ch, two, rel);
                }

                rel = getRel(one, ch);

                if ((rel != null) && (getRel(two, ch) == null)) {
                    setRel(two, ch, rel);
                }
            }
        }
    }

    //------------------------//
    // purgeMeasureRestChords //
    //------------------------//
    /**
     * Purge the provided collection from measure-long rest chords.
     *
     * @param chords (input/output) the chords to purge
     */
    private void purgeMeasureRestChords (Collection<AbstractChordInter> chords)
    {
        for (Iterator<AbstractChordInter> it = chords.iterator(); it.hasNext();) {
            final AbstractChordInter chord = it.next();

            if (chord.isMeasureRest()) {
                it.remove();
            }
        }
    }

    //--------//
    // setRel //
    //--------//
    /**
     * Store the relationship between a source and a target chord
     *
     * @param from source chord
     * @param to   target chord
     * @param rel  the relationship value
     */
    private void setRel (AbstractChordInter from,
                         AbstractChordInter to,
                         Rel rel)
    {
        if (from.isVip() && to.isVip()) {
            logger.info("VIP setRel {} {} {}", from, rel, to);
        }

        Edge edge = graph.getEdge(from, to);

        if (edge != null) {
            edge.rel = rel;
        } else {
            graph.addEdge(from, to, new Edge(rel));
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //---------------//
    // getMaxMergeDx //
    //---------------//
    public static Fraction getMaxMergeDx ()
    {
        return constants.maxMergeDx;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxSlotDxHigh = new Scale.Fraction(
                1.1, // Was 1.0, too strict for manual addition of heads on other stem side
                "High maximum horizontal delta between a slot and a chord");

        private final Scale.Fraction maxSlotDxLow = new Scale.Fraction(
                0.5,
                "Low maximum horizontal delta between a slot and a chord");

        private final Scale.Fraction maxMergeDx = new Scale.Fraction(
                1.0,
                "Maximum horizontal delta between two slots for a merge");

        private final Scale.Fraction maxAdjacencyXGap = new Scale.Fraction(
                0.4,
                "Maximum horizontal gap between adjacent chords bounds");

        private final Scale.Fraction maxVerticalOverlap = new Scale.Fraction(
                0.25,
                "Maximum vertical overlap tolerated");

        private final Constant.Boolean dumpRelationships = new Constant.Boolean(
                false,
                "(debug) Dump matrix of chords relationships");
    }

    //------//
    // Edge //
    //------//
    /**
     * Meant to store a relation instance (edge) between two chords (vertices).
     */
    protected static class Edge
    {

        Rel rel; // Relationship carried by the concrete edge

        /**
         * @param rel
         */
        Edge (Rel rel)
        {
            this.rel = rel;
        }
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {

        private final int maxSlotDxHigh;

        private final int maxSlotDxLow;

        private final int maxAdjacencyXGap;

        private final int maxVerticalOverlap;

        Parameters (Scale scale)
        {
            maxSlotDxHigh = scale.toPixels(constants.maxSlotDxHigh);
            maxSlotDxLow = scale.toPixels(constants.maxSlotDxLow);
            maxAdjacencyXGap = scale.toPixels(constants.maxAdjacencyXGap);
            maxVerticalOverlap = scale.toPixels(constants.maxVerticalOverlap);
        }
    }

    //~ Enumerations -------------------------------------------------------------------------------

    //-----//
    // Rel //
    //-----//
    /**
     * Describes the oriented relationship between two chords of the measure.
     */
    public static enum Rel
    {
        /**
         * Strongly before.
         * Stem-located before in the same beam group.
         * Abscissa-located before the vertically overlapping chord.
         * Important abscissa difference in different staves.
         */
        BEFORE("B"),

        /**
         * Strongly after.
         * Stem-located after in the same beam group.
         * Abscissa-located after the vertically overlapping chord.
         * Important abscissa difference in different staves.
         */
        AFTER("A"),

        /**
         * Strongly equal.
         * Identical thanks to an originating glyph in common.
         * Adjacency detected in same staff.
         */
        EQUAL("="),

        /**
         * Weakly close.
         * No important difference, use other separation criteria.
         */
        CLOSE("~");

        private final String mnemo;

        Rel (String mnemo)
        {
            this.mnemo = mnemo;
        }

        @Override
        public String toString ()
        {
            return mnemo;
        }
    }
}
