//----------------------------------------------------------------------------//
//                                                                            //
//                                  S l o t                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.math.InjectionSolver;
import omr.math.Population;
import omr.math.Rational;

import omr.util.Navigable;
import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class {@code Slot} represents a roughly defined time slot within a
 * measure, to gather all chords that start at the same time.
 *
 * <p>On the diagram shown, slots are indicated by vertical blue lines.</p>
 *
 * <p>The slot embraces all the staves of its part measure. Perhaps we should
 * consider merging slots between parts as well?
 *
 * <div style="float: right;">
 * <img src="doc-files/Slot.png" alt="diagram">
 * </div>
 *
 * @author Hervé Bitteur
 */
public class Slot
        implements Comparable<Slot>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Slot.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The containing measure. */
    @Navigable(false)
    private Measure measure;

    /** Id unique within the containing measure. */
    private final int id;

    /** Reference point of the slot. */
    private Point refPoint;

    /** Chords incoming into this slot, sorted by staff then ordinate. */
    private List<Chord> incomings = new ArrayList<>();

    /** Time offset since measure start. */
    private Rational startTime;

    //~ Constructors -----------------------------------------------------------
    //
    //------//
    // Slot //
    //------//
    /**
     * Creates a new Slot object.
     *
     * @param measure the containing measure
     */
    public Slot (Measure measure)
    {
        this.measure = measure;

        id = 1 + measure.getSlots().size();
    }

    //~ Methods ----------------------------------------------------------------
    //
    //------------//
    // getMeasure //
    //------------//
    /**
     * Report the measure that contains this slot
     *
     * @return the containing measure
     */
    public Measure getMeasure ()
    {
        return measure;
    }

    //-----------------//
    // dumpSystemSlots //
    //-----------------//
    public static void dumpSystemSlots (ScoreSystem system)
    {
        // Dump all measure slots
        logger.debug(system.toString());

        for (TreeNode node : system.getParts()) {
            SystemPart part = (SystemPart) node;

            logger.debug(part.toString());

            for (TreeNode mn : part.getMeasures()) {
                Measure measure = (Measure) mn;

                logger.debug(measure.toString());

                for (Slot slot : measure.getSlots()) {
                    logger.debug(slot.toString());
                }
            }
        }
    }

    //-----------//
    // setChords //
    //-----------//
    public void setChords (Collection<Chord> chords)
    {
        this.incomings.addAll(chords);

        for (Chord chord : chords) {
            chord.setSlot(this);
        }

        // Compute slot refPoint as average of chords centers
        Population xPop = new Population();
        Population yPop = new Population();

        for (Chord chord : chords) {
            Point center = chord.getCenter();
            xPop.includeValue(center.x);
            yPop.includeValue(center.y);
        }

        refPoint = new Point(
                (int) Math.rint(xPop.getMeanValue()),
                (int) Math.rint(yPop.getMeanValue()));
    }

    //-------------//
    // buildVoices //
    //-------------//
    /**
     * Compute the various voices in this slot.
     *
     * @param endingChords the chords that end right at this slot, with their
     *                     voice not available because their group is continuing.
     */
    public void buildVoices (List<Chord> endingChords)
    {
        logger.debug("endingChords={}", endingChords);
        logger.debug("incomings={}", incomings);

        // Sort chords vertically
        Collections.sort(incomings, Chord.byOrdinate);

        // Some chords already have the voice assigned
        List<Chord> endings = new ArrayList<>(endingChords);
        List<Chord> rookies = new ArrayList<>();

        for (Chord ch : incomings) {
            if (ch.getVoice() != null) {
                // Needed to populate the voice slotTable
                ch.setVoice(ch.getVoice());

                // Remove the ending chord with the same voice
                for (Iterator<Chord> it = endings.iterator(); it.hasNext();) {
                    Chord c = it.next();
                    if (c.getVoice() == ch.getVoice()) {
                        it.remove();
                        break;
                    }
                }

            } else {
                rookies.add(ch);
            }
        }

        // Nothing left to assign?
        if (rookies.isEmpty()) {
            return;
        }

        // Try to map some ending voices to some rookies
        if (!endings.isEmpty()) {
            InjectionSolver solver = new InjectionSolver(
                    rookies.size(),
                    endings.size() + rookies.size(),
                    new MyDistance(rookies, endings));
            int[] links = solver.solve();

            for (int i = 0; i < links.length; i++) {
                int index = links[i];

                // Map new chord to an ending chord?
                if (index < endings.size()) {
                    Voice voice = endings.get(index).getVoice();
                    logger.debug("Slot#{} Reusing voice#{}",
                            getId(), voice.getId());

                    Chord ch = rookies.get(i);

                    try {
                        ch.setVoice(voice);
                    } catch (Exception ex) {
                        ch.addError("Failed to set voice of chord");

                        return;
                    }
                }
            }
        }

        // Assign remaining non-mapped chords, using first voice available
        assignVoices();
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Compare this slot to another, as needed to insert slots in an
     * ordered collection.
     *
     * @param other another slot
     * @return -1, 0 or +1, according to their relative abscissae
     */
    @Override
    public int compareTo (Slot other)
    {
        return Integer.compare(getX(), other.getX());
    }

    //------//
    // getX //
    //------//
    /**
     * Report the abscissa of this slot.
     *
     * @return the slot abscissa (page-based, not measure-based)
     */
    public int getX ()
    {
        return refPoint.x;
    }

    //-------------------//
    // getChordJustAbove //
    //-------------------//
    /**
     * Report the chord which is in staff just above the given point
     * in this slot.
     *
     * @param point the given point
     * @return the chord above, or null
     */
    public Chord getChordJustAbove (Point point)
    {
        Chord chordAbove = null;

        // Staff at or above point
        Staff targetStaff = measure.getPart().getStaffJustAbove(point);

        // We look for the chord just above
        for (Chord chord : getChords()) {
            Point head = chord.getHeadLocation();
            if (head != null && head.y < point.y) {
                if (chord.getStaff() == targetStaff) {
                    chordAbove = chord;
                }
            } else {
                break; // Since slot chords are sorted from top to bottom
            }
        }

        return chordAbove;
    }

    //-------------------//
    // getChordJustBelow //
    //-------------------//
    /**
     * Report the chord which is in staff just below the given point
     * in this slot.
     *
     * @param point the given point
     * @return the chord below, or null
     */
    public Chord getChordJustBelow (Point point)
    {
        // Staff at or below
        Staff targetStaff = measure.getPart().getStaffJustBelow(point);

        // We look for the chord just below
        for (Chord chord : getChords()) {
            Point head = chord.getHeadLocation();
            if (head != null && head.y > point.y
                && chord.getStaff() == targetStaff) {
                return chord;
            }
        }

        // Not found
        return null;
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the (sorted) collection of chords in this time slot.
     *
     * @return the collection of chords
     */
    public List<Chord> getChords ()
    {
        return incomings;
    }

    //-------------------//
    // getEmbracedChords //
    //-------------------//
    /**
     * Report the chords whose notes stand in the given vertical range.
     *
     * @param top    upper point of range
     * @param bottom lower point of range
     * @return the collection of chords, which may be empty
     */
    public List<Chord> getEmbracedChords (Point top,
                                          Point bottom)
    {
        List<Chord> embracedChords = new ArrayList<>();

        for (Chord chord : getChords()) {
            if (chord.isEmbracedBy(top, bottom)) {
                embracedChords.add(chord);
            }
        }

        return embracedChords;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the slot Id.
     *
     * @return the slot id (for debug)
     */
    public int getId ()
    {
        return id;
    }

    //--------------//
    // getStartTime //
    //--------------//
    /**
     * Report the time offset of this slot since the beginning of
     * the measure.
     *
     * @return the time offset of this slot.
     */
    public Rational getStartTime ()
    {
        return startTime;
    }

    //--------------//
    // setStartTime //
    //--------------//
    /**
     * Assign the startTime since the beginning of the measure,
     * for all chords in this time slot.
     *
     * @param startTime time offset since measure start
     */
    public void setStartTime (Rational startTime)
    {
        if (this.startTime == null) {
            logger.debug("setStartTime {} for Slot #{}", startTime, getId());
            this.startTime = startTime;

            // Assign to all chords of this slot first
            for (Chord chord : getChords()) {
                chord.setStartTime(startTime);
            }

            // Then, extend this information through the beamed chords if any
            for (Chord chord : getChords()) {
                BeamGroup group = chord.getBeamGroup();

                if (group != null) {
                    group.computeStartTimes();
                }
            }

            // Update all voices
            for (Voice voice : measure.getVoices()) {
                voice.updateSlotTable();
            }
        } else {
            if (!this.startTime.equals(startTime)) {
                getChords().get(0).addError(
                        "Reassigning startTime from " + this.startTime + " to "
                        + startTime + " in " + this);
            }
        }
    }

    //---------------//
    // toChordString //
    //---------------//
    /**
     * Report a slot description focused on the chords that start at
     * the slot.
     *
     * @return slot with its incoming chords
     */
    public String toChordString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("slot#").append(getId());

        if (getStartTime() != null) {
            sb.append(" start=").append(String.format("%5s", getStartTime()));
        }

        sb.append(" [");

        boolean started = false;

        for (Chord chord : getChords()) {
            if (started) {
                sb.append(",");
            }

            sb.append(chord);
            started = true;
        }

        sb.append("]");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Slot#").append(id);

        if (refPoint != null) {
            sb.append(" x=").append(getX());
        }

        if (startTime != null) {
            sb.append(" start=").append(startTime);
        }

        sb.append(" incomings=[");
        for (Chord chord : incomings) {
            sb.append("#").append(chord.getId());
        }
        sb.append("]");

        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // toVoiceString //
    //---------------//
    /**
     * Report a slot description focused on intersected voices.
     *
     * @return slot with its voices
     */
    public String toVoiceString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("slot#").append(getId()).append(" start=").append(String.
                format("%5s", getStartTime())).append(" [");

        SortedMap<Integer, Chord> voiceChords = new TreeMap<>();

        for (Chord chord : getChords()) {
            voiceChords.put(chord.getVoice().getId(), chord);
        }

        boolean started = false;
        int voiceMax = measure.getVoicesNumber();

        for (int iv = 1; iv <= voiceMax; iv++) {
            if (started) {
                sb.append(", ");
            } else {
                started = true;
            }

            Chord chord = voiceChords.get(iv);

            if (chord != null) {
                sb.append("V").append(chord.getVoice().getId());
                sb.append(" Ch#").append(String.format("%02d", chord.getId()));
                sb.append(" St").append(chord.getStaff().getId());
                sb.append(" Dur=").append(String.format("%5s",
                        chord.getDuration()));
            } else {
                sb.append("----------------------");
            }
        }

        sb.append("]");

        return sb.toString();
    }

    //--------------//
    // assignVoices //
    //--------------//
    /**
     * Assign available voices to the chords that have yet no voice
     * assigned.
     *
     * @param chords the collection of chords to process for this slot
     */
    private void assignVoices ()
    {
        // Assign remaining non-mapped chords, using first voice available
        // with staff continuity whenever possible
        for (Chord chord : incomings) {
            // Process only the chords that have no voice assigned yet
            if (chord.getVoice() == null) {
                // Try to reuse an existing voice
                for (Voice voice : measure.getVoices()) {
                    if (voice.isFree(this)) {
                        // If we have more than one incoming,
                        // avoid migrating a voice from one staff to another
                        if (incomings.size() > 1) {
                            Chord latestVoiceChord = voice.getChordBefore(this);
                            if (latestVoiceChord != null
                                && latestVoiceChord.getStaff() != chord.getStaff()) {
                                continue;
                            }
                        }
                        chord.setVoice(voice);
                        break;
                    }
                }

                // No compatible voice found, let's create a new one
                if (chord.getVoice() == null) {
                    logger.debug("{} Slot#{} creating voice for Ch#{}",
                            chord.getContextString(), id, chord.getId());

                    // Add a new voice
                    new Voice(chord);
                }
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //------------//
    // MyDistance //
    //------------//
    private static final class MyDistance
            implements InjectionSolver.Distance
    {
        //~ Static fields/initializers -----------------------------------------

        private static final int NO_LINK = 20;

        private static final int STAFF_DIFF = 40;

        private static final int INCOMPATIBLE_VOICES = 10000; // Forbidden

        //~ Instance fields ----------------------------------------------------
        private final List<Chord> news;

        private final List<Chord> olds;

        //~ Constructors -------------------------------------------------------
        public MyDistance (List<Chord> news,
                           List<Chord> olds)
        {
            this.news = news;
            this.olds = olds;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int getDistance (int in,
                                int ip)
        {
            // No link to an old chord
            if (ip >= olds.size()) {
                return NO_LINK;
            }

            Chord newChord = news.get(in);
            Chord oldChord = olds.get(ip);

            if ((newChord.getVoice() != null)
                && (oldChord.getVoice() != null)
                && (newChord.getVoice() != oldChord.getVoice())) {
                return INCOMPATIBLE_VOICES;
            } else if (newChord.getStaff() != oldChord.getStaff()) {
                return STAFF_DIFF;
            } else {
                int dy = Math.abs(
                        newChord.getHeadLocation().y
                        - oldChord.getHeadLocation().y) / newChord.getScale().
                        getInterline();
                int dStem = Math.abs(
                        newChord.getStemDir() - oldChord.getStemDir());

                return dy + (2 * dStem);
            }
        }
    }
}
