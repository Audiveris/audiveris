//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M e a s u r e R h y t h m                                   //
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

import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.ProcessingSwitches;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.rhythm.ChordsMapper.ChordPair;
import org.audiveris.omr.sheet.rhythm.ChordsMapper.Mapping;
import org.audiveris.omr.sheet.rhythm.Slot.CompoundSlot;
import org.audiveris.omr.sheet.rhythm.Slot.MeasureSlot;
import org.audiveris.omr.sheet.rhythm.SlotsRetriever.Rel;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code MeasureRhythm} handles chords voices and time slots for a measure.
 * <p>
 * Voice and time information can be "propagated" from one chord to other chord(s) in the following
 * slots via two grouping mechanisms: tie and beam.
 * <p>
 * Note that the beam mechanism is able to handle rests embraced between two beamed chords, such
 * rest chords inherit voice and time information (see example below).
 * <br>
 * <img src="doc-files/EmbracedRest.png" alt="EmbracedRest">
 * <p>
 * The following diagram shows the computing of a given slot.
 * <ul>
 * <li>Here we are at slot #3
 * <li>Previous slots have their time offset assigned
 * <li>Some chords, even located in following slots, have their voice already assigned (via tie or
 * beam)
 * <li><b>Active</b> chords are highlighted in yellow
 * <li><b>Incoming</b> chords are pointed by red arrows
 * </ul>
 * <img src="doc-files/SlotComputing.png" alt="SlotComputing">
 *
 * @author Hervé Bitteur
 */
public class MeasureRhythm
{

    private static final Logger logger = LoggerFactory.getLogger(MeasureRhythm.class);

    /** The dedicated measure. */
    private final Measure measure;

    /** Voices no longer active. */
    private final Set<Voice> extinctVoices = new LinkedHashSet<>();

    /** Compound slots detected. */
    private final List<CompoundSlot> slots = new ArrayList<>();

    /** Voice distance adapted to measure part. */
    private final VoiceDistance voiceDistance;

    /** Switch for support of implicit tuplet signs. */
    private final boolean implicitTuplets;

    /** Companion in charge of chords relationships and narrow slots. */
    private SlotsRetriever narrowSlotsRetriever;

    /**
     * Creates a new {@code MeasureRhythm} object.
     *
     * @param measure the measure to process
     */
    public MeasureRhythm (Measure measure)
    {
        this.measure = measure;

        final Part part = measure.getPart();
        final Sheet sheet = measure.getStack().getSystem().getSheet();
        final Scale scale = sheet.getScale();
        voiceDistance = part.isMerged() ? new VoiceDistance.Merged(scale)
                : new VoiceDistance.Separated(scale);
        implicitTuplets = sheet.getStub().getProcessingSwitches().getValue(
                ProcessingSwitches.Switch.implicitTuplets);
    }

    //---------//
    // process //
    //---------//
    /**
     * Process the measure to retrieve voices and time offsets.
     *
     * @return true if OK
     */
    public boolean process ()
    {
        removeImplicitTuplets();

        boolean ok = true;

        for (int pass = 1; pass <= 2; pass++) {
            measure.resetRhythm();
            ok = true;

            // Process all whole rest chords, they are decoupled from any slot
            processWholeRestChords();

            // Retrieve slots
            slots.clear();
            narrowSlotsRetriever = new SlotsRetriever(measure, false); // Wide is false
            List<MeasureSlot> narrowSlots = narrowSlotsRetriever.buildSlots();

            SlotsRetriever wideSlotsRetriever = new SlotsRetriever(measure, true); // Wide is true
            List<MeasureSlot> wideSlots = wideSlotsRetriever.buildSlots();

            // Merge narrow into wide compounds
            slots.addAll(buildCompoundSlots(narrowSlots, wideSlots));

            if (slots.isEmpty()) {
                return ok;
            }

            setFirstSlot(); // Set and push time ZERO for chords of first slot

            for (CompoundSlot slot : slots) {
                ok &= new SlotMapper(slot).mapChords();
                purgeExtinctVoices(slot);
            }

            // Look for implicit tuplets?
            if (!implicitTuplets || (pass > 1)
                        || (measure.getStack().getExpectedDuration() == null)
                        || !new TupletGenerator(measure).findImplicitTuplets()) {
                break;
            }
        }

        if (measure.getStack().getExpectedDuration() != null) {
            measure.checkDuration();
        }

        return ok;
    }

