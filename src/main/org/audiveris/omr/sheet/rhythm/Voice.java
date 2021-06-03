//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            V o i c e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.math.GCD;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.score.Mark;
import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sheet.Staff;
import static org.audiveris.omr.sheet.rhythm.SlotVoice.Status;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code Voice} gathers all informations related to a voice within a measure.
 * <p>
 * A voice is a sequence of chords, each played at a certain time slot in the containing measure.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "voice")
public class Voice
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Voice.class);

    //~ Enumerations -------------------------------------------------------------------------------
    //--------//
    // Family //
    //--------//
    /**
     * To classify voices (and specifically their ID) according to their "height".
     */
    public static enum Family
    {

        /** Started in first staff, or chord with upward stem in merged grand staff. */
        HIGH,
        /** Started in second staff, or chord with downward stem in merged grand staff. */
        LOW,
        /** Started in third staff. */
        INFRA;

        /**
         * Offset in voice ID, according to voice family.
         * <ol>
         * <li>1-4 for HIGH family (first staff in standard part, upward stem in merged part)
         * <li>5-8 for LOW family (second staff in standard part, downward stem in merged part)
         * <li>9-12 for INFRA family (third staff in a 3-staff organ system)
         * </ol>
         */
        private static final int ID_FAMILY_OFFSET = 4;

        /**
         * Report the offset to be used for voice IDs within this family.
         *
         * @return the family ID offset
         */
        public int idOffset ()
        {
            return ID_FAMILY_OFFSET * ordinal();
        }

        /**
         * Report the id values for this family.
         *
         * @return array of family id values
         */
        public int[] ids ()
        {
            final int[] ids = new int[ID_FAMILY_OFFSET];
            final int offset = idOffset();

            for (int i = 0; i < ID_FAMILY_OFFSET; i++) {
                ids[i] = 1 + offset + i;
            }

            return ids;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------
    // Persistent data
    //----------------
    //
    /** The voice id. */
    @XmlAttribute
    private int id;

    /** Excess voice duration, if any. */
    @XmlAttribute
    @XmlJavaTypeAdapter(Rational.Adapter.class)
    private Rational excess;

    /**
     * Old wholeRestChord, if any, to be replaced by measureRestChord.
     */
    @XmlIDREF
    @XmlAttribute(name = "whole-rest-chord") // Renamed as measure-rest-chord
    @Deprecated
    private RestChordInter oldWholeRestChord;

    /**
     * Measure rest chord of the voice, if any.
     * If a voice is assigned to a measure/multi rest, then this rest chord is defined as the
     * measureRestChord of this voice, and the slots table is left empty.
     */
    @XmlIDREF
    @XmlAttribute(name = "measure-rest-chord")
    private RestChordInter measureRestChord;

    /**
     * Map (SlotId -> SlotVoice) to store chord information for each slot.
     * If the voice/slot combination is empty, the voice is free for this slot.
     * Otherwise, the active chord is referenced with a status flag to make a difference between a
     * slot where the chord starts, and the potential following slots for which the chord is still
     * active.
     */
    @XmlElement
    private TreeMap<Integer, SlotVoice> slots;

    // Transient data
    //---------------
    //
    /** Containing measure. */
    @Navigable(false)
    private Measure measure;

    /** The staff in which this voice started. */
    private Staff startingStaff;

    /** The related family. */
    private Family family;

    /** The sequence of chords. */
    private List<AbstractChordInter> chords;

    /**
     * How the voice finishes (value = voiceEndTime - expectedMeasureEndTime).
     * - null: We can't tell
     * - negative: Voice is too short WRT measure expected duration
     * - zero: Voice equals the measure expected duration
     * - positive: Voice is too long WRT measure expected duration
     */
    private Rational termination;

    /** Inferred time signature based on this voice content. */
    private TimeRational inferredTimeSig;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Voice object.
     *
     * @param chord   the initial chord for this voice
     * @param measure the containing measure
     */
    public Voice (AbstractChordInter chord,
                  Measure measure)
    {
        initTransient(measure);

        startingStaff = chord.getTopStaff();

        family = measure.inferVoiceFamily(chord);

        id = measure.generateVoiceId(family);

        if (chord.isMeasureRest()) {
            measureRestChord = (RestChordInter) chord;
        }

        chord.setVoice(this);

        logger.debug("Created voice#{}", id);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private Voice ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // afterReload //
    //-------------//
    /**
     * To be called right after unmarshalling.
     *
     * @param measure the containing measure
     * @return true if upgraded
     */
    public boolean afterReload (Measure measure)
    {
        try {
            this.measure = measure;

            final boolean upgraded = upgradeOldStuff();

            // Set chord voices
            if (isMeasureRest()) {
                measureRestChord.setVoice(this);
            } else if (slots != null) {
                for (SlotVoice info : slots.values()) {
                    if (info.status == Status.BEGIN) {
                        info.chord.justAssignVoice(this);
                        addChord(info.chord);

                        for (AbstractBeamInter beam : info.chord.getBeams()) {
                            if (beam.getGroup() != null) {
                                beam.getGroup().justAssignVoice(this);
                            }
                        }
                    }
                }
            }

            return upgraded;
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
            return false;
        }
    }

    //---------------//
    // checkDuration //
    //---------------//
    /**
     * Check the duration of the voice, compared to stack expected duration.
     */
    public void checkDuration ()
    {
        try {
            if (isMeasureRest()) {
                setTermination(null); // we can't tell anything
            } else if ((chords != null) && !chords.isEmpty()) {
                AbstractChordInter last = chords.get(chords.size() - 1);

                if (last.getTimeOffset() == null) {
                    measure.setAbnormal(true);
                } else {
                    Rational voiceEnd = last.getEndTime();
                    Rational expected = measure.getStack().getExpectedDuration();
                    Rational delta = voiceEnd.minus(expected);
                    setTermination(delta);

                    if (delta.compareTo(Rational.ZERO) > 0) {
                        MeasureStack stack = measure.getStack();
                        excess = delta; // For voice
                        logger.info("{} {} too long", measure, this);
                        measure.setAbnormal(true);

                        if ((stack.getExcess() == null) || (delta.compareTo(stack.getExcess()) > 0)) {
                            stack.setExcess(delta); // For stack
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Error checking {} duration " + ex, measure, ex);
            measure.setAbnormal(true);
        }
    }

    //-------------------//
    // completeSlotTable //
    //-------------------//
    /**
     * Complete the slotTable, by inserting CONTINUE items where needed.
     */
    public void completeSlotTable ()
    {
        AbstractChordInter prevChord = null;

        for (Slot slot : measure.getStack().getSlots()) {
            SlotVoice info = getSlotInfo(slot);

            if (info == null) {
                if ((prevChord != null)
                            && (prevChord.getEndTime().compareTo(slot.getTimeOffset()) > 0)) {
                    putSlotInfo(slot, new SlotVoice(prevChord, Status.CONTINUE));
                }
            } else {
                prevChord = info.chord;
            }
        }
    }

    //----------------//
    // getChordBefore //
    //----------------//
    /**
     * Retrieve within this voice the latest chord, if any, before the provided chord.
     *
     * @param chord the provided chord
     * @return the chord just before if any
     */
    AbstractChordInter getChordBefore (AbstractChordInter chord)
    {
        if (chords != null) {
            final int chordIndex = chords.indexOf(chord);

            if (chordIndex > 0) {
                return chords.get(chordIndex - 1);
            }
        }

        return null;
    }

    //----------------//
    // getChordBefore //
    //----------------//
    /**
     * Retrieve within this voice the latest chord, if any, before the provided slot.
     *
     * @param slot the provided slot
     * @return the latest chord, in this voice, before the provided slot
     */
    public AbstractChordInter getChordBefore (Slot slot)
    {
        AbstractChordInter prevChord = null;

        if (slots != null) {
            for (Map.Entry<Integer, SlotVoice> entry : slots.entrySet()) {
                if (slot.getId() == entry.getKey()) {
                    break;
                }

                SlotVoice info = entry.getValue();

                if (info != null) {
                    prevChord = info.chord;
                }
            }
        }

        return prevChord;
    }

    //-------------//
    // getDuration //
    //-------------//
    /**
     * Report the precise duration of this voice.
     * <p>
     * Note that if the voice is just a measure/multi rest we have no precise duration (the voice
     * duration will be the measure duration, whatever it is), hence we report a null value.
     *
     * @return the voice duration or null
     */
    public Rational getDuration ()
    {
        if (measureRestChord != null) {
            return null;
        }

        Rational voiceDur = Rational.ZERO;

        for (Slot slot : measure.getStack().getSlots()) {
            SlotVoice info = getSlotInfo(slot);

            if ((info != null) && (info.status == Status.BEGIN)) {
                Rational timeOffset = slot.getTimeOffset();

                if (timeOffset == null) {
                    // Slot with no timeOffset, due to stack rhythm not correct yet
                    return null;
                }

                Rational chordEnd = timeOffset.plus(info.chord.getDuration());

                if (chordEnd.compareTo(voiceDur) > 0) {
                    voiceDur = chordEnd;
                }
            }
        }

        return voiceDur;
    }

    //-----------------------//
    // getDurationSansTuplet //
    //-----------------------//
    /**
     * Report the voice duration without counting any tuplet effect.
     *
     * @return the tuplet-free duration of voice
     */
    public Rational getDurationSansTuplet ()
    {
        if (measureRestChord != null) {
            return null;
        }

        Rational voiceDur = Rational.ZERO;

        for (AbstractChordInter ch : chords) {
            voiceDur = voiceDur.plus(ch.getDurationSansTuplet());
        }

        return voiceDur;
    }

    //-----------//
    // getFamily //
    //-----------//
    /**
     * Report the family (HIGH, LOW, INFRA) this voice belongs to.
     *
     * @return the family
     */
    public Family getFamily ()
    {
        return family;
    }

    //---------------//
    // getFirstChord //
    //---------------//
    /**
     * Report the first chord of this voice.
     *
     * @return the first chord, which may be a whole/multi
     */
    public AbstractChordInter getFirstChord ()
    {
        if (isMeasureRest()) {
            return measureRestChord;
        }

        if ((chords != null) && !chords.isEmpty()) {
            return chords.get(0);
        }

        return null;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the voice id, starting from 1.
     *
     * @return the voice id
     */
    public int getId ()
    {
        return id;
    }

    //-------//
    // setId //
    //-------//
    /**
     * Change the voice id (to rename voices)
     *
     * @param id the new id value
     */
    public void setId (int id)
    {
        ///logger.debug("measure#{} {} renamed as {}", measure.getIdValue(), this, id);
        this.id = id;
    }

    //--------------------------//
    // getInferredTimeSignature //
    //--------------------------//
    /**
     * Report the time signature value that can be inferred from content of this voice.
     *
     * @return the "intrinsic" time signature rational value for this voice, or null
     */
    public TimeRational getInferredTimeSignature ()
    {
        if (inferredTimeSig == null) {
            try {
                // Sequence of group (beamed or isolated chords) durations
                List<Rational> durations = new ArrayList<>();

                // Voice time offset
                Rational timeOffset = null;

                // Start time of last note in group, if any
                Rational groupLastTime = null;

                if (slots != null) {
                    for (Map.Entry<Integer, SlotVoice> entry : slots.entrySet()) {
                        SlotVoice info = entry.getValue();

                        if (info.status == Status.BEGIN) {
                            AbstractChordInter chord = info.chord;

                            // Skip the remaining parts of beam group, including embraced rests
                            if ((groupLastTime != null) && (chord.getTimeOffset().compareTo(
                                    groupLastTime) <= 0)) {
                                continue;
                            }

                            BeamGroupInter group = chord.getBeamGroup();

                            if (group == null) {
                                // Isolated chord
                                durations.add(chord.getDuration());
                            } else {
                                // Starting a new group
                                Rational groupDuration = group.getDuration();

                                if (groupDuration != null) {
                                    durations.add(groupDuration);
                                    groupLastTime = group.getLastChord().getTimeOffset();
                                }
                            }

                            if (timeOffset == null) {
                                Slot slot = measure.getStack().getSlots().get(entry.getKey() - 1);
                                timeOffset = slot.getTimeOffset();
                            }
                        }
                    }
                }

                // Debug
                if (logger.isDebugEnabled()) {
                    StringBuilder sb = new StringBuilder("[");
                    boolean started = false;
                    Rational total = null;

                    for (Rational dur : durations) {
                        if (started) {
                            sb.append(",");
                        }

                        started = true;

                        if (dur == null) {
                            sb.append("null");
                        } else {
                            sb.append(dur);

                            if (total == null) {
                                total = dur;
                            } else {
                                total = total.plus(dur);
                            }
                        }
                    }

                    sb.append("] total:");

                    if (total != null) {
                        sb.append(total);
                    } else {
                        sb.append("null");
                    }

                    logger.debug("{}: {}", this, sb);
                }

                // Check this voice fills the measure stack
                if ((timeOffset == null) || !timeOffset.equals(Rational.ZERO)) {
                    return null;
                }

                if ((termination == null) || !termination.equals(Rational.ZERO)) {
                    return null;
                }

                // Do we have a regular pattern?
                int count = 0;
                Rational common = null;

                for (Rational dur : durations) {
                    if (common == null) {
                        common = dur;
                    } else if (!common.equals(dur)) {
                        break;
                    }

                    count++;
                }

                if ((common != null) && (count == durations.size())) {
                    // All the durations are equal
                    inferredTimeSig = timeSigOf(count, common);
                }
            } catch (Exception ex) {
                logger.warn("Could not guess time signature for {} {}", this, ex.toString(), ex);

                return null;
            }
        }

        return inferredTimeSig;
    }

    //--------------//
    // getLastChord //
    //--------------//
    /**
     * Report the last chord of this voice.
     *
     * @return the last chord, which may be a whole/multi
     */
    public AbstractChordInter getLastChord ()
    {
        if (isMeasureRest()) {
            return measureRestChord;
        }

        if ((chords != null) && !chords.isEmpty()) {
            return chords.get(chords.size() - 1);
        }

        return null;
    }

    //------------//
    // getMeasure //
    //------------//
    /**
     * @return the measure
     */
    public Measure getMeasure ()
    {
        return measure;
    }

    //------------//
    // setMeasure //
    //------------//
    /**
     * @param measure the measure to set
     */
    public void setMeasure (Measure measure)
    {
        this.measure = measure;
    }

    //----------//
    // getRests //
    //----------//
    /**
     * Report the sequence of rest-chords in this voice
     *
     * @return only the rests
     */
    public List<AbstractChordInter> getRests ()
    {
        final List<AbstractChordInter> rests = new ArrayList<>();

        if (isMeasureRest()) {
            rests.add(measureRestChord);
        } else {
            for (AbstractChordInter chord : chords) {
                if (chord.isRest()) {
                    rests.add(chord);
                }
            }
        }

        return rests;
    }

    //-------------//
    // getSlotInfo //
    //-------------//
    /**
     * Report the chord information for the specified slot.
     *
     * @param slot the specified slot
     * @return chordInfo the precise chord information, or null
     */
    public SlotVoice getSlotInfo (Slot slot)
    {
        if (slots != null) {
            return slots.get(slot.getId());
        }

        return null;
    }

    //------------------//
    // getStartingStaff //
    //------------------//
    /**
     * Report the staff in which this voice was created.
     *
     * @return the startingStaff
     */
    public Staff getStartingStaff ()
    {
        return startingStaff;
    }

    //----------------//
    // getTermination //
    //----------------//
    /**
     * Report how this voice finishes.
     *
     * @return 0=perfect, -n=too_short, +n=overlast, null=whole_rest/multi_rest
     */
    public Rational getTermination ()
    {
        return termination;
    }

    //------------//
    // getTuplets //
    //------------//
    /**
     * Report the sequence of tuplet signs used in voice
     *
     * @return the sequence found, perhaps empty
     */
    public List<TupletInter> getTuplets ()
    {
        List<TupletInter> found = null;

        for (AbstractChordInter chord : chords) {
            final TupletInter tuplet = chord.getTuplet();

            if ((tuplet != null) && ((found == null) || !found.contains(tuplet))) {
                if (found == null) {
                    found = new ArrayList<>();
                }

                found.add(tuplet);
            }
        }

        if (found == null) {
            return Collections.emptyList();
        }

        return found;
    }

    //----------------//
    // setTermination //
    //----------------//
    private void setTermination (Rational termination)
    {
        this.termination = termination;
    }

    //---------------//
    // getWholeChord //
    //---------------//
    /**
     * Report the whole/multi rest chord which fills the voice, if any.
     *
     * @return the whole chord or null
     */
    public AbstractChordInter getWholeChord ()
    {
        return measureRestChord;
    }

    //---------------//
    // initTransient //
    //---------------//
    /**
     * @param measure the containing measure
     */
    public final void initTransient (Measure measure)
    {
        this.measure = measure;
    }

    //--------//
    // isFree //
    //--------//
    /**
     * Report whether the voice is available at this slot.
     *
     * @param slot the specific slot for which we consider this voice
     * @return true if free
     */
    public boolean isFree (Slot slot)
    {
        return ((getWholeChord() == null) && (slots != null) && (slots.get(slot.getId()) == null));
    }

    //---------------//
    // isMeasureRest //
    //---------------//
    /**
     * Report whether this voice is made of just a measure-long rest.
     *
     * @return true if made of a measure rest
     */
    public boolean isMeasureRest ()
    {
        return measureRestChord != null;
    }

    //-------------//
    // putSlotInfo //
    //-------------//
    /**
     * Insert the chord information for the specified slot.
     *
     * @param slot      the specified slot
     * @param chordInfo the precise chord information, or null to free the slot
     */
    public void putSlotInfo (Slot slot,
                             SlotVoice chordInfo)
    {
        if (isMeasureRest()) {
            logger.error("You cannot insert a slot in a measure-rest voice");

            return;
        }

        if (slots == null) {
            slots = new TreeMap<>();
        }

        slots.put(slot.getId(), chordInfo);

        if (chordInfo.status == Status.BEGIN) {
            ///completeSlotTable();
        }

        logger.debug("putSlotInfo slot#{} {}", slot.getId(), this);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Voice{#").append(id);

        if (excess != null) {
            sb.append(" excess:").append(excess);
        }

        sb.append("}");

        return sb.toString();
    }

    //---------//
    // toStrip //
    //---------//
    /**
     * Return a string which represents the life of this voice within measure stack.
     *
     * @return a strip-like graphic of the voice
     */
    public String toStrip ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("V%2d ", id));

        if (isMeasureRest()) {
            // Measure-long rest
            sb.append("|Ch#").append(String.format("%-5s", measureRestChord.getId()));

            for (int s = 1; s < measure.getStack().getSlots().size(); s++) {
                sb.append("=========");
            }

            sb.append("|M");
        } else {
            Rational voiceDur = Rational.ZERO;

            for (Slot slot : measure.getStack().getSlots()) {
                SlotVoice info = getSlotInfo(slot);

                if (info != null) {
                    // Active chord => busy
                    if (info.status == Status.BEGIN) {
                        sb.append("|Ch#").append(String.format("%-5s", info.chord.getId()));

                        Rational chordDuration = info.chord.getDuration();
                        Rational timeOffset = slot.getTimeOffset();

                        if (timeOffset != null) {
                            Rational chordEnd = timeOffset.plus(chordDuration);

                            if (chordEnd.compareTo(voiceDur) > 0) {
                                voiceDur = chordEnd;
                            }
                        }
                    } else { // CONTINUE
                        sb.append("=========");
                    }
                } else { // No active chord => free
                    sb.append("|........");
                }
            }

            sb.append("|").append(voiceDur);
        }

        MeasureStack stack = getMeasure().getStack();

        if (!stack.isImplicit() && !stack.isFirstHalf()) {
            TimeRational ts = getInferredTimeSignature();

            if (ts != null) {
                sb.append(" (ts:").append(ts).append(")");
            }
        }

        return sb.toString();
    }

    //---------------//
    // insertForward //
    //---------------//
    private void insertForward (Rational duration,
                                Mark.Position position,
                                AbstractChordInter chord)
    {
        //        Point point = new Point(
        //                chord.getHeadLocation().x,
        //                (chord.getHeadLocation().y + chord.getTailLocation().y) / 2);
        //
        //        if (position == Mark.Position.AFTER) {
        //            point.x += 10;
        //        } else if (position == Mark.Position.BEFORE) {
        //            point.x -= 10;
        //        }
        //
        //        Mark mark = new Mark(chord.getSystem(), point, position, Symbols.SYMBOL_MARK, duration);
        //
        //        chord.addMark(mark);
    }

    //-----------//
    // timeSigOf //
    //-----------//
    /**
     * Based on the number of common groups, derive the proper time rational value.
     *
     * @param count  the number of groups
     * @param common the common time duration of each group
     * @return the inferred time rational
     */
    private TimeRational timeSigOf (int count,
                                    Rational common)
    {
        // Determine the time rational value of measure total duration
        TimeRational timeRational = new TimeRational(count * common.num, common.den);

        int gcd = GCD.gcd(count, timeRational.num);

        // Make sure num is a multiple of count
        timeRational = new TimeRational(
                (count / gcd) * timeRational.num,
                (count / gcd) * timeRational.den);

        // No 1 as num
        if (timeRational.num == 1) {
            timeRational = new TimeRational(2 * timeRational.num, 2 * timeRational.den);
        }

        // All 1/2 values resolve as 2/4
        if (timeRational.getValue().equals(Rational.HALF)) {
            timeRational = new TimeRational(2, 4);
        }

        return timeRational;
    }

    //----------//
    // addChord //
    //----------//
    public void addChord (AbstractChordInter chord)
    {
        if (chords == null) {
            chords = new ArrayList<>();
        }

        if (!chords.contains(chord)) {
            chords.add(chord);
        }
    }

    //-----------//
    // getChords //
    //-----------//
    public List<AbstractChordInter> getChords ()
    {
        if (chords != null) {
            return Collections.unmodifiableList(chords);
        }

        return Collections.emptyList();
    }

    //-------------//
    // resetChords //
    //-------------//
    public void resetChords ()
    {
        chords = null;
    }

    //------------------------//
    // createMeasureRestVoice //
    //------------------------//
    /**
     * Factory method to create a voice made of just one measure-long rest.
     *
     * @param measureRestChord the measure-long rest chord
     * @param measure          the containing measure
     * @return the created voice instance
     */
    public static Voice createMeasureRestVoice (RestChordInter measureRestChord,
                                                Measure measure)
    {
        logger.debug("createMeasureRestVoice for {} in {}", measureRestChord, measure);

        final Voice voice = new Voice(measureRestChord, measure);
        voice.measureRestChord = measureRestChord;

        return voice;
    }

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    /**
     * Upgrade from oldWholeRestChord to measureRestChord.
     *
     * @return true if really upgraded
     */
    public boolean upgradeOldStuff ()
    {
        if (oldWholeRestChord != null) {
            measureRestChord = oldWholeRestChord;
            oldWholeRestChord = null;

            return true;
        }

        return false;
    }
}
