//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 F l a g S t e m R e l a t i o n                                //
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
import static omr.glyph.ShapeSet.FlagsUp;

import omr.sheet.Scale;

import omr.sig.inter.Inter;
import static omr.sig.relation.StemPortion.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;

/**
 * Class {@code FlagStemRelation} represents the relation support between a flag and a
 * stem.
 *
 * @author Hervé Bitteur
 */
public class FlagStemRelation
        extends AbstractStemConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(FlagStemRelation.class);

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getStemPortion //
    //----------------//
    @Override
    public StemPortion getStemPortion (Inter source,
                                       Line2D stemLine,
                                       Scale scale)
    {
        final double margin = scale.getInterline(); // TODO: use a constant instead?

        if (FlagsUp.contains(source.getShape())) {
            return (extensionPoint.getY() > (stemLine.getY2() - margin)) ? STEM_BOTTOM : STEM_MIDDLE;
        } else {
            return (extensionPoint.getY() < (stemLine.getY1() + margin)) ? STEM_TOP : STEM_MIDDLE;
        }
    }

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

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        return constants.flagSupportCoeff.getValue();
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return constants.stemSupportCoeff.getValue();
    }

    //--------------//
    // getXInGapMax //
    //--------------//
    @Override
    protected Scale.Fraction getXInGapMax ()
    {
        return getXInGapMaximum();
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

        private final Constant.Ratio flagSupportCoeff = new Constant.Ratio(
                3,
                "Value for source (flag) coeff in support formula");

        private final Constant.Ratio stemSupportCoeff = new Constant.Ratio(
                3,
                "Value for target (stem) coeff in support formula");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.5,
                "Maximum vertical gap between stem & flag");

        private final Scale.Fraction xInGapMax = new Scale.Fraction(
                0.3,
                "Maximum horizontal overlap between stem & flag");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.3,
                "Maximum horizontal gap between stem & flag");
    }
}
