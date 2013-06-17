//----------------------------------------------------------------------------//
//                                                                            //
//                               D i g r a p h                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.graph;

import java.util.Collection;

/**
 * Class {@code Digraph} handles a directed graph, a structure
 * containing an <b>homogeneous</b> collection of instances of Vertex
 * (or a collection of homogeneous types derived from Vertex),
 * potentially linked by directed edges.
 * <p/>
 *
 * <p> Vertices can exist in isolation, but an edge can exist only from a vertex
 * to another vertex. Thus, removing a vertex implies removing all its incoming
 * and outgoing edges.
 *
 * <p><b>NOTA</b>: Since we have no data to carry in edges, there is no
 * {@code Edge} type per se, links between vertices are implemented simply
 * by Lists of Vertex.
 *
 * @param <D> precise type for digraph (which is pointed back by vertex)
 * @param <V> precise type for vertices handled by this digraph
 *
 * @author Hervé Bitteur
 */
public interface Digraph<D extends Digraph<D, V>, V extends Vertex>
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Add a vertex in the graph, the vertex is being assigned a
     * unique id by the graph.
     * (package access from {@link Vertex})
     * Made public just for access from glyph Verifier
     *
     * @param vertex the newly created vertex
     */
    void addVertex (V vertex);

    /**
     * Create a new vertex in the graph, using the provided vertex
     * class.
     *
     * @return the vertex created
     */
    V createVertex ();

    /**
     * A dump of the graph content, vertex by vertex
     *
     * @param title The title to be printed before the dump, or null
     */
    void dump (String title);

    /**
     * Give access to the last id assigned to a vertex in this graph.
     * This may be greater than the number of vertices currently in the graph,
     * because of potential deletion of vertices (a Vertex Id is never reused).
     *
     * @return id of the last vertex created
     * @see #getVertexCount
     */
    int getLastVertexId ();

    /**
     * Report the name assigned to this graph instance
     *
     * @return the readable name
     */
    String getName ();

    /**
     * Retrieve a vertex knowing its id
     *
     * @param id the vertex id
     * @return the vertex found, or null
     */
    V getVertexById (int id);

    /**
     * Give the number of vertices currently in the graph.
     *
     * @return the number of vertices
     * @see #getLastVertexId
     */
    int getVertexCount ();

    /**
     * Export an unmodifiable and non-sorted collection of vertices of
     * the graph
     *
     * @return the unmodifiable collection of vertices
     */
    Collection<V> getVertices ();

    /**
     * (package access from Vertex) to remove the vertex from the
     * graph, the removed vertex will now be stored in the oldVertices
     * map.
     *
     * @param vertex the vertex to be removed
     */
    void removeVertex (V vertex);

    /**
     * Restore an old vertex
     *
     * @param vertex the old vertex to restore
     */
    void restoreVertex (V vertex);
}