    //--------------------//
    // buildCompoundSlots //
    //--------------------//
    /**
     * Build the compound slots from the provided sequences of narrow and wide slots.
     * <p>
     * TODO: this implementation is very simplistic.
     * We could be more efficient especially when narrow and wide slots are identical!
     *
     * @param narrowSlots sequence of narrow slots
     * @param wideSlots   sequence of wide slots
     * @return the sequence of compound slots
     */
    private List<CompoundSlot> buildCompoundSlots (List<MeasureSlot> narrowSlots,
                                                   List<MeasureSlot> wideSlots)
    {
        final List<CompoundSlot> compounds = new ArrayList<>();
        final int narrowCount = narrowSlots.size();

        int slotCount = 0;
        int iStart = 0;

        WideSlots:
        for (MeasureSlot wide : wideSlots) {
            List<AbstractChordInter> wideChords = wide.getChords();

            for (int i = iStart; i < narrowCount; i++) {
                MeasureSlot narrow = narrowSlots.get(i);

                if (!intersection(wideChords, narrow.getChords())) {
                    compounds.add(
                            new CompoundSlot(++slotCount, measure, narrowSlots.subList(iStart, i)));
                    iStart = i;
                    continue WideSlots;
                }
            }

            // Register last slot
            compounds.add(
                    new CompoundSlot(
                            ++slotCount,
                            measure,
                            narrowSlots.subList(iStart, narrowCount)));
        }

        return compounds;
    }

    //--------------//
    // intersection //
    //--------------//
    /**
     * Report whether intersection of the provided collections is not empty.
     *
     * @param one a collection
     * @param two another collection
     * @return true if intersection is not empty
     */
    private boolean intersection (Collection one,
                                  Collection two)
    {
        for (Object obj : one) {
            if (two.contains(obj)) {
                return true;
            }
        }

        return false;
    }

    //------------------------//
    // processWholeRestChords //
    //------------------------//
    /**
     * Assign a dedicated voice to each whole rest chord in stack.
     */
    private List<Voice> processWholeRestChords ()
    {
        List<Voice> wholeVoices = new ArrayList<>();

        final List<AbstractChordInter> wholes = new ArrayList<>(measure.getWholeRestChords());
        Collections.sort(wholes, Inters.byAbscissa);

        for (AbstractChordInter chord : wholes) {
            chord.setTimeOffset(Rational.ZERO);

            Voice voice = Voice.createWholeVoice((RestChordInter) chord, chord.getMeasure());
            measure.addVoice(voice);
            wholeVoices.add(voice);
        }

        return wholeVoices;
    }

    //--------------------//
    // purgeExtinctVoices //
    //--------------------//
    /**
     * Detect which additional voices got extinct before the provided slot.
     * <p>
     * They can't be involved in mapping of the following slots.
     *
     * @param slot the provided slot
     */
    private void purgeExtinctVoices (CompoundSlot slot)
    {
        final Rational firstTime = slot.getMembers().get(0).getTimeOffset();

        if (firstTime == null) {
            return;
        }

        for (Voice voice : measure.getVoices()) {
            if (!voice.isWhole() && !extinctVoices.contains(voice)) {
                AbstractChordInter lastChord = voice.getLastChord(false);

                if (lastChord.getTimeOffset() != null) {
                    Rational end = lastChord.getEndTime();

                    if (end.compareTo(firstTime) < 0) {
                        logger.debug(
                                "{} {} extinct at {} before slot#{}",
                                voice,
                                Inters.ids(voice.getChords()),
                                end,
                                slot.getId());
                        extinctVoices.add(voice);
                    }
                } else {
                    measure.setAbnormal(true);
                }
            }
        }
    }

    //-----------------------//
    // removeImplicitTuplets //
    //-----------------------//
    /**
     * Remove all implicit tuplet signs before measure is processed again.
     */
    private void removeImplicitTuplets ()
    {
        final MeasureStack stack = measure.getStack();

        for (TupletInter tuplet : new ArrayList<>(measure.getTuplets())) {
            if (tuplet.isImplicit()) {
                measure.removeInter(tuplet);
                stack.removeInter(tuplet);
                tuplet.remove();
            }
        }
    }

