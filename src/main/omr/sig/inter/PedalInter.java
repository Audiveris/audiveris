//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P e d a l I n t e r                                      //
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

import omr.glyph.Glyph;
import omr.glyph.Shape;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code PedalInter} represents a pedal (start) or pedal up (stop) event
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "pedal")
public class PedalInter
        extends AbstractDirectionInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code PedalInter} object.
     *
     * @param glyph the pedal glyph
     * @param shape PEDAL_MARK (start) or PEDAL_UP_MARK (stop)
     * @param grade the interpretation quality
     */
    public PedalInter (Glyph glyph,
                       Shape shape,
                       double grade)
    {
        super(glyph, glyph.getBounds(), shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    public PedalInter ()
    {
        super(null, null, null, 0);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }
}
