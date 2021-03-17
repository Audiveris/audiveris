//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  T i m e N u m b e r I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.ui.HorizontalEditor;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code TimeNumberInter} represents a top or bottom number
 * (gathering one or several digits) in a time signature.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "time-number")
public class TimeNumberInter
        extends AbstractNumberInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TimeNumberInter.class);

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Top or bottom. */
    @XmlAttribute
    protected VerticalSide side;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TimeNumberInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param side  top or bottom
     */
    public TimeNumberInter (Glyph glyph,
                            Shape shape,
                            Double grade,
                            VerticalSide side)
    {
        super(glyph, shape, grade);

        this.side = side;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private TimeNumberInter ()
    {
        super((Glyph) null, null, 0.0);
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

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new HorizontalEditor(this);
    }

    //---------//
    // getSide //
    //---------//
    /**
     * Report vertical position with respect to the staff time signature.
     *
     * @return TOP or BOTTOM
     */
    public VerticalSide getSide ()
    {
        return side;
    }

    //---------//
    // setSide //
    //---------//
    /**
     * Set the vertical position with respect to the staff time signature.
     *
     * @param side the side to set
     */
    public void setSide (VerticalSide side)
    {
        this.side = side;
    }

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

        TimeNumberInter inter = new TimeNumberInter(glyph, shape, grade, side);
        inter.setStaff(staff);

        return inter;
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "TIME_" + getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

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
