//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                        C h o r d A r t i c u l a t i o n R e l a t i o n                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
import org.audiveris.omr.sig.inter.ArticulationInter;
import org.audiveris.omr.sig.inter.Inter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>ChordArticulationRelation</code> is a connection between a head-based chord
 * and an articulation sign (tenuto, accent, staccato, staccatissimo, marcato).
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

    private static final double[] WEIGHTS = new double[]
    { constants.xWeight.getValue(), constants.yWeight.getValue() };

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final ArticulationInter articulation = (ArticulationInter) e.getEdgeTarget();
        articulation.checkAbnormal();
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
    protected Scale.Fraction getXOutGapMax (int profile)
    {
        return getXOutGapMaximum(profile);
    }

    //------------//
    // getYGapMax //
    //------------//
    @Override
    protected Scale.Fraction getYGapMax (int profile)
    {
        return getYGapMaximum(profile);
    }

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

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final ArticulationInter articulation = (ArticulationInter) e.getEdgeTarget();

        if (!articulation.isRemoved()) {
            articulation.checkAbnormal();
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.xGapMax, profile);
    }

    //----------------//
    // getYGapMaximum //
    //----------------//
    public static Scale.Fraction getYGapMaximum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.yGapMax, profile);
    }

    //----------------//
    // getYGapMinimum //
    //----------------//
    public static Scale.Fraction getYGapMinimum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.yGapMin, profile);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio articulationSupportCoeff = new Constant.Ratio(
                3,
                "Supporting coeff for (target) articulation");

        private final Scale.Fraction xGapMax = new Scale.Fraction(
                0.8,
                "Maximum horizontal gap between articulation center & chord");

        @SuppressWarnings("unused")
        private final Scale.Fraction xGapMax_p1 = new Scale.Fraction(1.0, "Idem for profile 1");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                2.0,
                "Maximum vertical gap between articulation center & chord");

        @SuppressWarnings("unused")
        private final Scale.Fraction yGapMax_p1 = new Scale.Fraction(3.0, "Idem for profile 1");

        private final Scale.Fraction yGapMin = new Scale.Fraction(
                0.1,
                "Minimum vertical gap between articulation center & chord");

        @SuppressWarnings("unused")
        private final Scale.Fraction yGapMin_p1 = new Scale.Fraction(0.05, "Idem for profile 1");

        private final Constant.Ratio xWeight = new Constant.Ratio(
                3,
                "Relative impact weight for xGap (in or out)");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                1,
                "Relative impact weight for yGap");
    }
}
