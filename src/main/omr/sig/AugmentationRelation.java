//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A u g m e n t a t i o n R e l a t i o n                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code AugmentationRelation} represents the relation between an augmentation
 * dot and the related note or rest instance.
 *
 * @author Hervé Bitteur
 */
public class AugmentationRelation
        extends AbstractConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AugmentationRelation.class);

    private static final double[] OUT_WEIGHTS = new double[]{
        constants.xOutWeight.getValue(),
        constants.yWeight.getValue()
    };

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "Augmentation";
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

    //---------------//
    // getOutWeights //
    //---------------//
    @Override
    protected double[] getOutWeights ()
    {
        return OUT_WEIGHTS;
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    /**
     * AugmentationRelation brings no support on target (note or rest) side.
     *
     * @return 0
     */
    @Override
    protected double getTargetCoeff ()
    {
        return 0.0;
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    /**
     * @return the coefficient used to compute source support ratio
     */
    @Override
    protected double getSourceCoeff ()
    {
        return 0.5;
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

        final Scale.Fraction xOutGapMax = new Scale.Fraction(
                2.0,
                "Maximum horizontal gap between dot center & note/rest reference point");

        final Scale.Fraction yGapMax = new Scale.Fraction(
                0.75,
                "Maximum vertical gap between dot center & note/rest reference point");

        final Constant.Ratio xOutWeight = new Constant.Ratio(
                0,
                "Relative impact weight for xOutGap");

        final Constant.Ratio yWeight = new Constant.Ratio(1, "Relative impact weight for yGap");
    }
}
