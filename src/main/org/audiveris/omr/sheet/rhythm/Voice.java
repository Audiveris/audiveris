//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            V o i c e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
import org.audiveris.omr.sheet.beam.BeamGroup;
import static org.audiveris.omr.sheet.rhythm.Voice.Status.BEGIN;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
    public static enum Status
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** A chord begins at this slot. */
        BEGIN,
        /** A chord is still active at this slot. */
        CONTINUE;
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
     * Whole chord of the voice, if any.
     * If a voice is assigned to a whole/multi rest, then this rest chord is defined as the
     * wholeRestChord of this voice, and the slots table is left empty.
     */
    @XmlIDREF
    @XmlAttribute(name = "whole-rest-chord")
    private RestChordInter wholeRestChord;

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

        if (measure.isDummy()) {
            id = measure.getVoices().size() + 1;
        } else {
            id = measure.getVoiceCount() + 1;
        }

        chord.setVoice(this);

        if (chord.isWholeRest()) {
            wholeRestChord = (RestChordInter) chord;
        }

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
    public void afterReload (Measure measure)
    {
        try {
            this.measure = measure;

            // Set chord voices
            if (isWhole()) {
                wholeRestChord.setVoice(this);
            } else if (slots != null) {
                for (SlotVoice info : slots.values()) {
                    if (info.status == BEGIN) {
                        info.chord.assignVoice(this);

                        for (AbstractBeamInter beam : info.chord.getBeams()) {
                            beam.getGroup().assignVoice(this);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //------------------//
    // createWholeVoice //
    //------------------//
    /**
     * Factory method to create a voice made of just one whole/multi rest.
     *
     * @param wholeChord the whole/multi rest chord
     * @param measure    the containing measure
     * @return the created voice instance
     */
    public static Voice createWholeVoice (RestChordInter wholeChord,
                                          Measure measure)
    {
        logger.debug("createWholeVoice for {} in {}", wholeChord, measure);

        Voice voice = new Voice(wholeChord, measure);
        voice.wholeRestChord = wholeChord;

        return voice;
    }

    //---------------//
    // checkDuration //
    //---------------//
    /**
     * Check the duration of the voice, compared to stack expected duration.
     *
     * @param stack the containing measure stack
     */
    public void checkDuration (MeasureStack stack)
    {
        // Make all forward stuff explicit & visible
        try {
            if (isWhole()) {
                setTermination(null); // we can't tell anything
            } else {
                Rational timeCounter = Rational.ZERO;

                if (slots != null) {
                    for (SlotVoice info : slots.values()) {
                        if (info.status == Status.BEGIN) {
                            AbstractChordInter chord = info.chord;
                            Slot slot = chord.getSlot();

                            // Need a forward before this chord ?
                            if (timeCounter.compareTo(slot.getTimeOffset()) < 0) {
                                insertForward(
                                        slot.getTimeOffset().minus(timeCounter),
                                        Mark.Position.BEFORE,
                                        chord);
                                timeCounter = slot.getTimeOffset();
                            }

                            timeCounter = timeCounter.plus(chord.getDuration());
                        }
                    }
                }

                // Need an ending forward ?
                Rational delta = timeCounter.minus(stack.getExpectedDuration());
                setTermination(delta);

                if (delta.compareTo(Rational.ZERO) < 0) {
                    // Insert a forward mark
                    insertForward(delta.opposite(), Mark.Position.AFTER, getLastChord());
                } else if (delta.compareTo(Rational.ZERO) > 0) {
                    // Flag the voice as too long
                    String prefix = stack.getSystem().getLogPrefix();
                    logger.info("{}{} Voice #{} too long {}", prefix, stack, getId(), delta);
                    excess = delta; // For voice

                    if ((stack.getExcess() == null) || (delta.compareTo(stack.getExcess()) > 0)) {
                        stack.setExcess(delta); // For stack
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Error checking stack duration " + ex, ex);
        }
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
    public Rational getDuration ()
    {
        if (wholeRestChord != null) {
            return null;
        }

        Rational voiceDur = Rational.ZERO;

        for (Slot slot : measure.getStack().getSlots()) {
            SlotVoice info = getSlotInfo(slot);

            if ((info != null) && (info.status == Status.BEGIN)) {
                Rational chordEnd = slot.getTimeOffset().plus(info.chord.getDuration());

                if (chordEnd.compareTo(voiceDur) > 0) {
                    voiceDur = chordEnd;
                }
            }
        }

        return voiceDur;
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
        if (isWhole()) {
            return wholeRestChord;
        } else {
            if (slots != null) {
                for (SlotVoice info : slots.values()) {
                    return info.chord;
                }
            }

            return null;
        }
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
            // Sequence of group (beamed or isolated chords) durations
            List<Rational> durations = new ArrayList<Rational>();

            // Voice time offset
            Rational timeOffset = null;

            // Start time of last note in group, if any
            Rational groupLastTime = null;

            if (slots != null) {
                for (Map.Entry<Integer, SlotVoice> entry : slots.entrySet()) {
                    SlotVoice info = entry.getValue();

                    if (info.status == Voice.Status.BEGIN) {
                        AbstractChordInter chord = info.chord;

                        // Skip the remaining parts of beam group, including embraced rests
                        if ((groupLastTime != null)
                            && (chord.getTimeOffset().compareTo(groupLastTime) <= 0)) {
                            continue;
                        }

                        BeamGroup group = chord.getBeamGroup();

                        if (group == null) {
                            // Isolated chord
                            durations.add(chord.getDuration());
                        } else {
                            // Starting a new group
                            durations.add(group.getDuration());
                            groupLastTime = group.getLastChord().getTimeOffset();
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
        if (isWhole()) {
            return wholeRestChord;
        } else {
            AbstractChordInter lastChord = null;

            if (slots != null) {
                for (SlotVoice info : slots.values()) {
                    lastChord = info.chord;
                }
            }

            return lastChord;
        }
    }

    /**
     * @return the measure
     */
    public Measure getMeasure ()
    {
        return measure;
    }

    //
    //    //------------------//
    //    // getPreviousChord //
    //    //------------------//
    //    /**
    //     * Starting from a provided chord in this voice, report the previous chord, if any,
    //     * within that voice.
    //     *
    //     * @param chord the provided chord
    //     * @return the chord right before, or null
    //     */
    //    public AbstractChordInter getPreviousChord (AbstractChordInter chord)
    //    {
    //        AbstractChordInter prevChord = null;
    //
    //        for (Map.Entry<Slot, SlotVoice> entry : slotTable.entrySet()) {
    //            SlotVoice info = entry.getValue();
    //
    //            if (info.chord == chord) {
    //                break;
    //            }
    //
    //            prevChord = info.chord;
    //        }
    //
    //        return prevChord;
    //    }
    //
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
        List<AbstractChordInter> rests = new ArrayList<AbstractChordInter>();

        if (isWhole()) {
            rests.add(wholeRestChord);
        } else if (slots != null) {
            for (SlotVoice info : slots.values()) {
                if (info.status == Status.BEGIN) {
                    AbstractChordInter chord = info.chord;

                    if (chord.isRest()) {
                        rests.add(chord);
                    }
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
        return wholeRestChord;
    }

    //---------------//
    // initTransient //
    //---------------//
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

    //------------//
    // isOnlyRest //
    //------------//
    /**
     * report whether the voice is made of rests only.
     *
     * @return true if rests only
     */
    public boolean isOnlyRest ()
    {
        for (Slot slot : measure.getStack().getSlots()) {
            SlotVoice info = getSlotInfo(slot);

            if ((info != null) && (info.status == Status.BEGIN)) {
                if (info.chord.getNotes().get(0) instanceof HeadInter) {
                    return false;
                }
            }
        }

        return true;
    }

    //---------//
    // isWhole //
    //---------//
    /**
     * Report whether this voice is made of a whole/multi rest.
     *
     * @return true if made of a whole/multi rest
     */
    public boolean isWhole ()
    {
        return wholeRestChord != null;
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

    //-------------//
    // setSlotInfo //
    //-------------//
    /**
     * Define the chord information for the specified slot.
     *
     * @param slot      the specified slot
     * @param chordInfo the precise chord information, or null to free the slot
     */
    public void setSlotInfo (Slot slot,
                             SlotVoice chordInfo)
    {
        if (isWhole()) {
            logger.error("You cannot insert a slot in a whole-only voice");

            return;
        }

        if (slots == null) {
            slots = new TreeMap<Integer, SlotVoice>();
        }

        slots.put(slot.getId(), chordInfo);
        updateSlotTable();
        logger.debug("setSlotInfo slot#{} {}", slot.getId(), this);
    }

    //------------//
    // startChord //
    //------------//
    /**
     * Insert provided chord to begin at specified slot.
     *
     * @param slot  specified slot
     * @param chord the incoming chord
     */
    public void startChord (Slot slot,
                            AbstractChordInter chord)
    {
        setSlotInfo(slot, new SlotVoice(chord, Voice.Status.BEGIN));
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{Voice#").append(id);

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

        sb.append("V").append(id).append(" ");

        // Whole/Multi
        if (wholeRestChord != null) {
            sb.append("|Ch#").append(String.format("%-4s", wholeRestChord.getId()));

            for (int s = 1; s < measure.getStack().getSlots().size(); s++) {
                sb.append("========");
            }

            sb.append("|W");
        } else {
            Rational voiceDur = Rational.ZERO;

            for (Slot slot : measure.getStack().getSlots()) {
                SlotVoice info = getSlotInfo(slot);

                if (info != null) {
                    // Active chord => busy
                    if (info.status == Status.BEGIN) {
                        sb.append("|Ch#").append(String.format("%-4s", info.chord.getId()));

                        Rational chordEnd = slot.getTimeOffset().plus(info.chord.getDuration());

                        if (chordEnd.compareTo(voiceDur) > 0) {
                            voiceDur = chordEnd;
                        }
                    } else { // CONTINUE
                        sb.append("========");
                    }
                } else { // No active chord => free
                    sb.append("|.......");
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

    //-----------------//
    // updateSlotTable //
    //-----------------//
    /**
     * Update the slotTable.
     */
    public void updateSlotTable ()
    {
        AbstractChordInter lastChord = null;

        for (Slot slot : measure.getStack().getSlots()) {
            if (slot.getTimeOffset() != null) {
                SlotVoice info = getSlotInfo(slot);

                if (info == null) {
                    if ((lastChord != null)
                        && (lastChord.getEndTime().compareTo(slot.getTimeOffset()) > 0)) {
                        setSlotInfo(slot, new SlotVoice(lastChord, Status.CONTINUE));
                    }
                } else {
                    lastChord = info.chord;
                }
            }
        }
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

    //----------------//
    // setTermination //
    //----------------//
    private void setTermination (Rational termination)
    {
        this.termination = termination;
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // SlotVoice //
    //-----------//
    /**
     * Define which chord, if any, represents this voice in a given slot.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "slot-voice")
    public static class SlotVoice
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Related chord. */
        @XmlIDREF
        @XmlAttribute
        public final AbstractChordInter chord;

        /** Current status. */
        @XmlAttribute
        public final Status status;

        //~ Constructors ---------------------------------------------------------------------------
        public SlotVoice (AbstractChordInter chord,
                          Status status)
        {
            this.chord = chord;
            this.status = status;
        }

        // For JAXB.
        private SlotVoice ()
        {
            this.chord = null;
            this.status = null;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{Info");
            sb.append(" Ch#").append(chord.getId());
            sb.append(" ").append(status);
            sb.append("}");

            return sb.toString();
        }
    }
}
