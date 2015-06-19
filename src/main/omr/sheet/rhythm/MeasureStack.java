//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     M e a s u r e S t a c k                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.glyph.Shape;

import omr.math.Rational;

import omr.score.Page;
import omr.score.Score;

import omr.sheet.Part;
import omr.sheet.Skew;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.beam.BeamGroup;
import omr.sheet.grid.FilamentLine;

import omr.sig.inter.AbstractNoteInter;
import omr.sig.inter.ChordInter;
import omr.sig.inter.Inter;
import omr.sig.inter.RestInter;
import omr.sig.inter.SmallChordInter;
import omr.sig.inter.TimeInter;
import omr.sig.inter.TupletInter;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.LEFT;
import static omr.util.HorizontalSide.RIGHT;
import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code MeasureStack} represents a vertical stack of {@link Measure} instances,
 * embracing all parts of a system.
 * <p>
 * This approach is convenient to use vertical alignments when dealing with time slots.
 * <p>
 * How is measure ID handled:
 * <ol>
 * <li>IDs are recorded as page-based integer values.</li>
 * <li>Initial values start from 1 and are incremented for each following raw measure.</li>
 * <li>Once measure role is refined, its ID may change:
 * <ul>
 * <li>A pickup measure has ID 0, instead of 1.</li>
 * <li>A special value (Xn) is used for second half repeats, when first half repeat is n.</li>
 * <li>Courtesy measures that may be found at end of system (containing just cautionary key or time
 * signatures) are simply ignored, hence they have no ID.</li>
 * </ul>
 * <li>IDs, as exported in MusicXML, combine the page-based IDs to provide score-based (absolute)
 * IDs.</li>
 * </ol>
 *
 * @see Measure
 *
 * @author Hervé Bitteur
 */
