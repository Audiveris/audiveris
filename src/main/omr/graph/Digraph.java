//-----------------------------------------------------------------------//
//                                                                       //
//                             D i g r a p h                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.graph;

import omr.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class <code>Digraph</code> handles a directed graph, a structure
 * containing an <b>homogeneous</b> collection of instances of Vertex (or a
 * collection of homogeneous types derived from Vertex), potentially linked
 * by directed edges.  <p/>
 *
 * <p> Vertices can exist in isolation, but an edge can exist only from a
 * vertex to another vertex. Thus, removing a vertex implies removing all
 * its incoming and outgoing edges.
 *
 * <p><b>NOTA</b>: Since we have no data to carry in edges, there is no
 * <code>Edge</code> type per se, links between vertices are implemented
 * simply by Lists of Vertex.
 *
 * @param <D> precise (sub)type for the graph
 * @param <V> precise type for handled vertices
 */
public class Digraph <D extends Digraph <D, V>,
                      V extends Vertex>
    implements java.io.Serializable
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Digraph.class);

    //~ Instance variables ------------------------------------------------

    /**
     * Related Vertex (sub)class, to create vertices of the proper type
     */
    private Class<? extends V> vertexClass;

    /**
     * All Vertices of the graph
     */
    protected final SortedMap<Integer, V> vertices
        = new TreeMap<Integer, V>();

    /**
     * Global id to uniquely identify a vertex
     */
    protected int globalVertexId = 0;

    /**
     * All Views on this graph
     */
    protected transient List<DigraphView> views;

    /**
     * Name for debugging
     */
    protected String id;

    //~ Constructors ------------------------------------------------------

    //---------//
    // Digraph //
    //---------//
    /**
     * Default constructor.
     */
    public Digraph ()
    {
    }

    //~ Methods -----------------------------------------------------------

    //-------//
    // setId //
    //-------//
    /**
     * Assign an id to the graph (debug)
     *
     * @param id the new id
     */
    public void setId (String id)
    {
        this.id = id;
    }

    //----------------//
    // setVertexClass //
    //----------------//
    /**
     * Assigned the vertexClass (needed to create new vertices in the
     * graph).  This is meant to complement the default constructor.
     *
     * @param vertexClass the class to be used when instantiating vertices
     */
    public void setVertexClass (Class<? extends V> vertexClass)
    {
        this.vertexClass = vertexClass;
    }

    //-----------------//
    // getLastVertexId //
    //-----------------//
    /**
     * Give access to the last id assigned to a vertex in this graph. This
     * may be greater than the number of vertices currently in the graph,
     * because of potential deletion of vertices (Vertex Id is never
     * reused).
     *
     * @return id of the last vertex created
     * @see #getVertexCount
     */
    public int getLastVertexId ()
    {
        return globalVertexId;
    }

    //---------------//
    // getVertexById //
    //---------------//
    /**
     * Retrieve a vertex knowing its id
     *
     * @param id the vertex id
     * @return the vertex found, or null
     */
    public V getVertexById(int id)
    {
        return vertices.get(id);
    }

    //----------------//
    // getVertexCount //
    //----------------//
    /**
     * Give the number of vertices currently in the graph.
     *
     * @return the number of vertices
     * @see #getLastVertexId
     */
    public int getVertexCount ()
    {
        return vertices.size();
    }

    //----------//
    // getViews //
    //----------//
    /**
     * Give access to the ordered collection of related views if any
     *
     * @return the views
     */
    public List<DigraphView> getViews ()
    {
        if (views == null) {
            views = new ArrayList<DigraphView>();
        }

        return views;
    }

    //---------//
    // addView //
    //---------//
    /**
     * Add a view related to the graph
     *
     * @param view the view
     */
    public void addView (DigraphView view)
    {
        getViews().add(view);
    }

    //------//
    // dump //
    //------//
    /**
     * A dump of the graph content, vertex by vertex
     *
     * @param title The title to be printed before the dump, or null
     */
    public void dump (String title)
    {
        if (title != null) {
            System.out.println(title);
        }

        System.out.println(this);

        for (V vertex : getVertices()) {
            vertex.dump();
        }
    }

    //----------//
    // toString //
    //----------//
    /**
     * A readable description of the graph
     *
     * @return the string
     */
    @Override
        public String toString ()
    {
        StringBuffer sb = new StringBuffer(25);

        sb.append("{").append(getPrefix());

        if (id != null) {
            sb.append("#").append(id);
        }

        sb.append(" ").append(getVertexCount()).append(" vertices");

        if (this.getClass().getName() == DigraphView.class.getName()) {
            sb.append("}");
        }

        return sb.toString();
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
        return "Digraph";
    }

    //-------------//
    // getVertices //
    //-------------//
    /**
     * Export the vertices of the graph. TBD: should be unmutable ?
     *
     * @return the collection of vertices
     */
    public Collection<V> getVertices ()
    {
        return vertices.values();
    }

    //-----------//
    // addVertex //
    //-----------//
    /**
     * (package access from {@link Vertex}) to add a vertex in the graph
     *
     * @param vertex the newly created vertex
     *
     * @return the unique vertex id in the graph
     */
    public int addVertex (V vertex)     // HB PUBLIC just for Verifier
    {
        vertices.put(++globalVertexId, vertex);
        return globalVertexId;
    }

    //--------------//
    // createVertex //
    //--------------//
    /**
     * Create a new vertex in the graph, using the provided vertex class
     *
     * @return the vertex created
     */
    public V createVertex ()
    {
        V vertex;
        try {
            vertex = vertexClass.newInstance();
            vertex.setId(addVertex(vertex));
            vertex.setGraph(this);
            return vertex;
        } catch (NullPointerException ex) {
            logger.severe("Digraph cannot create vertex, vertexClass not set");
            return null;
        } catch (InstantiationException ex) {
            logger.severe("Cannot createVertex with an abstract class or interface: " + vertexClass);
            return null;
        } catch (IllegalAccessException ex) {
            System.err.println("IllegalAccessException occurred in createVertex");
            ex.printStackTrace();
            return null;
        }
    }

    //--------------//
    // removeVertex //
    //--------------//
    /**
     * (package access from Vertex) to remove the vertex from the graph
     *
     * @param vertex the vertex to be removed
     */
    void removeVertex (V vertex)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("remove " + vertex);
        }
        if (vertices.remove(vertex.getId()) == null) {
            logger.severe("removeVertex. vertex not found: " + vertex);
        }
    }
}
