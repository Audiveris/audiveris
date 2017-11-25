//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          R e l a t i o n                                       //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;

import org.jgrapht.event.GraphEdgeChangeEvent;

/**
 * Interface {@code Relation} describes a relation between two Interpretation instances.
 *
 * @author Hervé Bitteur
 */
public interface Relation
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Clone a relation.
     *
     * @return the cloned relation
     */
    Relation duplicate ();

    /**
     * Notifies that this edge has been added to the graph.
     *
     * @param e the edge event.
     */
    void added (GraphEdgeChangeEvent<Inter, Relation> e);

    /**
     * Notifies that this edge has been removed from the graph.
     *
     * @param e the edge event.
     */
    void removed (GraphEdgeChangeEvent<Inter, Relation> e);

    /**
     * Details for tip.
     *
     * @return relation details
     */
    String getDetails ();

    /**
     * Short name.
     *
     * @return the relation short name
     */
    String getName ();

    /**
     * Relation description when seen from one of its involved inters
     *
     * @param inter the interpretation point of view
     * @return the inter-based description
     */
    String seenFrom (Inter inter);

    /**
     * Report a long description of the relation
     *
     * @param sig the containing sig
     * @return long description
     */
    String toLongString (SIGraph sig);
}
