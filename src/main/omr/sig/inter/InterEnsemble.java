//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    I n t e r E n s e m b l e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import java.util.List;

/**
 * Interface {@code InterEnsemble} refers to an inter that is composed of other inters.
 * This class is not mutable.
 * <p>
 * Examples are:<ul>
 * <li>Sentence vs words</li>
 * <li>TimePairInter vs num & den</li>
 * <li>KeyInter vs its alterations</li>
 * <li>ChordInter vs its notes and stem</li>
 * </ul>
 *
 * @see InterMutableEnsemble
 *
 * @author Hervé Bitteur
 */
public interface InterEnsemble
        extends Inter
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the list of ensemble members.
     *
     * @return the members
     */
    List<? extends Inter> getMembers ();
}
