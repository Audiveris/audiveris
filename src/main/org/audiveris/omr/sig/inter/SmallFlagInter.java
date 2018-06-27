//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S m a l l F l a g I n t e r                                  //
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

import java.awt.Rectangle;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;

import javax.xml.bind.annotation.XmlRootElement;
import org.audiveris.omrdataset.api.OmrShape;

/**
 * Class {@code SmallFlagInter} is a flag for grace note (with or without slash).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-flag")
public class SmallFlagInter
        extends AbstractFlagInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SmallFlagInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public SmallFlagInter (Glyph glyph,
                           Shape shape,
                           double grade)
    {
        super(glyph, shape, grade);
    }

    /**
     * Creates a new SmallFlagInter object.
     *
     * @param annotationId ID of original annotation if any
     * @param bounds       bounding box
     * @param omrShape     flag shape
     * @param grade        evaluation value
     */
    public SmallFlagInter (int annotationId,
                           Rectangle bounds,
                           OmrShape omrShape,
                           double grade)
    {
        super(annotationId, bounds, omrShape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private SmallFlagInter ()
    {
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
}
