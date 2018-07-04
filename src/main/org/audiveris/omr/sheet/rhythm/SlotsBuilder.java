//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S l o t s B u i l d e r                                    //
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

import net.jcip.annotations.NotThreadSafe;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.beam.BeamGroup;
import static org.audiveris.omr.sheet.rhythm.SlotsBuilder.Rel.*;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.StemAlignmentRelation;

import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleDirectedGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code SlotsBuilder} is in charge, within one measure stack of a system,
 * to organize all chords into proper time slots and voices.
 * <p>
 * The key point is to determine when two chords should belong or not to the same time slot:
 * <ul>
 * <li>Chords that share a common stem belong to the same slot.</li>
 * <li>Chords that originate from mirrored heads belong to the same slot. (for example a
 * note head with one stem on left and one stem on right leads to two overlapping logical
 * chords)</li>
 * <li>Chords within the same beam group, but not on the same stem, cannot belong to the same
 * slot.</li>
 * <li>Similar abscissa is only an indication, it is not always reliable.</li>
 * </ul>
 * <p>
 * Example:<br>
 * <img src="doc-files/AdjacentChords.png" alt="Adjanced chords example">
 * <p>
 * More examples:<br>
 * <img src="doc-files/AdjacentChords4.png" alt="Another example of adjanced chords">
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class SlotsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SlotsBuilder.class);

    //~ Enumerations -------------------------------------------------------------------------------
    //-----//
    // Rel //
    //-----//
    /**
     * Describes the oriented relationship between two chords of the measure stack.
     */
    protected static enum Rel
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /**
         * Strongly before.
         * Stem-located before in the same beam group.
         * Abscissa-located before the vertically overlapping chord.
         * Important abscissa difference in different staves.
         */
        BEFORE("B"),
        //
        /** Strongly after.
         * Stem-located after in the same beam group.
         * Abscissa-located after the vertically overlapping chord.
         * Important abscissa difference in different staves.
         */
        AFTER("A"),
        //
        /**
         * Strongly equal.
         * Identical thanks to an originating glyph in common.
         * Adjacency detected in same staff.
         */
        EQUAL("="),
        //
        /**
         * Weakly close.
         * No important difference, use other separation criteria.
         */
        CLOSE("?");
        //~ Instance fields ------------------------------------------------------------------------

        private final String mnemo;

        //~ Constructors ---------------------------------------------------------------------------
        Rel (String mnemo)
        {
            this.mnemo = mnemo;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return mnemo;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated measure stack. */
    private final MeasureStack stack;

    /** To stop on first error encountered. */
    private final boolean failFast;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Inter-chord relationships for the current measure stack. */
    private SimpleDirectedGraph<AbstractChordInter, Edge> graph = new SimpleDirectedGraph<AbstractChordInter, Edge>(
            Edge.class);

    /** Current earliest term for each staff in stack. */
    private final Map<Staff, Rational> stackTerms = new LinkedHashMap<Staff, Rational>();

    /** Comparator based on inter-chord relationships, then on timeOffset when known. */
    private final Comparator<AbstractChordInter> byRel = new Comparator<AbstractChordInter>()
    {
        @Override
        public int compare (AbstractChordInter c1,
                            AbstractChordInter c2)
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
        }
    };

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SlotsBuilder} object for a measure stack.
     *
     * @param stack    the provided measure stack
     * @param failFast true to stop on first error encountered
     */
    public SlotsBuilder (MeasureStack stack,
                         boolean failFast)
    {
        this.stack = stack;
        this.failFast = failFast;

        params = new Parameters(stack.getSystem().getSheet().getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Determine the proper sequence of slots for the chords of the measure stack.
     *
     * @return true if OK, false if error was detected
     */
    public boolean process ()
    {
        // We work on the population of chords, using inter-chords constraints
        buildRelationships();

        if (!buildSlots() && failFast) {
            return false;
        }

        Voices.refineStack(stack);

        return true;
    }

    //-------------//
    // areAdjacent //
    //-------------//
    /**
     * Check whether the two provided chords can be considered as "adjacent" and thus
     * share the same time slot, though they are slightly separated in x and y.
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

            // If beam on each side -> false (different groups!) TODO: ??? ???
            if (!ch1.getBeams().isEmpty() && !ch2.getBeams().isEmpty()) {
                return false;
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

        // Check abscissa gap between stems
        return Math.abs(ch1.getHeadLocation().x - ch2.getHeadLocation().x) <= params.maxSlotDx;
    }

    //--------------------//
    // buildRelationships //
    //--------------------//
    /**
     * Compute the matrix of inter-chords relationships.
     */
    private void buildRelationships ()
    {
        // Sort measure standard chords by abscissa
        List<AbstractChordInter> stdChords = new ArrayList<AbstractChordInter>(
                stack.getStandardChords());
        Collections.sort(stdChords, Inters.byAbscissa);

        // Populate graph with chords
        Graphs.addAllVertices(graph, stdChords);

        // BeamGroup-based relationships
        inspectBeams();

        // Mirror-based relationships
        inspectMirrors();

        // RootStem-based relationships
        inspectRootStems();

        // Finally, default location-based relationships
        inspectLocations(stdChords);

        if (logger.isDebugEnabled()) {
            dumpRelationships(stdChords);
        }
    }

    //------------//
    // buildSlots //
    //------------//
    /**
     * Build the measure stack time slots, using the inter-chord relationships and the
     * chords durations.
     *
     * @return true if OK, false otherwise
     */
    private boolean buildSlots ()
    {
        logger.debug("buildSlots for {}", stack);

        // The 'actives' collection gathers the chords that are not terminated at the
        // time slot being considered. Initially, it contains just the whole chords.
        List<AbstractChordInter> actives = new ArrayList<AbstractChordInter>(
                stack.getWholeRestChords());
        Collections.sort(actives, Inters.byAbscissa);

        // Create voices for whole rest chords
        handleWholeVoices(actives);

        // List of chords assignable, but not yet assigned to a slot
        List<AbstractChordInter> pendings = getPendingChords();

        // Assign chords to time slots, until no chord is left pending
        // CAUTION: avoid endless loop!
        Rational prevTerm = null;

        while (!pendings.isEmpty()) {
            dump("actives", actives);

            // Earliest end time among all active chords
            // It must be later than time of previous slot if any
            Rational term = computeNextTerm(actives, pendings);
            Slot lastSlot = stack.getLastSlot();

            if ((lastSlot != null) && (term.compareTo(lastSlot.getTimeOffset()) <= 0)) {
                if (failFast) {
                    logger.info("Stack#{} suspicious {}", stack.getIdValue(), lastSlot);

                    return false;
                } else {
                    stack.setAbnormal(true);
                }
            }

            // Which chords end here, and is their voice available or not for the slot?
            // (if a beam group continues, its voice remains locked)
            List<AbstractChordInter> freeEndings = new ArrayList<AbstractChordInter>();
            List<AbstractChordInter> endings = new ArrayList<AbstractChordInter>();
            detectEndings(actives, term, endings, freeEndings);

            // Do we have pending chords that start at this slot?
            List<AbstractChordInter> incomings = retrieveIncomingChords(pendings, term);

            if (!incomings.isEmpty()) {
                // Allocate the slot with the incoming chords
                int slotId = stack.getSlots().size() + 1;
                Slot slot = new Slot(slotId, stack, incomings);
                stack.getSlots().add(slot);

                // Check slots time so far are consistent
                if (!slot.setTimeOffset(term) || !checkInterSlot(slot)) {
                    if (failFast) {
                        return false;
                    } else {
                        String prefix = stack.getSystem().getLogPrefix();
                        slot.setSuspicious(true);
                        stack.setAbnormal(true);
                        logger.info("{}{} {}", prefix, stack, slot);
                    }
                }

                // Determine the voice of each chord in the slot
                slot.buildVoices(freeEndings);
            } else if (term.equals(prevTerm)) {
                logger.info("Stack#{} endless loop detected", stack.getIdValue());
                stack.setAbnormal(true);

                break;
            }

            // Prepare for next iteration
            pendings.removeAll(incomings);
            actives.addAll(incomings);

            actives.removeAll(freeEndings);
            actives.removeAll(endings);

            prevTerm = term;
        }

        // Check that no staff is left empty
        return checkStavesAreFilled();
    }

    //----------------//
    // checkInterSlot //
    //----------------//
    /**
     * Check whether the provided slot is sufficiently distinct from previous one.
     *
     * @param slot the slot to check
     * @return true if OK
     */
    private boolean checkInterSlot (Slot slot)
    {
        // Do we have a previous slot?
        if (slot.getId() == 1) {
            return true;
        }

        Slot prevSlot = stack.getSlots().get(slot.getId() - 2);
        int dx = slot.getXOffset() - prevSlot.getXOffset();

        if (dx >= params.minInterSlotDx) {
            return true;
        }

        //
        //            // We are too close, find out the guilty chord
        //            logger.debug("Too close slot {}", slot);
        //
        //            for (AbstractChordInter prevChord : prevSlot.getChords()) {
        //                Rectangle prevRect = prevChord.getBounds();
        //
        //                for (AbstractChordInter nextChord : slot.getChords()) {
        //                    Rectangle nextRect = nextChord.getBounds();
        //
        //                    if (GeoUtil.yOverlap(prevRect, nextRect) > 0) {
        //                        if (prevChord.isVip() || nextChord.isVip() || logger.isDebugEnabled()) {
        //                            logger.info("VIP slots overlap between {} & {}", prevChord, nextChord);
        //                        }
        //
        //                        // Exclude the pair members in pending candidates!
        //                        if (prevChord instanceof HeadChordInter) {
        //                            if (nextChord instanceof RestChordInter) {
        //                                toRemove.add((RestChordInter) nextChord);
        //                            }
        //                        } else if (nextChord instanceof HeadChordInter) {
        //                            toRemove.add((RestChordInter) prevChord);
        //                        }
        //                    }
        //                }
        //            }
        //
        return false;
    }

    //----------------------//
    // checkStavesAreFilled //
    //----------------------//
    /**
     * Configuration checks may overlook (poor) rests, thus leaving staves empty.
     *
     * @return true if OK
     */
    private boolean checkStavesAreFilled ()
    {
        // Use a temporary map: (staff -> chords)
        Map<Staff, List<AbstractChordInter>> map = new HashMap<Staff, List<AbstractChordInter>>();

        for (Staff staff : stack.getSystem().getStaves()) {
            map.put(staff, new ArrayList<AbstractChordInter>());
        }

        // Populate map of staves with installed chords
        for (Measure measure : stack.getMeasures()) {
            for (AbstractChordInter chord : measure.getStandardChords()) {
                for (Staff staff : chord.getStaves()) {
                    map.get(staff).add(chord);
                }
            }
        }

        // Look for empty staves
        for (Map.Entry<Staff, List<AbstractChordInter>> entry : map.entrySet()) {
            if (entry.getValue().isEmpty()) {
                logger.debug("{} staff#{} is empty", stack, entry.getKey().getId());

                return false;
            }
        }

        return true;
    }

    //-----------------//
    // computeNextTerm //
    //-----------------//
    /**
     * Based on the provided active chords, determine the next expiration time.
     * <p>
     * We compute the earliest term for each staff based on the active chords.
     * We pickup the earliest among all staves.
     *
     * @param actives  chords still active
     * @param pendings chords still not assigned
     */
    private Rational computeNextTerm (Collection<AbstractChordInter> actives,
                                      List<AbstractChordInter> pendings)
    {
        Rational nextTerm = Rational.MAX_VALUE;
        stackTerms.clear();

        // Check for pending chords that could lower a staff term
        for (AbstractChordInter chord : pendings) {
            Rational timeOffset = chord.getTimeOffset();

            if (timeOffset != null) {
                for (Staff staff : chord.getStaves()) {
                    Rational staffTerm = stackTerms.get(staff);

                    if ((staffTerm == null) || (timeOffset.compareTo(staffTerm) < 0)) {
                        stackTerms.put(staff, timeOffset);

                        if (timeOffset.compareTo(nextTerm) < 0) {
                            nextTerm = timeOffset;
                        }
                    }
                }
            }
        }

        for (AbstractChordInter chord : actives) {
            // Skip the "whole" rest chords, since they don't expire before measure end
            if (!chord.isWholeRest()) {
                Rational endTime = chord.getEndTime();

                for (Staff staff : chord.getStaves()) {
                    Rational staffTerm = stackTerms.get(staff);

                    if ((staffTerm == null) || (endTime.compareTo(staffTerm) < 0)) {
                        stackTerms.put(staff, endTime);

                        if (endTime.compareTo(nextTerm) < 0) {
                            nextTerm = endTime;
                        }
                    }
                }
            }
        }

        if (nextTerm.equals(Rational.MAX_VALUE)) {
            // No non-whole chord found
            nextTerm = Rational.ZERO;
        }

        logger.debug("nextTerm={}", nextTerm);

        return nextTerm;
    }

    //---------------//
    // detectEndings //
    //---------------//
    /**
     * Detect which chords (among the active ones) expire at the provided term.
     *
     * @param actives     chords still active
     * @param term        term time
     * @param endings     (output) expiring chords that do not release their voice
     * @param freeEndings (output) expiring chords that release their voice
     */
    private void detectEndings (List<AbstractChordInter> actives,
                                Rational term,
                                List<AbstractChordInter> endings,
                                List<AbstractChordInter> freeEndings)
    {
        for (AbstractChordInter chord : actives) {
            // Look for chords that finish at the slot at hand
            if (!chord.isWholeRest() && (chord.getEndTime().compareTo(term) <= 0)) {
                // Make sure voice is really available
                BeamGroup group = chord.getBeamGroup();

                if ((group != null) && (chord != group.getLastChord())) {
                    // Group continuation
                    endings.add(chord);
                } else if (!chord.getFollowingTiedChords().isEmpty()) {
                    // Tie continuation
                    endings.add(chord);
                } else {
                    freeEndings.add(chord);
                }
            }
        }

        dump("freeEndings", freeEndings);
        dump("endings", endings);
    }

    //------//
    // dump //
    //------//
    /**
     * Dump a collection of chords (if debug flag is set)
     *
     * @param label  label to be printed
     * @param chords the provided chords
     */
    private void dump (String label,
                       Collection<AbstractChordInter> chords)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("{}{}", label, Inters.ids(chords));
        }
    }

    //-------------------//
    // dumpRelationships //
    //-------------------//
    /**
     * Print out the matrix of chord relationships for the stack.
     *
     * @param stdChords standard chords, sorted by abscissa
     */
    private void dumpRelationships (List<AbstractChordInter> stdChords)
    {
        // List BeamGroups
        if (logger.isDebugEnabled()) {
            for (Measure measure : stack.getMeasures()) {
                for (BeamGroup group : measure.getBeamGroups()) {
                    logger.info("  {}", group);
                }
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

        logger.info("\n{}", sb);
    }

    //------------//
    // getClosure //
    //------------//
    /**
     * Report the chords that are likely to belong to the same slot as the provided chord
     *
     * @param chord the provided chord
     * @return the set of chords candidates which are flagged as either EQUAL or CLOSE to the
     *         provided chord
     */
    private Set<AbstractChordInter> getClosure (AbstractChordInter chord)
    {
        Set<AbstractChordInter> closes = new LinkedHashSet<AbstractChordInter>();
        closes.add(chord);

        for (AbstractChordInter ch : stack.getStandardChords()) {
            Rel rel1 = getRel(chord, ch);
            Rel rel2 = getRel(ch, chord);

            if ((rel1 == CLOSE) || (rel1 == EQUAL) || (rel2 == CLOSE) || (rel2 == EQUAL)) {
                closes.add(ch);
            }
        }

        return closes;
    }

    //------------------//
    // getPendingChords //
    //------------------//
    /**
     * Report all chords in stack that are to be assigned a time slot.
     *
     * @return all stack chords, except the whole ones, sorted by relationship
     */
    private List<AbstractChordInter> getPendingChords ()
    {
        List<AbstractChordInter> pendings = new ArrayList<AbstractChordInter>();

        for (AbstractChordInter chord : stack.getStandardChords()) {
            if (!chord.isWholeRest()) {
                pendings.add(chord);
            }
        }

        Collections.sort(pendings, byRel);

        return pendings;
    }

    //--------//
    // getRel //
    //--------//
    /**
     * Report the inter-chord relationship, if any, that exists within the ordered pair
     * of chords
     *
     * @param from source chord
     * @param to   target chord
     * @return the relationship from source to target, if any
     */
    private Rel getRel (AbstractChordInter from,
                        AbstractChordInter to)
    {
        Edge edge = graph.getEdge(from, to);

        if (edge != null) {
            return edge.rel;
        }

        return null;
    }

    //-------------------//
    // handleWholeVoices //
    //-------------------//
    /**
     * Assign a dedicated voice to each whole chord
     *
     * @param wholes the whole chords
     */
    private void handleWholeVoices (List<AbstractChordInter> wholes)
    {
        for (AbstractChordInter chord : wholes) {
            chord.setTimeOffset(Rational.ZERO);

            Voice voice = Voice.createWholeVoice((RestChordInter) chord, chord.getMeasure());
            Measure measure = stack.getMeasureAt(chord.getPart());
            measure.addVoice(voice);
        }
    }

    //----------------//
    // haveCommonHead //
    //----------------//
    /**
     * Check whether the provided two chords have a head in common (either because
     * the two chords are mirrors of one another, or because they share mirror heads).
     * If so, they must share the same time slot (and must be in separate voices).
     *
     * @param ch1 first provided chord
     * @param ch2 second provided chord
     * @return true if there is a common note
     */
    private boolean haveCommonHead (AbstractChordInter ch1,
                                    AbstractChordInter ch2)
    {
        if (ch1.getMirror() == ch2) {
            return true;
        }

        for (Inter inter : ch1.getMembers()) {
            final Inter mirror = inter.getMirror();

            if ((mirror != null) && ch2.getMembers().contains(mirror)) {
                return true;
            }
        }

        return false;
    }

    //--------------//
    // inspectBeams //
    //--------------//
    /**
     * Derive some inter-chord relationships from BeamGroup instances.
     * Within a single BeamGroup, there are strict relationships between the chords.
     */
    private void inspectBeams ()
    {
        for (Measure measure : stack.getMeasures()) {
            for (BeamGroup group : measure.getBeamGroups()) {
                Set<AbstractChordInter> chordSet = new LinkedHashSet<AbstractChordInter>();

                for (AbstractBeamInter beam : group.getBeams()) {
                    chordSet.addAll(beam.getChords());
                }

                final List<AbstractChordInter> groupChords = new ArrayList<AbstractChordInter>(
                        chordSet);
                Collections.sort(groupChords, Inters.byAbscissa);

                for (int i = 0; i < groupChords.size(); i++) {
                    AbstractChordInter ch1 = groupChords.get(i);

                    for (AbstractChordInter ch2 : groupChords.subList(i + 1, groupChords.size())) {
                        setRel(ch1, ch2, BEFORE);
                        setRel(ch2, ch1, AFTER);
                    }
                }
            }
        }
    }

    //------------------//
    // inspectLocations //
    //------------------//
    /**
     * Derive the missing inter-chord relationships from chords relative locations.
     *
     * @param stdChords standard chords, sorted by abscissa
     */
    private void inspectLocations (List<AbstractChordInter> stdChords)
    {
        final List<ChordPair> adjacencies = new ArrayList<ChordPair>();

        for (int i = 0; i < stdChords.size(); i++) {
            AbstractChordInter ch1 = stdChords.get(i);

            if (ch1.isWholeRest()) {
                continue;
            }

            Rectangle box1 = ch1.getBounds();

            for (AbstractChordInter ch2 : stdChords.subList(i + 1, stdChords.size())) {
                if (ch1.isVip() && ch2.isVip()) {
                    logger.info("VIP inspectLocations {} vs {}", ch1, ch2);
                }

                if (ch2.isWholeRest() || (getRel(ch1, ch2) != null)) {
                    continue;
                }

                // Check y overlap
                Rectangle box2 = ch2.getBounds();
                int yOverlap = GeoUtil.yOverlap(box1, box2);

                if (yOverlap > params.maxVerticalOverlap) {
                    // Boxes overlap vertically
                    if (areAdjacent(ch1, ch2)) {
                        setRel(ch1, ch2, EQUAL);
                        setRel(ch2, ch1, EQUAL);
                        adjacencies.add(new ChordPair(ch1, ch2));
                    } else if (ch1.getCenter().x <= ch2.getCenter().x) {
                        setRel(ch1, ch2, BEFORE);
                        setRel(ch2, ch1, AFTER);
                    } else {
                        setRel(ch2, ch1, BEFORE);
                        setRel(ch1, ch2, AFTER);
                    }
                } else {
                    // Boxes do not overlap vertically
                    int dx = Math.abs(ch1.getCenter().x - ch2.getCenter().x);

                    if (dx <= params.maxSlotDx) {
                        setRel(ch1, ch2, CLOSE);
                        setRel(ch2, ch1, CLOSE);
                    } else if (ch1.getCenter().x <= ch2.getCenter().x) {
                        setRel(ch1, ch2, BEFORE);
                        setRel(ch2, ch1, AFTER);
                    } else {
                        setRel(ch2, ch1, BEFORE);
                        setRel(ch1, ch2, AFTER);
                    }
                }
            }
        }

        // Process detected adjacencies
        if (!adjacencies.isEmpty()) {
            logger.debug("Slot adjacencies: {}", adjacencies);

            for (ChordPair pair : adjacencies) {
                // Since ch1 ~ ch2, all neighbors of ch1 ~ neighbors of ch2
                Set<AbstractChordInter> n1 = getClosure(pair.one);
                Set<AbstractChordInter> n2 = getClosure(pair.two);

                for (AbstractChordInter ch1 : n1) {
                    for (AbstractChordInter ch2 : n2) {
                        if (ch1 != ch2) {
                            if (getRel(ch1, ch2) != EQUAL) {
                                setRel(ch1, ch2, CLOSE);
                            }

                            if (getRel(ch2, ch1) != EQUAL) {
                                setRel(ch2, ch1, CLOSE);
                            }
                        }
                    }
                }
            }
        }
    }

    //----------------//
    // inspectMirrors //
    //----------------//
    /**
     * Derive some inter-chords relationships from mirror chords (and heads).
     */
    private void inspectMirrors ()
    {
        final List<HeadChordInter> headChords = new ArrayList<HeadChordInter>(
                stack.getHeadChords());

        for (int i = 0; i < headChords.size(); i++) {
            HeadChordInter ch1 = headChords.get(i);

            if (ch1.isWholeRest()) {
                continue;
            }

            for (HeadChordInter ch2 : headChords.subList(i + 1, headChords.size())) {
                if (ch2.isWholeRest() || (getRel(ch1, ch2) != null)) {
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
        final SIGraph sig = stack.getSystem().getSig();

        final List<HeadChordInter> headChords = new ArrayList<HeadChordInter>(
                stack.getHeadChords());

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

            Set<Inter> alignedStems = new LinkedHashSet<Inter>();

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

    //------------------------//
    // propagateRelationships //
    //------------------------//
    /**
     * Since the chords in the provided pair are mirrored or share the same root stem,
     * they share identical relationships with the other chords in stack.
     *
     * @param pair array of exactly 2 head chords
     */
    private void propagateRelationships (HeadChordInter... pair)
    {
        Set<AbstractChordInter> stdChords = stack.getStandardChords();
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
    // retrieveIncomingChords //
    //------------------------//
    /**
     * Among the pending chords, select the ones that would start in the next slot.
     *
     * @param pendings the collection of all chords still to be assigned
     * @param term     time considered for next slot
     * @return the collection of chords for next slot
     */
    private List<AbstractChordInter> retrieveIncomingChords (List<AbstractChordInter> pendings,
                                                             Rational term)
    {
        dump("pendings", pendings);

        List<AbstractChordInter> incomings = new ArrayList<AbstractChordInter>();
        AbstractChordInter firstChord = pendings.get(0);

        PendingLoop:
        for (AbstractChordInter chord : pendings) {
            // Here all chords should be >= firstChord
            // Chords < firstChord indicate a time offset inconsistent with slot ordering !!!!!!!
            // Chords = firstChord are taken as incomings
            // Chords > firstChord are left for following time slots
            if (byRel.compare(chord, firstChord) <= 0) {
                // Check that this chord is compatible with each staff term
                for (Staff staff : chord.getStaves()) {
                    Rational staffTerm = stackTerms.get(staff);

                    if ((staffTerm != null) && (staffTerm.compareTo(term) > 0)) {
                        continue PendingLoop;
                    }
                }

                // If chord already has a voice assigned,
                // check time with end of previous chord in same voice
                if (chord.getVoice() != null) {
                    AbstractChordInter lastChord = chord.getVoice().getLastChord();

                    if ((lastChord != null) && (lastChord.getEndTime().compareTo(term) > 0)) {
                        continue;
                    }
                }

                incomings.add(chord);
            }
        }

        dump("incomings", incomings);

        return incomings;
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
        Edge edge = graph.getEdge(from, to);

        if (edge != null) {
            edge.rel = rel;
        } else {
            graph.addEdge(from, to, new Edge(rel));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------//
    // Edge //
    //------//
    /**
     * Meant to store a relation instance (edge) between two chords (vertices).
     */
    protected static class Edge
    {
        //~ Instance fields ------------------------------------------------------------------------

        Rel rel; // Relationship carried by the concrete edge

        //~ Constructors ---------------------------------------------------------------------------
        public Edge (Rel rel)
        {
            this.rel = rel;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction maxSlotDx = new Scale.Fraction(
                1.05,
                "Maximum horizontal delta between a slot and a chord");

        private final Scale.Fraction minInterSlotDx = new Scale.Fraction(
                0.5,
                "Minimum horizontal delta between two slots");

        private final Scale.Fraction maxAdjacencyXGap = new Scale.Fraction(
                0.3, //0.65,
                "Maximum horizontal gap between adjacent chords bounds");

        private final Scale.Fraction maxVerticalOverlap = new Scale.Fraction(
                0.25,
                "Maximum vertical overlap tolerated");
    }

    //-----------//
    // ChordPair //
    //-----------//
    /**
     * Meant to store a pair of chords found as being adjacent.
     */
    private static class ChordPair
    {
        //~ Instance fields ------------------------------------------------------------------------

        final AbstractChordInter one;

        final AbstractChordInter two;

        //~ Constructors ---------------------------------------------------------------------------
        public ChordPair (AbstractChordInter one,
                          AbstractChordInter two)
        {
            this.one = one;
            this.two = two;
            logger.debug(
                    "Adjacent {}@{} & {}@{}",
                    one,
                    one.getHeadLocation(),
                    two,
                    two.getHeadLocation());
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "{ch#" + one.getId() + ",ch#" + two.getId() + "}";
        }
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final int maxSlotDx;

        private final int minInterSlotDx;

        private final int maxAdjacencyXGap;

        private final int maxVerticalOverlap;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxSlotDx = scale.toPixels(constants.maxSlotDx);
            minInterSlotDx = scale.toPixels(constants.minInterSlotDx);
            maxAdjacencyXGap = scale.toPixels(constants.maxAdjacencyXGap);
            maxVerticalOverlap = scale.toPixels(constants.maxVerticalOverlap);
        }
    }
}
