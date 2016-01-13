//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                D o u b l e D o t R e l a t i o n                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
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

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code DoubleDotRelation} represents the relation between a second
 * augmentation dot and a first augmentation dot (case of double dot augmentation).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "double-dot")
public class DoubleDotRelation
        extends AbstractConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(DoubleDotRelation.class);

    private static final double[] OUT_WEIGHTS = new double[]{
        constants.xOutWeight.getValue(),
        constants.yWeight.getValue()
    };

    //~ Methods ------------------------------------------------------------------------------------
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
    // getSourceCoeff //
    //----------------//
    /**
     * @return the supporting coefficient for (source) second dot
     */
    @Override
    protected double getSourceCoeff ()
    {
        return constants.secondDotSupportCoeff.getValue();
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

        private final Constant.Ratio secondDotSupportCoeff = new Constant.Ratio(
                5,
                "Supporting coeff for (source) second dot");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                1.25,
                "Maximum horizontal gap between dots centers");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.25,
                "Maximum vertical gap between dots centers");

        private final Constant.Ratio xOutWeight = new Constant.Ratio(
                1,
                "Relative impact weight for xOutGap");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                3,
                "Relative impact weight for yGap");
    }
}
