//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  C l e f K e y R e l a t i o n                                 //
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
 * Class {@code ClefKeyRelation} represents a support relation between a clef and a
 * compatible key signature.
 *
 * @author Hervé Bitteur
 */
public class ClefKeyRelation
        extends BasicSupport
{
    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "Clef-Key";
    }
}
