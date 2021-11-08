//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  R e m o v a l S c e n a r i o                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.UITaskList.Option;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class <code>RemovalScenario</code> defines order of inter removals.
 *
 * @author Hervé Bitteur
 */
public class RemovalScenario
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RemovalScenario.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Non-ensemble inters to be removed. */
    LinkedHashSet<Inter> inters = new LinkedHashSet<>();

    /** Ensemble inters to be removed. */
    LinkedHashSet<InterEnsemble> ensembles = new LinkedHashSet<>();

    /** Ensemble inters to be watched for potential removal. */
    LinkedHashSet<InterEnsemble> watched = new LinkedHashSet<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates an <code>RemovalScenario</code> object.
     */
    public RemovalScenario ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // populate //
    //----------//
    /**
     * Populate the removal scenario.
     *
     * @param inters the inters to remove
     * @param seq    the task sequence to append to
     */
    public void populate (Collection<? extends Inter> inters,
                          UITaskList seq)
    {
        for (Inter inter : inters) {
            if (inter.isRemoved()) {
                continue;
            }

            if (inter.isVip()) {
                logger.info("VIP removeInter {}", inter);
            }

            // Removals: this inter plus related inters to remove as well
            WrappedBoolean cancel = seq.isOptionSet(Option.VALIDATED)
                    ? null
                    : new WrappedBoolean(false);
            Set<? extends Inter> toRemove = inter.preRemove(cancel);

            if ((cancel != null) && cancel.isSet()) {
                seq.setCancelled(true);
                return;
            }

            for (Inter item : toRemove) {
                include(item);
            }
        }

        // Now set the removal tasks
        populateTaskList(seq);
    }

    //---------//
    // include //
    //---------//
    private void include (Inter inter)
    {
        if (inter instanceof InterEnsemble) {
            // Include the ensemble and its members
            final InterEnsemble ens = (InterEnsemble) inter;
            final List<Inter> members = ens.getMembers();

            if (members.isEmpty()) {
                inters.add(inter);
            } else {
                ensembles.add(ens);
                inters.addAll(members);
            }
        } else {
            inters.add(inter);
            // Watch the containing ensemble (if not already to be removed)
            final SIGraph sig = inter.getSig();

            for (Relation rel : sig.incomingEdgesOf(inter)) {
                if (rel instanceof Containment) {
                    final InterEnsemble ens = (InterEnsemble) sig.getEdgeSource(rel);

                    if (!ensembles.contains(ens)) {
                        watched.add(ens);
                    }
                }
            }
        }
    }

    //------------------//
    // populateTaskList //
    //------------------//
    /**
     * Populate the operational task list.
     *
     * @param seq the task list to populate
     */
    private void populateTaskList (UITaskList seq)
    {
        // Examine watched ensembles
        for (InterEnsemble ens : watched) {
            List<Inter> members = new ArrayList<>(ens.getMembers());
            members.removeAll(inters);

            if (members.isEmpty()) {
                ensembles.add(ens); // This now empty ensemble is to be removed as well
            }
        }

        // Ensembles to remove first
        List<InterEnsemble> sortedEnsembles = new ArrayList<>(ensembles);
        Collections.sort(sortedEnsembles, Inters.membersFirst);
        Collections.reverse(sortedEnsembles);

        for (InterEnsemble ens : sortedEnsembles) {
            seq.add(new RemovalTask(ens));
        }

        // Simple inters to remove second
        for (Inter inter : inters) {
            seq.add(new RemovalTask(inter));
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("RemovalScenario{");
        sb.append("ensembles:").append(ensembles);
        sb.append(" inters:").append(inters);
        sb.append("}");
        return sb.toString();
    }
}
