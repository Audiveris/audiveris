//----------------------------------------------------------------------------//
//                                                                            //
//                               D i g r a p h                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.graph;

import omr.util.Logger;

import java.util.*;

/**
 * Class <code>Digraph</code> handles a directed graph, a structure containing
 * an <b>homogeneous</b> collection of instances of Vertex (or a collection of
 * homogeneous types derived from Vertex), potentially linked by directed edges.
 * <p/>
 *
 * <p> Vertices can exist in isolation, but an edge can exist only from a vertex
 * to another vertex. Thus, removing a vertex implies removing all its incoming
 * and outgoing edges.
 *
 * <p><b>NOTA</b>: Since we have no data to carry in edges, there is no
 * <code>Edge</code> type per se, links between vertices are implemented simply
 * by Lists of Vertex.
 *
 * @param <D> precise (sub)type for the graph
 * @param <V> precise type for handled vertices
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Digraph<D extends Digraph<D, V>, V extends Vertex>
    implements java.io.Serializable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Digraph.class);

    //~ Instance fields --------------------------------------------------------

    /**
     * All Vertices of the graph
     */
    private final SortedMap<Integer, V> vertices = new TreeMap<Integer, V>();

    /**
     * All Views on this graph
     */
    private transient List<DigraphView> views = new ArrayList<DigraphView>();

    /**
     * Name for debugging
     */
    private final String name;

    /**
     * Global id to uniquely identify a vertex
     */
    private int globalVertexId = 0;

    /**
     * Related Vertex (sub)class, to create vertices of the proper type
     */
    private Class<?extends V> vertexClass;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Digraph //
    //---------//
    /**
     * Default constructor.
     */
    public Digraph (String name)
    {
        this.name = name;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getLastVertexId //
    //-----------------//
    /**
     * Give access to the last id assigned to a vertex in this graph. This may
     * be greater than the number of vertices currently in the graph, because of
     * potential deletion of vertices (Vertex Id is never reused).
     *
     * @return id of the last vertex created
     * @see #getVertexCount
     */
    public int getLastVertexId ()
    {
        return globalVertexId;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name assigned to this graph instance
     *
     * @return the readable name
     */
    public String getName ()
    {
        return name;
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
    public V getVertexById (int id)
    {
        return vertices.get(id);
    }

    //----------------//
    // setVertexClass //
    //----------------//
    /**
     * Assigned the vertexClass (needed to create new vertices in the graph).
     * This is meant to complement the default constructor.
     *
     * @param vertexClass the class to be used when instantiating vertices
     */
    public void setVertexClass (Class<?extends V> vertexClass)
    {
        if (vertexClass == null) {
            throw new IllegalArgumentException("null vertex class");
        } else {
            this.vertexClass = vertexClass;
        }
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

    //-------------//
    // getVertices //
    //-------------//
    /**
     * Export the unmodifiable collection of vertices of the graph.
     *
     * @return the unmodifiable collection of vertices
     */
    public Collection<V> getVertices ()
    {
        return Collections.unmodifiableCollection(vertices.values());
    }

    //----------//
    // getViews //
    //----------//
    /**
     * Give access to the (unmodifiable) collection of related views if any
     *
     * @return the unmodifiable collection of views
     */
    public Collection<DigraphView> getViews ()
    {
        return Collections.unmodifiableCollection(views);
    }

    //-------------//
    // viewIndexOf //
    //-------------//
    /**
     * Return the index of the given view
     *
     * @return the view index, or -1 if not found
     */
    public int viewIndexOf (DigraphView view)
    {
        return views.indexOf(view);
    }

    //-----------//
    // addVertex //
    //-----------//
    /**
     * (package access from {@link Vertex}) to add a vertex in the graph, the
     * vertex is being assigned a unique id by the graph.
     *
     * @param vertex the newly created vertex
     */
    public void addVertex (V vertex) // HB PUBLIC just for Verifier

    {
        if (vertex == null) {
            throw new IllegalArgumentException("Cannot add a null vertex");
        }

        vertices.put(++globalVertexId, vertex);
        vertex.setId(globalVertexId);
        vertex.setGraph(this); // Compiler warning here
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
        views.add(view);
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
            addVertex(vertex);

            return vertex;
        } catch (NullPointerException ex) {
            throw new RuntimeException(
                "Digraph cannot create vertex, vertexClass not set");
        } catch (InstantiationException ex) {
            throw new RuntimeException(
                "Cannot createVertex with an abstract class or interface: " +
                vertexClass);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
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

        sb.append("{")
          .append(getPrefix());

        sb.append("#")
          .append(name);

        sb.append(" vertices=")
          .append(getVertexCount());

        if (this.getClass()
                .getName()
                .equals(Digraph.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }

    //-----------//
    // getPrefix //
    //-----------//
    /**
     * Return a distinctive string, to be used as a prefix in toString() for
     * example.
     *
     * @return the prefix string
     */
    protected String getPrefix ()
    {
        return "Digraph";
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
        if (logger.isFineEnabled()) {
            logger.fine("remove " + vertex);
        }

        if (vertex == null) {
            throw new IllegalArgumentException(
                "Trying to remove a null vertex");
        }

        if (vertices.remove(vertex.getId()) == null) {
            throw new RuntimeException(
                "Trying to remove an unknown vertex: " + vertex);
        }
    }
}
