//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          V e r t e x                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.graph;

import java.util.List;

/**
 * Interface {@code Vertex} encapsulates a Vertex (or Node) in a directed graph.
 * <p>
 * Any vertex can have incoming edges from other vertices and outgoing edges to other vertices.
 * <p>
 * The Vertex can have a list of related {@code VertexView}'s.
 * All the vertices in the graph have parallel lists of {@code VertexView}'s as the Digraph itself
 * which has a parallel list of {@code DigraphView}'s.
 *
 * @param <D> type for enclosing digraph precise subtype
 * @param <V> type for Vertex precise subtype
 *
 * @author Hervé Bitteur
 */
public interface Vertex<D extends Digraph, V extends Vertex<D, V>>
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Create an edge between this vertex and the target vertex
     *
     * @param target arrival vertex
     */
    public void addTarget (V target);

    /**
     * Delete this vertex.
     * This implies also the removal of all its incoming and outgoing edges.
     */
    public void delete ();

    /**
     * Prints on standard output a detailed information about this vertex.
     */
    public void dump ();

    /**
     * Report the containing graph of this vertex
     *
     * @return the containing graph
     */
    public D getGraph ();

    /**
     * Report the unique Id (within the containing graph) of this
     * vertex.
     *
     * @return the id
     */
    public int getId ();

    /**
     * Return how many incoming edges we have
     *
     * @return the number of incomings
     */
    public int getInDegree ();

    /**
     * Return the number of edges outgoing from this vertex
     *
     * @return the number of outgoings
     */
    public int getOutDegree ();

    /**
     * An access to incoming vertices
     *
     * @return the incoming vertices
     */
    public List<V> getSources ();

    /**
     * Return an access to the outgoing vertices of this vertex
     *
     * @return the outgoing vertices
     */
    public List<V> getTargets ();

    /**
     * Remove an edge between this vertex and a target vertex
     *
     * @param target arrival vertex
     * @param strict throw RuntimeException if the edge does not exist
     */
    public void removeTarget (V target,
                              boolean strict);

    /**
     * Assign the containing graph of this vertex
     *
     * @param graph The hosting graph
     */
    public void setGraph (D graph);

    /**
     * Assign a new Id (for expert use only)
     *
     * @param id The assigned id
     */
    public void setId (int id);
}
