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
 * Class {@code NoExclusion}
 *
 * @author Hervé Bitteur
 */
public class NoExclusion
        extends BasicSupport
{
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
