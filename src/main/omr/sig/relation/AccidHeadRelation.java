//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A c c i d H e a d R e l a t i o n                               //
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
package omr.sig.relation;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AccidHeadRelation} represents the relation support between an accidental
 * alteration (sharp, flat, natural, double-sharp, double-flat) and a note head.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "accid-head")
public class AccidHeadRelation
        extends AbstractConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AccidHeadRelation.class);

    private static final double[] IN_WEIGHTS = new double[]{
        constants.xInWeight.getValue(),
        constants.yWeight.getValue()
    };

    private static final double[] OUT_WEIGHTS = new double[]{
        constants.xOutWeight.getValue(),
        constants.yWeight.getValue()
    };

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // getXInGapMaximum //
    //------------------//
    public static Scale.Fraction getXInGapMaximum ()
    {
        return constants.xInGapMax;
    }

    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum ()
    {
        return constants.xOutGapMax;
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
        return IN_WEIGHTS;
    }

    //---------------//
    // getOutWeights //
    //---------------//
    @Override
    protected double[] getOutWeights ()
    {
        return OUT_WEIGHTS;
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    /**
     * Support coeff for accidental.
     *
     * @return coeff for accid.
     */
    @Override
    protected double getSourceCoeff ()
    {
        return constants.AccidentalCoeff.getValue();
    }

    @Override
    protected Scale.Fraction getXInGapMax ()
    {
        return getXInGapMaximum();
    }

    @Override
    protected Scale.Fraction getXOutGapMax ()
    {
        return getXOutGapMaximum();
    }

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

        private final Constant.Ratio AccidentalCoeff = new Constant.Ratio(
                3,
                "Supporting coeff for (source) accidental");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.4,
                "Maximum vertical gap between accid & note head");

        private final Scale.Fraction xInGapMax = new Scale.Fraction(
                0.2,
                "Maximum horizontal overlap between accid & note head");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                2.0,
                "Maximum horizontal gap between accid & note head");

        private final Constant.Ratio xInWeight = new Constant.Ratio(
                1,
                "Relative impact weight for xInGap");

        private final Constant.Ratio xOutWeight = new Constant.Ratio(
                1,
                "Relative impact weight for xOutGap");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                4,
                "Relative impact weight for yGap");
    }
}
