//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  T i m e N u m b e r I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sig.inter;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.sheet.Staff;

import omr.util.VerticalSide;

import java.awt.Point;
import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code TimeNumberInter} represents a top or bottom number in a time signature.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "time-number")
public class TimeNumberInter
        extends AbstractNumberInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Top or bottom. */
    @XmlAttribute
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
     * @param bounds bounding box of the number
     * @param shape  precise shape
     * @param grade  evaluation value
     * @param value  number value
     * @param side   top or bottom
     */
    public TimeNumberInter (Rectangle bounds,
                            Shape shape,
                            double grade,
                            int value,
                            VerticalSide side)
    {
        super(bounds, shape, grade, value);
        this.side = side;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private TimeNumberInter ()
    {
        super((Glyph) null, null, 0, 0);
        this.side = null;
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

        TimeNumberInter inter = new TimeNumberInter(glyph, shape, grade, value, side);
        inter.setStaff(staff);

        return inter;
    }

    //---------//
    // getSide //
    //---------//
    public VerticalSide getSide ()
    {
        return side;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
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
