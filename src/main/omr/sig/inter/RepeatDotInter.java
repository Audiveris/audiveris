//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e p e a t D o t I n t e r                                  //
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

import omr.sheet.Staff;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code RepeatDotInter} represents a repeat dot, near a bar line.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "repeat-dot")
public class RepeatDotInter
        extends AbstractPitchedInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new RepeatDotInter object.
     *
     * @param glyph underlying glyph
     * @param grade evaluation value
     * @param staff the related staff
     * @param pitch dot pitch
     */
    public RepeatDotInter (Glyph glyph,
                           double grade,
                           Staff staff,
                           int pitch)
    {
        super(glyph, null, Shape.REPEAT_DOT, grade, staff, pitch);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private RepeatDotInter ()
    {
    }
}
