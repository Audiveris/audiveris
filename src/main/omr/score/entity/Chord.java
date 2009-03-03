//----------------------------------------------------------------------------//
//                                                                            //
//                                 C h o r d                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;

import omr.log.Logger;

import omr.score.common.DurationFactor;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.entity.Voice.ChordInfo;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.stick.Stick;

import omr.util.Implement;
import omr.util.TreeNode;

import java.awt.Polygon;
import java.util.*;

/**
 * Class <code>Chord</code> represents an ensemble of entities (rests, notes)
 * attached to the same stem if any, and that occur on the same time in a staff.
 * <p><b>NB</>We assume that all notes of a chord have the same duration.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Chord
    extends MeasureNode
    implements Comparable<Chord>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Chord.class);

    /** Compare two chords (slot first, then ordinate) within a measure */
    private static final Comparator<TreeNode> chordComparator = new Comparator<TreeNode>() {
        public int compare (TreeNode tn1,
                            TreeNode tn2)
        {
            Chord c1 = (Chord) tn1;
            Chord c2 = (Chord) tn2;

            return c1.compareTo(c2);
        }
    };

    /**
     * Compare two notes of the same chord, ordered by increasing distance from
     * chord head ordinate
     */
    private static Comparator<TreeNode> noteHeadComparator = new Comparator<TreeNode>() {
        public int compare (TreeNode tn1,
                            TreeNode tn2)
        {
            if (tn1 == tn2) {
                return 0;
            }

            Note n1 = (Note) tn1;
            Note n2 = (Note) tn2;

            if (n1.getChord() != n2.getChord()) {
                logger.severe("Ordering notes from different chords");
            }

            return n1.getChord()
                     .getStemDir() * (n2.getCenter().y - n1.getCenter().y);
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Id for debug (unique within measure) */
    private final int id;

    /** A chord stem is virtual when there is no real stem (breve, rest...) */
    private Glyph stem;

    /** Containing slot, if any (no slot for whole/multi rests) */
    private final Slot slot;

    /** Location for chord head (head farthest from chord tail) */
    private SystemPoint headLocation;

    /** Location for chord tail */
    private SystemPoint tailLocation;

    /** Ordered collection of beams this chord is connected to */
    private SortedSet<Beam> beams = new TreeSet<Beam>();

    /** Number of augmentation dots */
    private int dotsNumber;

    /** Number of flags (a beam is not a flag) */
    private int flagsNumber;

    /** Ratio to get actual rawDuration wrt graphical notation */
    private DurationFactor tupletFactor;

    /** Start time since beginning of the containing measure */
    private Integer startTime;

    /** Voice this chord belongs to */
    private Voice voice;

    /** Collection of marks for user */
    private List<Mark> marks = new ArrayList<Mark>();

    /** Notations related  to this chord */
    private List<Notation> notations = new ArrayList<Notation>();

    /** Directions (loosely) related to this chord */
    private List<Direction> directions = new ArrayList<Direction>();

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Chord //
    //-------//
    /**
     * Creates a new instance of Chord
     *
     * @param measure the containing measure
     * @param slot the containing slot (null for whole/multi rest chords)
     */
    public Chord (Measure measure,
                  Slot    slot)
    {
        super(measure);
        reset();
        this.slot = slot;
        id = measure.getChords()
                    .size();
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // isAllRests //
    //------------//
    /**
     * Checks whether this chord contains only rests (and no standard note)
     *
     * @return true is made of rests only
     */
    public boolean isAllRests ()
    {
        for (TreeNode node : getNotes()) {
            Note note = (Note) node;

            if (!note.isRest()) {
                return false;
            }
        }

        return true;
    }

    //--------------//
    // getBeamGroup //
    //--------------//
    /**
     * Report the group of beams this chord belongs to
     *
     * @return the related group of beams
     */
    public BeamGroup getBeamGroup ()
    {
        if (!getBeams()
                .isEmpty()) {
            return getBeams()
                       .first()
                       .getGroup();
        } else {
            return null;
        }
    }

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the ordered sequence of beams that are attached to this chord
     *
     * @return the ordered set of attached beams
     */
    public SortedSet<Beam> getBeams ()
    {
        return beams;
    }

    //---------------//
    // getDirections //
    //---------------//
    /**
     * Report the direction entities loosely related to this chord
     *
     * @return the collection of (loosely) related directions, perhaps empty
     */
    public Collection<?extends Direction> getDirections ()
    {
        return directions;
    }

    //---------------//
    // getDotsNumber //
    //---------------//
    /**
     * Report the number of augmentation dots that impact this chord
     *
     * @return the number of dots (should be the same for all notes within this
     * chord)
     */
    public int getDotsNumber ()
    {
        return dotsNumber;
    }

    //-------------//
    // getDuration //
    //-------------//
    /**
     * Report the real rawDuration computed for this chord, including the tuplet
     * impact if any, with null value for whole/multi rest.
     *
     * @return The real chord/note rawDuration, or null for a whole rest chord
     * @see #getRawDuration
     */
    public Integer getDuration ()
    {
        if (this.isWholeDuration()) {
            return null;
        } else {
            Integer raw = getRawDuration();

            if (tupletFactor == null) {
                return raw;
            } else {
                final int num = tupletFactor.getNumerator();
                final int den = tupletFactor.getDenominator();

                return (raw * num) / den;
            }
        }
    }

    //--------------//
    // isEmbracedBy //
    //--------------//
    /**
     * Check whether the notes of this chord stand within the given
     * vertical range
     *
     * @param top top of vertical range
     * @param bottom bottom of vertical range
     * @return true if all notes are within the given range
     */
    public boolean isEmbracedBy (SystemPoint top,
                                 SystemPoint bottom)
    {
        for (TreeNode node : getNotes()) {
            Note        note = (Note) node;
            SystemPoint center = note.getCenter();

            if ((center.y >= top.y) && (center.y <= bottom.y)) {
                return true;
            }
        }

        return false;
    }

    //------------//
    // getEndTime //
    //------------//
    /**
     * Report the time when this chord ends
     *
     * @return chord ending time, since beginning of the measure
     */
    public Integer getEndTime ()
    {
        if (isWholeDuration()) {
            return null;
        }

        Integer chordDur = getDuration();

        if (chordDur == null) {
            return null;
        } else {
            return startTime + chordDur;
        }
    }

    //----------------//
    // getFlagsNumber //
    //----------------//
    /**
     * Report the number of flags attached to the chord stem
     *
     * @return the number of flags
     */
    public int getFlagsNumber ()
    {
        return flagsNumber;
    }

    //------------------------//
    // getFollowingTiedChords //
    //------------------------//
    /**
     * Report the x-ordered collection of chords which are directly tied to
     * the right of this chord
     *
     * @return the (perhaps empty) collection of tied chords
     */
    public SortedSet<Chord> getFollowingTiedChords ()
    {
        SortedSet<Chord> tied = new TreeSet<Chord>();

        for (TreeNode node : children) {
            Note note = (Note) node;

            for (Slur slur : note.getSlurs()) {
                if (slur.isTie() &&
                    (slur.getLeftNote() == note) &&
                    (slur.getRightNote() != null)) {
                    tied.add(slur.getRightNote().getChord());
                }
            }
        }

        return tied;
    }

    //-----------------//
    // getHeadLocation //
    //-----------------//
    /**
     * Report the system-based location of the chord head (the head which is
     * farthest from the tail)
     *
     * @return the head location
     */
    public SystemPoint getHeadLocation ()
    {
        if (headLocation == null) {
            computeLocations();
        }

        return headLocation;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the id of the chord within the measure (meant for debug)
     * @return the unique id within the measure
     */
    public int getId ()
    {
        return id;
    }

    //----------//
    // getNotes //
    //----------//
    /**
     * Report the collection of UI marks related to this chord
     *
     * @return the collection of marks, perhaps empty
     */
    public List<Mark> getMarks ()
    {
        return marks;
    }

    //--------------//
    // getNotations //
    //--------------//
    /**
     * Report the (perhaps empty) collection of related notations
     *
     * @return the collection of notations
     */
    public Collection<?extends Notation> getNotations ()
    {
        return notations;
    }

    //----------//
    // getNotes //
    //----------//
    /**
     * Report all the notes that compose this chord
     *
     * @return the chord notes
     */
    public List<TreeNode> getNotes ()
    {
        return children;
    }

    //-------------------------//
    // getPreviousChordInVoice //
    //-------------------------//
    /**
     * Report the chord that occurs right before this one, within the same voice
     *
     * @return the previous chord within the same voice
     */
    public Chord getPreviousChordInVoice ()
    {
        return voice.getPreviousChord(this);
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * Report the staff that contains this chord (we use the staff of the notes)
     *
     * @return the chord staff
     */
    @Override
    public Staff getStaff ()
    {
        if (super.getStaff() == null) {
            if (!getNotes()
                    .isEmpty()) {
                Note note = (Note) getNotes()
                                       .get(0);
                setStaff(note.getStaff());
            }
        }

        return super.getStaff();
    }

    //--------------//
    // setStartTime //
    //--------------//
    /**
     * Remember the starting time for this chord
     *
     * @param startTime chord starting time (counted within the measure)
     */
    public void setStartTime (int startTime)
    {
        // Already done?
        if (this.startTime == null) {
            if (logger.isFineEnabled()) {
                logger.info(
                    "setStartTime " + startTime + " for chord #" + getId());
            }

            this.startTime = startTime;

            // Set the same info in containing slot if any
            if (slot != null) {
                slot.setStartTime(startTime);
            }
        } else {
            if (!this.startTime.equals(startTime)) {
                addError(
                    "Reassigning startTime from " +
                    Note.quarterValueOf(this.startTime) + " to " +
                    Note.quarterValueOf(startTime) + " in " + this);
            }
        }
    }

    //---------//
    // setStem //
    //---------//
    /**
     * Assign the proper stem to this chord
     *
     * @param stem the chord stem
     */
    public void setStem (Glyph stem)
    {
        this.stem = stem;
    }

    //---------//
    // getStem //
    //---------//
    /**
     * Report the stem of this chord (or null in the case of chord with virtual
     * stem)
     *
     * @return the chord stem, or null
     */
    public Glyph getStem ()
    {
        return stem;
    }

    //---------------//
    // getStemChords //
    //---------------//
    /**
     * Find the chord(s) that are carried by a given stem
     *
     * @param measure the containing measure
     * @param stem the given stem
     * @return the collection of related chords, which may be empty if no chord
     *         is yet attached to this stem
     */
    public static List<Chord> getStemChords (Measure measure,
                                             Glyph   stem)
    {
        List<Chord> chords = new ArrayList<Chord>();

        ///stem.clearTranslations();
        for (TreeNode node : measure.getChords()) {
            Chord chord = (Chord) node;

            if (chord.getStem() == stem) {
                chords.add(chord);

                ///stem.addTranslation(chord);
            }
        }

        return chords;
    }

    //-----------------//
    // getTailLocation //
    //-----------------//
    /**
     * Report the system-based location of the tail of the chord
     *
     * @return the tail location
     */
    public SystemPoint getTailLocation ()
    {
        if (tailLocation == null) {
            computeLocations();
        }

        return tailLocation;
    }

    //-----------------//
    // setTupletFactor //
    //-----------------//
    /**
     * Assign a tuplet factor to this chord
     *
     * @param tupletFactor the factor to apply
     */
    public void setTupletFactor (DurationFactor tupletFactor)
    {
        this.tupletFactor = tupletFactor;
    }

    //-----------------//
    // getTupletFactor //
    //-----------------//
    /**
     * Report the chord tuplet factor, if any
     *
     * @return the factor to apply, or nulkl
     */
    public DurationFactor getTupletFactor ()
    {
        return tupletFactor;
    }

    //----------//
    // setVoice //
    //----------//
    /**
     * Assign a voice to this chord
     *
     * @param voice the voice to assign
     */
    public void setVoice (Voice voice)
    {
        // Already done?
        if (this.voice == null) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    getContextString() + " Ch#" + id + " setVoice " +
                    voice.getId());
            }

            this.voice = voice;

            // Update the voice entity
            if (!isWholeDuration()) {
                voice.setSlotInfo(
                    slot,
                    new ChordInfo(this, Voice.Status.BEGIN));

                // Extend this info to otherChord beamed chords if any
                BeamGroup group = getBeamGroup();

                if (group != null) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            getContextString() + " Ch#" + id +
                            " extending voice#" + voice.getId() + " to group#" +
                            group.getId());
                    }

                    group.setVoice(voice);
                }

                // Extend to the following tied chords as well
                SortedSet<Chord> tied = getFollowingTiedChords();

                for (Chord chord : tied) {
                    if (logger.isFineEnabled()) {
                        logger.fine(this + " tied to " + chord);
                    }

                    // Check the tied chords belong to the same measure
                    if (this.getMeasure() == chord.getMeasure()) {
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                getContextString() + " Ch#" + id +
                                " extending voice#" + voice.getId() +
                                " to tied chord#" + chord.getId());
                        }

                        chord.setVoice(voice);
                    } else {
                        // Chords tied across measure boundary
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                getContextString() + " Cross tie " +
                                toShortString() + " -> " +
                                chord.toShortString());
                        }
                    }
                }
            }
        } else if (this.voice != voice) {
            addError(
                "Chord. Attempt to reassign voice from " + this.voice.getId() +
                " to " + voice.getId() + " in " + this);
        }
    }

    //----------//
    // getVoice //
    //----------//
    /**
     * Report the (single) voice used by the notes of this chord
     *
     * @return the chord voice
     */
    public Voice getVoice ()
    {
        return voice;
    }

    //-----------------//
    // isWholeDuration //
    //-----------------//
    /**
     * Check whether the chord/note  is a whole rest
     *
     * @return true if whole
     */
    public boolean isWholeDuration ()
    {
        if (!getNotes()
                .isEmpty()) {
            Note note = (Note) getNotes()
                                   .get(0);

            return note.getShape()
                       .isWholeRest();
        }

        return false;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //---------//
    // addBeam //
    //---------//
    /**
     * Insert a beam as attached to this chord
     *
     * @param beam the attached beam
     */
    public void addBeam (Beam beam)
    {
        beams.add(beam);
    }

    //----------//
    // addChild //
    //----------//
    /**
     * Override normal behavior, so that adding a note resets chord internal
     * parameters
     *
     * @param node the child to insert in the chord
     */
    @Override
    public void addChild (TreeNode node)
    {
        super.addChild(node);

        // Side effect for note, since the geometric parameters of the chord
        // are modified
        if (node instanceof Note) {
            reset();
        }
    }

    //--------------//
    // addDirection //
    //--------------//
    /**
     * Add a direction element that should appear right before the chord
     * first note
     *
     * @param direction the direction element to add
     */
    public void addDirection (Direction direction)
    {
        directions.add(direction);
    }

    //---------//
    // addMark //
    //---------//
    /**
     * Add a UI mark to this chord
     *
     * @param mark the mark to add
     */
    public void addMark (Mark mark)
    {
        marks.add(mark);
    }

    //-------------//
    // addNotation //
    //-------------//
    /**
     * Add a notation element related to this chord note(s)
     *
     * @param notation the notation element to add
     */
    public void addNotation (Notation notation)
    {
        notations.add(notation);
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Compare this chord with another chord, to implement order based first on
     * slot abscissa, then on head ordinate within the same slot, finally by id
     *
     * @param otherChord the other chord
     * @return -1, 0, +1 according to the comparison result
     */
    @Implement(Comparable.class)
    public int compareTo (Chord otherChord)
    {
        Slot otherSlot = otherChord.getSlot();

        // Slot first
        if (slot == null) {
            if (otherSlot == null) {
                return Integer.signum(
                    getHeadLocation().y - otherChord.getHeadLocation().y);
            } else {
                return -1; // Wholes are before slot-based chords
            }
        } else {
            if (otherSlot == null) {
                return +1; // Wholes are before slot-based chords
            } else if (slot != otherSlot) {
                // Slot comparison
                return slot.compareTo(otherSlot);
            } else {
                // Chord comparison within the same slot
                return Slot.chordComparator.compare(this, otherChord);
            }
        }
    }

    //-----------//
    // duplicate //
    //-----------//
    /**
     * Make a clone of a chord (except for its beams). This duplication is
     * needed in cases such as: a note head with stems on both sides, or a chord
     * shared by two BeamGroups.
     *
     * @return a clone of this chord (including notes, but beams are not copied)
     */
    public Chord duplicate ()
    {
        // Beams are not copied
        Chord clone = new Chord(getMeasure(), slot);
        clone.stem = stem;

        // Notes (we make a deep copy of each note)
        List<TreeNode> notesCopy = new ArrayList<TreeNode>();
        notesCopy.addAll(getNotes());

        for (TreeNode node : notesCopy) {
            Note note = (Note) node;
            new Note(clone, note);
        }

        if (tupletFactor != null) {
            clone.tupletFactor = new DurationFactor(
                tupletFactor.getNumerator(),
                tupletFactor.getDenominator());
        }

        clone.dotsNumber = dotsNumber;
        clone.flagsNumber = flagsNumber; // Not sure TBD

        // Insure correct ordering of chords within their container
        Collections.sort(getParent().getChildren(), chordComparator);

        return clone;
    }

    //------------//
    // lookupRest //
    //------------//
    /**
     * Look up for a potential rest interleaved between the given stemed chords
     * @param left the chord on the left of the area
     * @param right the chord on the right of the area
     * @return the rest found, or null otherwise
     */
    public static Note lookupRest (Chord left,
                                   Chord right)
    {
        // Define the area limited by the left and right chords with their stems
        // and check for intersection with a rest note
        Polygon polygon = new Polygon();
        polygon.addPoint(left.headLocation.x, left.headLocation.y);
        polygon.addPoint(left.tailLocation.x, left.tailLocation.y);
        polygon.addPoint(right.tailLocation.x, right.tailLocation.y);
        polygon.addPoint(right.headLocation.x, right.headLocation.y);

        for (TreeNode node : left.getMeasure()
                                 .getChords()) {
            Chord chord = (Chord) node;

            // Not interested in the bounding chords
            if ((chord == left) || (chord == right)) {
                continue;
            }

            for (TreeNode n : chord.getNotes()) {
                Note note = (Note) n;

                // Interested in rest notes only
                if (note.isRest()) {
                    SystemRectangle box = note.getBox();

                    if (polygon.intersects(box.x, box.y, box.width, box.height)) {
                        return note;
                    }
                }
            }
        }

        return null;
    }

    //-------------//
    // populateDot //
    //-------------//
    /**
     * Try to assign an augmentation dot to the relevant chord if any,
     * otherwise try to assign it to a repeat barline.
     *
     * @param glyph the glyph of the given augmentation dot
     * @param measure the containing measure
     * @param dotCenter the system-based location of the dot
     */
    public static void populateDot (Glyph       glyph,
                                    Measure     measure,
                                    SystemPoint dotCenter)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Chord Populating dot " + glyph);
        }

        // A dot can be: an augmentation dot; part of repeat dots; staccato
        if (!tryAugmentation(glyph, measure, dotCenter)) {
            if (!tryRepeat(glyph, measure, dotCenter)) {
                if (!tryStaccato(glyph, measure, dotCenter)) {
                    // No translation for this dot!
                }
            }
        }
    }

    //--------------//
    // populateFlag //
    //--------------//
    /**
     * Try to assign a flag to a relevant chord
     *
     * @param glyph the underlying glyph of this flag
     * @param measure the containing measure
     */
    public static void populateFlag (Glyph   glyph,
                                     Measure measure)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Chord Populating flag " + glyph);
        }

        // Retrieve the related chord
        Glyph stem = null;

        if (glyph.getLeftStem() != null) {
            stem = glyph.getLeftStem();
        } else if (glyph.getRightStem() != null) {
            stem = glyph.getRightStem();
        }

        if (stem == null) {
            measure.addError(glyph, "Flag glyph with no stem");
        } else {
            List<Chord> sideChords = Chord.getStemChords(measure, stem);

            if (!sideChords.isEmpty()) {
                for (Chord chord : sideChords) {
                    chord.flagsNumber += getFlagValue(glyph);
                    glyph.addTranslation(chord);
                }
            } else {
                measure.addError(stem, "No chord for stem " + stem.getId());
            }
        }
    }

    //---------//
    // getSlot //
    //---------//
    /**
     * Report the slot this chord belongs to
     *
     * @return the containing slot (or null if not found)
     */
    public Slot getSlot ()
    {
        return slot;
    }

    //--------------//
    // getStartTime //
    //--------------//
    /**
     * Report the starting time for this chord
     *
     * @return startTime chord starting time (counted within the measure)
     */
    public Integer getStartTime ()
    {
        return startTime;
    }

    //------------//
    // getStemDir //
    //------------//
    /**
     * Report the stem direction of this chord
     *
     * @return -1 if stem is down, 0 if no stem, +1 if stem is up
     */
    public int getStemDir ()
    {
        if (stem == null) {
            return 0;
        } else {
            return Integer.signum(getHeadLocation().y - getTailLocation().y);
        }
    }

    //-----------//
    // checkTies //
    //-----------//
    /**
     * Check that all incoming ties come from the same chord, and similarly that
     * all outgoing ties go to the same chord.
     */
    public void checkTies ()
    {
        SplitOrder order = null;
        // Incoming ties
        order = checkTies(
            new TieRelation() {
                    public Note getDistantNote (Slur slur)
                    {
                        return slur.getLeftNote();
                    }

                    public Note getLocalNote (Slur slur)
                    {
                        return slur.getRightNote();
                    }
                });

        if (order != null) {
            split(order);

            return;
        }

        // Outgoing ties
        order = checkTies(
            new TieRelation() {
                    public Note getDistantNote (Slur slur)
                    {
                        return slur.getRightNote();
                    }

                    public Note getLocalNote (Slur slur)
                    {
                        return slur.getLeftNote();
                    }
                });

        if (order != null) {
            split(order);
        }
    }

    //--------------//
    // toLongString //
    //--------------//
    /**
     * Report a more detailed description than plain toStrïng
     * @return a detailed description
     */
    public String toLongString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this);
        sb.deleteCharAt(sb.length() - 1); // Remove trailing "}"

        try {
            if (headLocation != null) {
                sb.append(" head[x=")
                  .append(headLocation.x)
                  .append(",y=")
                  .append(headLocation.y)
                  .append("]");
            }

            if (tailLocation != null) {
                sb.append(" tail[x=")
                  .append(tailLocation.x)
                  .append(",y=")
                  .append(tailLocation.y)
                  .append("]");
            }

            if (!beams.isEmpty()) {
                try {
                    sb.append(
                        " beams G#" + beams.first().getGroup().getId() + "[");

                    for (Beam beam : beams) {
                        sb.append(beam + " ");
                    }

                    sb.append("]");
                } catch (Exception ex) {
                    logger.warning("Exception in chord toLongString()");
                }
            }
        } catch (NullPointerException e) {
            sb.append(" INVALID");
        }

        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // toShortString //
    //---------------//
    /**
     * A description meant for constrained labels
     * @return a short chord description
     */
    public String toShortString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        if (getVoice() != null) {
            sb.append("Voice#")
              .append(getVoice().getId());
        }

        sb.append(" Chord#")
          .append(getId());

        sb.append(" dur:");

        if (isWholeDuration()) {
            sb.append("W");
        } else {
            Integer chordDur = getDuration();

            if (chordDur != null) {
                sb.append(Note.quarterValueOf(chordDur));
            } else {
                sb.append("none");
            }
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
        StringBuilder sb = new StringBuilder();
        sb.append("{Chord");

        try {
            sb.append("#")
              .append(id);

            if (voice != null) {
                sb.append(" voice#")
                  .append(voice.getId());
            }

            // Staff ?
            if (!getNotes()
                     .isEmpty()) {
                Note note = (Note) getNotes()
                                       .get(0);

                if (note != null) {
                    sb.append(" staff#")
                      .append(note.getStaff().getId());
                }
            }

            if (startTime != null) {
                sb.append(" start=")
                  .append(Note.quarterValueOf(startTime));
            }

            sb.append(" dur=");

            if (isWholeDuration()) {
                sb.append("W");
            } else {
                Integer chordDur = getDuration();

                if (chordDur != null) {
                    sb.append(Note.quarterValueOf(chordDur));
                } else {
                    sb.append("none");
                }
            }

            if (stem != null) {
                sb.append(" stem#")
                  .append(stem.getId());
            }

            if (tupletFactor != null) {
                sb.append(" tupletFactor=")
                  .append(tupletFactor);
            }

            if (dotsNumber != 0) {
                sb.append(" dots=")
                  .append(dotsNumber);
            }

            if (flagsNumber != 0) {
                sb.append(" flags=")
                  .append(flagsNumber);
            }
        } catch (NullPointerException e) {
            sb.append(" INVALID");
        }

        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // computeCenter //
    //---------------//
    /**
     * Compute the center of Chord
     */
    @Override
    protected void computeCenter ()
    {
        setCenter(getHeadLocation());
    }

    //--------------//
    // getFlagValue //
    //--------------//
    /**
     * Report the number of flags that corresponds to the flag glyph
     *
     * @param glyph the given flag glyph
     * @return the number of flags
     */
    private static int getFlagValue (Glyph glyph)
    {
        switch (glyph.getShape()) {
        case COMBINING_FLAG_1 :
        case COMBINING_FLAG_1_UP :
        case HEAD_AND_FLAG_1 :
        case HEAD_AND_FLAG_1_UP :
            return 1;

        case COMBINING_FLAG_2 :
        case COMBINING_FLAG_2_UP :
        case HEAD_AND_FLAG_2 :
        case HEAD_AND_FLAG_2_UP :
            return 2;

        case COMBINING_FLAG_3 :
        case COMBINING_FLAG_3_UP :
        case HEAD_AND_FLAG_3 :
        case HEAD_AND_FLAG_3_UP :
            return 3;

        case COMBINING_FLAG_4 :
        case COMBINING_FLAG_4_UP :
        case HEAD_AND_FLAG_4 :
        case HEAD_AND_FLAG_4_UP :
            return 4;

        case COMBINING_FLAG_5 :
        case COMBINING_FLAG_5_UP :
        case HEAD_AND_FLAG_5 :
        case HEAD_AND_FLAG_5_UP :
            return 5;
        }

        logger.severe("Illegal flag shape: " + glyph.getShape());

        return 0;
    }

    //----------------//
    // getRawDuration //
    //---------------//
    /**
     * Report the intrinsic duration of this chord, taking flag/beams and dots
     * into account, but not the tuplet impact if any
     * Duration (assumed to be the same for all notes of this chord, otherwise
     * the chord must be split.
     * This includes the local information (flags, dots) but not the tuplet
     * impact if any.
     * A specific value (WHOLE_DURATION) indicates the whole/multi rest chord.
     *
     * Nota: this value is not cached, but computed at every time
     *
     * @return the intrinsic chord rawDuration
     * @see #getDuration
     */
    private Integer getRawDuration ()
    {
        Integer rawDuration = null;

        if (!getNotes()
                .isEmpty()) {
            // All note heads are assumed to be the same within one chord
            Note note = (Note) getNotes()
                                   .get(0);

            if (!note.getShape()
                     .isWholeRest()) {
                rawDuration = Note.getTypeDuration(note.getShape());

                // Apply fraction
                int fbn = getFlagsNumber() + getBeams()
                                                 .size();

                for (int i = 0; i < fbn; i++) {
                    rawDuration /= 2;
                }

                // Apply augmentation
                if (dotsNumber == 1) {
                    rawDuration += (rawDuration / 2);
                } else if (dotsNumber == 2) {
                    rawDuration += ((rawDuration * 3) / 4);
                }
            }
        }

        return rawDuration;
    }

    //-----------//
    // checkTies //
    //-----------//
    /**
     * For this chord, check either the incoming of the outgoing ties according
     * to the TieRelation information. For true ties (slurs linking notes with
     * same pitch) we make sure there is no more than one distant chord. If not,
     * we split the chord in two, so that each (sub)chord has only consistent
     * ties.
     * @param tie info about the relation between the slur and this chord
     * @return how to split the chord, or null if no split is needed
     */
    private SplitOrder checkTies (TieRelation tie)
    {
        List<Note>       distantNotes = new ArrayList<Note>();
        SortedSet<Chord> distantChords = new TreeSet<Chord>();

        for (TreeNode nn : getNotes()) {
            Note note = (Note) nn;

            for (Slur slur : note.getSlurs()) {
                if (tie.isRelevant(slur, note)) {
                    Note distantNote = tie.getDistantNote(slur);

                    if ((distantNote != null) &&
                        (distantNote.getMeasure() == getMeasure())) {
                        distantNotes.add(distantNote);
                        distantChords.add(distantNote.getChord());
                    }
                }
            }
        }

        if (distantChords.size() > 1) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    getContextString() + " Ch#" + getId() +
                    " with multiple tied chords: " + distantChords);
            }

            // Prepare the split of this chord, using the most distant note from 
            // chord head
            SortedSet<Note> tiedNotes = new TreeSet<Note>(noteHeadComparator);

            for (Note distantNote : distantNotes) {
                for (Slur slur : distantNote.getSlurs()) {
                    Note note = tie.getLocalNote(slur);

                    if ((note != null) && (note.getChord() == this)) {
                        tiedNotes.add(note);
                    }
                }
            }

            if (logger.isFineEnabled()) {
                logger.fine("Splitting from " + tiedNotes.last());
            }

            return new SplitOrder(this, tiedNotes.last());
        } else {
            return null;
        }
    }

    //-----------------//
    // tryAugmentation //
    //-----------------//
    /**
     * Try to assign a dot as a chord augmentation dot
     *
     * @param glyph the glyph of the given dot
     * @param measure the containing measure
     * @param dotCenter the system-based location of the dot
     * @return true if assignment is successful
     */
    private static boolean tryAugmentation (Glyph       glyph,
                                            Measure     measure,
                                            SystemPoint dotCenter)
    {
        Scale      scale = measure.getScale();
        final int  maxDx = scale.toUnits(constants.maxAugmentationDotDx);
        final int  maxDy = scale.toUnits(constants.maxAugmentationDotDy);
        Set<Chord> candidates = new HashSet<Chord>();

        // Check for a note/rest nearby:
        // - on the left w/ same even pitch (note w/ even pitch)
        // - slighly above or below (note with odd pitch = on a staff line)
        ChordLoop: 
        for (TreeNode node : measure.getChords()) {
            Chord chord = (Chord) node;

            for (TreeNode n : chord.getNotes()) {
                Note note = (Note) n;

                if (!note.getShape()
                         .isWholeRest()) {
                    SystemPoint noteRef = note.getCenterRight();
                    SystemPoint toDot = new SystemPoint(
                        dotCenter.x - noteRef.x,
                        dotCenter.y - noteRef.y);

                    if (logger.isFineEnabled()) {
                        logger.info(measure.getContextString() + " " + toDot);
                    }

                    if ((toDot.x > 0) &&
                        (toDot.x <= maxDx) &&
                        (Math.abs(toDot.y) <= maxDy)) {
                        candidates.add(chord);
                    }
                }
            }
        }

        // Assign the dot to the candidate with longest rawDuration, which boils
        // down to smallest number of flags/beams, as the note head is the same
        if (logger.isFineEnabled()) {
            logger.info(candidates.size() + " Candidates=" + candidates);
        }

        int   bestFb = Integer.MAX_VALUE;
        Chord bestChord = null;

        for (Chord chord : candidates) {
            int fb = chord.getFlagsNumber() + chord.getBeams()
                                                   .size();

            if (fb < bestFb) {
                bestFb = fb;
                bestChord = chord;
            }
        }

        if (bestChord != null) {
            // TBD: we should also handle case of double dots !
            bestChord.dotsNumber = 1;
            glyph.setTranslation(bestChord);

            if (logger.isFineEnabled()) {
                logger.fine(
                    bestChord.getContextString() + " Augmented " + bestChord);
            }

            return true;
        }

        return false;
    }

    //-----------//
    // tryRepeat //
    //-----------//
    /**
     * Try to assign a dot to the relevant repeat barline if any
     *
     * @param glyph the glyph of the given dot
     * @param measure the containing measure
     * @param dotCenter the system-based location of the dot
     * @return true if assignment is successful
     */
    private static boolean tryRepeat (Glyph       glyph,
                                      Measure     measure,
                                      SystemPoint dotCenter)
    {
        // Check vertical pitch position within the staff : close to +1 or -1
        double pitchDif = Math.abs(Math.abs(glyph.getPitchPosition()) - 1);

        if (pitchDif > (2 * constants.maxRepeatDotDy.getValue())) {
            return false;
        }

        // Check abscissa wrt the (ending) repeat barline on right
        Barline     barline = measure.getBarline();
        int         dx = barline.getLeftX() - dotCenter.x;
        final Scale scale = measure.getScale();
        final int   maxDx = scale.toUnits(constants.maxRepeatDotDx);

        if ((dx > 0) && (dx <= maxDx)) {
            barline.addStick((Stick) glyph);
            glyph.setTranslation(barline);

            return true;
        }

        // Check abscissa wrt the ending barline of the previous measure on left
        Measure prevMeasure = (Measure) measure.getPreviousSibling();

        if (prevMeasure != null) {
            barline = prevMeasure.getBarline();
            dx = dotCenter.x - barline.getRightX();

            if ((dx > 0) && (dx <= maxDx)) {
                barline.addStick((Stick) glyph);
                glyph.setTranslation(barline);

                return true;
            }
        }

        return false;
    }

    //-------------//
    // tryStaccato //
    //-------------//
    /**
     * Try to assign a dot as a staccato
     *
     * @param glyph the glyph of the given dot
     * @param measure the containing measure
     * @param dotCenter the system-based location of the dot
     * @return true if assignment is successful
     */
    private static boolean tryStaccato (Glyph       glyph,
                                        Measure     measure,
                                        SystemPoint dotCenter)
    {
        return false;
    }

    //-----------------//
    // getHeadLocation //
    //-----------------//
    /**
     * Compute the head location of a chord, given the chord head note
     *
     * @param note the head note
     * @return the head location
     */
    private SystemPoint getHeadLocation (Note note)
    {
        Staff staff = note.getStaff();

        return new SystemPoint(
            tailLocation.x,
            staff.getPageTopLeft().y - note.getSystem().getTopLeft().y +
            Staff.pitchToUnit(Math.rint(note.getPitchPosition())));
    }

    //------------------//
    // computeLocations //
    //------------------//
    /**
     * Compute the head and tail locations for this chord.
     */
    private void computeLocations ()
    {
        ScoreSystem system = getSystem();

        // Find the note farthest from stem middle point
        if (!getNotes()
                .isEmpty()) {
            if (stem != null) {
                SystemPoint middle = system.toSystemPoint(stem.getLocation());
                Note        bestNote = null;
                int         bestDy = Integer.MIN_VALUE;

                for (TreeNode node : getNotes()) {
                    Note note = (Note) node;
                    int  noteY = note.getCenter().y;
                    int  dy = Math.abs(noteY - middle.y);

                    if (dy > bestDy) {
                        bestNote = note;
                        bestDy = dy;
                    }
                }

                PixelRectangle stemBox = stem.getContourBox();

                if (middle.y < bestNote.getCenter().y) {
                    // Stem is up
                    tailLocation = system.toSystemPoint(
                        new PixelPoint(
                            stemBox.x + (stemBox.width / 2),
                            stemBox.y));
                } else {
                    // Stem is down
                    tailLocation = system.toSystemPoint(
                        new PixelPoint(
                            stemBox.x + (stemBox.width / 2),
                            (stemBox.y + stemBox.height)));
                }

                headLocation = getHeadLocation(bestNote);
            } else {
                Note note = (Note) getNotes()
                                       .get(0);
                headLocation = note.getCenter();
                tailLocation = headLocation;
            }
        } else {
            logger.warning("No notes in chord " + this);
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reset all internal data that depends on the chord composition in
     * terms of notes
     */
    private void reset ()
    {
        headLocation = null;
        tailLocation = null;
        startTime = null;
    }

    //-------//
    // split //
    //-------//
    /**
     * Apply the split order on this chord, which is impacted by the split
     *
     * @param order the details of the split order
     * @return the created chord
     */
    private Chord split (SplitOrder order)
    {
        if (logger.isFineEnabled()) {
            logger.fine(order.toString());
            logger.fine("Initial notes=" + getNotes());
        }

        // Same measure & slot 
        Chord alien = new Chord(getMeasure(), slot);

        // Same stem?
        alien.stem = stem;

        // Tuplet factor, if any, is copied
        if (tupletFactor != null) {
            alien.tupletFactor = new DurationFactor(
                tupletFactor.getNumerator(),
                tupletFactor.getDenominator());
        }

        // Augmentation dots as well
        alien.dotsNumber = dotsNumber;

        // Beams are not copied
        alien.flagsNumber = flagsNumber; // Not sure TBD

        // Notes, sorted from head
        Collections.sort(getNotes(), noteHeadComparator);

        boolean started = false;

        for (TreeNode tn : getChildrenCopy()) {
            Note note = (Note) tn;

            if (note == order.alienNote) {
                started = true;
            }

            if (started) {
                note.moveTo(alien);
            }
        }

        // Locations of the old and the new chord
        alien.tailLocation = this.tailLocation;
        alien.headLocation = getHeadLocation(order.alienNote);
        this.tailLocation = alien.headLocation;

        // Include the new chord in its slot
        slot.getChords()
            .add(alien);

        // Insure correct ordering of chords within their container
        Collections.sort(getParent().getChildren(), chordComparator);

        if (logger.isFineEnabled()) {
            logger.fine("Remaining notes=" + getNotes());
            logger.fine("Remaining " + this.toLongString());
            logger.fine("Alien notes=" + alien.getNotes());
            logger.fine("Alien " + alien.toLongString());
        }

        return alien;
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // SplitOrder //
    //------------//
    /**
     * Class <code>SplitOrder</code> records a chord split order.
     * Splitting must be separate from browsing to avoid concurrent modification
     * of collections
     */
    public static class SplitOrder
    {
        //~ Instance fields ----------------------------------------------------

        /** The chord to be split */
        final Chord chord;

        /** The first note of this chord to feed an alien chord  */
        final Note alienNote;

        //~ Constructors -------------------------------------------------------

        /**
         *
         * @param chord
         * @param alienNote
         */
        public SplitOrder(Chord chord,
                           Note  alienNote)
        {
            this.chord = chord;
            this.alienNote = alienNote;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{Split");
            sb.append(" chord=")
              .append(chord);
            sb.append(" alienNote=")
              .append(alienNote);
            sb.append("}");

            return sb.toString();
        }
    }

    //-------------//
    // TieRelation //
    //-------------//
    private abstract static class TieRelation
    {
        //~ Methods ------------------------------------------------------------

        public abstract Note getDistantNote (Slur slur);

        public abstract Note getLocalNote (Slur slur);

        public boolean isRelevant (Slur slur,
                                   Note note)
        {
            return slur.isTie() && (getLocalNote(slur) == note);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /**
         * Maximum dx between note and augmentation dot
         */
        Scale.Fraction maxAugmentationDotDx = new Scale.Fraction(
            1.5d,
            "Maximum dx between note and augmentation dot");

        /**
         * Maximum absolute dy between note and augmentation dot
         */
        Scale.Fraction maxAugmentationDotDy = new Scale.Fraction(
            1d,
            "Maximum absolute dy between note and augmentation dot");

        /**
         * Margin for vertical position of a dot againt a repeat barline
         */
        Scale.Fraction maxRepeatDotDy = new Scale.Fraction(
            0.5d,
            "Margin for vertical position of a dot againt a repeat barline");

        /**
         * Maximum dx between dot and edge of repeat barline
         */
        Scale.Fraction maxRepeatDotDx = new Scale.Fraction(
            1.5d,
            "Maximum dx between dot and edge of repeat barline");
    }
}
