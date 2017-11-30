//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C l e f I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ClefInter} handles a Clef interpretation.
 * <p>
 * The following image, directly pulled from wikipedia, explains the most popular clefs today
 * (Treble, Alto, Tenor and Bass) and for each presents where the "Middle C" note (C4) would take
 * place.
 * <p>
 * <img src="http://upload.wikimedia.org/wikipedia/commons/thumb/1/17/Middle_C_in_four_clefs.svg/600px-Middle_C_in_four_clefs.svg.png"
 * alt="Middle C in four clefs">
 * <p>
 * Step line of the clef : -4 for top line (Baritone), -2 for Bass and Tenor,
 * 0 for Alto, +2 for Treble and Mezzo-Soprano, +4 for bottom line (Soprano).
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
            1,
            null,
            +2.0,
            ClefKind.TREBLE);

    //~ Enumerations -------------------------------------------------------------------------------
    public static enum ClefKind
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        TREBLE(Shape.G_CLEF, 2),
        BASS(Shape.F_CLEF, -2),
        ALTO(Shape.C_CLEF, 0),
        TENOR(Shape.C_CLEF, -2),
        PERCUSSION(Shape.PERCUSSION_CLEF, 0);

        //~ Instance fields ------------------------------------------------------------------------
        /** Symbol shape class. (regardless of ottava mark if any) */
        public final Shape shape;

        /** Pitch of reference line. */
        public final int pitch;

        //~ Constructors ---------------------------------------------------------------------------
        ClefKind (Shape shape,
                  int pitch)
        {
            this.shape = shape;
            this.pitch = pitch;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Kind of the clef. */
    @XmlAttribute
    private final ClefKind kind;

    //~ Constructors -------------------------------------------------------------------------------
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
    public ClefInter (Glyph glyph,
                      Shape shape,
                      double grade,
                      Staff staff,
                      Double pitch,
                      ClefKind kind)
    {
        super(glyph, null, shape, grade, staff, pitch);
        this.kind = kind;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private ClefInter ()
    {
        super(null, null, null, null, null, null);
        this.kind = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * Create a Clef inter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance or null if failed
     */
    public static ClefInter create (Glyph glyph,
                                    Shape shape,
                                    double grade,
                                    Staff staff)
    {
        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
            return new ClefInter(glyph, shape, grade, staff, 2.0, ClefKind.TREBLE);

        case C_CLEF:

            // Depending on precise clef position, we can have
            // an Alto C-clef (pp=0) or a Tenor C-clef (pp=-2)
            Point center = glyph.getCenter();
            double pp = Math.rint(staff.pitchPositionOf(center));
            ClefKind kind = (pp >= -1) ? ClefKind.ALTO : ClefKind.TENOR;

            return new ClefInter(glyph, shape, grade, staff, pp, kind);

        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
            return new ClefInter(glyph, shape, grade, staff, -2.0, ClefKind.BASS);

        case PERCUSSION_CLEF:
            return new ClefInter(glyph, shape, grade, staff, 0.0, ClefKind.PERCUSSION);

        default:
            return null;
        }
    }

    //--------//
    // kindOf //
    //--------//
    public static ClefKind kindOf (Glyph glyph,
                                   Shape shape,
                                   Staff staff)
    {
        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
            return ClefKind.TREBLE;

        case C_CLEF:

            // Disambiguate between Alto C-clef (pp=0) and Tenor C-clef (pp=-2)
            Point center = glyph.getCenter();
            int pp = (int) Math.rint(staff.pitchPositionOf(center));

            return (pp >= -1) ? ClefKind.ALTO : ClefKind.TENOR;

        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
            return ClefKind.BASS;

        case PERCUSSION_CLEF:
            return ClefKind.PERCUSSION;

        default:
            return null;
        }
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
    public static HeadInter.Step noteStepOf (ClefInter clef,
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

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * @return the kind
     */
    public ClefKind getKind ()
    {
        return kind;
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
        return new ClefInter(null, shape, 0, targetStaff, pitch, kind);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape + " " + kind;
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
    private HeadInter.Step noteStepOf (int pitch)
    {
        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
            return HeadInter.Step.values()[(71 - pitch) % 7];

        case C_CLEF:

            // Depending on precise clef position, we can have
            // an Alto C-clef (pp=0) or a Tenor C-clef (pp=+2) [or other stuff]
            return HeadInter.Step.values()[(72 - (int) Math.rint(this.pitch) - pitch) % 7];

        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
            return HeadInter.Step.values()[(73 - pitch) % 7];

        case PERCUSSION_CLEF:
            return null;

        default:
            logger.error("No note step defined for {}", this);

            return null; // To keep compiler happy
        }
    }

    //----------//
    // octaveOf //
    //----------//
    /**
     * Report the octave corresponding to a note at the provided pitch position,
     * assuming we are governed by this clef
     *
     * @param pitch the pitch position of the note
     * @return the corresponding octave
     */
    private int octaveOf (double pitchPosition)
    {
        int pitch = (int) Math.rint(pitchPosition);

        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
            return (34 - pitch) / 7;

        case G_CLEF_8VA:
            return ((34 - pitch) / 7) + 1;

        case G_CLEF_8VB:
            return ((34 - pitch) / 7) - 1;

        case C_CLEF:

            // Depending on precise clef position, we can have
            // an Alto C-clef (pp=0) or a Tenor C-clef (pp=-2) [or other stuff]
            return (28 - (int) Math.rint(this.pitch) - pitch) / 7;

        case F_CLEF:
        case F_CLEF_SMALL:
            return (22 - pitch) / 7;

        case F_CLEF_8VA:
            return ((22 - pitch) / 7) + 1;

        case F_CLEF_8VB:
            return ((22 - pitch) / 7) - 1;

        case PERCUSSION_CLEF:
            return 0;

        default:
            logger.error("No note octave defined for {}", this);

            return 0; // To keep compiler happy
        }
    }
}
