//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   E n s e m b l e H e l p e r                                  //
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

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.SigListener;
import org.audiveris.omr.sig.relation.Containment;
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

    private static final Logger logger = LoggerFactory.getLogger(EnsembleHelper.class);

    /** Not meant to be instantiated. */
    private EnsembleHelper ()
    {
    }

    //-----------//
    // addMember //
    //-----------//
    /**
     * Set the containment relationship between an (ensemble) instance and a (member)
     * instance, if not already set.
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
        final SIGraph sig = ensemble.getSig();

        // Set membership if not already in place
        if (sig.getRelation(ensemble, member, Containment.class) == null) {
            sig.addEdge(ensemble, member, new Containment());
        }
    }

    //------------//
    // getMembers //
    //------------//
    /**
     * Report the sorted list of current member instances for a provided ensemble.
     *
     * @param ensemble   the containing inter
     * @param comparator optional comparator to sort the list of members, can be null
     * @return the members list, perhaps empty but not null
     */
    public static List<Inter> getMembers (InterEnsemble ensemble,
                                          Comparator<Inter> comparator)
    {
        final SIGraph sig = ensemble.getSig();

        if (sig == null) {
            logger.debug("Ensemble#{} not in sig", ensemble.getId());

            return Collections.emptyList();
        }

        final List<Inter> members = new ArrayList<>();

        if (sig.containsVertex(ensemble)) {
            for (Relation rel : sig.outgoingEdgesOf(ensemble)) {
                if (rel instanceof Containment) {
                    members.add(sig.getEdgeTarget(rel));
                }
            }

            if (!members.isEmpty() && (comparator != null)) {
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
     * based on explicit Containment in SIG.
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
        final SIGraph sig = ensemble.getSig();
        sig.removeEdge(ensemble, member);
    }

    //----------------------------//
    // computeMeanContextualGrade //
    //----------------------------//
    /**
     * Compute contextual grade of each ensemble member, then report the mean value.
     *
     * @param ensemble the containing inter
     * @return mean contextual grade, or null if no value could be computed
     */
    public static Double computeMeanContextualGrade (InterEnsemble ensemble)
    {
        final SIGraph sig = ensemble.getSig();

        if ((sig != null) && sig.containsVertex(ensemble)) {
            return Inters.computeMeanContextualGrade(ensemble.getMembers());
        }

        return null;
    }
}
