//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             S l o t                                            //
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

import org.audiveris.omr.math.InjectionSolver;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.beam.BeamGroup;
import org.audiveris.omr.sheet.rhythm.Voice.SlotVoice;
import static org.audiveris.omr.sheet.rhythm.Voice.Status.BEGIN;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "slot")
public class Slot
        implements Comparable<Slot>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            Slot.class);

    //~ Instance fields ----------------------------------------------------------------------------
    // Persistent data
    //----------------
    //
    /** Sequential Id unique within the containing stack. Starts at 1. */
    @XmlAttribute
    private final int id;

    /** Reference abscissa offset since measure start. */
    @XmlAttribute(name = "x-offset")
    private int xOffset;

    /** Time offset since measure start. */
    @XmlAttribute(name = "time-offset")
    @XmlJavaTypeAdapter(Rational.Adapter.class)
    private Rational timeOffset;

    /** Is slot suspicious?. */
    @XmlAttribute
    private Boolean suspicious;

    // Transient data
    //---------------
    //
    /** The containing measure stack. */
    @Navigable(false)
    private MeasureStack stack;

    /** Chords incoming into this slot, sorted by ordinate. */
    private List<AbstractChordInter> incomings;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Slot object.
     *
     * @param id        the slot id within the containing measure stack
     * @param stack     the containing measure stack
     * @param incomings the chords that start in this slot
     */
    public Slot (int id,
                 MeasureStack stack,
                 List<AbstractChordInter> incomings)
    {
        this.id = id;
        this.stack = stack;
        this.incomings = new ArrayList<AbstractChordInter>(incomings);

        for (AbstractChordInter chord : incomings) {
            chord.setSlot(this);
        }

        computeXOffset();
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private Slot ()
    {
        this.id = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // afterReload //
    //-------------//
    public void afterReload ()
    {
        try {
            // Populate incomings
            incomings = new ArrayList<AbstractChordInter>();

            final SystemInfo system = stack.getSystem();

            for (Part part : system.getParts()) {
                final Measure measure = stack.getMeasureAt(part);

                for (Voice voice : measure.getVoices()) {
                    final SlotVoice slotVoice = voice.getSlotInfo(this);

                    if ((slotVoice != null) && (slotVoice.status == BEGIN)) {
                        incomings.add(slotVoice.chord);

                        // Forward timeOffset to incoming chord
                        slotVoice.chord.setTimeOffset(timeOffset);
                        slotVoice.chord.setSlot(this);
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //-------------//
    // buildVoices //
    //-------------//
    /**
     * Compute the various voices in this slot.
     *
     * @param freeEndings the chords that end right at this slot, with their voice available.
     */
    public void buildVoices (List<AbstractChordInter> freeEndings)
    {
        logger.debug("incomings={}", incomings);

        // Sort incoming chords vertically
        Collections.sort(incomings, AbstractChordInter.byOrdinate);

        List<AbstractChordInter> rookies = new ArrayList<AbstractChordInter>();

        // Some chords already have their voice assigned
        for (AbstractChordInter ch : incomings) {
            if (ch.getVoice() != null) {
                // This pseudo-reassign is needed to populate the voice slotTable
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

                    AbstractChordInter ch = rookies.get(i);

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
    public AbstractChordInter getChordJustAbove (Point2D point)
    {
        AbstractChordInter chordAbove = null;

        // Staff at or above point
        Staff staff = stack.getSystem().getStaffAtOrAbove(point);

        if (staff != null) {
            // We look for the chord just above
            for (AbstractChordInter chord : getChords()) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y < point.getY())) {
                    if (chord.getBottomStaff() == staff) {
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
    public AbstractChordInter getChordJustBelow (Point2D point)
    {
        // Staff at or below point
        Staff staff = stack.getSystem().getStaffAtOrBelow(point);

        if (staff != null) {
            // We look for the chord just below
            for (AbstractChordInter chord : getChords()) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y > point.getY()) && (chord.getTopStaff() == staff)) {
                    return chord;
                }
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
    public List<AbstractChordInter> getChords ()
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
    public List<AbstractChordInter> getEmbracedChords (Point top,
                                                       Point bottom)
    {
        List<AbstractChordInter> embracedChords = new ArrayList<AbstractChordInter>();

        for (AbstractChordInter chord : getChords()) {
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

    //---------------//
    // getTimeOffset //
    //---------------//
    /**
     * Report the time offset of this slot since the beginning of the measure.
     *
     * @return the time offset of this slot.
     */
    public Rational getTimeOffset ()
    {
        return timeOffset;
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

    //--------------//
    // isSuspicious //
    //--------------//
    /**
     * @return the suspicious
     */
    public boolean isSuspicious ()
    {
        return (suspicious != null) && suspicious;
    }

    //----------//
    // setStack //
    //----------//
    public void setStack (MeasureStack stack)
    {
        this.stack = stack;
        computeXOffset();
    }

    //---------------//
    // setSuspicious //
    //---------------//
    /**
     * @param suspicious the suspicious to set
     */
    public void setSuspicious (boolean suspicious)
    {
        this.suspicious = suspicious ? true : null;
    }

    //---------------//
    // setTimeOffset //
    //---------------//
    /**
     * Assign the timeOffset since the beginning of the measure, for all chords in this
     * time slot.
     *
     * @param timeOffset time offset since measure start
     * @return true if OK, false otherwise
     */
    public boolean setTimeOffset (Rational timeOffset)
    {
        boolean failed = false;

        if (this.timeOffset == null) {
            logger.debug("setTimeOffset {} for Slot #{}", timeOffset, getId());
            this.timeOffset = timeOffset;

            // Assign to all chords of this slot first
            for (AbstractChordInter chord : incomings) {
                if (!chord.setTimeOffset(timeOffset)) {
                    failed = true;
                }
            }

            // Then, extend this information through the beamed chords if any
            for (AbstractChordInter chord : incomings) {
                BeamGroup group = chord.getBeamGroup();

                if (group != null) {
                    group.computeTimeOffsets();
                }
            }

            // Update all voices
            for (Voice voice : stack.getVoices()) {
                voice.updateSlotTable();
            }
        } else if (!this.timeOffset.equals(timeOffset)) {
            logger.warn(
                    "Reassigning timeOffset from " + this.timeOffset + " to " + timeOffset + " in "
                    + this);

            failed = true;
        }

        return !failed;
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

        if (getTimeOffset() != null) {
            sb.append(" start=").append(String.format("%5s", getTimeOffset()));
        }

        sb.append(" [");

        boolean started = false;

        for (AbstractChordInter chord : getChords()) {
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
        sb.append("Slot{#").append(id);

        sb.append(" xOffset=").append(xOffset);

        if (timeOffset != null) {
            sb.append(" timeOffset=").append(timeOffset);
        }

        sb.append(" incomings=[");

        for (AbstractChordInter chord : incomings) {
            sb.append("#").append(chord.getId());
        }

        sb.append("]");

        if (isSuspicious()) {
            sb.append(" SUSPICIOUS");
        }

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
        logger.warn("HB to be reimplemented");

        StringBuilder sb = new StringBuilder();

        //        sb.append("slot#").append(getId()).append(" start=")
        //                .append(String.format("%5s", getTimeOffset())).append(" [");
        //
        //        SortedMap<Integer, AbstractChordInter> voiceChords = new TreeMap<Integer, AbstractChordInter>();
        //
        //        for (AbstractChordInter chord : getChords()) {
        //            voiceChords.put(chord.getVoice().getId(), chord);
        //        }
        //
        //        boolean started = false;
        //        int voiceMax = stack.getVoiceCount();
        //
        //        for (int iv = 1; iv <= voiceMax; iv++) {
        //            if (started) {
        //                sb.append(", ");
        //            } else {
        //                started = true;
        //            }
        //
        //            AbstractChordInter chord = voiceChords.get(iv);
        //
        //            if (chord != null) {
        //                sb.append("V").append(chord.getVoice().getId());
        //                sb.append(" Ch#").append(String.format("%02d", chord.getId()));
        //                sb.append(" St").append(chord.getTopStaff().getId());
        //
        //                if (chord.getBottomStaff() != chord.getTopStaff()) {
        //                    sb.append("-").append(chord.getBottomStaff().getId());
        //                }
        //
        //                sb.append(" Dur=").append(String.format("%5s", chord.getDuration()));
        //            } else {
        //                sb.append("----------------------");
        //            }
        //        }
        //
        //        sb.append("]");
        return sb.toString();
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled
     * for this object, but before this object is set to the parent object.
     */
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        stack = (MeasureStack) parent;
    }

    //--------------//
    // assignVoices //
    //--------------//
    /**
     * Assign available voices to the chords that have no voice assigned yet.
     * <p>
     * A voice cannot migrate from one part to another, but may migrate from one staff to another
     * under some conditions.
     */
    private void assignVoices ()
    {
        // Assign remaining non-mapped chords, using first voice available in part and
        // with staff continuity whenever possible
        for (AbstractChordInter chord : incomings) {
            // Process only the chords that have no voice assigned yet
            if (chord.getVoice() != null) {
                continue;
            }

            final Part part = chord.getPart();
            final Measure measure = stack.getMeasureAt(part);

            // Try to reuse an existing voice in same part (and within same staff if possible)
            for (Voice voice : measure.getVoices()) {
                if (voice.isFree(this)) {
                    // If we have more than one incoming,
                    // avoid migrating a voice from one staff to another
                    AbstractChordInter latestVoiceChord = voice.getChordBefore(this);

                    if (latestVoiceChord != null) {
                        if ((incomings.size() > 1)
                            && (latestVoiceChord.getTopStaff() != chord.getTopStaff())) {
                            continue;
                        }

                        // // Check there is no time hole with latest voice chord
                        // // Otherwise there is a rhythm error
                        // // (missing rest/dot on this voice / missing tuplet on other voice)
                        // Rational delta = timeOffset.minus(latestVoiceChord.getEndTime());
                        //
                        // if (delta.compareTo(Rational.ZERO) > 0) {
                        //     logger.debug("{} {} {} time hole: {}", measure, this, voice, delta);
                        // }
                    }

                    chord.setVoice(voice);

                    break;
                }
            }

            // No compatible voice found in part, so let's create a new one
            if (chord.getVoice() == null) {
                logger.debug("{} Slot#{} creating voice for Ch#{}", measure, id, chord.getId());

                // Add a new voice
                measure.addVoice(new Voice(chord, chord.getMeasure()));
            }
        }
    }

    //----------------//
    // computeXOffset //
    //----------------//
    private void computeXOffset ()
    {
        // Compute slot refPoint as average of chords centers
        Population xPop = new Population();
        Population yPop = new Population();

        for (AbstractChordInter chord : incomings) {
            Point center = chord.getCenter();
            xPop.includeValue(center.x);
            yPop.includeValue(center.y);
        }

        Point2D ref = new Point2D.Double(xPop.getMeanValue(), yPop.getMeanValue());

        // Store abscissa offset WRT measure stack left border
        xOffset = (int) Math.rint(stack.getXOffset(ref));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //
    //------------//
    // MyDistance //
    //------------//
    /**
     * Implementation of a 'distance' between an old chord and a new chord to be
     * potentially linked in the same voice.
     */
    private static final class MyDistance
            implements InjectionSolver.Distance
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final int NOT_A_REST = 5;

        private static final int NEW_IN_STAFF = 10;

        private static final int NO_LINK = 20;

        private static final int STAFF_DIFF = 40;

        private static final int INCOMPATIBLE_VOICES = 10000; // Forbidden

        //~ Instance fields ------------------------------------------------------------------------
        private final List<AbstractChordInter> news;

        private final List<AbstractChordInter> olds;

        private final Scale scale;

        //~ Constructors ---------------------------------------------------------------------------
        public MyDistance (List<AbstractChordInter> news,
                           List<AbstractChordInter> olds,
                           Scale scale)
        {
            this.news = news;
            this.olds = olds;
            this.scale = scale;
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Distance between newChord (index 'in') and oldChord (index 'ip').
         *
         * @param in index in new chords
         * @param ip index in old chords
         * @return the assigned 'distance' between these two chords
         */
        @Override
        public int getDistance (int in,
                                int ip)
        {
            // No link to an old chord
            if (ip >= olds.size()) {
                return NO_LINK;
            }

            AbstractChordInter newChord = news.get(in);
            AbstractChordInter oldChord = olds.get(ip);

            // Different assigned voices?
            if ((newChord.getVoice() != null)
                && (oldChord.getVoice() != null)
                && (newChord.getVoice() != oldChord.getVoice())) {
                return INCOMPATIBLE_VOICES;
            }

            // Different staves? (beware: some chords embrace two staves, hence we use topStaff)
            if (newChord.getTopStaff() != oldChord.getTopStaff()) {
                // Different (top) staves, but are they in same part?
                if (newChord.getPart() != oldChord.getPart()) {
                    return INCOMPATIBLE_VOICES;
                } else {
                    return STAFF_DIFF;
                }
            }

            // OK, here old chord and new chord are in the same staff
            //
            // Penalty for a chord of a beam group that spans several staves
            int ds = 0;
            BeamGroup group = oldChord.getBeamGroup();

            if ((group != null) && group.isMultiStaff()) {
                ds = NEW_IN_STAFF;
            }

            // A rest is a placeholder, hence bonus for rest (implemented by penalty on non-rest)
            int dr = 0;

            if (oldChord instanceof HeadChordInter) {
                dr += NOT_A_REST;
            }

            // Pitch difference
            int dy = Math.abs(newChord.getHeadLocation().y - oldChord.getHeadLocation().y) / scale.getInterline();

            // Stem direction difference
            int dStem = Math.abs(newChord.getStemDir() - oldChord.getStemDir());

            return ds + dr + dy + (2 * dStem); // This is our recipe...
        }
    }
}
