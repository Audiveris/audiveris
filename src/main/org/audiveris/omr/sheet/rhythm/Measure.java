//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          M e a s u r e                                         //
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

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.DurationFactor;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.beam.BeamGroup;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.rhythm.Voice.Family;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Trimmable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code Measure} represents a measure in a system part, it vertically embraces
 * all the staves (usually 1 or 2) of the containing part.
 *
 * @see MeasureStack
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "measure")
public class Measure
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Measure.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Anomaly detected, if any. */
    @XmlAttribute(name = "abnormal")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean abnormal;

    /** Left barline, if any. */
    @XmlElement(name = "left-barline")
    private PartBarline leftBarline;

    /** Mid barline, if any. */
    @XmlElement(name = "mid-barline")
    private PartBarline midBarline;

    /** Right barline, if any. */
    @XmlElement(name = "right-barline")
    private PartBarline rightBarline;

    /** Groups of beams in this measure. Populated by CHORDS step. */
    @XmlElementRef
    @Trimmable.Collection
    private final LinkedHashSet<BeamGroup> beamGroups = new LinkedHashSet<>();

    /**
     * Possibly several Clefs per staff.
     * Implemented as a list, kept ordered by clef full abscissa
     */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "clefs")
    @Trimmable.Collection
    private final ArrayList<ClefInter> clefs = new ArrayList<>();

    /** Possibly one Key signature per staff, since keys may differ between staves. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "keys")
    @Trimmable.Collection
    private final LinkedHashSet<KeyInter> keys = new LinkedHashSet<>();

    /** Possibly one Time signature per staff. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "times")
    @Trimmable.Collection
    private final LinkedHashSet<AbstractTimeInter> timeSigs = new LinkedHashSet<>();

    /** Head chords, both standard and small. Populated by CHORDS step. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "head-chords")
    @Trimmable.Collection
    private final LinkedHashSet<HeadChordInter> headChords = new LinkedHashSet<>();

    /** Rest chords. Populated by RHYTHMS step. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "rest-chords")
    @Trimmable.Collection
    private final LinkedHashSet<RestChordInter> restChords = new LinkedHashSet<>();

    /** Flags. Populated by SYMBOLS step. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "flags")
    @Trimmable.Collection
    private final LinkedHashSet<FlagInter> flags = new LinkedHashSet<>();

    /** Tuplets. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "tuplets")
    @Trimmable.Collection
    private final LinkedHashSet<TupletInter> tuplets = new LinkedHashSet<>();

    /** Augmentation dots. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "augmentations-dots")
    @Trimmable.Collection
    private final LinkedHashSet<AugmentationDotInter> augDots = new LinkedHashSet<>();

    /** Voices within this measure, sorted by voice id. Populated by RHYTHMS step. */
    @XmlElement(name = "voice")
    @Trimmable.Collection
    private final ArrayList<Voice> voices = new ArrayList<>();

    // Transient data
    //---------------
    //
    /** To flag a dummy measure. */
    private boolean dummy;

    /** The containing part. */
    @Navigable(false)
    private Part part;

    /** The containing measure stack. */
    @Navigable(false)
    private MeasureStack stack;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Measure} object.
     *
     * @param part the containing part
     */
    public Measure (Part part)
    {
        this.part = part;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private Measure ()
    {
        this.part = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // addBeamGroup //
    //--------------//
    /**
     * Add a beam group to this measure.
     *
     * @param group a beam group to add
     */
    public void addBeamGroup (BeamGroup group)
    {
        beamGroups.add(group);
    }

    //-------------------//
    // addDummyWholeRest //
    //-------------------//
    /**
     * Insert a whole rest, with related chord, on provided staff in this measure.
     *
     * @param staff specified staff in measure
     */
    public void addDummyWholeRest (Staff staff)
    {
        // We use fakes that mimic a rest chord with its whole rest.
        class FakeChord
                extends RestChordInter
        {

            List<Inter> members;

            @Override
            public List<Inter> getMembers ()
            {
                return members;
            }

            @Override
            public DurationFactor getTupletFactor ()
            {
                return null;
            }
        }

        class FakeRest
                extends RestInter
        {

            FakeChord chord;

            FakeRest (Staff staff)
            {
                super(null, Shape.WHOLE_REST, 0, staff, -1.0);
            }

            @Override
            public RestChordInter getChord ()
            {
                return chord;
            }
        }

        FakeRest whole = new FakeRest(staff);
        FakeChord chord = new FakeChord();

        chord.setStaff(staff);
        chord.setTimeOffset(Rational.ZERO);
        chord.members = Collections.singletonList((Inter) whole);
        whole.chord = chord;

        addInter(chord);
        addVoice(Voice.createWholeVoice(chord, this));
    }

    //----------//
    // addInter //
    //----------//
    /**
     * Include the provided inter into its proper set within this measure.
     *
     * @param inter the inter to include
     */
    public void addInter (Inter inter)
    {
        if (inter.isVip()) {
            logger.info("VIP addInter {} into {}", inter, this);
        }

        if (inter instanceof ClefInter) {
            final ClefInter clef = (ClefInter) inter;

            if (!clefs.contains(clef)) {
                clefs.add(clef);

                if ((clefs.size() > 1) && (clef.getCenter() != null)) {
                    Collections.sort(clefs, Inters.byFullCenterAbscissa);
                }
            }
        } else if (inter instanceof KeyInter) {
            keys.add((KeyInter) inter);
        } else if (inter instanceof AbstractTimeInter) {
            timeSigs.add((AbstractTimeInter) inter);
        } else if (inter instanceof AbstractChordInter) {
            AbstractChordInter chord = (AbstractChordInter) inter;
            chord.setMeasure(this);

            if (chord instanceof HeadChordInter) {
                headChords.add((HeadChordInter) chord);
            } else if (chord instanceof RestChordInter) {
                restChords.add((RestChordInter) chord);
            }
        } else if (inter instanceof FlagInter) {
            flags.add((FlagInter) inter);
        } else if (inter instanceof TupletInter) {
            tuplets.add((TupletInter) inter);
        } else if (inter instanceof AugmentationDotInter) {
            augDots.add((AugmentationDotInter) inter);
        } else {
            logger.error("Attempt to use addInter() with {}", inter);
        }
    }

    //----------//
    // addVoice //
    //----------//
    /**
     * Add a voice into measure.
     *
     * @param voice the voice to add
     */
    public void addVoice (Voice voice)
    {
        voices.add(voice);
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
            final SIGraph sig = part.getSystem().getSig();

            // Clefs, keys, timeSigs to fill measure
            List<Inter> measureInters = filter(
                    sig.inters(
                            new Class[]{
                                ClefInter.class,
                                KeyInter.class,
                                AbstractTimeInter.class}));

            for (Inter inter : measureInters) {
                addInter(inter);
            }

            // BeamGroups
            for (BeamGroup beamGroup : beamGroups) {
                beamGroup.afterReload(this);
            }

            // Voices
            for (Voice voice : voices) {
                voice.afterReload(this);
            }

            // Chords
            for (AbstractChordInter chord : getHeadChords()) {
                chord.afterReload(this);
            }

            for (AbstractChordInter chord : getRestChords()) {
                chord.afterReload(this);
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //---------------//
    // checkDuration //
    //---------------//
    /**
     * Check the duration as computed in this measure from its contained voices,
     * compared to its theoretical duration.
     */
    public void checkDuration ()
    {
        // Check duration of each voice
        for (Voice voice : voices) {
            voice.checkDuration();
        }
    }

    //-----------------//
    // clearBeamGroups //
    //-----------------//
    /**
     * Reset collection of beam groups for this measure.
     */
    public void clearBeamGroups ()
    {
        beamGroups.clear();
    }

    //-------------//
    // clearVoices //
    //-------------//
    /**
     * Reset collection of voices for this measure.
     */
    public void clearVoices ()
    {
        voices.clear();
    }

    //--------//
    // filter //
    //--------//
    /**
     * Retrieve among the provided inters the ones contained in this measure.
     *
     * @param inters the provided inters
     * @return the contained inters
     */
    public List<Inter> filter (Collection<Inter> inters)
    {
        final int left = getLeft();
        final int right = getRight();
        final List<Inter> kept = new ArrayList<>();

        for (Inter inter : inters) {
            Point center = inter.getCenter();

            // Rough abscissa limits
            if ((center.x < left) || (center.x > right)) {
                continue;
            }

            // Check part
            Staff staff = inter.getStaff();

            if (staff != null) {
                if (staff.getPart().getMeasureAt(center) != this) {
                    continue;
                }
            } else {
                List<Staff> stavesAround = part.getSystem().getStavesAround(center); // 1 or 2 staves
                staff = stavesAround.get(0);
                logger.warn("Inter with no staff {}, assigned to staff#{}", inter, staff.getId());
                inter.setStaff(staff);

                if (!part.getStaves().contains(staff)) {
                    continue;
                }
            }

            // Precise abscissa limits
            if ((getAbscissa(LEFT, staff) <= center.x) && (center.x <= getAbscissa(RIGHT, staff))) {
                kept.add(inter);
            }
        }

        return kept;
    }

    //-----------------//
    // generateVoiceId //
    //-----------------//
    /**
     * Generate a new voice ID, based on voice family and current measure voices.
     *
     * @param family the voice family (HIGH, LOW, INFRA)
     * @return the generated ID, or -1 if none could be assigned.
     */
    public int generateVoiceId (Family family)
    {
        final int idOffset = family.idOffset();

        for (int id = idOffset + 1;; id++) {
            if (getVoiceById(id) == null) {
                return id;
            }
        }
    }

    //-------------//
    // getAbscissa //
    //-------------//
    /**
     * Report abscissa of desired measure side at ordinate of provided staff.
     * <p>
     * We consistently use the abscissa center of the right-most barline in
     * {@link StaffBarlineInter}.
     *
     * @param side  desired horizontal side
     * @param staff staff for ordinate
     * @return x value
     */
    public int getAbscissa (HorizontalSide side,
                            Staff staff)
    {
        Objects.requireNonNull(staff, "Null staff for Measure.getAbscissa()");

        switch (side) {
        case LEFT:

            // measure (left) bar?
            PartBarline leftBar = getPartBarlineOn(LEFT);

            if (leftBar != null) {
                return leftBar.getRightX(part, staff);
            }

            // Use start of staff
            return staff.getAbscissa(LEFT);

        default:
        case RIGHT:

            // Measure (right) bar?
            if (rightBarline != null) {
                return rightBarline.getRightX(part, staff);
            }

            // Use end of staff
            return staff.getAbscissa(RIGHT);
        }
    }

    //---------------------//
    // getAugmentationDots //
    //---------------------//
    /**
     * Report the augmentation dots in this measure.
     *
     * @return the augmentation dots in measure
     */
    public Set<AugmentationDotInter> getAugmentationDots ()
    {
        return Collections.unmodifiableSet(augDots);
    }

    //---------------//
    // getBeamGroups //
    //---------------//
    /**
     * Report the collection of beam groups.
     *
     * @return the set of beam groups
     */
    public Set<BeamGroup> getBeamGroups ()
    {
        return beamGroups;
    }

    //---------------//
    // getClefBefore //
    //---------------//
    /**
     * Report the first clef, if any, defined before this measure point
     * (looking in the beginning of the measure, then in previous measures in the same
     * system) while staying in the same physical staff.
     * <p>
     * NOTA: There is no point in looking before the current system, since any system staff is
     * required to start with a clef.
     *
     * @param point the point before which to look
     * @param staff the containing staff (cannot be null)
     * @return the latest clef defined, or null
     */
    public ClefInter getClefBefore (Point point,
                                    Staff staff)
    {
        // First, look in this measure, with same staff, going backwards
        ClefInter clef = getMeasureClefBefore(point, staff);

        if (clef != null) {
            return clef;
        }

        // Look in preceding measures, within the same system/part, within the same staff
        Measure measure = this;

        while ((measure = measure.getPrecedingInSystem()) != null) {
            clef = measure.getLastMeasureClef(staff);

            if (clef != null) {
                return clef;
            }
        }

        return null;
    }

    //----------//
    // getClefs //
    //----------//
    /**
     * @return the clefs
     */
    public List<ClefInter> getClefs ()
    {
        return Collections.unmodifiableList(clefs);
    }

    //--------------------------//
    // getContainedPartBarlines //
    //--------------------------//
    /**
     * Report the PartBarlines this measure <b>strictly contains</b>
     * (as opposed to {@link #getPartBarlineOn(HorizontalSide)})
     *
     * @return the list of contained PartBarlines, perhaps empty but not null
     * @see #getPartBarlineOn(HorizontalSide)
     */
    public List<PartBarline> getContainedPartBarlines ()
    {
        List<PartBarline> list = new ArrayList<>();

        if (leftBarline != null) {
            list.add(leftBarline);
        }

        if (midBarline != null) {
            list.add(midBarline);
        }

        if (rightBarline != null) {
            list.add(rightBarline);
        }

        return list;
    }

    //---------------------//
    // getFirstMeasureClef //
    //---------------------//
    /**
     * Report the first clef (if any) in this measure, if tagged with the specified
     * staff index
     *
     * @param staffIndexInPart the imposed part-based staff index
     * @return the first clef, or null
     */
    public ClefInter getFirstMeasureClef (int staffIndexInPart)
    {
        // Going forward
        for (ClefInter clef : clefs) {
            if (clef.getStaff().getIndexInPart() == staffIndexInPart) {
                return clef;
            }
        }

        return null;
    }

    //----------//
    // getFlags //
    //----------//
    /**
     * Report the flags in this measure.
     *
     * @return the flags in measure
     */
    public Set<FlagInter> getFlags ()
    {
        return Collections.unmodifiableSet(flags);
    }

    //---------------//
    // getHeadChords //
    //---------------//
    /**
     * Report the head chords in this measure.
     *
     * @return the measure head chords
     */
    public Set<HeadChordInter> getHeadChords ()
    {
        return Collections.unmodifiableSet(headChords);
    }

    //--------------------//
    // getHeadChordsAbove //
    //--------------------//
    /**
     * Report the collection of head-chords whose head is located in the staff above the
     * provided point.
     *
     * @param point the provided point
     * @return the (perhaps empty) collection of head chords
     */
    public Collection<HeadChordInter> getHeadChordsAbove (Point2D point)
    {
        Staff desiredStaff = stack.getSystem().getStaffAtOrAbove(point);
        Collection<HeadChordInter> found = new ArrayList<>();

        for (HeadChordInter chord : getHeadChords()) {
            if (chord.getBottomStaff() == desiredStaff) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y < point.getY())) {
                    found.add(chord);
                }
            }
        }

        return found;
    }

    //--------------------//
    // getHeadChordsBelow //
    //--------------------//
    /**
     * Report the collection of head-chords whose head is located in the staff below the
     * provided point.
     *
     * @param point the provided point
     * @return the (perhaps empty) collection of head chords
     */
    public Collection<HeadChordInter> getHeadChordsBelow (Point2D point)
    {
        Staff desiredStaff = stack.getSystem().getStaffAtOrBelow(point);
        Collection<HeadChordInter> found = new ArrayList<>();

        for (HeadChordInter chord : getHeadChords()) {
            if (chord.getTopStaff() == desiredStaff) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y > point.getY())) {
                    found.add(chord);
                }
            }
        }

        return found;
    }

    //--------//
    // getKey //
    //--------//
    /**
     * Report the potential key signature in this measure for the specified staff index
     * in part.
     *
     * @param staffIndexInPart staff index in part
     * @return the staff key signature, or null if not found
     */
    public KeyInter getKey (int staffIndexInPart)
    {
        for (KeyInter key : keys) {
            if (key.getStaff().getIndexInPart() == staffIndexInPart) {
                return key;
            }
        }

        return null;
    }

    //--------//
    // getKey //
    //--------//
    /**
     * Report the potential key signature in this measure for the specified staff.
     *
     * @param staff the desired staff
     * @return the staff key signature, or null if not found
     */
    public KeyInter getKey (Staff staff)
    {
        return getKey(staff.getIndexInPart());
    }

    //--------------//
    // getKeyBefore //
    //--------------//
    /**
     * Report the first key, if any, found at the beginning of this measure, then in
     * previous measures in the same system, while staying in the same physical staff.
     * <p>
     * NOTA: There is no point in looking before the current system, since any system staff is
     * required to start with a key or nothing.
     *
     * @param staff the containing staff (cannot be null)
     * @return the latest key defined, or null
     */
    public KeyInter getKeyBefore (Staff staff)
    {
        // Look in current & preceding measures, within the same system/part, within the same staff
        final int idx = staff.getIndexInPart();
        Measure measure = this;

        while (measure != null) {
            KeyInter key = measure.getKey(idx);

            if (key != null) {
                return key;
            }

            measure = measure.getPrecedingInSystem();
        }

        return null; // No key previously defined
    }

    //--------------------//
    // getLastMeasureClef //
    //--------------------//
    /**
     * Report the last clef (if any) in this measure, with the specified staff.
     *
     * @param staff the imposed staff
     * @return the last clef, or null
     */
    public ClefInter getLastMeasureClef (Staff staff)
    {
        // Going backwards
        for (ListIterator<ClefInter> lit = clefs.listIterator(clefs.size()); lit.hasPrevious();) {
            ClefInter clef = lit.previous();

            if (clef.getStaff() == staff) {
                return clef;
            }
        }

        return null;
    }

    //--------------------//
    // getLeftPartBarline //
    //--------------------//
    /**
     * Report the PartBarline, if any, on left.
     *
     * @return left PartBarline or null
     */
    public PartBarline getLeftPartBarline ()
    {
        return leftBarline;
    }

    //--------------------//
    // setLeftPartBarline //
    //--------------------//
    /**
     * Set the PartBarline on left.
     *
     * @param leftBarline left barline
     */
    public void setLeftPartBarline (PartBarline leftBarline)
    {
        this.leftBarline = leftBarline;
    }

    //----------------------//
    // getMeasureClefBefore //
    //----------------------//
    /**
     * Report the current clef, if any, defined within this measure and staff, and
     * located before this measure point.
     *
     * @param point the point before which to look
     * @param staff the containing staff (cannot be null)
     * @return the measure clef defined, or null
     */
    public ClefInter getMeasureClefBefore (Point point,
                                           Staff staff)
    {
        Objects.requireNonNull(point, "Point is null");
        Objects.requireNonNull(staff, "Staff is null");

        // Look in this measure, with same staff, going backwards
        for (ListIterator<ClefInter> lit = clefs.listIterator(clefs.size()); lit.hasPrevious();) {
            ClefInter clef = lit.previous();

            if ((clef.getStaff() == staff) && (clef.getCenter().x <= point.x)) {
                return clef;
            }
        }

        return null; // No clef previously defined in this measure and staff
    }

    //-------------------//
    // getMidPartBarline //
    //-------------------//
    /**
     * Report the mid barline, if any.
     *
     * @return the mid barline or null
     */
    public PartBarline getMidPartBarline ()
    {
        return midBarline;
    }

    //-------------------//
    // setMidPartBarline //
    //-------------------//
    /**
     * Set the middle PartBarline.
     *
     * @param midBarline mid barline
     */
    public void setMidPartBarline (PartBarline midBarline)
    {
        this.midBarline = midBarline;
    }

    //---------//
    // getPart //
    //---------//
    /**
     * Report the containing part.
     *
     * @return the part that contains this measure
     */
    public Part getPart ()
    {
        return part;
    }

    //------------------//
    // getPartBarlineOn //
    //------------------//
    /**
     * Report the PartBarline, if any, located on desired side of the measure
     * (<b>regardless</b> whether it strictly belongs to the measure or not,
     * as opposed to {@link #getContainedPartBarlines()}).
     *
     * @param side desired side
     * @return the PartBarline found, or null
     * @see #getContainedPartBarlines()
     */
    public PartBarline getPartBarlineOn (HorizontalSide side)
    {
        switch (side) {
        case LEFT:

            // Measure specific left bar?
            if (leftBarline != null) {
                return leftBarline;
            }

            // Previous measure in part?
            Measure prevMeasure = getSibling(LEFT);

            if (prevMeasure != null) {
                return prevMeasure.getRightPartBarline();
            }

            // Part starting bar?
            if (part.getLeftPartBarline() != null) {
                return part.getLeftPartBarline();
            }

            return null; // No barline found on LEFT

        default:
        case RIGHT:

            // Measure (right) bar?
            return rightBarline;
        }
    }

    //--------------------//
    // getPrecedingInPage //
    //--------------------//
    /**
     * Report the preceding measure of this one, either in this system / part, or in the
     * preceding system / part, but still in the same page.
     *
     * @return the preceding measure, or null if not found in the page
     */
    public Measure getPrecedingInPage ()
    {
        // Look in current part
        Measure prevMeasure = getPrecedingInSystem();

        if (prevMeasure != null) {
            return prevMeasure;
        }

        Part precedingPart = getPart().getPrecedingInPage();

        if (precedingPart != null) {
            return precedingPart.getLastMeasure();
        } else {
            return null;
        }
    }

    //----------------------//
    // getPrecedingInSystem //
    //----------------------//
    /**
     * Return the preceding measure within the same system.
     *
     * @return previous sibling measure in system, or null
     */
    public Measure getPrecedingInSystem ()
    {
        int index = part.getMeasures().indexOf(this);

        if (index > 0) {
            return part.getMeasures().get(index - 1);
        }

        return null;
    }

    //---------------//
    // getRestChords //
    //---------------//
    /**
     * Report the rest chords in this measure.
     *
     * @return all rest chords in this measure
     */
    public Set<RestChordInter> getRestChords ()
    {
        return Collections.unmodifiableSet(restChords);
    }

    //---------------------//
    // getRightPartBarline //
    //---------------------//
    /**
     * Report the right PartBarline, if any.
     *
     * @return the ending PartBarline or null
     */
    public PartBarline getRightPartBarline ()
    {
        return rightBarline;
    }

    //---------------------//
    // setRightPartBarline //
    //---------------------//
    /**
     * Assign the (right) PartBarline that ends this measure
     *
     * @param rightBarline the right PartBarline
     */
    public void setRightPartBarline (PartBarline rightBarline)
    {
        this.rightBarline = rightBarline;
    }

    //------------//
    // getSibling //
    //------------//
    /**
     * Report the sibling measure on the provided side.
     *
     * @param side horizontal side
     * @return sibling measure, or null if none
     */
    public Measure getSibling (HorizontalSide side)
    {
        final List<Measure> measures = part.getMeasures();
        int index = measures.indexOf(this);

        switch (side) {
        case LEFT:

            if (index > 0) {
                return measures.get(index - 1);
            }

            return null;

        default:
        case RIGHT:

            if (index < (measures.size() - 1)) {
                return measures.get(index + 1);
            }

            return null;
        }
    }

    //----------//
    // getPoint //
    //----------//
    /**
     * Report mid point of desired measure side at ordinate of provided staff
     *
     * @param side  desired horizontal side
     * @param staff staff for ordinate
     * @return mid point on desired side
     */
    public Point getSidePoint (HorizontalSide side,
                               Staff staff)
    {
        switch (side) {
        case LEFT:

            // Measure specific left bar?
            if (leftBarline != null) {
                return leftBarline.getStaffBarline(part, staff).getReferenceCenter();
            }

            // Previous measure in part?
            Measure prevMeasure = getSibling(LEFT);

            if (prevMeasure != null) {
                return prevMeasure.getSidePoint(RIGHT, staff);
            }

            // Part starting bar?
            if (part.getLeftPartBarline() != null) {
                return part.getLeftPartBarline().getStaffBarline(part, staff).getReferenceCenter();
            }

            // No bar, use start of staff
             {
                List<LineInfo> lines = staff.getLines();
                LineInfo midLine = lines.get(lines.size() / 2);
                int x = staff.getAbscissa(LEFT);

                return new Point(x, midLine.yAt(x));
            }

        default:
        case RIGHT:

            // Measure (right) bar?
            if (rightBarline != null) {
                return rightBarline.getStaffBarline(part, staff).getReferenceCenter();
            }

            // No bar, use end of staff
             {
                List<LineInfo> lines = staff.getLines();
                LineInfo midLine = lines.get(lines.size() / 2);
                int x = staff.getAbscissa(RIGHT);

                return new Point(x, midLine.yAt(x));
            }
        }
    }

    //----------//
    // getStack //
    //----------//
    /**
     * @return the stack
     */
    public MeasureStack getStack ()
    {
        return stack;
    }

    //----------//
    // setStack //
    //----------//
    /**
     * @param stack the stack to set
     */
    public void setStack (MeasureStack stack)
    {
        this.stack = stack;
    }

    //-------------------//
    // getStandardChords //
    //-------------------//
    /**
     * Report the collection of standard chords (head chords, rest chords) but not the
     * SmallChordInter instances.
     *
     * @return the set of all standard chords in this measure
     */
    public Set<AbstractChordInter> getStandardChords ()
    {
        final Set<AbstractChordInter> stdChords = new LinkedHashSet<>();
        stdChords.addAll(getHeadChords());
        stdChords.addAll(getRestChords());

        // Remove small head chords if any
        for (Iterator<AbstractChordInter> it = stdChords.iterator(); it.hasNext();) {
            if (it.next() instanceof SmallChordInter) {
                it.remove();
            }
        }

        return stdChords;
    }

    //-----------------------//
    // getStandardHeadChords //
    //-----------------------//
    /**
     * Report all standard (not small) head chords in this measure.
     *
     * @return all non-small head chords in measure
     */
    public Set<HeadChordInter> getStandardHeadChords ()
    {
        final Set<HeadChordInter> standardHeadChords = getHeadChords();

        for (Iterator<HeadChordInter> it = standardHeadChords.iterator(); it.hasNext();) {
            final HeadChordInter headChord = it.next();
            final List<Inter> notes = headChord.getMembers();

            if (notes.isEmpty() || notes.get(0).getShape().isSmall()) {
                it.remove();
            }
        }

        return standardHeadChords;
    }

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature in this measure (whatever the staff).
     *
     * @return the measure time signature, or null if not found
     */
    public AbstractTimeInter getTimeSignature ()
    {
        if (!timeSigs.isEmpty()) {
            return timeSigs.iterator().next();
        }

        return null; // Not found
    }

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature in this measure for the specified staff index.
     *
     * @param staffIndexInPart imposed part-based staff index
     * @return the staff time signature, or null if not found
     */
    public AbstractTimeInter getTimeSignature (int staffIndexInPart)
    {
        for (AbstractTimeInter ts : timeSigs) {
            final int index = part.getStaves().indexOf(ts.getStaff());

            if (index == staffIndexInPart) {
                return ts;
            }
        }

        return null; // Not found
    }

    //-----------------//
    // getTimingInters //
    //-----------------//
    /**
     * Report the set of measure inters involved in timing.
     *
     * @return chords, beams, flags, augmentation dots, tuplets
     */
    public Set<Inter> getTimingInters ()
    {
        Set<Inter> set = new LinkedHashSet<>();

        for (BeamGroup beamGroup : beamGroups) {
            set.addAll(beamGroup.getBeams());
        }

        set.addAll(getHeadChords());
        set.addAll(getFlags());
        set.addAll(getRestChords());
        set.addAll(getAugmentationDots());
        set.addAll(getTuplets());

        return set;
    }

    //------------//
    // getTuplets //
    //------------//
    /**
     * Report the tuplets in this measure.
     *
     * @return all tuplets in measure
     */
    public Set<TupletInter> getTuplets ()
    {
        return Collections.unmodifiableSet(tuplets);
    }

    //-----------//
    // getVoices //
    //-----------//
    /**
     * Report the sequence of voices in this measure.
     *
     * @return sequence of voices
     */
    public List<Voice> getVoices ()
    {
        return Collections.unmodifiableList(voices);
    }

    //--------------------//
    // getWholeRestChords //
    //--------------------//
    /**
     * Report all whole rest-chords in measure.
     *
     * @return all whole rest chords in measure
     */
    public Set<AbstractChordInter> getWholeRestChords ()
    {
        final Set<AbstractChordInter> set = new LinkedHashSet<>();

        for (RestChordInter chord : getRestChords()) {
            final List<Inter> members = chord.getMembers();

            if (!members.isEmpty() && (members.get(0).getShape() == Shape.WHOLE_REST)) {
                set.add(chord);
            }
        }

        return set;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the measure width
     *
     * @return measure width (at first staff in measure part)
     */
    public int getWidth ()
    {
        final Staff firstStaff = part.getFirstStaff();
        final int left = getAbscissa(LEFT, firstStaff);
        final int right = getAbscissa(RIGHT, firstStaff);

        return right - left;
    }

    //---------//
    // hasKeys //
    //---------//
    /**
     * Report whether there is at least one key signature, whatever the staff, in this
     * measure.
     *
     * @return true if one key was found
     */
    public boolean hasKeys ()
    {
        return !keys.isEmpty();
    }

    //-------------//
    // hasSameKeys //
    //-------------//
    /**
     * Report whether all key signatures, whatever the staff, are the same.
     *
     * @return true if identical
     */
    public boolean hasSameKeys ()
    {
        if (!hasKeys()) {
            return true;
        }

        final int staffCount = part.getStaves().size();
        Integer prevFifths = null;

        for (int index = 0; index < staffCount; index++) {
            KeyInter key = getKey(index);

            if (key == null) {
                return false;
            }

            if ((prevFifths != null) && !prevFifths.equals(key.getFifths())) {
                return false;
            }

            prevFifths = key.getFifths();
        }

        return true;
    }

    //------------------//
    // inferVoiceFamily //
    //------------------//
    /**
     * Infer the voice family for a voice started by the provided chord.
     *
     * @param chord the provided chord (assumed to be the first in voice)
     * @return the inferred voice family
     */
    public Family inferVoiceFamily (AbstractChordInter chord)
    {
        final Staff startingStaff = chord.getTopStaff();

        if (part.isMerged()) {
            switch (chord.getStemDir()) {
            case -1:
                return Family.HIGH;
            case +1:
                return Family.LOW;
            default:
                return (startingStaff == part.getFirstStaff()) ? Family.HIGH : Family.LOW;
            }
        } else {
            int index = part.getStaves().indexOf(startingStaff);

            if (index >= 0 && index < Family.values().length) {
                return Family.values()[index];
            }

            logger.error("{} Weird staff index {} in part", startingStaff, index);
            return Family.HIGH;
        }
    }

    //------------//
    // isAbnormal //
    //------------//
    /**
     * Report whether this measure is abnormal.
     *
     * @return the abnormal status
     */
    public boolean isAbnormal ()
    {
        return abnormal;
    }

    //-------------//
    // setAbnormal //
    //-------------//
    /**
     * Mark this measure as being abnormal or not.
     *
     * @param abnormal new value
     */
    public void setAbnormal (boolean abnormal)
    {
        this.abnormal = abnormal;
    }

    //---------//
    // isDummy //
    //---------//
    /**
     * Tell whether this measure is dummy (in a dummy part).
     *
     * @return true if so
     */
    public boolean isDummy ()
    {
        return dummy;
    }

    //----------//
    // setDummy //
    //----------//
    /**
     * Flag this measure as dummy.
     */
    public void setDummy ()
    {
        dummy = true;
    }

    //---------------//
    // isMeasureRest //
    //---------------//
    /**
     * Check whether the provided rest chord is a measure rest.
     *
     * @param restChord the provided rest chord
     * @return true if rest chord is actually a measure rest, false otherwise
     */
    public boolean isMeasureRest (RestChordInter restChord)
    {
        Inter noteInter = restChord.getMembers().get(0);
        Shape shape = noteInter.getShape();

        if (!shape.isWholeRest()) {
            return false;
        }

        if ((shape == Shape.BREVE_REST) || (shape == Shape.LONG_REST)) {
            return true;
        }

        // Here we have a WHOLE_REST shape
        RestInter rest = (RestInter) noteInter;

        // Check pitch?
        int pitch2 = (int) Math.rint(2.0 * rest.getPitch());

        if (pitch2 != -3) {
            return false;
        }

        // Check other chords in same staff-measure?
        Set<Inter> staffChords = filterByStaff(getStandardChords(), restChord.getTopStaff());

        return staffChords.size() == 1;
    }

    //----------------//
    // mergeWithBelow //
    //----------------//
    /**
     * Merge this measure with the content of the measure just below.
     *
     * @param measureBelow the measure below
     */
    public void mergeWithBelow (Measure measureBelow)
    {
        // Barlines
        if (measureBelow.leftBarline != null) {
            if (leftBarline == null) {
                leftBarline = measureBelow.leftBarline;
            } else {
                leftBarline.mergeWithBelow(measureBelow.leftBarline);
            }
        }

        if (measureBelow.midBarline != null) {
            if (midBarline == null) {
                midBarline = measureBelow.midBarline;
            } else {
                midBarline.mergeWithBelow(measureBelow.midBarline);
            }
        }

        if (measureBelow.rightBarline != null) {
            if (rightBarline == null) {
                rightBarline = measureBelow.rightBarline;
            } else {
                rightBarline.mergeWithBelow(measureBelow.rightBarline);
            }
        }

        // Keys
        keys.addAll(measureBelow.keys);

        mergeWithOther(measureBelow);

        // Voices: rhythm reprocessed
        //
        measureBelow.switchItemsPart(part);
    }

    //----------------//
    // mergeWithRight //
    //----------------//
    /**
     * Merge this measure with the content of the following measure on the right.
     *
     * @param right the following measure
     */
    public void mergeWithRight (Measure right)
    {
        // Barlines
        if (midBarline == null) {
            midBarline = rightBarline;
        }

        setRightPartBarline(right.rightBarline);

        // Keys
        if (right.hasKeys()) {
            if (hasKeys()) {
                logger.warn("Attempt to merge keySigs from 2 measures {} and {}", this, right);
            } else {
                keys.addAll(right.keys);
            }
        }

        mergeWithOther(right);

        // Voices: when merging voidStart <- realStart
        voices.addAll(right.voices);

        // Voices: when removing a barline, rhythm is reprocessed, all voices recreated from scratch
    }

    //-----------------//
    // removeBeamGroup //
    //-----------------//
    /**
     * Remove the provided beamGroup from this measure.
     *
     * @param beamGroup the beam group to remove
     */
    public void removeBeamGroup (BeamGroup beamGroup)
    {
        beamGroups.remove(beamGroup);
    }

    //-------------//
    // removeInter //
    //-------------//
    /**
     * Remove the provided inter from measure internals.
     *
     * @param inter the inter to remove
     */
    public void removeInter (Inter inter)
    {
        if (inter.isVip()) {
            logger.info("VIP removeInter {} from {}", inter, this);
        }

        if (inter instanceof ClefInter) {
            clefs.remove((ClefInter) inter);
        } else if (inter instanceof KeyInter) {
            keys.remove((KeyInter) inter);
        } else if (inter instanceof AbstractTimeInter) {
            timeSigs.remove((AbstractTimeInter) inter);
        } else if (inter instanceof HeadChordInter) {
            headChords.remove((HeadChordInter) inter);
        } else if (inter instanceof RestChordInter) {
            restChords.remove((RestChordInter) inter);
        } else if (inter instanceof FlagInter) {
            flags.remove((FlagInter) inter);
        } else if (inter instanceof TupletInter) {
            tuplets.remove((TupletInter) inter);
        } else if (inter instanceof AugmentationDotInter) {
            augDots.remove((AugmentationDotInter) inter);
        } else {
            logger.error("Attempt to use removeInter() with {}", inter);
        }
    }

    //--------------//
    // removeVoices //
    //--------------//
    /**
     * Remove the provided voices.
     *
     * @param toRemove the voices to remove
     */
    public void removeVoices (Collection<Voice> toRemove)
    {
        voices.removeAll(toRemove);
    }

    //--------------//
    // renameVoices //
    //--------------//
    /**
     * Adjust voice ID per family, in line with their order.
     */
    public void renameVoices ()
    {
        for (Family family : Family.values()) {
            int id = family.idOffset();

            for (int i = 0; i < voices.size(); i++) {
                final Voice voice = voices.get(i);

                if (voice.getFamily() == family) {
                    voice.setId(++id);
                }
            }
        }
    }

    //------------//
    // renderArea //
    //------------//
    /**
     * Render the measure area with provided color.
     *
     * @param g     graphics context
     * @param color provided color
     */
    public void renderArea (Graphics2D g,
                            Color color)
    {
        g.setColor(color);

        final int left = getLeft();
        final int right = getRight();

        final LineInfo firstLine = part.getFirstStaff().getFirstLine();
        int top = Math.min(firstLine.yAt(left), firstLine.yAt(right));

        final LineInfo lastLine = part.getLastStaff().getLastLine();
        int bottom = Math.max(lastLine.yAt(left), lastLine.yAt(right));

        // Most timing inters from measure
        for (Inter inter : getTimingInters()) {
            Rectangle bounds = inter.getBounds();

            if (bounds != null) {
                top = Math.min(top, bounds.y);
                bottom = Math.max(bottom, bounds.y + bounds.height);
            }
        }

        g.fill(new Rectangle(left, top, right - left + 1, bottom - top + 1));
    }

    //-----------//
    // replicate //
    //-----------//
    /**
     * Replicate this measure in a target part
     *
     * @param targetPart the target part
     * @return the replicate
     */
    public Measure replicate (Part targetPart)
    {
        Measure replicate = new Measure(targetPart);

        return replicate;
    }

    //-------------//
    // resetRhythm //
    //-------------//
    /**
     * Reset rhythm information in this measure (voices, beam groups, chords).
     */
    public void resetRhythm ()
    {
        setAbnormal(false);
        voices.clear();

        // Reset voice of every beam group
        for (BeamGroup group : beamGroups) {
            group.resetTiming();
        }

        // Forward reset to every chord in measure (standard and small)
        for (AbstractChordInter chord : getHeadChords()) {
            chord.resetTiming();
        }

        for (AbstractChordInter chord : getRestChords()) {
            chord.resetTiming();
        }
    }

    //--------------//
    // setCueVoices //
    //--------------//
    /**
     * Voice of every (standard) head chord is extended to its related preceding cue
     * chord(s) if any.
     */
    public void setCueVoices ()
    {
        for (HeadChordInter ch : headChords) {
            if (!(ch instanceof SmallChordInter)) {
                SmallChordInter small = ch.getGraceChord();

                if (small != null) {
                    final Voice voice = ch.getVoice();

                    if (voice != null) {
                        small.setVoice(voice);

                    }
                }
            }
        }
    }

    //------------//
    // sortVoices //
    //------------//
    /**
     * Sort measure voices.
     */
    public void sortVoices ()
    {
        Collections.sort(voices, Voices.byOrdinate);
    }

    //---------//
    // splitAt //
    //---------//
    /**
     * Split this measure at provided abscissae.
     * This is triggered by a barline insertion.
     *
     * @param xRefs split abscissa for each staff
     * @return the populated left new measure, the old (right) measure being half-purged
     */
    public Measure splitAt (Map<Staff, Integer> xRefs)
    {
        final Measure leftMeasure = new Measure(part);
        // part: assigned to leftmeasure
        // stack: assigned by caller

        // left barline ???
        // mid barline ???
        // right barline: assigned by caller
        //
        // Beam groups
        for (Iterator<BeamGroup> it = beamGroups.iterator(); it.hasNext();) {
            BeamGroup beamGroup = it.next();
            AbstractChordInter chord = beamGroup.getFirstChord();

            if (chord.getCenter().x <= xRefs.get(chord.getTopStaff())) {
                leftMeasure.addBeamGroup(beamGroup);
                it.remove();
            }
        }

        splitCollectionAt(xRefs, leftMeasure, clefs);

        // Keys
        leftMeasure.keys.addAll(keys);
        keys.clear();

        // Times
        leftMeasure.timeSigs.addAll(timeSigs);
        timeSigs.clear();

        splitCollectionAt(xRefs, leftMeasure, headChords);
        splitCollectionAt(xRefs, leftMeasure, restChords);
        splitCollectionAt(xRefs, leftMeasure, flags);
        splitCollectionAt(xRefs, leftMeasure, tuplets);
        splitCollectionAt(xRefs, leftMeasure, augDots);

        // voices: when a measure is split, rhythm is reprocessed, all voices recreated from scratch
        //
        return leftMeasure;
    }

    //-------------//
    // splitBefore //
    //-------------//
    /**
     * Split this measure before the provided staff.
     *
     * @param pivotStaff the provided staff
     * @param partBelow  the new part below, to be populated with lower half of measure
     */
    public void splitBefore (Staff pivotStaff,
                             Part partBelow)
    {
        final Measure measureBelow = new Measure(partBelow);
        final List<Staff> stavesBelow = partBelow.getStaves();

        // Barlines
        if (leftBarline != null) {
            measureBelow.leftBarline = leftBarline.splitBefore(pivotStaff);
        }
        if (midBarline != null) {
            measureBelow.midBarline = midBarline.splitBefore(pivotStaff);
        }
        if (rightBarline != null) {
            measureBelow.rightBarline = rightBarline.splitBefore(pivotStaff);
        }

        // Beam groups
        BeamGroupLoop:
        for (BeamGroup bg : beamGroups) {
            final Set<AbstractBeamInter> beams = bg.getBeams();

            for (AbstractBeamInter beam : beams) {
                // A beam may have no staff, with notes in upper staff and notes in lower staff
                for (AbstractChordInter chord : beam.getChords()) {
                    final Staff staff = chord.getBottomStaff();

                    if (stavesBelow.contains(staff)) {
                        measureBelow.beamGroups.add(bg);
                        continue BeamGroupLoop;
                    }
                }
            }

            bg.setMeasure(measureBelow);
        }
        beamGroups.removeAll(measureBelow.beamGroups);

        splitCollectionBefore(stavesBelow, measureBelow, keys);
        splitCollectionBefore(stavesBelow, measureBelow, clefs);
        Collections.sort(measureBelow.clefs, Inters.byFullCenterAbscissa); // Useful???
        splitCollectionBefore(stavesBelow, measureBelow, timeSigs);
        splitCollectionBefore(stavesBelow, measureBelow, headChords);
        splitCollectionBefore(stavesBelow, measureBelow, restChords);
        splitCollectionBefore(stavesBelow, measureBelow, flags);
        splitCollectionBefore(stavesBelow, measureBelow, tuplets);
        splitCollectionBefore(stavesBelow, measureBelow, augDots);

        // Voices: rhythm is reprocessed when part is vertically split (brace removal)
        //
        measureBelow.switchItemsPart(partBelow);
        partBelow.addMeasure(measureBelow);
        measureBelow.setStack(stack);
    }

    //-------------//
    // swapVoiceId //
    //-------------//
    /**
     * Change the id of the provided voice to the provided id
     * (and change the other voice, if any, which owned the provided id).
     *
     * @param voice the voice whose id must be changed
     * @param id    the new id
     * @return the old voice owner of id, if any
     */
    public Voice swapVoiceId (Voice voice,
                              int id)
    {
        // Existing voice?
        Voice oldOwner = null;

        for (Voice v : getVoices()) {
            if (v.getId() == id) {
                oldOwner = v;

                break;
            }
        }

        // Change voice id
        int oldId = voice.getId();
        voice.setId(id);

        // Assign the oldId to the oldOwner, if any
        if (oldOwner != null) {
            oldOwner.setId(oldId);
        }

        return oldOwner;
    }

    //-----------------//
    // switchItemsPart //
    //-----------------//
    /**
     * Assign the provided newPart to each item pointing to a different non-null part.
     *
     * @param newPart the provided newPart
     */
    public void switchItemsPart (Part newPart)
    {
        //TODO 3 barlines ???
        // beamGroups: no part field
        //
        switchCollectionPart(newPart, clefs);
        switchCollectionPart(newPart, keys);
        switchCollectionPart(newPart, timeSigs);
        switchCollectionPart(newPart, headChords);
        switchCollectionPart(newPart, restChords);
        switchCollectionPart(newPart, flags);
        switchCollectionPart(newPart, tuplets);
        switchCollectionPart(newPart, augDots);
        // voices: no part field
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Measure{");

        if (stack != null) {
            sb.append('#').append(stack.getPageId());
        } else {
            sb.append("-NOSTACK-");
        }

        if (part != null) {
            sb.append("P").append(part.getId());
        } else {
            sb.append("-NOPART-");
        }

        sb.append('}');

        return sb.toString();
    }

    //--------------------//
    // addOtherCollection //
    //--------------------//
    private void addOtherCollection (Collection<? extends Inter> otherCollection)
    {
        for (Inter inter : otherCollection) {
            addInter(inter);
        }
    }

    //--------------//
    // afterMarshal //
    //--------------//
    @SuppressWarnings("unused")
    private void afterMarshal (Marshaller m)
    {
        try {
            Trimmable.afterMarshal(this);
        } catch (Exception ex) {
            logger.error("Error afterMarshal {}", ex);
        }
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this object,
     * but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        part = (Part) parent;
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        try {
            Trimmable.beforeMarshal(this);
        } catch (Exception ex) {
            logger.error("Error beforeMarshal {}", ex);
        }
    }

    //---------------//
    // filterByStaff //
    //---------------//
    /**
     * Filter the inters that relate to the provided staff.
     *
     * @param inters the input collection of inters
     * @param staff  the imposed staff
     * @return the inters that related to staff
     */
    private Set<Inter> filterByStaff (Set<? extends Inter> inters,
                                      Staff staff)
    {
        Set<Inter> found = new LinkedHashSet<>();

        for (Inter inter : inters) {
            if (inter.getStaff() == staff) {
                found.add(inter);
            }
        }

        return found;
    }

    //---------//
    // getLeft //
    //---------//
    private int getLeft ()
    {
        int left = Integer.MAX_VALUE;

        for (Staff staff : getPart().getStaves()) {
            left = Math.min(left, getAbscissa(LEFT, staff));
        }

        return left;
    }

    //-----------//
    // getPageId //
    //-----------//
    /**
     * Report the measure ID within page (in fact the related stack ID).
     * <p>
     * NOTA: @XmlAttribute annotation forces this information to be written in book file
     * (although it is not used when unmarshalling)
     *
     * @return the page ID of containing stack
     */
    @XmlAttribute(name = "id")
    @SuppressWarnings("unused")
    private String getPageId ()
    {
        if (stack != null) {
            return stack.getPageId();
        }

        return null;
    }

    //----------//
    // getRight //
    //----------//
    private int getRight ()
    {
        int right = 0;

        for (Staff staff : getPart().getStaves()) {
            right = Math.max(right, getAbscissa(RIGHT, staff));
        }

        return right;
    }

    //--------------//
    // getVoiceById //
    //--------------//
    private Voice getVoiceById (int id)
    {
        for (Voice voice : voices) {
            if (voice.getId() == id) {
                return voice;
            }
        }

        return null;
    }

    //----------------//
    // mergeWithOther //
    //----------------//
    /**
     * Merge this measure with the content of the provided other measure.
     *
     * @param other the other measure
     */
    private void mergeWithOther (Measure other)
    {
        // Beam groups
        beamGroups.addAll(other.beamGroups);

        for (BeamGroup bg : other.beamGroups) {
            bg.setMeasure(this);
        }

        // Keys: addressed by caller
        //
        addOtherCollection(other.clefs);
        addOtherCollection(other.timeSigs);
        addOtherCollection(other.headChords);
        addOtherCollection(other.restChords);
        addOtherCollection(other.flags);
        addOtherCollection(other.tuplets);
        addOtherCollection(other.augDots);

        // Voices: addressed by caller
    }

    //-------------------//
    // splitCollectionAt //
    //-------------------//
    private void splitCollectionAt (Map<Staff, Integer> xRefs,
                                    Measure leftMeasure,
                                    Collection<? extends Inter> collection)
    {
        for (Iterator<? extends Inter> it = collection.iterator(); it.hasNext();) {
            final Inter inter = it.next();
            Staff staff = inter.getStaff();

            if ((staff == null) && (inter instanceof AbstractChordInter)) {
                staff = ((AbstractChordInter) inter).getTopStaff();
            }

            if (inter.getCenter().x <= xRefs.get(staff)) {
                leftMeasure.addInter(inter);
                it.remove();
            }
        }
    }

    //-----------------------//
    // splitCollectionBefore //
    //-----------------------//
    private void splitCollectionBefore (List<Staff> stavesBelow,
                                        Measure measureBelow,
                                        Collection<? extends Inter> collection)
    {
        for (Iterator<? extends Inter> it = collection.iterator(); it.hasNext();) {
            final Inter inter = it.next();
            Staff staff = inter.getStaff();

            if ((staff == null) && (inter instanceof AbstractChordInter)) {
                staff = ((AbstractChordInter) inter).getBottomStaff();
            }

            if (stavesBelow.contains(staff)) {
                measureBelow.addInter(inter);
                it.remove();
            }
        }
    }

    //----------------------//
    // switchCollectionPart //
    //----------------------//
    private void switchCollectionPart (Part newPart,
                                       Collection<? extends Inter> collection)
    {
        for (Inter item : collection) {
            if (item.getSpecificPart() != null && item.getSpecificPart() != newPart) {
                item.setPart(newPart);
            }
        }
    }
}
