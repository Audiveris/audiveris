//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t N o t e I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.Relation;

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

    private static final Logger logger = LoggerFactory.getLogger(AbstractNoteInter.class);

    /** The quarter duration value. */
    public static final Rational QUARTER_DURATION = new Rational(1, 4);

    /** All shape-based intrinsic durations. */
    private static final Map<Shape, Rational> shapeDurations = buildShapeDurations();

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
                              Double pitch)
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
                              Double pitch)
    {
        super(glyph, bounds, shape, grade, staff, pitch);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected AbstractNoteInter ()
    {
    }

    //----------//
    // getChord //
    //----------//
    /**
     * Report the containing chord, if any.
     *
     * @return containing chord or null
     */
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
        if (sig != null) {
            for (Relation dn : sig.getRelations(this, AugmentationRelation.class)) {
                return (AugmentationDotInter) sig.getOppositeInter(this, dn);
            }
        }

        return null;
    }

    //-------//
    // added //
    //-------//
    /**
     * Since a note instance is held by its containing staff, make sure staff
     * notes collection is updated.
     *
     * @see #remove()
     */
    @Override
    public void added ()
    {
        super.added();

        if (staff != null) {
            staff.addNote(this);
        }
    }

    //------------------//
    // getAbsolutePitch //
    //------------------//
    /**
     * Report the absolute pitch for this note, using the current clef, and the pitch
     * position of the note.
     *
     * @return the related "absolute" pitch
     */
    public int getAbsolutePitch ()
    {
        AbstractChordInter chord = getChord();
        Measure measure = chord.getMeasure();
        ClefInter clef = measure.getClefBefore(getCenter(), getStaff());

        return ClefInter.absolutePitchOf(clef, (int) Math.rint(pitch));
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
        AbstractChordInter chord = getChord();
        Measure measure = chord.getMeasure();
        ClefInter clef = measure.getClefBefore(getCenter(), getStaff());

        return ClefInter.octaveOf(clef, pitch);
    }

    //---------//
    // getStep //
    //---------//
    /**
     * Report the note step (within the octave).
     *
     * @return the note step
     */
    public Step getStep ()
    {
        AbstractChordInter chord = getChord();
        Measure measure = chord.getMeasure();
        ClefInter clef = measure.getClefBefore(getCenter(), staff);

        return ClefInter.noteStepOf(clef, (int) Math.rint(pitch));
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

    //--------//
    // remove //
    //--------//
    /**
     * Since a note instance is held by its containing staff, make sure staff
     * notes collection is updated.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        if (staff != null) {
            staff.removeNote(this);
        }

        super.remove(extensive);
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
        EnumMap<Shape, Rational> map = new EnumMap<>(Shape.class);

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

    /** Names of the various note steps. */
    public static enum Step
    {
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
}
