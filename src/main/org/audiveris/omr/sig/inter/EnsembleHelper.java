//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   E n s e m b l e H e l p e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.SigListener;
import org.audiveris.omr.sig.relation.BasicContainment;
import org.audiveris.omr.sig.relation.Relation;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code EnsembleHelper} gathers helping methods for ensembles.
 *
 * @author Hervé Bitteur
 */
public abstract class EnsembleHelper
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(EnsembleHelper.class);

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // addMember //
    //-----------//
    /**
     * Set the containment relationship between an (ensemble) instance and a (member)
     * instance.
     * <p>
     * Both instances must already exist in SIG.
     * <p>
     * This will trigger {@link SigListener#edgeAdded(GraphEdgeChangeEvent) } in
     * any SIG listener.
     *
     * @param ensemble the containing inter
     * @param member   the contained inter
     */
    public static void addMember (InterEnsemble ensemble,
                                  Inter member)
    {
        SIGraph sig = ensemble.getSig();
        sig.addEdge(ensemble, member, new BasicContainment());
    }

    //------------//
    // getMembers //
    //------------//
    /**
     * Report the sorted list of current member instances for a provided ensemble.
     *
     * @param ensemble   the containing inter
     * @param comparator the comparator to sort the list of members
     * @return the members list, perhaps empty but not null
     */
    public static List<Inter> getMembers (InterEnsemble ensemble,
                                          Comparator<Inter> comparator)
    {
        SIGraph sig = ensemble.getSig();

        if (sig == null) {
            logger.debug("Ensemble#{} not in sig", ensemble.getId());

            return Collections.EMPTY_LIST;
        }

        List<Inter> members = new ArrayList<Inter>();

        if (sig.containsVertex(ensemble)) {
            for (Relation rel : sig.getRelations(ensemble, BasicContainment.class)) {
                members.add(sig.getOppositeInter(ensemble, rel));
            }

            if (!members.isEmpty()) {
                Collections.sort(members, comparator);
            }
        }

        return members;
    }

    //----------------//
    // linkOldMembers //
    //----------------//
    /**
     * Convert old containment implementation (based on nesting) to new implementation
     * based on explicit BasicContainment in SIG.
     *
     * @param ensemble   the containing inter
     * @param oldMembers the (unmarshalled) old list of nested members
     */
    @Deprecated
    public static void linkOldMembers (InterEnsemble ensemble,
                                       List<? extends Inter> oldMembers)
    {
        if ((oldMembers != null) && !oldMembers.isEmpty()) {
            for (Inter member : oldMembers) {
                ensemble.addMember(member);
            }
        }
    }

    //--------------//
    // removeMember //
    //--------------//
    /**
     * Remove the containment relationship between an (ensemble) instance and a (member)
     * instance.
     * <p>
     * Both instances must exist in SIG.
     * <p>
     * This will trigger {@link SigListener#edgeRemoved(GraphEdgeChangeEvent)} in any SIG listener.
     *
     * @param ensemble the containing inter
     * @param member   the contained inter
     */
    public static void removeMember (InterEnsemble ensemble,
                                     Inter member)
    {
        SIGraph sig = ensemble.getSig();
        sig.removeEdge(ensemble, member);
    }
}
