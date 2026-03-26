//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C l e f I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.header.StaffHeader;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
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
 * Pitch of NoteStep line of the clef:
 * <ul>
 * <li>-4 for top line (old Baritone)
 * <li>-2 for Bass and Tenor
 * <li>0 for Alto and Baritone
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
     * @param kind  clef kind
     */
    private ClefInter (Glyph glyph,
                       Shape shape,
                       Double grade,
                       Staff staff,
                       ClefKind kind)
    {
        super(glyph, null, shape, grade, staff, kind == null ? null : (double) kind.pitch);
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
        this(null, shape, grade, null, null);
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
            case G_CLEF_8VB -> (34 - 7) - intPitch;
            case C_CLEF -> (28 - (int) Math.rint(this.pitch)) - intPitch;
            case F_CLEF, F_CLEF_SMALL -> (20 - (int) Math.rint(this.pitch)) - intPitch;
            case F_CLEF_8VA -> (22 + 7) - intPitch;
            case F_CLEF_8VB -> (22 - 7) - intPitch;
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

        // Add it to the containing measure stack
        MeasureStack stack = sig.getSystem().getStackAt(getCenter());

        if (stack != null) {
            stack.addInter(this);
        }

        if (kind == null) {
            kind = kindOf(getCenter(), shape, staff);
            pitch = Double.valueOf(kind.pitch);
        }
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation)
    {
        // First call needed to get clef bounds
        super.deriveFrom(symbol, sheet, font, dropLocation);

        if (staff != null) {
            // On a 1-line staff, only percussion clef is allowed
            if (staff.isOneLineStaff() && (shape != Shape.PERCUSSION_CLEF)) {
                return false;
            }

            // We snap ordinate to specific lines, according to the clef shape
            boolean modified = false;
            final Point center = getCenter();
            final Integer targetPitch = getTargetPitch(symbol.getShape(), center, staff);

            if (targetPitch != null) {
                // Adjust the ordinate
                final Rectangle box = getBounds();
                final double halfHeight = box.height / 2.0;
                final double pitchOffset = getAreaPitchOffset(shape);
                final double newCenterY = staff.pitchToOrdinate(
                        center.getX(),
                        targetPitch - pitchOffset);
                box.y = (int) Math.rint(newCenterY - halfHeight);
                setBounds(box);
                setPitch((double) targetPitch); // Force target pitch
                dropLocation = GeoUtil.center(box);
                modified = true;

                if (kindIsMutable(shape)) {
                    // Precise the clef kind
                    kind = kindOf(targetPitch, shape, staff);
                }
            }

            if (modified) {
                // Final call with refined dropLocation
                super.deriveFrom(symbol, sheet, font, dropLocation);
            }
        }

        return true;
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //---------//
    // getKind //
    //---------//
    /**
     * Report the current kind of the clef.
     * <p>
     * NOTA: The kind changes if a C_CLEF / F_CLEF / F_CLEF_SMALL is moved up or down
     *
     * @return the kind
     */
    public ClefKind getKind ()
    {
        return kind;
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    @Override
    public Point2D getRelationCenter ()
    {
        final Point2D center = getCenter2D();

        if (staff == null) {
            return center;
        }

        final double il = staff.getSpecificInterline();

        return new Point2D.Double(
                center.getX(),
                center.getY() + (0.5 * il * getAreaPitchOffset(shape)));
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return kind + " " + shape;
    }

    //-------------------------//
    // imposeWithinStaffLimits //
    //-------------------------//
    @Override
    public boolean imposeWithinStaffLimits ()
    {
        return true;
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
                    HeadInter.NoteStep.values()[((72 + (int) Math.rint(this.pitch)) - pitch) % 7];

            case F_CLEF, F_CLEF_SMALL -> //
                    HeadInter.NoteStep.values()[((75 + (int) Math.rint(this.pitch)) - pitch) % 7];

            case F_CLEF_8VA, F_CLEF_8VB -> //
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
        // @formatter:off
        final int intPitch = (int)Math.rint(pitchPosition);

        return switch (shape) {
            case G_CLEF, G_CLEF_SMALL, PERCUSSION_CLEF -> (34 - intPitch) / 7;
            case G_CLEF_8VA -> ((34 - intPitch) / 7) + 1;
            case G_CLEF_8VB -> ((34 - intPitch) / 7) - 1;
            case C_CLEF -> ((28 + (int) Math.rint(this.pitch)) - intPitch) / 7;
            case F_CLEF, F_CLEF_SMALL -> ((24  + (int) Math.rint(this.pitch)) - intPitch) / 7;
            case F_CLEF_8VA -> ((22 - intPitch) / 7) + 1;
            case F_CLEF_8VB -> ((22 - intPitch) / 7) - 1;

            default -> {
                logger.error("No note octave defined for {}", this);
                yield 0; // To keep compiler happy
            }
        };
        // @formatter:on
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
        return new ClefInter(null, shape, getGrade(), targetStaff, kind);
    }

    //-----------//
    // setBounds //
    //-----------//
    /**
     * This overriding method simply sets the bounds, <b>without re-computing the clef pitch</b>.
     *
     * @param bounds the clef bounds
     */
    @Override
    public void setBounds (Rectangle bounds)
    {
        // Just set bounds, without recomputing the pitch!
        this.bounds = (bounds != null) ? new Rectangle(bounds) : null;
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

    //---------//
    // cKindOf //
    //---------//
    /**
     * Report the precise ClefKind for a C_CLEF shape at the provided pitch.
     *
     * @param pitch the provided pitch (rounded to multiple of 2)
     * @return the corresponding ClefKind
     */
    public static ClefKind cKindOf (int pitch)
    {
        return switch (pitch) {
            case -2 -> ClefKind.TENOR;
            case 0 -> ClefKind.ALTO;
            case 2 -> ClefKind.MEZZO_SOPRANO;
            case 4 -> ClefKind.SOPRANO;
            default -> null;
        };
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
                    new ClefInter(glyph, shape, grade, staff, ClefKind.TREBLE);

            case C_CLEF, F_CLEF, F_CLEF_SMALL -> {
                final Point2D center = glyph.getCenter2D();
                final ClefKind kind = kindOf(center, shape, staff); // TODO: check this!
                yield new ClefInter(glyph, shape, grade, staff, kind);
            }

            case F_CLEF_8VA, F_CLEF_8VB -> //
                    new ClefInter(glyph, shape, grade, staff, ClefKind.BASS);

            case PERCUSSION_CLEF -> //
                    new ClefInter(glyph, shape, grade, staff, ClefKind.PERCUSSION);
        };
    }

    //---------//
    // fKindOf //
    //---------//
    /**
     * Report the precise ClefKind for a F_CLEF / F_CLEF_SMALL shape at the provided pitch.
     *
     * @param pitch the provided pitch (rounded to multiple of 2)
     * @return the corresponding ClefKind
     */
    public static ClefKind fKindOf (int pitch)
    {
        return switch (pitch) {
            case -2 -> ClefKind.BASS;
            case 0 -> ClefKind.BARITONE;
            default -> null;
        };
    }

    //----------------//
    // getTargetPitch //
    //----------------//
    /**
     * Report the theoretical pitch of clef reference line when correctly aligned with
     * possible staff lines.
     *
     * @param shape  clef shape
     * @param center location of clef center
     * @param staff  related staff
     * @return the proper ordinate if any, null otherwise
     */
    public static Integer getTargetPitch (Shape shape,
                                          Point2D center,
                                          Staff staff)
    {
        if ((staff == null) || staff.isTablature()) {
            return null;
        }

        return switch (shape) {
            case C_CLEF -> getTargetPitch(shape, center, staff, -2, 4);
            case F_CLEF, F_CLEF_SMALL -> getTargetPitch(shape, center, staff, -2, 0);
            case F_CLEF_8VA, F_CLEF_8VB -> -2;
            case G_CLEF, G_CLEF_SMALL, G_CLEF_8VA, G_CLEF_8VB -> 2;
            case PERCUSSION_CLEF -> 0;
            default -> {
                logger.error("{} is not a valid shape for a clef", shape);
                yield null;}
        };
    }

    //----------------//
    // getTargetPitch //
    //----------------//
    /**
     * Report the closest theoretical reference pitch for clef.
     *
     * @param shape  clef shape
     * @param center current clef area center
     * @param staff  underlying staff
     * @param min    minimum pitch value
     * @param max    maximum pitch value
     * @return the most suitable reference pitch value
     */
    private static int getTargetPitch (Shape shape,
                                       Point2D center,
                                       Staff staff,
                                       int min,
                                       int max)
    {
        // Pitch even target value
        final double centerPitch = staff.pitchPositionOf(center);
        final double pitchOffset = getAreaPitchOffset(shape); // Area center -> Clef reference point
        final int evenPitchTarget = 2 * (int) Math.rint((centerPitch + pitchOffset) / 2.0);

        return Math.min(max, Math.max(min, evenPitchTarget));
    }

    //---------------//
    // kindIsMutable //
    //---------------//
    /**
     * Report whether the clef kind can change, based on the vertical location of the shape.
     *
     * @param shape the clef shape
     * @return true if so
     */
    private static boolean kindIsMutable (Shape shape)
    {
        return (shape == Shape.C_CLEF) || (shape == Shape.F_CLEF) || (shape == Shape.F_CLEF_SMALL);
    }

    //--------//
    // kindOf //
    //--------//
    /**
     * Report the ClefKind for a provided OmrShape.
     *
     * @param omrShape provided OmrShape
     * @return related ClefKind
     * @throws IllegalArgumentException if provided omrShape is not mapped
     */
    public static ClefKind kindOf (OmrShape omrShape)
    {
        return switch (omrShape) {
            case gClef -> ClefKind.TREBLE;
            case cClefAlto -> ClefKind.ALTO;
            case cClefTenor -> ClefKind.TENOR;
            case fClef -> ClefKind.BASS;
            case unpitchedPercussionClef1 -> ClefKind.PERCUSSION;
            default -> throw new IllegalArgumentException("No ClefKind for " + omrShape);
        };
    }

    //--------//
    // kindOf //
    //--------//
    /**
     * Guess the clef kind, based on shape and pitch.
     *
     * @param pitch target pitch
     * @param shape clef shape
     * @param staff the containing staff
     * @return the precise clef kind
     */
    public static ClefKind kindOf (int pitch,
                                   Shape shape,
                                   Staff staff)
    {
        if (staff.isTablature()) {
            return null;
        }

        return switch (shape) {
            case G_CLEF, G_CLEF_SMALL, G_CLEF_8VA, G_CLEF_8VB -> ClefKind.TREBLE;
            case C_CLEF -> cKindOf(pitch);
            case F_CLEF, F_CLEF_SMALL -> fKindOf(pitch);
            case F_CLEF_8VA, F_CLEF_8VB -> ClefKind.BASS;

            case PERCUSSION_CLEF -> ClefKind.PERCUSSION;
            default -> null;
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
            case C_CLEF -> cKindOf(ClefInter.getTargetPitch(shape, center, staff));
            case F_CLEF, F_CLEF_SMALL -> fKindOf(ClefInter.getTargetPitch(shape, center, staff));
            case F_CLEF_8VA, F_CLEF_8VB -> ClefKind.BASS;

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

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // ClefKind //
    //----------//
    /**
     * Clef kind, based on shape and pitch.
     */
    public static enum ClefKind
    {
        TREBLE(Shape.G_CLEF, 2),

        BASS(Shape.F_CLEF, -2),
        BARITONE(Shape.F_CLEF, 0),

        TENOR(Shape.C_CLEF, -2),
        ALTO(Shape.C_CLEF, 0),
        MEZZO_SOPRANO(Shape.C_CLEF, 2),
        SOPRANO(Shape.C_CLEF, 4),

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

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a clef.
     * <p>
     * For a clef, we provide only one handle:
     * <ul>
     * <li>Middle handle, moving vertically only for C_CLEF / F_CLEF / F_CLEF_SMALL,
     * and horizontally for all shapes.
     * </ul>
     */
    private static class Editor
            extends InterEditor
    {
        // Original data
        private final Rectangle originalBounds;

        // Latest data
        private final Rectangle latestBounds;

        public Editor (final ClefInter clef)
        {
            super(clef);

            final Shape shape = clef.getShape();
            final Staff staff = clef.getStaff();

            originalBounds = clef.getBounds();
            latestBounds = clef.getBounds();

            final double halfHeight = latestBounds.height / 2.0;
            final double halfWidth = latestBounds.width / 2.0;

            handles.add(selectedHandle = new InterEditor.Handle(clef.getCenter())
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    if (!kindIsMutable(shape)) {
                        dy = 0;
                    }

                    // Handle
                    PointUtil.add(selectedHandle.getPoint(), dx, dy);

                    // Data
                    final Point2D center = selectedHandle.getPoint();

                    if (staff.contains(center)) {
                        latestBounds.x = (int) Math.rint(center.getX() - halfWidth);
                        latestBounds.y = (int) Math.rint(center.getY() - halfHeight);

                        // Adjust ordinate
                        final Integer targetPitch = getTargetPitch(shape, center, staff);
                        final double pitchOffset = getAreaPitchOffset(shape);
                        final double newCenterY = staff.pitchToOrdinate(
                                center.getX(),
                                targetPitch - pitchOffset);
                        latestBounds.y = (int) Math.rint(newCenterY - halfHeight);
                        clef.setBounds(latestBounds);
                        clef.setPitch((double) targetPitch); // Force target pitch

                        if (kindIsMutable(shape)) {
                            // Adjust precise kind
                            final ClefKind oldKind = clef.kind;
                            clef.kind = kindOf(targetPitch, clef.shape, staff);

                            if (clef.kind != oldKind) {
                                staff.getSystem().getSheet().getInterIndex().publish(clef);
                            }
                        }
                    }

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            getInter().setBounds(latestBounds);
            super.doit(); // No more glyph
        }

        @Override
        public void undo ()
        {
            getInter().setBounds(originalBounds);
            super.undo();
        }
    }
}
