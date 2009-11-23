//----------------------------------------------------------------------------//
//                                                                            //
//                                 V o i c e                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Shape;

import omr.log.Logger;

import omr.score.common.SystemPoint;

import omr.util.Navigable;

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
 * @author Herv&eacute Bitteur
 * @version $Id$
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
     * Related chord information for each slot.
     * If the voice/slot combination is empty, the voice is free for this slot
     * Otherwise, the active chord is referenced with a status flag to make a
     * difference between a slot where the chord starts, and the potential
     * following slots for which the chord is still active.
     */
    private final SortedMap<Integer, ChordInfo> slotTable = new TreeMap<Integer, ChordInfo>();

    /** Final duration of the voice */
    private Integer finalDuration;

    /** Whole chord of the voice, if any */
    private Chord wholeChord;

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
        this.measure = chord.getMeasure();
        this.id = measure.getVoicesNumber() + 1;
        measure.addVoice(this);
        chord.setVoice(this);

        if (logger.isFineEnabled()) {
            logger.fine("Created voice#" + id);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // getFinalDuration //
    //------------------//
    /**
     * Report how this voice finishes
     *
     * @return 0=perfect, -n=too_short, +n=overlast, null=whole_rest/multi_rest
     */
    public Integer getFinalDuration ()
    {
        return finalDuration;
    }

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
     * Report the whole/multi rest chord which fills the voice , if any
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
     * Check the duration of the voice, compared to measure theoretical duration
     */
    public void checkDuration ()
    {
        // Make all forward stuff explicit & visible
        try {
            if (isWhole()) {
                setFinalDuration(null); // we can't tell anything
            } else {
                int timeCounter = 0;

                for (ChordInfo info : slotTable.values()) {
                    if (info.getStatus() == Status.BEGIN) {
                        Chord chord = info.getChord();
                        Slot  slot = chord.getSlot();

                        // Need a forward before this chord ?
                        if (timeCounter < slot.getStartTime()) {
                            insertForward(
                                slot.getStartTime() - timeCounter,
                                Mark.Position.BEFORE,
                                chord);
                            timeCounter = slot.getStartTime();
                        }

                        timeCounter += chord.getDuration();
                    }
                }

                // Need an ending forward ?
                int delta = timeCounter - measure.getExpectedDuration();
                setFinalDuration(delta);

                if (delta < 0) {
                    // Insert a forward mark
                    insertForward(-delta, Mark.Position.AFTER, getLastChord());
                } else if (delta > 0) {
                    // Flag the measure as too long
                    measure.addError(
                        "Voice #" + getId() + " too long for " +
                        Note.quarterValueOf(delta));
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
                        (lastChord.getEndTime() > slot.getStartTime())) {
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

    //------------------//
    // setFinalDuration //
    //------------------//
    private void setFinalDuration (Integer finalDuration)
    {
        this.finalDuration = finalDuration;
    }

    //---------------//
    // insertForward //
    //---------------//
    private void insertForward (int           duration,
                                Mark.Position position,
                                Chord         chord)
    {
        SystemPoint point = new SystemPoint(
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
            Shape.FORWARD,
            duration);

        chord.addMark(mark);
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
