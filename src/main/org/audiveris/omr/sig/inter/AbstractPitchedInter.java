//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A b s t r a c t P i t c h e d I n t e r                            //
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
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.util.Jaxb;

import java.awt.Rectangle;
import java.util.Comparator;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code AbstractPitchedInter} is an abstract Inter class to carry pitch
 * information.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractPitchedInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    /** To order from bottom to top. */
    public static final Comparator<AbstractPitchedInter> bottomUp
            = (AbstractPitchedInter p1, AbstractPitchedInter p2) -> {
        // Pitch comparison is usable only within the SAME staff
        if ((p1.getStaff() != null) && (p2.getStaff() == p1.getStaff())) {
            return Double.compare(p2.pitch, p1.pitch);
        }

        // Resort to ordinate
        return Double.compare(p2.getCenter().y, p1.getCenter().y);
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** The assigned pitch. */
    @XmlAttribute
    @XmlJavaTypeAdapter(Jaxb.Double1Adapter.class)
    protected Double pitch;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractPitchedInter object.
     *
     * @param glyph   the glyph to interpret
     * @param bounds  the precise object bounds (if different from glyph bounds)
     * @param shape   the possible shape
     * @param impacts assignment details
     * @param staff   the related staff
     * @param pitch   the pitch value WRT staff
     */
    public AbstractPitchedInter (Glyph glyph,
                                 Rectangle bounds,
                                 Shape shape,
                                 GradeImpacts impacts,
                                 Staff staff,
                                 Double pitch)
    {
        super(glyph, bounds, shape, impacts);
        this.pitch = pitch;
        setStaff(staff);
    }

    /**
     * Creates a new AbstractPitchedInter object.
     *
     * @param glyph  the glyph to interpret
     * @param bounds the precise object bounds (if different from glyph bounds)
     * @param shape  the possible shape
     * @param grade  the interpretation quality
     * @param staff  the related staff
     * @param pitch  the pitch value WRT staff
     */
    public AbstractPitchedInter (Glyph glyph,
                                 Rectangle bounds,
                                 Shape shape,
                                 Double grade,
                                 Staff staff,
                                 Double pitch)
    {
        super(glyph, bounds, shape, grade);
        this.pitch = pitch;
        setStaff(staff);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected AbstractPitchedInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // getAreaPitchOffset //
    //--------------------//
    /**
     * Report for this inter the pitch center offset with respect to area center.
     * <p>
     * This is 0 by default (assuming inter is symmetric in vertical direction)
     *
     * @return offset of pitch center WRT area center
     */
    public double getAreaPitchOffset ()
    {
        return getAreaPitchOffset(shape);
    }

    //--------------------//
    // getAreaPitchOffset //
    //--------------------//
    /**
     * Report for the provided shape the pitch center offset with respect to area center.
     * <p>
     * This is 0 when shape is symmetrical in vertical direction.
     *
     * @param shape the provided shape
     * @return offset of pitch center WRT area center
     */
    public static double getAreaPitchOffset (Shape shape)
    {
        switch (shape) {
        case G_CLEF:
            return constants.areaPitchOffset_G_CLEF.getValue();

        case G_CLEF_SMALL:
            return constants.areaPitchOffset_G_CLEF_SMALL.getValue();

        case G_CLEF_8VA:
            return constants.areaPitchOffset_G_CLEF_8VA.getValue();

        case G_CLEF_8VB:
            return constants.areaPitchOffset_G_CLEF_8VB.getValue();

        case F_CLEF:
            return constants.areaPitchOffset_F_CLEF.getValue();

        case F_CLEF_SMALL:
            return constants.areaPitchOffset_F_CLEF_SMALL.getValue();

        case F_CLEF_8VA:
            return constants.areaPitchOffset_F_CLEF_8VA.getValue();

        case F_CLEF_8VB:
            return constants.areaPitchOffset_F_CLEF_8VB.getValue();

        case FLAT:
        case DOUBLE_FLAT:
        case KEY_FLAT_7:
        case KEY_FLAT_6:
        case KEY_FLAT_5:
        case KEY_FLAT_4:
        case KEY_FLAT_3:
        case KEY_FLAT_2:
        case KEY_FLAT_1:
            return constants.areaPitchOffset_FLAT.getValue();

        default:
            return 0;
        }
    }

    //-----------------//
    // getIntegerPitch //
    //-----------------//
    /**
     * Report the rounded integer pitch value
     *
     * @return the pitch
     */
    public int getIntegerPitch ()
    {
        return (int) Math.rint(pitch);
    }

    //----------//
    // getPitch //
    //----------//
    /**
     * Report the precise (double) pitch value
     *
     * @return the pitch
     */
    public Double getPitch ()
    {
        return pitch;
    }

    //----------//
    // setPitch //
    //----------//
    /**
     * Set pitch value.
     *
     * @param pitch the pitch to set
     */
    public void setPitch (Double pitch)
    {
        this.pitch = pitch;
    }

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public void setBounds (Rectangle bounds)
    {
        // Bounds
        super.setBounds(bounds);

        // Pitch
        if ((bounds == null) || (staff == null)) {
            setPitch(null);
        } else {
            setPitch(staff.pitchPositionOf(GeoUtil.center2D(bounds)) + getAreaPitchOffset());
        }
    }

    //----------//
    // setStaff //
    //----------//
    @Override
    public void setStaff (Staff staff)
    {
        super.setStaff(staff);

        // Pitch?
        if ((pitch == null) && (staff != null) && (bounds != null) && (shape != null)) {
            setPitch(staff.pitchPositionOf(GeoUtil.center2D(bounds)) + getAreaPitchOffset());
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + String.format(" p:%.1f", pitch);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Double areaPitchOffset_FLAT = new Constant.Double(
                "pitch",
                1.2,
                "Pitch offset WRT area center for FLAT");

        private final Constant.Double areaPitchOffset_G_CLEF = new Constant.Double(
                "pitch",
                1.8,
                "Pitch offset WRT area center for G_CLEF");

        private final Constant.Double areaPitchOffset_G_CLEF_SMALL = new Constant.Double(
                "pitch",
                1.2,
                "Pitch offset WRT area center for G_CLEF_SMALL");

        private final Constant.Double areaPitchOffset_G_CLEF_8VA = new Constant.Double(
                "pitch",
                2.9,
                "Pitch offset WRT area center for G_CLEF_8VA");

        private final Constant.Double areaPitchOffset_G_CLEF_8VB = new Constant.Double(
                "pitch",
                0.5,
                "Pitch offset WRT area center for G_CLEF_8VB");

        private final Constant.Double areaPitchOffset_F_CLEF = new Constant.Double(
                "pitch",
                -1.3,
                "Pitch offset WRT area center for F_CLEF");

        private final Constant.Double areaPitchOffset_F_CLEF_SMALL = new Constant.Double(
                "pitch",
                -0.8,
                "Pitch offset WRT area center for F_CLEF_SMALL");

        private final Constant.Double areaPitchOffset_F_CLEF_8VA = new Constant.Double(
                "pitch",
                0,
                "Pitch offset WRT area center for F_CLEF_8VA");

        private final Constant.Double areaPitchOffset_F_CLEF_8VB = new Constant.Double(
                "pitch",
                -2.5,
                "Pitch offset WRT area center for F_CLEF_8VB");
    }
}
