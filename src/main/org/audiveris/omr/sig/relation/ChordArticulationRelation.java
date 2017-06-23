//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                        C h o r d A r t i c u l a t i o n R e l a t i o n                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import javax.xml.bind.annotation.XmlRootElement;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ChordArticulationRelation} is a connection between a head-based chord
 * and an articulation sign (tenuto, accent, staccato, staccatissimo, mercato).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-articulation")
public class ChordArticulationRelation
        extends AbstractConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ChordArticulationRelation.class);

    private static final double[] WEIGHTS = new double[]{
        constants.xWeight.getValue(),
        constants.yWeight.getValue()
    };

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // getXInGapMaximum //
    //------------------//
    public static Scale.Fraction getInGapMaximum ()
    {
        return constants.xGapMax;
    }

    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum ()
    {
        return constants.xGapMax;
    }

    //----------------//
    // getYGapMaximum //
    //----------------//
    public static Scale.Fraction getYGapMaximum ()
    {
        return constants.yGapMax;
    }

    //--------------//
    // getInWeights //
    //--------------//
    @Override
    protected double[] getInWeights ()
    {
        return WEIGHTS;
    }

    //---------------//
    // getOutWeights //
    //---------------//
    @Override
    protected double[] getOutWeights ()
    {
        return WEIGHTS;
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    /**
     * @return the supporting coefficient for (target) articulation
     */
    @Override
    protected double getTargetCoeff ()
    {
        return constants.articulationSupportCoeff.getValue();
    }

    //---------------//
    // getXOutGapMax //
    //---------------//
    @Override
    protected Scale.Fraction getXOutGapMax ()
    {
        return getXOutGapMaximum();
    }

    //------------//
    // getYGapMax //
    //------------//
    @Override
    protected Scale.Fraction getYGapMax ()
    {
        return getYGapMaximum();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio articulationSupportCoeff = new Constant.Ratio(
                3,
                "Supporting coeff for (target) articulation");

        private final Scale.Fraction xGapMax = new Scale.Fraction(
                0.75,
                "Maximum horizontal gap between articulation center & chord");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                1.5,
                "Maximum vertical gap between articulation center & chord");

        private final Constant.Ratio xWeight = new Constant.Ratio(
                3,
                "Relative impact weight for xGap (in or out)");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                1,
                "Relative impact weight for yGap");
    }
}
