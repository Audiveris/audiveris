//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              C h o r d T u p l e t R e l a t i o n                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
import org.audiveris.omr.sig.inter.TupletInter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>ChordTupletRelation</code> represents the relation between a chord and an
 * embracing tuplet sign.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-tuplet")
public class ChordTupletRelation
        extends Support
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------

    /** Assigned tuplet support coefficient. */
    private final double tupletCoeff;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB and user allocation.
     */
    public ChordTupletRelation ()
    {
        this.tupletCoeff = 0;
    }

    private ChordTupletRelation (double tupletCoeff)
    {
        this.tupletCoeff = tupletCoeff;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final TupletInter tuplet = (TupletInter) e.getEdgeTarget();

        if (!tuplet.isImplicit()) {
            tuplet.checkAbnormal();
        }
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return tupletCoeff;
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
        final TupletInter tuplet = (TupletInter) e.getEdgeTarget();

        if (!tuplet.isRemoved() && !tuplet.isImplicit()) {
            tuplet.checkAbnormal();
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------//
    // create //
    //--------//
    /**
     * Create the relation of a chord that makes up a tuplet.
     * <p>
     * Every such chord brings the same support, whatever the tuplet number: how many chords
     * make up a tuplet follows from their durations, not from the number.
     *
     * @return the supporting relation
     */
    public static ChordTupletRelation create ()
    {
        return new ChordTupletRelation(constants.tupletSupportCoeff.getValue());
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Ratio tupletSupportCoeff = new Constant.Ratio(
                2 * 0.33,
                "Supporting coeff brought by each chord of a tuplet");
    }
}