    //--------------//
    // setFirstSlot //
    //--------------//
    /**
     * Assign time ZERO to all chords of first slot, and push time information to
     * following grouped chords (via beam or tie) if any.
     */
    private void setFirstSlot ()
    {
        Slot firstSlot = slots.get(0);

        for (AbstractChordInter ch : firstSlot.getChords()) {
            ch.setAndPushTime(Rational.ZERO);
        }
    }

    //------------//
    // SlotMapper //
    //------------//
    /**
     * Class in charge of processing one compound slot.
     */
    private class SlotMapper
    {

        // The compound slot to process
        private final CompoundSlot slot;

        // Incomings
        private final List<AbstractChordInter> rookies;

        // Known incompabilities
        private final Set<ChordPair> incomps = new LinkedHashSet<>();

        // Current mapping
        private Mapping mapping;

        public SlotMapper (CompoundSlot slot)
        {
            this.slot = slot;
            rookies = new ArrayList<>(slot.getChords());
        }

        public boolean mapChords ()
        {
            boolean ok = true;

            // Chords grouped with previous chords
            // Check time consistency of grouped chords in the same narrow slot
            for (MeasureSlot narrow : slot.getMembers()) {
                TreeMap<Rational, List<AbstractChordInter>> times = readSlotTimes(narrow);

                if (times.size() == 1) {
                    // All grouped chords in narrow slot agree, set slot time accordingly
                    Rational time = times.keySet().iterator().next();
                    narrow.setTimeOffset(time);
                } else if (times.size() > 1) {
                    // Time problem detected in a narrow slot
                    // Some data is wrong (recognition problem or implicit tuplet)
                    //TODO: check if some tuplet factor could explain the difference
                    logger.warn("Time inconsistency in {}", times);
                    ok = false;
                }
            }

            // Purge rookies which have voice already defined (via tie or beam group)
            purgeRookiesSet();

            if (rookies.isEmpty()) {
                return ok;
            }

            // Map some rookies to some actives
            mapRookies();

            if (rookies.isEmpty()) {
                return ok;
            }

            // Create a brand new voice for each rookie left
            createNewVoices();

            return ok;
        }

        //--------------//
        // analyzeTimes //
        //--------------//
        /**
         * Further analysis of different time values in the same slot.
         * <p>
         * We can suspect a need for implicit tuplet signs when two time values differ in the
         * specific 3/2 ratio since last synchro.
         * <p>
         * TODO: This is only for information right now.
         *
         * @param times the map of detected times
         */
        private void analyzeTimes (TreeMap<Rational, List<AbstractChordInter>> times)
        {
            Entry<Rational, List<AbstractChordInter>> lastEntry = times.lastEntry();
            Rational bestTime = lastEntry.getKey();

            for (Entry<Rational, List<AbstractChordInter>> entry : times.entrySet()) {
                if (entry.getKey() != bestTime) {
                    for (AbstractChordInter ch : entry.getValue()) {
                        for (AbstractChordInter b : lastEntry.getValue()) {
                            Rational ratio = deltaRatio(ch, entry.getKey(), b, bestTime);
                            logger.debug("ratio {}", ratio);
                        }
                    }
                }
            }
        }

        //--------------//
        // applyMapping //
        //--------------//
        /**
         * The current mapping has been validated, we can now apply it by setting voice
         * and time for real.
         */
        private void applyMapping ()
        {
            for (ChordPair pair : mapping.pairs) {
                AbstractChordInter ch = pair.newChord;
                AbstractChordInter act = pair.oldChord;
                ch.setVoice(act.getVoice());

                if (act.getTimeOffset() != null) {
                    ch.setAndPushTime(act.getEndTime());
                } else {
                    measure.setAbnormal(true);
                }

                rookies.remove(ch);
            }
        }

