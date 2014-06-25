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
package omr.sig;

import java.util.List;

/**
 * Interface {@code InterEnsemble} refers to an inter that is composed of other inters.
 * <p>
 * For example a {@link KeyInter} is an ensemble of {@link KeyAlterInter}, and a
 * {@link TimePairInter} is an ensemble of two {@link TimeNumberInter} (num & den).
 *
 * @author Hervé Bitteur
 */
public interface InterEnsemble
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the list of ensemble members.
     *
     * @return the members
     */
    List<?extends Inter> getMembers ();
}
