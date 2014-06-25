//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 K e y A l t e r R e l a t i o n                                //
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
 * Class {@code KeyAlterRelation} represents the support relation between the
 * alterations items of a key signature.
 *
 * @author Hervé Bitteur
 */
public class KeyAlterRelation
        extends BasicSupport
{
    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "Key-Alter";
    }
}
