//----------------------------------------------------------------------------//
//                                                                            //
//                               D i g r a p h                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.graph;

import omr.log.Logger;

import net.jcip.annotations.ThreadSafe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
 * @param <D> precise type for digraph (which is pointed back by vertex)
 * @param <V> precise type for vertices handled by this digraph
 * @param <SIG> precise type for vertex signature
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class Digraph<D extends Digraph<D, V, SIG>, V extends Vertex, SIG>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Digraph.class);

    //~ Instance fields --------------------------------------------------------

    /** Name of this instance, meant to ease debugging */
    private final String name;

    /** Related Vertex (sub)class, to create vertices of the proper type */
    private final Class<?extends V> vertexClass;

    /** All current Vertices of the graph, handled by a map: Id -> Vertex */
    private final ConcurrentHashMap<Integer, V> vertices = new ConcurrentHashMap<Integer, V>();

    /** Removed vertices */
    private final ConcurrentHashMap<Integer, V> oldVertices = new ConcurrentHashMap<Integer, V>();

    /** All vertices, accessed via their signature */
    private final ConcurrentHashMap<SIG, V> sigMap = new ConcurrentHashMap<SIG, V>();

    /** Global id to uniquely identify a vertex */
    private final AtomicInteger globalVertexId = new AtomicInteger(0);

    /** All Views on this graph, handled by a sequential collection */
    private final List<DigraphView> views = Collections.synchronizedList(
        new ArrayList<DigraphView>());

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Digraph //
    //---------//
    /**
     * Default constructor.
     * @param name the distinguished name for this instance
     * @param vertexClass precise class to be used when instantiating vertices
     */
    public Digraph (String            name,
                    Class<?extends V> vertexClass)
    {
        if (vertexClass == null) {
            throw new IllegalArgumentException("null vertex class");
        }

        this.name = name;
        this.vertexClass = vertexClass;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getLastVertexId //
    //-----------------//
    /**
     * Give access to the last id assigned to a vertex in this graph. This may
     * be greater than the number of vertices currently in the graph, because of
     * potential deletion of vertices (a Vertex Id is never reused).
     *
     * @return id of the last vertex created
     * @see #getVertexCount
     */
    public int getLastVertexId ()
    {
        return globalVertexId.get();
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
        V vertex = vertices.get(id);

        if (vertex != null) {
            return vertex;
        } else {
            return oldVertices.get(id);
        }
    }

    //----------------------//
    // getVertexBySignature //
    //----------------------//
    /**
     * Retrieve a vertex, knowing its signature
     * @param signature the vertex signature
     * @return the vertex found, or null
     */
    public V getVertexBySignature (SIG signature)
    {
        if (signature == null) {
            throw new RuntimeException(
                "Cannot retrieve a vertex via a null signature");
        }

        return sigMap.get(signature);
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

    // DEBUG
    public boolean isVertexCurrent (int id)
    {
        return vertices.get(id) != null;
    }

    // DEBUG
    public boolean isVertexOld (int id)
    {
        return oldVertices.get(id) != null;
    }

    //-------------//
    // getVertices //
    //-------------//
    /**
     * Export an unmodifiable and non-sorted collection of vertices of the graph
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

    //-----------//
    // addVertex //
    //-----------//
    /**
     * (package access from {@link Vertex}) to add a vertex in the graph, the
     * vertex is being assigned a unique id by the graph.
     * Made public just for access from glyph Verifier
     *
     * @param vertex the newly created vertex
     */
    @SuppressWarnings("unchecked")
    public void addVertex (V vertex)
    {
        if (vertex == null) {
            throw new IllegalArgumentException("Cannot add a null vertex");
        }

        vertex.setGraph(this); // Unchecked
        vertex.setId(globalVertexId.incrementAndGet()); // Atomic increment
        vertices.put(vertex.getId(), vertex); // Atomic insertion
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

    //-------------------//
    // registerSignature //
    //-------------------//
    /**
     * Register the signature of the provided vertex, thus allowing the
     * future retrieval of the vertex via its signature
     * @param vertex the provided signature
     */
    @SuppressWarnings("unchecked")
    public void registerSignature (V vertex)
    {
        SIG sig = (SIG) vertex.getSignature();

        if (sig != null) {
            sigMap.put(sig, vertex); // Atomic insertion
        }
    }

    //---------------//
    // restoreVertex //
    //---------------//
    /**
     * Restore an old vertex
     * @param vertex the old vertex to restore
     */
    public void restoreVertex (V vertex)
    {
        vertices.put(vertex.getId(), vertex); // Atomic insertion
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append(getClass().getSimpleName());
        sb.append(internalsString());
        sb.append("}");

        return sb.toString();
    }

    //-------------//
    // viewIndexOf //
    //-------------//
    /**
     * Return the index of the given view
     *
     * @param view the given view
     * @return the view index, or -1 if not found
     */
    public int viewIndexOf (DigraphView view)
    {
        return views.indexOf(view);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, typically for inclusion
     * in a toString. The overriding methods should comply with the following
     * rule: return either a totally empty string, or a string that begins with
     * a " " followed by some content.
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(25);

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

    //--------------//
    // removeVertex //
    //--------------//
    /**
     * (package access from Vertex) to remove the vertex from the graph, the
     * removed vertex will now be stored in the oldVertices map.
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

        if (vertices.remove(vertex.getId()) == null) { // Atomic removal
            throw new RuntimeException(
                "Trying to remove an unknown vertex: " + vertex);
        } else {
            oldVertices.put(vertex.getId(), vertex);
        }
    }
}
