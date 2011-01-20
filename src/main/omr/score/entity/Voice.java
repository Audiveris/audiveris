//----------------------------------------------------------------------------//
//                                                                            //
//                                 V o i c e                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.log.Logger;

import omr.math.GCD;
import omr.math.Rational;

import omr.score.common.PixelPoint;
import omr.score.ui.MusicFont;

import omr.util.Navigable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class <code>Voice</code> gathers all informations related to a voice within
 * a measure.
 * <p>If a voice is assigned to a whole/multi rest, then this rest chord is
 * defined as the wholeChord of this voice, and the slot table is left empty.
 * Otherwise, the status is defined by the slot table (empty => free, or filled
 * either by the beginning of a chord, or by a chord whose sound is still active
 * during the specified slot.
 *
 * @author Hervé Bitteur
 */
public class Voice
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Voice.class);

    //~ Enumerations -----------------------------------------------------------

    public static enum Status {
        //~ Enumeration constant initializers ----------------------------------


        /**  A chord begins at this slot */
        BEGIN,
        /** A chord is still active at this slot */
        CONTINUE;
    }

    //~ Instance fields --------------------------------------------------------

    /** Containing measure */
    @Navigable(false)
    private final Measure measure;

    /** The voice id */
    private final int id;

    /**
     * Map (SlotId -> ChordInfo) to store chord information for each slot.
     * If the voice/slot combination is empty, the voice is free for this slot
     * Otherwise, the active chord is referenced with a status flag to make a
     * difference between a slot where the chord starts, and the potential
     * following slots for which the chord is still active.
     */
    private final SortedMap<Integer, ChordInfo> slotTable = new TreeMap<Integer, ChordInfo>();

    /** Abnormal termination of the voice, if any */
    private Rational termination;

    /** Whole chord of the voice, if any */
    private Chord wholeChord;

    /** Inferred time signature based on this voice content */
    private TimeRational inferredTimeSig;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Voice //
    //-------//
    /**
     * Creates a new Voice object.
     *
     * @param chord the initial chord for this voice
     */
    public Voice (Chord chord)
    {
        measure = chord.getMeasure();
        id = measure.getVoicesNumber() + 1;
        measure.addVoice(this);
        chord.setVoice(this);

        if (logger.isFineEnabled()) {
            logger.fine("Created voice#" + id);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // isFree //
    //--------//
    /**
     * Report whether the voice is available at this slot
     *
     * @param slot the specific slot for which we consider this voice
     * @return true if free
     */
    public boolean isFree (Slot slot)
    {
        return ((getWholeChord() == null) &&
               (slotTable.get(slot.getId()) == null));
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the voice id, starting from 1
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
     * Report the time signature value that can be inferred from the content of
     * this voice
     *
     * @return the "intrinsic" time signature rational value for this voice,
     * or null
     */
    public TimeRational getInferredTimeSignature ()
    {
        if (inferredTimeSig == null) {
            // TODO: update for the use of tuplets

            // Sequence of group (beamed or isolated chords) durations
            List<Rational> durations = new ArrayList<Rational>();

            // Current beam group, if any
            BeamGroup currentGroup = null;

            for (Map.Entry<Integer, ChordInfo> entry : slotTable.entrySet()) {
                ChordInfo info = entry.getValue();

                if (info.getStatus() == Voice.Status.BEGIN) {
                    Chord     chord = info.getChord();
                    BeamGroup group = chord.getBeamGroup();

                    if (group == null) {
                        // Isolated chord
                        durations.add(chord.getDuration());
                    } else if (group != currentGroup) {
                        // Starting a new group
                        durations.add(group.getDuration());
                    }

                    currentGroup = group;
                }
            }

            // Debug
            if (logger.isFineEnabled()) {
                StringBuilder sb = new StringBuilder("[");
                boolean       started = false;
                Rational      total = null;

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

                logger.fine(this + ": " + sb);
            }

            // Do we have a regular pattern?
            int      count = 0;
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
     * Report the last chord of this voice
     *
     * @return the last chord, which may be a whole/multi
     */
    public Chord getLastChord ()
    {
        if (isWhole()) {
            return wholeChord;
        } else {
            for (int k = slotTable.lastKey(); k > 0; k--) {
                ChordInfo info = slotTable.get(k);

                if (info != null) {
                    return info.getChord();
                }
            }

            return null;
        }
    }

    //------------------//
    // getPreviousChord //
    //------------------//
    /**
     * Starting from a provided chord in this voice, report the previous chord
     * if any within that voice
     *
     * @param chord the provided chord
     * @return the chord right before, or null
     */
    public Chord getPreviousChord (Chord chord)
    {
        Chord prevChord = null;

        for (Map.Entry<Integer, ChordInfo> entry : slotTable.entrySet()) {
            ChordInfo info = entry.getValue();

            if (info.getChord() == chord) {
                break;
            }

            prevChord = info.getChord();
        }

        return prevChord;
    }

    //-------------//
    // setSlotInfo //
    //-------------//
    /**
     * Define the chord information for the specified slot
     *
     * @param slot the specified slot
     * @param chordInfo the precise chord information, or null to free the slot
     */
    public void setSlotInfo (Slot      slot,
                             ChordInfo chordInfo)
    {
        if (isWhole()) {
            logger.severe("You cannot insert a slot in a whole-only voice");

            return;
        }

        slotTable.put(slot.getId(), chordInfo);
        updateSlotTable();

        if (logger.isFineEnabled()) {
            logger.fine("setSlotInfo slot#" + slot.getId() + " " + this);
        }
    }

    //-------------//
    // getSlotInfo //
    //-------------//
    /**
     * Report the chord information for the specified slot
     *
     * @param slot the specified slot
     * @return chordInfo the precise chord information, or null
     */
    public ChordInfo getSlotInfo (Slot slot)
    {
        return slotTable.get(slot.getId());
    }

    //----------------//
    // getTermination //
    //----------------//
    /**
     * Report how this voice finishes
     *
     * @return 0=perfect, -n=too_short, +n=overlast, null=whole_rest/multi_rest
     */
    public Rational getTermination ()
    {
        return termination;
    }

    //---------//
    // isWhole //
    //---------//
    /**
     * Report whether this voice is made of a whole/multi rest
     *
     * @return true if made of a whole/multi rest
     */
    public boolean isWhole ()
    {
        return wholeChord != null;
    }

    //---------------//
    // getWholeChord //
    //---------------//
    /**
     * Report the whole/multi rest chord which fills the voice, if any
     *
     * @return the whole chord or null
     */
    public Chord getWholeChord ()
    {
        return wholeChord;
    }

    //------------------//
    // createWholeVoice //
    //------------------//
    /**
     * Factory method to create a voice made of just one whole/multi rest
     *
     * @param wholeChord the whole/multi rest chord
     * @return the created voice instance
     */
    public static Voice createWholeVoice (Chord wholeChord)
    {
        Voice voice = new Voice(wholeChord);
        voice.wholeChord = wholeChord;

        return voice;
    }

    //---------------//
    // checkDuration //
    //---------------//
    /**
     * Check the duration of the voice, compared to measure expected duration
     */
    public void checkDuration ()
    {
        // Make all forward stuff explicit & visible
        try {
            if (isWhole()) {
                setTermination(null); // we can't tell anything
            } else {
                Rational timeCounter = Rational.ZERO;

                for (ChordInfo info : slotTable.values()) {
                    if (info.getStatus() == Status.BEGIN) {
                        Chord chord = info.getChord();
                        Slot  slot = chord.getSlot();

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
                Rational delta = timeCounter.minus(
                    measure.getExpectedDuration());
                setTermination(delta);

                if (delta.compareTo(Rational.ZERO) < 0) {
                    // Insert a forward mark
                    insertForward(
                        delta.opposite(),
                        Mark.Position.AFTER,
                        getLastChord());
                } else if (delta.compareTo(Rational.ZERO) > 0) {
                    // Flag the measure as too long
                    measure.addError(
                        "Voice #" + getId() + " too long for " + delta);
                    measure.setExcess(delta);
                }
            }
        } catch (Exception ex) {
            // User has been informed
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{Voice#")
          .append(id)
          .append("}");

        return sb.toString();
    }

    //---------//
    // toStrip //
    //---------//
    /**
     * Return a string which represents the life of this voice
     *
     * @return a strip-like graphic of the voice
     */
    public String toStrip ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{V")
          .append(id)
          .append(" ");

        // Whole/Multi
        if (wholeChord != null) {
            sb.append("|Ch#")
              .append(String.format("%02d", wholeChord.getId()));

            for (int s = 1; s < measure.getSlots()
                                       .size(); s++) {
                sb.append("======");
            }

            sb.append("|W");
        } else {
            for (Slot slot : measure.getSlots()) {
                ChordInfo info = getSlotInfo(slot);

                if (info != null) { // Active chord => busy

                    if (info.getStatus() == Status.BEGIN) {
                        sb.append("|Ch#")
                          .append(
                            String.format("%02d", info.getChord().getId()));
                    } else { // CONTINUE
                        sb.append("======");
                    }
                } else { // No active chord => free
                    sb.append("|.....");
                }
            }

            sb.append("|");
        }

        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // updateSlotTable //
    //-----------------//
    /**
     * Update the slotTable
     */
    public void updateSlotTable ()
    {
        Chord lastChord = null;

        for (Slot slot : measure.getSlots()) {
            if (slot.getStartTime() != null) {
                ChordInfo info = getSlotInfo(slot);

                if (info == null) {
                    if ((lastChord != null) &&
                        (lastChord.getEndTime()
                                  .compareTo(slot.getStartTime()) > 0)) {
                        setSlotInfo(
                            slot,
                            new ChordInfo(lastChord, Status.CONTINUE));
                    }
                } else {
                    lastChord = info.chord;
                }
            }
        }
    }

    //----------------//
    // setTermination //
    //----------------//
    private void setTermination (Rational termination)
    {
        this.termination = termination;
    }

    //---------------//
    // insertForward //
    //---------------//
    private void insertForward (Rational      duration,
                                Mark.Position position,
                                Chord         chord)
    {
        PixelPoint point = new PixelPoint(
            chord.getHeadLocation().x,
            (chord.getHeadLocation().y + chord.getTailLocation().y) / 2);

        if (position == Mark.Position.AFTER) {
            point.x += 10;
        } else if (position == Mark.Position.BEFORE) {
            point.x -= 10;
        }

        Mark mark = new Mark(
            chord.getSystem(),
            point,
            position,
            MusicFont.MARK_DESC,
            duration);

        chord.addMark(mark);
    }

    //-----------//
    // timeSigOf //
    //-----------//
    /**
     * Based on the number of common groups, derive the proper time rational
     * @param count the number of groups
     * @param common the common time duration of each group
     * @return the inferred time rational
     */
    private TimeRational timeSigOf (int      count,
                                    Rational common)
    {
        // Determine the time rational value of measure total duration
        TimeRational timeRational = new TimeRational(
            count * common.num,
            common.den);

        int          gcd = GCD.gcd(count, timeRational.num);

        // Make sure num is a multiple of count
        timeRational = new TimeRational(
            (count / gcd) * timeRational.num,
            (count / gcd) * timeRational.den);

        // No 1 as num
        if (timeRational.num == 1) {
            timeRational = new TimeRational(
                2 * timeRational.num,
                2 * timeRational.den);
        }

        return timeRational;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // ChordInfo //
    //-----------//
    public static class ChordInfo
    {
        //~ Instance fields ----------------------------------------------------

        /** Related chord */
        private final Chord chord;

        /** Current status */
        private final Status status;

        //~ Constructors -------------------------------------------------------

        public ChordInfo (Chord  chord,
                          Status status)
        {
            this.chord = chord;
            this.status = status;
        }

        //~ Methods ------------------------------------------------------------

        public Chord getChord ()
        {
            return chord;
        }

        public Status getStatus ()
        {
            return status;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{Info");
            sb.append(" Ch#")
              .append(chord.getId());
            sb.append(" ")
              .append(status);
            sb.append("}");

            return sb.toString();
        }
    }
}
