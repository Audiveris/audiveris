//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               D o t F e r m a t a R e l a t i o n                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
 * Class {@code DotFermataRelation} represents the relation between a dot and a
 * compatible fermata arc.
 * <p>
 * Implementation remark:
 * This is just a temporary workaround to cope with the fact that the two parts of a fermata sign
 * (dot and arc) are rather far apart.
 * Accepting the dot-arc distance as an inner distance for a single symbol, would lead to
 * combinatory explosion when aggregating glyphs in SYMBOLS step.
 * Hence, for the time being, we handle them as two separate symbols linked together.
 * This approach is likely to evolve when the Patch Classifier gets available.
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

    private static final double[] OUT_WEIGHTS = new double[]{constants.xOutWeight.getValue(),
                                                             constants.yWeight.getValue()};

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        return true;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        return true;
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
        return constants.arcSupportCoeff.getValue();
    }

    @Override
    protected Scale.Fraction getXOutGapMax (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.xOutGapMax, profile);
    }

    @Override
    protected Scale.Fraction getYGapMax (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.yGapMax, profile);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio dotSupportCoeff = new Constant.Ratio(
                5,
                "Supporting coeff for (source) dot");

        private final Constant.Ratio arcSupportCoeff = new Constant.Ratio(
                5,
                "Supporting coeff for (target) fermata");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.75,
                "Maximum horizontal gap between dot center & fermata reference point");

        private final Scale.Fraction xOutGapMax_p1 = new Scale.Fraction(
                1.0,
                "Idem for profile 1");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.5,
                "Maximum vertical gap between dot center & fermata reference point");

        private final Scale.Fraction yGapMax_p1 = new Scale.Fraction(
                0.75,
                "Idem for profile 1");

        private final Constant.Ratio xOutWeight = new Constant.Ratio(
                1,
                "Relative impact weight for xOutGap");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                1,
                "Relative impact weight for yGap");
    }
}
