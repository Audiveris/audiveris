//----------------------------------------------------------------------------//
//                                                                            //
//                               M e a s u r e                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.facets.Glyph;

import omr.math.Rational;

import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.visitor.ScoreVisitor;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code Measure} handles a measure of a system part, that is
 * all entities within the same measure time frame, for all staves
 * that compose the system part.
 *
 * <p>As a ScoreNode, the children of a Measure are : ending Barline, list of
 * TimeSignature(s), list of Clef(s), list of KeySignature(s), list of Chord(s)
 * and list of Beam(s).</p>
 *
 * <p>Measure Ids are stored with respect to their containing page only, they
 * are page-based ids. Displayed to the user are score-based ids.</p>
 *
 * @author Hervé Bitteur
 */
public class Measure
        extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Measure.class);

    //~ Instance fields --------------------------------------------------------
    /** To flag dummy barline instances */
    private boolean dummy;

    /** To flag a temporary measure (for playback) */
    private boolean temporary;

    /** Flag for implicit (pickup or repeat last half) measure */
    private boolean implicit;

    /** Flag for firstHalf measure */
    private boolean firstHalf;

    /** Child: Left-inside bar line, if any */
    private Barline insideBarline;

    /** Child: Ending bar line, if any */
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

    /** Measure Id */
    private MeasureId.PageBased id;

    /** Identified time slots within the measure */
    private List<Slot> slots;

    /**
     * Chords of just whole rest (thus handled outside slots)
     * These chords are also contained in 'chords' container, like plain ones
     */
    private List<Chord> wholeChords;

    /** Groups of beams in this measure */
    private List<BeamGroup> beamGroups;

    /** Theoretical measure duration, based on current time signature */
    private Rational expectedDuration;

    /** Actual measure duration, based on durations of contained chords */
    private Rational actualDuration;

    /** Flag to indicate a excess duration */
    private Rational excess;

    /** Voices within this measure, sorted by increasing voice id */
    private List<Voice> voices;

    //~ Constructors -----------------------------------------------------------
    //---------//
    // Measure //
    //---------//
    /**
     * Create a measure with the specified parameters.
     *
     * @param part the containing system part
     */
    public Measure (SystemPart part)
    {
        super(part);
        cleanupNode();
    }

    //~ Methods ----------------------------------------------------------------
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
     * Override normal behavior, so that a given child is stored in its
     * proper type collection (clef to clef list, etc...).
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
     * Add a beam goup to this measure.
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

