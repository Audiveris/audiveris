//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             T r e m o l o W h o l e R e l a t i o n                            //
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

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.TremoloInter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>TremoloWholeRelation</code> represents the support relation between a tremolo
 * and its related stemless head (typically a whole note).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "tremolo-whole")
public class TremoloWholeRelation
        extends AbstractConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TremoloWholeRelation.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>WholeRelation</code> object.
     */
    public TremoloWholeRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final TremoloInter tremolo = (TremoloInter) e.getEdgeSource();

        tremolo.checkAbnormal();
    }

    //---------------//
    // getXOutGapMax //
    //---------------//
    @Override
    protected Scale.Fraction getXOutGapMax (int profile)
    {
        return getCenterDxMaximum(profile);
    }

    //------------//
    // getYGapMax //
    //------------//
    @Override
    protected Scale.Fraction getYGapMax (int profile)
    {
        return getYGapMaximum(profile);
    }

    //-------------//
    // isForbidden //
    //-------------//
    /**
     * {@inheritDoc }
     * <p>
     * Specifically, a TremoloWholeRelation can be targeted only to a stem-less head.
     */
    @Override
    public boolean isForbidden (Inter source,
                                Inter target)
    {
        if (!(source instanceof TremoloInter)) {
            return true;
        }

        if (!(target instanceof HeadInter head)) {
            return true;
        }

        return !ShapeSet.StemLessHeads.contains(head.getShape());
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
        final TremoloInter tremolo = (TremoloInter) e.getEdgeSource();

        if (!tremolo.isRemoved()) {
            tremolo.checkAbnormal();
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------------------//
    // getCenterDxMaximum //
    //--------------------//
    public static Scale.Fraction getCenterDxMaximum (int profile)
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
        private final Scale.Fraction yGapMax = new Scale.Fraction(
                2.0,
                "Maximum vertical gap between whole head & tremolo");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.3,
                "Maximum horizontal distance between whole head center & tremolo center");

    }
}
