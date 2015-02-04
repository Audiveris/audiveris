//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              T u p l e t C h o r d R e l a t i o n                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

/**
 * Class {@code TupletChordRelation} represents the relation between a Tuplet sign and
 * an embraced chord.
 *
 * @author Hervé Bitteur
 */
public class TupletChordRelation
        extends BasicSupport
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
    public TupletChordRelation (Shape shape)
    {
        tupletCoeff = getTupletCoeff(shape);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String getName ()
    {
        return "Tuplet-Chord";
    }

    @Override
    protected double getSourceCoeff ()
    {
        return tupletCoeff;
    }

    @Override
    protected double getTargetCoeff ()
    {
        return 0;
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

        final Constant.Ratio tupletThreeSupportCoeff = new Constant.Ratio(
                2 * 0.33,
                "Supporting coeff for tuplet 3");

        final Constant.Ratio tupletSixSupportCoeff = new Constant.Ratio(
                2 * 0.17,
                "Supporting coeff for tuplet 6");
    }
}
