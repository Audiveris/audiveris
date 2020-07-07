//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             R e p e a t D o t B a r R e l a t i o n                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.RepeatDotInter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code RepeatDotBarRelation} represents the relation between a repeat dot and
 * the related bar line.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "repeat-dot-bar")
public class RepeatDotBarRelation
        extends AbstractConnection
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(RepeatDotBarRelation.class);

    private static final double[] OUT_WEIGHTS = new double[]{
        constants.xOutWeight.getValue(),
        constants.yWeight.getValue()};

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final RepeatDotInter dot = (RepeatDotInter) e.getEdgeSource();

        dot.checkAbnormal();
    }

    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum (boolean manual)
    {
        return manual ? constants.xOutGapMaxManual : constants.xOutGapMax;
    }

    //----------------//
    // getYGapMaximum //
    //----------------//
    public static Scale.Fraction getYGapMaximum (boolean manual)
    {
        return manual ? constants.yGapMaxManual : constants.yGapMax;
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        return false;
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
     * @return the supporting coefficient for (source) repeat dot
     */
    @Override
    protected double getSourceCoeff ()
    {
        return constants.repeatDotSupportCoeff.getValue();
    }

    //---------------//
    // getXOutGapMax //
    //---------------//
    @Override
    protected Scale.Fraction getXOutGapMax (boolean manual)
    {
        return getXOutGapMaximum(manual);
    }

    //------------//
    // getYGapMax //
    //------------//
    @Override
    protected Scale.Fraction getYGapMax (boolean manual)
    {
        return getYGapMaximum(manual);
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final RepeatDotInter dot = (RepeatDotInter) e.getEdgeSource();

        if (!dot.isRemoved()) {
            dot.checkAbnormal();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio repeatDotSupportCoeff = new Constant.Ratio(
                5,
                "Supporting coeff for (source) repeat dot");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                1.5,
                "Maximum horizontal gap between dot center & barline reference point");

        private final Scale.Fraction xOutGapMaxManual = new Scale.Fraction(
                2.2,
                "Maximum manual horizontal gap between dot center & barline reference point");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.5,
                "Maximum vertical gap between dot center & barline reference point");

        private final Scale.Fraction yGapMaxManual = new Scale.Fraction(
                0.75,
                "Maximum manual vertical gap between dot center & barline reference point");

        private final Constant.Ratio xOutWeight = new Constant.Ratio(
                3,
                "Relative impact weight for xOutGap");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                1,
                "Relative impact weight for yGap");
    }
}
