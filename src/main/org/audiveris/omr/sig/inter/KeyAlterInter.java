//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    K e y A l t e r I n t e r                                   //
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
import org.audiveris.omr.sheet.Staff;
import static org.audiveris.omr.sig.inter.AlterInter.computePitch;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code KeyAlterInter} is an Alteration inter, which is part of a key signature.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "key-alter")
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
     * @param staff         the related staff
     * @param pitch         the pitch value WRT staff
     * @param measuredPitch the measured pitch
     */
    public KeyAlterInter (Glyph glyph,
                          Shape shape,
                          double grade,
                          Staff staff,
                          double pitch,
                          double measuredPitch)
    {
        super(glyph, shape, grade, staff, pitch, measuredPitch);
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private KeyAlterInter ()
    {
        super(null, null, 0, null, null, null);
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

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        // Skip AlterInder
        ((AbstractInter) this).added();
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Skip AlterInder
        return isAbnormal();
    }

    //--------//
    // create //
    //--------//
    /**
     * Create an Alter inter, with a grade value, determining pitch WRT provided staff.
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
                                        Staff staff)
    {
        Pitches p = computePitch(glyph, shape, staff);

        return new KeyAlterInter(glyph, shape, grade, staff, p.pitch, p.measuredPitch);
    }
}
