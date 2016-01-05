//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A b s t r a c t P i t c h e d I n t e r                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.sheet.Staff;

import omr.sig.GradeImpacts;

import omr.util.Jaxb;

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
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double1Adapter.class)
    protected double pitch;

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
                                 double pitch)
    {
        super(glyph, bounds, shape, impacts);
        setStaff(staff);
        this.pitch = pitch;
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
                                 double pitch)
    {
        super(glyph, bounds, shape, grade);
        setStaff(staff);
        this.pitch = pitch;
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
    public double getPitch ()
    {
        return pitch;
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

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " p:" + pitch;
    }
}
