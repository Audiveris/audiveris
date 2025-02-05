//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M e a s u r e R h y t h m                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import static org.audiveris.omr.math.Rational.THREE_OVER_TWO;
import static org.audiveris.omr.math.Rational.TWO_OVER_THREE;
import static org.audiveris.omr.math.Rational.ZERO;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.ProcessingSwitch;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.rhythm.ChordsMapper.Mapping;
import org.audiveris.omr.sheet.rhythm.Slot.CompoundSlot;
import org.audiveris.omr.sheet.rhythm.Slot.MeasureSlot;
import org.audiveris.omr.sheet.rhythm.SlotsRetriever.Rel;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.relation.BeamRestRelation;
import org.audiveris.omr.sig.relation.NextInVoiceRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SeparateVoiceRelation;
import org.audiveris.omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import static java.util.stream.Collectors.toList;

/**
 * Class <code>MeasureRhythm</code> handles chords voices and time slots for a measure.
 * <p>
 * Voice and time information can be "propagated" from one chord to other chord(s) in the following
 * slots via two grouping mechanisms: tie and beam, plus the "Next in Voice" manual relation.
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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MeasureRhythm.class);

    //~ Instance fields ----------------------------------------------------------------------------

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

    /** Companion in charge of tuplet generation. */
    private final TupletGenerator tupletGenerator;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>MeasureRhythm</code> object.
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
                ProcessingSwitch.implicitTuplets);
        tupletGenerator = implicitTuplets ? new TupletGenerator(measure) : null;
    }

    //~ Methods ------------------------------------------------------------------------------------

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
            final List<AbstractChordInter> wideChords = wide.getChords();

            for (int i = iStart; i < narrowCount; i++) {
                final MeasureSlot narrow = narrowSlots.get(i);

                if (!intersection(wideChords, narrow.getChords())) {
                    if (i > iStart) {
                        compounds.add(
                                new CompoundSlot(
                                        ++slotCount,
                                        measure,
                                        narrowSlots.subList(iStart, i)));
                        iStart = i;

                        continue WideSlots;
                    }
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

    //-----------------------//
    // clearInterleavedRests //
    //-----------------------//
    /**
     * Clear all {@link BeamRestRelation} instances for this measure, except the manual
     * instances.
     *
     * @since 5.2.3 (before this release, information was (wrongly) detected on-the-fly and not
     *        recorded in project file)
     */
    private void clearInterleavedRests ()
    {
        for (BeamGroupInter beamGroup : measure.getBeamGroups()) {
            for (Inter member : beamGroup.getMembers()) {
                final AbstractBeamInter beam = (AbstractBeamInter) member;
                for (Relation rel : beam.getSig().getRelations(beam, BeamRestRelation.class)) {
                    if (!rel.isManual()) {
                        beam.getSig().removeEdge(rel);
                    }
                }
            }
        }
    }

    //------------------------//
    // detectInterleavedRests //
    //------------------------//
    /**
     * Detect the rest chords that are interleaved between beam chords, and record
     * this information as a {@link BeamRestRelation} between beam and rest.
     *
     * @since 5.2.3 (before this release, information was (wrongly) detected on-the-fly and not
     *        recorded in project file)
     */
    private void detectInterleavedRests ()
    {
        for (BeamGroupInter beamGroup : measure.getBeamGroups()) {
            beamGroup.detectInterleavedRests();
        }
    }

    //-----------//
    // dumpSlots //
    //-----------//
    /**
     * Dump a list of slots.
     *
     * @param title title to be printed
     * @param slots the slot list to dump
     */
    private void dumpSlots (String title,
                            List<? extends MeasureSlot> slots)
    {
        final StringBuilder sb = new StringBuilder(measure.toString());
        sb.append(' ').append(title);

        for (MeasureSlot slot : slots) {
            sb.append("\n   ").append(slot);
        }

        logger.info("{}", sb);
    }

    //------------//
    // finalCheck //
    //------------//
    /**
     * Run a final check on the measure.
     *
     * @return true if ok
     */
    private boolean finalCheck ()
    {
        boolean ok = true;

        // Check if every measure chord has a voice and a time offset
        for (AbstractChordInter ch : measure.getStandardChords()) {
            ok &= (ch.getVoice() != null);
            ok &= (ch.getTimeOffset() != null);
        }

        // Check measure duration for every voice
        if (measure.getStack().getExpectedDuration() != null) {
            measure.checkDuration();
            ok &= !measure.isAbnormal();
        }

        return ok;
    }

    //--------------------//
    // getImplicitTuplets //
    //--------------------//
    /**
     * Report the collection of implicit tuplets in this measure.
     *
     * @return all the implicit tuplets in measure
     */
    private List<TupletInter> getImplicitTuplets ()
    {
        return measure.getTuplets().stream().filter(t -> t.isImplicit()).collect(toList());
    }

    //-----------------------//
    // getVoicesWithImplicit //
    //-----------------------//
    /**
     * Report the collection of voices that contain at least one implicit tuplet.
     *
     * @return the voices with an implicit tuplet
     */
    private Set<Voice> getVoicesWithImplicit ()
    {
        Set<Voice> found = null;

        for (Voice voice : measure.getVoices()) {
            for (AbstractChordInter chord : voice.getChords()) {
                final TupletInter tuplet = chord.getTuplet();

                if ((tuplet != null) && tuplet.isImplicit()) {
                    if (found == null) {
                        found = new LinkedHashSet<>();
                    }

                    found.add(voice);

                    break;
                }
            }
        }

        if (found == null) {
            return Collections.emptySet();
        }

        return found;
    }

    //------------------//
    // inspectVoicesEnd //
    //------------------//
    /**
     * Inspect the last portion of voices which have been assigned an implicit tuplet.
     * <p>
     * This is needed to process the chords that follow the last tuplet-involved chord.
     */
    private void inspectVoicesEnd ()
    {
        final Rational expectedDur = measure.getStack().getExpectedDuration();

        for (Voice voice : getVoicesWithImplicit()) {
            final List<AbstractChordInter> chords = voice.getChords();
            final AbstractChordInter lastChordWithTuplet = voice.getLastChordWithTuplet();

            if (lastChordWithTuplet == null) {
                continue;
            }

            final AbstractChordInter first = voice.getChordAfter(lastChordWithTuplet);

            if (first != null) {
                final Rational start = first.getTimeOffset();
                boolean apply = true;

                if (expectedDur != null) {
                    // Check excess ratio
                    final Rational stop = voice.getLastChord().getEndTime();
                    final Rational actualDur = stop.minus(start);
                    final Rational normalDur = expectedDur.minus(start);

                    if (normalDur.equals(Rational.ZERO)) {
                        apply = false;
                    } else {
                        final Rational ratio = actualDur.divides(normalDur);

                        if (Rational.THREE_OVER_TWO.equals(ratio)) {
                            logger.debug("{} Last tuplet portion for {}", measure, voice);
                        } else {
                            logger.debug("{} No last tuplet portion for {}", measure, voice);
                            apply = false;
                        }
                    }
                } else {
                    // Assume we can apply tuplet to the last portion
                    logger.debug(
                            "{} no expected duration, last tuplet portion for {}",
                            measure,
                            voice);
                }

                if (apply) {
                    shrinkVoice(null, voice, start);
                }
            }
        }
    }
    //
    //    //------------------//
    //    // inspectVoicesEnd //
    //    //------------------//
    //    /**
    //     * Inspect the last portion without tuplets of voices, for any (implicit) tuplet-based
    //     * adjustment to fit within the measure.
    //     * <p>
    //     * For every voice with an excess compared to the measure end time, we go back from end to
    //     * start, looking for group of chords that a tuplet would shrink enough.
    //     */
    //    private void inspectVoicesEnd ()
    //    {
    //        final Rational expectedDur = measure.getStack().getExpectedDuration();
    //        if (expectedDur == null) {
    //            return;
    //        }
    //
    //        NextVoice:
    //        for (Voice voice : measure.getVoices()) {
    //            // Excess?
    //            final Rational stop = voice.getLastChord().getEndTime();
    //            final Rational excess = stop.minus(expectedDur);
    //
    //            if (excess.compareTo(Rational.ZERO) <= 0) {
    //                continue;
    //            }
    //
    //            final Rational start = expectedDur.minus(excess.times(2));
    //
    //            //  Go back
    //            final List<AbstractChordInter> chords = voice.getChords();
    //            final AbstractChordInter lastChordWithTuplet = voice.getLastChordWithTuplet();
    //
    //            for (int idx = chords.size() - 1; idx >= 0; idx--) {
    //                final AbstractChordInter ch = chords.get(idx);
    //                if (ch == lastChordWithTuplet) {
    //                    continue NextVoice;
    //                }
    //
    //                if (ch.getTimeOffset().equals(start)) {
    //                    logger.info("BINGO");
    //                    shrinkVoice(null, voice, start);
    //                    continue NextVoice;
    //                }
    //            }
    //        }
    //    }

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

    //--------------//
    // mergeTuplets //
    //--------------//
    /**
     * Check the implicit tuplet signs and replace 3 by 6 where needed.
     */
    private void mergeTuplets ()
    {
        for (Voice voice : getVoicesWithImplicit()) {
            final List<TupletInter> tuplets = voice.getTuplets();

            TupletInter prevTuplet = null;
            List<AbstractChordInter> prevGroup = null;
            BeamGroupInter bgLast = null;

            for (TupletInter tuplet : tuplets) {
                List<AbstractChordInter> group = tuplet.getChords();

                if (bgLast != null) {
                    BeamGroupInter bgFirst = group.get(0).getBeamGroup();

                    if (bgFirst == bgLast) {
                        logger.debug("Merge {} with {}", prevTuplet, tuplet);
                        removeTuplet(prevTuplet);
                        removeTuplet(tuplet);

                        List<AbstractChordInter> doubleGroup = new ArrayList<>();
                        doubleGroup.addAll(prevGroup);
                        doubleGroup.addAll(group);

                        List<AbstractChordInter> extGroup = new ArrayList<>();
                        tupletGenerator.generateTuplets(doubleGroup, extGroup);
                    }
                }

                prevTuplet = tuplet;
                prevGroup = group;
                bgLast = group.get(group.size() - 1).getBeamGroup();
            }
        }
    }

    //-------------//
    // prevInVoice //
    //-------------//
    /**
     * Report the previous chord with the same voice as the provided chord.
     * <p>
     * We use the relation NextInVoice.
     *
     * @param right the provided chord
     * @return the previous chord in voice, or null
     */
    private AbstractChordInter prevInVoice (AbstractChordInter right)
    {
        final SIGraph sig = right.getSig();

        for (Relation rel : sig.incomingEdgesOf(right)) {
            if (rel instanceof NextInVoiceRelation) {
                return (AbstractChordInter) sig.getOppositeInter(right, rel);
            }
        }

        return null;
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
        removeTuplets(getImplicitTuplets());

        clearInterleavedRests();
        detectInterleavedRests();

        // Second pass can be used only when implicit tuplets option is enabled
        for (int pass = 1;; pass++) {
            logger.debug("\n{} pass {}", measure, pass);
            measure.resetRhythm();

            // Retrieve narrow slots
            slots.clear();
            narrowSlotsRetriever = new SlotsRetriever(measure, false); // Narrow

            final List<MeasureSlot> narrowSlots = narrowSlotsRetriever.buildSlots();
            if (logger.isTraceEnabled()) {
                dumpSlots("narrowSlots", narrowSlots);
            }

            // Retrieve wide slots
            final SlotsRetriever wideSlotsRetriever = new SlotsRetriever(measure, true); // Wide
            final List<MeasureSlot> wideSlots = wideSlotsRetriever.buildSlots();
            if (logger.isTraceEnabled()) {
                dumpSlots("wideSlots", wideSlots);
            }

            // Merge narrow into wide compounds
            slots.addAll(buildCompoundSlots(narrowSlots, wideSlots));
            if (logger.isTraceEnabled()) {
                dumpSlots("compoundSlots", slots);
            }

            // Starting chords (measure-long rests plus chords of first slot)
            processStartingChords();

            if (slots.isEmpty()) {
                break;
            }

            // Slots list may be shrunk dynamically
            for (int i = 0; i < slots.size(); i++) {
                final CompoundSlot slot = slots.get(i);
                new SlotMapper(slot).mapChords();

                if (slots.contains(slot)) {
                    purgeExtinctVoices(slot);
                } else {
                    i--;
                }
            }

            if (implicitTuplets) {
                // Inspect last portion of voices
                inspectVoicesEnd();

                // Generate more realistic implicit tuplets
                mergeTuplets();

                if ((pass == 1) && (measure.getStack().getExpectedDuration() != null)) {
                    // Check implicit tuplets on entire measure
                    final List<TupletInter> oldImplicits = getImplicitTuplets();
                    final List<TupletInter> newImplicits = tupletGenerator.findImplicitTuplets();

                    if (!newImplicits.isEmpty()) {
                        // Run pass #2 with the new implicits generated on measure-long voices
                        removeTuplets(oldImplicits);
                        continue;
                    }
                }
            }

            break;
        }

        return finalCheck();
    }

    //-----------------------//
    // processStartingChords //
    //-----------------------//
    /**
     * Process all measure-long rests as well as chords from the first slot.
     * <ul>
     * <li>Assign their voice number, ordered by their vertical position
     * <li>Set their time offset to ZERO
     * </ul>
     */
    private void processStartingChords ()
    {
        final List<AbstractChordInter> rookies = new ArrayList<>();

        final Set<AbstractChordInter> measureRestsChords = measure.getMeasureRestChords();
        rookies.addAll(measureRestsChords);

        if (!slots.isEmpty()) {
            rookies.addAll(slots.get(0).getChords());
        }

        Collections.sort(rookies, Inters.byCenterOrdinate);

        for (AbstractChordInter ch : rookies) {
            // Voice
            measure.addVoice(
                    measureRestsChords.contains(ch) //
                            ? Voice.createMeasureRestVoice((RestChordInter) ch, measure)
                            : new Voice(ch, measure));

            // Time
            ch.setAndPushTime(Rational.ZERO);
        }
    }

    //--------------------//
    // purgeExtinctVoices //
    //--------------------//
    /**
     * Detect which additional voices got extinct before the provided slot.
     * <p>
     * They can't be involved in mapping of the following slots.
     * <p>
     * A voice cannot be flagged as extinct if one of its chords has a NextInVoice
     * relation with a chord located later that the current slot in the same measure.
     *
     * @param slot the provided slot
     */
    private void purgeExtinctVoices (CompoundSlot slot)
    {
        final Rational firstTime = slot.getMembers().get(0).getTimeOffset();

        if (firstTime == null) {
            return;
        }

        VoiceLoop:
        for (Voice voice : measure.getVoices()) {
            if (!voice.isMeasureRest() && !extinctVoices.contains(voice)) {
                final AbstractChordInter lastChord = voice.getLastChord();

                if (lastChord.getTimeOffset() != null) {
                    final Rational end = lastChord.getEndTime();

                    if (end.compareTo(firstTime) < 0) {
                        // Lookup following NextInVoice chord(s) within the current measure
                        AbstractChordInter next = lastChord.getNextChordInVoiceSequence();

                        while ((next != null) && (next.getMeasure() == measure)) {
                            if (slot.compareTo(next.getSlot()) <= 0) {
                                continue VoiceLoop;
                            }

                            next = next.getNextChordInVoiceSequence();
                        }

                        if (lastChord.isVip() || logger.isDebugEnabled()) {
                            logger.info(
                                    "{} {} extinct at {} before slot#{} at {}",
                                    voice,
                                    Inters.ids(voice.getChords()),
                                    end,
                                    slot.getId(),
                                    firstTime);
                        }

                        extinctVoices.add(voice);
                    }
                } else {
                    measure.setAbnormal(true);
                }
            }
        }
    }

    //--------------//
    // removeTuplet //
    //--------------//
    private void removeTuplet (TupletInter tuplet)
    {
        final MeasureStack stack = measure.getStack();
        measure.removeInter(tuplet);
        stack.removeInter(tuplet);
        tuplet.remove();
    }

    //---------------//
    // removeTuplets //
    //---------------//
    /**
     * Remove the provided tuplet signs.
     */
    private void removeTuplets (Collection<TupletInter> tuplets)
    {
        for (TupletInter tuplet : tuplets) {
            removeTuplet(tuplet);
        }
    }

    //-------------//
    // shrinkVoice //
    //-------------//
    /**
     * Shrink times and durations via tuplet for portion of voice since last synchro.
     * <ul>
     * <li>In range [startChord..stopChord[, apply tuplet on chord duration
     * <li>in range ]startChord..stopChord], set time offset on chord (and on related slot)
     *
     * @param stopChord current chord in voice
     * @param lastSync  last good synchro
     */
    private void shrinkVoice (AbstractChordInter stopChord,
                              Voice voice,
                              Rational lastSync)
    {
        if ((voice == null) && (stopChord != null)) {
            voice = stopChord.getVoice();
        }

        if (voice == null) {
            return;
        }

        logger.debug("rectifyVoice stopChord:{} voice:{} lastSync: {}", stopChord, voice, lastSync);

        // Retrieve grouped chords
        final List<AbstractChordInter> chords = voice.getChords();
        int iFirst = 0;

        for (int i = 0; i < chords.size(); i++) {
            final AbstractChordInter chord = chords.get(i);
            final Rational time = chord.getTimeOffset();

            if (time.equals(lastSync)) {
                iFirst = i;

                break;
            }
        }

        final int iBreak = ((stopChord != null) && chords.contains(stopChord)) ? chords.indexOf(
                stopChord) : chords.size();
        final List<AbstractChordInter> group = chords.subList(iFirst, iBreak);

        if (group.isEmpty()) {
            return;
        }

        // Generate implicit tuplets for the group
        final List<AbstractChordInter> extGroup = new ArrayList<>();
        tupletGenerator.generateTuplets(group, extGroup);

        // Modify time offset for chords and slots
        AbstractChordInter prevChord = null;

        for (AbstractChordInter chord : extGroup) {
            if (prevChord != null) {
                final Rational newTime = prevChord.getEndTime();

                if (!newTime.equals(chord.getTimeOffset())) {
                    chord.setAndPushTime(newTime);
                }

                final MeasureSlot chSlot = ((CompoundSlot) chord.getSlot()).getNarrowSlot(chord);
                chSlot.setTimeOffset(newTime);
            }

            prevChord = chord;
        }

        if ((prevChord != null) && (stopChord != null)) {
            final Rational newTime = prevChord.getEndTime();

            if (!newTime.equals(stopChord.getTimeOffset())) {
                stopChord.setAndPushTime(newTime);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

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

        // Known voice incompabilities
        private final Set<ChordPair> blackList;

        // Explicit next in voice links
        private final Set<ChordPair> nextList;

        // Current mapping
        private Mapping mapping;

        public SlotMapper (CompoundSlot slot)
        {
            this.slot = slot;
            rookies = new ArrayList<>(slot.getChords());

            blackList = buildSet(SeparateVoiceRelation.class);
            nextList = buildSet((NextInVoiceRelation.class));
        }

        //--------------//
        // analyzeTimes //
        //--------------//
        /**
         * Further analysis of different time values in the same slot.
         * <p>
         * We can suspect a need for implicit tuplet signs when two time values differ in the
         * specific 3/2 ratio since last synchro.
         *
         * @param times the map of detected times
         * @return true if OK
         */
        private boolean analyzeTimes (TreeMap<Rational, List<AbstractChordInter>> times)
        {
            final Entry<Rational, List<AbstractChordInter>> bestEntry = times.firstEntry();
            final Rational bestTime = bestEntry.getKey();
            boolean ok = true;

            for (AbstractChordInter best : bestEntry.getValue()) {
                for (Entry<Rational, List<AbstractChordInter>> entry : times.entrySet()) {
                    if (entry.getKey() != bestTime) {
                        for (AbstractChordInter ch : entry.getValue()) {
                            if (implicitTuplets) {
                                final Rational ratio = deltaRatio(
                                        ch,
                                        entry.getKey(),
                                        best,
                                        bestTime);
                                logger.debug("Ratio {} at {}", ratio, best);

                                if (ratio == null) {
                                    continue;
                                } else if (ZERO.equals(ratio)) {
                                    continue;
                                } else if (THREE_OVER_TWO.equals(ratio)) {
                                    final Rational lastSync = lastSynchro(best, ch);
                                    logger.debug("T1 for {} since {}", entry.getValue(), lastSync);

                                    final Voice voice = (mapping != null) //
                                            ? mapping.ref(ch).getVoice()
                                            : ch.getVoice();
                                    if (voice != null) {
                                        shrinkVoice(ch, voice, lastSync);

                                        continue;
                                    }
                                }
                            }

                            // Discard
                            if (mapping != null) {
                                final ChordPair cp = new ChordPair(ch, mapping.ref(ch), null);
                                logger.debug("Blacklisting {}", cp);
                                blackList.add(cp);
                            }

                            ok = false;
                        }
                    }
                }
            }

            return ok;
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
                AbstractChordInter ch = pair.rookie;
                AbstractChordInter act = pair.active;
                ch.setVoice(act.getVoice());

                if (act.getTimeOffset() != null) {
                    ch.setAndPushTime(act.getEndTime());
                } else {
                    measure.setAbnormal(true);
                }

                rookies.remove(ch);
            }
        }

        //----------//
        // buildSet //
        //----------//
        /**
         * Initialize the set by looking up for explicit relation class instances.
         *
         * @return the populated sameList
         */
        private Set<ChordPair> buildSet (Class<?> classe)
        {
            final Set<ChordPair> set = new LinkedHashSet<>();

            for (AbstractChordInter ch : rookies) {
                final SIGraph sig = ch.getSig();

                for (Relation rel : sig.getRelations(ch, classe)) {
                    AbstractChordInter other = (AbstractChordInter) sig.getOppositeInter(ch, rel);
                    set.add(new ChordPair(ch, other, null));
                }
            }

            return set;
        }

        //---------------//
        // checkSlotTime //
        //---------------//
        /**
         * Make sure every narrow slot has just one time value.
         * <p>
         * If several time values are inferred, pick up the lowest one and reject the mappings
         * that led to other values.
         *
         * @return true if OK, false if some mapping has been rejected.
         */
        private boolean checkSlotTime ()
        {
            boolean ok = true;

            for (MeasureSlot narrow : slot.getMembers()) {
                final Rational slotTime = narrow.getTimeOffset();

                if (slotTime == null) {
                    final TreeMap<Rational, List<AbstractChordInter>> times //
                            = inferSlotTimes(narrow);

                    switch (times.size()) {
                        // No mapping for this narrow slot
                        case 0 -> logger.debug("No times for {}", narrow);

                        // Perfect case
                        case 1 -> narrow.setTimeOffset(times.firstKey());

                        // Several values
                        default -> {
                            logger.debug("Times {}", times);
                            // Check delta ratio since previous synchro (w/o slot time)
                            ok &= analyzeTimes(times);

                            // Pick up the lowest time value
                            narrow.setTimeOffset(times.firstEntry().getKey());
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
                    final Voice voice = new Voice(ch, measure);
                    measure.addVoice(voice);

                    Rational timeOffset = ch.getTimeOffset();

                    if (timeOffset == null) {
                        // Which time to use?
                        // Use time from EQUAL chord if any, else use time from closest sibling
                        final List<AbstractChordInter> siblings = getOrderedSiblings(ch);

                        for (AbstractChordInter sibling : siblings) {
                            if (sibling.getTimeOffset() != null) {
                                timeOffset = sibling.getTimeOffset();
                                ch.setAndPushTime(timeOffset);

                                break;
                            }
                        }

                        if (timeOffset == null) {
                            if (siblings.isEmpty()) {
                                // This chord is alone in its time slot
                                // If adjacent with a chord in previous slot, join previous slot
                                if (mergeWithPreviousSlot(ch)) {
                                    return;
                                }
                            }

                            logger.info("{} No timeOffset for {}", measure, ch);
                        }
                    }
                }
            }
        }

        //------------//
        // deltaRatio //
        //------------//
        /**
         * Report the delta ratio of the provided two time values, larger then smaller.
         *
         * @param ch1     chord related to first time value
         * @param target1 first time value (the larger)
         * @param ch2     chord related to second time value
         * @param target2 second time value (the smaller)
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

            if (delta2.equals(Rational.ZERO)) {
                return Rational.ZERO;
            }

            return delta1.divides(delta2);
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
            final StringBuilder sb = new StringBuilder();
            sb.append("Slot #").append(slot.getId()).append(" rookieMap:");

            for (AbstractChordInter rookie : rookies) {
                sb.append("\n   rookie ").append(rookie).append(":");

                for (AbstractChordInter act : actives) {
                    final StringBuilder details = new StringBuilder();
                    final int dist = voiceDistance.getDistance(act, rookie, details);
                    final String time = (act.getTimeOffset() != null) //
                            ? act.getEndTime().toString()
                            : "NT";
                    final String T = act.hasTuplet() ? "T" : " ";
                    sb.append(
                            String.format(
                                    "%n%8d %4s %1s active %s %s",
                                    dist,
                                    time,
                                    T,
                                    act,
                                    details));
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

            Collections.sort(
                    siblings,
                    (AbstractChordInter c1,
                     AbstractChordInter c2) -> {
                        // In fact any SlotsRetriever (narrow or wide) could fit!
                        final Rel r1 = narrowSlotsRetriever.getRel(rookie, c1);
                        final Rel r2 = narrowSlotsRetriever.getRel(rookie, c2);

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
            final List<AbstractChordInter> set = new ArrayList<>();

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
                final AbstractChordInter act = mapping.ref(chord);

                if (act != null) {
                    if (act.getTimeOffset() != null) {
                        final Rational end = act.getEndTime();
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
         * in these voices for the last common time value, without tuplet.
         *
         * @param ch1 chord in a voice
         * @param ch2 chord in a different voice
         * @return the last time value these two voices have in common
         */
        private Rational lastSynchro (AbstractChordInter ch1,
                                      AbstractChordInter ch2)
        {
            final List<Rational> l1 = timesUntil(ch1);
            final List<Rational> l2 = timesUntil(ch2);

            final List<Rational> commons = new ArrayList<>(l1);
            commons.retainAll(l2);

            if (commons.isEmpty()) {
                if (!l1.isEmpty()) {
                    return l1.get(0);
                }

                return null;
            }

            return commons.get(commons.size() - 1);
        }

        //-----------//
        // mapChords //
        //-----------//
        public boolean mapChords ()
        {
            boolean ok = true;

            // Chords grouped with previous chords
            // Check time consistency of grouped chords in the same narrow slot
            for (MeasureSlot narrow : slot.getMembers()) {
                final TreeMap<Rational, List<AbstractChordInter>> times = readSlotTimes(narrow);

                if (times.size() == 1) {
                    // All grouped chords in narrow slot agree, set slot time accordingly
                    final Rational time = times.keySet().iterator().next();
                    narrow.setTimeOffset(time);
                } else if (times.size() > 1) {
                    // Time problem detected in a narrow slot
                    // Some data is wrong (recognition problem or implicit tuplet)
                    logger.info("{} Time inconsistency in {}", measure, times);
                    ok &= analyzeTimes(times);
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
         * The slot time is initially known only if it's the slot #1 in measure or if it contains at
         * least one chord grouped (via beam, tie or NextInVoice) to a chord in a previous slot.
         * <p>
         * Otherwise, the slot time has to be determined by the slot chords themselves, using the
         * end time of their previous chords in their mapped voices.
         * If all chords don't agree, we select the smallest time value.
         * <p>
         * For any chord in slot which does not fit with the slot time, we reject its voice mapping
         * and new voice mappings can be tried.
         * <p>
         * If no suitable mapping can be found, the rookie chord is left over.
         * (These chords left over will end up creating new voices).
         */
        private void mapRookies ()
        {
            final Rational measureDuration = measure.getStack().getExpectedDuration();

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
                final ChordsMapper mapper = new ChordsMapper(
                        rookies,
                        actives,
                        voiceDistance,
                        blackList,
                        nextList);
                mapping = mapper.process();
                logger.debug("{}", mapping);

                ///mapper.processAll(); // Feature not yet OK

                if (mapping.pairs.isEmpty()) {
                    return;
                }

                // Check mapping before actual commit
                for (MeasureSlot narrow : slot.getMembers()) {
                    final Rational slotTime = narrow.getTimeOffset();

                    if (slotTime != null) {
                        final List<AbstractChordInter> narrowChords = narrow.getChords();
                        final List<AbstractChordInter> setChords = getSetChords(narrowChords);

                        for (ChordPair pair : mapping.pairsOf(narrowChords)) {
                            final AbstractChordInter ch = pair.rookie;
                            final AbstractChordInter act = pair.active;
                            final Rational actEnd = act.getEndTime();

                            if (actEnd == null) {
                                return; // Computing cannot continue
                            }

                            if (measureDuration != null) {
                                final Rational chEnd = actEnd.plus(ch.getDuration());
                                if (chEnd.compareTo(measureDuration) > 0) {
                                    logger.debug("Too late ending for {} plus {}", act, ch);
                                    if (!nextList.contains(pair) && !blackList.contains(pair)) {
                                        blackList.add(pair);
                                        done = false;

                                        continue Iteration;
                                    }
                                }
                            }

                            if (!actEnd.equals(slotTime)) {
                                logger.debug("{} slotTime:{} end:{}", slot, slotTime, actEnd);

                                if (implicitTuplets) {
                                    // Check delta ratio since previous synchro (w/ slot time)
                                    for (AbstractChordInter setCh : setChords) {
                                        Rational ratio = deltaRatio(setCh, slotTime, act, actEnd);
                                        logger.debug("Ratio {} at {}", ratio, ch);

                                        if (ratio == null) {
                                            continue;
                                        } else if (ZERO.equals(ratio)) {
                                            continue;
                                        } else if (THREE_OVER_TWO.equals(ratio)) {
                                            final AbstractChordInter ref = mapping.ref(setCh);
                                            if (ref != null) {
                                                final Rational lastSync = lastSynchro(setCh, act);
                                                logger.debug("T2 for {} since {}", setCh, lastSync);
                                                shrinkVoice(setCh, ref.getVoice(), lastSync);
                                            }
                                        } else if (TWO_OVER_THREE.equals(ratio)) {
                                            final Rational lastSync = lastSynchro(act, setCh);
                                            logger.debug("T3 for {} since {}", act, lastSync);
                                            shrinkVoice(act, null, lastSync);
                                        } else {
                                            // Discard
                                            if (blackList.contains(pair)) {
                                                break Iteration; // It's useless to keep trying
                                            }

                                            blackList.add(pair);
                                            done = false;

                                            continue Iteration;
                                        }
                                    }
                                } else {
                                    // Discard
                                    if (blackList.contains(pair)) {
                                        break Iteration; // It's useless to keep trying
                                    }

                                    blackList.add(pair);
                                    done = false;

                                    continue Iteration;
                                }
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

        //-----------------------//
        // mergeWithPreviousSlot //
        //-----------------------//
        /**
         * Try to merge this slot (containing just one rookie with no time offset)
         * with previous slot if it contains a chord EQUAL to rookie.
         *
         * @param rookie the single rookie to deal with
         * @return true if success
         */
        private boolean mergeWithPreviousSlot (AbstractChordInter rookie)
        {
            final CompoundSlot prevSlot = slots.get(slot.id - 2);

            for (AbstractChordInter ch : prevSlot.getChords()) {
                final Rel rel = narrowSlotsRetriever.getRel(ch, rookie);

                if (rel == Rel.EQUAL) {
                    final Rational timeOffset = ch.getTimeOffset();

                    if (timeOffset != null) {
                        prevSlot.getChords().add(rookie);
                        rookie.setAndPushTime(timeOffset);

                        for (CompoundSlot sl : slots.subList(slot.id, slots.size())) {
                            sl.setId(sl.getId() - 1);
                        }

                        slots.remove(slot);

                        return true;
                    }
                }
            }

            return false;
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
            final TreeMap<Rational, List<AbstractChordInter>> times = new TreeMap<>();

            for (AbstractChordInter chord : narrowSlot.getChords()) {
                final Rational time = chord.getTimeOffset();

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
         * <p>
         * A chord, belonging to an extinct voice, can still be kept, if there is an
         * explicit NextInVoiceRelation between this chord and a rookie.
         *
         * @return the active chords
         */
        private List<AbstractChordInter> retrieveActives ()
        {
            final Rational measureDuration = measure.getStack().getExpectedDuration();
            final List<AbstractChordInter> actives = new ArrayList<>();

            for (Voice voice : measure.getVoices()) {
                if (voice.isMeasureRest()) {
                    continue;
                }

                final AbstractChordInter lastChord = voice.getLastChord();

                // Make sure there is some time left after lastChord end
                if (measureDuration != null) {
                    final Rational lastChordEnd = lastChord.getEndTime();

                    if ((lastChordEnd != null) && lastChordEnd.compareTo(measureDuration) >= 0) {
                        if (!implicitTuplets) {
                            continue;
                        } else {
                            // Consider the current count of chords in voice
                            final int chordNb = voice.getChords().size();
                            if (chordNb == 1) {
                                continue;
                            }
                        }
                    }
                }

                // Make sure voice lastChord slot precedes this slot
                if (lastChord.getSlot().compareTo(slot) > 0) {
                    continue;
                }

                if (!extinctVoices.contains(voice)) {
                    actives.add(lastChord);
                } else {
                    // Keep extinct voice chord with an explicit next voice relation to a rookie
                    for (ChordPair p : nextList) {
                        if ((p.active == lastChord) && (rookies.contains(p.rookie))) {
                            actives.add(lastChord);

                            break;
                        }
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
         * (or mapped to), without a tuplet.
         *
         * @param chord the provided chord
         * @return voice history until this chord
         */
        private List<Rational> timesUntil (AbstractChordInter chord)
        {
            final List<Rational> list = new ArrayList<>();
            Voice voice = chord.getVoice();

            if (voice == null) {
                if (mapping != null) {
                    voice = mapping.ref(chord).getVoice();
                } else {
                    final AbstractChordInter prev = prevInVoice(chord);
                    if (prev != null) {
                        voice = prev.getVoice();
                    }
                }
            }

            if (voice != null) {
                for (AbstractChordInter ch : voice.getChords()) {
                    final Rational time = ch.getTimeOffset();

                    if (time != null && !ch.hasTuplet()) {
                        list.add(time);
                    }

                    if (ch == chord) {
                        break;
                    }
                }
            }

            return list;
        }
    }
}
