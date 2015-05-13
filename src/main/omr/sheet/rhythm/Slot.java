//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             S l o t                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.math.InjectionSolver;
import omr.math.Population;
import omr.math.Rational;

import omr.sheet.Scale;
import omr.sheet.Staff;
import omr.sheet.beam.BeamGroup;

import omr.sig.inter.ChordInter;

import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class {@code Slot} represents a roughly defined time slot within a measure stack,
 * to gather all chords that start at the same time.
 * <p>
 * On the diagram shown, slots are indicated by vertical blue lines.</p>
 * <p>
 * The slot embraces all the staves of the system.
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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Slot.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The containing measure stack. */
    @Navigable(false)
    private final MeasureStack stack;

    /** Sequential Id unique within the containing stack. Starts at 1. */
    private final int id;

    /** Reference abscissa offset since measure start. */
    private int xOffset;

    /** Chords incoming into this slot, sorted by ordinate. */
    private final List<ChordInter> incomings = new ArrayList<ChordInter>();

    /** Time offset since measure start. */
    private Rational startTime;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Slot object.
     *
     * @param id    the slot id within the containing measure stack
     * @param stack the containing measure stack
     */
    public Slot (int id,
                 MeasureStack stack)
    {
        this.id = id;
        this.stack = stack;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // buildVoices //
    //-------------//
    /**
     * Compute the various voices in this slot.
     *
     * @param freeEndings the chords that end right at this slot, with their voice available.
     */
    public void buildVoices (List<ChordInter> freeEndings)
    {
        logger.debug("incomings={}", incomings);

        // Sort incoming chords vertically
        Collections.sort(incomings, ChordInter.byOrdinate);

        List<ChordInter> rookies = new ArrayList<ChordInter>();

        // Some chords already have their voice assigned
        for (ChordInter ch : incomings) {
            if (ch.getVoice() != null) {
                // This pseudo-reassign is needed to populate the voice slotTable (???)
                ch.setVoice(ch.getVoice());
            } else {
                rookies.add(ch);
            }
        }

        // Nothing left to assign?
        if (rookies.isEmpty()) {
            return;
        }

        // Try to map some free ending voices to some rookies
        if (!freeEndings.isEmpty()) {
            final Scale scale = stack.getSystem().getSheet().getScale();
            final InjectionSolver solver = new InjectionSolver(
                    rookies.size(),
                    freeEndings.size() + rookies.size(),
                    new MyDistance(rookies, freeEndings, scale));
            final int[] links = solver.solve();

            for (int i = 0; i < links.length; i++) {
                int index = links[i];

                // Map new chord to a free ending chord?
                if (index < freeEndings.size()) {
                    Voice voice = freeEndings.get(index).getVoice();
                    logger.debug("Slot#{} Reusing voice#{}", getId(), voice.getId());

                    ChordInter ch = rookies.get(i);

                    try {
                        ch.setVoice(voice);
                    } catch (Exception ex) {
                        logger.warn("{} failed to set voice of chord", ch);

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
     * Compare this slot to another, as needed to insert slots in an ordered collection.
     *
     * @param other another slot
     * @return -1, 0 or +1, according to their relative abscissae
     */
    @Override
    public int compareTo (Slot other)
    {
        return Double.compare(xOffset, other.xOffset);
    }

    //-------------------//
    // getChordJustAbove //
    //-------------------//
    /**
     * Report the chord which is in staff just above the given point in this slot.
     *
     * @param point the given point
     * @return the chord above, or null
     */
    public ChordInter getChordJustAbove (Point2D point)
    {
        ChordInter chordAbove = null;

        // Staff at or above point
        Staff staff = stack.getSystem().getStaffAtOrAbove(point);

        if (staff != null) {
            // We look for the chord just above
            for (ChordInter chord : getChords()) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y < point.getY())) {
                    if (chord.getStaff() == staff) {
                        chordAbove = chord;
                    }
                } else {
                    break; // Since slot chords are sorted from top to bottom
                }
            }
        }

        return chordAbove;
    }

    //-------------------//
    // getChordJustBelow //
    //-------------------//
    /**
     * Report the chord which is in staff just below the given point in this slot.
     *
     * @param point the given point
     * @return the chord below, or null
     */
    public ChordInter getChordJustBelow (Point2D point)
    {
        // Staff at or below point
        Staff staff = stack.getSystem().getStaffAtOrBelow(point);

        if (staff != null) {
            // We look for the chord just below
            for (ChordInter chord : getChords()) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y > point.getY()) && (chord.getStaff() == staff)) {
                    return chord;
                }
            }
        }

        // Not found
        return null;
    }

    //
    //    //-------------------//
    //    // getChordJustAbove //
    //    //-------------------//
    //    /**
    //     * Report the chord which is in staff just above the given point in this slot.
    //     *
    //     * @param point the given point
    //     * @return the chord above, or null
    //     */
    //    public ChordInter getChordJustAbove (Point point)
    //    {
    //        ChordInter chordAbove = null;
    //
    //        // Staff at or above point
    //        Staff targetStaff = stack.getPart().getStaffJustAbove(point);
    //
    //        // We look for the chord just above
    //        for (ChordInter chord : getChords()) {
    //            Point head = chord.getHeadLocation();
    //
    //            if ((head != null) && (head.y < point.y)) {
    //                if (chord.getStaff() == targetStaff) {
    //                    chordAbove = chord;
    //                }
    //            } else {
    //                break; // Since slot chords are sorted from top to bottom
    //            }
    //        }
    //
    //        return chordAbove;
    //    }
    //
    //    //-------------------//
    //    // getChordJustBelow //
    //    //-------------------//
    //    /**
    //     * Report the chord which is in staff just below the given point
    //     * in this slot.
    //     *
    //     * @param point the given point
    //     * @return the chord below, or null
    //     */
    //    public ChordInter getChordJustBelow (Point point)
    //    {
    //        // Staff at or below
    //        Staff targetStaff = stack.getPart().getStaffJustBelow(point);
    //
    //        // We look for the chord just below
    //        for (ChordInter chord : getChords()) {
    //            Point head = chord.getHeadLocation();
    //
    //            if ((head != null) && (head.y > point.y) && (chord.getStaff() == targetStaff)) {
    //                return chord;
    //            }
    //        }
    //
    //        // Not found
    //        return null;
    //    }
    //
    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the (sorted) collection of chords in this time slot.
     *
     * @return the collection of chords
     */
    public List<ChordInter> getChords ()
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
    public List<ChordInter> getEmbracedChords (Point top,
                                               Point bottom)
    {
        List<ChordInter> embracedChords = new ArrayList<ChordInter>();

        for (ChordInter chord : getChords()) {
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

    //-----------------//
    // getMeasureStack //
    //-----------------//
    /**
     * Report the measure stack that contains this slot
     *
     * @return the containing measure stack
     */
    public MeasureStack getMeasureStack ()
    {
        return stack;
    }

    //--------------//
    // getStartTime //
    //--------------//
    /**
     * Report the time offset of this slot since the beginning of the measure.
     *
     * @return the time offset of this slot.
     */
    public Rational getStartTime ()
    {
        return startTime;
    }

    //------------//
    // getXOffset //
    //------------//
    /**
     * Report the abscissa offset of this slot, WRT measure start.
     *
     * @return the abscissa offset within measure
     */
    public int getXOffset ()
    {
        return xOffset;
    }

    //-----------//
    // setChords //
    //-----------//
    public void setChords (Collection<ChordInter> chords)
    {
        this.incomings.addAll(chords);

        for (ChordInter chord : chords) {
            chord.setSlot(this);
        }

        // Compute slot refPoint as average of chords centers
        Population xPop = new Population();
        Population yPop = new Population();

        for (ChordInter chord : chords) {
            Point center = chord.getCenter();
            xPop.includeValue(center.x);
            yPop.includeValue(center.y);
        }

        Point2D ref = new Point2D.Double(xPop.getMeanValue(), yPop.getMeanValue());

        // Store abscissa offset WRT measure stack left border
        xOffset = (int) Math.rint(stack.getXOffset(ref));
    }

    //--------------//
    // setStartTime //
    //--------------//
    /**
     * Assign the startTime since the beginning of the measure, for all chords in this
     * time slot.
     *
     * @param startTime time offset since measure start
     * @return true if OK, false otherwise
     */
    public boolean setStartTime (Rational startTime)
    {
        if (this.startTime == null) {
            logger.debug("setStartTime {} for Slot #{}", startTime, getId());
            this.startTime = startTime;

            // Assign to all chords of this slot first
            for (ChordInter chord : incomings) {
                if (!chord.setStartTime(startTime)) {
                    return false;
                }
            }

            // Then, extend this information through the beamed chords if any
            for (ChordInter chord : incomings) {
                BeamGroup group = chord.getBeamGroup();

                if (group != null) {
                    group.computeStartTimes();
                }
            }

            // Update all voices
            for (Voice voice : stack.getVoices()) {
                voice.updateSlotTable();
            }
        } else {
            if (!this.startTime.equals(startTime)) {
                logger.warn(
                        "Reassigning startTime from " + this.startTime + " to " + startTime + " in "
                        + this);

                return false;
            }
        }

        return true;
    }

    //---------------//
    // toChordString //
    //---------------//
    /**
     * Report a slot description focused on the chords that start at the slot.
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

        for (ChordInter chord : getChords()) {
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

        sb.append(" xOffset=").append(xOffset);

        if (startTime != null) {
            sb.append(" start=").append(startTime);
        }

        sb.append(" incomings=[");

        for (ChordInter chord : incomings) {
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

        SortedMap<Integer, ChordInter> voiceChords = new TreeMap<Integer, ChordInter>();

        for (ChordInter chord : getChords()) {
            voiceChords.put(chord.getVoice().getId(), chord);
        }

        boolean started = false;
        int voiceMax = stack.getVoiceCount();

        for (int iv = 1; iv <= voiceMax; iv++) {
            if (started) {
                sb.append(", ");
            } else {
                started = true;
            }

            ChordInter chord = voiceChords.get(iv);

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
     * Assign available voices to the chords that have yet no voice assigned.
     */
    private void assignVoices ()
    {
        // Assign remaining non-mapped chords, using first voice available
        // with staff continuity whenever possible
        for (ChordInter chord : incomings) {
            // Process only the chords that have no voice assigned yet
            if (chord.getVoice() != null) {
                continue;
            }

            // Try to reuse an existing voice (within same staff if possible)
            for (Voice voice : stack.getVoices()) {
                if (voice.isFree(this)) {
                    // If we have more than one incoming,
                    // avoid migrating a voice from one staff to another
                    if (incomings.size() > 1) {
                        ChordInter latestVoiceChord = voice.getChordBefore(this);

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
                logger.debug("{} Slot#{} creating voice for Ch#{}", stack, id, chord.getId());

                // Add a new voice
                stack.getVoices().add(new Voice(chord, chord.getMeasure()));
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

        private static final int NEW_IN_STAFF = 10;

        private static final int NO_LINK = 20;

        private static final int STAFF_DIFF = 40;

        private static final int INCOMPATIBLE_VOICES = 10000; // Forbidden

        //~ Instance fields ------------------------------------------------------------------------
        private final List<ChordInter> news;

        private final List<ChordInter> olds;

        private final Scale scale;

        //~ Constructors ---------------------------------------------------------------------------
        public MyDistance (List<ChordInter> news,
                           List<ChordInter> olds,
                           Scale scale)
        {
            this.news = news;
            this.olds = olds;
            this.scale = scale;
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

            ChordInter newChord = news.get(in);
            ChordInter oldChord = olds.get(ip);

            if ((newChord.getVoice() != null)
                && (oldChord.getVoice() != null)
                && (newChord.getVoice() != oldChord.getVoice())) {
                return INCOMPATIBLE_VOICES;
            } else if (newChord.getStaff() != oldChord.getStaff()) {
                // Different staves, but are they in same part?
                if (newChord.getStaff().getPart() != oldChord.getStaff().getPart()) {
                    return INCOMPATIBLE_VOICES;
                } else {
                    return STAFF_DIFF;
                }
            } else {
                int ds = 0;
                BeamGroup group = oldChord.getBeamGroup();

                if ((group != null) && group.isMultiStaff()) {
                    ds = NEW_IN_STAFF;
                }

                int dy = Math.abs(newChord.getHeadLocation().y - oldChord.getHeadLocation().y) / scale.getInterline();
                int dStem = Math.abs(newChord.getStemDir() - oldChord.getStemDir());

                return ds + dy + (2 * dStem);
            }
        }
    }
}
