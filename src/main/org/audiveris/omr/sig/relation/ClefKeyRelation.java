//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  C l e f K e y R e l a t i o n                                 //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sig.inter.Inter;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ClefKeyRelation} represents a support relation between a clef and a
 * compatible key signature.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "clef-key")
public class ClefKeyRelation
        extends AbstractSupport
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ClefKeyRelation} object.
     */
    public ClefKeyRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------------//
    // maxContributionForClef //
    //------------------------//
    /**
     * Report the maximum contribution for the clef, brought by the following key.
     *
     * @return maximum contribution a clef can expect (from the following key)
     */
    public static double maxContributionForClef ()
    {
        // Maximum key grade value is Inter.intrinsicRatio
        return Inter.intrinsicRatio * constants.clefSupportCoeff.getValue();
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        return constants.clefSupportCoeff.getValue();
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return constants.keySupportCoeff.getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio clefSupportCoeff = new Constant.Ratio(
                5,
                "Value for (source) clef coeff in support formula");

        private final Constant.Ratio keySupportCoeff = new Constant.Ratio(
                5,
                "Value for (target) key coeff in support formula");
    }
}
