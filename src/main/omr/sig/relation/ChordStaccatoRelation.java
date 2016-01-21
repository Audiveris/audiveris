//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            C h o r d S t a c c a t o R e l a t i o n                           //
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
 * Class {@code ChordStaccatoRelation} represents the relation between a note-based
 * chord and a staccato dot.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-staccato")
public class ChordStaccatoRelation
        extends AbstractConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ChordStaccatoRelation.class);

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
     * @return the supporting coefficient for (source) staccato dot
     */
    @Override
    protected double getTargetCoeff ()
    {
        return constants.staccatoDotSupportCoeff.getValue();
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

        private final Constant.Ratio staccatoDotSupportCoeff = new Constant.Ratio(
                3,
                "Supporting coeff for (target) staccato dot");

        private final Scale.Fraction xGapMax = new Scale.Fraction(
                0.75,
                "Maximum horizontal gap between staccato center & chord");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                1.5,
                "Maximum vertical gap between staccato center & chord");

        private final Constant.Ratio xWeight = new Constant.Ratio(
                3,
                "Relative impact weight for xGap (in or out)");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                1,
                "Relative impact weight for yGap");
    }
}
