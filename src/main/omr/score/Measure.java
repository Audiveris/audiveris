//----------------------------------------------------------------------------//
//                                                                            //
//                               M e a s u r e                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.TimeSignature.InvalidTimeSignature;
import omr.score.visitor.ScoreVisitor;

import omr.util.Logger;
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

    /** Child: Ending bar line */
    private Barline barline;

    /** Child: Potential one time signature per staff */
    private TimeSigList timesigs;

    /** Children: possibly several clefs per staff */
    private ClefList clefs;

    /** Children: possibly several KeySignature's per staff */
    private KeySigList keysigs;

    /** Children: possibly several Chord's per staff */
    private ChordList chords;

    /** Children: possibly several Beam's per staff */
    private BeamList beams;

    //
    //    /** Children: possibly several dynamics */
    //    private DynamicList dynamics;
    //
    //    /** Children: possibly several lines of lyrics */
    //    private LyricList lyriclines;
    //
    //    /** Children: possibly several texts */
    //    private TextList texts;

    /** Left abscissa (in units, wrt system left side) of this measure */
    private Integer leftX;

    /** For measure with no physical ending bar line */
    private boolean lineInvented;

    /** Flag for implicit (introduction) measure */
    private boolean implicit;

    /** Measure Id */
    private int id;

    /** Identified time slots within the measure */
    private SortedSet<Slot> slots;

    /** Chords of just whole rest (thus handled outside slots) */
    private List<Chord> wholeChords;

    /** Groups of beams in this measure */
    private List<BeamGroup> beamGroups;

    /** Number of voices */
    private Integer voicesNumber;

    /** Theoretical measure duration */
    private Integer expectedDuration;

    /** Flag to indicate a excess duration */
    private Integer excess;

    /**
     * Final duration per voice:
     * 0=perfect, -n=too_short, +n=overlast, null=whole_rest
     */
    private Map<Integer, Integer> finalDurations = new HashMap<Integer, Integer>();

    /** Final chord of each voice */
    private Map<Integer, Chord> finalChords = new HashMap<Integer, Chord>();

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Measure //
    //---------//
    /**
     * Create a measure with the specified parameters
     *
     * @param part        the containing system part
     * @param lineInvented flag an artificial ending bar line if none existed
     */
    public Measure (SystemPart part,
                    boolean    lineInvented)
    {
        super(part);

        this.lineInvented = lineInvented;
        cleanupNode();
    }

    //---------//
    // Measure //
    //---------//
    /**
     * Default constructor (needed by XML Binder)
     */
    private Measure ()
    {
        super(null);
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
     * Override normal behavior, so that a given child is stored in its proper
     * type collection (clef to clef list, etc...)
     *
     * @param node the child to insert in the staff
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

    //---------------//
    // checkDuration //
    //---------------//
    /**
     * Check the duration as computed in this measure from its contained voices,
     * compared to its theoretical duration.
     */
    public void checkDuration ()
    {
        // As a first attempt, make all forward stuff explicit & visible
        for (int voice = 1; voice <= getVoicesNumber(); voice++) {
            try {
                int   timeCounter = 0;
                Chord lastChord = null;

                for (Slot slot : getSlots()) {
                    for (Chord chord : slot.getChords()) {
                        if (chord.getVoice() == voice) {
                            // Need a forward before this chord ?
                            if (timeCounter < slot.getStartTime()) {
                                insertForward(
                                    slot.getStartTime() - timeCounter,
                                    Mark.Position.BEFORE,
                                    chord);
                                timeCounter = slot.getStartTime();
                            }

                            lastChord = chord;
                            timeCounter += chord.getDuration();
                        }
                    }
                }

                // Need an ending forward ?
                if (lastChord != null) {
                    finalChords.put(voice, lastChord);

                    int delta = timeCounter - getExpectedDuration();
                    finalDurations.put(voice, delta);

                    if (delta < 0) {
                        // Insert a forward mark
                        insertForward(-delta, Mark.Position.AFTER, lastChord);
                    } else if (delta > 0) {
                        // Flag the measure as too long
                        addError(
                            "Voice #" + voice + " too long for " +
                            Note.quarterValueOf(delta));
                        excess = delta;
                    } else if (lastChord.isWholeDuration()) {
                        // Remember we can't tell anything
                        finalDurations.put(voice, null);
                    }
                }
            } catch (Exception ex) {
                // User has been informed
            }
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
            ScoreNode node = (ScoreNode) it.next();

            if (!(node instanceof Barline)) {
                it.remove();
            }
        }

        // Invalidate data
        slots = null;
        expectedDuration = null;
        excess = null;

        // (Re)Allocate specific children lists
        clefs = new ClefList(this);
        keysigs = new KeySigList(this);
        timesigs = new TimeSigList(this);
        chords = new ChordList(this);
        beams = new BeamList(this);

        //        dynamics = new DynamicList(this);
        //        lyriclines = new LyricList(this);
        //        texts = new TextList(this);

        // Should this be a MeasureNode ??? TBD
        slots = new TreeSet<Slot>();
        beamGroups = new ArrayList<BeamGroup>();
        wholeChords = new ArrayList<Chord>();
    }

    //----------------//
    // findEventChord //
    //----------------//
    /**
     * Retrieve the most suitable chord to connect the event point to
     *
     * @param point the system-based location
     * @return the most suitable chord, or null
     */
    public Chord findEventChord (SystemPoint point)
    {
        // Choose the x-closest slot
        Slot slot = getClosestSlot(point);

        if (slot != null) {
            // Choose the y-closest chord with normal (non-rest) note (WRONG !!!)
            // TO BE IMPROVED !!! TBD
            Chord chord = slot.getChordAbove(point);

            if (chord == null) {
                chord = slot.getChordBelow(point);
            }

            return chord;
        } else {
            return null;
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

    //---------------//
    // getClefBefore //
    //---------------//
    /**
     * Report the latest clef, if any, defined before this measure point
     * (looking in beginning of the measure, then in previous measures, then in
     * previous systems)
     *
     * @return the latest clef defined, or null
     */
    public Clef getClefBefore (SystemPoint point)
    {
        // Which staff we are in
        Clef clef = null;
        int  staffId = getPart()
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

        // Look in previous measures, with the same staff
        Measure measure = (Measure) getPreviousSibling();

        for (; measure != null;
             measure = (Measure) measure.getPreviousSibling()) {
            clef = measure.getLastMeasureClef(staffId);

            if (clef != null) {
                return clef;
            }
        }

        // Remember part index in system
        final int partIndex = getPart()
                                  .getId() - 1;

        // Look in previous system(s) of the page
        System system = (System) getSystem()
                                     .getPreviousSibling();

        for (; system != null; system = (System) system.getPreviousSibling()) {
            SystemPart prt = (SystemPart) system.getParts()
                                                .get(partIndex);

            for (int im = prt.getMeasures()
                             .size() - 1; im >= 0; im--) {
                measure = (Measure) prt.getMeasures()
                                       .get(im);
                clef = measure.getLastMeasureClef(staffId);

                if (clef != null) {
                    return clef;
                }
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
    public ClefList getClefList ()
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

        // Look in previous measures in the system
        Measure measure = (Measure) getPreviousSibling();

        for (; measure != null;
             measure = (Measure) measure.getPreviousSibling()) {
            ts = measure.getTimeSignature();

            if (ts != null) {
                return ts;
            }
        }

        // Remember part index in system
        final int partIndex = getPart()
                                  .getId() - 1;

        // Look in previous system(s) of the page
        System system = (System) getSystem()
                                     .getPreviousSibling();

        for (; system != null; system = (System) system.getPreviousSibling()) {
            SystemPart prt = (SystemPart) system.getParts()
                                                .get(partIndex);

            for (int im = prt.getMeasures()
                             .size() - 1; im >= 0; im--) {
                measure = (Measure) prt.getMeasures()
                                       .get(im);
                ts = measure.getTimeSignature();

                if (ts != null) {
                    return ts;
                }
            }
        }

        return null; // Not found !!!
    }

    //-------------//
    // getDuration //
    //-------------//
    /**
     * Report the duration of this measure, as computed from its contained
     * voices
     *
     * @return the (measured) measure duration
     */
    public int getDuration ()
    {
        return 0;
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

    //------------------//
    // getFinalDuration //
    //------------------//
    /**
     * Report how a given voice terminates in this measure
     *
     * @param voice the given voice
     * @return the duration delta at end of the measure (or null for whole rest)
     */
    public Integer getFinalDuration (int voice)
    {
        return finalDurations.get(voice);
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

    //--------------//
    // getKeyBefore //
    //--------------//
    /**
     * Report the key signature which applies in this measure, whether a key
     * signature actually starts this measure in the same staff, or whether a
     * key signature was found in a previous measure, for the same staff.
     *
     * @return the current time signature, or null if not found
     */
    public KeySignature getKeyBefore (SystemPoint point)
    {
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

        // Look in previous measures in the system
        Measure measure = (Measure) getPreviousSibling();

        for (; measure != null;
             measure = (Measure) measure.getPreviousSibling()) {
            ks = measure.getLastMeasureKey(staffId);

            if (ks != null) {
                return ks;
            }
        }

        // Remember part index in system
        final int partIndex = getPart()
                                  .getId() - 1;

        // Look in previous system(s) of the page
        System system = (System) getSystem()
                                     .getPreviousSibling();

        for (; system != null; system = (System) system.getPreviousSibling()) {
            SystemPart prt = (SystemPart) system.getParts()
                                                .get(partIndex);

            for (int im = prt.getMeasures()
                             .size() - 1; im >= 0; im--) {
                measure = (Measure) prt.getMeasures()
                                       .get(im);
                ks = measure.getLastMeasureKey(staffId);

                if (ks != null) {
                    return ks;
                }
            }
        }

        return null; // Not found !!!
    }

    //---------------//
    // getKeySigList //
    //---------------//
    public KeySigList getKeySigList ()
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

    //----------------//
    // getTimeSigList //
    //----------------//
    public TimeSigList getTimeSigList ()
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
        return (voicesNumber == null) ? 0 : voicesNumber;
    }

    //----------------//
    // getWholeChords //
    //----------------//
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
     * @return the measure width
     */
    public int getWidth ()
    {
        return getBarline()
                   .getCenter().x - getLeftX();
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
        barline.reset();
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

    //-------------//
    // setImplicit //
    //-------------//
    /**
     * Flag this measure as implicit
     */
    public void setImplicit ()
    {
        implicit = true;

        // Remove any final forward mark (to be improved ?)
        for (int voice = 1; voice <= getVoicesNumber(); voice++) {
            Integer duration = finalDurations.get(voice);

            if ((duration != null) && (duration < 0)) {
                Chord chord = finalChords.get(voice);

                if (chord != null) {
                    int nbMarks = chord.getMarks()
                                       .size();

                    if (nbMarks > 0) {
                        chord.getMarks()
                             .remove(chord.getMarks().get(nbMarks - 1));

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                getContextString() + " Final forward removed");
                        }
                    } else {
                        chord.addError(
                            "No final mark to remove in an implicit measure");
                    }
                } else {
                    addError("No final chord in voice " + voice);
                }
            }
        }
    }

    //-----------------//
    // setVoicesNumber //
    //-----------------//
    /**
     * Assign the number of voices in this measure
     *
     * @param voicesNumber total number of voices
     */
    public void setVoicesNumber (int voicesNumber)
    {
        this.voicesNumber = voicesNumber;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Measure id=" + id + "}";
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
    void addWholeChord (Glyph glyph)
    {
        Chord chord = new Chord(this);

        // No slot for this chord, but a whole rest
        Note note = new Note(chord, glyph);
        wholeChords.add(chord);
    }

    //---------------//
    // insertForward //
    //---------------//
    private void insertForward (int           duration,
                                Mark.Position position,
                                Chord         chord)
    {
        SystemPoint point = new SystemPoint(
            chord.getHeadLocation().x,
            (chord.getHeadLocation().y + chord.getTailLocation().y) / 2);

        if (position == Mark.Position.AFTER) {
            point.x += 10;
        } else if (position == Mark.Position.BEFORE) {
            point.x -= 10;
        }

        Mark mark = new Mark(
            chord.getSystem(),
            point,
            position,
            Shape.FORWARD,
            Note.quarterValueOf(duration));
        //new Integer(duration));
        chord.addMark(mark);
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------//
    // ClefList //
    //----------//
    public static class ClefList
        extends MeasureNode
    {
        ClefList (Measure measure)
        {
            super(measure);
        }
    }

    //-------------//
    // DynamicList //
    //-------------//
    //    private static class DynamicList
    //        extends MeasureNode
    //    {
    //        DynamicList (Measure measure)
    //        {
    //            super(measure);
    //        }
    //    }

    //------------//
    // KeySigList //
    //------------//
    public static class KeySigList
        extends MeasureNode
    {
        KeySigList (Measure measure)
        {
            super(measure);
        }
    }

    //    //-----------//
    //    // LyricList //
    //    //-----------//
    //    private static class LyricList
    //        extends MeasureNode
    //    {
    //        LyricList (Measure measure)
    //        {
    //            super(measure);
    //        }
    //    }

    //----------//
    // TextList //
    //----------//
    //    private static class TextList
    //        extends MeasureNode
    //    {
    //        TextList (Measure measure)
    //        {
    //            super(measure);

    //        }
    //    }

    //-------------//
    // TimeSigList //
    //-------------//
    public static class TimeSigList
        extends MeasureNode
    {
        TimeSigList (Measure measure)
        {
            super(measure);
        }
    }

    //----------//
    // BeamList //
    //----------//
    private static class BeamList
        extends MeasureNode
    {
        BeamList (Measure measure)
        {
            super(measure);
        }
    }

    //-----------//
    // ChordList //
    //-----------//
    private static class ChordList
        extends MeasureNode
    {
        ChordList (Measure measure)
        {
            super(measure);
        }
    }
}
