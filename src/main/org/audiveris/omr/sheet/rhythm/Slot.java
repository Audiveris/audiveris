//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             S l o t                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.math.Population;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code Slot} represents a roughly defined time slot within a measure stack,
 * or just within a measure, to gather all chords that start at the same time.
 * <p>
 * On the diagram shown, slots are indicated by vertical blue lines.
 * <p>
 * NOTA: A system slot embraces all the staves in the system, while a measure slot embraces only
 * the staves in the containing part.
 * <br>
 * <img src="doc-files/Slot.png" alt="Slot">
 * <p>
 * Data model:
 * <br>
 * <img src="doc-files/Slots.png" alt="Slots">
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "slot")
public class Slot
        implements Comparable<Slot>
{

    private static final Logger logger = LoggerFactory.getLogger(Slot.class);

    // Persistent data
    //----------------
    //
    /** Sequential Id unique within the containing stack. Starts at 1. */
    @XmlAttribute
    protected final int id;

    /** Reference abscissa offset since measure start. */
    @XmlAttribute(name = "x-offset")
    protected int xOffset;

    /** Time offset since measure start. */
    @XmlAttribute(name = "time-offset")
    @XmlJavaTypeAdapter(Rational.Adapter.class)
    protected Rational timeOffset;

    /** Is slot suspicious?. */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    protected boolean suspicious;

    // Transient data
    //---------------
    //
    /** The containing measure stack. */
    @Navigable(false)
    protected MeasureStack stack;

    /** Chords incoming into this slot, ordered by center ordinate. */
    protected List<AbstractChordInter> incomings;

    /**
     * Creates a new (stack) Slot object.
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
        this.incomings = new ArrayList<>(incomings);
        Collections.sort(this.incomings, Inters.byCenterOrdinate);

        for (AbstractChordInter chord : this.incomings) {
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

    //-------------//
    // afterReload //
    //-------------//
    /**
     * To be called right after unmarshalling.
     */
    public void afterReload ()
    {
        try {
            // Populate incomings
            incomings = new ArrayList<>();

            final SystemInfo system = stack.getSystem();

            for (Part part : system.getParts()) {
                final Measure measure = stack.getMeasureAt(part);

                for (Voice voice : measure.getVoices()) {
                    final SlotVoice slotVoice = voice.getSlotInfo(this);

                    if ((slotVoice != null) && (slotVoice.status == SlotVoice.Status.BEGIN)) {
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

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Slot) {
            return compareTo((Slot) obj) == 0;
        }

        return false;
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
    //    public AbstractChordInter getChordJustAbove (Point2D point)
    //    {
    //        AbstractChordInter chordAbove = null;
    //
    //        // Staff at or above point
    //        Staff staff = stack.getSystem().getStaffAtOrAbove(point);
    //
    //        if (staff != null) {
    //            // We look for the chord just above
    //            for (AbstractChordInter chord : getChords()) {
    //                Point head = chord.getHeadLocation();
    //
    //                if ((head != null) && (head.y < point.getY())) {
    //                    if (chord.getBottomStaff() == staff) {
    //                        chordAbove = chord;
    //                    }
    //                } else {
    //                    break; // Since slot chords are sorted from top to bottom
    //                }
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
    //     * Report the chord which is in staff just below the given point in this slot.
    //     *
    //     * @param point the given point
    //     * @return the chord below, or null
    //     */
    //    public AbstractChordInter getChordJustBelow (Point2D point)
    //    {
    //        // Staff at or below point
    //        Staff staff = stack.getSystem().getStaffAtOrBelow(point);
    //
    //        if (staff != null) {
    //            // We look for the chord just below
    //            for (AbstractChordInter chord : getChords()) {
    //                Point head = chord.getHeadLocation();
    //
    //                if ((head != null) && (head.y > point.getY()) && (chord.getTopStaff() == staff)) {
    //                    return chord;
    //                }
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
     * Report the collection of incoming chords in this time slot.
     *
     * @return the collection of incoming chords
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
        List<AbstractChordInter> embracedChords = new ArrayList<>();

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

    //----------//
    // getStack //
    //----------//
    /**
     * Report the measure stack that contains this slot
     *
     * @return the containing measure stack
     */
    public MeasureStack getStack ()
    {
        return stack;
    }

    //----------//
    // setStack //
    //----------//
    /**
     * Set the containing stack for this slot.
     *
     * @param stack containing stack
     */
    public void setStack (MeasureStack stack)
    {
        this.stack = stack;
        computeXOffset();
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

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (59 * hash) + this.xOffset;

        return hash;
    }

    //--------------//
    // isSuspicious //
    //--------------//
    /**
     * @return the suspicious
     */
    public boolean isSuspicious ()
    {
        return suspicious;
    }

    //---------------//
    // setSuspicious //
    //---------------//
    /**
     * @param suspicious the suspicious to set
     */
    public void setSuspicious (boolean suspicious)
    {
        this.suspicious = suspicious;
    }

    //---------------//
    // setTimeOffset //
    //---------------//
    /**
     * Assign the timeOffset since the beginning of the measure.
     *
     * @param timeOffset time offset since measure start
     */
    public void setTimeOffset (Rational timeOffset)
    {
        logger.debug("Stack#{} slot#{} setTimeOffset {}", stack.getIdValue(), id, timeOffset);
        this.timeOffset = timeOffset;
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
        final StringBuilder sb = new StringBuilder();
        sb.append("Slot{#").append(id);

        sb.append(" xOffset=").append(xOffset);

        if (timeOffset != null) {
            sb.append(" timeOffset=").append(timeOffset);
        }

        if (incomings != null) {
            sb.append(" incomings=[");

            for (AbstractChordInter chord : incomings) {
                sb.append("#").append(chord.getId());
            }

            sb.append("]");
        }

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
        StringBuilder sb = new StringBuilder();

        sb.append("slot#").append(getId()).append(" start=").append(
                String.format("%5s", getTimeOffset())).append(" [");

        SortedMap<Integer, AbstractChordInter> voiceChords = new TreeMap<>();

        for (AbstractChordInter chord : getChords()) {
            voiceChords.put(chord.getVoice().getId(), chord);
        }

        boolean started = false;

        for (Entry<Integer, AbstractChordInter> entry : voiceChords.entrySet()) {
            if (started) {
                sb.append(", ");
            } else {
                started = true;
            }

            final AbstractChordInter chord = entry.getValue();

            if (chord != null) {
                sb.append("V").append(chord.getVoice().getId());
                sb.append(" Ch#").append(String.format("%02d", chord.getId()));
                sb.append(" s:").append(chord.getTopStaff().getId());

                if (chord.getBottomStaff() != chord.getTopStaff()) {
                    sb.append("-").append(chord.getBottomStaff().getId());
                }

                sb.append(" Dur=").append(String.format("%5s", chord.getDuration()));
            } else {
                sb.append("----------------------");
            }
        }

        sb.append("]");

        return sb.toString();
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled
     * for this object, but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        stack = (MeasureStack) parent;
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

    //-------------//
    // MeasureSlot //
    //-------------//
    /**
     * A slot which embraces just a measure height.
     * (whereas a "full" slot embraces a system height).
     */
    public static class MeasureSlot
            extends Slot
    {

        /** The containing measure. */
        @Navigable(false)
        protected Measure measure;

        /**
         * Creates a new {@code MeasureSlot} object.
         *
         * @param id        the slot id within the containing measure stack
         * @param measure   the containing measure
         * @param incomings the chords that start in this slot
         */
        public MeasureSlot (int id,
                            Measure measure,
                            List<AbstractChordInter> incomings)
        {
            super(id, measure.getStack(), incomings);

            this.measure = measure;
        }
    }

    //--------------//
    // CompoundSlot //
    //--------------//
    /**
     * A (rather wide) measure slot composed of one or several (narrow) slots.
     * <ul>
     * <li>The <b>wide</b> slot is used for voice linking: there is no guarantee that all chords in
     * such slot share the same time offset, but they cannot be in sequence, meaning two chords in
     * this slot cannot belong to the same voice.
     * <li>The <b>narrow</b> slot is used for time information: we can consider that all its chords
     * are so well graphically aligned that they share the same time offset value.
     * Two narrow slots that are members of the same wide slot, may or may not have the same time
     * offset value.
     * </ul>
     * This relies on the assumption that all time durations are correct, which is <b>not</b> the
     * case for implicit tuplet signs which have not been detected yet.
     */
    public static class CompoundSlot
            extends MeasureSlot
    {

        /** The narrow slots that compose this compound. */
        private final List<MeasureSlot> members;

        public CompoundSlot (int id,
                             Measure measure,
                             List<MeasureSlot> members)
        {
            super(id, measure, chordsOf(members));
            this.members = members;
        }

        public List<MeasureSlot> getMembers ()
        {
            return members;
        }

        public MeasureSlot getNarrowSlot (AbstractChordInter chord)
        {
            for (MeasureSlot member : members) {
                if (member.incomings.contains(chord)) {
                    return member;
                }
            }

            logger.error("{} not in any member slot of {}", chord, this);

            return null;
        }
    }

    //----------//
    // chordsOf //
    //----------//
    /**
     * Report the global list of chords of the provided slots.
     *
     * @param slots the provided slots
     * @return the global sequence of chords
     */
    private static List<AbstractChordInter> chordsOf (List<MeasureSlot> slots)
    {
        List<AbstractChordInter> chords = new ArrayList<>();

        for (MeasureSlot slot : slots) {
            chords.addAll(slot.getChords());
        }

        Collections.sort(chords, Inters.byCenterOrdinate);

        return chords;
    }
}
