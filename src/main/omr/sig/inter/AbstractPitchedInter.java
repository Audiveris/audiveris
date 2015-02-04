//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A b s t r a c t P i t c h e d I n t e r                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.sheet.Staff;

import omr.sig.GradeImpacts;

import java.awt.Rectangle;

/**
 * Class {@code AbstractPitchedInter} is an abstract Inter class to carry pitch
 * information.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractPitchedInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The assigned pitch. */
    protected int pitch;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractPitchedInter object.
     *
     * @param glyph   the glyph to interpret
     * @param box     the precise object bounds (if different from glyph bounds)
     * @param shape   the possible shape
     * @param impacts assignment details
     * @param staff   the related staff
     * @param pitch   the pitch value WRT staff
     */
    public AbstractPitchedInter (Glyph glyph,
                                 Rectangle box,
                                 Shape shape,
                                 GradeImpacts impacts,
                                 Staff staff,
                                 int pitch)
    {
        super(glyph, box, shape, impacts);
        setStaff(staff);
        this.pitch = pitch;
    }

    /**
     * Creates a new AbstractPitchedInter object.
     *
     * @param glyph the glyph to interpret
     * @param box   the precise object bounds (if different from glyph bounds)
     * @param shape the possible shape
     * @param grade the interpretation quality
     * @param staff the related staff
     * @param pitch the pitch value WRT staff
     */
    public AbstractPitchedInter (Glyph glyph,
                                 Rectangle box,
                                 Shape shape,
                                 double grade,
                                 Staff staff,
                                 int pitch)
    {
        super(glyph, box, shape, grade);
        setStaff(staff);
        this.pitch = pitch;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getPitch //
    //----------//
    /**
     * @return the pitch
     */
    public int getPitch ()
    {
        return pitch;
    }

    //----------//
    // setPitch //
    //----------//
    /**
     * @param pitch the pitch to set
     */
    public void setPitch (int pitch)
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
