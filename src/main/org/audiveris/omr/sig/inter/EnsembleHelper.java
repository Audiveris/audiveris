//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   E n s e m b l e H e l p e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.sig.relation.ContainmentRelation;
import org.audiveris.omr.sig.relation.Relation;

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
    //~ Methods ------------------------------------------------------------------------------------

    public static void addMember (InterEnsemble ensemble,
                                  Inter member)
    {
    }

    public static void addMember (InterEnsemble ensemble,
                                  Inter member,
                                  ContainmentRelation relation)
    {
        SIGraph sig = ensemble.getSig();
        sig.addEdge(ensemble, member, (relation != null) ? relation : new ContainmentRelation());
        member.setEnsemble(ensemble); // Deprecated?
    }

    public static List<Inter> getMembers (InterEnsemble ensemble,
                                          Comparator<Inter> comparator)
    {
        SIGraph sig = ensemble.getSig();
        List<Inter> members = new ArrayList<Inter>();

        for (Relation rel : sig.getRelations(ensemble, ContainmentRelation.class)) {
            members.add(sig.getOppositeInter(ensemble, rel));
        }

        if (!members.isEmpty()) {
            Collections.sort(members, comparator);
        }

        return members;
    }

    public static void linkOldMembers (InterEnsemble ensemble,
                                       List<? extends Inter> oldMembers)
    {
        if ((oldMembers != null) && !oldMembers.isEmpty()) {
            for (Inter member : oldMembers) {
                ensemble.addMember(member);
            }
        }
    }

    public static void removeMember (InterEnsemble ensemble,
                                     Inter member)
    {
        SIGraph sig = ensemble.getSig();
        sig.removeEdge(ensemble, member);
        member.setEnsemble(null); // Deprecated?
    }
}
