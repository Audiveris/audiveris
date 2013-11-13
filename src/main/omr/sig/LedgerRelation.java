//----------------------------------------------------------------------------//
//                                                                            //
//                         L e d g e r R e l a t i o n                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Scale;

/**
 * Class {@code LedgerRelation} represents a support relation between
 * two ledgers.
 *
 * @author Hervé Bitteur
 */
public class LedgerRelation
        extends AbstractConnection
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new LedgerRelation object.
     */
    public LedgerRelation ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum ()
    {
        return constants.xOutGapMax;
    }

    //----------------//
    // getYGapMaximum //
    //----------------//
    public static Scale.Fraction getYGapMaximum ()
    {
        return constants.yGapMax;
    }

    @Override
    public String getName ()
    {
        return "Ledger-Ledger";
    }

    //-----------------//
    // getSupportCoeff //
    //-----------------//
    @Override
    protected double getSupportCoeff ()
    {
        return constants.supportCoeff.getValue();
    }

    //--------------//
    // getXInGapMax //
    //--------------//
    @Override
    protected Scale.Fraction getXInGapMax ()
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    //---------------//
    // getXOutGapMax //
    //---------------//
    @Override
    protected Scale.Fraction getXOutGapMax ()
    {
        return getXOutGapMaximum();
    }

    //------------//
    // getYGapMax //
    //------------//
    @Override
    protected Scale.Fraction getYGapMax ()
    {
        return getYGapMaximum();
    }

    //~ Inner Classes ----------------------------------------------------------
    //------------//
    // OutImpacts //
    //------------//
    public static class OutImpacts
            extends SupportImpacts
    {
        //~ Static fields/initializers -----------------------------------------

        protected static final String[] NAMES = new String[]{"yGap", "xOutGap"};

        protected static final double[] WEIGHTS = new double[]{4, 1};

        //~ Constructors -------------------------------------------------------
        public OutImpacts (double yGap,
                           double xOutGap)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, yGap);
            setImpact(1, xOutGap);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Ratio supportCoeff = new Constant.Ratio(
                5,
                "Value for coeff in support formula");

        final Scale.Fraction yGapMax = new Scale.Fraction(
                0.25,
                "Maximum vertical gap between two sibling stems");

        final Scale.Fraction xOutGapMax = new Scale.Fraction(
                1.0,
                "Maximum horizontal gap between two sibling stems");

    }
}
