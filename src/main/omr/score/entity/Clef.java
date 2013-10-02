//----------------------------------------------------------------------------//
//                                                                            //
//                                  C l e f                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.score.visitor.ScoreVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

/**
 * Class {@code Clef} encapsulates a clef.
 *
 * <p>The following image, directly pulled from wikipedia, explains the most
 * popular clefs today (Treble, Alto, Tenor and Bass) and for each presents
 * where the "Middle C" note (C4) would take place. These informations are used
 * by methods {@link #octaveOf(omr.score.entity.Clef, int)} and
 * {@link #noteStepOf(omr.score.entity.Clef, int)}.</p>
 * <p>
 * <img
 * src="http://upload.wikimedia.org/wikipedia/commons/thumb/1/17/Middle_C_in_four_clefs.svg/600px-Middle_C_in_four_clefs.svg.png"
 * />
 *
 * @author Hervé Bitteur
 */
public class Clef
        extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Clef.class);

    /** A dummy default clef to be used when no current clef is defined */
    private static Clef defaultClef = new Clef(
            null,
            null,
            Shape.G_CLEF,
            null,
            +2,
            null);

    //~ Instance fields --------------------------------------------------------
    /** Precise clef shape, from Clefs range in Shape class */
    private Shape shape;

    /**
     * Step line of the clef : -4 for top line (Baritone), -2 for Bass and
     * Tenor,
     * 0 for Alto, +2 for Treble and Mezzo-Soprano, +4 for bottom line
     * (Soprano).
     */
    private int pitchPosition;

    //~ Constructors -----------------------------------------------------------
    //------//
    // Clef //
    //------//
    /**
     * Create a Clef instance
     *
     * @param measure       the containing measure
     * @param staff         the assigned staff
     * @param shape         precise clef shape
     * @param center        center wrt system (in units)
     * @param pitchPosition pitch position
     * @param glyph         underlying glyph, if any
     */
    public Clef (Measure measure,
                 Staff staff,
                 Shape shape,
                 Point center,
                 int pitchPosition,
                 Glyph glyph)
    {
        super(measure);

        if (glyph != null) {
            addGlyph(glyph);
        }

        setStaff(staff);
        this.shape = (shape == Shape.G_CLEF_SMALL) ? Shape.G_CLEF : shape;
        setCenter(center);
        this.pitchPosition = pitchPosition;

        getBox(); // Not really needed
    }

    //------//
    // Clef //
    //------//
    /**
     * Create a Clef instance, by cloning another clef
     *
     * @param measure the containing measure
     * @param staff   the assigned staff
     * @param other   the existing clef to clone
     */
    public Clef (Measure measure,
                 Staff staff,
                 Clef other)
    {
        this(
                measure,
                staff,
                other.getShape(),
                other.getCenter(),
                other.getPitchPosition(),
                null);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // noteStepOf //
    //------------//
    /**
     * Report the note step that corresponds to a note in the provided pitch
     * position, using the current clef if any, otherwise using the default clef
     * (G_CLEF)
     *
     * @param clef          the provided current clef
     * @param pitchPosition the pitch position of the provided note
     * @return the corresponding note step
     */
    public static Note.Step noteStepOf (Clef clef,
                                        int pitchPosition)
    {
        if (clef == null) {
            return defaultClef.noteStepOf(pitchPosition);
        } else {
            return clef.noteStepOf(pitchPosition);
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

    //------------------//
    // getPitchPosition //
    //------------------//
    /**
     * Report the vertical position of this clef within the staff
     *
     * @return the pitch position
     */
    public int getPitchPosition ()
    {
        return pitchPosition;
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the precise shape of this clef
     *
     * @return the clef shape
     */
    public Shape getShape ()
    {
        return shape;
    }

    //----------//
    // octaveOf //
    //----------//
    /**
     * Report the octave corresponding to a note at the provided pitch position,
     * assuming we are governed by the provided clef, otherwise (if clef is
     * null)
     * we use the default clef (G_CLEF)
     *
     * @param clef          the current clef if any
     * @param pitchPosition the pitch position of the note
     * @return the corresponding octave
     */
    public static int octaveOf (Clef clef,
                                int pitchPosition)
    {
        if (clef == null) {
            return defaultClef.octaveOf(pitchPosition);
        } else {
            return clef.octaveOf(pitchPosition);
        }
    }

    //----------//
    // populate //
    //----------//
    /**
     * Create the relevant Clef entity that translates the provided glyph
     *
     * @param glyph   the provided glyph
     * @param measure the containing measure
     * @param staff   the containing staff
     * @param center  the precise location in the system
     * @return true if Clef was successfully created
     */
    public static boolean populate (Glyph glyph,
                                    Measure measure,
                                    Staff staff,
                                    Point center)
    {
        Shape shape = glyph.getShape();

        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
            glyph.setTranslation(
                    new Clef(measure, staff, shape, center, 2, glyph));

            return true;

        case C_CLEF:

            // Depending on precise clef position, we can have
            // an Alto C-clef (pp=0) or a Tenor C-clef (pp=+2) [or other stuff]
            int pp = (int) Math.rint(staff.pitchPositionOf(center));
            glyph.setTranslation(
                    new Clef(measure, staff, shape, center, pp, glyph));

            return true;

        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
            glyph.setTranslation(
                    new Clef(measure, staff, shape, center, -2, glyph));

            return true;

        case PERCUSSION_CLEF:
            glyph.setTranslation(
                    new Clef(measure, staff, shape, center, 0, glyph));

            return true;

        default:
            measure.addError(glyph, "No implementation yet for " + shape);

            return false;
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Clef");
        sb.append(" ")
                .append(shape);

        sb.append(" pp=")
                .append(pitchPosition);

        sb.append(" ")
                .append(Glyphs.toString(getGlyphs()));

        sb.append("}");

        return sb.toString();
    }

    //------------//
    // noteStepOf //
    //------------//
    /**
     * Report the note step corresponding to a note at the provided pitch
     * position, assuming we are governed by this clef
     *
     * @param pitchPosition the pitch position of the note
     * @return the corresponding note step
     */
    private Note.Step noteStepOf (int pitchPosition)
    {
        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
            return Note.Step.values()[(71 - pitchPosition) % 7];

        case C_CLEF:

            // Depending on precise clef position, we can have
            // an Alto C-clef (pp=0) or a Tenor C-clef (pp=+2) [or other stuff]
            return Note.Step.values()[(72 - this.pitchPosition - pitchPosition) % 7];

        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
            return Note.Step.values()[(73 - pitchPosition) % 7];

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
     * @param pitchPosition the pitch position of the note
     * @return the corresponding octave
     */
    private int octaveOf (int pitchPosition)
    {
        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
            return (34 - pitchPosition) / 7;

        case G_CLEF_8VA:
            return ((34 - pitchPosition) / 7) + 1;

        case G_CLEF_8VB:
            return ((34 - pitchPosition) / 7) - 1;

        case C_CLEF:

            // Depending on precise clef position, we can have
            // an Alto C-clef (pp=0) or a Tenor C-clef (pp=+2) [or other stuff]
            return (28 - this.pitchPosition - pitchPosition) / 7;

        case F_CLEF:
        case F_CLEF_SMALL:
            return (22 - pitchPosition) / 7;

        case F_CLEF_8VA:
            return ((22 - pitchPosition) / 7) + 1;

        case F_CLEF_8VB:
            return ((22 - pitchPosition) / 7) - 1;

        case PERCUSSION_CLEF:
            return 0;

        default:
            logger.error("No note octave defined for {}", this);

            return 0; // To keep compiler happy
        }
    }
}