        //---------------//
        // checkSlotTime //
        //---------------//
        /**
         * Make sure every narrow slot has just one time value.
         * <p>
         * If several time values are inferred, pick up the highest one and reject the mappings
         * that led to other values.
         *
         * @return true if OK, false if some mapping has been rejected.
         */
        private boolean checkSlotTime ()
        {
            boolean ok = true;

            for (MeasureSlot narrow : slot.getMembers()) {
                Rational slotTime = narrow.getTimeOffset();

                if (slotTime == null) {
                    TreeMap<Rational, List<AbstractChordInter>> times = inferSlotTimes(narrow);

                    switch (times.size()) {
                    case 0:
                        // No mapping for this narrow slot
                        logger.debug("no times for {}", narrow);

                        break;

                    case 1:
                        // Perfect case
                        Rational time = times.firstKey();
                        narrow.setTimeOffset(time);

                        break;

                    default:
                        // Several values
                        logger.debug("Slot#{} times {}", slot.getId(), times);
                        ok = false;

                        // Check delta ratio since previous synchro (w/o slot time)
                        analyzeTimes(times);

                        // Pick up the largest time value (likely to be the one for implicit tuplets)
                        Entry<Rational, List<AbstractChordInter>> lastEntry = times.lastEntry();
                        Rational bestTime = lastEntry.getKey();
                        narrow.setTimeOffset(bestTime);

                        // Discard mapping of non-consistent chords
                        for (Entry<Rational, List<AbstractChordInter>> entry : times.entrySet()) {
                            if (entry.getKey() != bestTime) {
                                for (AbstractChordInter ch : entry.getValue()) {
                                    incomps.add(new ChordPair(ch, mapping.ref(ch)));
                                }
                            }
                        }
                    }
                }
            }

            return ok;
        }

        //-----------------//
        // createNewVoices //
        //-----------------//
        /**
         * For each rookie chord that could not be mapped to any existing voice,
         * start a brand new voice.
         */
        private void createNewVoices ()
        {
            for (AbstractChordInter ch : rookies) {
                if (ch.getVoice() == null) {
                    // Assign a brand new voice to this rookie
                    Voice voice = new Voice(ch, measure);
                    measure.addVoice(voice);

                    Rational timeOffset = ch.getTimeOffset();

                    if (timeOffset == null) {
                        // Which time to use?
                        // Use time from EQUAL chord if any, else use time from closest sibling
                        List<AbstractChordInter> siblings = getOrderedSiblings(ch);

                        for (AbstractChordInter sibling : siblings) {
                            if (sibling.getTimeOffset() != null) {
                                timeOffset = sibling.getTimeOffset();
                                ch.setAndPushTime(timeOffset);
                                break;
                            }
                        }

                        if (timeOffset == null) {
                            logger.warn("No timeOffset for {}", ch); // TODO: handle this!!!!!!
                        }
                    }
                }
            }
        }

        //------------//
        // deltaRatio //
        //------------//
        /**
         * Report the delta ratio of the provided two time values
         *
         * @param ch1     chord related to first time value
         * @param target1 first time value
         * @param ch2     chord related to second time value
         * @param target2 second time value
         * @return the time delta ratio since last common slot
         */
        private Rational deltaRatio (AbstractChordInter ch1,
                                     Rational target1,
                                     AbstractChordInter ch2,
                                     Rational target2)
        {
            final Rational lastSynchro = lastSynchro(ch1, ch2);

            if (lastSynchro == null) {
                return null;
            }

            final Rational delta1 = target1.minus(lastSynchro);
            final Rational delta2 = target2.minus(lastSynchro);

            return delta2.divides(delta1);
        }

        //---------------//
        // dumpRookieMap //
        //---------------//
        /**
         * Debugging tool that displays, for each rookie chord, the active chords it can
         * be mapped with.
         *
         * @param actives the collection of available chords
         */
        public void dumpRookieMap (List<AbstractChordInter> actives)
        {
            StringBuilder sb = new StringBuilder();

            for (AbstractChordInter right : rookies) {
                sb.append("\nTo ").append(right).append(":");

                for (AbstractChordInter left : actives) {
                    StringBuilder details = new StringBuilder();
                    int dist = voiceDistance.getDistance(left, right, details);
                    String time = (left.getTimeOffset() != null) ? left.getEndTime().toString()
                            : "NT";
                    sb.append(String.format("%n%5d %4s from %s %s", dist, time, left, details));
                }
            }

            logger.info("{}", sb);
        }

