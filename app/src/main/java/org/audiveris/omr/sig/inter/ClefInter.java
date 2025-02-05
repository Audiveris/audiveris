//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C l e f I n t e r                                       //
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
import org.audiveris.omr.sheet.header.StaffHeader;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>ClefInter</code> handles a Clef interpretation.
 * <p>
 * The following image, borrowed from wikipedia, explains the most popular clefs today
 * (Treble, Alto, Tenor and Bass) and for each presents where the "Middle C" note (C4) would take
 * place.
 * <p>
 * <img src=
 * "http://upload.wikimedia.org/wikipedia/commons/thumb/1/17/Middle_C_in_four_clefs.svg/600px-Middle_C_in_four_clefs.svg.png"
 * alt="Middle C in four clefs">
 * <p>
 * NoteStep line of the clef:
 * <ul>
 * <li>-4 for top line (Baritone)
 * <li>-2 for Bass and Tenor
 * <li>0 for Alto
 * <li>+2 for Treble and Mezzo-Soprano
 * <li>+4 for bottom line (Soprano)
 * </ul>
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "clef")
public class ClefInter
        extends AbstractPitchedInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ClefInter.class);

    /** A dummy default clef to be used when no current clef is defined. */
    private static final ClefInter defaultClef = new ClefInter(
            null,
            Shape.G_CLEF,
            1.0,
            null,
            +2.0,
            ClefKind.TREBLE);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Kind of the clef. */
    @XmlAttribute
    private ClefKind kind;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed for JAXB.
     */
    private ClefInter ()
    {
        super(null, null, null, (Double) null, null, null);
    }

    /**
     * Creates a new ClefInter object.
     *
     * @param glyph the glyph to interpret
     * @param shape the possible shape
     * @param grade the interpretation quality
     * @param staff the related staff
     * @param pitch pitch position
     * @param kind  clef kind
     */
    private ClefInter (Glyph glyph,
                       Shape shape,
                       Double grade,
                       Staff staff,
                       Double pitch,
                       ClefKind kind)
    {
        super(glyph, null, shape, grade, staff, pitch);
        this.kind = kind;
    }

    /**
     * Creates a <b>ghost</b> ClefInter object.
     *
     * @param shape the possible shape
     * @param grade the interpretation quality
     */
    public ClefInter (Shape shape,
                      Double grade)
    {
        this(null, shape, grade, null, null, null);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------------//
    // absolutePitchOf //
    //-----------------//
    /**
     * Report the absolute pitch corresponding to a note at the provided pitch position,
     * assuming we are governed by this clef
     *
     * @param intPitch the pitch position of the note
     * @return the corresponding absolute pitch
     */
    private int absolutePitchOf (int intPitch)
    {
        return switch (shape) {
            case G_CLEF, G_CLEF_SMALL -> 34 - intPitch;
            case G_CLEF_8VA -> (34 + 7) - intPitch;
            case G_CLEF_8VB -> 34 - 7 - intPitch;
            case C_CLEF ->
                    // Depending on precise clef position, we can have
                    // an Alto C-clef (pp=0) or a Tenor C-clef (pp=-2) [or other stuff]
                    28 - (int) Math.rint(this.pitch) - intPitch;
            case F_CLEF, F_CLEF_SMALL -> 22 - intPitch;
            case F_CLEF_8VA -> (22 + 7) - intPitch;
            case F_CLEF_8VB -> 22 - 7 - intPitch;
            case PERCUSSION_CLEF -> 0;

            default -> {
                logger.error("No absolute note pitch defined for {}", this);
                yield 0; // To keep compiler happy
            }
        };
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        // Add it to containing measure stack
        MeasureStack stack = sig.getSystem().getStackAt(getCenter());

        if (stack != null) {
            stack.addInter(this);
        }

        if (kind == null) {
            kind = kindOf(getCenter(), shape, staff);
            pitch = Double.valueOf(kind.pitch);
        }
    }

    //---------//
    // getKind //
    //---------//
    /**
     * @return the kind
     */
    public ClefKind getKind ()
    {
        return kind;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + kind;
    }

    //------------//
    // noteStepOf //
    //------------//
    /**
     * Report the note step corresponding to a note at the provided pitch position,
     * assuming we are governed by this clef
     *
     * @param pitch the pitch position of the note
     * @return the corresponding note step
     */
    private HeadInter.NoteStep noteStepOf (int pitch)
    {
        return switch (shape) {
            case G_CLEF, G_CLEF_SMALL, G_CLEF_8VA, G_CLEF_8VB, PERCUSSION_CLEF -> //
                    HeadInter.NoteStep.values()[(71 - pitch) % 7];

            case C_CLEF ->
                    // Depending on precise clef position, we can have
                    // an Alto C-clef (pp=0) or a Tenor C-clef (pp=-2) [or other stuff]
                    HeadInter.NoteStep.values()[((72 + (int) Math.rint(this.pitch)) - pitch) % 7];

            case F_CLEF, F_CLEF_SMALL, F_CLEF_8VA, F_CLEF_8VB -> //
                    HeadInter.NoteStep.values()[(73 - pitch) % 7];

            default -> {
                logger.error("No note step defined for {}", this);
                yield null; // To keep compiler happy
            }
        };
    }

    //----------//
    // octaveOf //
    //----------//
    /**
     * Report the octave corresponding to a note at the provided pitch position,
     * assuming we are governed by this clef
     *
     * @param pitchPosition the pitch position of the note
     * @return the corresponding octave
     */
    private int octaveOf (double pitchPosition)
    {
        final int intPitch = (int) Math.rint(pitchPosition);

        return switch (shape) {
            case G_CLEF, G_CLEF_SMALL, PERCUSSION_CLEF -> (34 - intPitch) / 7;
            case G_CLEF_8VA -> ((34 - intPitch) / 7) + 1;
            case G_CLEF_8VB -> ((34 - intPitch) / 7) - 1;
            case C_CLEF ->
                    // Depending on precise clef position, we can have
                    // an Alto C-clef (pp=0) or a Tenor C-clef (pp=-2) [or other stuff]
                    ((28 + (int) Math.rint(this.pitch)) - intPitch) / 7;
            case F_CLEF, F_CLEF_SMALL -> (22 - intPitch) / 7;
            case F_CLEF_8VA -> ((22 - intPitch) / 7) + 1;
            case F_CLEF_8VB -> ((22 - intPitch) / 7) - 1;

            default -> {
                logger.error("No note octave defined for {}", this);
                yield 0; // To keep compiler happy
            }
        };
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove it from containing measure.
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

        // Remove from staff header if relevant
        final StaffHeader header = staff.getHeader();

        if ((header != null) && (header.clef == this)) {
            header.clef = null;
            staff.getSystem().updateHeadersStop();
        }

        MeasureStack stack = sig.getSystem().getStackAt(getCenter());

        if (stack != null) {
            stack.removeInter(this);
        }

        super.remove(extensive);
    }

    //-----------//
    // replicate //
    //-----------//
    /**
     * Replicate this clef in a target staff.
     *
     * @param targetStaff the target staff
     * @return the replicated clef, whose bounds may need an update
     */
    public ClefInter replicate (Staff targetStaff)
    {
        return new ClefInter(null, shape, getGrade(), targetStaff, pitch, kind);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------------//
    // absolutePitchOf //
    //-----------------//
    /**
     * Report an absolute pitch value, using the current clef if any,
     * otherwise using the default clef (G_CLEF)
     *
     * @param clef          the provided current clef
     * @param pitchPosition the pitch position of the provided note
     * @return the corresponding absolute
     */
    public static int absolutePitchOf (ClefInter clef,
                                       int pitchPosition)
    {
        if (clef == null) {
            return defaultClef.absolutePitchOf(pitchPosition);
        } else {
            return clef.absolutePitchOf(pitchPosition);
        }
    }

    //-------------//
    // createValid //
    //-------------//
    /**
     * Try to create a Clef inter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance or null if failed
     */
    public static ClefInter createValid (Glyph glyph,
                                         Shape shape,
                                         Double grade,
                                         Staff staff)
    {
        if (staff.isTablature()) {
            return null;
        }

        return switch (shape) {
            default -> null;

            case G_CLEF, G_CLEF_SMALL, G_CLEF_8VA, G_CLEF_8VB -> //
                    new ClefInter(glyph, shape, grade, staff, 2.0, ClefKind.TREBLE);

            case C_CLEF -> {
                // Depending on precise clef position, we can have
                // an Alto C-clef (pp=0) or a Tenor C-clef (pp=-2)
                final Point2D center = glyph.getCenter2D();
                final double pp = Math.rint(staff.pitchPositionOf(center));
                final ClefKind kind = (pp >= -1) ? ClefKind.ALTO : ClefKind.TENOR;
                yield new ClefInter(glyph, shape, grade, staff, pp, kind);
            }

            case F_CLEF, F_CLEF_SMALL, F_CLEF_8VA, F_CLEF_8VB -> //
                    new ClefInter(glyph, shape, grade, staff, -2.0, ClefKind.BASS);

            case PERCUSSION_CLEF -> //
                    new ClefInter(glyph, shape, grade, staff, 0.0, ClefKind.PERCUSSION);
        };
    }

    //--------//
    // kindOf //
    //--------//
    /**
     * Report the ClefKind for a provided OmrShape
     *
     * @param omrShape provided OmrShape
     * @return related ClefKind
     * @throws IllegalArgumentException if provided omrShape is not mapped
     */
    public static ClefKind kindOf (OmrShape omrShape)
    {
        return switch (omrShape) {
            case gClef, gClef8vb, gClef8va, gClef15mb, gClef15ma -> ClefKind.TREBLE;
            case cClefAlto -> ClefKind.ALTO;
            case cClefTenor -> ClefKind.TENOR;
            case fClef, fClef8vb, fClef8va, fClef15mb, fClef15ma -> ClefKind.BASS;
            case unpitchedPercussionClef1 -> ClefKind.PERCUSSION;
            default -> throw new IllegalArgumentException("No ClefKind for " + omrShape);
        };
    }

    //--------//
    // kindOf //
    //--------//
    /**
     * Guess the clef kind, based on shape and location.
     *
     * @param center area center of the clef
     * @param shape  clef shape
     * @param staff  the containing shape
     * @return the precise clef kind
     */
    public static ClefKind kindOf (Point2D center,
                                   Shape shape,
                                   Staff staff)
    {
        if (staff.isTablature()) {
            return null;
        }

        return switch (shape) {
            case G_CLEF, G_CLEF_SMALL, G_CLEF_8VA, G_CLEF_8VB -> ClefKind.TREBLE;
            case C_CLEF -> {
                // Disambiguate between Alto C-clef (pp=0) and Tenor C-clef (pp=-2)
                final int pp = (int) Math.rint(staff.pitchPositionOf(center));
                yield (pp >= -1) ? ClefKind.ALTO : ClefKind.TENOR;
            }
            case F_CLEF, F_CLEF_SMALL, F_CLEF_8VA, F_CLEF_8VB -> ClefKind.BASS;
            case PERCUSSION_CLEF -> ClefKind.PERCUSSION;
            default -> null;
        };
    }

    //------------//
    // noteStepOf //
    //------------//
    /**
     * Report the note step that corresponds to a note in the provided pitch position,
     * using the current clef if any, otherwise using the default clef (G_CLEF)
     *
     * @param clef          the provided current clef
     * @param pitchPosition the pitch position of the provided note
     * @return the corresponding note step
     */
    public static HeadInter.NoteStep noteStepOf (ClefInter clef,
                                                 int pitchPosition)
    {
        if (clef == null) {
            return defaultClef.noteStepOf(pitchPosition);
        } else {
            return clef.noteStepOf(pitchPosition);
        }
    }

    //----------//
    // octaveOf //
    //----------//
    /**
     * Report the octave corresponding to a note at the provided pitch position,
     * assuming we are governed by the provided clef, otherwise (if clef is null)
     * we use the default clef (G_CLEF)
     *
     * @param clef  the current clef if any
     * @param pitch the pitch position of the note
     * @return the corresponding octave
     */
    public static int octaveOf (ClefInter clef,
                                double pitch)
    {
        if (clef == null) {
            return defaultClef.octaveOf(pitch);
        } else {
            return clef.octaveOf(pitch);
        }
    }

    //~ Enumerations -------------------------------------------------------------------------------

    /**
     * Clef kind, based on shape and pitch.
     */
    public static enum ClefKind
    {
        TREBLE(Shape.G_CLEF, 2),
        BASS(Shape.F_CLEF, -2),
        ALTO(Shape.C_CLEF, 0),
        TENOR(Shape.C_CLEF, -2),
        PERCUSSION(Shape.PERCUSSION_CLEF, 0);

        /** Symbol shape class. (regardless of ottava mark if any) */
        public final Shape shape;

        /** Pitch of reference line. */
        public final int pitch;

        ClefKind (Shape shape,
                  int pitch)
        {
            this.shape = shape;
            this.pitch = pitch;
        }
    }
}
