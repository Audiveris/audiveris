//-----------------------------------------------------------------------//
//                                                                       //
//                              V e r t e x                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.graph;

import omr.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>Vertex</code> encapsulates a Vertex (or Node) in a directed
 * graph. Any vertex can have incoming edges from other vertices and
 * outgoing edges to other vertices.
 *
 * <p>The Vertex can have a list of related <code>VertexView</code>s. All
 * the vertices in the graph have parallel lists of
 * <code>VertexView</code>'s as the Digraph itself which has a parallel
 * list of <code>DigraphView</code>'s.
 *
 * <p>NOTA: This plain Vertex type has no room for user-defined data, if
 * such feature is needed then a proper subtype of Vertex should be used.
 *
 * @param <D> type for enclosing digraph precise subtype
 * @param <V> type for Vertex precise subtype
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Vertex <D extends Digraph,
                     V extends Vertex  <D, V>>
    implements java.io.Serializable
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Vertex.class);

    /** Compilation flag to forbid duplication of edges : {@value} */
    public static final boolean NO_EDGE_DUPLICATES = true;

    //~ Instance variables ------------------------------------------------

    /**
     * Incoming edges from other vertices
     */
    protected final List<V> sources = new ArrayList<V>();

    /**
     * Outgoing edges to other vertices
     */
    protected final List<V> targets = new ArrayList<V>();

    /**
     * Containing graph
     */
    protected D graph;

    /**
     * Unique vertex Id (for debugging mainly)
     */
    protected int id;

    /**
     * Collection of views created on this vertex
     */
    protected transient List<VertexView> views;

    //~ Constructors ------------------------------------------------------

    //--------//
    // Vertex //
    //--------//
    /**
     * Create a Vertex.
     */
    protected Vertex ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("new vertex");
        }
    }

    //--------//
    // Vertex //
    //--------//
    /**
     * Create a Vertex in a graph.
     * @param graph The containing graph where this vertex is to be hosted
     */
    protected Vertex (D graph)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("new vertex in graph " + graph);
        }
        //this.graph = graph;
        graph.addVertex(this);          // Compiler warning here
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // getGraph //
    //----------//
    /**
     * Report the containing graph of this vertex
     *
     * @return the containing graph
     */
    public D getGraph ()
    {
        return graph;
    }

    //----------//
    // setGraph //
    //----------//
    /**
     * (package access from graph) Assign the containing graph of this
     * vertex
     * @param graph The hosting graph
     */
    public void setGraph (D graph)
    {
        this.graph = graph;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the unique Id (within the containing graph) of this vertex
     *
     * @return the id
     */
    public int getId ()
    {
        return id;
    }

    //-------//
    // setId //
    //-------//
    /**
     * Assign a new Id (for expert use only)
     *
     * @param id The assigned id
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //-------------//
    // getInDegree //
    //-------------//
    /**
     * Return how many incoming edges we have
     *
     * @return the number of incomings
     */
    public int getInDegree ()
    {
        return sources.size();
    }

    //--------------//
    // getOutDegree //
    //--------------//
    /**
     * Return the number of edges outgoing from this vertex
     *
     * @return the number of outgoings
     */
    public int getOutDegree ()
    {
        return targets.size();
    }

    //----------//
    // getViews //
    //----------//
    /**
     * Pointers to the related view if any
     *
     * @return the view collection
     */
    public List<VertexView> getViews ()
    {
        if (views == null) {
            views = new ArrayList<VertexView>();
        }

        return views;
    }

    //---------//
    // addView //
    //---------//
    /**
     * Add the related view of this vertex
     *
     * @param view the view to be linked
     */
    public void addView (VertexView view)
    {
        getViews().add(view);
    }

    //------//
    // dump //
    //------//
    /**
     * Prints on standard output a detailed information about this vertex.
     */
    public void dump ()
    {
        // The vertex
        System.out.print(" ");
        System.out.println(this);

        // The in edges
        for (V vertex : sources) {
            System.out.println("  - edge from " + vertex);
        }

        // The out edges
        for (V vertex : targets) {
            System.out.println("  + edge to   " + vertex);
        }
    }

    //-----------//
    // getPrefix //
    //-----------//
    /**
     * Return a distinctive string, to be used as a prefix in toString()
     * for example.
     *
     * @return the prefix string
     */
    protected String getPrefix ()
    {
        return "Vertex";
    }

    //------------//
    // getSources //
    //------------//
    /**
     * An access to incoming vertices
     *
     * @return the incoming vertices
     */
    public List<V> getSources ()
    {
        return sources;
    }

    //------------//
    // getTargets //
    //------------//
    /**
     * Return an access to the outgoing vertices of this vertex
     *
     * @return the outgoing vertices
     */
    public List<V> getTargets ()
    {
        return targets;
    }

    //--------//
    // delete //
    //--------//
    /**
     * Delete this vertex. This implies also the removal of all its
     * incoming and outgoing edges.
     */
    public void delete ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("deleting vertex " + this);
        }

        // Remove in vertices of the vertex
        for (V source : sources) {
            source.targets.remove(this);
        }

        // Remove out vertices of the vertex
        for (V target : targets) {
            target.sources.remove(this);
        }

        // Remove from graph
        graph.removeVertex(this);       // Compiler warning here
    }

    //----------//
    // toString //
    //----------//
    /**
     * Return a readable description of the vertex
     *
     * @return the string
     */
    @Override
        public String toString ()
    {
        StringBuffer sb = new StringBuffer(25);

        sb.append("{").append(getPrefix()).append("#").append(id);
        sb.append(" ").append(getInDegree());
        sb.append("/").append(getOutDegree());

        if (this.getClass().getName() == Vertex.class.getName()) {
            sb.append("}");
        }

        return sb.toString();
    }

    //---------//
    // addEdge //
    //---------//
    /**
     * Create an edge between two vertices
     *
     * @param source departure vertex
     * @param target arrival vertex
     */
    public static <V extends Vertex<D, V>,
                   D extends Digraph<D, V>> void addEdge (V source,
                                                          V target)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("adding edge from " + source + " to " + target);
        }

        // Assert we have real vertices
        if (source == null || target == null) {
            throw new IllegalArgumentException
                ("At least one of the edge vertices is null");
        }

        // Assert they belong to the same graph
        if (source.getGraph() != target.getGraph()) {
            throw new RuntimeException
                ("An edge can link vertices of the same graph only");
        }

        if (NO_EDGE_DUPLICATES) {
            source.targets.remove(target);
            target.sources.remove(source);
        }

        source.targets.add(target);
        target.sources.add(source);
    }

    //------------//
    // removeEdge //
    //------------//
    /**
     * Remove an edge between two vertices
     *
     * @param source departure vertex
     * @param target arrival vertex
     */
    public static <V extends Vertex> void removeEdge (V source,
                                                      V target)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("removing edge from " + source + " to " + target);
        }

        if (!source.targets.remove(target)) {
            throw new RuntimeException
                ("Attempting to remove non-existing edge between " +
                 source + " and " + target);
        }

        if (!target.sources.remove(source)) {
            throw new RuntimeException
                ("Attempting to remove non-existing edge between " +
                 source + " and " + target);
        }
    }
}
