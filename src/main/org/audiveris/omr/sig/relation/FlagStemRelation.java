//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 F l a g S t e m R e l a t i o n                                //
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
import static org.audiveris.omr.glyph.ShapeSet.FlagsUp;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sig.inter.AbstractFlagInter;
import org.audiveris.omr.sig.inter.Inter;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_BOTTOM;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_MIDDLE;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_TOP;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>FlagStemRelation</code> represents the relation support between a flag and a
 * stem.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "flag-stem")
public class FlagStemRelation
        extends AbstractStemConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(FlagStemRelation.class);

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final AbstractFlagInter flag = (AbstractFlagInter) e.getEdgeSource();
        flag.checkAbnormal();
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
    // getStemPortion //
    //----------------//
    @Override
    public StemPortion getStemPortion (Inter source,
                                       Line2D stemLine,
                                       Scale scale)
    {
        final double margin = scale.getInterline(); // TODO: use a constant instead?

        if (FlagsUp.contains(source.getShape())) {
            return (extensionPoint.getY() < (stemLine.getY1() + margin)) ? STEM_TOP : STEM_MIDDLE;
        } else {
            return (extensionPoint.getY() > (stemLine.getY2() - margin)) ? STEM_BOTTOM
                    : STEM_MIDDLE;
        }
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
    protected Scale.Fraction getXInGapMax (int profile)
    {
        return getXInGapMaximum(profile);
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

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final AbstractFlagInter flag = (AbstractFlagInter) e.getEdgeSource();

        if (!flag.isRemoved()) {
            flag.checkAbnormal();
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // getXInGapMaximum //
    //------------------//
    public static Scale.Fraction getXInGapMaximum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.xInGapMax, profile);
    }

    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.xOutGapMax, profile);
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

        private final Constant.Ratio flagSupportCoeff = new Constant.Ratio(
                3,
                "Value for source (flag) coeff in support formula");

        private final Constant.Ratio stemSupportCoeff = new Constant.Ratio(
                3,
                "Value for target (stem) coeff in support formula");

        private final Scale.Fraction xInGapMax = new Scale.Fraction(
                0.3,
                "Maximum horizontal overlap between stem & flag");

        @SuppressWarnings("unused")
        private final Scale.Fraction xInGapMax_p1 = new Scale.Fraction(0.45, "Idem for profile 1");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.3,
                "Maximum horizontal gap between stem & flag");

        @SuppressWarnings("unused")
        private final Scale.Fraction xOutGapMax_p1 = new Scale.Fraction(0.45, "Idem for profile 1");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.5,
                "Maximum vertical gap between stem & flag");

        @SuppressWarnings("unused")
        private final Scale.Fraction yGapMax_p1 = new Scale.Fraction(0.75, "Idem for profile 1");
    }
}
