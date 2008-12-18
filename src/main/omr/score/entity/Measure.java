//----------------------------------------------------------------------------//
//                                                                            //
//                               M e a s u r e                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.glyph.Glyph;

import omr.log.Logger;

import omr.score.common.SystemPoint;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.visitor.ScoreVisitor;

import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>Measure</code> handles a measure of a system part, that is all
 * entities within the same measure time frame, for all staves that compose the
 * system part.
 *
 * <p>As a ScoreNode, the children of a Measure are : Barline, TimeSignature,
 * list of Clef(s), list of KeySignature(s), list of Chord(s) and list of
 * Beam(s).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Measure
    extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Measure.class);

    //~ Instance fields --------------------------------------------------------

    /** To flag dummy barline instances */
    private boolean dummy;

    /** To flag a temporary measure */
    private boolean temporary;

    /** Flag for implicit (introduction) measure */
    private boolean implicit;

    /** Flag for partial (short) measure */
    private boolean partial;

    /** Child: Ending bar line */
    private Barline barline;

    /** Child: Potential one time signature per staff */
    private Container timesigs;

    /** Children: possibly several clefs per staff */
    private Container clefs;

    /** Children: possibly several KeySignature's per staff */
    private Container keysigs;

    /** Children: possibly several Chord's per staff */
    private Container chords;

    /** Children: possibly several Beam's per staff */
    private Container beams;

    /** Left abscissa (in units, wrt system left side) of this measure */
    private Integer leftX;

    /** Measure Id */
    private int id;

    /** Identified time slots within the measure */
    private SortedSet<Slot> slots;

    /** Chords of just whole rest (thus handled outside slots) */
    private List<Chord> wholeChords;

    /** Groups of beams in this measure */
    private List<BeamGroup> beamGroups;

    /** Start time of this measure since beginning of the system */
    private Integer startTime;

    /** Theoretical measure duration */
    private Integer expectedDuration;

    /** Actual measure duration */
    private Integer actualDuration;

    /** Flag to indicate a excess duration */
    private Integer excess;

    /** Voices within this measure, sorted by increasing voice id */
    private List<Voice> voices;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Measure //
    //---------//
    /**
     * Create a measure with the specified parameters
     *
     * @param part the containing system part
     */
    public Measure (SystemPart part)
    {
        super(part);
        cleanupNode();
    }

    //---------//
    // Measure //
    //---------//
    /**
     * Default constructor
     */
    private Measure ()
    {
        super(null);
        cleanupNode();
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // setActualDuration //
    //-------------------//
    /**
     * Register in this measure its actual duration
     *
     * @param actualDuration the duration value
     */
    public void setActualDuration (int actualDuration)
    {
        this.actualDuration = actualDuration;
    }

    //-------------------//
    // getActualDuration //
    //-------------------//
    /**
     * Report the duration of this measure, as computed from its contained
     * voices
     *
     * @return the (actual) measure duration, or 0 if no rest / note exist in
     * this measure
     */
    public Integer getActualDuration ()
    {
        if (actualDuration != null) {
            return actualDuration;
        } else {
            ///logger.warning(getContextString() + " no actual duration");
            return 0;
        }
    }

    //------------//
    // getBarline //
    //------------//
    /**
     * Report the ending bar line
     *
     * @return the ending bar line
     */
    public Barline getBarline ()
    {
        return barline;
    }

    //---------------//
    // getBeamGroups //
    //---------------//
    /**
     * Report the collection of beam groups
     *
     * @return the list of beam groups
     */
    public List<BeamGroup> getBeamGroups ()
    {
        return beamGroups;
    }

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the collection of beams
     *
     * @return the list of beams
     */
    public List<TreeNode> getBeams ()
    {
        return beams.getChildren();
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the collection of chords
     *
     * @return the list of chords
     */
    public List<TreeNode> getChords ()
    {
        return chords.getChildren();
    }

    //----------------//
    // getChordsAbove //
    //----------------//
    /**
     * Report the collection of chords whose head is located above the provided
     * point
     * @param point the provided point
     * @return the (perhaps empty) collection of chords
     */
    public Collection<Chord> getChordsAbove (SystemPoint point)
    {
        Collection<Chord> found = new ArrayList<Chord>();

        for (TreeNode node : getChords()) {
            Chord chord = (Chord) node;

            if (chord.getHeadLocation().y < point.y) {
                found.add(chord);
            }
        }

        return found;
    }

    //----------------//
    // getChordsBelow //
    //----------------//
    /**
     * Report the collection of chords whose head is located below the provided
     * point
     * @param point the provided point
     * @return the (perhaps empty) collection of chords
     */
    public Collection<Chord> getChordsBelow (SystemPoint point)
    {
        Collection<Chord> found = new ArrayList<Chord>();

        for (TreeNode node : getChords()) {
            Chord chord = (Chord) node;

            if (chord.getHeadLocation().y > point.y) {
                found.add(chord);
            }
        }

        return found;
    }

    //--------------//
    // getClefAfter //
    //--------------//
    /**
     * Report the first clef, if any, defined after this measure point
     * (looking in end of the measure, then in next measures, then in
     * next systems) while staying in the same logical staff
     *
     * @param point the point after which to look
     * @return the first clef defined, or null
     */
    public Clef getClefAfter (SystemPoint point)
    {
        // Which staff we are in
        Clef clef = null;
        int  staffId = getPart()
                           .getStaffAt(point)
                           .getId();

        // Look in this measure, with same staff, going forward
        for (TreeNode cn : getClefs()) {
            clef = (Clef) cn;

            if ((clef.getStaff()
                     .getId() == staffId) &&
                (clef.getCenter().x >= point.x)) {
                return clef;
            }
        }

        // Look in all following measures, with the same staff id
        Measure measure = this;

        while ((measure = measure.getFollowing()) != null) {
            clef = measure.getFirstMeasureClef(staffId);

            if (clef != null) {
                return clef;
            }
        }

        return null; // No clef later defined
    }

    //---------------//
    // getClefBefore //
    //---------------//
    /**
     * Report the latest clef, if any, defined before this measure point
     * (looking in beginning of the measure, then in previous measures, then in
     * previous systems) while staying in the same logical staff
     *
     * @param point the point before which to look
     * @return the latest clef defined, or null
     */
    public Clef getClefBefore (SystemPoint point)
    {
        return getClefBefore(point, null);
    }

    //---------------//
    // getClefBefore //
    //---------------//
    /**
     * Same functionally than the other method, but with a staff provided
     *
     * @param point the point before which to look
     * @param staff the containing staff (if null, it is derived from center.y)
     * @return the latest clef defined, or null
     */
    public Clef getClefBefore (SystemPoint point,
                               Staff       staff)
    {
        Clef clef = null;

        // Which staff we are in
        int staffId = (staff != null) ? staff.getId()
                      : getPart()
                            .getStaffAt(point)
                            .getId();

        // Look in this measure, with same staff, going backwards
        for (int ic = getClefs()
                          .size() - 1; ic >= 0; ic--) {
            clef = (Clef) getClefs()
                              .get(ic);

            if ((clef.getStaff()
                     .getId() == staffId) &&
                (clef.getCenter().x <= point.x)) {
                return clef;
            }
        }

        // Look in all preceding measures, with the same staff id
        Measure measure = this;

        while ((measure = measure.getPreceding()) != null) {
            clef = measure.getLastMeasureClef(staffId);

            if (clef != null) {
                return clef;
            }
        }

        return null; // No clef previously defined
    }

    //-------------//
    // getClefList //
    //-------------//
    /**
     * Report the node that collect the clefs
     *
     * @return the clef list node
     */
    public Container getClefList ()
    {
        return clefs;
    }

    //----------//
    // getClefs //
    //----------//
    /**
     * Report the collection of clefs
     *
     * @return the list of clefs
     */
    public List<TreeNode> getClefs ()
    {
        return clefs.getChildren();
    }

    //-----------------//
    // getClosestChord //
    //-----------------//
    /**
     * From a provided Chord collection, report the chord which has the closest
     * abscissa to a provided point
     *
     * @param chords the collection of chords to browse
     * @param point the reference point
     * @return the abscissa-wise closest chord
     */
    public Chord getClosestChord (Collection<Chord> chords,
                                  SystemPoint       point)
    {
        Chord bestChord = null;
        int   bestDx = Integer.MAX_VALUE;

        for (Chord chord : chords) {
            int dx = Math.abs(chord.getHeadLocation().x - point.x);

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
     * Report the time slot which has the closest abscissa to a provided point
     *
     * @param point the reference point
     * @return the abscissa-wise closest slot
     */
    public Slot getClosestSlot (SystemPoint point)
    {
        Slot bestSlot = null;
        int  bestDx = Integer.MAX_VALUE;

        for (Slot slot : getSlots()) {
            int dx = Math.abs(slot.getX() - point.x);

            if (dx < bestDx) {
                bestDx = dx;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    //-------------------------//
    // getCurrentTimeSignature //
    //-------------------------//
    /**
     * Report the time signature which applies in this measure, whether a time
     * signature actually starts this measure in whatever staff, or whether a
     * time signature was found in a previous measure.
     *
     * @return the current time signature, or null if not found
     */
    public TimeSignature getCurrentTimeSignature ()
    {
        TimeSignature ts = null;

        // Look in this measure
        ts = getTimeSignature();

        if (ts != null) {
            return ts;
        }

        // Look in preceding measures in this system/part and before
        Measure measure = this;

        while ((measure = measure.getPreceding()) != null) {
            ts = measure.getTimeSignature();

            if (ts != null) {
                return ts;
            }
        }

        return null; // Not found !!!
    }

    //---------//
    // isDummy //
    //---------//
    public boolean isDummy ()
    {
        return dummy;
    }

    //---------------//
    // getEventChord //
    //---------------//
    /**
     * Retrieve the most suitable chord to connect the event point to
     *
     * @param point the system-based location
     * @return the most suitable chord, or null
     */
    public Chord getEventChord (SystemPoint point)
    {
        // Choose the x-closest slot
        Slot slot = getClosestSlot(point);

        if (slot != null) {
            // Choose the y-closest staff
            Staff staff = getPart()
                              .getStaffAt(point);

            int   staffY = staff.getTopLeft().y - getSystem()
                                                      .getTopLeft().y +
                           (staff.getHeight() / 2);

            if (staffY <= point.y) {
                return slot.getChordAbove(point);
            } else {
                return slot.getChordBelow(point);
            }
        } else {
            return null;
        }
    }

    //-----------//
    // setExcess //
    //-----------//
    /**
     * Assign an excess duration for this measure
     *
     * @param excess the duration in excess
     */
    public void setExcess (Integer excess)
    {
        this.excess = excess;
    }

    //-----------//
    // getExcess //
    //-----------//
    /**
     * Report the excess duration of this measure, if any
     *
     * @return the duration in excess, or null
     */
    public Integer getExcess ()
    {
        return excess;
    }

    //---------------------//
    // getExpectedDuration //
    //---------------------//
    /**
     * Report the theoretical duration of this measure, based on current time
     * signature
     *
     * @return the expected measure duration
     * @throws InvalidTimeSignature
     */
    public int getExpectedDuration ()
        throws InvalidTimeSignature
    {
        try {
            if (expectedDuration == null) {
                int           numerator;
                int           denominator;
                TimeSignature ts = getCurrentTimeSignature();

                if (ts != null) {
                    numerator = ts.getNumerator();
                    denominator = ts.getDenominator();
                } else {
                    numerator = 4;
                    denominator = 4;
                }

                expectedDuration = (4 * Note.QUARTER_DURATION * numerator) / denominator;
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
     * Report the first clef (if any) in this measure, tagged  with the provided
     * staff
     *
     * @param staffId the imposed related staff id
     * @return the first clef, or null
     */
    public Clef getFirstMeasureClef (int staffId)
    {
        // Going forward
        for (TreeNode cn : getClefs()) {
            Clef clef = (Clef) cn;

            if (clef.getStaff()
                    .getId() == staffId) {
                return clef;
            }
        }

        return null;
    }

    //--------------//
    // getFollowing //
    //--------------//
    /**
     * Report the following measure of this one, either in this system / part,
     * or in the following system /part.
     *
     * @return the following measure, or null if none
     */
    public Measure getFollowing ()
    {
        Measure nextMeasure = (Measure) getNextSibling();

        if (nextMeasure != null) {
            return nextMeasure;
        }

        SystemPart followingPart = getPart()
                                       .getFollowing();

        if (followingPart != null) {
            return followingPart.getFirstMeasure();
        } else {
            return null;
        }
    }

    //-------//
    // setId //
    //-------//
    /**
     * Assign the proper id to this measure
     *
     * @param id the proper measure id
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the measure id
     *
     * @return the measure id
     */
    public int getId ()
    {
        return id;
    }

    //-------------//
    // setImplicit //
    //-------------//
    /**
     * Flag this measure as implicit
     */
    public void setImplicit ()
    {
        implicit = true;
    }

    //------------//
    // isImplicit //
    //------------//
    /**
     * Report whether this measure is implicit
     *
     * @return true if measure is implicit
     */
    public boolean isImplicit ()
    {
        return implicit;
    }

    //--------------//
    // getKeyBefore //
    //--------------//
    /**
     * Report the key signature which applies in this measure, whether a key
     * signature actually starts this measure in the same staff, or whether a
     * key signature was found in a previous measure, for the same staff.
     *
     * @param point the point before which to look
     * @return the current key signature, or null if not found
     */
    public KeySignature getKeyBefore (SystemPoint point)
    {
        if (point == null) {
            throw new NullPointerException();
        }

        KeySignature ks = null;
        int          staffId = getPart()
                                   .getStaffAt(point)
                                   .getId();

        // Look in this measure, with same staff, going backwards
        for (int ik = getKeySignatures()
                          .size() - 1; ik >= 0; ik--) {
            ks = (KeySignature) getKeySignatures()
                                    .get(ik);

            if ((ks.getStaff()
                   .getId() == staffId) &&
                (ks.getCenter().x < point.x)) {
                return ks;
            }
        }

        // Look in previous measures in the system part and the preceding ones
        Measure measure = this;

        while ((measure = measure.getPreceding()) != null) {
            ks = measure.getLastMeasureKey(staffId);

            if (ks != null) {
                return ks;
            }
        }

        return null; // Not found !!!
    }

    //---------------//
    // getKeySigList //
    //---------------//
    /**
     * Report the list that collects the KeySignature instances
     *
     * @return the single instance of KeySigList
     */
    public Container getKeySigList ()
    {
        return keysigs;
    }

    //------------------//
    // getKeySignatures //
    //------------------//
    /**
     * Report the collection of KeySignature's
     *
     * @return the list of KeySignature's
     */
    public List<TreeNode> getKeySignatures ()
    {
        return keysigs.getChildren();
    }

    //--------------------//
    // getLastMeasureClef //
    //--------------------//
    /**
     * Report the last clef (if any) in this measure, tagged  with the provided
     * staff
     *
     * @param staffId the imposed related staff id
     * @return the last clef, or null
     */
    public Clef getLastMeasureClef (int staffId)
    {
        // Going backwards
        for (int ic = getClefs()
                          .size() - 1; ic >= 0; ic--) {
            Clef clef = (Clef) getClefs()
                                   .get(ic);

            if (clef.getStaff()
                    .getId() == staffId) {
                return clef;
            }
        }

        return null;
    }

    //-------------------//
    // getLastMeasureKey //
    //-------------------//
    /**
     * Report the last key signature (if any) in this measure, tagged with the
     * provided staff
     *
     * @param staffId the imposed related staff id
     * @return the last key signature, or null
     */
    public KeySignature getLastMeasureKey (int staffId)
    {
        // Going backwards
        for (int ik = getKeySignatures()
                          .size() - 1; ik >= 0; ik--) {
            KeySignature key = (KeySignature) getKeySignatures()
                                                  .get(ik);

            if (key.getStaff()
                   .getId() == staffId) {
                return key;
            }
        }

        return null;
    }

    //------------------//
    // getLastSoundTime //
    //------------------//
    /**
     * Report the time, counted from beginning of this measure, when sound stops
     * which means that ending rests are not counted.
     *
     * @return the relative time of last Midi "note off" in this measure
     */
    public int getLastSoundTime ()
    {
        int lastTime = 0;

        for (TreeNode chordNode : getChords()) {
            Chord chord = (Chord) chordNode;

            if (!chord.isAllRests()) {
                int time = chord.getStartTime() + chord.getDuration();

                if (time > lastTime) {
                    lastTime = time;
                }
            }
        }

        return lastTime;
    }

    //----------//
    // setLeftX //
    //----------//
    public void setLeftX (int val)
    {
        leftX = new Integer(val);
    }

    //----------//
    // getLeftX //
    //----------//
    /**
     * Report the abscissa of the start of the measure, relative to system/part
     * left edge (so 0 for first measure in the part)
     *
     * @return part-based abscissa of left side of the measure
     */
    public Integer getLeftX ()
    {
        if (leftX == null) {
            // Start of the measure
            Measure prevMeasure = (Measure) getPreviousSibling();

            if (prevMeasure == null) { // Very first measure in the staff
                leftX = 0;
            } else {
                leftX = prevMeasure.getBarline()
                                   .getCenter().x;
            }
        }

        return leftX;
    }

    //------------//
    // setPartial //
    //------------//
    /**
     * Flag this measure as partial (shorter than expected duration)
     *
     * @param shortening how much the measure duration is to be reduced
     */
    public void setPartial (int shortening)
    {
        // Remove any final forward mark consistent with the shortening
        for (Voice voice : voices) {
            Integer duration = voice.getFinalDuration();

            if (duration != null) {
                if (duration == shortening) {
                    if (!voice.isWhole()) {
                        Chord chord = voice.getLastChord();

                        if (chord != null) {
                            int nbMarks = chord.getMarks()
                                               .size();

                            if (nbMarks > 0) {
                                Mark mark = chord.getMarks()
                                                 .get(nbMarks - 1);

                                if (logger.isFineEnabled()) {
                                    logger.fine(
                                        getContextString() +
                                        " Removing final forward: " +
                                        Note.quarterValueOf(
                                            (Integer) mark.getData()));
                                }

                                chord.getMarks()
                                     .remove(mark);
                            } else {
                                chord.addError(
                                    "No final mark to remove in a partial measure");

                                return;
                            }
                        } else {
                            addError("No final chord in " + voice);

                            return;
                        }
                    }
                } else {
                    addError(
                        "Non consistent partial measure shortening:" +
                        Note.quarterValueOf(-shortening) + " " + voice + ": " +
                        Note.quarterValueOf(-duration));

                    return;
                }
            }
        }

        setPartial(true);
    }

    //-----------//
    // isPartial //
    //-----------//
    /**
     * Report whether this measure is partial
     *
     * @return true if measure is partial
     */
    public boolean isPartial ()
    {
        return partial;
    }

    //--------------//
    // getPreceding //
    //--------------//
    /**
     * Report the preceding measure of this one, either in this system / part,
     * or in the preceding system /part.
     *
     * @return the preceding measure, or null if none
     */
    public Measure getPreceding ()
    {
        Measure prevMeasure = (Measure) getPreviousSibling();

        if (prevMeasure != null) {
            return prevMeasure;
        }

        SystemPart precedingPart = getPart()
                                       .getPreceding();

        if (precedingPart != null) {
            return precedingPart.getLastMeasure();
        } else {
            return null;
        }
    }

    //----------//
    // getSlots //
    //----------//
    /**
     * Report the ordered collection of slots
     *
     * @return the collection of slots
     */
    public SortedSet<Slot> getSlots ()
    {
        return slots;
    }

    //--------------//
    // getStartTime //
    //--------------//
    /**
     * Report the start time of the measure, relative to system/part beginning
     *
     * @return part-based start time of the measure
     */
    public Integer getStartTime ()
    {
        if (startTime == null) {
            // Start of this measure is the end of the previous one
            Measure prevMeasure = (Measure) getPreviousSibling();

            if (prevMeasure == null) { // Very first measure in the part
                startTime = 0;
            } else {
                startTime = prevMeasure.getStartTime() +
                            prevMeasure.getActualDuration();
            }

            if (logger.isFineEnabled()) {
                logger.fine(
                    getContextString() + " id=" + this.getId() + " startTime=" +
                    startTime);
            }
        }

        return startTime;
    }

    //----------------//
    // getTimeSigList //
    //----------------//
    /**
     * Report the node that collects the TimeSignature instances
     *
     * @return the node of TimeSignature instances
     */
    public Container getTimeSigList ()
    {
        return timesigs;
    }

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature in this measure for the related staff
     *
     * @param staff the related staff
     * @return the time signature, or null if not found
     */
    public TimeSignature getTimeSignature (Staff staff)
    {
        for (TreeNode node : timesigs.getChildren()) {
            TimeSignature ts = (TimeSignature) node;

            if (ts.getStaff() == staff) {
                return ts;
            }
        }

        return null; // Not found
    }

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature in this measure (whatever the staff)
     *
     * @return the time signature, or null if not found
     */
    public TimeSignature getTimeSignature ()
    {
        for (TreeNode node : timesigs.getChildren()) {
            return (TimeSignature) node;
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
     * Report the number of voices in this measure
     *
     * @return the number fo voices computed
     */
    public int getVoicesNumber ()
    {
        return voices.size();
    }

    //----------------//
    // getWholeChords //
    //----------------//
    /**
     * Report the collection of whole chords
     *
     * @return the whole chords of this measure
     */
    public Collection<Chord> getWholeChords ()
    {
        return wholeChords;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width, in units, of the measure
     *
     * @return the measure width, or null in case of dummy measure
     */
    public Integer getWidth ()
    {
        if (isDummy()) {
            return null;
        } else {
            return getBarline()
                       .getCenter().x - getLeftX();
        }
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addChild //
    //----------//
    /**
     * Override normal behavior, so that a given child is stored in its proper
     * type collection (clef to clef list, etc...)
     *
     * @param node the child to insert into the measure
     */
    @Override
    public void addChild (TreeNode node)
    {
        // Special children lists
        if (node instanceof Clef) {
            clefs.addChild(node);
        } else if (node instanceof TimeSignature) {
            timesigs.addChild(node);
        } else if (node instanceof KeySignature) {
            keysigs.addChild(node);
        } else if (node instanceof Beam) {
            beams.addChild(node);
        } else if (node instanceof Chord) {
            chords.addChild(node);
        } else {
            super.addChild(node);
        }

        // Side effect for barline
        if (node instanceof Barline) {
            barline = (Barline) node; // Ending barline
        }
    }

    //----------//
    // addGroup //
    //----------//
    /**
     * Add a beam goup to this measure
     *
     * @param group a beam group to add
     */
    public void addGroup (BeamGroup group)
    {
        beamGroups.add(group);
    }

    //----------//
    // addVoice //
    //----------//
    public void addVoice (Voice voice)
    {
        voices.add(voice);
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

    //----------------------//
    // checkPartialMeasures //
    //----------------------//
    /**
     * Check for measures for which all voices are shorter than the expected
     * duration, and for the same duration, across all parts of the system
     *
     * @param system the system to inspect
     */
    public static void checkPartialMeasures (ScoreSystem system)
    {
        // Use a loop on measures, across system parts
        final int imMax = system.getFirstRealPart()
                                .getMeasures()
                                .size();

        for (int im = 0; im < imMax; im++) {
            Integer measureFinal = null;
            partLoop: 
            for (TreeNode node : system.getParts()) {
                SystemPart part = (SystemPart) node;

                if (part.isDummy()) {
                    continue;
                }

                Measure measure = (Measure) part.getMeasures()
                                                .get(im);

                for (Voice voice : measure.getVoices()) {
                    Integer voiceFinal = voice.getFinalDuration();

                    if (voiceFinal != null) {
                        if (measureFinal == null) {
                            measureFinal = voiceFinal;
                        } else if (!voiceFinal.equals(measureFinal)) {
                            if (logger.isFineEnabled()) {
                                logger.fine("No partial measure");
                            }

                            measureFinal = null;

                            break partLoop;
                        }
                    }
                }
            }

            if ((measureFinal != null) && (measureFinal < 0)) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        system.getContextString() + "M" + im +
                        " Found a partial measure for -" +
                        Note.quarterValueOf(-measureFinal));
                }

                // Flag these measures as partial, and get rid of their final
                // forward marks if any
                for (TreeNode node : system.getParts()) {
                    SystemPart part = (SystemPart) node;
                    Measure    measure = (Measure) part.getMeasures()
                                                       .get(im);
                    measure.setPartial(measureFinal);
                }
            }
        }
    }

    //----------//
    // setDummy //
    //----------//
    public void setDummy (boolean dummy)
    {
        this.dummy = dummy;
    }

    //------------//
    // setPartial //
    //------------//
    public void setPartial (boolean partial)
    {
        this.partial = partial;
    }

    //--------------//
    // setTemporary //
    //--------------//
    public void setTemporary (boolean temporary)
    {
        this.temporary = temporary;
    }

    //-------------//
    // isTemporary //
    //-------------//
    public boolean isTemporary ()
    {
        return temporary;
    }

    //-------------//
    // buildVoices //
    //-------------//
    /**
     * Browse the slots and chords, in order to compute the various voices and
     * start times
     */
    public void buildVoices ()
    {
        // Debug
        if (logger.isFineEnabled()) {
            printChords("Initial chords for ");
        }

        // The 'activeChords' collection gathers the chords that are "active"
        // (not terminated) at the time slot being considered. Initially, it
        // contains just the whole chords.
        List<Chord> activeChords = new ArrayList<Chord>(getWholeChords());
        Collections.sort(activeChords);

        // Create voices for whole chords
        for (Chord chord : activeChords) {
            chord.setStartTime(0);
            Voice.createWholeVoice(chord);
        }

        // Process slot after slot, if any
        for (Slot slot : getSlots()) {
            slot.buildVoices(activeChords);
        }

        // Debug
        if (logger.isFineEnabled()) {
            printVoices("Final voices for ");
        }
    }

    //-----------------//
    // checkTiedChords //
    //-----------------//
    /**
     * Check ties for all chords of this measure
     */
    public void checkTiedChords ()
    {
        // Use a copy of chords collection, to avoid concurrent modifications
        for (TreeNode cn : chords.getChildrenCopy()) {
            Chord chord = (Chord) cn;
            chord.checkTies();
        }
    }

    //-------------//
    // cleanupNode //
    //-------------//
    /**
     * Get rid of all nodes of this measure, except the barlines
     */
    public void cleanupNode ()
    {
        // Remove all direct children except barlines
        for (Iterator it = children.iterator(); it.hasNext();) {
            VisitableNode node = (VisitableNode) it.next();

            if (!(node instanceof Barline)) {
                it.remove();
            }
        }

        // Invalidate data
        startTime = null;
        expectedDuration = null;
        excess = null;
        implicit = false;
        setPartial(false);

        // (Re)Allocate specific children lists
        clefs = new Container(this, "Clefs");
        keysigs = new Container(this, "KeySigs");
        timesigs = new Container(this, "TimeSigs");
        chords = new Container(this, "Chords");
        beams = new Container(this, "Beams");

        //        dynamics = new DynamicList(this);
        //        lyriclines = new LyricList(this);
        //        texts = new TextList(this);

        // Should this be a MeasureNode ??? TBD
        slots = new TreeSet<Slot>();
        beamGroups = new ArrayList<BeamGroup>();
        wholeChords = new ArrayList<Chord>();
        voices = new ArrayList<Voice>();
    }

    //-----------------------//
    // createTemporaryBefore //
    //-----------------------//
    /**
     * Create a temporary initial measure to be exported right before this
     * measure, just to set up global parameters (clef, time, key)
     *
     * @return the created dummy measure
     */
    public Measure createTemporaryBefore ()
    {
        Measure dummyMeasure = new Measure();
        dummyMeasure.setTemporary(true);
        dummyMeasure.setDummy(true);

        // Populate the dummy measure, staff per staff
        SystemPart part = this.getPart();

        for (TreeNode sn : part.getStaves()) {
            Staff       staff = (Staff) sn;
            int         right = getLeftX(); // Right of dummy = Left of current
            int         midY = (staff.getTopLeft().y + (staff.getHeight() / 2)) -
                               getSystem()
                                   .getTopLeft().y;
            SystemPoint staffPoint = new SystemPoint(right, midY);

            // Clef?
            Clef clef = getClefBefore(staffPoint);

            if (clef != null) {
                new Clef(
                    dummyMeasure,
                    staff,
                    clef.getShape(),
                    new SystemPoint(right - 40, midY),
                    clef.getPitchPosition(),
                    null); // No glyph
            }

            // Key?
            KeySignature key = getKeyBefore(staffPoint);

            if (key != null) {
                key.createDummyCopy(
                    dummyMeasure,
                    new SystemPoint(right - 30, midY));
            }

            // Time?
            TimeSignature time = getCurrentTimeSignature();

            if (time != null) {
                time.createDummyCopy(
                    dummyMeasure,
                    new SystemPoint(right - 20, midY));
            }
        }

        return dummyMeasure;
    }

    //-------------//
    // printChords //
    //-------------//
    /**
     * Print the chords of this measure on standard output
     *
     * @param title a specific title, or null
     */
    public void printChords (String title)
    {
        StringBuilder sb = new StringBuilder();

        if (title != null) {
            sb.append(title);
        }

        sb.append(this);

        for (TreeNode cn : getChords()) {
            Chord chord = (Chord) cn;
            sb.append("\n")
              .append(chord);
        }

        logger.info(sb.toString());
    }

    //------------//
    // printSlots //
    //------------//
    /**
     * Print the slots of this measure on standard output
     *
     * @param title a specific title, or null
     */
    public void printSlots (String title)
    {
        StringBuilder sb = new StringBuilder();

        if (title != null) {
            sb.append(title);
        }

        sb.append(this);

        for (Slot slot : this.getSlots()) {
            sb.append("\n")
              .append(slot.toChordString());
        }

        logger.info(sb.toString());
    }

    //------------//
    // printVoices//
    //------------//
    /**
     * Print the voices of this measure on standard output
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
        if (slots.size() > 0) {
            sb.append("\n    ");

            for (Slot slot : slots) {
                if (slot.getStartTime() != null) {
                    sb.append("|")
                      .append(
                        String.format(
                            "%-5s",
                            Note.quarterValueOf(slot.getStartTime())));
                }
            }

            sb.append("|")
              .append(getCurrentDuration());
        }

        for (Voice voice : voices) {
            sb.append("\n")
              .append(voice.toStrip());
        }

        logger.info(sb.toString());
    }

    //----------------//
    // resetAbscissae //
    //----------------//
    /**
     * Reset the coordinates of the measure, they will be lazily recomputed when
     * needed
     */
    public void resetAbscissae ()
    {
        leftX = null;

        if (barline != null) {
            barline.reset();
        }
    }

    //----------------//
    // resetStartTime //
    //----------------//
    public void resetStartTime ()
    {
        startTime = null;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Measure#" + id + "}";
    }

    //---------------//
    // computeCenter //
    //---------------//
    /**
     * The 'center' here is the middle of the measure
     */
    @Override
    protected void computeCenter ()
    {
        SystemPoint bl = barline.getCenter();
        setCenter(new SystemPoint((bl.x + getLeftX()) / 2, bl.y));
    }

    //---------------//
    // addWholeChord //
    //---------------//
    /**
     * Insert a note as a whole (or multi) rest
     *
     * @param glyph the underlying glyph
     */
    void addWholeChord (Glyph glyph)
    {
        Chord chord = new Chord(this, null);

        // No slot for this chord, but a whole rest
        new Note(chord, glyph); // Records note in chord
        wholeChords.add(chord);
    }

    //--------------//
    // addWholeRest //
    //--------------//
    /**
     * Insert a whole rest at provided center
     * @param center the location for the rest note
     */
    void addWholeRest (Staff       staff,
                       SystemPoint center)
    {
        Chord chord = new Chord(this, null);
        Note.createWholeRest(staff, chord, center);
        wholeChords.add(chord);
    }

    //--------------------//
    // getCurrentDuration //
    //--------------------//
    private String getCurrentDuration ()
    {
        int measureDur = 0;

        for (Slot slot : getSlots()) {
            if (slot.getStartTime() != null) {
                for (Chord chord : slot.getChords()) {
                    measureDur = Math.max(
                        measureDur,
                        slot.getStartTime() + chord.getDuration());
                }
            }
        }

        if ((measureDur == 0) && !getWholeChords()
                                      .isEmpty()) {
            return "W";
        }

        return String.format("%-5s", Note.quarterValueOf(measureDur));
    }
}
