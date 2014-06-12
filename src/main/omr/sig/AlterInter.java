//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       A l t e r I n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.util.WrappedDouble;

import java.awt.Point;

/**
 * Class {@code AlterInter} represents an alteration (sharp, flat, natural,
 * double-sharp, double-flat).
 * It can be an accidental alteration or a part of a key signature.
 *
 * @author Hervé Bitteur
 */
public class AlterInter
        extends AbstractPitchedInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Measured pitch value. */
    private final double measuredPitch;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AlterInter object.
     *
     * @param glyph         underlying glyph
     * @param shape         precise shape
     * @param grade         evaluation value
     * @param pitch         the pitch value WRT staff
     * @param measuredPitch the measured pitch
     */
    public AlterInter (Glyph glyph,
                       Shape shape,
                       double grade,
                       int pitch,
                       double measuredPitch)
    {
        super(glyph, null, shape, grade, pitch);
        this.measuredPitch = measuredPitch;
    }

    /**
     * Creates a new AlterlInter object.
     *
     * @param glyph         underlying glyph
     * @param shape         precise shape
     * @param impacts       assignment details
     * @param pitch         the pitch value WRT staff
     * @param measuredPitch the measured pitch
     */
    public AlterInter (Glyph glyph,
                       Shape shape,
                       GradeImpacts impacts,
                       int pitch,
                       double measuredPitch)
    {
        super(glyph, null, shape, impacts, pitch);
        this.measuredPitch = measuredPitch;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //--------//
    // create //
    //--------//
    /**
     * Create an Alter inter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance or null if failed
     */
    public static AlterInter create (Glyph glyph,
                                     Shape shape,
                                     double grade,
                                     StaffInfo staff)
    {
        Pitches pitches = computePitch(glyph, shape, staff);

        return new AlterInter(glyph, shape, grade, pitches.pitch, pitches.measuredPitch);
    }

    //--------//
    // create //
    //--------//
    /**
     * Create an Alter inter.
     *
     * @param glyph   underlying glyph
     * @param shape   precise shape
     * @param impacts assignment details
     * @param staff   related staff
     * @return the created instance or null if failed
     */
    public static AlterInter create (Glyph glyph,
                                     Shape shape,
                                     GradeImpacts impacts,
                                     StaffInfo staff)
    {
        Pitches pitches = computePitch(glyph, shape, staff);

        return new AlterInter(glyph, shape, impacts, pitches.pitch, pitches.measuredPitch);
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        return super.getDetails() + String.format(" mPitch:%.1f", measuredPitch);
    }

    //--------------------//
    // getFlatPitchOffset //
    //--------------------//
    public static double getFlatPitchOffset ()
    {
        return constants.flatPitchOffset.getValue();
    }

    /**
     * @return the measuredPitch
     */
    public Double getMeasuredPitch ()
    {
        return measuredPitch;
    }

    //--------------//
    // computePitch //
    //--------------//
    /**
     * Compute pitch and measuredPitch values according to glyph centroid and shape.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param staff related staff
     * @return the pitch values (assigned, measured)
     */
    protected static Pitches computePitch (Glyph glyph,
                                           Shape shape,
                                           StaffInfo staff)
    {
        // Determine pitch according to shape and glyph centroid
        Point centroid = glyph.getCentroid();
        double measuredPitch = staff.pitchPositionOf(centroid);

        // Pitch offset for flat-based alterations
        if ((shape == Shape.FLAT) || (shape == Shape.DOUBLE_FLAT)) {
            measuredPitch += constants.flatPitchOffset.getValue();
        }

        return new Pitches((int) Math.rint(measuredPitch), measuredPitch);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Pitches //
    //---------//
    protected static class Pitches
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final int pitch;

        public final double measuredPitch;

        //~ Constructors ---------------------------------------------------------------------------
        public Pitches (int pitch,
                        double measuredPitch)
        {
            this.pitch = pitch;
            this.measuredPitch = measuredPitch;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Double flatPitchOffset = new Constant.Double(
                "pitch",
                0.75,
                "Pitch offset of flat WRT centroid-based pitch");
    }
}
