//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          M e a s u r e                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.Shape;

import omr.grid.FilamentLine;

import omr.math.Rational;

import omr.score.entity.Page;
import omr.score.entity.TimeSignature.InvalidTimeSignature;

import omr.sig.inter.ChordInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.Inter;
import omr.sig.inter.KeyInter;
import omr.sig.inter.RestChordInter;
import omr.sig.inter.RestInter;
import omr.sig.inter.TimeInter;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code Measure} represents a measure in a system part, it vertically embraces
 * all the staves of the part.
 *
 * @author Hervé Bitteur
 */
public class Measure
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Measure.class);

    //~ Enumerations -------------------------------------------------------------------------------
    public enum Special
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        PICKUP,
        FIRST_HALF,
        SECOND_HALF;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** To flag a dummy measure. */
    private boolean dummy;

    /** The containing part. */
    @Navigable(false)
    private final Part part;

    /** The containing page. */
    @Navigable(false)
    private final Page page;

    /** The containing measure stack. */
    private MeasureStack stack;

    /** (Page-based) measure Id. */
    private Integer id;

    /** Flag for special measure. */
    private Special special;

    /** Left bar-line, if any. */
    private PartBarline leftBarline;

    /** Inside bar-line, if any. */
    private PartBarline insideBarline;

    /** Right bar-line, if any. */
    private PartBarline barline;

    /** Theoretical measure duration, based on current time signature. */
    private Rational expectedDuration;

    /** Actual measure duration, based on durations of contained chords. */
    private Rational actualDuration;

    /** Excess duration, if any. */
    private Rational excess;

    //
    //    /** Sequence of time slots within the measure. */
    //    private final List<Slot> slots = new ArrayList<Slot>();
    //
    /** Voices within this measure, sorted by increasing voice id. */
    private final List<Voice> voices = new ArrayList<Voice>();

    /** Possibly several Clefs per staff. */
    private final List<ClefInter> clefs = new ArrayList<ClefInter>();

    /** Possibly several Key signatures per staff. */
    private final List<KeyInter> keysigs = new ArrayList<KeyInter>();

    /** Potential one Time signature per staff. */
    private final List<TimeInter> timesigs = new ArrayList<TimeInter>();

    /** Possibly several Chords per staff. */
    private final List<ChordInter> chords = new ArrayList<ChordInter>();

    /** Other rhythm inters, relevant for this measure. */
    private final List<Inter> otherTimings = new ArrayList<Inter>();

    /**
     * Chords of just whole rest (thus handled outside time slots).
     * These chords are also contained in 'chords' container, like plain ones
     */
    private final List<ChordInter> wholeChords = new ArrayList<ChordInter>();

    //~ Constructors -------------------------------------------------------------------------------
    //
    //    /** Groups of beams in this measure. */
    //    private final List<BeamGroup> beamGroups = new ArrayList<BeamGroup>();
    //
    /**
     * Creates a new {@code Measure} object.
     *
     * @param part the containing part
     */
    public Measure (Part part)
    {
        this.part = part;
        page = part.getSystem().getPage();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addInter //
    //----------//
    public void addInter (Inter inter)
    {
        if (inter instanceof ChordInter) {
            ChordInter chord = (ChordInter) inter;
            chords.add(chord);

            if (chord.isWholeDuration()) {
                wholeChords.add(chord);
            }
        } else if (inter instanceof ClefInter) {
            clefs.add((ClefInter) inter);
        } else if (inter instanceof KeyInter) {
            keysigs.add((KeyInter) inter);
        } else if (inter instanceof TimeInter) {
            timesigs.add((TimeInter) inter);
        } else if (!otherTimings.contains(inter)) {
            otherTimings.add(inter);
        }
    }

    //----------//
    // addVoice //
    //----------//
    public void addVoice (Voice voice)
    {
        voices.add(voice);
    }

    //--------------//
    // addWholeRest //
    //--------------//
    /**
     * Insert a whole rest, with related chord, on provided staff in this measure.
     *
     * @param staff specified staff in measure
     * @return the whole rest created
     */
    public RestInter addWholeRest (Staff staff)
    {
        RestChordInter chord = new RestChordInter(0);
        chord.setStaff(staff);
        chord.setStartTime(Rational.ZERO);

        RestInter whole = new RestInter(null, Shape.WHOLE_REST, 0, staff, -1);
        chord.addMember(whole);
        stack.addInter(chord);

        Voice.createWholeVoice(chord, this);

        return whole;
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
            if (barline != null) {
                return barline.getRightX(part, staff);
            }

            // Use end of staff
            return staff.getAbscissa(RIGHT);
        }
    }

    //-------------------//
    // getActualDuration //
    //-------------------//
    /**
     * Report the duration of this measure, as computed from its contained voices.
     *
     * @return the (actual) measure duration, or 0 if no rest / note exists in this measure
     */
    public Rational getActualDuration ()
    {
        if (actualDuration != null) {
            return actualDuration;
        } else {
            return Rational.ZERO;
        }
    }

    //------------//
    // getBarline //
    //------------//
    /**
     * Report the bar-line, if any, on desired side of the measure.
     *
     * @param side desired side
     * @return the bar-line found, or null
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
                return prevMeasure.getBarline();
            }

            // Part starting bar?
            if (part.getStartingBarline() != null) {
                return part.getStartingBarline();
            }

            return null; // No bar-line found on LEFT

        default:
        case RIGHT:

            // Measure (right) bar?
            if (barline != null) {
                return barline;
            }

            return null; // No bar-line found on RIGHT
        }
    }

    //------------//
    // getBarline //
    //------------//
    /**
     * Report the ending bar line.
     *
     * @return the ending bar line
     */
    public PartBarline getBarline ()
    {
        return barline;
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

    //---------------//
    // getClefBefore //
    //---------------//
    /**
     * Report the first clef, if any, defined before this measure point
     * (looking in the beginning of the measure, then in previous measures, then in
     * previous systems) while staying in the same logical staff.
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

        // Which (logical) staff we are in
        int staffIndexInPart = part.getStaves().indexOf(staff);

        // Look in all preceding measures, with the same staff id
        Measure measure = this;

        while ((measure = measure.getPrecedingInPage()) != null) {
            clef = measure.getLastMeasureClef(staffIndexInPart);

            if (clef != null) {
                return clef;
            }
        }

        return null; // No clef previously defined
    }

    //-------------------------//
    // getCurrentTimeSignature //
    //-------------------------//
    /**
     * Report the time signature which applies in this measure, whether a time signature
     * actually starts this measure in whatever staff, or whether a time signature was
     * found in a previous measure, even in preceding pages.
     * <p>
     * <b>NOTA</b>This method looks up for time sig in preceding pages as well</p>
     *
     * @return the current time signature, or null if not found at all
     */
    public TimeInter getCurrentTimeSignature ()
    {
        // Backward from this measure to the beginning of the score
        Measure measure = this;
        Page page = getPage();

        while (measure != null) {
            // Check in the measure
            TimeInter ts = measure.getTimeSignature();

            if (ts != null) {
                return ts;
            }

            // Move to preceding measure (same part)
            measure = measure.getPrecedingInPage();

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

    //-----------//
    // getExcess //
    //-----------//
    /**
     * Report the excess duration of this measure, if any.
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
     * Report the theoretical duration of this measure, based on
     * current time signature.
     *
     * @return the expected measure duration
     * @throws InvalidTimeSignature
     */
    public Rational getExpectedDuration ()
            throws InvalidTimeSignature
    {
        try {
            if (expectedDuration == null) {
                int numerator;
                int denominator;
                TimeInter ts = getCurrentTimeSignature();

                if (ts != null) {
                    numerator = ts.getNumerator();
                    denominator = ts.getDenominator();
                } else {
                    numerator = 4;
                    denominator = 4;
                }

                expectedDuration = new Rational(numerator, denominator);
            }

            return expectedDuration;
        } catch (NullPointerException npe) {
            throw new InvalidTimeSignature();
        }
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

    //--------------------//
    // getFirstMeasureKey //
    //--------------------//
    /**
     * Report the first key signature (if any) in this measure, if tagged with the
     * specified staff index.
     *
     * @param staffIndexInPart the imposed part-based staff index
     * @return the first key signature, or null
     */
    public KeyInter getFirstMeasureKey (int staffIndexInPart)
    {
        for (KeyInter key : keysigs) {
            if (key.getStaff().getIndexInPart() == staffIndexInPart) {
                return key;
            }
        }

        return null;
    }

    //--------------------//
    // getLastMeasureClef //
    //--------------------//
    /**
     * Report the last clef (if any) in this measure, if tagged with the specified staff
     * index.
     *
     * @param staffIndexInPart the imposed part-based staff index
     * @return the last clef, or null
     */
    public ClefInter getLastMeasureClef (int staffIndexInPart)
    {
        // Going backwards
        for (int ic = clefs.size() - 1; ic >= 0; ic--) {
            ClefInter clef = clefs.get(ic);

            if (clef.getStaff().getIndexInPart() == staffIndexInPart) {
                return clef;
            }
        }

        return null;
    }

    //-------------------//
    // getLastMeasureKey //
    //-------------------//
    /**
     * Report the last key signature (if any) in this measure, if tagged with the
     * specified staff index.
     *
     * @param staffIndexInPart the imposed part-based staff index
     * @return the last key signature, or null
     */
    public KeyInter getLastMeasureKey (int staffIndexInPart)
    {
        // Going backwards
        for (int ik = keysigs.size() - 1; ik >= 0; ik--) {
            KeyInter key = keysigs.get(ik);

            if (key.getStaff().getIndexInPart() == staffIndexInPart) {
                return key;
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
        if (staff == null) {
            throw new IllegalArgumentException("Staff is null");
        }

        // Look in this measure, with same staff, going backwards
        for (int ic = clefs.size() - 1; ic >= 0; ic--) {
            ClefInter clef = clefs.get(ic);

            if ((clef.getStaff() == staff) && (clef.getCenter().x <= point.x)) {
                return clef;
            }
        }

        return null; // No clef previously defined in this measure and staff
    }

    //---------//
    // getPage //
    //---------//
    public Page getPage ()
    {
        return page;
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
                List<FilamentLine> lines = staff.getLines();
                FilamentLine midLine = lines.get(lines.size() / 2);
                int x = staff.getAbscissa(LEFT);

                return new Point(x, midLine.yAt(x));
            }

        default:
        case RIGHT:

            // Measure (right) bar?
            if (barline != null) {
                return barline.getBarline(part, staff).getRightBar().getCenter();
            }
            // No bar, use end of staff
             {
                List<FilamentLine> lines = staff.getLines();
                FilamentLine midLine = lines.get(lines.size() / 2);
                int x = staff.getAbscissa(RIGHT);

                return new Point(x, midLine.yAt(x));
            }
        }
    }

    //------------//
    // getSpecial //
    //------------//
    public Special getSpecial ()
    {
        return special;
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

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature in this measure for the specified staff index.
     *
     * @param staffIndexInPart imposed part-based staff index
     * @return the time signature, or null if not found
     */
    public TimeInter getTimeSignature (int staffIndexInPart)
    {
        for (TimeInter ts : timesigs) {
            if (ts.getStaff().getId() == staffIndexInPart) {
                return ts;
            }
        }

        return null; // Not found
    }

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature in this measure
     * (whatever the staff).
     *
     * @return the time signature, or null if not found
     */
    public TimeInter getTimeSignature ()
    {
        if (!timesigs.isEmpty()) {
            return timesigs.get(0);
        }

        return null; // Not found
    }

    //-----------//
    // getVoices //
    //-----------//
    public List<Voice> getVoices ()
    {
        return voices;
    }

    //-----------------//
    // getVoicesNumber //
    //-----------------//
    /**
     * Report the number of voices in this measure.
     *
     * @return the number of voices computed
     */
    public int getVoicesNumber ()
    {
        return voices.size();
    }

    /**
     * @return the wholeChords
     */
    public List<ChordInter> getWholeChords ()
    {
        return wholeChords;
    }

    //---------//
    // isDummy //
    //---------//
    public boolean isDummy ()
    {
        return dummy;
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
        keysigs.addAll(right.keysigs);
        timesigs.addAll(right.timesigs);
        chords.addAll(right.chords);
        ///beamGroups.addAll(right.beamGroups);

        ///        slots.addAll(right.slots);
        ///beamGroups.addAll(right.beamGroups);
        wholeChords.addAll(right.wholeChords);
        voices.addAll(right.voices);

        insideBarline = barline;
        setBarline(right.barline);
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
        replicate.id = id;
        replicate.special = special;

        return replicate;
    }

    //-------------------//
    // setActualDuration //
    //-------------------//
    /**
     * Register in this measure its actual duration.
     *
     * @param actualDuration the duration value
     */
    public void setActualDuration (Rational actualDuration)
    {
        this.actualDuration = actualDuration;
    }

    //------------//
    // setBarline //
    //------------//
    /**
     * Assign the (right) bar-line that ends this measure
     *
     * @param barline the right bar-line
     */
    public void setBarline (PartBarline barline)
    {
        this.barline = barline;
    }

    //----------//
    // setDummy //
    //----------//
    public void setDummy ()
    {
        dummy = true;
    }

    //-----------//
    // setExcess //
    //-----------//
    /**
     * Assign an excess duration for this measure.
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
     * Assign the proper page-based id value to this measure.
     *
     * @param id the proper page-based measure id value
     */
    public void setIdValue (int id)
    {
        this.id = id;
    }

    //----------------//
    // setLeftBarline //
    //----------------//
    public void setLeftBarline (PartBarline leftBarline)
    {
        this.leftBarline = leftBarline;
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

    //---------//
    // shorten //
    //---------//
    /**
     * Flag this measure as partial (shorter than expected duration).
     *
     * @param shortening how much the measure duration is to be reduced
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
        return "{Measure#" + stack.getPageId() + "P" + part.getId() + "}";
    }

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
    //                for (ChordInter chord : slot.getChords()) {
    //                    Rational chordEnd = slot.getStartTime().plus(chord.getDuration());
    //
    //                    if (chordEnd.compareTo(measureDuration) > 0) {
    //                        measureDuration = chordEnd;
    //                    }
    //                }
    //            }
    //        }
    //
    //        if (measureDuration.equals(Rational.ZERO) && !wholeChords.isEmpty()) {
    //            return "W";
    //        }
    //
    //        return String.format("%-5s", measureDuration.toString());
    //    }
    //    //----------//
    //    // addGroup //
    //    //----------//
    //    /**
    //     * Add a beam group to this measure.
    //     *
    //     * @param group a beam group to add
    //     */
    //    public void addGroup (BeamGroup group)
    //    {
    //        beamGroups.add(group);
    //    }
    //
    //
    //    //---------------//
    //    // getBeamGroups //
    //    //---------------//
    //    /**
    //     * Report the collection of beam groups.
    //     *
    //     * @return the list of beam groups
    //     */
    //    public List<BeamGroup> getBeamGroups ()
    //    {
    //        return beamGroups;
    //    }
    //
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
    //    public RestInter lookupRest (ChordInter left,
    //                                 ChordInter right)
    //    {
    //        // Define the area limited by the left and right chords with their stems
    //        // and check for intersection with a rest note
    //        Polygon polygon = new Polygon();
    //        polygon.addPoint(left.getHeadLocation().x, left.getHeadLocation().y);
    //        polygon.addPoint(left.getTailLocation().x, left.getTailLocation().y);
    //        polygon.addPoint(right.getTailLocation().x, right.getTailLocation().y);
    //        polygon.addPoint(right.getHeadLocation().x, right.getHeadLocation().y);
    //
    //        for (ChordInter chord : chords) {
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
}
