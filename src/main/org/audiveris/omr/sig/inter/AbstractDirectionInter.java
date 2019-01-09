//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           A b s t r a c t D i r e c t i o n I n t e r                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omrdataset.api.OmrShape;

import java.awt.Rectangle;

/**
 * Class {@code AbstractDirectionInter} represents any direction.
 * Coda, text, pedal, segno, wedge.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractDirectionInter
        extends AbstractInter
{

    /**
     * Creates a new {@code AbstractDirectionInter} object.
     *
     * @param glyph the glyph to interpret
     * @param box   the precise object bounds (if different from glyph bounds)
     * @param shape the possible shape
     * @param grade the interpretation quality
     */
    public AbstractDirectionInter (Glyph glyph,
                                   Rectangle box,
                                   Shape shape,
                                   double grade)
    {
        super(glyph, box, shape, grade);
    }

    /**
     * Creates a new {@code AbstractDirectionInter} object.
     *
     * @param annotationId ID of the original annotation if any
     * @param bounds       bounding box
     * @param omrShape     precise shape
     * @param grade        the interpretation quality
     */
    public AbstractDirectionInter (int annotationId,
                                   Rectangle bounds,
                                   OmrShape omrShape,
                                   double grade)
    {
        super(annotationId, bounds, omrShape, grade);
    }

    /**
     * Creates a new {@code AbstractDirectionInter} object.
     *
     * @param glyph   the glyph to interpret
     * @param box     the precise object bounds (if different from glyph bounds)
     * @param shape   the possible shape
     * @param impacts assignment details
     */
    public AbstractDirectionInter (Glyph glyph,
                                   Rectangle box,
                                   Shape shape,
                                   GradeImpacts impacts)
    {
        super(glyph, box, shape, impacts);
    }
}
