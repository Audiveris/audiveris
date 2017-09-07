//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R e m o v a l T a s k                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.sig.inter.InterMutableEnsemble;
import org.audiveris.omr.sig.relation.AbstractContainment;
import org.audiveris.omr.sig.relation.Partnership;
import org.audiveris.omr.sig.relation.Relation;

import org.jdesktop.application.Task;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class {@code RemovalTask} removes an inter (with its relations).
 *
 * @author Hervé Bitteur
 */
public class RemovalTask
        extends InterTask
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code RemovalTask} object.
     *
     * @param inter the inter to remove
     */
    public RemovalTask (Inter inter)
    {
        super(inter.getSig(), inter, partnershipsOf(inter));
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public Task<Void, Void> performDo ()
    {
        for (Partnership partnership : partnerships) {
            if (partnership.relation instanceof AbstractContainment) {
                InterMutableEnsemble ime = (InterMutableEnsemble) (partnership.outgoing ? inter
                        : partnership.partner);
                Inter member = partnership.outgoing ? partnership.partner : inter;
                ime.removeMember(member);
            }
        }

        inter.delete(false);

        return null;
    }

    @Override
    public Task<Void, Void> performRedo ()
    {
        return performDo();
    }

    @Override
    public Task<Void, Void> performUndo ()
    {
        sig.addVertex(inter);

        for (Partnership partnership : partnerships) {
            partnership.applyTo(inter);
        }

        return null;
    }

    @Override
    protected String actionName ()
    {
        return "removal";
    }

    /**
     * Retrieve the current partnerships around the provided inter.
     *
     * @param inter the provided inter
     * @return its partnerships, perhaps empty
     */
    private static Collection<Partnership> partnershipsOf (Inter inter)
    {
        final SIGraph sig = inter.getSig();
        Set<Partnership> partnerships = null;

        for (Relation rel : sig.edgesOf(inter)) {
            if (partnerships == null) {
                partnerships = new LinkedHashSet<Partnership>();
            }

            Inter partner = sig.getOppositeInter(inter, rel);

            partnerships.add(
                    new Partnership(
                            sig.getOppositeInter(inter, rel),
                            rel,
                            sig.getEdgeTarget(rel) == partner));
        }

        if (partnerships == null) {
            return Collections.emptySet();
        }

        return partnerships;
    }
}
