//----------------------------------------------------------------------------//
//                                                                            //
//                           B a s i c V e r t e x                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.graph;

import net.jcip.annotations.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code BasicVertex} is a basic implementation of {@link Vertex}.
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
    private static final Logger logger = LoggerFactory.getLogger(BasicVertex.class);

    //~ Instance fields --------------------------------------------------------
    /**
     * Unique vertex Id (for debugging mainly)
     */
    private int id;

    /**
     * Incoming edges from other vertices
     */
    protected final List<V> sources = new ArrayList<>();

    /**
     * Outgoing edges to other vertices
     */
    protected final List<V> targets = new ArrayList<>();

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
        logger.debug("new vertex");
    }

    //--------//
    // Vertex //
    //--------//
    /**
     * Create a Vertex in a graph.
     *
     * @param graph The containing graph where this vertex is to be hosted
     */
    protected BasicVertex (D graph)
    {
        logger.debug("new vertex in graph {}", graph);

        graph.addVertex(this); // Compiler warning here
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // addTarget //
    //-----------//
    @Override
    public void addTarget (V target)
    {
        logger.debug("adding edge from {} to {}", this, target);

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
        getTargets().remove(target);
        target.getSources().remove((V) this);

        targets.add(target);
        target.getSources().add((V) this);
    }

    //---------//
    // addView //
    //---------//
    @Override
    public void addView (VertexView view)
    {
        getViews().add(view);
    }

    //------------//
    // clearViews //
    //------------//
    @Override
    public void clearViews ()
    {
        getViews().clear();
    }

    //--------//
    // delete //
    //--------//
    @Override
    public void delete ()
    {
        try {
            logger.debug("deleting vertex {}", this);

            // Remove in vertices of the vertex
            for (V source : new ArrayList<>(getSources())) {
                source.removeTarget((V) this, false);
            }

            // Remove out vertices of the vertex
            for (V target : new ArrayList<>(getTargets())) {
                removeTarget(target, false);
            }

            // Remove from graph
            graph.removeVertex(this);
        } catch (Exception ex) {
            logger.error("Error deleting " + this, ex);
        }
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        StringBuilder sb = new StringBuilder();

        // The vertex
        sb.append(String.format("%s%n", this));

        // The in edges
        for (V vertex : sources) {
            sb.append(String.format("  edge from %s%n", vertex));
        }

        // The out edges
        for (V vertex : targets) {
            sb.append(String.format("  edge to %s%n", vertex));
        }

        logger.info(sb.toString());
    }

    //----------//
    // getGraph //
    //----------//
    @Override
    public D getGraph ()
    {
        return graph;
    }

    //-------//
    // getId //
    //-------//
    @Override
    public int getId ()
    {
        return id;
    }

    //-------------//
    // getInDegree //
    //-------------//
    @Override
    public int getInDegree ()
    {
        return sources.size();
    }

    //--------------//
    // getOutDegree //
    //--------------//
    @Override
    public int getOutDegree ()
    {
        return targets.size();
    }

    //------------//
    // getSources //
    //------------//
    @Override
    public List<V> getSources ()
    {
        return sources;
    }

    //------------//
    // getTargets //
    //------------//
    @Override
    public List<V> getTargets ()
    {
        return targets;
    }

    //---------//
    // getView //
    //---------//
    @Override
    public VertexView getView (int index)
    {
        return getViews().get(index);
    }

    //---------------//
    // getViewsCount //
    //---------------//
    @Override
    public int getViewsCount ()
    {
        return getViews().size();
    }

    //--------------//
    // removeTarget //
    //--------------//
    @Override
    public void removeTarget (V target,
                              boolean strict)
    {
        // Assert we have real target
        if (target == null) {
            throw new IllegalArgumentException(
                    "Cannot remove an edge to a null target");
        }

        if (!this.targets.remove(target) && strict) {
            throw new RuntimeException(
                    "Attempting to remove non-existing edge between " + this
                    + " and " + target);
        }

        if (!target.getSources().remove((V) this) && strict) {
            throw new RuntimeException(
                    "Attempting to remove non-existing edge between " + this
                    + " and " + target);
        }
    }

    //----------//
    // setGraph //
    //----------//
    /**
     * (package access from graph)
     */
    @Override
    public void setGraph (D graph)
    {
        this.graph = graph;
    }

    //-------//
    // setId //
    //-------//
    @XmlAttribute
    @Override
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

        sb.append("{").append(getClass().getSimpleName()).append("#").append(id);

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
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(100);

        sb.append(" ").append(getInDegree());
        sb.append("/").append(getOutDegree());

        return sb.toString();
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
            views = new ArrayList<>();
        }

        return views;
    }
}
