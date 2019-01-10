//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    I n t e r E n s e m b l e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig.inter;

import java.util.List;

/**
 * Interface {@code InterEnsemble} refers to an inter that is composed of other inters,
 * with the ability to add or remove members.
 * <p>
 * Examples are:
 * <ul>
 * <li>Sentence vs words (and LyricLine vs LyricItems)</li>
 * <li>TimePairInter vs num and den</li>
 * <li>KeyInter vs its alterations</li>
 * <li>ChordInter vs its notes</li>
 * <li>FermataInter vs its arc and dot</li>
 * </ul>
 * <p>
 * Such ensembles cannot durably exist and be fully defined without their members: for example, a
 * {@link SentenceInter} instance cannot exist without at least one member {@link WordInter}
 * instance.
 * <p>
 * Hence, care must be taken to avoid such "empty ensembles":
 * <ul>
 * <li>Sentence creation must be followed by inclusion of a word.
 * <li>Deletion of sole word of a sentence must be followed by sentence deletion.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public interface InterEnsemble
        extends Inter
{

    /**
     * Add a member to the ensemble.
     * <p>
     * Both the ensemble and the member instances must already exist in the SIG.
     * <p>
     * Setting this containment relationship will, via the SIG listener, trigger
     * {@link #invalidateCache()} in the ensemble.
     *
     * @param member the member to add
     */
    void addMember (Inter member);

    /**
     * Report the list of members of the ensemble.
     * <p>
     * The ensemble instance (and its members if any) must already exist in the SIG.
     *
     * @return the members
     */
    List<Inter> getMembers ();

    /**
     * Remove a member from the ensemble.
     * <p>
     * This does not delete the member instance, but simply deletes the containment relationship
     * between the ensemble instance and the member instance.
     * <p>
     * On the opposite, directly deleting a (member) instance, automatically triggers the removal of
     * this member from any containing ensemble.
     * <p>
     * Deleting this containment relationship will, via the SIG listener, trigger
     * {@link #invalidateCache()} in the ensemble.
     *
     * @param member the member to remove
     */
    void removeMember (Inter member);
}
