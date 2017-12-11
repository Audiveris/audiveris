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
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;

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
        super(inter.getSig(), inter, inter.getBounds(), null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void performDo ()
    {
        links = linksOf(inter);
        inter.remove(false);
    }

    @Override
    public void performRedo ()
    {
        performDo();
    }

    @Override
    public void performUndo ()
    {
        inter.setBounds(initialBounds);
        sig.addVertex(inter);

        for (Link link : links) {
            link.applyTo(inter);
        }
    }

    @Override
    protected String actionName ()
    {
        return "del";
    }

    /**
     * Retrieve the current links around the provided inter.
     *
     * @param inter the provided inter
     * @return its links, perhaps empty
     */
    private static Collection<Link> linksOf (Inter inter)
    {
        final SIGraph sig = inter.getSig();
        Set<Link> links = null;

        for (Relation rel : sig.edgesOf(inter)) {
            if (links == null) {
                links = new LinkedHashSet<Link>();
            }

            Inter partner = sig.getOppositeInter(inter, rel);

            links.add(new Link(
                            sig.getOppositeInter(inter, rel),
                            rel,
                            sig.getEdgeTarget(rel) == partner));
        }

        if (links == null) {
            return Collections.emptySet();
        }

        return links;
    }
}
