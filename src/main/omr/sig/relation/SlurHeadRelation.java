//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S l u r H e a d R e l a t i o n                                //
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

import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SlurHeadRelation} represents a link between a slur and one of the two
 * embraced note heads.
 * <p>
 * Distance from slur end to embraced note head is quite complex. For the time being, we simply
 * record the detected relation without further evaluation detail.
 * <p>
 * TODO: Actually, perhaps we should deal with embraced chord instead of embraced head?
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "slur-head")
public class SlurHeadRelation
        extends AbstractSupport
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SlurHeadRelation.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Left or right side of the slur. */
    @XmlAttribute(name = "side")
    private final HorizontalSide side;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SlurNoteRelation} object.
     *
     * @param side the left or right side of the slur
     */
    public SlurHeadRelation (HorizontalSide side)
    {
        this.side = side;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private SlurHeadRelation ()
    {
        this.side = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getSide //
    //---------//
    /**
     * @return the side
     */
    public HorizontalSide getSide ()
    {
        return side;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return super.toString() + "/" + side;
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        return constants.slurSupportCoeff.getValue();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //    //-------------------//
    //    // getXOutGapMaximum //
    //    //-------------------//
    //    public static Scale.Fraction getXOutGapMaximum ()
    //    {
    //        return constants.xOutGapMax;
    //    }
    //
    //    //----------------//
    //    // getYGapMaximum //
    //    //----------------//
    //    public static Scale.Fraction getYGapMaximum ()
    //    {
    //        return constants.yGapMax;
    //    }
    //
    //    //------------------//
    //    // getXInGapMaximum //
    //    //------------------//
    //    public static Scale.Fraction getXInGapMaximum ()
    //    {
    //        return constants.xInGapMax;
    //    }
    //
    //    //--------------//
    //    // getInWeights //
    //    //--------------//
    //    @Override
    //    protected double[] getInWeights ()
    //    {
    //        return IN_WEIGHTS;
    //    }
    //
    //    //---------------//
    //    // getOutWeights //
    //    //---------------//
    //    @Override
    //    protected double[] getOutWeights ()
    //    {
    //        return OUT_WEIGHTS;
    //    }
    //
    //    //----------------//
    //    // getTargetCoeff //
    //    //----------------//
    //    /**
    //     * StaccatoNoteRelation brings no support on target (Note) side.
    //     *
    //     * @return 0
    //     */
    //    @Override
    //    protected double getTargetCoeff ()
    //    {
    //        return 0.0;
    //    }
    //
    //    @Override
    //    protected Scale.Fraction getXInGapMax ()
    //    {
    //        return getXInGapMaximum();
    //    }
    //
    //    @Override
    //    protected Scale.Fraction getXOutGapMax ()
    //    {
    //        return getXOutGapMaximum();
    //    }
    //
    //    @Override
    //    protected Scale.Fraction getYGapMax ()
    //    {
    //        return getYGapMaximum();
    //    }
    //
    //    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio slurSupportCoeff = new Constant.Ratio(
                5,
                "Value for (source) slur coeff in support formula");

        //
        //            private final Scale.Fraction xInGapMax = new Scale.Fraction(
        //                    0.5,
        //                    "Maximum horizontal overlap between slur end & note reference point");
        //
        //            private final Scale.Fraction xOutGapMax = new Scale.Fraction(
        //                    0.75,
        //                    "Maximum horizontal gap between slur end & note reference point");
        //
        //            private final Scale.Fraction yGapMax = new Scale.Fraction(
        //                    6.0,
        //                    "Maximum vertical gap between slur end & note reference point");
        //
        //            private final Constant.Ratio xInWeight = new Constant.Ratio(3, "Relative impact weight for xInGap");
        //
        //            private final Constant.Ratio xOutWeight = new Constant.Ratio(
        //                    3,
        //                    "Relative impact weight for xOutGap");
        //
        //            private final Constant.Ratio yWeight = new Constant.Ratio(1, "Relative impact weight for yGap");
    }
}
