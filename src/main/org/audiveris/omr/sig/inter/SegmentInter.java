//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S e g m e n t I n t e r                                    //
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
import org.audiveris.omr.sheet.curve.SegmentInfo;
import org.audiveris.omr.sig.GradeImpacts;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>SegmentInter</code> represents a line segment (used in wedge or ending).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "segment")
public class SegmentInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final SegmentInfo info;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor meant for JAXB.
     */
    private SegmentInter ()
    {
        this.info = null;
    }

    /**
     * Creates a new SegmentInter object.
     *
     * @param info    segment information
     * @param impacts assignment details
     */
    public SegmentInter (SegmentInfo info,
                         GradeImpacts impacts)
    {
        super(null, info.getBounds(), Shape.SEGMENT, impacts);

        this.info = info;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // getInfo //
    //---------//
    /**
     * Report the related building information.
     *
     * @return segment building info
     */
    public SegmentInfo getInfo ()
    {
        return info;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends GradeImpacts
    {

        private static final String[] NAMES = new String[]
        { "dist" };

        private static final double[] WEIGHTS = new double[]
        { 1 };

        public Impacts (double dist)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, dist);
        }
    }
}
