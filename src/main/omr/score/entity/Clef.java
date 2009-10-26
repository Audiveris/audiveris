//----------------------------------------------------------------------------//
//                                                                            //
//                                  C l e f                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.log.Logger;

import omr.score.common.SystemPoint;
import omr.score.visitor.ScoreVisitor;

/**
 * Class <code>Clef</code> encapsulates a clef.
 *
 * <p>
 * <img src="http://en.wikipedia.org/wiki/File:Middle_C_in_four_clefs.svg" />
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Clef
    extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Clef.class);

    /** A dummy default clef to be used when no current clef is defined */
    private static Clef defaultClef = new Clef(
        null,
        null,
        Shape.G_CLEF,
        null,
        +2,
        null);

    //~ Instance fields --------------------------------------------------------

    /** The underlying glyph */
    private final Glyph glyph;

    /** Precise clef shape, from Clefs range in Shape class */
    private Shape shape;

    /**
     * Step line of the clef : -4 for top line (Baritone), -2 for Bass and Tenor,
     * 0 for Alto, +2 for Treble and Mezzo-Soprano, +4 for bottom line (Soprano).
     */
    private int pitchPosition;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Clef //
    //------//
    /**
     * Create a Clef instance
     *
     * @param measure the containing measure
     * @param staff the assigned staff
     * @param shape precise clef shape
     * @param center center wrt system (in units)
     * @param pitchPosition pitch position
     * @param glyph underlying glyph, if any
     */
    public Clef (Measure     measure,
                 Staff       staff,
                 Shape       shape,
                 SystemPoint center,
                 int         pitchPosition,
                 Glyph       glyph)
    {
        super(measure);

        setStaff(staff);
        this.shape = shape;
        setCenter(center);
        this.pitchPosition = pitchPosition;
        this.glyph = glyph;
    }

    //------//
    // Clef //
    //------//
    /**
     * Create a Clef instance, by cloning another clef
     *
     * @param measure the containing measure
     * @param staff the assigned staff
     * @param other the existing clef to clone
     */
    public Clef (Measure measure,
                 Staff   staff,
                 Clef    other)
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
     * @param clef the provided current clef
     * @param pitchPosition the pitch position of the provided note
     * @return the corresponding note step
     */
    public static Note.Step noteStepOf (Clef clef,
                                        int  pitchPosition)
    {
        if (clef == null) {
            return defaultClef.noteStepOf(pitchPosition);
        } else {
            return clef.noteStepOf(pitchPosition);
        }
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

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // octaveOf //
    //----------//
    /**
     * Report the octave corresponding to a note at the provided pitch position,
     * assuming we are governed by the provided clef, otherwise (if clef is null)
     * we use the default clef (G_CLEF)
     * @param clef the current clef if any
     * @param pitchPosition the pitch position of the note
     * @return the corresponding octave
     */
    public static int octaveOf (Clef clef,
                                int  pitchPosition)
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
     * @param glyph the provided glyph
     * @param measure the containing measure
     * @param staff the containing staff
     * @param center the precise location in the system
     * @return true if Clef was successfully created
     */
    public static boolean populate (Glyph       glyph,
                                    Measure     measure,
                                    Staff       staff,
                                    SystemPoint center)
    {
        Shape shape = glyph.getShape();

        switch (shape) {
        case G_CLEF :
        case G_CLEF_OTTAVA_ALTA :
        case G_CLEF_OTTAVA_BASSA :
            glyph.setTranslation(
                new Clef(measure, staff, shape, center, 2, glyph));

            return true;

        case C_CLEF :

            // Depending on precise clef position, we can have
            // an Alto C-clef (pp=0) or a Tenor C-clef (pp=+2) [or other stuff]
            int pp = (int) Math.rint(staff.pitchPositionOf(center));
            glyph.setTranslation(
                new Clef(measure, staff, shape, center, pp, glyph));

            return true;

        case F_CLEF :
        case F_CLEF_OTTAVA_ALTA :
        case F_CLEF_OTTAVA_BASSA :
            glyph.setTranslation(
                new Clef(measure, staff, shape, center, -2, glyph));

            return true;

        default :
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
          .append((int) Math.rint(pitchPosition));

        if (glyph != null) {
            sb.append(" glyph#")
              .append(glyph.getId());
        }

        sb.append("}");

        return sb.toString();
    }

    //------------//
    // noteStepOf //
    //------------//
    /**
     * Report the note step corresponding to a note at the provided pitch
     * position, assuming we are governed by this clef
     * @param pitchPosition the pitch position of the note
     * @return the corresponding note step
     */
    private Note.Step noteStepOf (int pitchPosition)
    {
        switch (shape) {
        case G_CLEF :
        case G_CLEF_OTTAVA_ALTA :
        case G_CLEF_OTTAVA_BASSA :
            return Note.Step.values()[(71 - pitchPosition) % 7];

        case C_CLEF :

            // Depending on precise clef position, we can have
            // an Alto C-clef (pp=0) or a Tenor C-clef (pp=+2) [or other stuff]
            return Note.Step.values()[(72 - this.pitchPosition - pitchPosition) % 7];

        case F_CLEF :
        case F_CLEF_OTTAVA_ALTA :
        case F_CLEF_OTTAVA_BASSA :
            return Note.Step.values()[(73 - pitchPosition) % 7];

        default :
            logger.severe("No note step defined for " + this);

            return Note.Step.A; // To keep compiler happy
        }
    }

    //----------//
    // octaveOf //
    //----------//
    /**
     * Report the octave corresponding to a note at the provided pitch position,
     * assuming we are governed by this clef
     * @param pitchPosition the pitch position of the note
     * @return the corresponding octave
     */
    private int octaveOf (int pitchPosition)
    {
        switch (shape) {
        case G_CLEF :
            return (34 - pitchPosition) / 7;

        case G_CLEF_OTTAVA_ALTA :
            return ((34 - pitchPosition) / 7) + 1;

        case G_CLEF_OTTAVA_BASSA :
            return ((34 - pitchPosition) / 7) - 1;

        case C_CLEF :

            // Depending on precise clef position, we can have
            // an Alto C-clef (pp=0) or a Tenor C-clef (pp=+2) [or other stuff]
            return (28 - this.pitchPosition - pitchPosition) / 7;

        case F_CLEF :
            return (22 - pitchPosition) / 7;

        case F_CLEF_OTTAVA_ALTA :
            return ((22 - pitchPosition) / 7) + 1;

        case F_CLEF_OTTAVA_BASSA :
            return ((22 - pitchPosition) / 7) - 1;

        default :
            logger.severe("No note octave defined for " + this);

            return 0; // To keep compiler happy
        }
    }
}
