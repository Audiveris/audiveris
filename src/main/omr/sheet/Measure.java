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
 * all the staves (usually 1 or 2) of the containing part.
 *
 * @author Hervé Bitteur
 */
public class Measure
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Measure.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** To flag a dummy measure. */
    private boolean dummy;

    /** The containing part. */
    @Navigable(false)
    private final Part part;

    /** The containing measure stack. */
    private MeasureStack stack;

    /** Left bar-line, if any. */
    private PartBarline leftBarline;

    /** Inside bar-line, if any. */
    private PartBarline insideBarline;

    /** Right bar-line, if any. */
    private PartBarline barline;

    /** Voices within this measure, sorted by increasing voice id. */
    private final List<Voice> voices = new ArrayList<Voice>();

    /** Possibly several Clefs per staff. */
    private final List<ClefInter> clefs = new ArrayList<ClefInter>();

    /** Possibly several Key signatures per staff. */
    private final List<KeyInter> keySigs = new ArrayList<KeyInter>();

    /** Potential one Time signature per staff. */
    private final List<TimeInter> timeSigs = new ArrayList<TimeInter>();

    /** Possibly several Chords per staff. */
    private final List<ChordInter> chords = new ArrayList<ChordInter>();

    /**
     * Chords of just whole rest (thus handled outside time slots).
     * These chords are also contained in 'chords' container, like plain ones
     */
    private final List<ChordInter> wholeChords = new ArrayList<ChordInter>();

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

    //~ Methods ------------------------------------------------------------------------------------
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
        wholeChords.add(chord);

        Voice.createWholeVoice(chord, this);

        return whole;
    }

    //----------//
    // addInter //
    //----------//
    public void addInter (Inter inter)
    {
        if (inter instanceof ChordInter) {
            ChordInter chord = (ChordInter) inter;
            chords.add(chord);

            if (chord.isWholeRest()) {
                wholeChords.add(chord);
            }
        } else if (inter instanceof ClefInter) {
            clefs.add((ClefInter) inter);
        } else if (inter instanceof KeyInter) {
            keySigs.add((KeyInter) inter);
        } else if (inter instanceof TimeInter) {
            timeSigs.add((TimeInter) inter);
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

    //----------//
    // getClefs //
    //----------//
    /**
     * @return the clefs
     */
    public List<ClefInter> getClefs ()
    {
        return clefs;
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
        for (KeyInter key : keySigs) {
            if (key.getStaff().getIndexInPart() == staffIndexInPart) {
                return key;
            }
        }

        return null;
    }

    //--------------//
    // getKeyBefore //
    //--------------//
    /**
     * Report the key signature which applies in this measure, whether a key signature
     * actually starts this measure in the same staff, or whether a key signature was
     * found in a previous measure, for the same staff.
     *
     * @param point the point before which to look
     * @param staff the containing staff (cannot be null)
     * @return the current key signature, or null if not found
     */
    public KeyInter getKeyBefore (Point point,
                                  Staff staff)
    {
        if (point == null) {
            throw new NullPointerException();
        }

        int staffIndexInPart = staff.getIndexInPart();

        // Look in this measure, with same staff, going backwards
        // TODO: make sure keysigs is sorted by abscissa !!!!!
        for (int ik = keySigs.size() - 1; ik >= 0; ik--) {
            final KeyInter ks = keySigs.get(ik);

            if ((ks.getStaff() == staff) && (ks.getCenter().x < point.x)) {
                return ks;
            }
        }

        // Look in previous measures in the system part and the preceding ones
        Measure measure = this;

        while ((measure = measure.getPrecedingInPage()) != null) {
            final KeyInter ks = measure.getLastMeasureKey(staffIndexInPart);

            if (ks != null) {
                return ks;
            }
        }

        return null; // Not found (in this page)
    }

    //-----------------//
    // getKeySignature //
    //-----------------//
    /**
     * Report the potential key signature in this measure (whatever the staff).
     *
     * @return the measure key signature, or null if not found
     */
    public KeyInter getKeySignature ()
    {
        if (!keySigs.isEmpty()) {
            return keySigs.get(0);
        }

        return null; // Not found
    }

    //-----------------//
    // getKeySignature //
    //-----------------//
    /**
     * Report the potential key signature in this measure for the specified staff index.
     *
     * @param staffIndexInPart imposed part-based staff index
     * @return the staff key signature, or null if not found
     */
    public KeyInter getKeySignature (int staffIndexInPart)
    {
        for (KeyInter ks : keySigs) {
            if (ks.getStaff().getId() == staffIndexInPart) {
                return ks;
            }
        }

        return null; // Not found
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
        for (int ik = keySigs.size() - 1; ik >= 0; ik--) {
            KeyInter key = keySigs.get(ik);

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
     * Report the potential time signature in this measure (whatever the staff).
     *
     * @return the measure time signature, or null if not found
     */
    public TimeInter getTimeSignature ()
    {
        if (!timeSigs.isEmpty()) {
            return timeSigs.get(0);
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
     * Check whether the provided chord is a measure rest.
     *
     * @param chord the provided chord
     * @return true if chord is actually a measure rest, false otherwise
     */
    public boolean isMeasureRest (ChordInter chord)
    {
        Inter noteInter = chord.getMembers().get(0);
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
        List<Inter> staffChords = filterByStaff(chords, chord.getStaff());

        return staffChords.size() == 1;
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
        keySigs.addAll(right.keySigs);
        timeSigs.addAll(right.timeSigs);
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

        return replicate;
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

    //----------------//
    // setLeftBarline //
    //----------------//
    public void setLeftBarline (PartBarline leftBarline)
    {
        this.leftBarline = leftBarline;
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
    private List<Inter> filterByStaff (List<? extends Inter> inters,
                                       Staff staff)
    {
        List<Inter> found = new ArrayList<Inter>();

        for (Inter inter : inters) {
            if (inter.getStaff() == staff) {
                found.add(inter);
            }
        }

        return found;
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
