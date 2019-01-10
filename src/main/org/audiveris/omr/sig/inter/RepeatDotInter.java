//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e p e a t D o t I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import java.awt.Rectangle;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omrdataset.api.OmrShape;

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
                           Double pitch)
    {
        super(glyph, null, Shape.REPEAT_DOT, grade, staff, pitch);
    }

    /**
     * Creates a new RepeatDotInter object.
     *
     * @param annotationId id of original annotation if any
     * @param bounds       bounding box
     * @param grade        evaluation value
     * @param staff        the related staff
     * @param pitch        dot pitch
     */
    public RepeatDotInter (int annotationId,
                           Rectangle bounds,
                           double grade,
                           Staff staff,
                           Double pitch)
    {
        super(annotationId, bounds, OmrShape.repeatDot, grade, staff, pitch);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private RepeatDotInter ()
    {
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
