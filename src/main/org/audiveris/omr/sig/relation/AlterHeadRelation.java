//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A l t e r H e a d R e l a t i o n                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.Inter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AlterHeadRelation} represents the relation support between an accidental
 * alteration (sharp, flat, natural, double-sharp, double-flat) and a note head.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "alter-head")
public class AlterHeadRelation
        extends AbstractConnection
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AlterHeadRelation.class);

    private static final double[] IN_WEIGHTS = new double[]{constants.xInWeight.getValue(),
                                                            constants.yWeight.getValue()};

    private static final double[] OUT_WEIGHTS = new double[]{constants.xOutWeight.getValue(),
                                                             constants.yWeight.getValue()};

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final AlterInter alter = (AlterInter) e.getEdgeSource();
        alter.checkAbnormal();
    }

    //------------------//
    // getXInGapMaximum //
    //------------------//
    public static Scale.Fraction getXInGapMaximum (boolean manual)
    {
        return manual ? constants.xInGapMaxManual : constants.xInGapMax;
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
        final AlterInter alter = (AlterInter) e.getEdgeSource();

        if (!alter.isRemoved()) {
            alter.checkAbnormal();
        }
    }

    //--------------//
    // getInWeights //
    //--------------//
    @Override
    protected double[] getInWeights ()
    {
        return IN_WEIGHTS;
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
     * Support coeff for accidental.
     *
     * @return coeff for accid.
     */
    @Override
    protected double getSourceCoeff ()
    {
        return constants.AccidentalCoeff.getValue();
    }

    @Override
    protected Scale.Fraction getXInGapMax (boolean manual)
    {
        return getXInGapMaximum(manual);
    }

    @Override
    protected Scale.Fraction getXOutGapMax (boolean manual)
    {
        return getXOutGapMaximum(manual);
    }

    @Override
    protected Scale.Fraction getYGapMax (boolean manual)
    {
        return getYGapMaximum(manual);
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {

        private final Constant.Ratio AccidentalCoeff = new Constant.Ratio(3,
                                                                          "Supporting coeff for (source) accidental");

        private final Scale.Fraction yGapMax = new Scale.Fraction(0.45,
                                                                  "Maximum vertical gap between accid & note head");

        private final Scale.Fraction yGapMaxManual = new Scale.Fraction(0.6,
                                                                        "Maximum vertical gap between manual accid & note head");

        private final Scale.Fraction xInGapMax = new Scale.Fraction(0.2,
                                                                    "Maximum horizontal overlap between accid & note head");

        private final Scale.Fraction xInGapMaxManual = new Scale.Fraction(0.3,
                                                                          "Maximum horizontal overlap between manual accid & note head");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(2.0,
                                                                     "Maximum horizontal gap between accid & note head");

        private final Scale.Fraction xOutGapMaxManual = new Scale.Fraction(3.0,
                                                                           "Maximum horizontal gap between manual accid & note head");

        private final Constant.Ratio xInWeight = new Constant.Ratio(1,
                                                                    "Relative impact weight for xInGap");

        private final Constant.Ratio xOutWeight = new Constant.Ratio(1,
                                                                     "Relative impact weight for xOutGap");

        private final Constant.Ratio yWeight = new Constant.Ratio(4,
                                                                  "Relative impact weight for yGap");
    }
}
