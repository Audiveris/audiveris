//----------------------------------------------------------------------------//
//                                                                            //
//                           B a s i c V e r t e x                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.graph;

import omr.log.Logger;

import net.jcip.annotations.NotThreadSafe;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code BasicVertex} is a basic implementation of {@link Vertex}
 *
 * <p>NOTA: This plain Vertex type has no room for user-defined data, if such
 * feature is needed then a proper subtype of BasicVertex should be used.
 *
 * <p>This class is not thread-safe, because it is not intended to be used by
 * several threads simultaneously. However, the graph structure which contains
 * instances of vertices is indeed thread-safe.
 *
 * @param <D> type for enclosing digraph precise subtype
 * @param <V> type for Vertex precise subtype
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
@XmlAccessorType(XmlAccessType.NONE)
public abstract class BasicVertex<D extends Digraph, V extends Vertex<D, V>>
    implements Vertex<D, V>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BasicVertex.class);

    //~ Instance fields --------------------------------------------------------

    /**
     * Unique vertex Id (for debugging mainly)
     */
    private int id;

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
     * Sequence of views created on this vertex. Index in the sequence is
     * important, since this sequence is kept parallel to the sequence of views
     * on the containing graph.
     */
    protected List<VertexView> views;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // Vertex //
    //--------//
    /**
     * Create a Vertex.
     */
    protected BasicVertex ()
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
    protected BasicVertex (D graph)
    {
        if (logger.isFineEnabled()) {
            logger.fine("new vertex in graph " + graph);
        }

        graph.addVertex(this); // Compiler warning here
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // addTarget //
    //-----------//
    public void addTarget (V target)
    {
        if (logger.isFineEnabled()) {
            logger.fine("adding edge from " + this + " to " + target);
        }

        // Assert we have real target
        if (target == null) {
            throw new IllegalArgumentException(
                "Cannot add an edge to a null target");
        }

        // Assert this vertex and target vertex belong to the same graph
        if (this.getGraph() != target.getGraph()) {
            throw new RuntimeException(
                "An edge can link vertices of the same graph only");
        }

        // Avoid duplicates
        getTargets()
            .remove(target);
        target.getSources()
              .remove((V) this);

        targets.add(target);
        target.getSources()
              .add((V) this);
    }

    //---------//
    // addView //
    //---------//
    public void addView (VertexView view)
    {
        getViews()
            .add(view);
    }

    //------------//
    // clearViews //
    //------------//
    public void clearViews ()
    {
        getViews()
            .clear();
    }

    //--------//
    // delete //
    //--------//
    public void delete ()
    {
        try {
            if (logger.isFineEnabled()) {
                logger.fine("deleting vertex " + this);
            }

            // Remove in vertices of the vertex
            for (V source : new ArrayList<V>(getSources())) {
                source.removeTarget((V) this, false);
            }

            // Remove out vertices of the vertex
            for (V target : new ArrayList<V>(getTargets())) {
                removeTarget(target, false);
            }

            // Remove from graph
            graph.removeVertex(this);
        } catch (Exception ex) {
            logger.severe("Error deleting " + this, ex);
        }
    }

    //------//
    // dump //
    //------//
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

    //----------//
    // getGraph //
    //----------//
    public D getGraph ()
    {
        return graph;
    }

    //-------//
    // getId //
    //-------//
    public int getId ()
    {
        return id;
    }

    //-------------//
    // getInDegree //
    //-------------//
    public int getInDegree ()
    {
        return sources.size();
    }

    //--------------//
    // getOutDegree //
    //--------------//
    public int getOutDegree ()
    {
        return targets.size();
    }

    //------------//
    // getSources //
    //------------//
    public List<V> getSources ()
    {
        return sources;
    }

    //------------//
    // getTargets //
    //------------//
    public List<V> getTargets ()
    {
        return targets;
    }

    //---------//
    // getView //
    //---------//
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

    //--------------//
    // removeTarget //
    //--------------//
    public void removeTarget (V       target,
                              boolean strict)
    {
        // Assert we have real target
        if (target == null) {
            throw new IllegalArgumentException(
                "Cannot remove an edge to a null target");
        }

        if (!this.targets.remove(target) && strict) {
            throw new RuntimeException(
                "Attempting to remove non-existing edge between " + this +
                " and " + target);
        }

        if (!target.getSources()
                   .remove((V) this) && strict) {
            throw new RuntimeException(
                "Attempting to remove non-existing edge between " + this +
                " and " + target);
        }
    }

    //----------//
    // setGraph //
    //----------//
    /**
     * (package access from graph)
     */
    public void setGraph (D graph)
    {
        this.graph = graph;
    }

    //-------//
    // setId //
    //-------//
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

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, typically for inclusion
     * in a toString. The overriding methods, if any, should return a string
     * that begins with a " " followed by some content.
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

    //----------//
    // getViews //
    //----------//
    /**
     * Report the sequence of the related views, lazily created.
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
