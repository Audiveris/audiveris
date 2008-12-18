//----------------------------------------------------------------------------//
//                                                                            //
//                                  C l e f                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.common.SystemPoint;
import omr.score.visitor.ScoreVisitor;

import omr.log.Logger;

/**
 * Class <code>Clef</code> encapsulates a clef.
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

    //~ Instance fields --------------------------------------------------------

    /** The underlying glyph */
    private final Glyph glyph;

    /** Precise clef shape, from Clefs range in Shape class */
    private Shape shape;

    /**
     * Step line of the clef : -4 for top line (Baritone), -2 for Bass, 0 for
     * Alto, +2 for Treble and Mezzo-Soprano, +4 for bottom line (Soprano).
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
    public static Note.Step noteStepOf (int   pitchPosition,
                                        Shape clefShape)
    {
        switch (clefShape) {
        case G_CLEF :
        case G_CLEF_OTTAVA_ALTA :
        case G_CLEF_OTTAVA_BASSA :
            return Note.Step.values()[(71 - pitchPosition) % 7];

        case F_CLEF :
        case F_CLEF_OTTAVA_ALTA :
        case F_CLEF_OTTAVA_BASSA :
            return Note.Step.values()[(73 - pitchPosition) % 7];

        default :
            logger.severe("No note step defined for clef shape " + clefShape);

            return Note.Step.A; // To keep compiler happy
        }
    }

    //----------//
    // octaveOf //
    //----------//
    public static int octaveOf (int   pitchPosition,
                                Shape clefShape)
    {
        switch (clefShape) {
        case G_CLEF :
            return (34 - pitchPosition) / 7;

        case G_CLEF_OTTAVA_ALTA :
            return ((34 - pitchPosition) / 7) + 1;

        case G_CLEF_OTTAVA_BASSA :
            return ((34 - pitchPosition) / 7) - 1;

        case F_CLEF :
            return (22 - pitchPosition) / 7;

        case F_CLEF_OTTAVA_ALTA :
            return ((22 - pitchPosition) / 7) + 1;

        case F_CLEF_OTTAVA_BASSA :
            return ((22 - pitchPosition) / 7) - 1;

        default :
            logger.severe("No note octave defined for clef shape " + clefShape);

            return 0; // To keep compiler happy
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
     * Report the vertical position within the staff
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

    //----------//
    // populate //
    //----------//
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
}
