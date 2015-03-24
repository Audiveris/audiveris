//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            V o i c e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.math.GCD;
import omr.math.Rational;

import omr.score.entity.Mark;
import omr.score.entity.TimeRational;

import omr.sheet.Part;
import omr.sheet.beam.BeamGroup;

import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.ChordInter;
import omr.sig.inter.Inter;

import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class {@code Voice} gathers all informations related to a voice within a measure.
 *
 * @author Hervé Bitteur
 */
public class Voice
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Voice.class);

    /** To sort voices by their ID. */
    public static final Comparator<Voice> byId = new Comparator<Voice>()
    {
        @Override
        public int compare (Voice v1,
                            Voice v2)
        {
            return Integer.compare(v1.id, v2.id);
        }
    };

    /** To sort voices by vertical position within their containing measure stack. */
    public static final Comparator<Voice> byOrdinate = new Comparator<Voice>()
    {
        @Override
        public int compare (Voice v1,
                            Voice v2)
        {
            if (v1.getMeasure().getStack() != v2.getMeasure().getStack()) {
                throw new IllegalArgumentException("Comparing voices in different stacks");
            }

            // Check if they are located in different parts
            Part p1 = v1.getMeasure().getPart();
            Part p2 = v2.getMeasure().getPart();

            if (p1 != p2) {
                return Part.byId.compare(p1, p2);
            }

            // Look for the first time slot with incoming chords for both voices.
            // If such slot exists, compare the two chords ordinates in that slot.
            for (Slot slot : v1.getMeasure().getStack().getSlots()) {
                SlotVoice vc1 = v1.getSlotInfo(slot);

                if ((vc1 == null) || (vc1.status != Status.BEGIN)) {
                    continue;
                }

                ChordInter c1 = vc1.chord;

                SlotVoice vc2 = v2.getSlotInfo(slot);

                if ((vc2 == null) || (vc2.status != Status.BEGIN)) {
                    continue;
                }

                ChordInter c2 = vc2.chord;

                return Inter.byOrdinate.compare(c1, c2);
            }

            // No common slot found, use ordinate of first chord for each voice
            ChordInter c1 = v1.getFirstChord();
            ChordInter c2 = v2.getFirstChord();

            return Inter.byOrdinate.compare(c1, c2);
        }
    };

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
    /** Containing measure */
    @Navigable(false)
    private final Measure measure;

    /** The voice id. */
    private int id;

    /**
     * Map (Slot -> SlotVoice) to store chord information for each slot.
     * If a voice is assigned to a whole/multi rest, then this rest chord is defined as the
     * wholeChord of this voice, and the whole slot table is left empty.
     * If the voice/slot combination is empty, the voice is free for this slot.
     * Otherwise, the active chord is referenced with a status flag to make a difference between a
     * slot where the chord starts, and the potential following slots for which the chord is still
     * active.
     */
    private final SortedMap<Slot, SlotVoice> slotTable = new TreeMap<Slot, SlotVoice>();

    /**
     * How the voice finishes (value = voiceEndTime - expectedMeasureEndTime).
     * - null: We can't tell
     * - negative: Voice is too short WRT measure expected duration
     * - zero: Voice equals the measure expected duration
     * - positive: Voice is too long WRT measure expected duration
     */
    private Rational termination;

    /** Whole chord of the voice, if any. */
    private ChordInter wholeChord;

    /** Inferred time signature based on this voice content. */
    private TimeRational inferredTimeSig;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Voice object.
     *
     * @param chord   the initial chord for this voice
     * @param measure the containing measure
     */
    public Voice (ChordInter chord,
                  Measure measure)
    {
        this.measure = measure;

        MeasureStack stack = measure.getStack();
        id = stack.getVoicesNumber() + 1;
        chord.setVoice(this);

        if (chord.isWholeRest()) {
            wholeChord = chord;
        }

        logger.debug("Created voice#{}", id);
    }

    //~ Methods ------------------------------------------------------------------------------------
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

                for (SlotVoice info : slotTable.values()) {
                    if (info.status == Status.BEGIN) {
                        ChordInter chord = info.chord;
                        Slot slot = chord.getSlot();

                        // Need a forward before this chord ?
                        if (timeCounter.compareTo(slot.getStartTime()) < 0) {
                            insertForward(
                                    slot.getStartTime().minus(timeCounter),
                                    Mark.Position.BEFORE,
                                    chord);
                            timeCounter = slot.getStartTime();
                        }

                        timeCounter = timeCounter.plus(chord.getDuration());
                    }
                }

                // Need an ending forward ?
                Rational delta = timeCounter.minus(stack.getExpectedDuration());
                setTermination(delta);

                if (delta.compareTo(Rational.ZERO) < 0) {
                    // Insert a forward mark
                    insertForward(delta.opposite(), Mark.Position.AFTER, getLastChord());
                } else if (delta.compareTo(Rational.ZERO) > 0) {
                    // Flag the measure as too long
                    ///measure.addError("Voice #" + getId() + " too long for " + delta);
                    logger.warn("{} Voice #{} too long {}", stack, getId(), delta);
                    stack.setExcess(delta);
                }
            }
        } catch (Exception ex) {
            // User has been informed
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
    public static Voice createWholeVoice (ChordInter wholeChord,
                                          Measure measure)
    {
        Voice voice = new Voice(wholeChord, measure);
        voice.wholeChord = wholeChord;

        return voice;
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
    public ChordInter getChordBefore (Slot slot)
    {
        ChordInter prevChord = null;

        for (Map.Entry<Slot, SlotVoice> entry : slotTable.entrySet()) {
            if (slot == entry.getKey()) {
                break;
            }

            SlotVoice info = entry.getValue();

            if (info != null) {
                prevChord = info.chord;
            }
        }

        return prevChord;
    }

    //-------------//
    // getDuration //
    //-------------//
    public Rational getDuration ()
    {
        if (wholeChord != null) {
            return null;
        }

        Rational voiceDur = Rational.ZERO;

        for (Slot slot : measure.getStack().getSlots()) {
            SlotVoice info = getSlotInfo(slot);

            if ((info != null) && (info.status == Status.BEGIN)) {
                Rational chordEnd = slot.getStartTime().plus(info.chord.getDuration());

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
    public ChordInter getFirstChord ()
    {
        if (isWhole()) {
            return wholeChord;
        } else {
            for (SlotVoice info : slotTable.values()) {
                return info.chord;
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

            // Voice starting time
            Rational startTime = null;

            // Start time of last note in group, if any
            Rational groupLastTime = null;

            for (Map.Entry<Slot, SlotVoice> entry : slotTable.entrySet()) {
                SlotVoice info = entry.getValue();

                if (info.status == Voice.Status.BEGIN) {
                    ChordInter chord = info.chord;

                    // Skip the remaining parts of beam group, including embraced rests
                    if ((groupLastTime != null)
                        && (chord.getStartTime().compareTo(groupLastTime) <= 0)) {
                        continue;
                    }

                    BeamGroup group = chord.getBeamGroup();

                    if (group == null) {
                        // Isolated chord
                        durations.add(chord.getDuration());
                    } else {
                        // Starting a new group
                        durations.add(group.getDuration());
                        groupLastTime = group.getLastChord().getStartTime();
                    }

                    if (startTime == null) {
                        startTime = entry.getKey().getStartTime();
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
            if ((startTime == null) || !startTime.equals(Rational.ZERO)) {
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
    public ChordInter getLastChord ()
    {
        if (isWhole()) {
            return wholeChord;
        } else {
            ChordInter lastChord = null;

            for (SlotVoice info : slotTable.values()) {
                lastChord = info.chord;
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
    //    public ChordInter getPreviousChord (ChordInter chord)
    //    {
    //        ChordInter prevChord = null;
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
    public List<ChordInter> getRests ()
    {
        List<ChordInter> rests = new ArrayList<ChordInter>();

        if (isWhole()) {
            rests.add(wholeChord);
        } else {
            for (SlotVoice info : slotTable.values()) {
                if (info.status == Status.BEGIN) {
                    ChordInter chord = info.chord;

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
        return slotTable.get(slot);
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
    public ChordInter getWholeChord ()
    {
        return wholeChord;
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
        return ((getWholeChord() == null) && (slotTable.get(slot) == null));
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
                if (info.chord.getNotes().get(0) instanceof AbstractHeadInter) {
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
        return wholeChord != null;
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

        slotTable.put(slot, chordInfo);
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
                            ChordInter chord)
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

        sb.append("{Voice#").append(id).append("}");

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
        if (wholeChord != null) {
            sb.append("|Ch#").append(String.format("%-4d", wholeChord.getId()));

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
                        sb.append("|Ch#").append(String.format("%-4d", info.chord.getId()));

                        Rational chordEnd = slot.getStartTime().plus(info.chord.getDuration());

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
        ChordInter lastChord = null;

        for (Slot slot : measure.getStack().getSlots()) {
            if (slot.getStartTime() != null) {
                SlotVoice info = getSlotInfo(slot);

                if (info == null) {
                    if ((lastChord != null)
                        && (lastChord.getEndTime().compareTo(slot.getStartTime()) > 0)) {
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
                                ChordInter chord)
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
     * Define which chord represents this voice in a given slot. If any.
     */
    public static class SlotVoice
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Related chord. */
        public final ChordInter chord;

        /** Current status. */
        public final Status status;

        //~ Constructors ---------------------------------------------------------------------------
        public SlotVoice (ChordInter chord,
                          Status status)
        {
            this.chord = chord;
            this.status = status;
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
