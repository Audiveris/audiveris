//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S i g A t t i c                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.Inter;
import omr.sig.relation.Relation;

import org.jgrapht.graph.Multigraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
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
        Set<Inter> vertices = new HashSet<Inter>(seeds);

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
        Set<Inter> vertices = new HashSet<Inter>(seeds);

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
