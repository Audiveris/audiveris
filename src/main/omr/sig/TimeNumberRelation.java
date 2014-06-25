//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               T i m e N u m b e r R e l a t i o n                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

/**
 * Class {@code TimeNumberRelation} represents the relation between a top number and a
 * bottom number in a time signature.
 *
 * @author Hervé Bitteur
 */
public class TimeNumberRelation
        extends BasicSupport
{

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "Num-Den";
    }

}