        //--------------------//
        // getOrderedSiblings //
        //--------------------//
        /**
         * For a provided rookie, report the collection of other chords within the same
         * compound slot, ordered by decreasing relevance for sharing same time value.
         *
         * @param rookie the provided rookie
         * @return the ordered collection of siblings
         */
        private List<AbstractChordInter> getOrderedSiblings (final AbstractChordInter rookie)
        {
            final MeasureStack stack = measure.getStack();
            final double x = stack.getXOffset(rookie.getCenter());
            final List<AbstractChordInter> siblings = new ArrayList<>(slot.getChords());
            siblings.remove(rookie);

            Collections.sort(siblings, new Comparator<AbstractChordInter>()
                     {
                         @Override
                         public int compare (AbstractChordInter c1,
                                             AbstractChordInter c2)
                         {
                             // In fact any SlotsRetriever (narrow or wide) could fit!
                             Rel r1 = narrowSlotsRetriever.getRel(rookie, c1);
                             Rel r2 = narrowSlotsRetriever.getRel(rookie, c2);

                             if (r1 == Rel.EQUAL) {
                                 if (r2 == Rel.EQUAL) {
                                     return 0;
                                 } else {
                                     return -1;
                                 }
                             } else {
                                 if (r2 == Rel.EQUAL) {
                                     return +1;
                                 } else {
                                     double x1 = stack.getXOffset(c1.getCenter());
                                     double x2 = stack.getXOffset(c2.getCenter());
                                     return Double.compare(Math.abs(x1 - x), Math.abs(x2 - x));
                                 }
                             }
                         }

                     });

            return siblings;
        }

        //--------------//
        // getSetChords //
        //--------------//
        /**
         * Among the provided chords, report those with time offset already set.
         *
         * @param chords all provided chords
         * @return the chords with time set
         */
        private List<AbstractChordInter> getSetChords (List<AbstractChordInter> chords)
        {
            List<AbstractChordInter> set = new ArrayList<>();

            for (AbstractChordInter chord : chords) {
                if (chord.getTimeOffset() != null) {
                    set.add(chord);
                }
            }

            return set;
        }

        //----------------//
        // inferSlotTimes //
        //----------------//
        /**
         * Based on the current mapping proposal, infer the possible slot time(s) for
         * the provided narrow slot.
         *
         * @param narrowSlot the narrow slot to inspect
         * @return map of time values inferred
         */
        private TreeMap<Rational, List<AbstractChordInter>> inferSlotTimes (MeasureSlot narrowSlot)
        {
            final TreeMap<Rational, List<AbstractChordInter>> times = new TreeMap<>();

            for (AbstractChordInter chord : narrowSlot.getChords()) {
                AbstractChordInter act = mapping.ref(chord);

                if (act != null) {
                    if (act.getTimeOffset() != null) {
                        Rational end = act.getEndTime();
                        List<AbstractChordInter> list = times.get(end);

                        if (list == null) {
                            times.put(end, list = new ArrayList<>());
                        }

                        list.add(chord);
                    } else {
                        measure.setAbnormal(true);
                    }
                }
            }

            return times;
        }

        //-------------//
        // lastSynchro //
        //-------------//
        /**
         * Given two chords (with different time values) in separate voices, look back
         * in these voices for the last common time value.
         *
         * @param ch1 chord in a voice
         * @param ch2 chord in a different voice
         * @return the last time value these two voices have in common
         */
        private Rational lastSynchro (AbstractChordInter ch1,
                                      AbstractChordInter ch2)
        {
            List<Rational> l1 = timesUntil(ch1);
            List<Rational> l2 = timesUntil(ch2);

            List<Rational> commons = new ArrayList<>(l1);
            commons.retainAll(l2);

            if (commons.isEmpty()) {
                return null;
            }

            return commons.get(commons.size() - 1);
        }

