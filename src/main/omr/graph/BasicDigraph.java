//----------------------------------------------------------------------------//
//                                                                            //
//                          B a s i c D i g r a p h                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.graph;

import omr.log.Logger;

import net.jcip.annotations.ThreadSafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class {@code BasicDigraph} is a basic implementation of Digraph.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class BasicDigraph<D extends Digraph<D, V>, V extends Vertex>
        implements Digraph<D, V>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Digraph.class);

    //~ Instance fields --------------------------------------------------------
    /** Name of this instance, meant to ease debugging */
    private final String name;

    /** Related Vertex (sub)class, to create vertices of the proper type */
    private final Class<? extends V> vertexClass;

    /** All current Vertices of the graph, handled by a map: Id -> Vertex */
    private final ConcurrentHashMap<Integer, V> vertices = new ConcurrentHashMap<>();

    /** Removed vertices */
    private final ConcurrentHashMap<Integer, V> oldVertices = new ConcurrentHashMap<>();

    /** Global id to uniquely identify a vertex */
    private final AtomicInteger globalVertexId = new AtomicInteger(0);

    /** All Views on this graph, handled by a sequential collection */
    private final List<DigraphView> views = Collections.synchronizedList(
            new ArrayList<DigraphView>());

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // BasicDigraph //
    //--------------//
    /**
     * Construct a BasicDigraph object.
     *
     * @param name        the distinguished name for this instance
     * @param vertexClass precise class to be used when instantiating vertices
     */
    public BasicDigraph (String name,
                         Class<? extends V> vertexClass)
    {
        if (vertexClass == null) {
            throw new IllegalArgumentException("null vertex class");
        }

        this.name = name;
        this.vertexClass = vertexClass;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // addVertex //
    //-----------//
    @Override
    public void addVertex (V vertex)
    {
        if (vertex == null) {
            throw new IllegalArgumentException("Cannot add a null vertex");
        }

        vertex.setGraph(this); // Unchecked
        vertex.setId(globalVertexId.incrementAndGet()); // Atomic increment
        vertices.put(vertex.getId(), vertex); // Atomic insertion
    }

    //--------------//
    // createVertex //
    //--------------//
    @Override
    public V createVertex ()
    {
        V vertex;

        try {
            vertex = vertexClass.newInstance();
            addVertex(vertex);

            return vertex;
        } catch (NullPointerException ex) {
            throw new RuntimeException(
                    "BasicDigraph cannot create vertex, vertexClass not set");
        } catch (InstantiationException ex) {
            throw new RuntimeException(
                    "Cannot createVertex with an abstract class or interface: "
                    + vertexClass);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    //------//
    // dump //
    //------//
    @Override
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

    //-----------------//
    // getLastVertexId //
    //-----------------//
    @Override
    public int getLastVertexId ()
    {
        return globalVertexId.get();
    }

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return name;
    }

    //---------------//
    // getVertexById //
    //---------------//
    @Override
    public V getVertexById (int id)
    {
        V vertex = vertices.get(id);

        if (vertex != null) {
            return vertex;
        } else {
            return oldVertices.get(id);
        }
    }

    //----------------//
    // getVertexCount //
    //----------------//
    @Override
    public int getVertexCount ()
    {
        return vertices.size();
    }

    //-------------//
    // getVertices //
    //-------------//
    @Override
    public Collection<V> getVertices ()
    {
        return Collections.unmodifiableCollection(vertices.values());
    }

    //----------//
    // getViews //
    //----------//
    @Override
    public Collection<DigraphView> getViews ()
    {
        return Collections.unmodifiableCollection(views);
    }

    //--------------//
    // removeVertex //
    //--------------//
    @Override
    public void removeVertex (V vertex)
    {
        logger.fine("remove {0}", vertex);

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

    //---------------//
    // restoreVertex //
    //---------------//
    @Override
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
        sb.append("{").append(getClass().getSimpleName());
        sb.append(internalsString());
        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, typically for
     * inclusion in a toString.
     * The overriding methods should comply with the following rule:
     * return either a totally empty string, or a string that begins with
     * a " " followed by some content.
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(25);

        sb.append("#").append(name);

        sb.append(" vertices=").append(getVertexCount());

        if (this.getClass().getName().equals(Digraph.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }
}
