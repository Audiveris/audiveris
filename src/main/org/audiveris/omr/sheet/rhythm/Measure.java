//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          M e a s u r e                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.sig.SIGraph;
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
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Measure} represents a measure in a system part, it vertically embraces
 * all the staves (usually 1 or 2) of the containing part.
 *
 * @see MeasureStack
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "measure")
public class Measure
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            Measure.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
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
    private final Set<BeamGroup> beamGroups = new LinkedHashSet<BeamGroup>();

    /** Possibly several Clefs per staff.
     * Implemented as a list, kept ordered by clef full abscissa */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "clefs")
    private List<ClefInter> clefs;

    /** Possibly one Key signature per staff, since keys may differ between staves. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "keys")
    private Set<KeyInter> keys;

    /** Possibly one Time signature per staff. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "times")
    private Set<AbstractTimeInter> timeSigs;

    /** Head chords. Populated by CHORDS step. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "head-chords")
    private Set<HeadChordInter> headChords;

    /** Rest chords. Populated by RHYTHMS step. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "rest-chords")
    private Set<RestChordInter> restChords;

    /** Flags. Populated by SYMBOLS step. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "flags")
    private Set<FlagInter> flags;

    /** Tuplets. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "tuplets")
    private Set<TupletInter> tuplets;

    /** Augmentation dots. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "augmentations-dots")
    private Set<AugmentationDotInter> augDots;

    /** Voices within this measure, sorted by voice id. Populated by RHYTHMS step. */
    @XmlElement(name = "voice")
    private final List<Voice> voices = new ArrayList<Voice>();

    // Transient data
    //---------------
    //
    /** To flag a dummy measure. */
    private boolean dummy;

    /** The containing part. */
    @Navigable(false)
    private Part part;

    /** The containing measure stack. */
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

            public FakeRest (Staff staff)
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

        if (inter instanceof AbstractChordInter) {
            AbstractChordInter chord = (AbstractChordInter) inter;
            chord.setMeasure(this);

            if (chord instanceof HeadChordInter) {
                needHeadChords().add((HeadChordInter) chord);
            } else if (chord instanceof RestChordInter) {
                needRestChords().add((RestChordInter) chord);
            }
        } else if (inter instanceof ClefInter) {
            final ClefInter clef = (ClefInter) inter;

            if (clefs == null) {
                clefs = new ArrayList<ClefInter>();
            }

            if (!clefs.contains(clef)) {
                clefs.add(clef);
                Collections.sort(clefs, Inters.byFullCenterAbscissa);
            }
        } else if (inter instanceof KeyInter) {
            final KeyInter key = (KeyInter) inter;

            if (keys == null) {
                keys = new LinkedHashSet<KeyInter>();
            }

            keys.add(key);
        } else if (inter instanceof AbstractTimeInter) {
            final AbstractTimeInter time = (AbstractTimeInter) inter;

            if (timeSigs == null) {
                timeSigs = new LinkedHashSet<AbstractTimeInter>();
            }

            timeSigs.add(time);
        } else if (inter instanceof FlagInter) {
            if (flags == null) {
                flags = new LinkedHashSet<FlagInter>();
            }

            flags.add((FlagInter) inter);
        } else if (inter instanceof AugmentationDotInter) {
            if (augDots == null) {
                augDots = new LinkedHashSet<AugmentationDotInter>();
            }

            augDots.add((AugmentationDotInter) inter);
        } else if (inter instanceof TupletInter) {
            if (tuplets == null) {
                tuplets = new LinkedHashSet<TupletInter>();
            }

            tuplets.add((TupletInter) inter);
        } else {
            logger.error("Attempt to use addInter() with {}", inter);
        }
    }

    //----------//
    // addVoice //
    //----------//
    public void addVoice (Voice voice)
    {
        voices.add(voice);
    }

    //-------------//
    // afterReload //
    //-------------//
    public void afterReload ()
    {
        try {
            final SIGraph sig = part.getSystem().getSig();

            // Clefs, keys, timeSigs to fill measure
            List<Inter> measureInters = filter(
                    sig.inters(
                            new Class[]{ClefInter.class, KeyInter.class, AbstractTimeInter.class}));

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

    //-----------------//
    // clearBeamGroups //
    //-----------------//
    public void clearBeamGroups ()
    {
        beamGroups.clear();
    }

    //------------//
    // clearFrats //
    //------------//
    public void clearFrats ()
    {
        flags = null;
        restChords = null;
        augDots = null;
        tuplets = null;
    }

    //--------//
    // filter //
    //--------//
    public List<Inter> filter (Collection<Inter> inters)
    {
        final int left = getLeft();
        final int right = getRight();
        final List<Inter> kept = new ArrayList<Inter>();

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

    //-------------//
    // getAbscissa //
    //-------------//
    /**
     * Report abscissa of desired measure side at ordinate of provided staff
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
            PartBarline leftBar = getBarline(LEFT);

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
    public Set<AugmentationDotInter> getAugmentationDots ()
    {
        return (augDots != null) ? Collections.unmodifiableSet(augDots) : Collections.EMPTY_SET;
    }

    //------------//
    // getBarline //
    //------------//
    /**
     * Report the barline, if any, on desired side of the measure.
     *
     * @param side desired side
     * @return the barline found, or null
     */
    public PartBarline getBarline (HorizontalSide side)
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
                return prevMeasure.getRightBarline();
            }

            // Part starting bar?
            if (part.getLeftBarline() != null) {
                return part.getLeftBarline();
            }

            return null; // No barline found on LEFT

        default:
        case RIGHT:

            // Measure (right) bar?
            if (rightBarline != null) {
                return rightBarline;
            }

            return null; // No barline found on RIGHT
        }
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

        return null; // No clef previously defined
    }

    //----------//
    // getClefs //
    //----------//
    /**
     * @return the clefs
     */
    public List<ClefInter> getClefs ()
    {
        return (clefs != null) ? Collections.unmodifiableList(clefs) : Collections.EMPTY_LIST;
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
        if (clefs != null) {
            for (ClefInter clef : clefs) {
                if (clef.getStaff().getIndexInPart() == staffIndexInPart) {
                    return clef;
                }
            }
        }

        return null;
    }

    //----------//
    // getFlags //
    //----------//
    public Set<FlagInter> getFlags ()
    {
        return (flags != null) ? Collections.unmodifiableSet(flags) : Collections.EMPTY_SET;
    }

    //---------------//
    // getHeadChords //
    //---------------//
    public Set<HeadChordInter> getHeadChords ()
    {
        return (headChords != null) ? Collections.unmodifiableSet(headChords) : Collections.EMPTY_SET;
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
    public Collection<HeadChordInter> getHeadChordsAbove (Point point)
    {
        Staff desiredStaff = stack.getSystem().getStaffAtOrAbove(point);
        Collection<HeadChordInter> found = new ArrayList<HeadChordInter>();

        for (HeadChordInter chord : getHeadChords()) {
            if (chord.getBottomStaff() == desiredStaff) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y < point.y)) {
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
        if (keys != null) {
            for (KeyInter key : keys) {
                if (key.getStaff().getIndexInPart() == staffIndexInPart) {
                    return key;
                }
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
        if (clefs != null) {
            for (ListIterator<ClefInter> lit = clefs.listIterator(clefs.size()); lit.hasPrevious();) {
                ClefInter clef = lit.previous();

                if (clef.getStaff() == staff) {
                    return clef;
                }
            }
        }

        return null;
    }

    //----------------//
    // getLeftBarline //
    //----------------//
    public PartBarline getLeftBarline ()
    {
        return leftBarline;
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
        Objects.requireNonNull(staff, "Staff is null");

        // Look in this measure, with same staff, going backwards
        if (clefs != null) {
            for (ListIterator<ClefInter> lit = clefs.listIterator(clefs.size()); lit.hasPrevious();) {
                ClefInter clef = lit.previous();

                if ((clef.getStaff() == staff) && (clef.getCenter().x <= point.x)) {
                    return clef;
                }
            }
        }

        return null; // No clef previously defined in this measure and staff
    }

    //---------------//
    // getMidBarline //
    //---------------//
    /**
     * Report the mid barline, if any.
     *
     * @return the mid barline or null
     */
    public PartBarline getMidBarline ()
    {
        return midBarline;
    }

    //---------//
    // getPart //
    //---------//
    public Part getPart ()
    {
        return part;
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
    public Set<RestChordInter> getRestChords ()
    {
        return (restChords != null) ? Collections.unmodifiableSet(restChords) : Collections.EMPTY_SET;
    }

    //-----------------//
    // getRightBarline //
    //-----------------//
    /**
     * Report the ending barline.
     *
     * @return the ending barline
     */
    public PartBarline getRightBarline ()
    {
        return rightBarline;
    }

    //------------//
    // getSibling //
    //------------//
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
                return leftBarline.getBarline(part, staff).getRightBar().getCenter();
            }

            // Previous measure in part?
            Measure prevMeasure = getSibling(LEFT);

            if (prevMeasure != null) {
                return prevMeasure.getSidePoint(RIGHT, staff);
            }

            // Part starting bar?
            if (part.getLeftBarline() != null) {
                return part.getLeftBarline().getBarline(part, staff).getRightBar().getCenter();
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
                return rightBarline.getBarline(part, staff).getRightBar().getCenter();
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

    //-------------------//
    // getStandardChords //
    //-------------------//
    /**
     * Report the collection of standard chords (head chords, rest chords)
     *
     * @return the set of all standard chords in this measure
     */
    public Set<AbstractChordInter> getStandardChords ()
    {
        final Set<AbstractChordInter> stdChords = new LinkedHashSet<AbstractChordInter>();
        stdChords.addAll(getHeadChords());
        stdChords.addAll(getRestChords());

        return stdChords;
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
        if ((timeSigs != null) && !timeSigs.isEmpty()) {
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
        if (timeSigs != null) {
            for (AbstractTimeInter ts : timeSigs) {
                final int index = part.getStaves().indexOf(ts.getStaff());

                if (index == staffIndexInPart) {
                    return ts;
                }
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
        Set<Inter> set = new LinkedHashSet<Inter>();

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
    public Set<TupletInter> getTuplets ()
    {
        return (tuplets != null) ? Collections.unmodifiableSet(tuplets) : Collections.EMPTY_SET;
    }

    //---------------//
    // getVoiceCount //
    //---------------//
    /**
     * Report the number of voices in this measure.
     *
     * @return the number of voices computed
     */
    public int getVoiceCount ()
    {
        return voices.size();
    }

    //-----------//
    // getVoices //
    //-----------//
    public List<Voice> getVoices ()
    {
        return Collections.unmodifiableList(voices);
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
        return (keys != null) && !keys.isEmpty();
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

    //---------//
    // isDummy //
    //---------//
    public boolean isDummy ()
    {
        return dummy;
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

    //------------//
    // lookupRest //
    //------------//
    /**
     * Look up for a potential rest interleaved between the given stemmed chords
     *
     * @param left  the chord on the left of the area
     * @param right the chord on the right of the area
     * @return the rest found, or null otherwise
     */
    public RestInter lookupRest (AbstractChordInter left,
                                 AbstractChordInter right)
    {
        // Define the area limited by the left and right chords with their stems
        // and check for intersection with a rest note
        Polygon polygon = new Polygon();
        polygon.addPoint(left.getHeadLocation().x, left.getHeadLocation().y);
        polygon.addPoint(left.getTailLocation().x, left.getTailLocation().y);
        polygon.addPoint(right.getTailLocation().x, right.getTailLocation().y);
        polygon.addPoint(right.getHeadLocation().x, right.getHeadLocation().y);

        for (RestChordInter restChord : getRestChords()) {
            for (Inter inter : restChord.getMembers()) {
                Rectangle box = inter.getBounds();

                if (polygon.intersects(box.x, box.y, box.width, box.height)) {
                    return (RestInter) inter;
                }
            }
        }

        return null;
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
        if ((right.clefs != null) && !right.clefs.isEmpty()) {
            if (clefs == null) {
                clefs = new ArrayList<ClefInter>();
            }

            clefs.removeAll(right.clefs); // Just in cases
            clefs.addAll(right.clefs);
            Collections.sort(clefs, Inters.byFullCenterAbscissa);
        }

        if (right.hasKeys()) {
            if (hasKeys()) {
                logger.warn("Attempt to merge keySigs from 2 measures {} and {}", this, right);
            } else {
                keys = right.keys;
            }
        }

        if (right.timeSigs != null) {
            if (timeSigs == null) {
                timeSigs = new LinkedHashSet<AbstractTimeInter>();
            }

            timeSigs.addAll(right.timeSigs);
        }

        if (!right.getRestChords().isEmpty()) {
            needRestChords().addAll(right.getRestChords());
        }

        if (!right.getHeadChords().isEmpty()) {
            needHeadChords().addAll(right.getHeadChords());
        }

        voices.addAll(right.voices); // (left) voices are empty
        beamGroups.addAll(right.beamGroups);

        midBarline = rightBarline;
        setRightBarline(right.rightBarline);
    }

    //-----------------//
    // removeBeamGroup //
    //-----------------//
    public void removeBeamGroup (BeamGroup beamGroup)
    {
        beamGroups.remove(beamGroup);
    }

    //-------------//
    // removeInter //
    //-------------//
    /**
     * This method is meant for (adjustable) FRAT data only.
     *
     *
     * @param inter an instance of flag, rest chord, augDot or tuplet
     */
    public void removeInter (Inter inter)
    {
        if (inter.isVip()) {
            logger.info("VIP removeInter {} from {}", inter, this);
        }

        if (inter instanceof FlagInter) {
            if (flags != null) {
                flags.remove((FlagInter) inter);

                if (flags.isEmpty()) {
                    flags = null;
                }
            }
        } else if (inter instanceof RestChordInter) {
            if (restChords != null) {
                restChords.remove((RestChordInter) inter);

                if (restChords.isEmpty()) {
                    restChords = null;
                }
            }
        } else if (inter instanceof AugmentationDotInter) {
            if (augDots != null) {
                augDots.remove((AugmentationDotInter) inter);

                if (augDots.isEmpty()) {
                    augDots = null;
                }
            }
        } else if (inter instanceof TupletInter) {
            if (tuplets != null) {
                tuplets.remove((TupletInter) inter);

                if (tuplets.isEmpty()) {
                    tuplets = null;
                }
            }
        } else if (inter instanceof HeadChordInter) {
            if (headChords != null) {
                headChords.remove((HeadChordInter) inter);

                if (headChords.isEmpty()) {
                    headChords = null;
                }
            }
        } else if (inter instanceof KeyInter) {
            if (keys != null) {
                keys.remove((KeyInter) inter);

                if (keys.isEmpty()) {
                    keys = null;
                }
            }
        } else if (inter instanceof AbstractTimeInter) {
            if (timeSigs != null) {
                timeSigs.remove((AbstractTimeInter) inter);

                if (timeSigs.isEmpty()) {
                    timeSigs = null;
                }
            }
        } else {
            logger.error("Attempt to use removeInter() with {}", inter);
        }
    }

    //--------------//
    // renameVoices //
    //--------------//
    public void renameVoices ()
    {
        for (int i = 0; i < voices.size(); i++) {
            voices.get(i).setId(i + 1);
        }
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
    public void resetRhythm ()
    {
        voices.clear();

        // Reset voice of every beam group
        for (BeamGroup group : beamGroups) {
            group.resetTiming();
        }

        // Forward reset to every chord handled
        for (AbstractChordInter chord : getStandardChords()) {
            chord.resetTiming();
        }
    }

    //----------//
    // setDummy //
    //----------//
    public void setDummy ()
    {
        dummy = true;
    }

    //----------------//
    // setLeftBarline //
    //----------------//
    public void setLeftBarline (PartBarline leftBarline)
    {
        this.leftBarline = leftBarline;
    }

    //------------//
    // setBarline //
    //------------//
    /**
     * Assign the (right) barline that ends this measure
     *
     * @param rightBarline the right barline
     */
    public void setRightBarline (PartBarline rightBarline)
    {
        this.rightBarline = rightBarline;
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

    //------------//
    // sortVoices //
    //------------//
    public void sortVoices ()
    {
        Collections.sort(voices, Voices.byOrdinate);
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Measure#" + stack.getPageId() + "P" + part.getId() + "}";
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
        Set<Inter> found = new LinkedHashSet<Inter>();

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

    //----------------//
    // needHeadChords //
    //----------------//
    private Set<HeadChordInter> needHeadChords ()
    {
        if (headChords == null) {
            headChords = new LinkedHashSet<HeadChordInter>();
        }

        return headChords;
    }

    //----------------//
    // needRestChords //
    //----------------//
    private Set<RestChordInter> needRestChords ()
    {
        if (restChords == null) {
            restChords = new LinkedHashSet<RestChordInter>();
        }

        return restChords;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // KeyEntry //
    //----------//
    /**
     * Entry [staff index, key] to implement a map of key signatures.
     */
    private static class KeyEntry
            implements Comparable<KeyEntry>
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final int staffIndexInPart; // Staff index in part

        private final KeyInter key; // The key

        //~ Constructors ---------------------------------------------------------------------------
        public KeyEntry (Integer staffIndex,
                         KeyInter key)
        {
            this.staffIndexInPart = staffIndex;
            this.key = key;
        }

        // Needed for JAXB
        private KeyEntry ()
        {
            staffIndexInPart = 0;
            key = null;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int compareTo (KeyEntry that)
        {
            return Integer.compare(staffIndexInPart, that.staffIndexInPart);
        }
    }
}
//
//    //--------------//
//    // getKeyBefore //
//    //--------------//
//    /**
//     * Report the key signature which applies in this measure, whether a key signature
//     * actually starts this measure in the same staff, or whether a key signature was
//     * found in a previous measure, for the same staff.
//     *
//     * @param point the point before which to look
//     * @param staff the containing staff (cannot be null)
//     * @return the current key signature, or null if not found
//     */
//    public KeyInter getKeyBefore (Point point,
//                                  Staff staff)
//    {
//        if (point == null) {
//            throw new NullPointerException();
//        }
//
//        int staffIndexInPart = staff.getIndexInPart();
//
//        // Look in this measure, with same staff, going backwards
//        // TODO: make sure keysigs is sorted by abscissa !!!!!
//        for (int ik = keySigs.size() - 1; ik >= 0; ik--) {
//            final KeyInter ks = keySigs.get(ik);
//
//            if ((ks.getStaff() == staff) && (ks.getCenter().x < point.x)) {
//                return ks;
//            }
//        }
//
//        // Look in previous measures in the system part and the preceding ones
//        Measure measure = this;
//
//        while ((measure = measure.getPrecedingInPage()) != null) {
//            final KeyInter ks = measure.getLastMeasureKey(staffIndexInPart);
//
//            if (ks != null) {
//                return ks;
//            }
//        }
//
//        return null; // Not found (in this page)
//    }
//
//
//    //-------------------//
//    // getLastMeasureKey //
//    //-------------------//
//    /**
//     * Report the last key signature (if any) in this measure, if tagged with the
//     * specified staff index.
//     *
//     * @param staffIndexInPart the imposed part-based staff index
//     * @return the last key signature, or null
//     */
//    public KeyInter getLastMeasureKey (int staffIndexInPart)
//    {
//        // Going backwards
//        for (int ik = keySigs.size() - 1; ik >= 0; ik--) {
//            KeyInter key = keySigs.get(ik);
//
//            if (key.getStaff().getIndexInPart() == staffIndexInPart) {
//                return key;
//            }
//        }
//
//        return null;
//    }
//
