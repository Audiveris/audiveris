//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S m a l l F l a g I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
import org.audiveris.omr.sig.relation.FlagStemRelation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>SmallFlagInter</code> is a flag for grace note (with or without slash).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-flag")
public class SmallFlagInter
        extends AbstractFlagInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor meant for JAXB.
     */
    private SmallFlagInter ()
    {
    }

    /**
     * Creates a new SmallFlagInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public SmallFlagInter (Glyph glyph,
                           Shape shape,
                           Double grade)
    {
        super(glyph, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        setAbnormal(true); // No stem linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if flag is connected to a stem
        setAbnormal(!sig.hasRelation(this, FlagStemRelation.class));

        return isAbnormal();
    }
}
