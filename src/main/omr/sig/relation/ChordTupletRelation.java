//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              C h o r d T u p l e t R e l a t i o n                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sig.relation;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ChordTupletRelation} represents the relation between a chord and an
 * embracing tuplet sign.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-tuplet")
public class ChordTupletRelation
        extends AbstractSupport
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Assigned tuplet support coefficient. */
    private final double tupletCoeff;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TupletChordRelation} object.
     */
    public ChordTupletRelation (Shape shape)
    {
        tupletCoeff = getTupletCoeff(shape);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private ChordTupletRelation ()
    {
        this.tupletCoeff = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return tupletCoeff;
    }

    //----------------//
    // getTupletCoeff //
    //----------------//
    private double getTupletCoeff (Shape shape)
    {
        switch (shape) {
        case TUPLET_THREE:
            return constants.tupletThreeSupportCoeff.getValue();

        case TUPLET_SIX:
            return constants.tupletSixSupportCoeff.getValue();

        default:
            throw new IllegalArgumentException("Illegal tuplet shape " + shape);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio tupletThreeSupportCoeff = new Constant.Ratio(
                2 * 0.33,
                "Supporting coeff for tuplet 3");

        private final Constant.Ratio tupletSixSupportCoeff = new Constant.Ratio(
                2 * 0.17,
                "Supporting coeff for tuplet 6");
    }
}
