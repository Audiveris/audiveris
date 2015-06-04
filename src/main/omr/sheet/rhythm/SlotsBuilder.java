//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S l o t s B u i l d e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.constant.ConstantSet;

import omr.math.GeoUtil;
import omr.math.Rational;

import omr.sheet.Scale;
import omr.sheet.Staff;
import omr.sheet.beam.BeamGroup;
import static omr.sheet.rhythm.SlotsBuilder.Rel.*;

import omr.sig.inter.AbstractBeamInter;
import omr.sig.inter.AbstractNoteInter;
import omr.sig.inter.ChordInter;
import omr.sig.inter.HeadChordInter;
import omr.sig.inter.Inter;
import omr.sig.inter.Inters;
import omr.sig.inter.RestChordInter;

import net.jcip.annotations.NotThreadSafe;

import org.jgrapht.graph.SimpleDirectedGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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

    /** The ChordInter to remove from stack current content. */
    private final Set<RestChordInter> toRemove;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Inter-chord relationships for the current measure stack. */
    private SimpleDirectedGraph<ChordInter, Link> graph = new SimpleDirectedGraph<ChordInter, Link>(
            Link.class);

    /** Earliest term for each staff in stack. */
    private final Map<Staff, Rational> stackTerms = new LinkedHashMap<Staff, Rational>();

    /** Comparator based on inter-chord relationships, then on startTime when known. */
    private final Comparator<ChordInter> byRel = new Comparator<ChordInter>()
    {
        @Override
        public int compare (ChordInter c1,
                            ChordInter c2)
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

                    // Use start time difference when known
                    if ((c1.getStartTime() != null) && (c2.getStartTime() != null)) {
                        return c1.getStartTime().compareTo(c2.getStartTime());
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
     * @param toRemove (output) the set of rest chords to remove
     */
    public SlotsBuilder (MeasureStack stack,
                         Set<RestChordInter> toRemove)
    {
        this.stack = stack;
        this.toRemove = toRemove;

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

        if (!buildSlots()) {
            return false;
        }

        refineVoices();

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
    private boolean areAdjacent (ChordInter ch1,
                                 ChordInter ch2)
    {
        // Adjacency cannot occur if at least 1 rest-chord is involved.
        if (ch1.isRest() || ch2.isRest()) {
            return false;
        }

        final Rectangle box1 = ch1.getBounds();
        final Rectangle box2 = ch2.getBounds();

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

            // Case of nearly shared heads (put on slighly different x because deltaPitch = 1)
            // TODO: perhaps check for minimum x overlap?
            if ((xGap < 0) && (ch1.getStemDir() != ch2.getStemDir())) {
                AbstractNoteInter h1 = ch1.getLeadingNote();
                AbstractNoteInter h2 = ch2.getLeadingNote();

                // TODO: perhaps accept deltaPitch of 2 only with very similar abscissae?
                if (Math.abs(h1.getIntegerPitch() - h2.getIntegerPitch()) <= 2) {
                    return true;
                }
            }

            // If beam on each side -> false (different groups!)
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
        // Sort measure chords by abscissa
        Collections.sort(stack.getChords(), Inter.byAbscissa);

        // Populate graph with chords
        for (ChordInter chord : stack.getChords()) {
            graph.addVertex(chord);
        }

        // BeamGroup-based relationships
        inspectBeams();

        // Mirror-based relationships
        inspectMirrors();

        // Finally, default location-based relationships
        inspectLocations();

        if (logger.isDebugEnabled()) {
            dumpRelationships();
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
        toRemove.clear();

        // The 'actives' collection gathers the chords that are not terminated at the
        // time slot being considered. Initially, it contains just the whole chords.
        List<ChordInter> actives = new ArrayList<ChordInter>(stack.getWholeRestChords());
        Collections.sort(actives, ChordInter.byAbscissa);

        // Create voices for whole chords
        handleWholeVoices(actives);

        // List of chords assignable, but not yet assigned to a slot
        List<ChordInter> pendings = getPendingChords();

        // Assign chords to time slots, until no chord is left pending
        while (!pendings.isEmpty()) {
            dump("actives", actives);

            // Earliest end time among all active chords
            Rational term = computeNextTerm(actives);

            // Which chords end here, and is their voice available or not for the slot?
            // (if a beam group continues, its voice remains locked)
            List<ChordInter> freeEndings = new ArrayList<ChordInter>();
            List<ChordInter> endings = new ArrayList<ChordInter>();
            detectEndings(actives, term, endings, freeEndings);

            // Do we have pending chords that start at this slot?
            List<ChordInter> incomings = retrieveIncomingChords(pendings, term);

            if (!incomings.isEmpty()) {
                // Allocate the slot with the incoming chords
                int slotId = stack.getSlots().size() + 1;
                Slot slot = new Slot(slotId, stack);
                stack.getSlots().add(slot);
                slot.setChords(incomings);

                // Check slots time so far are consistent & that dx with previous slot is wide enough
                if (!slot.setStartTime(term) || !checkInterSlot(slot)) {
                    return false;
                }

                // Determine the voice of each chord in the slot
                slot.buildVoices(freeEndings);
            }

            // Prepare for next iteration
            pendings.removeAll(incomings);
            actives.addAll(incomings);

            actives.removeAll(freeEndings);
            actives.removeAll(endings);
        }

        return true;
    }

    //----------------//
    // checkInterSlot //
    //----------------//
    /**
     * Check whether the provided slot is sufficiently distinct from previous one.
     * if not, a modified config candidate is posted
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

        // We are too close, find out the guilty chord?
        logger.debug("Too close slot {}", slot);

        for (ChordInter prevChord : prevSlot.getChords()) {
            Rectangle prevRect = prevChord.getBounds();

            for (ChordInter nextChord : slot.getChords()) {
                Rectangle nextRect = nextChord.getBounds();

                if (GeoUtil.yOverlap(prevRect, nextRect) > 0) {
                    if (prevChord.isVip() || nextChord.isVip() || logger.isDebugEnabled()) {
                        logger.info("VIP slots overlap between {} & {}", prevChord, nextChord);
                    }

                    // Exclude the pair members in pending candidates!
                    if (prevChord instanceof HeadChordInter) {
                        if (nextChord instanceof RestChordInter) {
                            toRemove.add((RestChordInter) nextChord);
                        }
                    } else if (nextChord instanceof HeadChordInter) {
                        toRemove.add((RestChordInter) prevChord);
                    }
                }
            }
        }

        return false;
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
     * @param actives chords still active
     */
    private Rational computeNextTerm (Collection<ChordInter> actives)
    {
        Rational nextTerm = Rational.MAX_VALUE;
        stackTerms.clear();

        for (ChordInter chord : actives) {
            // Skip the "whole" chords, since they don't expire before measure end
            if (!chord.isWholeRest()) {
                Rational endTime = chord.getEndTime();
                Staff staff = chord.getStaff();
                Rational staffTerm = stackTerms.get(staff);

                if ((staffTerm == null) || (endTime.compareTo(staffTerm) < 0)) {
                    stackTerms.put(staff, endTime);

                    if (endTime.compareTo(nextTerm) < 0) {
                        nextTerm = endTime;
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
    private void detectEndings (List<ChordInter> actives,
                                Rational term,
                                List<ChordInter> endings,
                                List<ChordInter> freeEndings)
    {
        for (ChordInter chord : actives) {
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
                       Collection<ChordInter> chords)
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
     */
    private void dumpRelationships ()
    {
        // List BeamGroups
        if (logger.isDebugEnabled()) {
            for (BeamGroup group : stack.getBeamGroups()) {
                logger.info("  {}", group);
            }
        }

        // List chords relationships
        StringBuilder sb = new StringBuilder("    ");
        List<ChordInter> chords = stack.getChords();
        int length = chords.size();

        for (int ix = 0; ix < length; ix++) {
            sb.append(String.format(" %4d", chords.get(ix).getId()));
        }

        for (int iy = 0; iy < length; iy++) {
            ChordInter chy = chords.get(iy);
            sb.append("\n");
            sb.append(String.format("%4d", chy.getId()));

            for (int ix = 0; ix < length; ix++) {
                sb.append("  ");

                ChordInter chx = chords.get(ix);

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
    private Set<ChordInter> getClosure (ChordInter chord)
    {
        Set<ChordInter> closes = new LinkedHashSet<ChordInter>();
        closes.add(chord);

        for (ChordInter ch : stack.getChords()) {
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
     * @return all stack chords, except the whole ones
     */
    private List<ChordInter> getPendingChords ()
    {
        List<ChordInter> pendings = new ArrayList<ChordInter>();

        for (ChordInter chord : stack.getChords()) {
            if (!chord.isWholeRest()) {
                pendings.add(chord);
            }
        }

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
    private Rel getRel (ChordInter from,
                        ChordInter to)
    {
        Link link = graph.getEdge(from, to);

        if (link != null) {
            return link.rel;
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
    private void handleWholeVoices (List<ChordInter> wholes)
    {
        for (ChordInter chord : wholes) {
            chord.setStartTime(Rational.ZERO);

            Voice voice = Voice.createWholeVoice(chord, chord.getMeasure());
            stack.getVoices().add(voice);
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
    private boolean haveCommonHead (ChordInter ch1,
                                    ChordInter ch2)
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
        for (BeamGroup group : stack.getBeamGroups()) {
            Set<ChordInter> chordSet = new HashSet<ChordInter>();

            for (AbstractBeamInter beam : group.getBeams()) {
                chordSet.addAll(beam.getChords());
            }

            final List<ChordInter> groupChords = new ArrayList<ChordInter>(chordSet);
            Collections.sort(groupChords, ChordInter.byAbscissa);

            for (int i = 0; i < groupChords.size(); i++) {
                ChordInter ch1 = groupChords.get(i);

                for (ChordInter ch2 : groupChords.subList(i + 1, groupChords.size())) {
                    setRel(ch1, ch2, BEFORE);
                    setRel(ch2, ch1, AFTER);
                }
            }
        }
    }

    //------------------//
    // inspectLocations //
    //------------------//
    /**
     * Derive the missing inter-chord relationships from chords relative locations.
     */
    private void inspectLocations ()
    {
        final List<ChordInter> measureChords = stack.getChords();
        final List<ChordPair> adjacencies = new ArrayList<ChordPair>();

        for (int i = 0; i < measureChords.size(); i++) {
            ChordInter ch1 = measureChords.get(i);

            if (ch1.isWholeRest()) {
                continue;
            }

            Rectangle box1 = ch1.getBounds();

            for (ChordInter ch2 : measureChords.subList(i + 1, measureChords.size())) {
                if (ch1.isVip() && ch2.isVip()) {
                    logger.info("VIP inspectLocations {} vs {}", ch1, ch2);
                }

                if (ch2.isWholeRest() || (getRel(ch1, ch2) != null)) {
                    continue;
                }

                // Check y overlap
                Rectangle box2 = ch2.getBounds();
                int yOverlap = GeoUtil.yOverlap(box1, box2);

                if (yOverlap > 0) {
                    // Boxes overlap vertically
                    if (areAdjacent(ch1, ch2)) {
                        setRel(ch1, ch2, EQUAL);
                        setRel(ch2, ch1, EQUAL);
                        adjacencies.add(new ChordPair(ch1, ch2));
                    } else {
                        if (ch1.getCenter().x <= ch2.getCenter().x) {
                            setRel(ch1, ch2, BEFORE);
                            setRel(ch2, ch1, AFTER);
                        } else {
                            setRel(ch2, ch1, BEFORE);
                            setRel(ch1, ch2, AFTER);
                        }
                    }
                } else {
                    // Boxes do not overlap vertically
                    int dx = Math.abs(ch1.getCenter().x - ch2.getCenter().x);

                    if (dx <= params.maxSlotDx) {
                        setRel(ch1, ch2, CLOSE);
                        setRel(ch2, ch1, CLOSE);
                    } else {
                        if (ch1.getCenter().x <= ch2.getCenter().x) {
                            setRel(ch1, ch2, BEFORE);
                            setRel(ch2, ch1, AFTER);
                        } else {
                            setRel(ch2, ch1, BEFORE);
                            setRel(ch1, ch2, AFTER);
                        }
                    }
                }
            }
        }

        // Process detected adjacencies
        if (!adjacencies.isEmpty()) {
            logger.info("Adjacencies: {}", adjacencies);

            for (ChordPair pair : adjacencies) {
                // Since ch1 ~ ch2, all neighbors of ch1 ~ neighbors of ch2
                Set<ChordInter> n1 = getClosure(pair.one);
                Set<ChordInter> n2 = getClosure(pair.two);

                for (ChordInter ch1 : n1) {
                    for (ChordInter ch2 : n2) {
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
        final List<ChordInter> stackChords = stack.getChords();

        for (int i = 0; i < stackChords.size(); i++) {
            ChordInter ch1 = stackChords.get(i);

            if (ch1.isWholeRest()) {
                continue;
            }

            for (ChordInter ch2 : stackChords.subList(i + 1, stackChords.size())) {
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

    //------------------------//
    // propagateRelationships //
    //------------------------//
    /**
     * Since the chords in the provided pair are mirrored, they share identical
     * relationships with the other chords in stack
     *
     * @param pair array of exactly 2 chords
     */
    private void propagateRelationships (ChordInter... pair)
    {
        final List<ChordInter> stackChords = stack.getChords();

        for (ChordInter ch : stackChords) {
            for (int i = 0; i < 2; i++) {
                ChordInter one = pair[i];
                ChordInter two = pair[(i + 1) % 2];

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

    //--------------//
    // refineVoices //
    //--------------//
    /**
     * Slight improvements to voices in the current measure stack.
     * TODO: could be improved, so that from stack to stack voice colors stay in their part
     * (this is mandatory) and (as much as possible) in the same staff.
     */
    private void refineVoices ()
    {
        //        // Preserve vertical voice order at beginning of measure
        //        // Use chords from first time slot + whole chords
        //        List<ChordInter> firsts = new ArrayList<ChordInter>();
        //
        //        if (!stack.getSlots().isEmpty()) {
        //            firsts.addAll(stack.getSlots().get(0).getChords());
        //        }
        //
        //        if (stack.getWholeRestChords() != null) {
        //            firsts.addAll(stack.getWholeRestChords());
        //        }
        //
        //        Collections.sort(firsts, ChordInter.byOrdinate);
        //
        //        // Rename voices accordingly
        //        for (int i = 0; i < firsts.size(); i++) {
        //            ChordInter chord = firsts.get(i);
        //            Voice voice = chord.getVoice();
        //            stack.swapVoiceId(voice, i + 1);
        //        }

        // Sort voices vertically in stack
        List<Voice> voices = stack.getVoices();
        Collections.sort(voices, Voice.byOrdinate);

        // Assign each voice ID according to its relative vertical position
        for (int i = 0; i < voices.size(); i++) {
            voices.get(i).setId(i + 1);
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
    private List<ChordInter> retrieveIncomingChords (List<ChordInter> pendings,
                                                     Rational term)
    {
        Collections.sort(pendings, byRel);
        dump("pendings", pendings);

        List<ChordInter> incomings = new ArrayList<ChordInter>();
        ChordInter firstChord = pendings.get(0);

        for (ChordInter chord : pendings) {
            // Here all chords should be >= firstChord
            // Chords < firstChord indicate a startTime inconsistent with slot ordering !!!!!!!
            // Chords = firstChord are taken as incomings
            // Chords > firstChord are left for following time slots
            if (byRel.compare(chord, firstChord) <= 0) {
                // Check that this is compatible with staff term
                Staff staff = chord.getStaff();
                Rational staffTerm = stackTerms.get(staff);

                if ((staffTerm == null) || (staffTerm.compareTo(term) >= 0)) {
                    incomings.add(chord);
                }

                // TODO: use a break condition based on sufficient x gap from firstChord
                //            } else {
                //                break;
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
     * @param from     source chord
     * @param totarget chord
     * @param rel      the relationship value
     */
    private void setRel (ChordInter from,
                         ChordInter to,
                         Rel rel)
    {
        Link link = graph.getEdge(from, to);

        if (link != null) {
            link.rel = rel;
        } else {
            graph.addEdge(from, to, new Link(rel));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------//
    // Link //
    //------//
    /**
     * Meant to store a relation instance (edge) between two chords (vertices).
     */
    protected static class Link
    {
        //~ Instance fields ------------------------------------------------------------------------

        Rel rel;

        //~ Constructors ---------------------------------------------------------------------------
        public Link (Rel rel)
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
                1,
                "Maximum horizontal delta between a slot and a chord");

        private final Scale.Fraction minInterSlotDx = new Scale.Fraction(
                0.5,
                "Minimum horizontal delta between two slots");

        private final Scale.Fraction maxAdjacencyXGap = new Scale.Fraction(
                0.5,
                "Maximum horizontal gap between adjacent chords bounds");
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

        final ChordInter one;

        final ChordInter two;

        //~ Constructors ---------------------------------------------------------------------------
        public ChordPair (ChordInter one,
                          ChordInter two)
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

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxSlotDx = scale.toPixels(constants.maxSlotDx);
            minInterSlotDx = scale.toPixels(constants.minInterSlotDx);
            maxAdjacencyXGap = scale.toPixels(constants.maxAdjacencyXGap);
        }
    }
}
