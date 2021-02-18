//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                E n d i n g B a r R e l a t i o n                               //
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
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;

import org.jgrapht.event.GraphEdgeChangeEvent;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code EndingBarRelation} connects an ending side with a bar line.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "ending-bar")
public class EndingBarRelation
        extends AbstractConnection
{

    private static final Constants constants = new Constants();

    private static final double[] WEIGHTS = new double[]{
        constants.xWeight.getValue(),
        constants.yWeight.getValue()};

    /** Which side of ending is used?. */
    @XmlAttribute(name = "side")
    private HorizontalSide endingSide;

    /** Horizontal delta (in interline) between bar line and ending side. */
    private final double xDistance;

    /**
     * Creates a new EndingBarRelation object.
     *
     * @param endingSide which side of ending
     * @param xDistance  horizontal delta
     */
    public EndingBarRelation (HorizontalSide endingSide,
                              double xDistance)
    {
        this.endingSide = endingSide;
        this.xDistance = xDistance;
    }

    /**
     * No-arg constructor meant for JAXB and user allocation.
     */
    public EndingBarRelation ()
    {
        this.xDistance = 0;
    }

    //----------------//
    // getXGapMaximum //
    //----------------//
    public static Scale.Fraction getXGapMaximum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.xGapMax, profile);
    }

    //-------//
    // added //
    //-------//
    /**
     * Populate endingSide if needed.
     *
     * @param e edge change event
     */
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        if (endingSide == null) {
            final EndingInter ending = (EndingInter) e.getEdgeSource();
            endingSide = (e.getEdgeTarget().getCenter().x < ending.getCenter().x) ? LEFT : RIGHT;
        }
    }

    //---------------//
    // getEndingSide //
    //---------------//
    /**
     * Report the horizontal side of ending where the barline is located
     *
     * @return the endingSide
     */
    public HorizontalSide getEndingSide ()
    {
        return endingSide;
    }

    //--------------//
    // getXInGapMax //
    //--------------//
    @Override
    public Scale.Fraction getXInGapMax (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.xGapMax, profile);
    }

    //---------------//
    // getXOutGapMax //
    //---------------//
    @Override
    public Scale.Fraction getXOutGapMax (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.xGapMax, profile);
    }

    //------------//
    // getYGapMax //
    //------------//
    @Override
    public Scale.Fraction getYGapMax (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.yGapMax, profile);
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
        return false;
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
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        // Support for ending
        return constants.endingSupportCoeff.getValue();
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        // No support for barline
        return 0.0;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(endingSide).append("@(").append(String.format("%.2f", xDistance)).append(")");

        return sb.toString();
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio endingSupportCoeff = new Constant.Ratio(
                3,
                "Supporting coeff for (source) ending");

        private final Scale.Fraction xGapMax = new Scale.Fraction(
                2.0,
                "Maximum horizontal gap between ending and barline");

        @SuppressWarnings("unused")
        private final Scale.Fraction xGapMax_p1 = new Scale.Fraction(
                3.0,
                "Idem for profile 1");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                10.0,
                "Maximum vertical gap between bottom of ending leg and top of barline");

        @SuppressWarnings("unused")
        private final Scale.Fraction yGapMax_p1 = new Scale.Fraction(
                15.0,
                "Idem for profile 1");

        private final Constant.Ratio xWeight = new Constant.Ratio(
                1,
                "Relative impact weight for xGap (in or out)");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                0,
                "Relative impact weight for yGap");
    }
}
