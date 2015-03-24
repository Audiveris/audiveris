//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I d e m R e l a t i o n                                    //
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
 * Class {@code IdemRelation} represents the relation of identical inters from one
 * staff to the other within a system.
 *
 * @author Hervé Bitteur
 */
public class IdemRelation
        extends BasicSupport
{
    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "Idem";
    }
}
