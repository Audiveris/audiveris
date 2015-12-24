//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t N o t e I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.math.Rational;

import omr.sheet.Staff;
import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.Voice;

import omr.sig.GradeImpacts;
import omr.sig.relation.AugmentationRelation;
import omr.sig.relation.DoubleDotRelation;
import omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.Map;

/**
 * Class {@code AbstractNoteInter} is an abstract base for all notes interpretations,
 * that is heads (with or without stem) and rests.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractNoteInter
        extends AbstractPitchedInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            AbstractNoteInter.class);

    /** The quarter duration value. */
    public static final Rational QUARTER_DURATION = new Rational(1, 4);

    /** All shape-based intrinsic durations. */
    private static final Map<Shape, Rational> shapeDurations = buildShapeDurations();

    //~ Enumerations -------------------------------------------------------------------------------
    /** Names of the various note steps. */
    public static enum Step
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** La */
        A,
        /** Si */
        B,
        /** Do */
        C,
        /** Ré */
        D,
        /** Mi */
        E,
        /** Fa */
        F,
        /** Sol */
        G;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Note step. */
    protected Step step;

    /** Octave. */
    protected Integer octave;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractNoteInter object.
     *
     * @param glyph   the underlying glyph, if any
     * @param bounds  the object bounds
     * @param shape   the underlying shape
     * @param impacts the grade details
     * @param staff   the related staff
     * @param pitch   the note pitch
     */
    public AbstractNoteInter (Glyph glyph,
                              Rectangle bounds,
                              Shape shape,
                              GradeImpacts impacts,
                              Staff staff,
                              double pitch)
    {
        super(glyph, bounds, shape, impacts, staff, pitch);
    }

    /**
     * Creates a new AbstractNoteInter object.
     *
     * @param glyph  the underlying glyph, if any
     * @param bounds the object bounds
     * @param shape  the underlying shape
     * @param grade  the assignment quality
     * @param staff  the related staff
     * @param pitch  the note pitch
     */
    public AbstractNoteInter (Glyph glyph,
                              Rectangle bounds,
                              Shape shape,
                              double grade,
                              Staff staff,
                              double pitch)
    {
        super(glyph, bounds, shape, grade, staff, pitch);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected AbstractNoteInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getChord //
    //----------//
    public AbstractChordInter getChord ()
    {
        return (AbstractChordInter) getEnsemble();
    }

    //-------------//
    // getDotCount //
    //-------------//
    /**
     * Report the number of augmentation dots (0, 1 or 2) that apply to this note.
     *
     * @return the count of augmentation dots
     */
    public int getDotCount ()
    {
        AugmentationDotInter firstDot = getFirstAugmentationDot();

        if (firstDot != null) {
            if (sig.hasRelation(firstDot, DoubleDotRelation.class)) {
                return 2;
            }

            return 1;
        }

        return 0;
    }

    //-------------------------//
    // getFirstAugmentationDot //
    //-------------------------//
    /**
     * Report the first augmentation dot, if any, that is linked to this note.
     *
     * @return the first dot, if any
     */
    public AugmentationDotInter getFirstAugmentationDot ()
    {
        for (Relation dn : sig.getRelations(this, AugmentationRelation.class)) {
            return (AugmentationDotInter) sig.getOppositeInter(this, dn);
        }

        return null;
    }

    //------------------//
    // getShapeDuration //
    //------------------//
    /**
     * Report the duration indicated by the shape of the note or rest
     * (regardless of any beam, flag, dot or tuplet).
     *
     * @param shape the shape of the note / rest
     * @return the corresponding intrinsic duration
     */
    public static Rational getShapeDuration (Shape shape)
    {
        return shapeDurations.get(shape);
    }

    //--------//
    // delete //
    //--------//
    /**
     * Since a note instance is held by its containing staff, make sure staff
     * notes collection is updated.
     *
     * @see #undelete()
     */
    @Override
    public void delete ()
    {
        if (staff != null) {
            staff.removeNote(this);
        }

        super.delete();
    }

    //-----------//
    // getOctave //
    //-----------//
    /**
     * Report the octave for this note, using the current clef, and the pitch position
     * of the note.
     *
     * @return the related octave
     */
    public int getOctave ()
    {
        if (octave == null) {
            AbstractChordInter chord = (AbstractChordInter) getEnsemble();
            Measure measure = chord.getMeasure();
            octave = ClefInter.octaveOf(measure.getClefBefore(getCenter(), getStaff()), pitch);
        }

        return octave;
    }

    //---------//
    // getStep //
    //---------//
    /**
     * Report the note step (within the octave)
     *
     * @return the note step
     */
    public Step getStep ()
    {
        if (step == null) {
            AbstractChordInter chord = (AbstractChordInter) getEnsemble();
            Measure measure = chord.getMeasure();
            step = ClefInter.noteStepOf(
                    measure.getClefBefore(getCenter(), staff),
                    (int) Math.rint(pitch));
        }

        return step;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        final AbstractChordInter chord = getChord();

        if (chord != null) {
            return chord.getVoice();
        }

        return null;
    }

    //----------//
    // undelete //
    //----------//
    /**
     * Since a note instance is held by its containing staff, make sure staff
     * notes collection is updated.
     *
     * @see #delete()
     */
    @Override
    public void undelete ()
    {
        super.undelete();

        if (staff != null) {
            staff.addNote(this);
        }
    }

    //---------------------//
    // buildShapeDurations //
    //---------------------//
    /**
     * Populate the map of intrinsic shape durations.
     *
     * @return the populated map
     */
    private static EnumMap<Shape, Rational> buildShapeDurations ()
    {
        EnumMap<Shape, Rational> map = new EnumMap<Shape, Rational>(Shape.class);

        map.put(Shape.LONG_REST, new Rational(4, 1)); // 4 measures

        map.put(Shape.BREVE_REST, new Rational(2, 1)); // 2 measures
        map.put(Shape.BREVE, new Rational(2, 1));

        map.put(Shape.WHOLE_REST, Rational.ONE); // 1 measure
        map.put(Shape.WHOLE_NOTE, Rational.ONE);

        map.put(Shape.HALF_REST, new Rational(1, 2));
        map.put(Shape.NOTEHEAD_VOID, new Rational(1, 2));
        map.put(Shape.NOTEHEAD_VOID_SMALL, new Rational(1, 2));

        map.put(Shape.QUARTER_REST, QUARTER_DURATION);
        map.put(Shape.NOTEHEAD_BLACK, QUARTER_DURATION);
        map.put(Shape.NOTEHEAD_BLACK_SMALL, QUARTER_DURATION);

        map.put(Shape.EIGHTH_REST, new Rational(1, 8));

        map.put(Shape.ONE_16TH_REST, new Rational(1, 16));

        map.put(Shape.ONE_32ND_REST, new Rational(1, 32));

        map.put(Shape.ONE_64TH_REST, new Rational(1, 64));

        map.put(Shape.ONE_128TH_REST, new Rational(1, 128));

        return map;
    }
}
