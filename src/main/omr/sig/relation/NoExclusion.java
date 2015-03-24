//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      N o E x c l u s i o n                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

/**
 * Class {@code NoExclusion} is used to formalize that two Inters, generally originating
 * from mirrored entities do not exclude each other, although they overlap.
 * This occurs with beams of mirrored stems.
 *
 * @author Hervé Bitteur
 */
public class NoExclusion
        extends BasicSupport
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code NoExclusion} object.
     */
    public NoExclusion ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "No-Exclusion";
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        return 0;
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return 0;
    }
}
