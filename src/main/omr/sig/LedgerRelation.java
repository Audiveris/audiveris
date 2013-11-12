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
    @Override
    public String getName ()
    {
        return "Ledger-Ledger";
    }

    //----------//
    // getRatio //
    //----------//
    @Override
    public Double getRatio ()
    {
        return 1.0 + (10.0 * grade);
    }

    @Override
    protected double getXWeight ()
    {
        return constants.xWeight.getValue();
    }

    @Override
    protected double getYWeight ()
    {
        return constants.yWeight.getValue();
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Double xWeight = new Constant.Double(
                "weight",
                1,
                "Weight assigned to horizontal gap");

        final Constant.Double yWeight = new Constant.Double(
                "weight",
                4,
                "Weight assigned to ordinate delta");

    }
}
