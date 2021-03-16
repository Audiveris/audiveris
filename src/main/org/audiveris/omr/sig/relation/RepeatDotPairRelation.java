//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            R e p e a t D o t P a i r R e l a t i o n                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.RepeatDotInter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code RepeatDotPairRelation} represents the relation between two repeat dots
 * in a pair.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "repeat-dot-pair")
public class RepeatDotPairRelation
        extends Support
{

    private static final Constants constants = new Constants();

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final RepeatDotInter dot1 = (RepeatDotInter) e.getEdgeSource();
        final RepeatDotInter dot2 = (RepeatDotInter) e.getEdgeTarget();

        dot1.checkAbnormal();
        dot2.checkAbnormal();
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

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        return constants.dotSupportCoeff.getValue();
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return constants.dotSupportCoeff.getValue();
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final RepeatDotInter dot1 = (RepeatDotInter) e.getEdgeSource();
        final RepeatDotInter dot2 = (RepeatDotInter) e.getEdgeTarget();

        if (!dot1.isRemoved()) {
            dot1.checkAbnormal();
        }

        if (!dot2.isRemoved()) {
            dot2.checkAbnormal();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio dotSupportCoeff = new Constant.Ratio(
                5,
                "Value for source/target (dot) coeff in support formula");
    }
}
