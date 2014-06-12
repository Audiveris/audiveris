//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    K e y A l t e r I n t e r                                   //
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
import static omr.sig.AlterInter.computePitch;

/**
 * Class {@code KeyAlterInter} is an Alteration inter, which is part of a key signature.
 *
 * @author Hervé Bitteur
 */
public class KeyAlterInter
        extends AlterInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new KeyAlterInter object.
     *
     * @param glyph         underlying glyph
     * @param shape         precise shape
     * @param grade         evaluation value
     * @param pitch         the pitch value WRT staff
     * @param measuredPitch the measured pitch
     */
    public KeyAlterInter (Glyph glyph,
                          Shape shape,
                          double grade,
                          int pitch,
                          double measuredPitch)
    {
        super(glyph, shape, grade, pitch, measuredPitch);
    }

    /**
     * Creates a new KeyAlterInter object.
     *
     * @param glyph         underlying glyph
     * @param shape         precise shape
     * @param impacts       assignment details
     * @param pitch         the pitch value WRT staff
     * @param measuredPitch the measured pitch
     */
    public KeyAlterInter (Glyph glyph,
                          Shape shape,
                          GradeImpacts impacts,
                          int pitch,
                          double measuredPitch)
    {
        super(glyph, shape, impacts, pitch, measuredPitch);
    }

    //~ Methods ------------------------------------------------------------------------------------
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
    public static KeyAlterInter create (Glyph glyph,
                                        Shape shape,
                                        double grade,
                                        StaffInfo staff)
    {
        Pitches pitches = computePitch(glyph, shape, staff);

        return new KeyAlterInter(glyph, shape, grade, pitches.pitch, pitches.measuredPitch);
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
    public static KeyAlterInter create (Glyph glyph,
                                        Shape shape,
                                        GradeImpacts impacts,
                                        StaffInfo staff)
    {
        Pitches pitches = computePitch(glyph, shape, staff);

        return new KeyAlterInter(glyph, shape, impacts, pitches.pitch, pitches.measuredPitch);
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }
}
