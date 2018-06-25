//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A b s t r a c t P i t c h e d I n t e r                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omrdataset.api.OmrShape;

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

    /** To order from bottom to top. */
    public static Comparator<AbstractPitchedInter> bottomUp = new Comparator<AbstractPitchedInter>()
    {
        @Override
        public int compare (AbstractPitchedInter p1,
                            AbstractPitchedInter p2)
        {
            // Pitch comparison is usable only within the SAME staff
            if ((p1.getStaff() != null) && (p2.getStaff() == p1.getStaff())) {
                return Double.compare(p2.pitch, p1.pitch);
            }

            // Resort to ordinate
            return Double.compare(p2.getCenter().y, p1.getCenter().y);
        }
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
                                 double grade,
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
     * @param bounds   the object bounds
     * @param omrShape the OMR shape
     * @param grade    the interpretation quality
     * @param staff    the related staff
     * @param pitch    the pitch value WRT staff
     */
    public AbstractPitchedInter (Rectangle bounds,
                                 OmrShape omrShape,
                                 double grade,
                                 Staff staff,
                                 Double pitch)
    {
        super(bounds, omrShape, grade);
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
    // setBounds //
    //-----------//
    @Override
    public void setBounds (Rectangle bounds)
    {
        super.setBounds(bounds);

        if ((pitch == null) && (staff != null)) {
            setPitch(staff.pitchPositionOf(GeoUtil.centerOf(bounds)));
        }
    }

    //----------//
    // setPitch //
    //----------//
    /**
     * @param pitch the pitch to set
     */
    public void setPitch (double pitch)
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

        if ((pitch == null) && (staff != null) && (bounds != null)) {
            setPitch(staff.pitchPositionOf(GeoUtil.centerOf(bounds)));
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + String.format(" p:%.2f", pitch);
    }
}
