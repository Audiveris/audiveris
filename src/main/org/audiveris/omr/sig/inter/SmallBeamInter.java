//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S m a l l B e a m I n t e r                                  //
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

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.GradeImpacts;

import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>SmallBeamInter</code> represents a small (cue) beam, with a height smaller than
 * standard beams.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-beam")
public class SmallBeamInter
        extends AbstractBeamInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor meant for JAXB.
     */
    @SuppressWarnings("unused")
    private SmallBeamInter ()
    {
        super((Shape) null, (GradeImpacts) null, null, 0);
    }

    /**
     * Creates manually a new SmallBeamInter ghost object.
     *
     * @param grade quality grade
     */
    public SmallBeamInter (Double grade)
    {
        super(Shape.BEAM_SMALL, grade);
    }

    /**
     * Creates a new SmallBeamInter object.
     *
     * @param impacts the grade details
     * @param median  median beam line
     * @param height  beam height
     */
    public SmallBeamInter (GradeImpacts impacts,
                           Line2D median,
                           double height)
    {
        super(Shape.BEAM_SMALL, impacts, median, height);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // isSmall //
    //---------//
    @Override
    public boolean isSmall ()
    {
        return true;
    }
}
