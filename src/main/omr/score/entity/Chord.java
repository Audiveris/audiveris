//----------------------------------------------------------------------------//
//                                                                            //
//                                 C h o r d                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.math.Rational;

import omr.score.entity.Voice.VoiceChord;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.util.HorizontalSide;
import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code Chord} represents an ensemble of entities (rests, notes)
 * attached to the same stem if any, and that occur on the same time in
 * a staff.
 * <p><b>NB</b>We assume that all notes of a chord have the same duration.
 *
 * @author Hervé Bitteur
 */
public class Chord
        extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Chord.class);

    /** Compare two chords by abscissa within a measure. */
    public static final Comparator<TreeNode> byNodeAbscissa = new Comparator<TreeNode>()
    {
        @Override
        public int compare (TreeNode tn1,
                            TreeNode tn2)
        {
            Chord c1 = (Chord) tn1;
            Chord c2 = (Chord) tn2;

            return byAbscissa.compare(c1, c2);
        }
    };

    /** Compare two chords by abscissa within a measure. */
    public static final Comparator<Chord> byAbscissa = new Comparator<Chord>()
    {
        @Override
        public int compare (Chord c1,
                            Chord c2)
        {
            return Integer.signum(c1.getHeadLocation().x - c2.getHeadLocation().x);
        }
    };

    /** Comparator to sort chords vertically within the same slot. */
    public static final Comparator<Chord> byOrdinate = new Comparator<Chord>()
    {
        @Override
        public int compare (Chord c1,
                            Chord c2)
        {
            Note n1 = (Note) c1.getNotes().get(0);
            Note n2 = (Note) c2.getNotes().get(0);

            // First : staff
            int dStaff = n1.getStaff().getId() - n2.getStaff().getId();

            if (dStaff != 0) {
                return Integer.signum(dStaff);
            }

            // Second : head ordinate
            int dHead = c1.getHeadLocation().y - c2.getHeadLocation().y;

            if (dHead != 0) {
                return Integer.signum(dHead);
            }

            // Third : chord id
            return Integer.signum(c1.getId() - c2.getId());
        }
    };

    /**
     * Compare two notes of the same chord, ordered by increasing
     * distance from chord head ordinate.
     */
    public static Comparator<TreeNode> noteHeadComparator = new Comparator<TreeNode>()
    {
        @Override
        public int compare (TreeNode tn1,
                            TreeNode tn2)
        {
            if (tn1 == tn2) {
                return 0;
            }

            Note n1 = (Note) tn1;
            Note n2 = (Note) tn2;

            if (n1.getChord() != n2.getChord()) {
                logger.error("Ordering notes from different chords");
            }

            return n1.getChord().getStemDir() * (n2.getCenter().y - n1.
                    getCenter().y);
        }
    };
    //~ Instance fields --------------------------------------------------------

    /** Id for debug (unique within measure) */
    private final int id;

    /** A chord stem is virtual when there is no real stem (breve, rest...) */
    private Glyph stem;

    /** Containing slot, if any (no slot for whole/multi rests) */
    private Slot slot;

    /** Location for chord head (head farthest from chord tail) */
    private Point headLocation;

    /** Location for chord tail */
    private Point tailLocation;

    /** Collection of beams this chord is connected to */
    private List<Beam> beams = new ArrayList<>();

    /** Number of augmentation dots */
    private int dotsNumber;

    /** Flag glyphs */
    private Set<Glyph> flagGlyphs = new HashSet<>();

    /** Number of flags (a beam is not a flag) */
    private int flagsNumber;

    /** Ratio to get actual rawDuration wrt graphical notation */
    private DurationFactor tupletFactor;

    /** Start time since beginning of the containing measure */
    private Rational startTime;

    /** Voice this chord belongs to */
    private Voice voice;

    /** Collection of marks for user */
    private List<Mark> marks = new ArrayList<>();

    /** Notations related to this chord */
    private List<Notation> notations = new ArrayList<>();

    /** Directions (loosely) related to this chord */
    private List<Direction> directions = new ArrayList<>();

    /** Symbol (chord name), if any, related to this chord */
    private ChordSymbol chordSymbol;

    /** Compare two beams linked to this chord, ordered from tail. */
    private Comparator<Beam> byLevel = new Comparator<Beam>()
    {
        @Override
        public int compare (Beam b1,
                            Beam b2)
        {
            int x = stem.getLocation().x;
            int y1 = b1.getLine().yAtX(x);
            int y2 = b2.getLine().yAtX(x);
            int yHead = getHeadLocation().y;

            int result = Integer.signum(
                    Math.abs(yHead - y2) - Math.abs(yHead - y1));

            return result;
        }
    };

    //~ Constructors -----------------------------------------------------------
    //
    //-------//
    // Chord //
    //-------//
    /**
     * Creates a new instance of Chord
     *
     * @param measure the containing measure
     * @param slot    the containing slot (null for whole/multi rest chords)
     */
    public Chord (Measure measure,
                  Slot slot)
    {
        super(measure);
        reset();
        this.slot = slot;
        id = measure.getChords().size();
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // getFlagShape //
    //--------------//
    public static Shape getFlagShape (int fn,
                                      boolean up)
    {
        switch (fn) {
        case 1:
            return up ? FLAG_1_UP : FLAG_1;

        case 2:
            return up ? FLAG_2_UP : FLAG_2;

        case 3:
            return up ? FLAG_3_UP : FLAG_3;

        case 4:
            return up ? FLAG_4_UP : FLAG_4;

        case 5:
            return up ? FLAG_5_UP : FLAG_5;
        }

        logger.error("Illegal flag number: {}", fn);

        return null;
    }

    //---------------//
    // getStemChords //
    //---------------//
    /**
     * Find the chord(s) that are carried by a given stem
     *
     * @param measure the containing measure
     * @param stem    the given stem
     * @return the collection of related chords, which may be empty if no chord
     *         is yet attached to this stem
     */
    public static List<Chord> getStemChords (Measure measure,
                                             Glyph stem)
    {
        List<Chord> chords = new ArrayList<>();

        for (TreeNode node : measure.getChords()) {
            Chord chord = (Chord) node;

            if (chord.getStem() == stem) {
                chords.add(chord);
            }
        }

        return chords;
    }

    //-------------------------//
    // lookupInterleavedChords //
    //-------------------------//
    /**
     * Look up for all chords interleaved between the given stemed chords
     *
     * @param left  the chord on the left of the area
     * @param right the chord on the right of the area
     * @return the collection of interleaved chords, which may be empty
     */
    public static SortedSet<Chord> lookupInterleavedChords (Chord left,
                                                            Chord right)
    {
        SortedSet<Chord> found = new TreeSet<>(Chord.byAbscissa);

        if ((left == null) || (right == null)) {
            return found; // Safer
        }

        // Define the area limited by the left and right chords with their stems
        // and check for intersection with a rest note
        // More precisely, we use the area half on the tail side.
        // And we check that the interleaved chords have the same stem dir
        Polygon polygon = new Polygon();
        polygon.addPoint(left.getHeadLocation().x, left.getCenter().y);
        polygon.addPoint(left.getTailLocation().x, left.getTailLocation().y);
        polygon.addPoint(right.getTailLocation().x, right.getTailLocation().y);
        polygon.addPoint(right.getHeadLocation().x, right.getCenter().y);

        for (TreeNode node : left.getMeasure().getChords()) {
            Chord chord = (Chord) node;

            // Not interested in the bounding chords (TBC)
            if ((chord == left) || (chord == right)) {
                continue;
            }

            // Additional check on stem dir, if left & right agree
            if (left.getStemDir() == right.getStemDir()) {
                if (chord.getReferencePoint() == null
                    || (chord.getStemDir() != 0 && chord.getStemDir() != left.getStemDir())) {
                    continue;
                }
            }

            Rectangle box = chord.getBox();

            if (polygon.intersects(box.x, box.y, box.width, box.height)) {
                found.add(chord);
            }
        }

        return found;
    }

    //------------//
    // lookupRest //
    //------------//
    /**
     * Look up for a potential rest interleaved between the given stemed
     * chords
     *
     * @param left  the chord on the left of the area
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

        for (TreeNode node : left.getMeasure().getChords()) {
            Chord chord = (Chord) node;

            // Not interested in the bounding chords
            if ((chord == left) || (chord == right)) {
                continue;
            }

            for (TreeNode n : chord.getNotes()) {
                Note note = (Note) n;

                // Interested in rest notes only
                if (note.isRest()) {
                    Rectangle box = note.getBox();

                    if (polygon.intersects(box.x, box.y, box.width, box.height)) {
                        return note;
                    }
                }
            }
        }

        return null;
    }

    //--------------//
    // populateFlag //
    //--------------//
    /**
     * Try to assign a flag to a relevant chord.
     *
     * @param glyph   the underlying glyph of this flag
     * @param measure the containing measure
     */
    public static void populateFlag (Glyph glyph,
                                     Measure measure)
    {
        logger.debug("Chord Populating flag {}", glyph);

        // Retrieve the related chord
        Glyph stem = null;

        for (HorizontalSide side : HorizontalSide.values()) {
            stem = glyph.getStem(side);

            if (stem != null) {
                List<Chord> sideChords = Chord.getStemChords(measure, stem);

                if (!sideChords.isEmpty()) {
                    for (Chord chord : sideChords) {
                        chord.flagsNumber += getFlagValue(glyph.getShape());
                        chord.addFlagGlyph(glyph);
                        glyph.addTranslation(chord);
                    }
                } else {
                    measure.addError(stem, "No chord for stem " + stem.getId());
                }

                return;
            }
        }

        measure.addError(glyph, "Flag glyph with no stem");
    }

    //----------//
    // populate //
    //----------//
    /**
     * Create a new chord (or join an existing one) to host the note(s)
     * that correspond to the provided glyph.
     *
     * @param glyph   the provided underlying glyph (notehead, note or rest)
     * @param measure the containing measure
     */
    public static void populate (Glyph glyph,
                                 Measure measure)
    {
        if (glyph.isVip()) {
            logger.info("Chord. populate {}", glyph.idString());
        }

        glyph.clearTranslations();
        Set<Integer> assigned = new HashSet<>();

        // Allocate 1 chord per stem, per rest, per whole note
        if (glyph.getStemNumber() > 0) {
            // Beware of noteheads with 2 stems, we need to duplicate them
            // in order to actually have two logical chords.
            // Sides are browsed LEFT, then RIGHT
            for (HorizontalSide side : HorizontalSide.values()) {
                boolean completing = side == HorizontalSide.RIGHT;
                Glyph stem = glyph.getStem(side);

                if (stem != null) {
                    List<Chord> chords = Chord.getStemChords(measure, stem);
                    if (!chords.isEmpty()) {
                        // At this point, we can have at most one chord per stem
                        // Join the chord(s)
                        for (Chord chord : chords) {
                            Note.createPack(chord, glyph, assigned, completing);
                        }

                    } else {
                        // Create a new stem-based chord
                        Chord chord = new Chord(measure, null);
                        chord.setStem(stem);
                        stem.setTranslation(chord);
                        Note.createPack(chord, glyph, assigned, completing);
                    }
                }
            }
        } else {
            // Create a brand-new stem-less chord (rest or whole note)
            Chord chord = new Chord(measure, null);
            Note.createPack(chord, glyph, assigned, true);

            // Keep track of measure rests (linked to no time slot)
            if (glyph.getShape().isMeasureRest()) {
                measure.addWholeChord(chord);
            }
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

    //---------------------//
    // getTranslationLinks //
    //---------------------//
    @Override
    public List<Line2D> getTranslationLinks (Glyph glyph)
    {
        Point from = glyph.getLocation();
        Point to = stem != null
                ? stem.getAreaCenter()
                : getReferencePoint();

        Line2D line = new Line2D.Double(from, to);
        return Arrays.asList(line);
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
        if (!beams.contains(beam)) {
            beams.add(beam);

            // Keep the sequence sorted
            Collections.sort(beams, byLevel);
        }
    }

    //----------//
    // addChild //
    //----------//
    /**
     * Override normal behavior, so that adding a note resets chord
     * internal parameters
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

    //----------------//
    // setChordSymbol //
    //----------------//
    /**
     * Set the chord symbol for this chord
     *
     * @param chordSymbol the symbol to assign
     */
    public void setChordSymbol (ChordSymbol chordSymbol)
    {
        this.chordSymbol = chordSymbol;
    }

    //--------------//
    // addFlagGlyph //
    //--------------//
    /**
     * Remember a flag glyph for this chord
     *
     * @param flag the glyph (which may represent several flags)
     */
    public void addFlagGlyph (Glyph flag)
    {
        flagGlyphs.add(flag);
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
    // checkTies //
    //-----------//
    /**
     * Check that all incoming ties come from the same chord,
     * and similarly that all outgoing ties go to the same chord.
     */
    public void checkTies ()
    {
        // Incoming ties
        SplitOrder order = checkTies(
                new TieRelation()
        {
            @Override
            public Note getDistantNote (Slur slur)
            {
                return slur.getLeftNote();
            }

            @Override
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
                new TieRelation()
        {
            @Override
            public Note getDistantNote (Slur slur)
            {
                return slur.getRightNote();
            }

            @Override
            public Note getLocalNote (Slur slur)
            {
                return slur.getLeftNote();
            }
        });

        if (order != null) {
            split(order);
        }
    }

    //-----------//
    // duplicate //
    //-----------//
    /**
     * Make a clone of a chord (except for its beams).
     * This duplication is needed in cases such as: a note head with stems on
     * both sides, or a chord shared by two BeamGroups.
     *
     * @return a clone of this chord (including notes, but beams are not copied)
     */
    public Chord duplicate ()
    {
        // Beams are not copied
        Chord clone = new Chord(getMeasure(), slot);
        clone.stem = stem;
        stem.addTranslation(clone);

        // Notes (we make a deep copy of each note)
        List<TreeNode> notesCopy = new ArrayList<>();
        notesCopy.addAll(getNotes());

        for (TreeNode node : notesCopy) {
            Note note = (Note) node;
            new Note(clone, note);
        }

        clone.tupletFactor = tupletFactor;

        clone.dotsNumber = dotsNumber;
        clone.flagsNumber = flagsNumber; // Not sure TODO

        // Insure correct ordering of chords within their container
        ///Collections.sort(getParent().getChildren(), chordComparator);

        return clone;
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
        if (!getBeams().isEmpty()) {
            return getBeams().get(0).getGroup();
        } else {
            return null;
        }
    }

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the sequence of beams that are attached to this chord,
     * ordered from the tail of the chord.
     *
     * @return the attached beams
     */
    public List<Beam> getBeams ()
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
    public Collection<? extends Direction> getDirections ()
    {
        return directions;
    }

    //----------------//
    // getChordSymbol //
    //----------------//
    /**
     * Report the symbol (chord name) related to this chord
     *
     * @return the chord symbol, perhaps null
     */
    public ChordSymbol getChordSymbol ()
    {
        return chordSymbol;
    }

    //---------------//
    // getDotsNumber //
    //---------------//
    /**
     * Report the number of augmentation dots that impact this chord
     *
     * @return the number of dots (should be the same for all notes within this
     *         chord)
     */
    public int getDotsNumber ()
    {
        return dotsNumber;
    }

    //-------------//
    // getDuration //
    //-------------//
    /**
     * Report the real duration computed for this chord, including the
     * tuplet impact if any, with null value for whole/multi rest.
     *
     * @return The real chord/note duration, or null for a whole rest chord
     * @see #getRawDuration
     */
    public Rational getDuration ()
    {
        if (this.isWholeDuration()) {
            return null;
        } else {
            Rational raw = getRawDuration();

            if (tupletFactor == null) {
                return raw;
            } else {
                return raw.times(tupletFactor);
            }
        }
    }

    //------------//
    // getEndTime //
    //------------//
    /**
     * Report the time when this chord ends
     *
     * @return chord ending time, since beginning of the measure
     */
    public Rational getEndTime ()
    {
        if (isWholeDuration()) {
            return null;
        }

        Rational chordDur = getDuration();

        if (chordDur == null) {
            return null;
        } else {
            return startTime.plus(chordDur);
        }
    }

    //---------------//
    // getFlagGlyphs //
    //---------------//
    /**
     * @return the flagGlyphs
     */
    public Set<Glyph> getFlagGlyphs ()
    {
        return flagGlyphs;
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
     * Report the x-ordered collection of chords which are directly
     * tied to the right of this chord.
     *
     * @return the (perhaps empty) collection of tied chords
     */
    public List<Chord> getFollowingTiedChords ()
    {
        List<Chord> tied = new ArrayList<>();

        for (TreeNode node : children) {
            Note note = (Note) node;

            for (Slur slur : note.getSlurs()) {
                if (slur.isTie()
                    && (slur.getLeftNote() == note)
                    && (slur.getRightNote() != null)) {
                    tied.add(slur.getRightNote().getChord());
                }
            }
        }

        Collections.sort(tied, Chord.byAbscissa);
        return tied;
    }

    //-----------------//
    // getHeadLocation //
    //-----------------//
    /**
     * Report the system-based location of the chord head (the head
     * which is farthest from the tail).
     *
     * @return the head location
     */
    public Point getHeadLocation ()
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
     *
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
    public Collection<? extends Notation> getNotations ()
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
     * Report the chord that occurs right before this one, within the
     * same voice.
     *
     * @return the previous chord within the same voice
     */
    public Chord getPreviousChordInVoice ()
    {
        return voice.getPreviousChord(this);
    }

    //----------------//
    // getRawDuration //
    //---------------//
    /**
     * Report the intrinsic duration of this chord, taking flag/beams
     * and dots into account, but not the tuplet impact if any.
     * The duration is assumed to be the same for all notes of this chord,
     * otherwise the chord must be split.
     * This includes the local information (flags, dots) but not the tuplet
     * impact if any.
     * A specific value (WHOLE_DURATION) indicates the whole/multi rest chord.
     *
     * Nota: this value is not cached, but computed at every call
     *
     * @return the intrinsic chord duration
     * @see #getDuration
     */
    public Rational getRawDuration ()
    {
        Rational rawDuration = null;

        if (!getNotes().isEmpty()) {
            // All note heads are assumed to be the same within one chord
            Note note = (Note) getNotes().get(0);

            if (!note.getShape().isMeasureRest()) {
                // Duration (with flags/beams applied for non-rests)
                rawDuration = note.getNoteDuration();

                // Apply augmentation (applies to rests as well)
                if (dotsNumber == 1) {
                    rawDuration = rawDuration.times(new Rational(3, 2));
                } else if (dotsNumber == 2) {
                    rawDuration = rawDuration.times(new Rational(7, 4));
                }
            }
        }

        return rawDuration;
    }

    //---------//
    // setSlot //
    //---------//
    public void setSlot (Slot slot)
    {
        this.slot = slot;
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
            if (!getNotes().isEmpty()) {
                Note note = (Note) getNotes().get(0);
                setStaff(note.getStaff());
            }
        }

        return super.getStaff();
    }

    //--------------//
    // getStartTime //
    //--------------//
    /**
     * Report the starting time for this chord
     *
     * @return startTime chord starting time (counted within the measure)
     */
    public Rational getStartTime ()
    {
        return startTime;
    }

    //---------//
    // getStem //
    //---------//
    /**
     * Report the stem of this chord (or null in the case of stem-less
     * chord).
     *
     * @return the chord stem, or null
     */
    public Glyph getStem ()
    {
        return stem;
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

    //-----------------//
    // getTailLocation //
    //-----------------//
    /**
     * Report the system-based location of the tail of the chord
     *
     * @return the tail location
     */
    public Point getTailLocation ()
    {
        if (tailLocation == null) {
            computeLocations();
        }

        return tailLocation;
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
    // isEmbracedBy //
    //--------------//
    /**
     * Check whether the notes of this chord stand within the given
     * vertical range
     *
     * @param top    top of vertical range
     * @param bottom bottom of vertical range
     * @return true if all notes are within the given range
     */
    public boolean isEmbracedBy (Point top,
                                 Point bottom)
    {
        for (TreeNode node : getNotes()) {
            Note note = (Note) node;
            Point center = note.getCenter();

            if ((center.y >= top.y) && (center.y <= bottom.y)) {
                return true;
            }
        }

        return false;
    }

    //-----------------//
    // isWholeDuration //
    //-----------------//
    /**
     * Check whether the chord/note is a whole rest
     *
     * @return true if whole
     */
    public boolean isWholeDuration ()
    {
        if (!getNotes().isEmpty()) {
            Note note = (Note) getNotes().get(0);

            return note.getShape().isMeasureRest();
        }

        return false;
    }

    //---------------//
    // setDotsNumber //
    //---------------//
    /**
     * Define the number of augmentation dots that impact this chord
     *
     * @param dotsNumber the number of dots (should be the same for all notes
     *                   within this chord)
     */
    public void setDotsNumber (int dotsNumber)
    {
        this.dotsNumber = dotsNumber;
    }

    //--------------//
    // setStartTime //
    //--------------//
    /**
     * Remember the starting time for this chord
     *
     * @param startTime chord starting time (counted within the measure)
     */
    public void setStartTime (Rational startTime)
    {
        // Already done?
        if (this.startTime == null) {
            logger.debug("setStartTime {} for chord #{}", startTime, getId());

            this.startTime = startTime;
//
//            // Set the same info in containing slot if any
//            if (slot != null) {
//                slot.setStartTime(startTime);
//            }
        } else {
            if (!this.startTime.equals(startTime)) {
                addError(
                        "Reassigning startTime from " + this.startTime + " to "
                        + startTime + " in " + this);
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
            final String contextString = getContextString();
            logger.debug("{} Ch#{} setVoice {}",
                    contextString, id, voice.getId());

            this.voice = voice;

            // Update the voice entity
            if (!isWholeDuration()) {
                if (slot != null) {
                    voice.setSlotInfo(
                            slot,
                            new VoiceChord(this, Voice.Status.BEGIN));
                }

                // Extend this info to other grouped chords if any
                BeamGroup group = getBeamGroup();

                if (group != null) {
                    logger.debug(
                            "{} Ch#{} extending voice#{} to group#{}",
                            contextString, id, voice.getId(), group.getId());

                    group.setVoice(voice);
                }

                // Extend to the following tied chords as well
                List<Chord> tied = getFollowingTiedChords();

                for (Chord chord : tied) {
                    logger.debug("{} tied to {}", this, chord);

                    // Check the tied chords belong to the same measure
                    if (this.getMeasure() == chord.getMeasure()) {
                        logger.debug(
                                "{} Ch#{} extending voice#{} to tied chord#{}",
                                contextString, id, voice.getId(), chord.getId());

                        chord.setVoice(voice);
                    } else {
                        // Chords tied across measure boundary
                        logger.debug("{} Cross tie {} -> {}",
                                contextString,
                                toShortString(), chord.toShortString());
                    }
                }
            }
        } else if (this.voice != voice) {
            addError(
                    "Chord. Attempt to reassign voice from " + this.voice.
                    getId()
                    + " to " + voice.getId() + " in " + this);
        } else {
            if (!isWholeDuration()) {
                if (slot != null) {
                    voice.setSlotInfo(
                            slot,
                            new VoiceChord(this, Voice.Status.BEGIN));
                }
            }
        }
    }

    //--------------//
    // toLongString //
    //--------------//
    /**
     * Report a more detailed description than plain toStrïng
     *
     * @return a detailed description
     */
    public String toLongString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this);
        sb.deleteCharAt(sb.length() - 1); // Remove trailing "}"

        try {
            if (headLocation != null) {
                sb.append(" head[x=").append(headLocation.x).append(",y=").
                        append(headLocation.y).append("]");
            }

            if (tailLocation != null) {
                sb.append(" tail[x=").append(tailLocation.x).append(",y=").
                        append(tailLocation.y).append("]");
            }

            if (!beams.isEmpty()) {
                try {
                    sb.append(" beams G#").
                            append(beams.get(0).getGroup().getId()).append(
                            "[");

                    for (Beam beam : beams) {
                        sb.append(beam).append(" ");
                    }

                    sb.append("]");
                } catch (Exception ex) {
                    logger.warn("Exception in chord toLongString()");
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
     *
     * @return a short chord description
     */
    public String toShortString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        if (getVoice() != null) {
            sb.append("Voice#").append(getVoice().getId());
        }

        sb.append(" Chord#").append(getId());

        sb.append(" dur:");

        if (isWholeDuration()) {
            sb.append("W");
        } else {
            Rational chordDur = getDuration();

            if (chordDur != null) {
                sb.append(chordDur);
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
            sb.append("#").append(id);

            if (voice != null) {
                sb.append(" voice#").append(voice.getId());
            }

            // Staff ?
            if (!getNotes().isEmpty()) {
                Note note = (Note) getNotes().get(0);

                if (note != null) {
                    sb.append(" staff#").append(note.getStaff().getId());
                }
            }

            if (startTime != null) {
                sb.append(" start=").append(startTime);
            }

            sb.append(" dur=");

            if (isWholeDuration()) {
                sb.append("W");
            } else {
                Rational chordDur = getDuration();

                if (chordDur != null) {
                    sb.append(chordDur);
                } else {
                    sb.append("none");
                }
            }

            if (isAllRests()) {
                sb.append(" rest");
            }

            if (stem != null) {
                sb.append(" stem#").append(stem.getId());
            }

            if (tupletFactor != null) {
                sb.append(" tupletFactor=").append(tupletFactor);
            }

            if (dotsNumber != 0) {
                sb.append(" dots=").append(dotsNumber);
            }

            if (flagsNumber != 0) {
                sb.append(" flags=").append(flagsNumber);
            }
        } catch (NullPointerException e) {
            sb.append(" INVALID");
        }

        sb.append("}");

        return sb.toString();
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reset all internal data that depends on the chord composition in
     * terms of notes.
     */
    @Override
    protected final void reset ()
    {
        super.reset();

        headLocation = null;
        tailLocation = null;
        startTime = null;
    }

    //------------//
    // computeBox //
    //------------//
    /**
     * Compute the chord bounding box, including its stem (if any) as
     * well as all the notes of the chord.
     */
    @Override
    protected void computeBox ()
    {
        // Stem or similar info
        Rectangle newBox = new Rectangle(getTailLocation());
        newBox.add(getHeadLocation());

        // Each and every note
        for (TreeNode n : getNotes()) {
            Note note = (Note) n;

            newBox.add(note.getBox());
        }

        setBox(newBox);
    }

    //-----------------------//
    // computeReferencePoint //
    //-----------------------//
    /**
     * Define the reference point as the head location.
     */
    @Override
    protected void computeReferencePoint ()
    {
        setReferencePoint(getHeadLocation());
    }

    //-----------//
    // checkTies //
    //-----------//
    /**
     * For this chord, check either the incoming or the outgoing ties
     * according to the TieRelation information.
     * For true ties (slurs linking notes with same pitch) we make sure there
     * is no more than one distant chord. If not, we split the chord in two,
     * so that each (sub)chord has only consistent ties.
     *
     * @param tie info about the relation between the slur and this chord
     * @return how to split the chord, or null if no split is needed
     */
    private SplitOrder checkTies (TieRelation tie)
    {
        List<Note> distantNotes = new ArrayList<>();
        List<Chord> distantChords = new ArrayList<>();

        for (TreeNode nn : getNotes()) {
            Note note = (Note) nn;

            // Use a COPY of slurs to allow concurrent deletion
            for (Slur slur : new ArrayList<>(note.getSlurs())) {
                if (tie.isRelevant(slur, note)) {
                    Note distantNote = tie.getDistantNote(slur);

                    if (distantNote != null) {
                        // Safety check
                        if (distantNote == note
                            || distantNote.getChord() == this) {
                            // This slur is a loop on the same note or chord!
                            logger.info("Looping slur detected {}", slur);
                            slur.destroy();
                            continue;
                        }

                        if (distantNote.getMeasure() == getMeasure()) {
                            distantNotes.add(distantNote);
                            distantChords.add(distantNote.getChord());
                        }
                    }
                }
            }
        }

        if (distantChords.size() > 1) {
            logger.debug("{} Ch#{} with multiple tied chords: {}",
                    getContextString(), getId(), distantChords);

            // Prepare the split of this chord, using the most distant note 
            // from chord head
            SortedSet<Note> tiedNotes = new TreeSet<>(noteHeadComparator);

            for (Note distantNote : distantNotes) {
                for (Slur slur : distantNote.getSlurs()) {
                    Note note = tie.getLocalNote(slur);

                    if ((note != null) && (note.getChord() == this)) {
                        tiedNotes.add(note);
                    }
                }
            }

            logger.debug("Splitting from {}", tiedNotes.last());

            return new SplitOrder(this, tiedNotes.last());
        } else {
            return null;
        }
    }

    //------------------//
    // computeLocations //
    //------------------//
    /**
     * Compute the head and tail locations for this chord.
     */
    private void computeLocations ()
    {
        // Find the note farthest from stem middle point
        if (!getNotes().isEmpty()) {
            if (stem != null) {
                Point middle = stem.getLocation();
                Note bestNote = null;
                int bestDy = Integer.MIN_VALUE;

                for (TreeNode node : getNotes()) {
                    Note note = (Note) node;
                    int noteY = note.getCenter().y;
                    int dy = Math.abs(noteY - middle.y);

                    if (dy > bestDy) {
                        bestNote = note;
                        bestDy = dy;
                    }
                }

                Rectangle stemBox = stem.getBounds();

                if (middle.y < bestNote.getCenter().y) {
                    // Stem is up
                    tailLocation = new Point(
                            stemBox.x + (stemBox.width / 2),
                            stemBox.y);
                } else {
                    // Stem is down
                    tailLocation = new Point(
                            stemBox.x + (stemBox.width / 2),
                            (stemBox.y + stemBox.height));
                }

                headLocation = getHeadLocation(bestNote);
            } else {
                Note note = (Note) getNotes().get(0);
                headLocation = note.getCenter();
                tailLocation = headLocation;
            }
        } else {
            addError("No notes in chord " + this);
        }
    }

    //--------------//
    // getFlagValue //
    //--------------//
    /**
     * Report the number of flags that corresponds to the flag glyph
     *
     * @param shape the given flag glyph shape
     * @return the number of flags
     */
    private static int getFlagValue (Shape shape)
    {
        switch (shape) {
        case FLAG_1:
        case FLAG_1_UP:
            return 1;

        case FLAG_2:
        case FLAG_2_UP:
            return 2;

        case FLAG_3:
        case FLAG_3_UP:
            return 3;

        case FLAG_4:
        case FLAG_4_UP:
            return 4;

        case FLAG_5:
        case FLAG_5_UP:
            return 5;
        }

        logger.error("Illegal flag shape: {}", shape);

        return 0;
    }

    //-----------------//
    // getHeadLocation //
    //-----------------//
    /**
     * Compute the head location of a chord, given the chord head note.
     * (TODO: Perhaps we could adjust note ordinate, to align it to pitch while
     * using the real staff lines ordinates)
     *
     * @param note the head note
     * @return the head location
     */
    private Point getHeadLocation (Note note)
    {
        return new Point(tailLocation.x, note.getReferencePoint().y);
    }

    //---------------//
    // retrieveFlags //
    //---------------//
    /**
     * Report the set of flags for this chord.
     * Nota: This is not an efficient implementation. A better one would require
     * to record a link from chord to flag glyphs.
     *
     * @return the set of flags, perhaps empty but not null
     */
    private Set<Glyph> retrieveFlags ()
    {
        Set<Glyph> foundFlags = new HashSet<>();

        for (Glyph glyph : getSystem().getInfo().getGlyphs()) {
            Shape shape = glyph.getShape();

            if (ShapeSet.Flags.contains(shape)) {
                if (glyph.getTranslations().contains(this)) {
                    foundFlags.add(glyph);
                }
            }
        }

        return foundFlags;
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
        logger.debug("{}", order);
        logger.debug("Initial notes={}", getNotes());

        // Same measure & slot
        Chord alien = new Chord(getMeasure(), slot);

        // Same stem
        if (stem != null) {
            alien.stem = stem;
            stem.addTranslation(alien);
        }

        // Tuplet factor, if any, is copied
        alien.tupletFactor = tupletFactor;

        // Augmentation dots as well
        alien.dotsNumber = dotsNumber;

        // Beams are not copied, but flags are
        if (flagsNumber > 0) {
            alien.flagsNumber = flagsNumber;

            for (Glyph flag : retrieveFlags()) {
                flag.addTranslation(alien);
            }
        }

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

        // Should we split the stem as well?
        // Use a test on length of resulting stem fragment
        int stemFragmentLength = Math.abs(
                alien.headLocation.y - this.headLocation.y);

        if (stemFragmentLength >= getScale().toPixels(
                constants.minStemFragmentLength)) {
            this.tailLocation = alien.headLocation;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Remaining notes={}", getNotes());
            logger.debug("Remaining {}", this.toLongString());
            logger.debug("Alien notes={}", alien.getNotes());
            logger.debug("Alien {}", alien.toLongString());
        }

        return alien;
    }

    //~ Inner Classes ----------------------------------------------------------
    //------------//
    // SplitOrder //
    //------------//
    /**
     * Class {@code SplitOrder} records a chord split order.
     * Splitting must be separate from browsing to avoid concurrent modification
     * of collections
     */
    public static class SplitOrder
    {
        //~ Instance fields ----------------------------------------------------

        /** The chord to be split */
        final Chord chord;

        /** The first note of this chord to feed an alien chord */
        final Note alienNote;

        //~ Constructors -------------------------------------------------------
        /**
         * Create a chord SplitOrder
         *
         * @param chord     the chord to be split
         * @param alienNote the first chord note to feed an alien chord
         */
        public SplitOrder (Chord chord,
                           Note alienNote)
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
            sb.append(" chord=").append(chord);
            sb.append(" alienNote=").append(alienNote);
            sb.append("}");

            return sb.toString();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction minStemFragmentLength = new Scale.Fraction(
                2d,
                "Minimum length for stem fragment in split");

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
}