//    //-------------//
//    // buildVoices //
//    //-------------//
//    /**
//     * Browse the slots and chords, in order to compute the various
//     * voices and start times.
//     */
//    public void buildVoices ()
//    {
//        // Debug
//        if (logger.isDebugEnabled()) {
//            printChords("Initial chords for ");
//        }
//
//        // The 'activeChords' collection gathers the chords that are "active"
//        // (not terminated) at the time slot being considered. Initially, it
//        // contains just the whole chords.
//        List<Chord> activeChords = new ArrayList<>(getWholeChords());
//        Collections.sort(activeChords, Chord.byAbscissa);
//
//        // Create voices for whole chords
//        for (Chord chord : activeChords) {
//            chord.setStartTime(Rational.ZERO);
//            Voice.createWholeVoice(chord);
//        }
//
//        // Process slot after slot, if any
//        try {
//            for (Slot slot : getSlots()) {
//                slot.buildVoices(activeChords);
//            }
//        } catch (Exception ex) {
//            logger.warn(
//                    "Error building voices in measure " + getPageId(),
//                    ex);
//        }
//
//        // Debug
//        if (logger.isDebugEnabled()) {
//            printVoices("Final voices for ");
//        }
//    }
    //---------------//
    // checkDuration //
    //---------------//
    /**
     * Check the duration as computed in this measure from its
     * contained voices, compared to its theoretical duration.
     */
    public void checkDuration ()
    {
        // Check duration of each voice
        for (Voice voice : voices) {
            voice.checkDuration();
        }
    }

    //-----------------//
    // checkTiedChords //
    //-----------------//
    /**
     * Check ties for all chords of this measure.
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
     * Get rid of all nodes of this measure, except the barlines.
     */
    public void cleanupNode ()
    {
        // Remove all direct children except barlines
        for (Iterator<TreeNode> it = children.iterator(); it.hasNext();) {
            VisitableNode node = (VisitableNode) it.next();

            if (!(node instanceof Barline)) {
                it.remove();
            }
        }

        // Invalidate data
        expectedDuration = null;
        excess = null;
        implicit = false;
        setFirstHalf(false);

        // (Re)Allocate specific children lists
        clefs = new Container(this, "Clefs");
        keysigs = new Container(this, "KeySigs");
        timesigs = new Container(this, "TimeSigs");
        chords = new Container(this, "Chords");
        beams = new Container(this, "Beams");

        //        dynamics = new DynamicList(this);
        //        lyriclines = new LyricList(this);
        //        texts = new TextList(this);

        // Should this be a MeasureNode ??? TODO
        slots = new ArrayList<>();
        beamGroups = new ArrayList<>();
        wholeChords = new ArrayList<>();
        voices = new ArrayList<>();
    }

    //-----------------------//
    // createTemporaryBefore //
    //-----------------------//
    /**
     * Create a temporary initial measure to be exported right before
     * this measure, just to set up global parameters (clef, time, key).
     *
     * @return the created dummy measure
     */
    public Measure createTemporaryBefore ()
    {
        Measure dummyMeasure = new Measure(null);
        dummyMeasure.setTemporary(true);
        dummyMeasure.setDummy(true);

        // Populate the dummy measure, staff per staff
        SystemPart part = this.getPart();

        for (TreeNode sn : part.getStaves()) {
            Staff staff = (Staff) sn;
            int right = getLeftX(); // Right of dummy = Left of current
            int midY = (staff.getTopLeft().y + (staff.getHeight() / 2))
                       - getSystem().getTopLeft().y;
            Point staffPoint = new Point(right, midY);

            // Clef?
            Clef clef = getClefBefore(staffPoint, staff);

            if (clef != null) {
                new Clef(
                        dummyMeasure,
                        staff,
                        clef.getShape(),
                        new Point(right - 40, midY),
                        clef.getPitchPosition(),
                        null); // No glyph
            }

            // Key?
            KeySignature key = getKeyBefore(staffPoint, staff);

            if (key != null) {
                key.createDummyCopy(
                        dummyMeasure,
                        new Point(right - 30, midY));
            }

            // Time?
            TimeSignature time = getCurrentTimeSignature();

            if (time != null) {
                time.createDummyCopy(
                        dummyMeasure,
                        new Point(right - 20, midY));
            }
        }

        return dummyMeasure;
    }

    //-------------------//
    // getActualDuration //
    //-------------------//
    /**
     * Report the duration of this measure, as computed from its
     * contained voices.
     *
     * @return the (actual) measure duration, or 0 if no rest / note exist in
     *         this measure
     */
    public Rational getActualDuration ()
    {
        if (actualDuration != null) {
            return actualDuration;
        } else {
            ///logger.warn(getContextString() + " no actual duration");
            return Rational.ZERO;
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
    public Barline getBarline ()
    {
        return barline;
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

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the collection of beams.
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
     * Report the collection of chords.
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
     * Report the collection of chords whose head is located in the
     * staff above the provided point.
     *
     * @param point the provided point
     * @return the (perhaps empty) collection of chords
     */
    public Collection<Chord> getChordsAbove (Point point)
    {
        Staff desiredStaff = getSystem().getStaffAbove(point);
        Collection<Chord> found = new ArrayList<>();

        for (TreeNode node : getChords()) {
            Chord chord = (Chord) node;

            if (chord.getStaff() == desiredStaff) {
                Point head = chord.getHeadLocation();
                if (head != null && head.y < point.y) {
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
     * Report the collection of chords whose head is located in the
     * staff below the provided point.
     *
     * @param point the provided point
     * @return the (perhaps empty) collection of chords
     */
    public Collection<Chord> getChordsBelow (Point point)
    {
        Staff desiredStaff = getSystem().getStaffBelow(point);
        Collection<Chord> found = new ArrayList<>();

        for (TreeNode node : getChords()) {
            Chord chord = (Chord) node;

            if (chord.getStaff() == desiredStaff) {
                Point head = chord.getHeadLocation();
                if (head != null && head.y > point.y) {
                    found.add(chord);
                }
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
     * next systems) while staying in the same logical staff.
     *
     * @param point the point after which to look
     * @return the first clef defined, or null
     */
    public Clef getClefAfter (Point point)
    {
        // Which staff we are in
        Clef clef;
        int staffId = getPart().getStaffAt(point).getId();

        // Look in this measure, with same staff, going forward
        for (TreeNode cn : getClefs()) {
            clef = (Clef) cn;

            if ((clef.getStaff().getId() == staffId)
                && (clef.getCenter().x >= point.x)) {
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
     * Same functionally than the other method, but with a staff
     * provided.
     *
     * @param point the point before which to look
     * @param staff the containing staff (if null, it is derived from center.y)
     * @return the latest clef defined, or null
     */
    public Clef getClefBefore (Point point,
                               Staff staff)
    {
        // First, look in this measure, with same staff, going backwards
        Clef clef = getMeasureClefBefore(point, staff);

        if (clef != null) {
            return clef;
        }

        // Which staff we are in
        int staffId = getStaffId(point, staff);

        // Look in all preceding measures, with the same staff id
        Measure measure = this;

        while ((measure = measure.getPrecedingInPage()) != null) {
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
     * Report the node that collect the clefs.
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
     * Report the collection of clefs.
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
     * From a provided Chord collection, report the chord which has the
     * closest abscissa to a provided point.
     *
     * @param chords the collection of chords to browse
     * @param point  the reference point
     * @return the abscissa-wise closest chord
     */
    public Chord getClosestChord (Collection<Chord> chords,
                                  Point point)
    {
        Chord bestChord = null;
        int bestDx = Integer.MAX_VALUE;

        for (Chord chord : chords) {
            int dx = Math.abs(chord.getHeadLocation().x - point.x);

            if (dx < bestDx) {
                bestDx = dx;
                bestChord = chord;
            }
        }

        return bestChord;
    }

    //-----------------//
    // getClosestChord //
    //-----------------//
    /**
     * Report the chord of this measure which has the closest
     * abscissa to a provided point.
     *
     * @param point the reference point
     * @return the abscissa-wise closest chord, perhaps null
     */
    public Chord getClosestChord (Point point)
    {
        Chord bestChord = null;
        int bestDx = Integer.MAX_VALUE;

        for (TreeNode node : getChords()) {
            Chord chord = (Chord) node;
            int dx = Math.abs(chord.getHeadLocation().x - point.x);

            if (dx < bestDx) {
                bestDx = dx;
                bestChord = chord;
            }
        }

        return bestChord;
    }

    //----------------------//
    // getClosestChordAbove //
    //----------------------//
    /**
     * Report the chord above the provided point which has the closest
     * abscissa to the provided point.
     *
     * @param point the reference point
     * @return the abscissa-wise closest chord among the chords above, if any.
     */
    public Chord getClosestChordAbove (Point point)
    {
        return getClosestChord(getChordsAbove(point), point);
    }

    //----------------------//
    // getClosestChordBelow //
    //----------------------//
    /**
     * Report the chord below the provided point which has the closest
     * abscissa to the provided point.
     *
     * @param point the reference point
     * @return the abscissa-wise closest chord among the chords below, if any.
     */
    public Chord getClosestChordBelow (Point point)
    {
        return getClosestChord(getChordsBelow(point), point);
    }

    //----------------//
    // getClosestSlot //
    //----------------//
    /**
     * Report the time slot which has the closest abscissa to a
     * provided point.
     *
     * @param point the reference point
     * @return the abscissa-wise closest slot
     */
    public Slot getClosestSlot (Point point)
    {
        Slot bestSlot = null;
        int bestDx = Integer.MAX_VALUE;

        for (Slot slot : getSlots()) {
            int dx = Math.abs(slot.getX() - point.x);

            if (dx < bestDx) {
                bestDx = dx;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    //---------------------------//
    // getClosestWholeChordAbove //
    //---------------------------//
    /**
     * Report the whole chord above the provided point which has the
     * closest abscissa to the provided point.
     *
     * @param point the reference point
     * @return the abscissa-wise closest chord among the chords below, if any.
     */
    public Chord getClosestWholeChordAbove (Point point)
    {
        return getClosestChord(getWholeChordsAbove(point), point);
    }

    //---------------------------//
    // getClosestWholeChordBelow //
    //---------------------------//
    /**
     * Report the whole chord below the provided point which has the
     * closest abscissa to the provided point.
     *
     * @param point the reference point
     * @return the abscissa-wise closest chord among the chords below, if any.
     */
    public Chord getClosestWholeChordBelow (Point point)
    {
        return getClosestChord(getWholeChordsBelow(point), point);
    }

    //-------------------------//
    // getCurrentTimeSignature //
    //-------------------------//
    /**
     * Report the time signature which applies in this measure,
     * whether a time signature actually starts this measure in
     * whatever staff, or whether a time signature was found in a
     * previous measure, even in preceding pages.
     * <p><b>NOTA</b>This method looks up for time sig in preceding pages
     * as well</p>
     *
     * @return the current time signature, or null if not found at all
     */
    public TimeSignature getCurrentTimeSignature ()
    {
        // Backward from this measure to the beginning of the score
        Measure measure = this;
        Page page = getPage();

        while (measure != null) {
            // Check in the measure
            TimeSignature ts = measure.getTimeSignature();

            if (ts != null) {
                return ts;
            }

            // Move to preceding measure (same part)
            measure = measure.getPrecedingInPage();

            if (measure == null) {
                page = page.getPrecedingInScore();

                if (page == null) {
                    return null;
                } else {
                    measure = page.getLastSystem().getLastPart().
                            getLastMeasure();
                }
            }
        }

        return null; // Not found !!!
    }

    //-------------------//
    // getDirectionChord //
    //-------------------//
    /**
     * Retrieve the most suitable chord for a direction.
     *
     * @param point the system-based location
     * @return the most suitable chord, or null
     */
    public Chord getDirectionChord (Point point)
    {
        // Priority on slot
        Chord chord = getEventChord(point);

        // Then on staff
        if (chord == null) {
            chord = getClosestChordAbove(point);
        }

        if (chord == null) {
            chord = getClosestChordBelow(point);
        }

        return chord;
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
    public Chord getEventChord (Point point)
    {
        // Choose the x-closest slot
        Slot slot = getClosestSlot(point);

        if (slot != null) {
            // First, try staff just above
            Chord chordAbove = slot.getChordJustAbove(point);
            if (chordAbove != null) {
                return chordAbove;
            }

            // Second, try staff just below
            Chord chordBelow = slot.getChordJustBelow(point);
            if (chordBelow != null) {
                return chordBelow;
            }
        }

        return null;
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
                TimeSignature ts = getCurrentTimeSignature();

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
     * Report the first clef (if any) in this measure, tagged with
     * the provided staff.
     *
     * @param staffId the imposed related staff id
     * @return the first clef, or null
     */
    public Clef getFirstMeasureClef (int staffId)
    {
        // Going forward
        for (TreeNode cn : getClefs()) {
            Clef clef = (Clef) cn;

            if (clef.getStaff().getId() == staffId) {
                return clef;
            }
        }

        return null;
    }

    //--------------------//
    // getFirstMeasureKey //
    //--------------------//
    /**
     * Report the first key signature (if any) in this measure,
     * tagged with the provided staff.
     *
     * @param staffId the imposed related staff id
     * @return the first key signature, or null
     */
    public KeySignature getFirstMeasureKey (int staffId)
    {
        for (TreeNode kn : getKeySignatures()) {
            KeySignature key = (KeySignature) kn;

            if (key.getStaff().getId() == staffId) {
                return key;
            }
        }

        return null;
    }

    //--------------//
    // getFollowing //
    //--------------//
    /**
     * Report the following measure of this one, either in this
     * system / part, or in the following system /part.
     *
     * @return the following measure, or null if none
     */
    public Measure getFollowing ()
    {
        Measure nextMeasure = (Measure) getNextSibling();

        if (nextMeasure != null) {
            return nextMeasure;
        }

        SystemPart followingPart = getPart().getFollowing();

        if (followingPart != null) {
            return followingPart.getFirstMeasure();
        } else {
            return null;
        }
    }

    //------------//
    // getIdValue //
    //------------//
    /**
     * Report the numeric value of the measure id.
     *
     * @return the numeric value of measure id
     */
    public int getIdValue ()
    {
        return (id == null) ? 0 : id.value;
    }

    //------------------//
    // getInsideBarline //
    //------------------//
    /**
     * @return the insideBarline, if any.
     */
    public Barline getInsideBarline ()
    {
        return insideBarline;
    }

    //--------------//
    // getKeyBefore //
    //--------------//
    /**
     * Report the key signature which applies in this measure, whether
     * a key signature actually starts this measure in the same staff,
     * or whether a key signature was found in a previous measure,
     * for the same staff.
     *
     * @param point the point before which to look
     * @param staff the containing staff (cannot be null)
     * @return the current key signature, or null if not found
     */
    public KeySignature getKeyBefore (Point point,
                                      Staff staff)
    {
        if (point == null) {
            throw new NullPointerException();
        }

        KeySignature ks;
        int staffId = staff.getId();

        // Look in this measure, with same staff, going backwards
        for (int ik = getKeySignatures().size() - 1; ik >= 0; ik--) {
            ks = (KeySignature) getKeySignatures().get(ik);

            if ((ks.getStaff().getId() == staffId)
                && (ks.getCenter().x < point.x)) {
                return ks;
            }
        }

        // Look in previous measures in the system part and the preceding ones
        Measure measure = this;

        while ((measure = measure.getPrecedingInPage()) != null) {
            ks = measure.getLastMeasureKey(staffId);

            if (ks != null) {
                return ks;
            }
        }

        return null; // Not found (in this page)
    }

    //---------------//
    // getKeySigList //
    //---------------//
    /**
     * Report the list that collects the KeySignature instances.
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
     * Report the collection of KeySignature's.
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
     * Report the last clef (if any) in this measure, tagged with the
     * provided staff.
     *
     * @param staffId the imposed related staff id
     * @return the last clef, or null
     */
    public Clef getLastMeasureClef (int staffId)
    {
        // Going backwards
        for (int ic = getClefs().size() - 1; ic >= 0; ic--) {
            Clef clef = (Clef) getClefs().get(ic);

            if (clef.getStaff().getId() == staffId) {
                return clef;
            }
        }

        return null;
    }

    //-------------------//
    // getLastMeasureKey //
    //-------------------//
    /**
     * Report the last key signature (if any) in this measure,
     * tagged with the provided staff.
     *
     * @param staffId the imposed related staff id
     * @return the last key signature, or null
     */
    public KeySignature getLastMeasureKey (int staffId)
    {
        // Going backwards
        for (int ik = getKeySignatures().size() - 1; ik >= 0; ik--) {
            KeySignature key = (KeySignature) getKeySignatures().get(ik);

            if (key.getStaff().getId() == staffId) {
                return key;
            }
        }

        return null;
    }

    //------------------//
    // getLastSoundTime //
    //------------------//
    /**
     * Report the time, counted from beginning of this measure, when
     * sound stops which means that ending rests are not counted.
     *
     * @return the relative time of last Midi "note off" in this measure
     */
    public Rational getLastSoundTime ()
    {
        Rational lastTime = Rational.ZERO;

        for (TreeNode chordNode : getChords()) {
            Chord chord = (Chord) chordNode;

            if (!chord.isAllRests()) {
                Rational time = chord.getStartTime().plus(chord.getDuration());

                if (time.compareTo(lastTime) > 0) {
                    lastTime = time;
                }
            }
        }

        return lastTime;
    }

    //----------//
    // getLeftX //
    //----------//
    /**
     * Report the abscissa of the start of the measure.
     *
     * @return abscissa of left side of the measure
     */
    public Integer getLeftX ()
    {
        return getBox().x;
    }

    //----------------------//
    // getMeasureClefBefore //
    //----------------------//
    /**
     * Report the current clef, if any, defined within this measure
     * and staff, and located before this measure point.
     *
     * @param point the point before which to look
     * @param staff the containing staff (if null, it is derived from point.y)
     * @return the measure clef defined, or null
     */
    public Clef getMeasureClefBefore (Point point,
                                      Staff staff)
    {
        Clef clef = null;

        // Which staff we are in
        int staffId = getStaffId(point, staff);

        // Look in this measure, with same staff, going backwards
        for (int ic = getClefs().size() - 1; ic >= 0; ic--) {
            clef = (Clef) getClefs().get(ic);

            if ((clef.getStaff().getId() == staffId)
                && (clef.getCenter().x <= point.x)) {
                return clef;
            }
        }

        return null; // No clef previously defined
    }

    //-----------//
    // getPageId //
    //-----------//
    /**
     * Report the page-based measure id.
     *
     * @return the page-based measure id
     */
    public MeasureId.PageBased getPageId ()
    {
        return id;
    }

    //--------------------//
    // getPrecedingInPage //
    //--------------------//
    /**
     * Report the preceding measure of this one, either in this
     * system / part, or in the preceding system /part, but still
     * in the same page.
     *
     * @return the preceding measure, or null if not found in the page
     */
    public Measure getPrecedingInPage ()
    {
        Measure prevMeasure = (Measure) getPreviousSibling();

        if (prevMeasure != null) {
            return prevMeasure;
        }

        SystemPart precedingPart = getPart().getPrecedingInPage();

        if (precedingPart != null) {
            return precedingPart.getLastMeasure();
        } else {
            return null;
        }
    }

    //-----------//
    // getRightX //
    //-----------//
    /**
     * Report the abscissa of the end of the measure.
     *
     * @return part-based abscissa of right side of the measure
     */
    public Integer getRightX ()
    {
        return getBox().x + getBox().width;
    }

    //------------//
    // getScoreId //
    //------------//
    /**
     * Report the image of the score-based measure id.
     *
     * @return the score-based measure id string
     */
    public String getScoreId ()
    {
        MeasureId.PageBased mid = id;
        if (mid != null) {
            return mid.toScoreString();
        } else {
            return null;
        }
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

    //------------//
    // getStaffId //
    //------------//
    /**
     * Report the id of the staff containing the provided point.
     *
     * @param point the provided point
     * @param staff the staff if known, otherwise null
     * @return the staff id
     */
    public int getStaffId (Point point,
                           Staff staff)
    {
        return (staff != null) ? staff.getId()
                : getPart().getStaffAt(point).getId();
    }

    //----------------//
    // getTimeSigList //
    //----------------//
    /**
     * Report the node that collects the TimeSignature instances.
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
     * Report the potential time signature in this measure for the
     * related staff.
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
     * Report the potential time signature in this measure
     * (whatever the staff).
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
     * Report the number of voices in this measure.
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
     * Report the collection of whole chords.
     *
     * @return the whole chords of this measure
     */
    public Collection<Chord> getWholeChords ()
    {
        return wholeChords;
    }

    //---------------------//
    // getWholeChordsAbove //
    //---------------------//
    /**
     * Report the collection of whole chords located in the
     * staff above the provided point.
     *
     * @param point the provided point
     * @return the (perhaps empty) collection of chords
     */
    public Collection<Chord> getWholeChordsAbove (Point point)
    {
        Staff desiredStaff = getSystem().getStaffAbove(point);
        Collection<Chord> found = new ArrayList<>();

        for (Chord chord : getWholeChords()) {
            if ((chord.getStaff() == desiredStaff)
                && (chord.getHeadLocation().y < point.y)) {
                found.add(chord);
            }
        }

        return found;
    }

    //---------------------//
    // getWholeChordsBelow //
    //---------------------//
    /**
     * Report the collection of whole chords located in the
     * staff below the provided point.
     *
     * @param point the provided point
     * @return the (perhaps empty) collection of whole chords
     */
    public Collection<Chord> getWholeChordsBelow (Point point)
    {
        Staff desiredStaff = getSystem().getStaffBelow(point);
        Collection<Chord> found = new ArrayList<>();

        for (Chord chord : getWholeChords()) {
            if ((chord.getStaff() == desiredStaff)
                && (chord.getHeadLocation().y > point.y)) {
                found.add(chord);
            }
        }

        return found;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width, in units, of the measure.
     *
     * @return the measure width, or null in case of dummy measure
     */
    public Integer getWidth ()
    {
        if (isDummy()) {
            return null;
        } else {
            return getRightX() - getLeftX();
        }
    }

    //---------//
    // isDummy //
    //---------//
    public boolean isDummy ()
    {
        return dummy;
    }

    //-------------//
    // isFirstHalf //
    //-------------//
    /**
     * @return the firstHalf
     */
    public boolean isFirstHalf ()
    {
        return firstHalf;
    }

    //------------//
    // isImplicit //
    //------------//
    /**
     * Report whether this measure is implicit.
     *
     * @return true if measure is implicit
     */
    public boolean isImplicit ()
    {
        return implicit;
    }

    //-------------//
    // isTemporary //
    //-------------//
    public boolean isTemporary ()
    {
        return temporary;
    }

    //----------------//
    // mergeWithRight //
    //----------------//
    /**
     * Merge this measure with the content of the following measure on
     * the right.
     *
     * @param right the following measure
     */
    public void mergeWithRight (Measure right)
    {
        clefs.getChildren().addAll(right.clefs.getChildren());
        keysigs.getChildren().addAll(right.keysigs.getChildren());
        timesigs.getChildren().addAll(right.timesigs.getChildren());
        chords.getChildren().addAll(right.chords.getChildren());
        beams.getChildren().addAll(right.beams.getChildren());

        slots.addAll(right.slots);
        beamGroups.addAll(right.beamGroups);
        wholeChords.addAll(right.wholeChords);
        voices.addAll(right.voices);

        setBox(getBox().union(right.getBox()));

        insideBarline = barline;
        addChild(right.barline);
    }

    //-------------//
    // printChords //
    //-------------//
    /**
     * Print the chords of this measure on standard output.
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
            sb.append("\n").append(chord);
        }

        logger.info(sb.toString());
    }

    //------------//
    // printSlots //
    //------------//
    /**
     * Print the slots of this measure on standard output.
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
            sb.append("\n").append(slot.toChordString());
        }

        logger.info(sb.toString());
    }

    //------------//
    // printVoices//
    //------------//
    /**
     * Print the voices of this measure on standard output.
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
            sb.append("\n    ");

            for (Slot slot : slots) {
                if (slot.getStartTime() != null) {
                    sb.append("|").append(String.format("%-5s", slot.
                            getStartTime()));
                }
            }

            sb.append("|").append(getCurrentDuration());
        }

        for (Voice voice : voices) {
            sb.append("\n").append(voice.toStrip());
        }

        logger.info(sb.toString());
    }

    //----------------//
    // resetAbscissae //
    //----------------//
    /**
     * Reset the coordinates of the measure, they will be lazily
     * recomputed when needed.
     */
    public void resetAbscissae ()
    {
        reset();

        if (barline != null) {
            barline.reset();
        }
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

    //----------//
    // setDummy //
    //----------//
    public void setDummy (boolean dummy)
    {
        this.dummy = dummy;
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
    /**
     * @param firstHalf the firstHalf to set
     */
    public void setFirstHalf (boolean firstHalf)
    {
        this.firstHalf = firstHalf;
    }

    //-------------//
    // setImplicit //
    //-------------//
    /**
     * Flag this measure as implicit.
     */
    public void setImplicit ()
    {
        implicit = true;
    }

    //----------//
    // setLeftX //
    //----------//
    public void setLeftX (int val)
    {
        Rectangle newBox = getBox();
        newBox.width = val;
        setBox(newBox);
    }

    //-----------//
    // setPageId //
    //-----------//
    /**
     * Assign the proper page-based id to this measure.
     *
     * @param id         the proper page-based measure id value
     * @param secondHalf true if the measure is the second half of a repeat
     */
    public void setPageId (int id,
                           boolean secondHalf)
    {
        this.id = new MeasureId.PageBased(this, id, secondHalf);
    }

    //-----------//
    // setPageId //
    //-----------//
    /**
     * Assign the proper page-based id to this measure.
     *
     * @param pageId the page-based id
     */
    public void setPageId (MeasureId.PageBased pageId)
    {
        this.id = new MeasureId.PageBased(this, pageId);
    }

    //--------------//
    // setTemporary //
    //--------------//
    public void setTemporary (boolean temporary)
    {
        this.temporary = temporary;
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
                if (duration.equals(shortening)) {
                    if (!voice.isWhole()) {
                        // Remove the related mark
                        Chord chord = voice.getLastChord();

                        if (chord != null) {
                            int nbMarks = chord.getMarks().size();

                            if (nbMarks > 0) {
                                Mark mark = chord.getMarks().get(nbMarks - 1);
                                logger.debug("{} Removing final forward: {}",
                                        getContextString(),
                                        (Rational) mark.getData());
                                chord.getMarks().remove(mark);
                            } else {
                                chord.
                                        addError(
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
                            "Non consistent partial measure shortening:"
                            + shortening.opposite() + " " + voice + ": "
                            + duration.opposite());

                    return;
                }
            }
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Measure#" + id + "}";
    }

    //------------//
    // computeBox //
    //------------//
    @Override
    protected void computeBox ()
    {
        // Start of the measure
        int leftX;
        Measure prevMeasure = (Measure) getPreviousSibling();

        if (prevMeasure == null) { // Very first measure in the staff
            leftX = getSystem().getTopLeft().x;
        } else {
            leftX = prevMeasure.getBarline().getCenter().x;
        }

        // End of the measure
        int rightX;

        if (barline != null) {
            rightX = barline.getCenter().x;
        } else {
            // Last measure of a part/system with no ending barline
            ScoreSystem system = getSystem();
            rightX = system.getTopLeft().x + system.getDimension().width;
        }

        Rectangle partBox = getPart().getBox();
        setBox(
                new Rectangle(
                leftX,
                partBox.y,
                rightX - leftX,
                partBox.height));
    }

    //---------------//
    // computeCenter //
    //---------------//
    /**
     * The 'center' here is the middle of the measure.
     */
    @Override
    protected void computeCenter ()
    {
        Point bl = barline.getCenter();
        setCenter(new Point((bl.x + getLeftX()) / 2, bl.y));
    }

    //---------------//
    // addWholeChord //
    //---------------//
    /**
     * Insert a note as a whole (or multi) rest.
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

    //---------------//
    // addWholeChord //
    //---------------//
    public void addWholeChord (Chord chord)
    {
        wholeChords.add(chord);
    }

    //--------------//
    // addWholeRest //
    //--------------//
    /**
     * Insert a whole rest at provided center.
     *
     * @param center the location for the rest note
     */
    void addWholeRest (Staff staff,
                       Point center)
    {
        Chord chord = new Chord(this, null);
        Note.createWholeRest(staff, chord, center);
        wholeChords.add(chord);

        chord.setStartTime(Rational.ZERO);
        Voice.createWholeVoice(chord);

    }

    //--------------------//
    // getCurrentDuration //
    //--------------------//
    private String getCurrentDuration ()
    {
        Rational measureDur = Rational.ZERO;

        for (Slot slot : getSlots()) {
            if (slot.getStartTime() != null) {
                for (Chord chord : slot.getChords()) {
                    Rational chordEnd = slot.getStartTime().plus(chord.
                            getDuration());

                    if (chordEnd.compareTo(measureDur) > 0) {
                        measureDur = chordEnd;
                    }
                }
            }
        }

        if (measureDur.equals(Rational.ZERO) && !getWholeChords().isEmpty()) {
            return "W";
        }

        return String.format("%-5s", measureDur.toString());
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
}
