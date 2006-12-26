//----------------------------------------------------------------------------//
//                                                                            //
//                                  N o t e                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
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
 * Class <code>Note</code> represents the characteristics of a note. Besides a
 * regular note (standard note, or rest), it can also be a cue note or a grace
 * note.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Note
    extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Note.class);

    /**
     * A quarter duration value that should fit all cases for internal
     * computations. This will be simplified when the score is exported to XML,
     * using the greatest common divisor found in the score.
     */
    public static final int QUARTER_DURATION = 96;

    /** Names of the various note types (used in MusicXML) */
    public static final String[] typeNames = new String[] {
                                                 "256th", "128th", "64th",
                                                 "32nd", "16th", "eighth",
                                                 "quarter", "half", "whole",
                                                 "breve", "long"
                                             };

    //~ Enumerations -----------------------------------------------------------

    /** Names of the various note steps (used in MusicXML) */
    public static enum Step {
        /** La */ A,

        /** Si */ B,
        /** Do */ C, 
        /** Ré */ D, 
        /** Mi */ E, 
        /** Fa */ F, 
        /** Sol */ G;
    }

    //~ Instance fields --------------------------------------------------------

    /** The underlying glyph */
    private final Glyph glyph;

    /** The note shape */
    private final Shape shape;

    /** Accidental is any */
    private Shape accidental;

    /** Indicate a rest */
    private final boolean isRest;

    /** Pitch alteration (not for rests) */
    private Integer alter;

    /** Note step */
    private Step step;

    /** Octave */
    private Integer octave;

    /** Tie ??? */

    /** Pitch position */
    private final double pitchPosition;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Note //
    //------//
    /** Creates a new instance of Note */
    public Note (Chord chord,
                 Glyph glyph)
    {
        super(chord);
        this.glyph = glyph;

        // Shape
        shape = glyph.getShape();

        // Rest?
        isRest = Shape.Rests.contains(glyph.getShape());

        // Location center
        System system = getSystem();
        setCenter(system.toSystemPoint(glyph.getCenter()));

        // Staff
        setStaff(system.getStaffAt(getCenter()));

        // Pitch Position
        pitchPosition = getStaff()
                            .pitchPositionOf(getCenter());
    }

    //------//
    // Note //
    //------//
    /** Clone a Note */
    public Note (Note other)
    {
        super(other.getChord());
        glyph = other.glyph;
        isRest = other.isRest;
        setCenter(other.getCenter());
        setStaff(other.getStaff());
        pitchPosition = other.pitchPosition;
        shape = other.getShape();
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getChord //
    //----------//
    public Chord getChord ()
    {
        return (Chord) getParent();
    }

    //----------//
    // getGlyph //
    //----------//
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //------------------//
    // getPitchPosition //
    //------------------//
    public double getPitchPosition ()
    {
        return pitchPosition;
    }

    //--------//
    // isRest //
    //--------//
    public boolean isRest ()
    {
        return isRest;
    }

    //----------//
    // getShape //
    //----------//
    public Shape getShape ()
    {
        return shape;
    }

    //-----------------//
    // getTypeDuration //
    //-----------------//
    public int getTypeDuration (Shape shape)
    {
        switch (shape) {
        // Rests
        //
        case WHOLE_REST :
            return 4 * QUARTER_DURATION;

        case HALF_REST :
            return 2 * QUARTER_DURATION;

        case QUARTER_REST :
            return QUARTER_DURATION;

        case EIGHTH_REST :
            return QUARTER_DURATION / 2;

        case SIXTEENTH_REST :
            return QUARTER_DURATION / 4;

        case THIRTY_SECOND_REST :
            return QUARTER_DURATION / 8;

        case SIXTY_FOURTH_REST :
            return QUARTER_DURATION / 16;

        case ONE_HUNDRED_TWENTY_EIGHTH_REST :
            return QUARTER_DURATION / 32;

        // Notehead
        //
        case VOID_NOTEHEAD :
        case VOID_NOTEHEAD_2 :
        case VOID_NOTEHEAD_3 :
            return 2 * QUARTER_DURATION;

        case NOTEHEAD_BLACK :
        case NOTEHEAD_BLACK_2 :
        case NOTEHEAD_BLACK_3 :
            return QUARTER_DURATION;

        // Notes
        //
        case BREVE :
            return 8 * QUARTER_DURATION;

        case WHOLE_NOTE :
        case WHOLE_NOTE_2 :
        case WHOLE_NOTE_3 :
            return 4 * QUARTER_DURATION;

        // Head-Note combos
        //
        case HEAD_AND_FLAG_1 :
        case HEAD_AND_FLAG_1_UP :
        case HEAD_AND_FLAG_2 :
        case HEAD_AND_FLAG_2_UP :
        case HEAD_AND_FLAG_3 :
        case HEAD_AND_FLAG_3_UP :
        case HEAD_AND_FLAG_4 :
        case HEAD_AND_FLAG_4_UP :
        case HEAD_AND_FLAG_5 :
        case HEAD_AND_FLAG_5_UP :
            return QUARTER_DURATION;
        }

        // Error
        logger.severe("Illegal note type " + shape);

        return 0;
    }

    //--------------------//
    // populateAccidental //
    //--------------------//
    public static void populateAccidental (Glyph       glyph,
                                           Measure     measure,
                                           SystemPoint accidCenter)
    {
        Scale     scale = measure.getScale();
        final int minDx = scale.toUnits(constants.minAccidDx);
        final int maxDx = scale.toUnits(constants.maxAccidDx);
        final int maxDy = scale.toUnits(constants.maxAccidDy);
        Set<Note> candidates = new HashSet<Note>();

        // An accidental impacts the note right after (even if duplicated)
        ChordLoop: 
        for (TreeNode node : measure.getChords()) {
            Chord chord = (Chord) node;

            for (TreeNode n : chord.getNotes()) {
                Note note = (Note) n;

                if (!note.isRest()) {
                    PixelRectangle box = note.getGlyph()
                                             .getContourBox();
                    SystemPoint    noteRef = measure.getSystem()
                                                    .toSystemPoint(
                        new PixelPoint(box.x, box.y + (box.height / 2)));
                    SystemPoint    toNote = new SystemPoint(
                        noteRef.x - accidCenter.x,
                        noteRef.y - accidCenter.y);

                    if (logger.isFineEnabled()) {
                        logger.info(measure.getContextString() + " " + toNote);
                    }

                    if (toNote.x > (2 * maxDx)) {
                        break ChordLoop; // Other chords/notes will be too far
                    }

                    if ((toNote.x >= minDx) &&
                        (toNote.x <= maxDx) &&
                        (Math.abs(toNote.y) <= maxDy)) {
                        candidates.add(note);
                    }
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.info(candidates.size() + " Candidates=" + candidates);
        }

        for (Note note : candidates) {
            note.accidental = glyph.getShape();

            if (logger.isFineEnabled()) {
                logger.fine(
                    note.getContextString() + " accidental " + note.accidental +
                    " at " + note.getCenter());
            }
        }
    }

    //---------------//
    // getAccidental //
    //---------------//
    public Shape getAccidental ()
    {
        return accidental;
    }

    //----------//
    // getAlter //
    //----------//
    public int getAlter ()
    {
        if (alter == null) {
            if (accidental != null) {
                // TODO: handle double flat & double sharp !!!
                switch (accidental) {
                case SHARP :
                    return alter = 1;

                case FLAT :
                    return alter = -1;

                default :
                }
            }

            // Look for a previous accidental with the same note step in the measure
            Slot[]  slots = getMeasure()
                                .getSlots()
                                .toArray(new Slot[0]);

            boolean started = false;

            for (int is = slots.length - 1; is >= 0; is--) {
                Slot slot = slots[is];

                if (slot.isAlignedWith(getCenter())) {
                    started = true;
                }

                if (started) {
                    // Inspect all notes of all chords
                    for (Chord chord : slot.getChords()) {
                        for (TreeNode node : chord.getNotes()) {
                            Note note = (Note) node;

                            if (note == this) {
                                continue;
                            }

                            if ((note.getStep() == getStep()) &&
                                (note.getAccidental() != null)) {
                                switch (note.getAccidental()) {
                                case SHARP :
                                    return alter = 1;

                                case FLAT :
                                    return alter = -1;

                                default :
                                }
                            }
                        }
                    }
                }
            }

            // Finally, use the current key signature
            KeySignature ks = getMeasure()
                                  .getKeyBefore(getCenter());

            if (ks != null) {
                return alter = ks.getAlterFor(getStep());
            }

            // By default ...
            alter = 0;
        }

        return alter;
    }

    //-----------//
    // getOctave //
    //-----------//
    public int getOctave ()
    {
        if (octave == null) {
            Clef  clef = getMeasure()
                             .getClefBefore(getCenter());
            Shape shape = (clef != null) ? clef.getShape() : Shape.G_CLEF;
            octave = Clef.octaveOf((int) Math.rint(getPitchPosition()), shape);
        }

        return octave;
    }

    //---------//
    // getStep //
    //---------//
    public Note.Step getStep ()
    {
        if (step == null) {
            Clef  clef = getMeasure()
                             .getClefBefore(getCenter());
            Shape shape = (clef != null) ? clef.getShape() : Shape.G_CLEF;
            step = Clef.noteStepOf((int) Math.rint(getPitchPosition()), shape);
        }

        return step;
    }

    //-------------//
    // getTypeName //
    //-------------//
    public String getTypeName ()
    {
        int dur = (getTypeDuration(shape) * 48) / QUARTER_DURATION;

        // Apply fraction if any (not for rests)
        int fbn = getChord()
                      .getFlagsNumber() + getChord()
                                              .getBeams()
                                              .size();

        for (int i = 0; i < fbn; i++) {
            dur /= 2;
        }

        int index = (int) Math.rint(Math.log(dur) / Math.log(2));

        return typeNames[index];
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
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Note");
        sb.append(" ")
          .append(shape);

        if (accidental != null) {
            sb.append(" ")
              .append(accidental);
        }

        if (isRest) {
            sb.append(" ")
              .append("rest");
        }

        if (alter != 0) {
            sb.append(" alter=")
              .append(alter);
        }

        sb.append(" pp=")
          .append((int) Math.rint(pitchPosition));

        sb.append("}");

        return sb.toString();
    }

    //---------//
    // alterOf //
    //---------//
    private int alterOf (Shape accid)
    {
        switch (accid) {
        case FLAT :
            return -1;

        case NATURAL :
            return 0;

        case SHARP :
            return +1;

        case DOUBLE_SHARP :
            return +2; // To be verified

        case DOUBLE_FLAT :
            return -2; // To be verified
        }

        logger.severe("Illegal accidental shape: " + accid);

        return 0;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /**
         * Minimum dx between accidental and note
         */
        Scale.Fraction minAccidDx = new Scale.Fraction(
            0.3d,
            "Minimum dx (in interline fraction) between accidental and note");

        /**
         * Maximum dx between accidental and note
         */
        Scale.Fraction maxAccidDx = new Scale.Fraction(
            1d,
            "Maximum dx (in interline fraction) between accidental and note");

        /**
         * Maximum absolute dy between note and accidental
         */
        Scale.Fraction maxAccidDy = new Scale.Fraction(
            1d,
            "Maximum absolute dy (in interline fraction) between note and accidental");
    }
}
