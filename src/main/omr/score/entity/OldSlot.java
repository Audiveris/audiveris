//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            S l o t                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
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
 * Class {@code Slot} represents a roughly defined time slot within a measure,
 * to gather all chords that start at the same time.
 * <p>
 * On the diagram shown, slots are indicated by vertical blue lines.</p>
 * <p>
 * The slot embraces all the staves of its part measure. Perhaps we should
 * consider merging slots between parts as well?
 *
 * <div style="float: right;">
 * <img src="doc-files/Slot.png" alt="diagram">
 * </div>
 *
 * @author Hervé Bitteur
 */
public class OldSlot
        implements Comparable<OldSlot>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(OldSlot.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The containing measure. */
    @Navigable(false)
    private final OldMeasure measure;

    /** Id unique within the containing measure. */
    private final int id;

    /** Reference point of the slot. */
    private Point refPoint;

    /** Chords incoming into this slot, sorted by staff then ordinate. */
    private final List<OldChord> incomings = new ArrayList<OldChord>();

    /** Time offset since measure start. */
    private Rational startTime;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Slot object.
     *
     * @param measure the containing measure
     */
    public OldSlot (OldMeasure measure)
    {
        this.measure = measure;

        id = 1 + measure.getSlots().size();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // dumpSystemSlots //
    //-----------------//
    public static void dumpSystemSlots (ScoreSystem system)
    {
        // Dump all measure slots
        logger.debug(system.toString());

        for (TreeNode node : system.getParts()) {
            OldSystemPart part = (OldSystemPart) node;

            logger.debug(part.toString());

            for (TreeNode mn : part.getMeasures()) {
                OldMeasure measure = (OldMeasure) mn;

                logger.debug(measure.toString());

                for (OldSlot slot : measure.getSlots()) {
                    logger.debug(slot.toString());
                }
            }
        }
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
    public void buildVoices (List<OldChord> endingChords)
    {
        logger.debug("endingChords={}", endingChords);
        logger.debug("incomings={}", incomings);

        // Sort chords vertically
        Collections.sort(incomings, OldChord.byOrdinate);

        // Some chords already have the voice assigned
        List<OldChord> endings = new ArrayList<OldChord>(endingChords);
        List<OldChord> rookies = new ArrayList<OldChord>();

        for (OldChord ch : incomings) {
            if (ch.getVoice() != null) {
                // Needed to populate the voice slotTable
                ch.setVoice(ch.getVoice());

                // Remove the ending chord with the same voice
                for (Iterator<OldChord> it = endings.iterator(); it.hasNext();) {
                    OldChord c = it.next();

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
                    OldVoice voice = endings.get(index).getVoice();
                    logger.debug("Slot#{} Reusing voice#{}", getId(), voice.getId());

                    OldChord ch = rookies.get(i);

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
    public int compareTo (OldSlot other)
    {
        return Integer.compare(getX(), other.getX());
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
    public OldChord getChordJustAbove (Point point)
    {
        OldChord chordAbove = null;

        // Staff at or above point
        OldStaff targetStaff = measure.getPart().getStaffJustAbove(point);

        // We look for the chord just above
        for (OldChord chord : getChords()) {
            Point head = chord.getHeadLocation();

            if ((head != null) && (head.y < point.y)) {
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
    public OldChord getChordJustBelow (Point point)
    {
        // Staff at or below
        OldStaff targetStaff = measure.getPart().getStaffJustBelow(point);

        // We look for the chord just below
        for (OldChord chord : getChords()) {
            Point head = chord.getHeadLocation();

            if ((head != null) && (head.y > point.y) && (chord.getStaff() == targetStaff)) {
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
    public List<OldChord> getChords ()
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
    public List<OldChord> getEmbracedChords (Point top,
                                          Point bottom)
    {
        List<OldChord> embracedChords = new ArrayList<OldChord>();

        for (OldChord chord : getChords()) {
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

    //
    //------------//
    // getMeasure //
    //------------//
    /**
     * Report the measure that contains this slot
     *
     * @return the containing measure
     */
    public OldMeasure getMeasure ()
    {
        return measure;
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

    //-----------//
    // setChords //
    //-----------//
    public void setChords (Collection<OldChord> chords)
    {
        this.incomings.addAll(chords);

        for (OldChord chord : chords) {
            chord.setSlot(this);
        }

        // Compute slot refPoint as average of chords centers
        Population xPop = new Population();
        Population yPop = new Population();

        for (OldChord chord : chords) {
            Point center = chord.getCenter();
            xPop.includeValue(center.x);
            yPop.includeValue(center.y);
        }

        refPoint = new Point(
                (int) Math.rint(xPop.getMeanValue()),
                (int) Math.rint(yPop.getMeanValue()));
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
            for (OldChord chord : getChords()) {
                chord.setStartTime(startTime);
            }

            // Then, extend this information through the beamed chords if any
            for (OldChord chord : getChords()) {
                OldBeamGroup group = chord.getBeamGroup();

                if (group != null) {
                    group.computeStartTimes();
                }
            }

            // Update all voices
            for (OldVoice voice : measure.getVoices()) {
                voice.updateSlotTable();
            }
        } else {
            if (!this.startTime.equals(startTime)) {
                getChords().get(0).addError(
                        "Reassigning startTime from " + this.startTime + " to " + startTime + " in "
                        + this);
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

        for (OldChord chord : getChords()) {
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

        for (OldChord chord : incomings) {
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
        sb.append("slot#").append(getId()).append(" start=")
                .append(String.format("%5s", getStartTime())).append(" [");

        SortedMap<Integer, OldChord> voiceChords = new TreeMap<Integer, OldChord>();

        for (OldChord chord : getChords()) {
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

            OldChord chord = voiceChords.get(iv);

            if (chord != null) {
                sb.append("V").append(chord.getVoice().getId());
                sb.append(" Ch#").append(String.format("%02d", chord.getId()));
                sb.append(" St").append(chord.getStaff().getId());
                sb.append(" Dur=").append(String.format("%5s", chord.getDuration()));
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
        for (OldChord chord : incomings) {
            // Process only the chords that have no voice assigned yet
            if (chord.getVoice() == null) {
                // Try to reuse an existing voice
                for (OldVoice voice : measure.getVoices()) {
                    if (voice.isFree(this)) {
                        // If we have more than one incoming,
                        // avoid migrating a voice from one staff to another
                        if (incomings.size() > 1) {
                            OldChord latestVoiceChord = voice.getChordBefore(this);

                            if ((latestVoiceChord != null)
                                && (latestVoiceChord.getStaff() != chord.getStaff())) {
                                continue;
                            }
                        }

                        chord.setVoice(voice);

                        break;
                    }
                }

                // No compatible voice found, let's create a new one
                if (chord.getVoice() == null) {
                    logger.debug(
                            "{} Slot#{} creating voice for Ch#{}",
                            chord.getContextString(),
                            id,
                            chord.getId());

                    // Add a new voice
                    new OldVoice(chord);
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //
    //------------//
    // MyDistance //
    //------------//
    private static final class MyDistance
            implements InjectionSolver.Distance
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final int NO_LINK = 20;

        private static final int STAFF_DIFF = 40;

        private static final int INCOMPATIBLE_VOICES = 10000; // Forbidden

        //~ Instance fields ------------------------------------------------------------------------
        private final List<OldChord> news;

        private final List<OldChord> olds;

        //~ Constructors ---------------------------------------------------------------------------
        public MyDistance (List<OldChord> news,
                           List<OldChord> olds)
        {
            this.news = news;
            this.olds = olds;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int getDistance (int in,
                                int ip)
        {
            // No link to an old chord
            if (ip >= olds.size()) {
                return NO_LINK;
            }

            OldChord newChord = news.get(in);
            OldChord oldChord = olds.get(ip);

            if ((newChord.getVoice() != null)
                && (oldChord.getVoice() != null)
                && (newChord.getVoice() != oldChord.getVoice())) {
                return INCOMPATIBLE_VOICES;
            } else if (newChord.getStaff() != oldChord.getStaff()) {
                return STAFF_DIFF;
            } else {
                int dy = Math.abs(newChord.getHeadLocation().y - oldChord.getHeadLocation().y) / newChord.getScale()
                        .getInterline();
                int dStem = Math.abs(newChord.getStemDir() - oldChord.getStemDir());

                return dy + (2 * dStem);
            }
        }
    }
}
