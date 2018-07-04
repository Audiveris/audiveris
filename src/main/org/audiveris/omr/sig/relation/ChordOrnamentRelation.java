//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            C h o r d O r n a m e n t R e l a t i o n                           //
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

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ChordOrnamentRelation} represents the relation between a head-chord and
 * an ornament.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-ornament")
public class ChordOrnamentRelation
        extends AbstractConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum (boolean manual)
    {
        return manual ? constants.xGapMaxManual : constants.xGapMax;
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

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return constants.ornamentTargetCoeff.getValue();
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio ornamentTargetCoeff = new Constant.Ratio(
                0.5,
                "Supporting coeff for (target) ornament");

        private final Scale.Fraction xGapMax = new Scale.Fraction(
                0.75,
                "Maximum horizontal gap between ornament center & chord");

        private final Scale.Fraction xGapMaxManual = new Scale.Fraction(
                1.2,
                "Maximum manual horizontal gap between ornament center & chord");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                2.0,
                "Maximum vertical gap between ornament center & chord");

        private final Scale.Fraction yGapMaxManual = new Scale.Fraction(
                3.0,
                "Maximum manual vertical gap between ornament center & chord");
    }
}
