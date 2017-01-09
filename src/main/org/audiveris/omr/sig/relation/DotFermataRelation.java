//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               D o t F e r m a t a R e l a t i o n                              //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code DotFermataRelation} represents the relation between a dot in the middle
 * of a fermata half-circle and the fermata symbol (perhaps just the half-circle).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "dot-fermata")
public class DotFermataRelation
        extends AbstractConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(DotFermataRelation.class);

    private static final double[] OUT_WEIGHTS = new double[]{
        constants.xOutWeight.getValue(),
        constants.yWeight.getValue()
    };

    //~ Methods ------------------------------------------------------------------------------------
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
     * DotFermataRelation supports the dot source.
     *
     * @return how much dot is supported
     */
    @Override
    protected double getSourceCoeff ()
    {
        return constants.dotSupportCoeff.getValue();
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    /**
     * DotFermataRelation supports the fermata target.
     *
     * @return how much fermata is supported
     */
    @Override
    protected double getTargetCoeff ()
    {
        return constants.fermataSupportCoeff.getValue();
    }

    @Override
    protected Scale.Fraction getXOutGapMax ()
    {
        return constants.xOutGapMax;
    }

    @Override
    protected Scale.Fraction getYGapMax ()
    {
        return constants.yGapMax;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio dotSupportCoeff = new Constant.Ratio(
                5,
                "Supporting coeff for (source) dot");

        private final Constant.Ratio fermataSupportCoeff = new Constant.Ratio(
                5,
                "Supporting coeff for (target) fermata");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.75,
                "Maximum horizontal gap between dot center & fermata reference point");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.5,
                "Maximum vertical gap between dot center & fermata reference point");

        private final Constant.Ratio xOutWeight = new Constant.Ratio(
                1,
                "Relative impact weight for xOutGap");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                1,
                "Relative impact weight for yGap");
    }
}
