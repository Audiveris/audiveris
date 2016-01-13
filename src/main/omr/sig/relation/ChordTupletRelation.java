//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              C h o r d T u p l e t R e l a t i o n                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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
