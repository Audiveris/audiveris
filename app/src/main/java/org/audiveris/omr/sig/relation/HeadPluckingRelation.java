//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             H e a d P l u c k i n g R e l a t i o n                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.sig.inter.PluckingInter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>HeadPluckingRelation</code> represents the relation between a note head and
 * a plucking indication.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "head-plucking")
public class HeadPluckingRelation
        extends AbstractConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Constructors -------------------------------------------------------------------------------

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final PluckingInter plucking = (PluckingInter) e.getEdgeTarget();

        plucking.checkAbnormal();
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return constants.pluckingTargetCoeff.getValue();
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
        final PluckingInter plucking = (PluckingInter) e.getEdgeTarget();

        if (!plucking.isRemoved()) {
            plucking.checkAbnormal();
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

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Ratio pluckingTargetCoeff = new Constant.Ratio(
                2.0,
                "Supporting coeff for (target) plucking");

        private final Scale.Fraction xGapMax = new Scale.Fraction(
                1.5,
                "Maximum horizontal gap between plucking center & chord");

        private final Scale.Fraction xGapMax_p1 = new Scale.Fraction(2.5, "Idem for profile 1");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                2.0,
                "Maximum vertical gap between plucking center & chord");

        private final Scale.Fraction yGapMax_p1 = new Scale.Fraction(3.0, "Idem for profile 1");
    }
}
