//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               D o t F e r m a t a R e l a t i o n                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code DotFermataRelation} represents the relation between a dot in the middle
 * of a fermata half-circle and the fermata symbol (perhaps just the half-circle).
 *
 * @author Hervé Bitteur
 */
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

        final Constant.Ratio dotSupportCoeff = new Constant.Ratio(
                5,
                "Supporting coeff for (source) dot");

        final Constant.Ratio fermataSupportCoeff = new Constant.Ratio(
                5,
                "Supporting coeff for (target) fermata");

        final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.75,
                "Maximum horizontal gap between dot center & fermata reference point");

        final Scale.Fraction yGapMax = new Scale.Fraction(
                0.5,
                "Maximum vertical gap between dot center & fermata reference point");

        final Constant.Ratio xOutWeight = new Constant.Ratio(
                1,
                "Relative impact weight for xOutGap");

        final Constant.Ratio yWeight = new Constant.Ratio(1, "Relative impact weight for yGap");
    }
}
