//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          M e a s u r e                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.glyph.Shape;

import omr.math.Rational;

import omr.sheet.Part;
import omr.sheet.PartBarline;
import omr.sheet.Staff;
import omr.sheet.beam.BeamGroup;
import omr.sheet.grid.LineInfo;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.AugmentationDotInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.FlagInter;
import omr.sig.inter.HeadChordInter;
import omr.sig.inter.Inter;
import omr.sig.inter.KeyInter;
import omr.sig.inter.RestChordInter;
import omr.sig.inter.RestInter;
import omr.sig.inter.SmallChordInter;
import omr.sig.inter.TimeInter;
import omr.sig.inter.TupletInter;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
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

    /** Groups of beams in this measure. */
    @XmlElement(name = "beam-group")
    private final Set<BeamGroup> beamGroups = new LinkedHashSet<BeamGroup>();

    /** Head chords, populated once for all by CHORDS step. */
    @XmlElement(name = "head-chord")
    private final Set<HeadChordInter> headChords = new LinkedHashSet<HeadChordInter>();

    /** Grace chords, populated in CHORDS step. */
    @XmlElement(name = "small-chord")
    private final Set<SmallChordInter> graceChords = new LinkedHashSet<SmallChordInter>();

    /** Rest chords, populated by SYMBOLS step & adjusted by RHYTHMS step. */
    @XmlElement(name = "rest-chord")
    private final Set<RestChordInter> restChords = new LinkedHashSet<RestChordInter>();

    /** Voices within this measure, sorted by voice id, populated by RHYTHMS step. */
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

    /** Possibly several Clefs per staff. (Abscissa-ordered) */
    private final TreeSet<ClefInter> clefs = new TreeSet<ClefInter>(Inter.byFullAbscissa);

    /** Possibly one Key signature per staff, since keys may differ between staves.
     * Implemented as a map: (staff index in part) -> Key sig
     */
    private final Map<Integer, KeyInter> keys = new TreeMap<Integer, KeyInter>();

    /** Potential one Time signature per staff. */
    private final Set<TimeInter> timeSigs = new LinkedHashSet<TimeInter>();

    /** Only whole rest-based chords (handled outside time slots). (subset of restChords) */
    private final Set<AbstractChordInter> wholeRestChords = new LinkedHashSet<AbstractChordInter>();

    /** FRAT inters (other than Rest chords) for this measure. */
    private final Set<Inter> otherRhythms = new LinkedHashSet<Inter>(); // FAT actually

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
    //
    //    //--------------------//
    //    // getCurrentDuration //
    //    //--------------------//
    //    private String getCurrentDuration ()
    //    {
    //        Rational measureDuration = Rational.ZERO;
    //
    //        for (Slot slot : slots) {
    //            if (slot.getStartTime() != null) {
    //                for (AbstractChordInter chord : slot.getChords()) {
    //                    Rational chordEnd = slot.getStartTime().plus(chord.getDuration());
    //
    //                    if (chordEnd.compareTo(measureDuration) > 0) {
    //                        measureDuration = chordEnd;
    //                    }
    //                }
    //            }
    //        }
    //
    //        if (measureDuration.equals(Rational.ZERO) && !wholeRestChords.isEmpty()) {
    //            return "W";
    //        }
    //
    //        return String.format("%-5s", measureDuration.toString());
    //    }
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
     * @return the whole rest created
     */
    public RestInter addDummyWholeRest (Staff staff)
    {
        RestChordInter chord = new RestChordInter(0);
        chord.setStaff(staff);
        chord.setStartTime(Rational.ZERO);

        RestInter whole = new RestInter(null, Shape.WHOLE_REST, 0, staff, -1);
        chord.addMember(whole);
        addInter(chord);

        addVoice(Voice.createWholeVoice(chord, this));

        return whole;
    }

    //----------//
    // addInter //
    //----------//
    public void addInter (Inter inter)
    {
        if (inter instanceof AbstractChordInter) {
            AbstractChordInter chord = (AbstractChordInter) inter;
            chord.setMeasure(this);

            if (chord instanceof HeadChordInter) {
                headChords.add((HeadChordInter) chord);
            } else if (chord instanceof RestChordInter) {
                restChords.add((RestChordInter) chord);

                if (chord.getMembers().get(0).getShape() == Shape.WHOLE_REST) {
                    wholeRestChords.add(chord);
                }
            } else if (chord instanceof SmallChordInter) {
                graceChords.add((SmallChordInter) chord);
            }
        } else if (inter instanceof ClefInter) {
            clefs.add((ClefInter) inter);
        } else if (inter instanceof KeyInter) {
            KeyInter key = (KeyInter) inter;
            List<Staff> staves = part.getStaves();
            keys.put(staves.indexOf(key.getStaff()), key);
        } else if (inter instanceof TimeInter) {
            timeSigs.add((TimeInter) inter);
        } else if (inter instanceof FlagInter
                   || inter instanceof AugmentationDotInter
                   || inter instanceof TupletInter) {
            otherRhythms.add(inter);
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
                    sig.inters(new Class[]{ClefInter.class, KeyInter.class, TimeInter.class}));

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
            for (AbstractChordInter chord : headChords) {
                chord.afterReload(this);
            }

            for (AbstractChordInter chord : graceChords) {
                chord.afterReload(this);
            }

            for (AbstractChordInter chord : restChords) {
                chord.afterReload(this);
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //------------//
    // clearFrats //
    //------------//
    public void clearFrats ()
    {
        restChords.clear();
        wholeRestChords.clear();
        otherRhythms.clear();
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
                logger.warn("Inter with no staff {}", inter);

                List<Staff> stavesAround = part.getSystem().getStavesAround(center); // 1 or 2 staves
                staff = stavesAround.get(0);

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

    //--------------//
    // getAllChords //
    //--------------//
    /**
     * Report the collection of all chords (head chords, rest chords, small chords)
     *
     * @return the set of all chords in this measure
     */
    public Set<AbstractChordInter> getAllChords ()
    {
        final Set<AbstractChordInter> allChords = new HashSet<AbstractChordInter>();
        allChords.addAll(headChords);
        allChords.addAll(restChords);
        allChords.addAll(graceChords);

        return allChords;
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
            if (part.getStartingBarline() != null) {
                return part.getStartingBarline();
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

        while ((measure = measure.getPreviousSibling()) != null) {
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
    public SortedSet<ClefInter> getClefs ()
    {
        return clefs;
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

    //---------------//
    // getHeadChords //
    //---------------//
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
    public Collection<HeadChordInter> getHeadChordsAbove (Point point)
    {
        Staff desiredStaff = stack.getSystem().getStaffAtOrAbove(point);
        Collection<HeadChordInter> found = new ArrayList<HeadChordInter>();

        for (HeadChordInter chord : headChords) {
            if (chord.getBottomStaff() == desiredStaff) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y < point.y)) {
                    found.add(chord);
                }
            }
        }

        return found;
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
    //--------//
    // getKey //
    //--------//
    /**
     * Report the potential key signature in this measure for the specified staff index.
     *
     * @param staffIndexInPart imposed part-based staff index
     * @return the staff key signature, or null if not found
     */
    public KeyInter getKey (int staffIndexInPart)
    {
        return keys.get(staffIndexInPart);
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
        for (Iterator<ClefInter> it = clefs.descendingIterator(); it.hasNext();) {
            ClefInter clef = it.next();

            if (clef.getStaff() == staff) {
                return clef;
            }
        }

        return null;
    }

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
        for (Iterator<ClefInter> it = clefs.descendingIterator(); it.hasNext();) {
            ClefInter clef = it.next();

            if ((clef.getStaff() == staff) && (clef.getCenter().x <= point.x)) {
                return clef;
            }
        }

        return null; // No clef previously defined in this measure and staff
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
        Measure prevMeasure = getPreviousSibling();

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

    //--------------------//
    // getPreviousSibling //
    //--------------------//
    /**
     * Return the preceding measure within the same part.
     *
     * @return previous sibling measure in part, or null
     */
    public Measure getPreviousSibling ()
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
        return Collections.unmodifiableSet(restChords);
    }

    //
    //    //---------------//
    //    // getRestChords //
    //    //---------------//
    //    /**
    //     * @return the restChords
    //     */
    //    public Set<AbstractChordInter> getRestChords ()
    //    {
    //        return restChords;
    //    }
    //
    //    //------------//
    //    // getRhythms //
    //    //------------//
    //    /**
    //     * @return the rhythms
    //     */
    //    public Set<Inter> getRhythms ()
    //    {
    //        return rhythms;
    //    }
    //
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
            if (part.getStartingBarline() != null) {
                return part.getStartingBarline().getBarline(part, staff).getRightBar().getCenter();
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
     * Report the collection of standard chords (head chords, rest chords, NO small chords)
     *
     * @return the set of all standard chords in this measure
     */
    public Set<AbstractChordInter> getStandardChords ()
    {
        final Set<AbstractChordInter> stdChords = new HashSet<AbstractChordInter>();
        stdChords.addAll(headChords);
        stdChords.addAll(restChords);

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
    public TimeInter getTimeSignature ()
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
    public TimeInter getTimeSignature (int staffIndexInPart)
    {
        for (TimeInter ts : timeSigs) {
            if (ts.getStaff().getId() == staffIndexInPart) {
                return ts;
            }
        }

        return null; // Not found
    }

    //------------//
    // getTuplets //
    //------------//
    public Set<TupletInter> getTuplets ()
    {
        Set<TupletInter> tuplets = new LinkedHashSet<TupletInter>();

        for (Inter inter : otherRhythms) {
            if (inter instanceof TupletInter) {
                tuplets.add((TupletInter) inter);
            }
        }

        return tuplets;
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

    //--------------------//
    // getWholeRestChords //
    //--------------------//
    /**
     * @return the wholeRestChords
     */
    public Set<AbstractChordInter> getWholeRestChords ()
    {
        return wholeRestChords;
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
        if (keys.isEmpty()) {
            return true;
        }

        final int staffCount = part.getStaves().size();
        Integer prevFifths = null;

        for (int index = 0; index < staffCount; index++) {
            KeyInter key = keys.get(index);

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

        for (RestChordInter restChord : restChords) {
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
        clefs.addAll(right.clefs);
        keys.putAll(right.keys);
        timeSigs.addAll(right.timeSigs);
        restChords.addAll(right.restChords);
        headChords.addAll(right.headChords);
        voices.addAll(right.voices); // (left) voices are empty
        beamGroups.addAll(right.beamGroups);

        midBarline = rightBarline;
        setRightBarline(right.rightBarline);
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
        if (inter instanceof RestChordInter) {
            RestChordInter restChord = (RestChordInter) inter;
            restChords.remove(restChord);
            wholeRestChords.remove(restChord); // Just in case
        } else if (inter instanceof FlagInter
                   || inter instanceof AugmentationDotInter
                   || inter instanceof TupletInter) {
            otherRhythms.remove(inter);
        } else if (inter instanceof HeadChordInter) {
            headChords.remove((HeadChordInter) inter);
        } else if (inter instanceof SmallChordInter) {
            graceChords.remove((SmallChordInter) inter);
        } else {
            logger.error("Attempt to use removeInter() with {}", inter);
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

    //-------------//
    // swapVoiceId //
    //-------------//
    /**
     * Change the id of the provided voice to the provided id
     * (and change the other voice, if any, which owned the provided id).
     *
     * @param voice the voice whose id must be changed
     * @param id    the new id
     */
    public void swapVoiceId (Voice voice,
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
        Set<Inter> found = new HashSet<Inter>();

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
     * NOTA: @XmlAttribute annotation forces this information to be written in project file
     * (although it is not used when unmarshalling)
     *
     * @return the page ID of containing stack
     */
    @XmlAttribute(name = "id")
    @SuppressWarnings("unused")
    private String getPageId ()
    {
        return stack.getPageId();
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
}
//
//    //------------//
//    // printVoices//
//    //------------//
//    /**
//     * Print the voices of this measure on standard output.
//     *
//     * @param title a potential title for this printout, or null
//     */
//    public void printVoices (String title)
//    {
//        StringBuilder sb = new StringBuilder();
//
//        // Title
//        if (title != null) {
//            sb.append(title);
//        }
//
//        // Measure
//        sb.append(this);
//
//        // Slot headers
//        if (!slots.isEmpty()) {
//            sb.append("\n    ");
//
//            for (Slot slot : slots) {
//                if (slot.getStartTime() != null) {
//                    sb.append("|").append(String.format("%-7s", slot.getStartTime()));
//                }
//            }
//
//            sb.append("|").append(getCurrentDuration());
//        }
//
//        for (Voice voice : voices) {
//            sb.append("\n").append(voice.toStrip());
//        }
//
//        logger.info(sb.toString());
//    }
//
//
//    //------------//
//    // isImplicit //
//    //------------//
//    /**
//     * Report whether this measure is implicit (pickup or repeat last half).
//     *
//     * @return true if measure is pickup or secondHalf
//     */
//    public boolean isImplicit ()
//    {
//        return (special == Special.PICKUP) || (special == Special.SECOND_HALF);
//    }
//
//
//    //------------//
//    // lookupRest //
//    //------------//
//    /**
//     * Look up for a potential rest interleaved between the given stemmed chords
//     *
//     * @param left  the chord on the left of the area
//     * @param right the chord on the right of the area
//     * @return the rest found, or null otherwise
//     */
//    public RestInter lookupRest (AbstractChordInter left,
//                                 AbstractChordInter right)
//    {
//        // Define the area limited by the left and right chords with their stems
//        // and check for intersection with a rest note
//        Polygon polygon = new Polygon();
//        polygon.addPoint(left.getHeadLocation().x, left.getHeadLocation().y);
//        polygon.addPoint(left.getTailLocation().x, left.getTailLocation().y);
//        polygon.addPoint(right.getTailLocation().x, right.getTailLocation().y);
//        polygon.addPoint(right.getHeadLocation().x, right.getHeadLocation().y);
//
//        for (AbstractChordInter chord : chords) {
//            // Not interested in the bounding chords
//            if ((chord == left) || (chord == right)) {
//                continue;
//            }
//
//            for (Inter inter : chord.getMembers()) {
//                AbstractNoteInter note = (AbstractNoteInter) inter;
//
//                // Interested in rest notes only
//                if (note instanceof RestInter) {
//                    Rectangle box = note.getBounds();
//
//                    if (polygon.intersects(box.x, box.y, box.width, box.height)) {
//                        return (RestInter) note;
//                    }
//                }
//            }
//        }
//
//        return null;
//    }
//
//
//    //----------------//
//    // getClosestSlot //
//    //----------------//
//    /**
//     * Report the time slot which has the closest abscissa to a provided point.
//     *
//     * @param point the reference point
//     * @return the abscissa-wise closest slot
//     */
//    public Slot getClosestSlot (Point point)
//    {
//        Slot bestSlot = null;
//        int bestDx = Integer.MAX_VALUE;
//
//        for (Slot slot : getSlots()) {
//            int dx = Math.abs(slot.getDskX() - point.x);
//
//            if (dx < bestDx) {
//                bestDx = dx;
//                bestSlot = slot;
//            }
//        }
//
//        return bestSlot;
//    }
//
//
//    //----------//
//    // getSlots //
//    //----------//
//    /**
//     * Report the ordered collection of slots.
//     *
//     * @return the collection of slots
//     */
//    public List<Slot> getSlots ()
//    {
//        return slots;
//    }
//
