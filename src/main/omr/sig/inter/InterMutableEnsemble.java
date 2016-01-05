//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             I n t e r M u t a b l e E n s e m b l e                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

/**
 * Interface {@code InterMutableEnsemble} is a mutable {@link InterEnsemble}, with the
 * ability to add or remove members.
 *
 * @author Hervé Bitteur
 */
public interface InterMutableEnsemble
        extends InterEnsemble
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Add a member to the mutable ensemble.
     *
     * @param member the member to add
     */
    void addMember (Inter member);

    /**
     * Remove a member from the mutable ensemble.
     *
     * @param member the member to remove
     */
    void removeMember (Inter member);
}
