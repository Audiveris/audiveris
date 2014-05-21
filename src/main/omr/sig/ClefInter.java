//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C l e f I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

/**
 * Class {@code ClefInter} handles a Clef interpretation.
 * <p>
 * The following image, directly pulled from wikipedia, explains the most popular clefs today
 * (Treble, Alto, Tenor and Bass) and for each presents where the "Middle C" note (C4) would take
 * place.
 * These informations are used by methods {@link #octaveOf(omr.score.entity.Clef, int)} and
 * {@link #noteStepOf(omr.score.entity.Clef, int)}.</p>
 * <p>
 * <img
 * src="http://upload.wikimedia.org/wikipedia/commons/thumb/1/17/Middle_C_in_four_clefs.svg/600px-Middle_C_in_four_clefs.svg.png"
 * />
 *
 * @author Hervé Bitteur
 */
public class ClefInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ClefInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** A dummy default clef to be used when no current clef is defined */
    ///private static Clef defaultClef = new Clef(null, null, Shape.G_CLEF, null, +2, null);
    /**
     * Step line of the clef : -4 for top line (Baritone), -2 for Bass and Tenor,
     * 0 for Alto, +2 for Treble and Mezzo-Soprano, +4 for bottom line (Soprano).
     */
    private final int pitchPosition;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ClefInter object.
     *
     * @param glyph         the glyph to interpret
     * @param shape         the possible shape
     * @param grade         the interpretation quality
     * @param pitchPosition pitch position
     */
    public ClefInter (Glyph glyph,
                      Shape shape,
                      double grade,
                      int pitchPosition)
    {
        super(glyph, null, shape, grade);
        this.pitchPosition = pitchPosition;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a Clef inter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance or null if failed
     */
    public static Inter create (Glyph glyph,
                                Shape shape,
                                double grade,
                                StaffInfo staff)
    {
        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
            return new ClefInter(glyph, shape, grade, 2);

        case C_CLEF:

            // Depending on precise clef position, we can have
            // an Alto C-clef (pp=0) or a Tenor C-clef (pp=+2) [or other stuff]
            Point center = glyph.getLocation();
            int pp = (int) Math.rint(staff.pitchPositionOf(center));

            return new ClefInter(glyph, shape, grade, pp);

        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
            return new ClefInter(glyph, shape, grade, -2);

        case PERCUSSION_CLEF:
            return new ClefInter(glyph, shape, grade, 0);

        default:
            return null;
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
}
