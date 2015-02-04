//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             R e p e a t D o t D o t R e l a t i o n                            //
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
 * Class {@code RepeatDotDotRelation} represents the relation between two repeat dots
 * in a pair.
 *
 * @author Hervé Bitteur
 */
public class RepeatDotDotRelation
        extends BasicSupport
{
    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "RepeatDot-Dot";
    }
}
