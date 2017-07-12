//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S i g A t t i c                                        //
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
package org.audiveris.omr.sig;

import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Relation;

import org.jgrapht.graph.Multigraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class {@code SigAttic} is a graph used as a temporary storage for some inters of a
 * sig, as well as all the relations these inters are involved in.
 *
 * @author Hervé Bitteur
 */
public class SigAttic
        extends Multigraph<Inter, Relation>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SigAttic.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SigAttic} object.
     */
    public SigAttic ()
    {
        super(Relation.class);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // restore //
    //---------//
    /**
     * Restore from this attic to sig the provided collection of Inter instances, as
     * well as the relations they are involved in.
     *
     * @param sig   the sig to partially restore
     * @param seeds the collection of primary Inter instances to restore
     */
    public void restore (SIGraph sig,
                         Collection<Inter> seeds)
    {
        // Include chord notes as well
        Set<Inter> vertices = new LinkedHashSet<Inter>(seeds);

        for (Inter inter : seeds) {
            if (inter instanceof AbstractChordInter) {
                vertices.addAll(((AbstractChordInter) inter).getNotes());
            }
        }

        // Restore primary inter instances
        for (Inter inter : vertices) {
            sig.addVertex(inter);
        }

        // Restore relations from seeds
        for (Inter inter : seeds) {
            for (Relation rel : edgesOf(inter)) {
                Inter source = getEdgeSource(rel);
                Inter target = getEdgeTarget(rel);

                if (inter == source) {
                    if (sig.containsVertex(target)) {
                        sig.addEdge(inter, target, rel);
                    }
                } else if (sig.containsVertex(source)) {
                    sig.addEdge(source, inter, rel);
                }
            }
        }
    }

    //------//
    // save //
    //------//
    /**
     * Save from sig to this attic a collection of Inter instances, as well as the
     * relations these inters are involved in, which may sometimes also require to
     * save secondary inters that are located on the opposite side of a relation.
     *
     * @param sig   the sig to partially backup
     * @param seeds the collection of primary Inter instances to save
     */
    public void save (SIGraph sig,
                      Collection<Inter> seeds)
    {
        // Save needed vertices
        Set<Inter> vertices = new LinkedHashSet<Inter>(seeds);

        for (Inter seed : seeds) {
            if (seed instanceof AbstractChordInter) {
                // Include chord notes as well
                vertices.addAll(((AbstractChordInter) seed).getNotes());
            }
        }

        for (Inter vertex : vertices) {
            addVertex(vertex);
        }

        // Save involved relations as edges (including linked vertices)
        for (Inter seed : seeds) {
            for (Relation rel : sig.edgesOf(seed)) {
                Inter source = sig.getEdgeSource(rel);
                Inter target = sig.getEdgeTarget(rel);

                // Save secondary Inter if so needed (just to allow the relation)
                if (seed == source) {
                    addVertex(target);
                } else {
                    addVertex(source);
                }

                // Save relation
                addEdge(source, target, rel);
            }
        }
    }
}