        //------------//
        // mapRookies //
        //------------//
        /**
         * Try to map some rookies to some active voices.
         * <p>
         * Strategy for <b>no-implicit-tuplet-mode</b> is the following:
         * <p>
         * The principle is to assume that all chords in a given narrow slot share the same time.
         * However, we may not know this slot time initially.
         * The slot time is initially known only it it's the slot #1 in measure or if it contains at
         * least one chord grouped (via beam or tie) to a chord in a previous slot.
         * <p>
         * Otherwise, the slot time has to be determined by the slot chords themselves, using the
         * end time of their previous chords in their mapped voices.
         * If all chords don't agree, we select the largest time value.
         * <p>
         * For any chord in slot which does not fit with the slot time, we reject its voice mapping
         * and new voice mappings can be tempted.
         * <p>
         * If no suitable mapping can be found, the chord is left over.
         * (These chords left over will end up creating new voices).
         */
        private void mapRookies ()
        {
            // Still active chords
            final List<AbstractChordInter> actives = retrieveActives();

            if (actives.isEmpty()) {
                return; // Since we have nothing to map with!
            }

            if (logger.isDebugEnabled() || Entities.containsVip(rookies)) {
                dumpRookieMap(actives);
            }

            boolean done = false;

            Iteration:
            while (!done) {
                done = true;
                ChordsMapper mapper = new ChordsMapper(rookies, actives, voiceDistance, incomps);
                mapping = mapper.process();

                if (!mapping.pairs.isEmpty()) {
                    // Check mapping before actual commit
                    for (MeasureSlot narrow : slot.getMembers()) {
                        final Rational slotTime = narrow.getTimeOffset();

                        if (slotTime != null) {
                            final List<AbstractChordInter> narrowChords = narrow.getChords();
                            final List<AbstractChordInter> setChords = getSetChords(narrowChords);

                            for (ChordPair pair : mapping.pairsOf(narrowChords)) {
                                final AbstractChordInter ch = pair.newChord;
                                final AbstractChordInter act = pair.oldChord;
                                final Rational end = act.getEndTime();

                                if (!end.equals(slotTime)) {
                                    logger.debug("{} slotTime:{} end:{}", slot, slotTime, end);

                                    // Check delta ratio since previous synchro (w/ slot time)
                                    for (AbstractChordInter setCh : setChords) {
                                        Rational ratio = deltaRatio(act, end, setCh, slotTime);
                                        logger.debug("ratio {}", ratio);
                                    }

                                    // Discard
                                    incomps.add(pair);
                                    done = false;
                                    continue Iteration;
                                }
                            }
                        }
                    }

                    // Check each narrow slot time is known
                    if (!checkSlotTime()) {
                        done = false;
                    }

                    if (done) {
                        applyMapping(); // Apply the mapping for real
                    }
                }
            }
        }

        //-----------------//
        // purgeRookiesSet //
        //-----------------//
        /**
         * Purge rookies which have voice already set (via tie or beam group).
         */
        private void purgeRookiesSet ()
        {
            for (Iterator<AbstractChordInter> it = rookies.iterator(); it.hasNext();) {
                AbstractChordInter ch = it.next();
                Voice voice = ch.getVoice();

                if (voice != null) {
                    it.remove();
                }
            }
        }

        //---------------//
        // readSlotTimes //
        //---------------//
        /**
         * Report the different chord time values as found in the provided narrow slot.
         *
         * @param narrowSlot the narrow slot to inspect
         * @return the map of times found
         */
        private TreeMap<Rational, List<AbstractChordInter>> readSlotTimes (MeasureSlot narrowSlot)
        {
            TreeMap<Rational, List<AbstractChordInter>> times = new TreeMap<>();

            for (AbstractChordInter chord : narrowSlot.getChords()) {
                Rational time = chord.getTimeOffset();

                if (time != null) {
                    List<AbstractChordInter> list = times.get(time);

                    if (list == null) {
                        times.put(time, list = new ArrayList<>());
                    }

                    list.add(chord);
                }
            }

            return times;
        }

        //-----------------//
        // retrieveActives //
        //-----------------//
        /**
         * Retrieve all the chords that are still active before current compound slot
         * and could be mapped to slot chords.
         *
         * @return the active chords
         */
        private List<AbstractChordInter> retrieveActives ()
        {
            List<AbstractChordInter> actives = new ArrayList<>();

            for (Voice voice : measure.getVoices()) {
                if (!voice.isWhole() && !extinctVoices.contains(voice)) {
                    AbstractChordInter lastChord = voice.getLastChord(false);

                    if (lastChord != null) {
                        // Make sure voice lastChord slot precedes this slot
                        if (lastChord.getSlot().compareTo(slot) < 0) {
                            actives.add(lastChord);
                        }
                    } else {
                        logger.error("Voice {} with no last chord", voice);
                    }
                }
            }

            return actives;
        }

        //------------//
        // timesUntil //
        //------------//
        /**
         * Report the history of time values in the voice the provided chord is set to
         * (or mapped to).
         *
         * @param chord the provided chord
         * @return voice history until this chord
         */
        private List<Rational> timesUntil (AbstractChordInter chord)
        {
            final List<Rational> list = new ArrayList<>();
            Voice voice = chord.getVoice();

            if (voice == null) {
                voice = mapping.ref(chord).getVoice();
            }

            for (AbstractChordInter ch : voice.getChords()) {
                Rational time = ch.getTimeOffset();

                if (time != null) {
                    list.add(time);
                }

                if (ch == chord) {
                    break;
                }
            }

            return list;
        }
    }
}
