//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S I G r a p h T e s t                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.sig.Exclusion.Cause;

import omr.util.HorizontalSide;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultListenableGraph;
import org.jgrapht.graph.Multigraph;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

/**
 * Class {@literal SIGraphTest} tests unitary features of SIGraph.
 *
 * @author Hervé Bitteur
 */
public class SIGraphTest
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static JGraphModelAdapter<Inter, Relation> jgAdapter;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SIGraphTest object.
     */
    public SIGraphTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * An alternative starting point for this demo, to also allow running this
     * applet as an application.
     *
     * @param args ignored.
     */
    public static void main (String[] args)
    {
        //SIGraph     sig = new SIGraph();
        ListenableGraph sig = new ListenableGraph();

        // create a visualization using JGraph, via an adapter
        jgAdapter = new JGraphModelAdapter<Inter, Relation>(sig);

        JGraph jgraph = new JGraph(jgAdapter);

        JScrollPane scroller = new JScrollPane(jgraph);

        JFrame frame = new JFrame();
        frame.add(scroller);
        frame.setTitle("Interpretation/Relation Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        frame.setSize(600, 600);
        frame.setVisible(true);

        Inter blanche = new TestInter(Shape.NOTEHEAD_VOID, 0.5);
        sig.addVertex(blanche);

        Inter head = new TestInter(Shape.NOTEHEAD_BLACK, 0.6);
        sig.addVertex(head);

        Inter beam = new TestInter(Shape.BEAM, 0.8);
        sig.addVertex(beam);

        Inter hook = new TestInter(Shape.BEAM_HOOK, 0.7);
        sig.addVertex(hook);

        Inter stem = new TestInter(Shape.STEM, 0.3);
        sig.addVertex(stem);

        //        RelationFactory factory = new RelationFactory();
        //        Relation hs = factory.createEdge(head, stem);
        HeadStemRelation hs = new HeadStemRelation();
        hs.setHeadSide(HorizontalSide.LEFT);
        hs.setGrade(0.7);
        sig.addEdge(head, stem, hs);

        HeadStemRelation hs2 = new HeadStemRelation();
        hs2.setHeadSide(HorizontalSide.LEFT);
        hs2.setGrade(0.7);
        sig.addEdge(blanche, stem, hs2);

        sig.addEdge(head, blanche, new BasicExclusion(Cause.OVERLAP));

        sig.addEdge(beam, stem, new BasicSupport(0.2));
        sig.addEdge(hook, stem, new BasicSupport(0.4));

        Inter stem2 = new TestInter(Shape.STEM, 0.5);
        sig.addVertex(stem2);
        sig.addEdge(beam, stem2);

        System.out.println(sig.toString());

        //        positionVertexAt(head, 50, 150);
        //        positionVertexAt(stem, 150, 250);
        //        positionVertexAt(beam, 250, 350);
        //        positionVertexAt(stem2, 300, 250);
        // Pass the facade the JGraph instance
        JGraphFacade facade = new JGraphFacade(jgraph);

        // Create an instance of the appropriate layout
        //JGraphLayout layout = new JGraphFastOrganicLayout();
        JGraphLayout layout = new JGraphHierarchicalLayout();

        // Run the layout on the facade. Note that layouts do not implement the Runnable interface, to avoid confusion
        layout.run(facade);

        // Obtain a map of the resulting attribute changes from the facade
        Map nested = facade.createNestedMap(true, true);

        // Apply the results to the actual graph
        jgraph.getGraphLayoutCache().edit(nested);
    }

    @SuppressWarnings("unchecked")
    private static void positionVertexAt (Object vertex,
                                          int x,
                                          int y)
    {
        DefaultGraphCell cell = jgAdapter.getVertexCell(vertex);
        AttributeMap attr = cell.getAttributes();
        Rectangle2D bounds = GraphConstants.getBounds(attr);

        Rectangle2D newBounds = new Rectangle2D.Double(
                x,
                y,
                bounds.getWidth(),
                bounds.getHeight());

        GraphConstants.setBounds(attr, newBounds);

        // TODO: Clean up generics once JGraph goes generic
        AttributeMap cellAttr = new AttributeMap();
        cellAttr.put(cell, attr);
        jgAdapter.edit(cellAttr, null, null, null);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // TestInter //
    //-----------//
    private static class TestInter
            extends AbstractInter
    {
        //~ Constructors ---------------------------------------------------------------------------

        public TestInter (Shape shape,
                           double grade)
        {
            super((Glyph) null, (Rectangle) null, shape, grade);
        }
    }

    //-----------------//
    // ListenableGraph //
    //-----------------//
    /**
     * a listenable directed multigraph that allows loops and parallel edges.
     */
    private static class ListenableGraph
            extends DefaultListenableGraph<Inter, Relation>
            implements UndirectedGraph<Inter, Relation>
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final long serialVersionUID = 1L;

        //~ Constructors ---------------------------------------------------------------------------
        ListenableGraph ()
        {
            super(new Multigraph<Inter, Relation>(Relation.class));
        }
    }
}
