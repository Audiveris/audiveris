//----------------------------------------------------------------------------//
//                                                                            //
//                                V e r t e x                                 //
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

import net.jcip.annotations.NotThreadSafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class <code>Vertex</code> encapsulates a Vertex (or Node) in a directed
 * graph. Any vertex can have incoming edges from other vertices and outgoing
 * edges to other vertices.
 *
 * <p>The Vertex can have a list of related <code>VertexView</code>s. All the
 * vertices in the graph have parallel lists of <code>VertexView</code>'s as the
 * Digraph itself which has a parallel list of <code>DigraphView</code>'s.
 *
 * <p>NOTA: This plain Vertex type has no room for user-defined data, if such
 * feature is needed then a proper subtype of Vertex should be used.
 *
 * <p>This class is not thread-safe, because it is not intended to be used by
 * several threads simultaneously. However, the graph structure which contains
 * instances of vertices is indeed thread-safe.
 *
 * @param <D> type for enclosing digraph precise subtype
 * @param <V> type for Vertex precise subtype
 * @param <SIG> type for vertex precise signature
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
@XmlAccessorType(XmlAccessType.NONE)
public abstract class Vertex<D extends Digraph, V extends Vertex<D, V, SIG>, SIG>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Vertex.class);

    /** Compilation flag to forbid duplication of edges : {@value} */
    public static final boolean NO_EDGE_DUPLICATES = true;

    //~ Instance fields --------------------------------------------------------

    /**
     * Unique vertex Id (for debugging mainly)
     */
    private int id;

    /**
     * Incoming edges from other vertices
     */
    private final List<V> sources = new ArrayList<V>();

    /**
     * Outgoing edges to other vertices
     */
    private final List<V> targets = new ArrayList<V>();

    /**
     * Containing graph
     */
    protected D graph;

    /**
     * Sequence of views created on this vertex. Index in the sequence is
     * important, since this sequence is kept parallel to the sequence of views
     * on the containing graph.
     */
    protected List<VertexView> views;

    /**
     * Computed vertex signature if any
     */
    protected SIG signature;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // Vertex //
    //--------//
    /**
     * Create a Vertex.
     */
    protected Vertex ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("new vertex");
        }
    }

    //--------//
    // Vertex //
    //--------//
    /**
     * Create a Vertex in a graph.
     * @param graph The containing graph where this vertex is to be hosted
     */
    @SuppressWarnings("unchecked")
    protected Vertex (D graph)
    {
        if (logger.isFineEnabled()) {
            logger.fine("new vertex in graph " + graph);
        }

        graph.addVertex(this); // Compiler warning here
    }

    //~ Methods ----------------------------------------------------------------

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

    //--------------//
    // setSignature //
    //--------------//
    /**
     * Ask the vertex to compute its signature and to register it in the
     * containing graph.
     * <b>Note</b>: If later you modify properties of this vertex in a way that
     * would modify the signature, note that the signature will not be
     * automatically recomputed.
     */
    @SuppressWarnings("unchecked")
    public void setSignature ()
    {
        SIG sig = computeSignature();

        if (sig == null) {
            throw new RuntimeException("Vertex computed signature is null");
        }

        if ((signature != null) && !signature.equals(sig)) {
            throw new RuntimeException("Attempt to modify a vertex signature");
        }

        this.signature = sig;
        graph.registerSignature(this);
    }

    //--------------//
    // getSignature //
    //--------------//
    /**
     * Report the vertex signature
     * @return the vertex signature
     */
    public SIG getSignature ()
    {
        if (signature == null) {
            throw new RuntimeException("Vertex with null signature");
        }

        return signature;
    }

    //---------//
    // addEdge //
    //---------//
    /**
     * Create an edge between two vertices
     *
     * @param <D> precise digraph type
     * @param <V> precise vertex type
     * @param <SIG> precise signature type
     * @param source departure vertex
     * @param target arrival vertex
     */
    public static <D extends Digraph, V extends Vertex<D, V, SIG>, SIG> void addEdge (V source,
                                                                                      V target)
    {
        if (logger.isFineEnabled()) {
            logger.fine("adding edge from " + source + " to " + target);
        }

        // Assert we have real vertices
        if ((source == null) || (target == null)) {
            throw new IllegalArgumentException(
                "At least one of the edge vertices is null");
        }

        // Assert they belong to the same graph
        if (source.getGraph() != target.getGraph()) {
            throw new RuntimeException(
                "An edge can link vertices of the same graph only");
        }

        if (NO_EDGE_DUPLICATES) {
            source.removeTarget(target);
            target.removeSource(source);
        }

        source.addTarget(target);
        target.addSource(source);
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
        return Collections.unmodifiableList(sources);
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
        return Collections.unmodifiableList(targets);
    }

    //---------//
    // getView //
    //---------//
    /**
     * Report the view at given index
     * @param index index of the desired view
     * @return the desired view
     */
    public VertexView getView (int index)
    {
        return getViews()
                   .get(index);
    }

    //---------------//
    // getViewsCount //
    //---------------//
    public int getViewsCount ()
    {
        return getViews()
                   .size();
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
        getViews()
            .add(view);
    }

    //------------//
    // clearViews //
    //------------//
    /**
     * Get rid of all views for this vertex
     */
    public void clearViews ()
    {
        getViews()
            .clear();
    }

    //--------//
    // delete //
    //--------//
    /**
     * Delete this vertex. This implies also the removal of all its incoming and
     * outgoing edges.
     */
    @SuppressWarnings("unchecked")
    public void delete ()
    {
        try {
            if (logger.isFineEnabled()) {
                logger.fine("deleting vertex " + this);
            }

            // Remove in vertices of the vertex
            for (V source : new ArrayList<V>(getSources())) {
                removeEdge(source, this, false);
            }

            // Remove out vertices of the vertex
            for (V target : new ArrayList<V>(getTargets())) {
                removeEdge(this, target, false);
            }

            // Remove from graph
            graph.removeVertex(this); // Compiler warning here
        } catch (Exception ex) {
            logger.severe("Error deleting " + this, ex);
        }
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

    //------------//
    // removeEdge //
    //------------//
    /**
     * Remove an edge between two vertices
     *
     * @param source departure vertex
     * @param target arrival vertex
     * @param strict throw RuntimeException if the edge does not exist
     */
    @SuppressWarnings("unchecked")
    public static <V extends Vertex> void removeEdge (V       source,
                                                      V       target,
                                                      boolean strict)
    {
        if (logger.isFineEnabled()) {
            logger.fine("removing edge from " + source + " to " + target);
        }

        if (!source.removeTarget(target) && strict) {
            throw new RuntimeException(
                "Attempting to remove non-existing edge between " + source +
                " and " + target);
        }

        if (!target.removeSource(source) && strict) {
            throw new RuntimeException(
                "Attempting to remove non-existing edge between " + source +
                " and " + target);
        }
    }

    //----------//
    // setGraph //
    //----------//
    /**
     * (package access from graph) Assign the containing graph of this vertex
     * @param graph The hosting graph
     */
    public void setGraph (D graph)
    {
        this.graph = graph;
    }

    //-------//
    // setId //
    //-------//
    /**
     * Assign a new Id (for expert use only)
     *
     * @param id The assigned id
     */
    @XmlAttribute
    public void setId (int id)
    {
        this.id = id;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{")
          .append(getClass().getSimpleName())
          .append("#")
          .append(id);

        sb.append(internalsString());

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // addSource //
    //-----------//
    protected boolean addSource (V source)
    {
        return sources.add(source);
    }

    //-----------//
    // addTarget //
    //-----------//
    protected boolean addTarget (V target)
    {
        return targets.add(target);
    }

    //------------------//
    // computeSignature //
    //------------------//
    /**
     * The method which computes the vertex signature
     * @return the vertex signature
     */
    protected abstract SIG computeSignature ();

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, typically for inclusion
     * in a toString. The overriding methods, if any, should return a string
     * that begins with a " " followed by some content.
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(100);

        sb.append(" ")
          .append(getInDegree());
        sb.append("/")
          .append(getOutDegree());

        return sb.toString();
    }

    //--------------//
    // removeSource //
    //--------------//
    protected boolean removeSource (V source)
    {
        return sources.remove(source);
    }

    //--------------//
    // removeTarget //
    //--------------//
    protected boolean removeTarget (V target)
    {
        return targets.remove(target);
    }

    //----------//
    // getViews //
    //----------//
    /**
     * Report the sequence of the related views, lazily created.
     *
     * @return the views collection (perhaps empty, but not null)
     */
    private List<VertexView> getViews ()
    {
        if (views == null) {
            views = new ArrayList<VertexView>();
        }

        return views;
    }
}
