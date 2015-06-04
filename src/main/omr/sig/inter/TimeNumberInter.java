//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  T i m e N u m b e r I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.sheet.Staff;

import omr.util.VerticalSide;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code TimeNumberInter} represents a top or bottom number in a time signature.
 *
 * @author Hervé Bitteur
 */
public class TimeNumberInter
        extends AbstractNumberInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Top or bottom. */
    protected final VerticalSide side;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TimeNumberInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param value number value
     * @param side  top or bottom
     */
    public TimeNumberInter (Glyph glyph,
                            Shape shape,
                            double grade,
                            int value,
                            VerticalSide side)
    {
        super(glyph, shape, grade, value);
        this.side = side;
    }

    /**
     * Creates a new TimeNumberInter object.
     *
     * @param box   bounding box of the number
     * @param shape precise shape
     * @param grade evaluation value
     * @param value number value
     * @param side  top or bottom
     */
    public TimeNumberInter (Rectangle box,
                            Shape shape,
                            double grade,
                            int value,
                            VerticalSide side)
    {
        super(box, shape, grade, value);
        this.side = side;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * (Try to) create a top or bottom number for time signature.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance or null if failed
     */
    public static TimeNumberInter create (Glyph glyph,
                                          Shape shape,
                                          double grade,
                                          Staff staff)
    {
        // Check pitch of item
        Point centroid = glyph.getCentroid();
        double pitch = staff.pitchPositionOf(centroid);
        double absPitch = Math.abs(pitch);

        if ((absPitch < constants.minAbsolutePitch.getValue())
            || (absPitch > constants.maxAbsolutePitch.getValue())) {
            return null;
        }

        VerticalSide side = (pitch < 0) ? VerticalSide.TOP : VerticalSide.BOTTOM;

        // Check value
        int value = valueOf(shape);

        if (value == -1) {
            return null;
        }

        return new TimeNumberInter(glyph, shape, grade, value, side);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Double minAbsolutePitch = new Constant.Double(
                "pitch",
                1.0,
                "Minimum absolute pitch value for a time signature number");

        private final Constant.Double maxAbsolutePitch = new Constant.Double(
                "pitch",
                3.0,
                "Maximum absolute pitch value for a time signature number");
    }
}
