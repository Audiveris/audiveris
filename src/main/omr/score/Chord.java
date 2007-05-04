//----------------------------------------------------------------------------//
//                                                                            //
//                                 C h o r d                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.PixelPoint;
import omr.sheet.PixelRectangle;
import omr.sheet.Scale;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>Chord</code> represents an ensemble of entities (rests, notes)
 * that occur on the same time in a staff.
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

    /** Compare two chords within a measure */
    private static final Comparator<TreeNode> chordComparator = new Comparator<TreeNode>() {
        public int compare (TreeNode tn1,
                            TreeNode tn2)
        {
            Chord c1 = (Chord) tn1;
            Chord c2 = (Chord) tn2;

            return c1.compareTo(c2);
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Id for debug (unique within measure) */
    private final int id;

    /** A chord stem is virtual when there is no real stem (breve, rest...) */
    private Glyph stem;

    /** Ratio to get real duration wrt notation */
    private Double tupletRatio;

    /** Index of this chord in tuplet */
    private Integer tupletIndex;

    /** Number of augmentation dots */
    private int dotsNumber;

    /** Number of flags (a beam is not a flag) */
    private int flagsNumber;

    /** Location for chord head (head farthest from chord tail) */
    private SystemPoint headLocation;

    /** Location for chord tail */
    private SystemPoint tailLocation;

    /** Ordered collection of beams this chord is connected to */
    private SortedSet<Beam> beams = new TreeSet<Beam>();

    /** Voice this chord belongs to */
    private Integer voice;

    /**
     * Duration (must the same for all notes of this chord, otherwise the chord
     * must be split. This may be too restrictive, TBD)
     */
    private Integer duration;

    /** Computed ending time of this chord */
    private Integer endTime;

    /** Collection of marks for user */
    private List<Mark> marks = new ArrayList<Mark>();

    /** Notations related  to this chord */
    private List<Notation> notations = new ArrayList<Notation>();

    /** Direction (loosely) related to this chord */
    private List<Direction> directions = new ArrayList<Direction>();

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Chord //
    //-------//
    /**
     * Creates a new instance of Chord
     * @param measure the containing measure
     */
    public Chord (Measure measure)
    {
        super(measure);
        reset();
        id = measure.getChords()
                    .indexOf(this) + 1;
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
        // are changed
        if (node instanceof Note) {
            reset();
        }
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Compare this chord with another chord, to implement order based first on
     * head abscissa, then on head ordinate
     *
     * @param other the other chord
     * @return -1, 0, +1 according to the comparison result
     */
    public int compareTo (Chord other)
    {
        int dx = getHeadLocation().x - other.getHeadLocation().x;

        if (dx != 0) {
            return Integer.signum(dx);
        } else {
            return Integer.signum(
                getHeadLocation().y - other.getHeadLocation().y);
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
        Chord clone = new Chord(getMeasure());

        // Insert clone in proper slot
        for (Slot slot : getMeasure()
                             .getSlots()) {
            if (slot.getChords()
                    .contains(this)) {
                slot.getChords()
                    .add(clone);

                break;
            }
        }

        clone.stem = stem;

        // Notes (we make a deep copy of each note)
        List<TreeNode> notesCopy = new ArrayList<TreeNode>();
        notesCopy.addAll(getNotes());

        for (TreeNode node : notesCopy) {
            Note note = (Note) node;
            clone.addChild(new Note(note));
        }

        if (tupletRatio != null) {
            clone.tupletRatio = new Double(tupletRatio);
        }

        if (tupletIndex != null) {
            clone.tupletIndex = new Integer(tupletIndex);
        }

        clone.dotsNumber = dotsNumber;
        clone.flagsNumber = flagsNumber; // Not sure TBD

        // Insure correct ordering of chords within their container
        Collections.sort(getParent().getChildren(), chordComparator);

        return clone;
    }

    //--------------//
    // getBeamGroup //
    //--------------//
    public BeamGroup getBeamGroup ()
    {
        if (getBeams()
                .size() > 0) {
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
    public Integer getDuration ()
    {
        if (duration == null) {
            if (getNotes()
                    .size() > 0) {
                // Note heads are assumed to be the same ...
                Note note = (Note) getNotes()
                                       .get(0);

                if (note.getShape() == Shape.WHOLE_REST) {
                    duration = getMeasure()
                                   .getExpectedDuration();
                } else {
                    duration = note.getTypeDuration(note.getShape());

                    // Apply fraction
                    int fbn = getFlagsNumber() + getBeams()
                                                     .size();

                    for (int i = 0; i < fbn; i++) {
                        duration /= 2;
                    }

                    // Apply augmentation
                    if (dotsNumber == 1) {
                        duration += (duration / 2);
                    } else if (dotsNumber == 2) {
                        duration += ((duration * 3) / 4);
                    }
                }
            }
        }

        return duration;
    }

    //------------//
    // getEndTime //
    //------------//
    public Integer getEndTime ()
    {
        return endTime;
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
    public int getId ()
    {
        return id;
    }

    public List<Mark> getMarks ()
    {
        return marks;
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
    public Chord getPreviousChordInVoice ()
    {
        int   voice = getVoice();
        Chord prev = (Chord) getPreviousSibling();

        for (; prev != null; prev = (Chord) prev.getPreviousSibling()) {
            if (prev.getVoice() == voice) {
                return prev;
            }
        }

        return null;
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * Report the staff that contains this chord (we use the staff of this note)
     *
     * @return the chord staff
     */
    @Override
    public Staff getStaff ()
    {
        if (super.getStaff() == null) {
            if (getNotes()
                    .size() > 0) {
                Note note = (Note) getNotes()
                                       .get(0);
                setStaff(note.getStaff());
            }
        }

        return super.getStaff();
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

        for (TreeNode node : measure.getChords()) {
            Chord chord = (Chord) node;

            if (chord.getStem() == stem) {
                chords.add(chord);
            }
        }

        return chords;
    }

    //---------------//
    // getDirections //
    //---------------//
    public Collection<?extends Direction> getDirections ()
    {
        return directions;
    }

    //--------------//
    // getNotations //
    //--------------//
    public Collection<?extends Notation> getNotations ()
    {
        return notations;
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

    //----------//
    // getVoice //
    //----------//
    public Integer getVoice ()
    {
        return voice;
    }

    //-----------------//
    // isWholeDuration //
    //-----------------//
    public boolean isWholeDuration ()
    {
        if (getNotes()
                .size() > 0) {
            Note note = (Note) getNotes()
                                   .get(0);

            return note.getShape() == Shape.WHOLE_REST;
        }

        return false;
    }

    //------------//
    // setEndTime //
    //------------//
    public void setEndTime (Integer endTime)
    {
        this.endTime = endTime;
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

    //----------//
    // setVoice //
    //----------//
    public void setVoice (Integer voice)
    {
        this.voice = voice;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Chord");

        sb.append(" #")
          .append(id);

        if (voice != null) {
            sb.append(" voice#")
              .append(voice);
        }

        if (duration != null) {
            sb.append(" dur=")
              .append(duration);
        }

        if (stem != null) {
            sb.append(" stem=")
              .append(stem.getId());
        }

        if (tupletRatio != null) {
            sb.append(" tupletRatio=")
              .append(tupletRatio);
        }

        if (tupletIndex != null) {
            sb.append(" tupletIndex=")
              .append(tupletIndex);
        }

        if (dotsNumber != 0) {
            sb.append(" dots=")
              .append(dotsNumber);
        }

        if (flagsNumber != 0) {
            sb.append(" flags=")
              .append(flagsNumber);
        }

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

        if (beams.size() > 0) {
            sb.append(" beams[");

            for (Beam beam : beams) {
                sb.append(beam + " ");
            }

            sb.append("]");
        }

        sb.append("}");

        return sb.toString();
    }

    //---------//
    // addMark //
    //---------//
    void addMark (Mark mark)
    {
        marks.add(mark);
    }

    //-------------//
    // populateDot //
    //-------------//
    static void populateDot (Glyph       glyph,
                             Measure     measure,
                             SystemPoint dotCenter)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Chord Populating dot " + glyph);
        }

        Scale      scale = measure.getScale();
        final int  maxDx = scale.toUnits(constants.maxDotDx);
        final int  maxDy = scale.toUnits(constants.maxDotDy);
        Set<Chord> candidates = new HashSet<Chord>();

        // A dot can be: an augmentation dot; part of repeat dots; staccato
        // Check for a note/rest nearby:
        // - on the left w/ same even pitch (note w/ even pitch)
        // - slighly above or below (note with odd pitch = on a staff line)
        ChordLoop: 
        for (TreeNode node : measure.getChords()) {
            Chord chord = (Chord) node;

            for (TreeNode n : chord.getNotes()) {
                Note        note = (Note) n;

                SystemPoint noteRef = note.getCenterRight();
                SystemPoint toDot = new SystemPoint(
                    dotCenter.x - noteRef.x,
                    dotCenter.y - noteRef.y);

                if (logger.isFineEnabled()) {
                    logger.info(measure.getContextString() + " " + toDot);
                }

                if (toDot.x <= 0) {
                    break ChordLoop; // All other notes will be on right, so...
                }

                if ((toDot.x <= maxDx) && (Math.abs(toDot.y) <= maxDy)) {
                    candidates.add(chord);
                }
            }
        }

        // Assign the dot to the candidate with longest duration, which boils
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

            if (logger.isFineEnabled()) {
                logger.fine(
                    bestChord.getContextString() + " Augmented " + bestChord);
            }
        }
    }

    //--------------//
    // populateFlag //
    //--------------//
    static void populateFlag (Glyph   glyph,
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
            logger.warning(
                measure.getContextString() + " Flag glyph with no stem");
        } else {
            List<Chord> sideChords = Chord.getStemChords(measure, stem);

            if (sideChords.size() > 0) {
                for (Chord chord : sideChords) {
                    chord.flagsNumber += getFlagValue(glyph);
                }
            } else {
                logger.warning(
                    measure.getContextString() + " No chord for stem " + stem);
            }
        }
    }

    //--------------//
    // addDirection //
    //--------------//
    void addDirection (Direction direction)
    {
        directions.add(direction);
    }

    //-------------//
    // addNotation //
    //-------------//
    void addNotation (Notation notation)
    {
        notations.add(notation);
    }

    //--------------//
    // isEmbracedBy //
    //--------------//
    boolean isEmbracedBy (SystemPoint top,
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

    //--------------//
    // getFlagValue //
    //--------------//
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

    //------------------//
    // computeLocations //
    //------------------//
    /**
     * Compute the head and tail locations for this chord.
     */
    private void computeLocations ()
    {
        System system = getSystem();

        // Find the note farthest from stem middle point
        if (getNotes()
                .size() > 0) {
            if (stem != null) {
                SystemPoint middle = system.toSystemPoint(stem.getCenter());
                Note        bestNote = null;
                int         bestDy = 0;

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

                // Use note pitch position for more precise location
                Staff staff = bestNote.getStaff();
                headLocation = new SystemPoint(
                    tailLocation.x,
                    staff.getTopLeft().y - bestNote.getSystem().getTopLeft().y +
                    staff.pitchToUnit(Math.rint(bestNote.getPitchPosition())));
            } else {
                Note note = (Note) getNotes()
                                       .get(0);
                headLocation = note.getCenter();
                tailLocation = headLocation;
            }
        }
    }

    //-------//
    // reset //
    //-------//
    private void reset ()
    {
        headLocation = null;
        tailLocation = null;
        duration = null;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /**
         * Maximum dx between note and augmentation dot
         */
        Scale.Fraction maxDotDx = new Scale.Fraction(
            1d,
            "Maximum dx between note and augmentation dot");

        /**
         * Maximum absolute dy between note and augmentation dot
         */
        Scale.Fraction maxDotDy = new Scale.Fraction(
            1d,
            "Maximum absolute dy between note and augmentation dot");
    }
}
