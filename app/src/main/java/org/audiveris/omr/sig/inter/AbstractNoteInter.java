//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t N o t e I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

/**
 * Class <code>AbstractNoteInter</code> is an abstract base for all notes interpretations,
 * that is heads (with or without stem) and rests.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractNoteInter
        extends AbstractPitchedInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractNoteInter.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    protected AbstractNoteInter ()
    {
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
                              Double grade,
                              Staff staff,
                              Double pitch)
    {
        super(glyph, bounds, shape, grade, staff, pitch);
    }

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

    //~ Methods ------------------------------------------------------------------------------------

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

        return ClefInter.absolutePitchOf(clef, getIntegerPitch());
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
    public NoteStep getStep ()
    {
        AbstractChordInter chord = getChord();
        Measure measure = chord.getMeasure();
        ClefInter clef = measure.getClefBefore(getCenter(), staff);

        return ClefInter.noteStepOf(clef, getIntegerPitch());
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
        if (isRemoved()) {
            return;
        }

        if (staff != null) {
            staff.removeNote(this);
        }

        super.remove(extensive);
    }

    //~ Enumerations -------------------------------------------------------------------------------

    /**
     * Enum <code>NoteStep</code> describes the names of the various note steps.
     */
    public static enum NoteStep
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