public class MeasureStack
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MeasureStack.class);

    /** String prefix for a second half id. */
    private static final String SH_STRING = "X";

    //~ Enumerations -------------------------------------------------------------------------------
    public enum Special
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        PICKUP,
        FIRST_HALF,
        SECOND_HALF;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** The containing system. */
    @Navigable(false)
    private final SystemInfo system;

    /** Vertical sequence of (Part) measures. */
    private final List<Measure> measures = new ArrayList<Measure>();

    /** (Page-based) measure Id. */
    private Integer id;

    /** Minimum left abscissa, across all staves. */
    private int left;

    /** Maximum right abscissa, across all staves. */
    private int right;

    /** Flag for special measure. */
    private Special special;

    /** Repeat sign on either side of the measure stack. */
    private Set<HorizontalSide> repeat;

    /** Theoretical measure stack duration, based on current time signature. */
    private Rational expectedDuration;

    /** Groups of beams in this measure stack. */
    private final List<BeamGroup> beamGroups = new ArrayList<BeamGroup>();

    /** All chords (head or rest, but without grace chords). */
    private final List<ChordInter> chords = new ArrayList<ChordInter>();

    /** Grace chords. */
    private final List<ChordInter> graceChords = new ArrayList<ChordInter>();

    /** Only rest-based chords. (subset of chords) */
    private final List<ChordInter> restChords = new ArrayList<ChordInter>();

    /** Only whole rest-based chords (handled outside time slots). (subset of restChords) */
    private final List<ChordInter> wholeRestChords = new ArrayList<ChordInter>();

    /** Non-chord rhythm inters for this stack. */
    private final List<Inter> rhythms = new ArrayList<Inter>();

    //-- Resettable rhythm data --
    //----------------------------
    //
    /** Sequence of time slots within the measure. */
    private final List<Slot> slots = new ArrayList<Slot>();

    /** Voices within this measure stack, sorted by increasing voice id. */
    private final List<Voice> voices = new ArrayList<Voice>();

    /** Actual measure stack duration, based on durations of contained chords. */
    private Rational actualDuration;

    /** Excess duration, if any. */
    private Rational excess;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MeasureStack} object.
     *
     * @param system containing system
     */
    public MeasureStack (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addGroup //
    //----------//
    /**
     * Add a beam group to this measure.
     *
     * @param group a beam group to add
     */
    public void addGroup (BeamGroup group)
    {
        beamGroups.add(group);
    }

    //----------//
    // addInter //
    //----------//
    public void addInter (Inter inter)
    {
        if (inter instanceof ChordInter) {
            ChordInter chord = (ChordInter) inter;
            Staff staff = chord.getStaff();
            Part part = staff.getPart();
            int partIndex = system.getParts().indexOf(part);
            Measure measure = measures.get(partIndex);
            chord.setMeasure(measure);

            if (chord instanceof SmallChordInter) {
                graceChords.add(chord);
            } else {
                chords.add(chord);
            }

            Inter note = chord.getMembers().get(0);

            if (note instanceof RestInter) {
                restChords.add(chord);

                if (note.getShape() == Shape.WHOLE_REST) {
                    wholeRestChords.add(chord);
                }
            }
        } else if (!rhythms.contains(inter)) {
            rhythms.add(inter);
        }
    }

    //------------//
    // addMeasure //
    //------------//
    public void addMeasure (Measure measure)
    {
        if (measures.isEmpty()) {
            left = Integer.MAX_VALUE;
            right = 0;
        }

        measures.add(measure);

        for (Staff staff : measure.getPart().getStaves()) {
            left = Math.min(left, measure.getAbscissa(LEFT, staff));
            right = Math.max(right, measure.getAbscissa(RIGHT, staff));
        }
    }

    //-----------//
    // addRepeat //
    //-----------//
    public void addRepeat (HorizontalSide side)
    {
        if (repeat == null) {
            repeat = EnumSet.noneOf(HorizontalSide.class);
        }

        repeat.add(side);
    }

    //------------------//
    // addTimeSignature //
    //------------------//
    public void addTimeSignature (TimeInter ts)
    {
        // Populate (part) measure with provided time signature
        Staff staff = ts.getStaff();
        Point center = ts.getCenter();
        Measure measure = staff.getPart().getMeasureAt(center);
        measure.addInter(ts);
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
            voice.checkDuration(this);
        }
    }

    //-------------//
    // clearInters //
    //-------------//
    public void clearInters ()
    {
        graceChords.clear();
        chords.clear();
        restChords.clear();
        wholeRestChords.clear();
        rhythms.clear();
    }

    //-------------------//
    // connectTiedVoices //
    //-------------------//
    /**
     * Make sure that notes tied across measures stacks keep the same voice.
     * This is performed for all ties in this part.
     */
    public void connectTiedVoices ()
    {
        //        for (TreeNode tn : getSlurs()) {
        //            Slur slur = (Slur) tn;
        //
        //            if (!slur.isTie()) {
        //                continue;
        //            }
        //
        //            // Voice on left (perhaps in a previous measure / system / page)
        //            Note leftNote = slur.getLeftNote();
        //
        //            if (leftNote == null) {
        //                Slur leftExtension = slur.getLeftExtension();
        //
        //                if (leftExtension == null) {
        //                    continue;
        //                }
        //
        //                leftNote = leftExtension.getLeftNote();
        //
        //                if (leftNote == null) {
        //                    continue;
        //                }
        //            }
        //
        //            ChordInter leftChord = leftNote.getChord();
        //            OldVoice leftVoice = leftChord.getVoice();
        //
        //            // Voice on right
        //            Note rightNote = slur.getRightNote();
        //
        //            if (rightNote == null) {
        //                continue;
        //            }
        //
        //            ChordInter rightChord = rightNote.getChord();
        //            OldVoice rightVoice = rightChord.getVoice();
        //
        //            if (leftVoice.getId() != rightVoice.getId()) {
        //                logger.debug("Tie to map {} and {}", leftChord, rightChord);
        //                rightChord.getMeasure().swapVoiceId(rightVoice, leftVoice.getId());
        //            }
        //        }
    }

    //--------//
    // filter //
    //--------//
    public List<Inter> filter (Collection<Inter> systemInters)
    {
        List<Inter> kept = new ArrayList<Inter>();

        for (Inter inter : systemInters) {
            Point center = inter.getCenter();

            // Rough abscissa limits
            if ((center.x < left) || (center.x > right)) {
                continue;
            }

            Staff staff = inter.getStaff();
            final Measure measure;

            if (staff != null) {
                Part part = staff.getPart();
                measure = part.getMeasureAt(center);
            } else {
                List<Staff> stavesAround = system.getStavesAround(center); // 1 or 2 staves
                staff = stavesAround.get(0);
                measure = getMeasureAt(staff);
            }

            // Precise abscissa limits
            if ((measure.getAbscissa(LEFT, staff) <= center.x)
                && (center.x <= measure.getAbscissa(RIGHT, staff))) {
                kept.add(inter);
            }
        }

        return kept;
    }

    //-------------------//
    // getActualDuration //
    //-------------------//
    /**
     * Report the duration of this measure stack , as computed from its contained voices.
     *
     * @return the (actual) measure stack duration, or 0 if no rest / note exists in this stack
     */
    public Rational getActualDuration ()
    {
        if (actualDuration != null) {
            return actualDuration;
        } else {
            return Rational.ZERO;
        }
    }

    //---------------//
    // getBeamGroups //
    //---------------//
    /**
     * Report the collection of beam groups.
     *
     * @return the list of beam groups
     */
    public List<BeamGroup> getBeamGroups ()
    {
        return beamGroups;
    }

    //---------------//
    // getChordAbove //
    //---------------//
    /**
     * Retrieve the closest chord within staff above.
     *
     * @param point the system-based location
     * @return the most suitable chord, or null
     */
    public ChordInter getChordAbove (Point2D point)
    {
        Collection<ChordInter> aboves = getChordsAbove(point);

        if (!aboves.isEmpty()) {
            return getClosestChord(aboves, point);
        }

        return null;
    }

    //---------------//
    // getChordBelow //
    //---------------//
    /**
     * Retrieve the closest chord within staff below.
     *
     * @param point the system-based location
     * @return the most suitable chord, or null
     */
    public ChordInter getChordBelow (Point2D point)
    {
        Collection<ChordInter> belows = getChordsBelow(point);

        if (!belows.isEmpty()) {
            return getClosestChord(belows, point);
        }

        return null;
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the collection of chords.
     *
     * @return the list of chords
     */
    public List<ChordInter> getChords ()
    {
        return chords;
    }

    //----------------//
    // getChordsAbove //
    //----------------//
    /**
     * Report the collection of chords whose head is located in the staff above the
     * provided point.
     *
     * @param point the provided point
     * @return the (perhaps empty) collection of chords
     */
    public Collection<ChordInter> getChordsAbove (Point2D point)
    {
        Staff desiredStaff = getSystem().getStaffAtOrAbove(point);
        Collection<ChordInter> found = new ArrayList<ChordInter>();

        for (ChordInter chord : chords) {
            if (chord.getStaff() == desiredStaff) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y < point.getY())) {
                    found.add(chord);
                }
            }
        }

        return found;
    }

    //----------------//
    // getChordsBelow //
    //----------------//
    /**
     * Report the collection of chords whose head is located in the staff below the
     * provided point.
     *
     * @param point the provided point
     * @return the (perhaps empty) collection of chords
     */
    public Collection<ChordInter> getChordsBelow (Point2D point)
    {
        Staff desiredStaff = getSystem().getStaffAtOrBelow(point);
        Collection<ChordInter> found = new ArrayList<ChordInter>();

        for (ChordInter chord : chords) {
            if (chord.getStaff() == desiredStaff) {
                Point head = chord.getHeadLocation();

                if ((head != null) && (head.y > point.getY())) {
                    found.add(chord);
                }
            }
        }

        return found;
    }

    //-----------------//
    // getClosestChord //
    //-----------------//
    /**
     * From a provided Chord collection, report the chord which has the closest abscissa
     * to a provided point.
     *
     * @param chords the collection of chords to browse
     * @param point  the reference point
     * @return the abscissa-wise closest chord
     */
    public ChordInter getClosestChord (Collection<ChordInter> chords,
                                       Point2D point)
    {
        ChordInter bestChord = null;
        double bestDx = Double.MAX_VALUE;

        for (ChordInter chord : chords) {
            double dx = Math.abs(chord.getHeadLocation().x - point.getX());

            if (dx < bestDx) {
                bestDx = dx;
                bestChord = chord;
            }
        }

        return bestChord;
    }

    //----------------//
    // getClosestSlot //
    //----------------//
    /**
     * Report the time slot which has the closest abscissa to a provided point.
     *
     * @param point the reference point
     * @return the abscissa-wise closest slot
     */
    public Slot getClosestSlot (Point2D point)
    {
        final double xOffset = getXOffset(point);
        Slot bestSlot = null;
        double bestDx = Double.MAX_VALUE;

        for (Slot slot : slots) {
            double dx = Math.abs(slot.getXOffset() - xOffset);

            if (dx < bestDx) {
                bestDx = dx;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    //--------------------//
    // getCurrentDuration //
    //--------------------//
    /**
     * Report the current duration for this measure, as computed from current content in
     * terms of slots and chords.
     *
     * @return the current measure duration
     */
    public Rational getCurrentDuration ()
    {
        Rational measureDur = Rational.ZERO;

        // Whole/multi rests are handled outside of slots
        for (Slot slot : slots) {
            if (slot.getStartTime() != null) {
                for (ChordInter chord : slot.getChords()) {
                    Rational chordEnd = slot.getStartTime().plus(chord.getDuration());

                    if (chordEnd.compareTo(measureDur) > 0) {
                        measureDur = chordEnd;
                    }
                }
            }
        }

        return measureDur;
    }

    //-------------------------//
    // getCurrentTimeSignature //
    //-------------------------//
    /**
     * Report the time signature which applies in this stack, whether a time signature
     * actually starts this stack in whatever staff, or whether a time signature was
     * found in a previous stack, even in preceding pages.
     * <p>
     * <b>NOTA</b>This method looks up for time sig in preceding pages as well</p>
     *
     * @return the current time signature, or null if not found at all
     */
    public TimeInter getCurrentTimeSignature ()
    {
        // Backward from this measure to the beginning of the score
        MeasureStack stack = this;
        Page page = system.getPage();

        while (stack != null) {
            // Check in the measure stack
            TimeInter ts = stack.getFirstMeasure().getTimeSignature();

            if (ts != null) {
                return ts;
            }

            // Move to preceding measure stack
            stack = stack.getPrecedingInPage();

            //
            //            if (measure == null) {
            //                page = page.getPrecedingInScore();
            //
            //                if (page == null) {
            //                    return null;
            //                } else {
            //                    measure = page.getLastSystem().getLastPart().getLastMeasure();
            //                }
            //            }
        }

        return null; // Not found !!!
    }

    //---------------//
    // getEventChord //
    //---------------//
    /**
     * Retrieve the most suitable chord to connect the event point to.
     *
     * @param point the system-based location
     * @return the most suitable chord, or null
     */
    public ChordInter getEventChord (Point2D point)
    {
        // First, try staff just above
        ChordInter above = getChordAbove(point);

        if (above != null) {
            return above;
        }

        // Second, try staff just below
        return getChordBelow(point);
    }

    //-----------//
    // getExcess //
    //-----------//
    /**
     * Report the excess duration of this stack, if any.
     *
     * @return the duration in excess, or null
     */
    public Rational getExcess ()
    {
        return excess;
    }

    //---------------------//
    // getExpectedDuration //
    //---------------------//
    /**
     * Report the theoretical duration of this stack.
     *
     * @return the expected measure stack duration
     */
    public Rational getExpectedDuration ()
    {
        return expectedDuration;

        //        try {
        //            if (expectedDuration == null) {
        //                int numerator;
        //                int denominator;
        //                TimeInter ts = getCurrentTimeSignature();
        //
        //                if (ts != null) {
        //                    numerator = ts.getNumerator();
        //                    denominator = ts.getDenominator();
        //                } else {
        //                    numerator = 4;
        //                    denominator = 4;
        //                }
        //
        //                expectedDuration = new Rational(numerator, denominator);
        //            }
        //
        //            return expectedDuration;
        //        } catch (NullPointerException npe) {
        //            throw new TimeSignature.InvalidTimeSignature();
        //        }
    }

    //-----------------//
    // getFirstMeasure //
    //-----------------//
    public Measure getFirstMeasure ()
    {
        if (measures.isEmpty()) {
            return null;
        }

        return measures.get(0);
    }

    //--------------------//
    // getFollowingInPage //
    //--------------------//
    /**
     * Report the following measure stack of this one, either in this system, or in the
     * following system, but still in the same page.
     *
     * @return the following measure stack, or null if not found in the page
     */
    public MeasureStack getFollowingInPage ()
    {
        // Look in current part
        MeasureStack nextStack = getNextSibling();

        if (nextStack != null) {
            return nextStack;
        }

        SystemInfo nextSystem = system.getFollowingInPage();

        if (nextSystem != null) {
            return nextSystem.getFirstMeasureStack();
        } else {
            return null;
        }
    }

    //------------//
    // getIdValue //
    //------------//
    /**
     * Report the numeric value of the measure id.
     * Note that first (n) & second (Xn) measure halves share the same numeric value n.
     *
     * @return the numeric value of measure id
     */
    public int getIdValue ()
    {
        return id;
    }

    //-------------//
    // getLastSlot //
    //-------------//
    /**
     * Report the last slot in stack.
     *
     * @return the last slot or null if none
     */
    public Slot getLastSlot ()
    {
        if (slots.isEmpty()) {
            return null;
        }

        return slots.get(slots.size() - 1);
    }

    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Report the measure at provided staff
     *
     * @param staff the provided staff
     * @return the measure that contains the provided staff
     */
    public Measure getMeasureAt (Staff staff)
    {
        for (Measure measure : measures) {
            if (measure.getPart().getStaves().contains(staff)) {
                return measure;
            }
        }

        return null;
    }

    //-------------//
    // getMeasures //
    //-------------//
    public List<Measure> getMeasures ()
    {
        return measures;
    }

    //----------------//
    // getNextSibling //
    //----------------//
    /**
     * Return the next measure stack within the same system.
     *
     * @return previous sibling measure stack in system, or null
     */
    public MeasureStack getNextSibling ()
    {
        int index = system.getMeasureStacks().indexOf(this);

        if (index < (system.getMeasureStacks().size() - 1)) {
            return system.getMeasureStacks().get(index + 1);
        }

        return null;
    }

    //-----------//
    // getPageId //
    //-----------//
    /**
     * Report the page-based measure id string.
     *
     * @return the page-based measure id string
     */
    public String getPageId ()
    {
        if (id != null) {
            return ((special == Special.SECOND_HALF) ? SH_STRING : "") + id;
        }

        // No id defined yet
        StringBuilder sb = new StringBuilder();
        sb.append("S").append(system.getId());
        sb.append("M").append(1 + system.getMeasureStacks().indexOf(this));

        return sb.toString();
    }

    //--------------------//
    // getPrecedingInPage //
    //--------------------//
    /**
     * Report the preceding measure stack of this one, either in this system, or in the
     * preceding system, but still in the same page.
     *
     * @return the preceding measure stack, or null if not found in the page
     */
    public MeasureStack getPrecedingInPage ()
    {
        // Look in current part
        MeasureStack prevStack = getPreviousSibling();

        if (prevStack != null) {
            return prevStack;
        }

        SystemInfo precedingSystem = system.getPrecedingInPage();

        if (precedingSystem != null) {
            return precedingSystem.getLastMeasureStack();
        } else {
            return null;
        }
    }

    //--------------------//
    // getPreviousSibling //
    //--------------------//
    /**
     * Return the preceding measure stack within the same system.
     *
     * @return previous sibling measure stack in system, or null
     */
    public MeasureStack getPreviousSibling ()
    {
        int index = system.getMeasureStacks().indexOf(this);

        if (index > 0) {
            return system.getMeasureStacks().get(index - 1);
        }

        return null;
    }

    //---------------//
    // getRestChords //
    //---------------//
    /**
     * @return the restChords
     */
    public List<ChordInter> getRestChords ()
    {
        return restChords;
    }

    //------------//
    // getRhythms //
    //------------//
    /**
     * @return the rhythms
     */
    public List<Inter> getRhythms ()
    {
        return rhythms;
    }

    //------------//
    // getScoreId //
    //------------//
    /**
     * Report the character string of the score-based measure id.
     *
     * @param score the containing score
     * @return the (absolute) score-based measure id string
     */
    public String getScoreId (Score score)
    {
        if (id == null) {
            return null;
        }

        final Page page = system.getPage();
        final int pageMeasureIdOffset = score.getMeasureIdOffset(page);

        return ((special == Special.SECOND_HALF) ? SH_STRING : "") + (pageMeasureIdOffset + id);
    }

    //----------//
    // getSlots //
    //----------//
    /**
     * Report the ordered collection of slots.
     *
     * @return the collection of slots
     */
    public List<Slot> getSlots ()
    {
        return slots;
    }

    //----------------//
    // getStaffVoices //
    //----------------//
    /**
     * Report the voices that start in provided staff
     *
     * @param staff the provided staff
     * @return the sequence of voices for this staff
     */
    public List<Voice> getStaffVoices (Staff staff)
    {
        List<Voice> found = new ArrayList<Voice>();

        for (Voice voice : voices) {
            if (staff == voice.getFirstChord().getStaff()) {
                found.add(voice);
            }
        }

        return found;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * @return the system
     */
    public SystemInfo getSystem ()
    {
        return system;
    }

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature in this stack (whatever the staff).
     *
     * @return the stack time signature, or null if not found
     */
    public TimeInter getTimeSignature ()
    {
        for (Measure measure : measures) {
            TimeInter ts = measure.getTimeSignature();

            if (ts != null) {
                return ts;
            }
        }

        return null; // Not found
    }

    //------------//
    // getTuplets //
    //------------//
    public List<TupletInter> getTuplets ()
    {
        List<TupletInter> tuplets = new ArrayList<TupletInter>();

        for (Inter inter : rhythms) {
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
     * Report the number of voices in this measure stack.
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
        return voices;
    }

    //--------------------//
    // getWholeRestChords //
    //--------------------//
    /**
     * @return the wholeRestChords
     */
    public List<ChordInter> getWholeRestChords ()
    {
        return wholeRestChords;
    }

    //------------//
    // getXOffset //
    //------------//
    /**
     * Report the abscissa offset since stack left border of the provided point.
     *
     * @param point the provided point
     * @return xOffset of the point WRT stack start
     */
    public double getXOffset (Point2D point)
    {
        return getXOffset(point, system.getStavesAround(point));
    }

    //------------//
    // getXOffset //
    //------------//
    /**
     * Report the precise abscissa offset since stack left border of the provided point.
     *
     * @param point the provided point
     * @param staff the staff of reference
     * @return xOffset of the point WRT stack left side
     */
    public double getXOffset (Point2D point,
                              Staff staff)
    {
        // Extrapolate, using skew, from single staff
        final Skew skew = system.getSkew();
        final Measure measure = getMeasureAt(staff);
        final Point left = measure.getSidePoint(HorizontalSide.LEFT, staff);

        return skew.deskewed(point).getX() - skew.deskewed(left).getX();
    }

    //------------//
    // getXOffset //
    //------------//
    /**
     * Report the precise abscissa offset since stack left border of the provided point.
     *
     * @param point        the provided point
     * @param stavesAround one or two staves that surround the provided point
     * @return xOffset of the point WRT stack left side
     */
    public double getXOffset (Point2D point,
                              List<Staff> stavesAround)
    {
        Staff staff1 = stavesAround.get(0);
        Measure measure1 = getMeasureAt(staff1);
        Point left1 = measure1.getSidePoint(HorizontalSide.LEFT, staff1);

        if (stavesAround.size() > 1) {
            // Interpolate between staff above & staff below
            FilamentLine line1 = staff1.getLines().get(staff1.getLines().size() / 2);
            double y1 = line1.yAt(point.getX());
            double offset1 = point.getX() - left1.x;

            Staff staff2 = stavesAround.get(1);
            Measure measure2 = getMeasureAt(staff2);
            Point left2 = measure2.getSidePoint(HorizontalSide.LEFT, staff2);
            FilamentLine line2 = staff2.getLines().get(staff2.getLines().size() / 2);
            double y2 = line2.yAt(point.getX());
            double offset2 = point.getX() - left2.x;

            return offset1 + (((offset2 - offset1) * (point.getY() - y1)) / (y2 - y1));
        } else {
            return getXOffset(point, staff1);
        }
    }

    //-------------//
    // isFirstHalf //
    //-------------//
    /**
     * Report whether this measure stack is a repeat first half.
     *
     * @return true if measure is firstHalf
     */
    public boolean isFirstHalf ()
    {
        return special == Special.FIRST_HALF;
    }

    //------------//
    // isImplicit //
    //------------//
    /**
     * Report whether this measure stack is implicit (pickup or repeat last half).
     *
     * @return true if measure is pickup or secondHalf
     */
    public boolean isImplicit ()
    {
        return (special == Special.PICKUP) || (special == Special.SECOND_HALF);
    }

    //----------//
    // isRepeat //
    //----------//
    public boolean isRepeat (HorizontalSide side)
    {
        return (repeat != null) && repeat.contains(side);
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
    public RestInter lookupRest (ChordInter left,
                                 ChordInter right)
    {
        // Define the area limited by the left and right chords with their stems
        // and check for intersection with a rest note
        Polygon polygon = new Polygon();
        polygon.addPoint(left.getHeadLocation().x, left.getHeadLocation().y);
        polygon.addPoint(left.getTailLocation().x, left.getTailLocation().y);
        polygon.addPoint(right.getTailLocation().x, right.getTailLocation().y);
        polygon.addPoint(right.getHeadLocation().x, right.getHeadLocation().y);

        for (ChordInter chord : chords) {
            // Not interested in the bounding chords
            if ((chord == left) || (chord == right)) {
                continue;
            }

            for (Inter inter : chord.getMembers()) {
                AbstractNoteInter note = (AbstractNoteInter) inter;

                // Interested in rest notes only
                if (note instanceof RestInter) {
                    Rectangle box = note.getBounds();

                    if (polygon.intersects(box.x, box.y, box.width, box.height)) {
                        return (RestInter) note;
                    }
                }
            }
        }

        return null;
    }

    //----------------//
    // mergeWithRight //
    //----------------//
    /**
     * Merge this stack with the content of the following stack on the right.
     *
     * @param rightStack the following measure stack
     */
    public void mergeWithRight (MeasureStack rightStack)
    {
        // Merge the measures, part by part
        for (int iLine = 0; iLine < rightStack.getMeasures().size(); iLine++) {
            measures.get(iLine).mergeWithRight(rightStack.measures.get(iLine));
        }

        // Merge the stacks data
        right = rightStack.right;
        actualDuration = this.getActualDuration().plus(rightStack.getActualDuration());
        chords.addAll(rightStack.chords);
        wholeRestChords.addAll(rightStack.wholeRestChords);
        voices.addAll(rightStack.voices);

        // Beware, merged slots must have their stack & xOffset updated accordingly
        slots.addAll(rightStack.slots);

        for (Slot slot : slots) {
            slot.setStack(this);
        }

        ///beamGroups.addAll(rightStack.beamGroups); ???
        //TODO: what about the now "inside" barline (which may have a repeat sign) ???
    }

    //------------//
    // printVoices//
    //------------//
    /**
     * Print the voices of this measure stack on standard output.
     *
     * @param title a potential title for this printout, or null
     */
    public void printVoices (String title)
    {
        StringBuilder sb = new StringBuilder();

        // Title
        if (title != null) {
            sb.append(title);
        }

        // Measure
        sb.append(this);

        // Slot headers
        if (!slots.isEmpty()) {
            sb.append("\n   ");

            for (Slot slot : slots) {
                if (slot.getStartTime() != null) {
                    sb.append("|").append(String.format("%-7s", slot.getStartTime()));
                }
            }

            sb.append("|").append(getCurrentDuration());
        }

        for (Voice voice : voices) {
            sb.append("\n").append(voice.toStrip());
        }

        logger.info(sb.toString());
    }

    //-------------//
    // removeInter //
    //-------------//
    public void removeInter (Inter inter)
    {
        if (inter instanceof ChordInter) {
            ChordInter chord = (ChordInter) inter;
            chords.remove(chord);
            restChords.remove(chord);
            wholeRestChords.remove(chord);
        } else {
            rhythms.remove(inter);
        }
    }

    //-------------//
    // resetRhythm //
    //-------------//
    public void resetRhythm ()
    {
        slots.clear();
        voices.clear();
        actualDuration = null;

        // Reset voice of every beam group
        for (BeamGroup group : beamGroups) {
            group.resetTiming();
        }

        // Forward reset to every chord handled
        for (ChordInter chord : chords) {
            chord.resetTiming();
        }
    }

    //-------------------//
    // setActualDuration //
    //-------------------//
    /**
     * Register in this measure stack its actual duration.
     *
     * @param actualDuration the duration value
     */
    public void setActualDuration (Rational actualDuration)
    {
        this.actualDuration = actualDuration;
    }

    //-----------//
    // setExcess //
    //-----------//
    /**
     * Assign an excess duration for this stack.
     *
     * @param excess the duration in excess
     */
    public void setExcess (Rational excess)
    {
        this.excess = excess;
    }

    //---------------------//
    // setExpectedDuration //
    //---------------------//
    public void setExpectedDuration (Rational expectedDuration)
    {
        this.expectedDuration = expectedDuration;
    }

    //--------------//
    // setFirstHalf //
    //--------------//
    public void setFirstHalf ()
    {
        special = Special.FIRST_HALF;
    }

    //------------//
    // setIdValue //
    //------------//
    /**
     * Assign the proper page-based id value to this measure stack.
     *
     * @param id the proper page-based measure stack id value
     */
    public void setIdValue (int id)
    {
        this.id = id;
    }

    //-----------//
    // setPickup //
    //-----------//
    public void setPickup ()
    {
        special = Special.PICKUP;
    }

    //---------------//
    // setSecondHalf //
    //---------------//
    public void setSecondHalf ()
    {
        special = Special.SECOND_HALF;
    }

    //------------//
    // setSpecial //
    //------------//
    public void setSpecial (Special special)
    {
        this.special = special;
    }

    //---------//
    // shorten //
    //---------//
    /**
     * Flag this measure stack as partial (shorter than expected duration).
     *
     * @param shortening how much the measure stack duration is to be reduced
     */
    public void shorten (Rational shortening)
    {
        // Remove any final forward mark consistent with the shortening
        for (Voice voice : voices) {
            Rational duration = voice.getTermination();

            if (duration != null) {
                //                if (duration.equals(shortening)) {
                //                    if (!voice.isWhole()) {
                //                        // Remove the related mark
                //                        ChordInter chord = voice.getLastChord();
                //
                //                        if (chord != null) {
                //                            int nbMarks = chord.getMarks().size();
                //
                //                            if (nbMarks > 0) {
                //                                Mark mark = chord.getMarks().get(nbMarks - 1);
                //                                logger.debug(
                //                                        "{} Removing final forward: {}",
                //                                        getContextString(),
                //                                        (Rational) mark.getData());
                //                                chord.getMarks().remove(mark);
                //                            } else {
                //                                chord.addError("No final mark to remove in a partial measure");
                //
                //                                return;
                //                            }
                //                        } else {
                //                            addError("No final chord in " + voice);
                //
                //                            return;
                //                        }
                //                    }
                //                } else {
                //                    addError(
                //                            "Non consistent partial measure shortening:" + shortening.opposite() + " "
                //                            + voice + ": " + duration.opposite());
                //
                //                    return;
                //                }
            }
        }
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
        return "{MeasureStack#" + getPageId() + "}";
    }

    //--------------------------//
    // getCurrentDurationString //
    //--------------------------//
    private String getCurrentDurationString ()
    {
        Rational measureDuration = getCurrentDuration();

        //
        //        if (measureDuration.equals(Rational.ZERO) && !wholeRestChords.isEmpty()) {
        //            return "W";
        //        }
        return String.format("%-5s", measureDuration.toString());
    }
}
