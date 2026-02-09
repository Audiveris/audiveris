//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A b s t r a c t P i t c h e d I n t e r                            //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.ui.symbol.BravuraSymbols;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>AbstractPitchedInter</code> is an abstract Inter class to carry pitch
 * information.
 * <p>
 * Pitch position is counted from 0 on staff mid line and increases in top down direction,
 * at line, inter-line, line, inter-line, etc.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractPitchedInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AbstractPitchedInter.class);

    /** To order from bottom to top. */
    public static final Comparator<AbstractPitchedInter> bottomUp = (AbstractPitchedInter p1,
                                                                     AbstractPitchedInter p2) -> {
        // Pitch comparison is usable only within the SAME staff
        if ((p1.getStaff() != null) && (p2.getStaff() == p1.getStaff())) {
            return Double.compare(p2.pitch, p1.pitch);
        }

        // Resort to ordinate
        return Double.compare(p2.getCenter().y, p1.getCenter().y);
    };

    /** Vertical pitch offset from symbol center to focus line. */
    private static final Map<Shape, Double> pitchOffsets = new TreeMap<>();

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * The assigned pitch coded as a double value, 1 digit maximum after the dot.
     */
    @XmlAttribute
    @XmlJavaTypeAdapter(Jaxb.Double1Adapter.class)
    protected Double pitch;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    protected AbstractPitchedInter ()
    {
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

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + String.format(" pitch:%.1f", pitch);
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
        if ((bounds == null) || (staff == null) || staff.isTablature()) {
            setPitch(null);
        } else {
            setPitch(staff.pitchPositionOf(GeoUtil.center2D(bounds)) + getAreaPitchOffset());
        }
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

    //----------//
    // setStaff //
    //----------//
    @Override
    public void setStaff (Staff staff)
    {
        super.setStaff(staff);

        // Pitch?
        if ((pitch == null) && (staff != null) && !staff.isTablature() && (bounds != null)
                && (shape != null)) {
            setPitch(staff.pitchPositionOf(GeoUtil.center2D(bounds)) + getAreaPitchOffset());
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------------------//
    // getAreaPitchOffset //
    //--------------------//
    /**
     * Report the pitch offset of the shape focus line with respect to the area center.
     * <p>
     * This is 0 when shape is symmetrical in vertical direction.
     *
     * @param shape the provided shape
     * @return pitch offset of the focus line WRT area center
     */
    public static double getAreaPitchOffset (Shape shape)
    {
        return switch (shape) {
            case //
                    G_CLEF, G_CLEF_SMALL, G_CLEF_8VA, G_CLEF_8VB, //
                    F_CLEF, F_CLEF_SMALL, F_CLEF_8VA, F_CLEF_8VB -> pitchOffsets.get(shape);
            case FLAT, DOUBLE_FLAT, //
                    KEY_FLAT_7, KEY_FLAT_6, KEY_FLAT_5, //
                    KEY_FLAT_4, KEY_FLAT_3, KEY_FLAT_2, KEY_FLAT_1 -> pitchOffsets.get(Shape.FLAT);
            default -> 0;
        };
    }

    //----------------------//
    // populatePitchOffsets //
    //----------------------//
    /**
     * Populate the 'pitchOffsets' map.
     */
    private static void populatePitchOffsets ()
    {
        final MusicFamily musicFamily = MusicFamily.Bravura;
        final MusicFont font = MusicFont.getMusicFont(musicFamily, 200); // Arbitrary size

        // Retrieve pitch height from 5-line and 1-line symbols
        final double staffHeight = font.layout(MusicFont.getString(BravuraSymbols.STAFF_CODE))
                .getBounds().getHeight();
        final double lineHeight = font.layout(MusicFont.getString(BravuraSymbols.LINE_CODE))
                .getBounds().getHeight();
        final double pitchHeight = (staffHeight - lineHeight) / 8; // 4 interlines = 8 pitches

        // For each shape, retrieve delta pitch from area center to focus line (at ordinate zero)
        Arrays.asList(
                Shape.G_CLEF,
                Shape.G_CLEF_SMALL,
                Shape.G_CLEF_8VA,
                Shape.G_CLEF_8VB,
                Shape.F_CLEF,
                Shape.F_CLEF_SMALL,
                Shape.F_CLEF_8VA,
                Shape.F_CLEF_8VB,
                Shape.FLAT)//
                .forEach(s -> {
                    final TextLayout layout = font.layoutShapeByCode(s);
                    final Rectangle2D rect = layout.getBounds();
                    final double offset = (-rect.getY() - rect.getHeight() / 2) / pitchHeight;
                    pitchOffsets.put(s, offset);
                });
    }

    static {
        populatePitchOffsets();

        if (constants.printPitchOffsets.isSet()) {
            System.out.println("pitchOffsets:");
            pitchOffsets.entrySet().forEach(
                    e -> System.out.printf("%-12s : %5.2f%n", e.getKey(), e.getValue()));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean printPitchOffsets = new Constant.Boolean(
                false,
                "Should we print all pitch offsets");
    }
}
